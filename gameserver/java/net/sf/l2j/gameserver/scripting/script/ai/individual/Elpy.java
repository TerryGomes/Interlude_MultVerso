package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * A fleeing NPC.<br>
 * <br>
 * His behavior is to always flee, and never attack.
 */
public class Elpy extends AttackableAIScript
{
	public Elpy()
	{
		super("ai/individual");
	}
	
	@Override
	protected void registerNpcs()
	{
		addAttackId(20432);
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		npc.disableCoreAi(true);
		
		// Wait the NPC to be immobile to move him again.
		if (!npc.isMoving())
			npc.fleeFrom(attacker, Config.MAX_DRIFT_RANGE);
		
		return super.onAttack(npc, attacker, damage, skill);
	}
}