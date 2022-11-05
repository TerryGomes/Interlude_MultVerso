package net.sf.l2j.gameserver.scripting.script.ai.area;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class HotSprings extends AttackableAIScript
{
	private static final Map<Integer, Integer> MONSTERS_DISEASES = new HashMap<>(6);
	
	static
	{
		MONSTERS_DISEASES.put(21314, 4551);
		MONSTERS_DISEASES.put(21316, 4552);
		MONSTERS_DISEASES.put(21317, 4553);
		MONSTERS_DISEASES.put(21319, 4552);
		MONSTERS_DISEASES.put(21321, 4551);
		MONSTERS_DISEASES.put(21322, 4553);
	}
	
	public HotSprings()
	{
		super("ai/area");
	}
	
	@Override
	protected void registerNpcs()
	{
		addEventIds(MONSTERS_DISEASES.keySet(), ScriptEventType.ON_ATTACK_ACT, ScriptEventType.ON_FACTION_CALL, ScriptEventType.ON_SKILL_SEE);
	}
	
	@Override
	public String onAttackAct(Npc npc, Player victim)
	{
		// Try to apply Malaria.
		tryToApplyEffect(npc, victim, 4554);
		
		// Try to apply another disease, based on npcId.
		tryToApplyEffect(npc, victim, MONSTERS_DISEASES.get(npc.getNpcId()));
		
		return super.onAttackAct(npc, victim);
	}
	
	@Override
	public String onFactionCall(Attackable caller, Attackable called, Creature target)
	{
		final Player player = target.getActingPlayer();
		if (player != null)
		{
			// Try to apply Malaria.
			tryToApplyEffect(called, player, 4554);
			
			// Try to apply another disease, based on npcId.
			tryToApplyEffect(called, player, MONSTERS_DISEASES.get(called.getNpcId()));
		}
		return super.onFactionCall(caller, called, target);
	}
	
	@Override
	public String onSkillSee(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		// Try to apply Malaria.
		tryToApplyEffect(npc, caster, 4554);
		
		// Try to apply another disease, based on npcId.
		tryToApplyEffect(npc, caster, MONSTERS_DISEASES.get(npc.getNpcId()));
		
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
	
	/**
	 * Try to apply a disease with a 10% luck.
	 * @param npc : The {@link Npc} used as caster.
	 * @param player : The {@link Player} used as target.
	 * @param skillId : The id of the {@link L2Skill} to launch.
	 */
	private static void tryToApplyEffect(Npc npc, Player player, int skillId)
	{
		if (Rnd.get(100) < 10)
		{
			int level = 1;
			
			for (AbstractEffect effect : player.getAllEffects())
			{
				if (effect.getSkill().getId() != skillId)
					continue;
				
				// Calculate the new level skill to apply.
				level = Math.min(10, effect.getSkill().getLevel() + 1);
				
				// Exit the previous effect.
				effect.exit();
				break;
			}
			
			// Apply new effect.
			SkillTable.getInstance().getInfo(skillId, level).getEffects(npc, player);
		}
	}
}