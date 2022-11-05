package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Antharas is one of the great dragons in the history of Aden. This dragon is by far the most fearsome creature in the Lineage II world. The earth tyrant Antharas, also known as Giran's Disaster, has finally awakened from a long, deep slumber.<br>
 * <br>
 * <b>Conditions for Meeting Antharas</b><br>
 * <br>
 * <ul>
 * <li>To enter the lair of Antharas, a portal stone must be obtained, which is only possible for characters level fifty or above.</li>
 * <li>To obtain a portal stone, first seek Gabriel and fulfill her tasks and those of the other seal guardians. With the portal stone, it is possible to cross the barrier surrounding Antharas'lair.</li>
 * <li>The entrance to the lair of Antharas is located in an underground floor of a dungeon deep in Dragon Valley.</li>
 * </ul>
 * <b>Attacking Antharas</b><br>
 * <ul>
 * <li>Characters who have managed to gain entrance to his lair will be overwhelmed, as the ancient and incredibly powerful Antharas dramatically emerges.</li>
 * <li>Once inside, players will no longer be able to enter or leave, until the great dragon falls or their forces are completely wiped out.</li>
 * <li>Be forewarned! Doing battle with Antharas is the penultimate challenge, requiring many sacrifices, intense coordination and the gathering of the mightiest forces of Aden. Only those willing to struggle against incredible odds for many hours have even the slightest chance for victory.</li>
 * </ul>
 */
public class Antharas extends AttackableAIScript
{
	private static final BossZone ANTHARAS_LAIR = ZoneManager.getInstance().getZoneById(110001, BossZone.class);
	
	private static final int[] ANTHARAS_IDS =
	{
		29066,
		29067,
		29068
	};
	
	public static final int ANTHARAS = 29019; // Dummy Antharas id used for status updates only.
	
	public static final byte DORMANT = 0; // No one has entered yet. Entry is unlocked.
	public static final byte WAITING = 1; // Someone has entered, triggering a 30 minute window for additional people to enter. Entry is unlocked.
	public static final byte FIGHTING = 2; // Antharas is engaged in battle, annihilating his foes. Entry is locked.
	public static final byte DEAD = 3; // Antharas has been killed. Entry is locked.
	
	private final Set<Npc> _minions = ConcurrentHashMap.newKeySet();
	
	private long _timeTracker = 0;
	private Player _actualVictim;
	
	// Values set based on Antharas strength.
	private int _antharasId;
	private L2Skill _skillRegen;
	private int _minionTimer;
	
	public Antharas()
	{
		super("ai/boss");
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(ANTHARAS);
		
		switch (GrandBossManager.getInstance().getBossStatus(ANTHARAS))
		{
			case DEAD: // Launch the timer to set DORMANT, or set DORMANT directly if timer expired while offline.
				long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
				if (temp > 0)
					startQuestTimer("antharas_unlock", null, null, temp);
				else
					GrandBossManager.getInstance().setBossStatus(ANTHARAS, DORMANT);
				break;
			
			case WAITING: // Launch beginning timer.
				startQuestTimer("beginning", null, null, Config.WAIT_TIME_ANTHARAS);
				break;
			
			case FIGHTING:
				final int loc_x = info.getInteger("loc_x");
				final int loc_y = info.getInteger("loc_y");
				final int loc_z = info.getInteger("loc_z");
				final int heading = info.getInteger("heading");
				final int hp = info.getInteger("currentHP");
				final int mp = info.getInteger("currentMP");
				
				// Update Antharas informations.
				updateAntharas();
				
				final Npc antharas = addSpawn(_antharasId, loc_x, loc_y, loc_z, heading, false, 0, false);
				GrandBossManager.getInstance().addBoss(ANTHARAS, (GrandBoss) antharas);
				
				antharas.getStatus().setHpMp(hp, mp);
				antharas.forceRunStance();
				
				// stores current time for inactivity task.
				_timeTracker = System.currentTimeMillis();
				
				startQuestTimerAtFixedRate("regen_task", antharas, null, 60000);
				startQuestTimerAtFixedRate("skill_task", antharas, null, 2000);
				startQuestTimerAtFixedRate("minions_spawn", antharas, null, _minionTimer);
				break;
		}
	}
	
	@Override
	protected void registerNpcs()
	{
		addEventIds(ANTHARAS_IDS, ScriptEventType.ON_ATTACK, ScriptEventType.ON_SPAWN);
		addKillId(29066, 29067, 29068, 29069, 29070, 29071, 29072, 29073, 29074, 29075, 29076);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		// Regeneration && inactivity task
		if (name.equalsIgnoreCase("regen_task"))
		{
			// Inactivity task - 30min
			if (_timeTracker + 1800000 < System.currentTimeMillis())
			{
				// Set it dormant.
				GrandBossManager.getInstance().setBossStatus(ANTHARAS, DORMANT);
				
				// Drop all players from the zone.
				ANTHARAS_LAIR.oustAllPlayers();
				
				// Drop tasks.
				dropTimers(npc);
				
				// Delete current instance of Antharas.
				npc.deleteMe();
				return null;
			}
			_skillRegen.getEffects(npc, npc);
		}
		// Spawn cinematic, regen_task and choose of skill.
		else if (name.equalsIgnoreCase("spawn_1"))
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 700, 13, -19, 0, 20000, 0, 0, 1, 0));
		else if (name.equalsIgnoreCase("spawn_2"))
		{
			npc.broadcastPacket(new SocialAction(npc, 1));
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 700, 13, 0, 6000, 20000, 0, 0, 1, 0));
		}
		else if (name.equalsIgnoreCase("spawn_3"))
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 3700, 0, -3, 0, 10000, 0, 0, 1, 0));
		else if (name.equalsIgnoreCase("spawn_4"))
		{
			npc.broadcastPacket(new SocialAction(npc, 2));
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 0, -3, 22000, 30000, 0, 0, 1, 0));
		}
		else if (name.equalsIgnoreCase("spawn_5"))
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 0, -3, 300, 7000, 0, 0, 1, 0));
		else if (name.equalsIgnoreCase("spawn_6"))
		{
			// stores current time for inactivity task.
			_timeTracker = System.currentTimeMillis();
			
			GrandBossManager.getInstance().setBossStatus(ANTHARAS, FIGHTING);
			npc.setInvul(false);
			npc.forceRunStance();
			
			startQuestTimerAtFixedRate("regen_task", npc, null, 60000);
			startQuestTimerAtFixedRate("skill_task", npc, null, 2000);
			startQuestTimerAtFixedRate("minions_spawn", npc, null, _minionTimer);
		}
		else if (name.equalsIgnoreCase("skill_task"))
			callSkillAI(npc);
		else if (name.equalsIgnoreCase("minions_spawn"))
		{
			boolean isBehemoth = Rnd.get(100) < 60;
			int mobNumber = isBehemoth ? 2 : 3;
			
			// Set spawn.
			for (int i = 0; i < mobNumber; i++)
			{
				if (_minions.size() > 9)
					break;
				
				final int npcId = isBehemoth ? 29069 : Rnd.get(29070, 29076);
				final Npc dragon = addSpawn(npcId, npc.getX() + Rnd.get(-200, 200), npc.getY() + Rnd.get(-200, 200), npc.getZ(), 0, false, 0, true);
				((Monster) dragon).setMinion(true);
				
				_minions.add(dragon);
				
				final Player victim = getRandomPlayer(dragon);
				if (victim != null)
					dragon.forceAttack(victim, 200);
				
				if (!isBehemoth)
					startQuestTimer("self_destruct", dragon, null, (_minionTimer / 3));
			}
		}
		else if (name.equalsIgnoreCase("self_destruct"))
		{
			L2Skill skill;
			switch (npc.getNpcId())
			{
				case 29070:
				case 29071:
				case 29072:
				case 29073:
				case 29074:
				case 29075:
					skill = SkillTable.getInstance().getInfo(5097, 1);
					break;
				default:
					skill = SkillTable.getInstance().getInfo(5094, 1);
			}
			npc.getAI().tryToCast(npc, skill);
		}
		// Cinematic
		else if (name.equalsIgnoreCase("beginning"))
		{
			updateAntharas();
			
			final Npc antharas = addSpawn(_antharasId, 181323, 114850, -7623, 32542, false, 0, false);
			GrandBossManager.getInstance().addBoss(ANTHARAS, (GrandBoss) antharas);
			antharas.setInvul(true);
			
			// Earthquake.
			antharas.broadcastPacket(new Earthquake(antharas, 20, 10, true));
			
			// Launch the cinematic, and tasks (regen + skill).
			startQuestTimer("spawn_1", antharas, null, 16);
			startQuestTimer("spawn_2", antharas, null, 3016);
			startQuestTimer("spawn_3", antharas, null, 13016);
			startQuestTimer("spawn_4", antharas, null, 13216);
			startQuestTimer("spawn_5", antharas, null, 24016);
			startQuestTimer("spawn_6", antharas, null, 25916);
		}
		// spawn of Teleport Cube.
		else if (name.equalsIgnoreCase("die_1"))
		{
			addSpawn(31859, 177615, 114941, -7709, 0, false, 900000, false);
			startQuestTimer("remove_players", null, null, 900000);
		}
		else if (name.equalsIgnoreCase("antharas_unlock"))
			GrandBossManager.getInstance().setBossStatus(ANTHARAS, DORMANT);
		else if (name.equalsIgnoreCase("remove_players"))
			ANTHARAS_LAIR.oustAllPlayers();
		
		return super.onTimer(name, npc, player);
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
		if (npc.getNpcId() == _antharasId)
		{
			// Drop tasks.
			dropTimers(npc);
			
			// Launch death animation.
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1200, 20, -10, 10000, 13000, 0, 0, 0, 0));
			ANTHARAS_LAIR.broadcastPacket(new PlaySound(1, "BS01_D", npc));
			startQuestTimer("die_1", null, null, 8000);
			
			GrandBossManager.getInstance().setBossStatus(ANTHARAS, DEAD);
			
			long respawnTime = (long) Config.SPAWN_INTERVAL_ANTHARAS + Rnd.get(-Config.RANDOM_SPAWN_TIME_ANTHARAS, Config.RANDOM_SPAWN_TIME_ANTHARAS);
			respawnTime *= 3600000;
			
			startQuestTimer("antharas_unlock", null, null, respawnTime);
			
			StatSet info = GrandBossManager.getInstance().getStatSet(ANTHARAS);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(ANTHARAS, info);
		}
		else
		{
			cancelQuestTimers("self_destruct", npc);
			_minions.remove(npc);
		}
		
		return super.onKill(npc, killer);
	}
	
	private void callSkillAI(Npc npc)
	{
		if (npc.isInvul())
			return;
		
		// Pickup a target if no or dead victim. 10% luck he decides to reconsiders his target.
		if (_actualVictim == null || _actualVictim.isDead() || !npc.knows(_actualVictim) || Rnd.get(10) == 0)
			_actualVictim = getRandomPlayer(npc);
		
		// If result is still null, Antharas will roam. Don't go deeper in skill AI.
		if (_actualVictim == null)
		{
			if (Rnd.get(10) == 0)
				npc.moveUsingRandomOffset(1400);
			
			return;
		}
		
		npc.getAI().tryToCast(_actualVictim, getRandomSkill(npc), false, false, 0);
	}
	
	/**
	 * Pick a random skill.<br>
	 * The use is based on current HPs ratio.
	 * @param npc Antharas
	 * @return a usable skillId
	 */
	private static L2Skill getRandomSkill(Npc npc)
	{
		final double hpRatio = npc.getStatus().getHpRatio();
		
		// Find enemies surrounding Antharas.
		final int[] playersAround = getPlayersCountInPositions(1100, npc, false);
		
		if (hpRatio < 0.25)
		{
			if (Rnd.get(100) < 30)
				return FrequentSkill.ANTHARAS_MOUTH.getSkill();
			
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (playersAround[0] >= 10)
			{
				if (Rnd.get(100) < 40)
					return FrequentSkill.ANTHARAS_DEBUFF.getSkill();
				
				if (Rnd.get(100) < 10)
					return FrequentSkill.ANTHARAS_JUMP.getSkill();
			}
			
			if (Rnd.get(100) < 10)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		else if (hpRatio < 0.5)
		{
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (playersAround[0] >= 10)
			{
				if (Rnd.get(100) < 40)
					return FrequentSkill.ANTHARAS_DEBUFF.getSkill();
				
				if (Rnd.get(100) < 10)
					return FrequentSkill.ANTHARAS_JUMP.getSkill();
			}
			
			if (Rnd.get(100) < 7)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		else if (hpRatio < 0.75)
		{
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (playersAround[0] >= 10 && Rnd.get(100) < 10)
				return FrequentSkill.ANTHARAS_JUMP.getSkill();
			
			if (Rnd.get(100) < 5)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		else
		{
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (Rnd.get(100) < 3)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		
		if (Rnd.get(100) < 6)
			return FrequentSkill.ANTHARAS_BREATH.getSkill();
		
		if (Rnd.get(100) < 50)
			return FrequentSkill.ANTHARAS_NORMAL_ATTACK.getSkill();
		
		if (Rnd.get(100) < 5)
		{
			if (Rnd.get(100) < 50)
				return FrequentSkill.ANTHARAS_FEAR.getSkill();
			
			return FrequentSkill.ANTHARAS_SHORT_FEAR.getSkill();
		}
		
		return FrequentSkill.ANTHARAS_NORMAL_ATTACK_EX.getSkill();
	}
	
	/**
	 * Update Antharas informations depending about how much players joined the fight.<br>
	 * Used when server restarted and Antharas is fighting, or used while the cinematic occurs (after the 30min timer).
	 */
	private void updateAntharas()
	{
		final int playersNumber = ANTHARAS_LAIR.getAllowedPlayers().size();
		if (playersNumber < 45)
		{
			_antharasId = ANTHARAS_IDS[0];
			_skillRegen = SkillTable.getInstance().getInfo(4239, 1);
			_minionTimer = 180000;
		}
		else if (playersNumber < 63)
		{
			_antharasId = ANTHARAS_IDS[1];
			_skillRegen = SkillTable.getInstance().getInfo(4240, 1);
			_minionTimer = 150000;
		}
		else
		{
			_antharasId = ANTHARAS_IDS[2];
			_skillRegen = SkillTable.getInstance().getInfo(4241, 1);
			_minionTimer = 120000;
		}
	}
	
	/**
	 * Drop timers, meaning Antharas is dead or inactivity task occured.
	 * @param npc : The NPC to affect.
	 */
	private void dropTimers(Npc npc)
	{
		cancelQuestTimers("regen_task", npc);
		cancelQuestTimers("skill_task", npc);
		cancelQuestTimers("minions_spawn", npc);
		
		cancelQuestTimers("self_destruct");
		
		_minions.forEach(Npc::deleteMe);
		_minions.clear();
	}
}