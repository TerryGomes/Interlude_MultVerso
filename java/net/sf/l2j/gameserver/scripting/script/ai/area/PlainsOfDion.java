package net.sf.l2j.gameserver.scripting.script.ai.area;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * TODO To fully review. AI for mobs in Plains of Dion (near Floran Village)
 */
public final class PlainsOfDion extends AttackableAIScript
{
	private static final int[] MONSTERS =
	{
		21104, // Delu Lizardman Supplier
		21105, // Delu Lizardman Special Agent
		21107, // Delu Lizardman Commander
	};

	private static final NpcStringId[] MONSTERS_MSG =
	{
		NpcStringId.ID_1000288,
		NpcStringId.ID_1000388,
		NpcStringId.ID_1000389,
		NpcStringId.ID_1000390,
		NpcStringId.ID_1000391
	};

	private static final NpcStringId[] MONSTERS_ASSIST_MSG =
	{
		NpcStringId.ID_1000392,
		NpcStringId.ID_1000393,
		NpcStringId.ID_1000394
	};

	public PlainsOfDion()
	{
		super("ai/area");
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(MONSTERS);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.isScriptValue(0))
		{
			npc.broadcastNpcSay(Rnd.get(MONSTERS_MSG), attacker.getName());

			for (Monster monster : npc.getKnownTypeInRadius(Monster.class, 600))
			{
				if (!monster.getAttack().isAttackingNow() && !monster.isDead() && ArraysUtil.contains(MONSTERS, monster.getNpcId()))
				{
					monster.forceAttack(attacker, 200);
					monster.broadcastNpcSay(Rnd.get(MONSTERS_ASSIST_MSG));
				}
			}
			npc.setScriptValue(1);
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
}