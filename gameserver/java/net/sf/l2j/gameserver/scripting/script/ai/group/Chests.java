package net.sf.l2j.gameserver.scripting.script.ai.group;

import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Chests extends AttackableAIScript
{
	private static final int[] NPC_IDS =
	{
		18265,
		18266,
		18267,
		18268,
		18269,
		18270,
		18271,
		18272,
		18273,
		18274,
		18275,
		18276,
		18277,
		18278,
		18279,
		18280,
		18281,
		18282,
		18283,
		18284,
		18285,
		18286,
		18287,
		18288,
		18289,
		18290,
		18291,
		18292,
		18293,
		18294,
		18295,
		18296,
		18297,
		18298,
	};

	public Chests()
	{
		super("ai/group");
	}

	@Override
	protected void registerNpcs()
	{
		addEventIds(NPC_IDS, EventHandler.ATTACKED, EventHandler.SEE_SPELL);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc instanceof Chest)
		{
			testAndExplode((Chest) npc);
			return;
		}
		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (npc instanceof Chest)
		{
			// This behavior is only run when the target of skill is the passed npc.
			if (!ArraysUtil.contains(targets, npc))
			{
				super.onSeeSpell(npc, caster, skill, targets, isPet);
				return;
			}

			testAndExplode((Chest) npc);
		}
		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}

	private static void testAndExplode(Chest chest)
	{
		// If this chest has already been interacted, no further AI decisions are needed.
		if (chest.isInteracted())
		{
			return;
		}

		chest.setInteracted();
		chest.getAI().tryToCast(chest, 4143, Math.min(10, Math.round(chest.getStatus().getLevel() / 10)));
	}
}