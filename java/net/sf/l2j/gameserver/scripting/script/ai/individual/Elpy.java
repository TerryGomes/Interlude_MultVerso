package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.EventHandler;
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
		addEventIds(20432, EventHandler.ATTACKED, EventHandler.CREATED);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		// Wait the NPC to be immobile to move him again.
		if (!npc.isMoving())
		{
			npc.fleeFrom(attacker, Config.MAX_DRIFT_RANGE);
		}

		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onCreated(Npc npc)
	{
		npc.disableCoreAi(true);

		super.onCreated(npc);
	}
}