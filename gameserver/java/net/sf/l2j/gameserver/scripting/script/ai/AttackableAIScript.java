package net.sf.l2j.gameserver.scripting.script.ai;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AttackableAIScript extends Quest
{
	private static final String ACTOR_INSTANCE_PACKAGE = "net.sf.l2j.gameserver.model.actor.instance.";
	
	/**
	 * Implicit constructor for generic AI script.<br>
	 * It is used by default for all {@link Attackable} instances.
	 */
	public AttackableAIScript()
	{
		super(-1, "ai");
		
		registerNpcs();
	}
	
	/**
	 * A superclass constructor for all inherited AI scripts.<br>
	 * Inherited AI provides special behavior for particular {@link Attackable} instances.
	 * @param descr : The path/package of the AI script.
	 */
	protected AttackableAIScript(String descr)
	{
		super(-1, descr);
		
		registerNpcs();
	}
	
	/**
	 * Registers this AI script to the {@link Attackable}'s {@link NpcTemplate} for various {@link ScriptEventType} events.<br>
	 * All inherited AI scripts must override this method and register only to related {@link NpcTemplate}s + {@link ScriptEventType}s.<br>
	 * Every overridden {@link ScriptEventType} replaces default {@link AttackableAIScript} with the new AI script.
	 */
	protected void registerNpcs()
	{
		// register all mobs here...
		for (final NpcTemplate template : NpcData.getInstance().getAllNpcs())
		{
			try
			{
				if (Attackable.class.isAssignableFrom(Class.forName(ACTOR_INSTANCE_PACKAGE + template.getType())))
				{
					template.addQuestEvent(ScriptEventType.ON_ATTACK, this);
					template.addQuestEvent(ScriptEventType.ON_KILL, this);
					template.addQuestEvent(ScriptEventType.ON_SPAWN, this);
					template.addQuestEvent(ScriptEventType.ON_SKILL_SEE, this);
					template.addQuestEvent(ScriptEventType.ON_FACTION_CALL, this);
					template.addQuestEvent(ScriptEventType.ON_AGGRO, this);
				}
			}
			catch (final ClassNotFoundException e)
			{
				LOGGER.error("An unknown template type {} has been found on {}.", e, template.getType(), toString());
			}
		}
	}
	
	@Override
	public String onSkillSee(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (caster == null)
			return null;
		
		if (!(npc instanceof Attackable))
			return null;
		
		final Attackable attackable = (Attackable) npc;
		int skillAggroPoints = skill.getAggroPoints();
		
		// Do not hate if the skill is a solo target skill, and if the target is player summon OR if the target is the npc and the skill was a positive effect.
		if (targets.length == 1 && ((caster.getSummon() != null && ArraysUtil.contains(targets, caster.getSummon())) || (!skill.isOffensive() && !skill.isDebuff() && ArraysUtil.contains(targets, npc))))
			skillAggroPoints = 0;
		
		if (skillAggroPoints > 0 && attackable.hasAI() && attackable.getAI().getCurrentIntention().getType() == IntentionType.ATTACK)
		{
			final WorldObject npcTarget = attackable.getTarget();
			for (Creature target : targets)
			{
				if (npcTarget == target || npc == target)
				{
					final Creature originalCaster = isPet ? caster.getSummon() : caster;
					attackable.getAggroList().addDamageHate(originalCaster, 0, (skillAggroPoints * 150) / (attackable.getStatus().getLevel() + 7));
				}
			}
		}
		return null;
	}
	
	@Override
	public String onFactionCall(Attackable caller, Attackable called, Creature target)
	{
		called.getAggroList().addDamageHate(target, 0, 1);
		return null;
	}
	
	@Override
	public String onAggro(Npc npc, Player player, boolean isPet)
	{
		((Attackable) npc).getAggroList().addDamageHate(isPet ? player.getSummon() : player, 0, 1);
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		((Attackable) npc).getAggroList().addDamageHate(attacker, damage, damage / (npc.getStatus().getLevel() + 7) * ((npc.isRaidRelated()) ? 20000 : 100));
		return null;
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		if (npc instanceof Monster)
		{
			final Monster monster = (Monster) npc;
			final Monster master = monster.getMaster();
			
			if (master != null)
				master.getMinionList().onMinionDie(monster, (master.isRaidBoss()) ? Config.RAID_MINION_RESPAWN_TIMER : (master.getSpawn().getRespawnDelay() * 1000 / 2));
			
			if (monster.hasMinions())
				monster.getMinionList().onMasterDie();
		}
		return null;
	}
	
	/**
	 * This method selects a random player.<br>
	 * Player can't be dead and isn't an hidden GM aswell.
	 * @param npc to check.
	 * @return the random player.
	 */
	public static Player getRandomPlayer(Npc npc)
	{
		final List<Player> result = new ArrayList<>();
		
		for (final Player player : npc.getKnownType(Player.class))
		{
			if (player.isDead())
				continue;
			
			if (player.isGM() && !player.getAppearance().isVisible())
				continue;
			
			result.add(player);
		}
		
		return (result.isEmpty()) ? null : Rnd.get(result);
	}
	
	/**
	 * Return the number of players in a defined radius.<br>
	 * Dead players aren't counted, invisible ones is the boolean parameter.
	 * @param range : the radius.
	 * @param npc : the object to make the test on.
	 * @param invisible : true counts invisible characters.
	 * @return the number of targets found.
	 */
	public static int getPlayersCountInRadius(int range, Creature npc, boolean invisible)
	{
		int count = 0;
		for (final Player player : npc.getKnownTypeInRadius(Player.class, range))
		{
			if (player.isDead())
				continue;
			
			if (!invisible && !player.getAppearance().isVisible())
				continue;
			
			count++;
		}
		return count;
	}
	
	/**
	 * Under that barbarian name, return the number of players in front, back and sides of the npc.<br>
	 * Dead players aren't counted, invisible ones is the boolean parameter.
	 * @param range : the radius.
	 * @param npc : the object to make the test on.
	 * @param invisible : true counts invisible characters.
	 * @return an array composed of front, back and side targets number.
	 */
	public static int[] getPlayersCountInPositions(int range, Creature npc, boolean invisible)
	{
		int frontCount = 0;
		int backCount = 0;
		int sideCount = 0;
		
		for (final Player player : npc.getKnownType(Player.class))
		{
			if (player.isDead())
				continue;
			
			if (!invisible && !player.getAppearance().isVisible())
				continue;
			
			if (!MathUtil.checkIfInRange(range, npc, player, true))
				continue;
			
			if (player.isInFrontOf(npc))
				frontCount++;
			else if (player.isBehind(npc))
				backCount++;
			else
				sideCount++;
		}
		
		return new int[]
		{
			frontCount,
			backCount,
			sideCount
		};
	}
}