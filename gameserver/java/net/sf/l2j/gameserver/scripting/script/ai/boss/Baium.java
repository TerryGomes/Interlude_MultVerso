package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Baium is the last famous Emperor of Elmoreden, the creator of the Tower of Insolence, and the bringer of the Golden Age. He is also the father of Frintezza, the last Emperor, and of Saint Solina.<br>
 * <br>
 * Following animations are handled in that time tempo :
 * <ul>
 * <li>wake(2), 0-13 secs</li>
 * <li>neck(3), 14-24 secs.</li>
 * <li>roar(1), 25-37 secs.</li>
 * </ul>
 * Waker's sacrifice is handled between neck and roar animation.
 */
public class Baium extends AttackableAIScript
{
	public static final BossZone BAIUM_LAIR = ZoneManager.getInstance().getZoneById(110002, BossZone.class);
	
	private static final int LIVE_BAIUM = 29020;
	private static final int ARCHANGEL = 29021;
	private static final int STONE_BAIUM = 29025;
	private static final int TELEPORTATION_CUBIC = 29055;
	
	public static final byte ASLEEP = 0; // Baium is in the stone version, waiting to be woken up. Entry is unlocked.
	public static final byte AWAKE = 1; // Baium is awake and fighting. Entry is locked.
	public static final byte DEAD = 2; // Baium has been killed and has not yet spawned. Entry is locked.
	
	private static final SpawnLocation STONE_BAIUM_LOC = new SpawnLocation(116033, 17447, 10104, 40188);
	private static final SpawnLocation TELEPORTATION_CUBIC_LOC = new SpawnLocation(115203, 16620, 10078, 0);
	
	private static final SpawnLocation[] ARCHANGEL_LOCS =
	{
		new SpawnLocation(114239, 17168, 10080, 63544),
		new SpawnLocation(115780, 15564, 10080, 13620),
		new SpawnLocation(114880, 16236, 10080, 5400),
		new SpawnLocation(115168, 17200, 10080, 0),
		new SpawnLocation(115792, 16608, 10080, 0)
	};
	
	private final Set<Npc> _minions = ConcurrentHashMap.newKeySet(5);
	
	private Creature _actualVictim;
	private long _timeTracker = 0;
	
	public Baium()
	{
		super("ai/boss");
		
		addTalkId(STONE_BAIUM);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(LIVE_BAIUM);
		final int status = GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM);
		
		if (status == DEAD)
		{
			final long respawnTime = (info.getLong("respawn_time") - System.currentTimeMillis());
			
			// The time has not yet expired. Mark Baium as currently locked (dead).
			if (respawnTime > 0)
				startQuestTimer("baium_unlock", null, null, respawnTime);
			// The time has expired while the server was offline. Spawn the stone-baium as ASLEEP.
			else
			{
				addSpawn(STONE_BAIUM, STONE_BAIUM_LOC, false, 0, false);
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			}
		}
		else if (status == AWAKE)
		{
			final Npc baium = addGrandBossSpawn(LIVE_BAIUM, info);
			
			// Start monitoring Baium's inactivity.
			_timeTracker = System.currentTimeMillis();
			
			// Spawn angels, set them as minions, force run and add them to List.
			for (SpawnLocation loc : ARCHANGEL_LOCS)
			{
				final Npc angel = addSpawn(ARCHANGEL, loc, false, 0, true);
				((Monster) angel).setMinion(true);
				angel.forceRunStance();
				
				_minions.add(angel);
			}
			
			startQuestTimerAtFixedRate("baium_despawn", baium, null, 60000);
			startQuestTimerAtFixedRate("skill_range", baium, null, 2000);
			startQuestTimerAtFixedRate("angels_aggro_reconsider", null, null, 5000);
		}
		else
			addSpawn(STONE_BAIUM, STONE_BAIUM_LOC, false, 0, false);
	}
	
	@Override
	protected void registerNpcs()
	{
		addEventIds(LIVE_BAIUM, ScriptEventType.ON_ATTACK, ScriptEventType.ON_KILL, ScriptEventType.ON_SPAWN);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("skill_range"))
		{
			if (npc.isInvul())
				return super.onTimer(name, npc, player);
			
			// Pickup a target if no or dead victim. If Baium was hitting an angel, 50% luck he reconsiders his target. 10% luck he decides to reconsiders his target.
			if (_actualVictim == null || _actualVictim.isDead() || !npc.knows(_actualVictim) || (_actualVictim instanceof Monster && Rnd.get(10) < 5) || Rnd.get(10) == 0)
				_actualVictim = getRandomTarget(npc);
			
			// If result is null, return directly.
			if (_actualVictim == null)
				return super.onTimer(name, npc, player);
			
			final L2Skill skill = SkillTable.getInstance().getInfo(getRandomSkill(npc), 1);
			npc.getAI().tryToCast((skill.getId() == 4135) ? npc : _actualVictim, skill);
		}
		else if (name.equalsIgnoreCase("baium_neck"))
		{
			npc.broadcastPacket(new SocialAction(npc, 3));
		}
		else if (name.equalsIgnoreCase("sacrifice_waker"))
		{
			if (player != null)
			{
				// If player is far of Baium, teleport him back.
				if (!player.isIn3DRadius(npc, 300))
				{
					BAIUM_LAIR.allowPlayerEntry(player, 10);
					player.teleportTo(115929, 17349, 10077, 0);
				}
				
				// 60% to die.
				if (Rnd.get(100) < 60)
				{
					npc.broadcastNpcSay(NpcStringId.ID_22937);
					player.doDie(npc);
				}
			}
		}
		else if (name.equalsIgnoreCase("baium_roar"))
		{
			// Roar animation
			npc.broadcastPacket(new SocialAction(npc, 1));
			
			// Spawn angels, set them as minions, force run and add them to List.
			for (SpawnLocation loc : ARCHANGEL_LOCS)
			{
				final Npc angel = addSpawn(ARCHANGEL, loc, false, 0, true);
				((Monster) angel).setMinion(true);
				angel.forceRunStance();
				
				_minions.add(angel);
			}
			
			startQuestTimerAtFixedRate("angels_aggro_reconsider", null, null, 5000);
		}
		else if (name.equalsIgnoreCase("baium_move"))
		{
			npc.setInvul(false);
			npc.forceRunStance();
			
			// Start monitoring baium's inactivity and activate the AI
			_timeTracker = System.currentTimeMillis();
			
			startQuestTimerAtFixedRate("baium_despawn", npc, null, 60000);
			startQuestTimerAtFixedRate("skill_range", npc, null, 2000);
		}
		else if (name.equalsIgnoreCase("baium_despawn"))
		{
			final long currentTime = System.currentTimeMillis();
			if (_timeTracker + 1800000 < currentTime)
			{
				// Stop all tasks.
				cancelQuestTimers("baium_despawn");
				cancelQuestTimers("skill_range");
				cancelQuestTimers("angels_aggro_reconsider");
				
				// Despawn Baium.
				npc.deleteMe();
				
				// Unspawn angels, and clear the associated List.
				for (Npc minion : _minions)
				{
					minion.getSpawn().setRespawnState(false);
					minion.deleteMe();
				}
				_minions.clear();
				
				// Spawn Stone-like Baium.
				addSpawn(STONE_BAIUM, STONE_BAIUM_LOC, false, 0, false);
				
				// Set it as asleep.
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
				
				// Kick all Players.
				BAIUM_LAIR.oustAllPlayers();
			}
			else if ((_timeTracker + 300000 < currentTime) && npc.getStatus().getHpRatio() < 0.75)
			{
				npc.getAI().tryToCast(npc, 4135, 1);
			}
			else if (!BAIUM_LAIR.isInsideZone(npc))
			{
				npc.teleportTo(STONE_BAIUM_LOC, 0);
			}
		}
		else if (name.equalsIgnoreCase("baium_unlock"))
		{
			GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			addSpawn(STONE_BAIUM, STONE_BAIUM_LOC, false, 0, false);
		}
		else if (name.equalsIgnoreCase("angels_aggro_reconsider"))
		{
			boolean updateTarget = false; // Update or no the target
			
			for (Npc minion : _minions)
			{
				final Attackable angel = ((Attackable) minion);
				final AggroInfo ai = angel.getAggroList().getMostHated();
				
				// Chaos time.
				if (Rnd.get(100) < 10)
					updateTarget = true;
				else
				{
					// No target currently.
					if (ai == null)
						updateTarget = true;
					// Target is a unarmed player ; clean aggro.
					else if (ai.getAttacker() instanceof Player && ai.getAttacker().getActiveWeaponInstance() == null)
					{
						ai.stopHate();
						updateTarget = true;
					}
				}
				
				if (updateTarget)
				{
					Creature target = getRandomTarget(minion);
					if (target != null && ai != null && ai.getAttacker() != target)
						angel.forceAttack(target, 10000);
				}
			}
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM) == ASLEEP)
		{
			GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, AWAKE);
			
			final Npc baium = addSpawn(LIVE_BAIUM, npc, false, 0, false);
			baium.setInvul(true);
			
			GrandBossManager.getInstance().addBoss((GrandBoss) baium);
			
			// First animation.
			baium.broadcastPacket(new SocialAction(baium, 2));
			baium.broadcastPacket(new Earthquake(baium, 40, 10, true));
			
			// Second animation, waker sacrifice, followed by angels spawn, third animation and finally movement.
			startQuestTimer("baium_neck", baium, null, 13000);
			startQuestTimer("sacrifice_waker", baium, player, 24000);
			startQuestTimer("baium_roar", baium, null, 28000);
			startQuestTimer("baium_move", baium, null, 35000);
			
			// Delete the statue.
			npc.deleteMe();
		}
		return null;
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.disableCoreAi(true);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.isInvul())
			return null;
		
		if (attacker instanceof Playable)
		{
			// Curses
			if (attacker.testCursesOnAttack(npc))
				return null;
			
			// Refresh timer on every hit.
			_timeTracker = System.currentTimeMillis();
		}
		return super.onAttack(npc, attacker, damage, skill);
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		// Stop all tasks.
		cancelQuestTimers("baium_despawn");
		cancelQuestTimers("skill_range");
		cancelQuestTimers("angels_aggro_reconsider");
		
		npc.broadcastPacket(new PlaySound(1, "BS01_D", npc));
		
		// spawn the "Teleportation Cubic" for 15 minutes (to allow players to exit the lair)
		addSpawn(TELEPORTATION_CUBIC, TELEPORTATION_CUBIC_LOC, false, 900000, false);
		
		long respawnTime = (long) Config.SPAWN_INTERVAL_BAIUM + Rnd.get(-Config.RANDOM_SPAWN_TIME_BAIUM, Config.RANDOM_SPAWN_TIME_BAIUM);
		respawnTime *= 3600000;
		
		GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, DEAD);
		startQuestTimer("baium_unlock", null, null, respawnTime);
		
		StatSet info = GrandBossManager.getInstance().getStatSet(LIVE_BAIUM);
		info.set("respawn_time", System.currentTimeMillis() + respawnTime);
		GrandBossManager.getInstance().setStatSet(LIVE_BAIUM, info);
		
		// Unspawn angels.
		for (Npc minion : _minions)
		{
			minion.getSpawn().setRespawnState(false);
			minion.deleteMe();
		}
		_minions.clear();
		
		return super.onKill(npc, killer);
	}
	
	/**
	 * This method allows to select a random {@link Creature} target, and is used both for Baium and angels.
	 * @param npc : The {@link Npc} to check.
	 * @return A random {@link Creature} target.
	 */
	private Creature getRandomTarget(Npc npc)
	{
		final List<Creature> result = new ArrayList<>();
		
		for (Creature obj : BAIUM_LAIR.getKnownTypeInside(Creature.class))
		{
			if (obj instanceof Player)
			{
				if (obj.isDead() || !(GeoEngine.getInstance().canSeeTarget(npc, obj)))
					continue;
				
				if (((Player) obj).isGM() && !((Player) obj).getAppearance().isVisible())
					continue;
				
				if (npc.getNpcId() == ARCHANGEL && ((Player) obj).getActiveWeaponInstance() == null)
					continue;
				
				result.add(obj);
			}
			// Case of Archangels, they can hit Baium.
			else if (obj instanceof GrandBoss && npc.getNpcId() == ARCHANGEL)
				result.add(obj);
		}
		
		// If there's no players available, Baium and Angels are hitting each other.
		if (result.isEmpty() && npc.getNpcId() == LIVE_BAIUM)
		{
			for (Npc minion : _minions)
				result.add(minion);
		}
		
		return (result.isEmpty()) ? null : Rnd.get(result);
	}
	
	/**
	 * Pick a random skill id, based on multiple conditions.<br>
	 * <br>
	 * If Baium feels surrounded, he will use AoE skills. Same behavior if he is near 2+ angels.
	 * @param npc : The {@link Npc} to check.
	 * @return A usable skillId, 4127 by default.
	 */
	private static int getRandomSkill(Npc npc)
	{
		final double hpRatio = npc.getStatus().getHpRatio();
		
		// Baium's selfheal. It happens exceptionaly.
		if (hpRatio < 0.1 && Rnd.get(10000) == 0)
			return 4135;
		
		// Default attack if nothing is possible.
		int skill = 4127;
		
		final int chance = Rnd.get(100);
		
		// If Baium feels surrounded or see 2+ angels, he unleashes his wrath upon heads :).
		if (getPlayersCountInRadius(600, npc, false) >= 20 || npc.getKnownTypeInRadius(Monster.class, 600).size() >= 2)
		{
			if (chance < 25)
				skill = 4130;
			else if (chance < 50)
				skill = 4131;
			else if (chance < 75)
				skill = 4128;
			else
				skill = 4129;
		}
		else
		{
			if (hpRatio > 0.75)
			{
				if (chance < 10)
					skill = 4128;
				else if (chance < 20)
					skill = 4129;
			}
			else if (hpRatio > 0.5)
			{
				if (chance < 10)
					skill = 4131;
				else if (chance < 20)
					skill = 4128;
				else if (chance < 30)
					skill = 4129;
			}
			else
			{
				if (chance < 10)
					skill = 4130;
				else if (chance < 20)
					skill = 4131;
				else if (chance < 30)
					skill = 4128;
				else if (chance < 40)
					skill = 4129;
			}
		}
		return skill;
	}
}