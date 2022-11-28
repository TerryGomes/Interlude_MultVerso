package net.sf.l2j.gameserver.scripting.script.ai.group;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Summon minions the first time being hitten.<br>
 * For Orcs case, send also a message.
 */
public class SummonMinions extends AttackableAIScript
{
	private static final NpcStringId[] ORCS_WORDS =
	{
		NpcStringId.ID_1000404,
		NpcStringId.ID_1000405,
		NpcStringId.ID_1000403,
		NpcStringId.ID_1000294,
	};

	private static final Map<Integer, List<Integer>> MINIONS = new HashMap<>();

	static
	{
		MINIONS.put(20767, Arrays.asList(20768, 20769, 20770)); // Timak Orc Troop
		MINIONS.put(21524, Arrays.asList(21525)); // Blade of Splendor
		MINIONS.put(21531, Arrays.asList(21658)); // Punishment of Splendor
		MINIONS.put(21539, Arrays.asList(21540)); // Wailing of Splendor
	}

	public SummonMinions()
	{
		super("ai/group");
	}

	@Override
	protected void registerNpcs()
	{
		addEventIds(MINIONS.keySet(), EventHandler.ATTACKED);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.isScriptValue(0))
		{
			final int npcId = npc.getNpcId();
			if (npcId != 20767)
			{
				for (int val : MINIONS.get(npcId))
				{
					final Npc newNpc = addSpawn(val, npc, true, 0, false);
					newNpc.forceAttack(attacker, 200);
				}
			}
			else
			{
				for (int val : MINIONS.get(npcId))
				{
					addSpawn(val, npc, true, 0, false);
				}

				npc.broadcastNpcSay(Rnd.get(ORCS_WORDS));
			}
			npc.setScriptValue(1);
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
}