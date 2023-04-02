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

public class PolymorphingOnAttack extends AttackableAIScript
{
	private static final Map<Integer, List<Integer>> MOBSPAWNS = new HashMap<>();
	static
	{
		MOBSPAWNS.put(21258, Arrays.asList(21259, 100, 100, -1)); // Fallen Orc Shaman -> Sharp Talon Tiger (always polymorphs)
		MOBSPAWNS.put(21261, Arrays.asList(21262, 100, 20, 0)); // Ol Mahum Transcender 1st stage
		MOBSPAWNS.put(21262, Arrays.asList(21263, 100, 10, 1)); // Ol Mahum Transcender 2nd stage
		MOBSPAWNS.put(21263, Arrays.asList(21264, 100, 5, 2)); // Ol Mahum Transcender 3rd stage
		MOBSPAWNS.put(21265, Arrays.asList(21271, 100, 33, 0)); // Cave Ant Larva -> Cave Ant
		MOBSPAWNS.put(21266, Arrays.asList(21269, 100, 100, -1)); // Cave Ant Larva -> Cave Ant (always polymorphs)
		MOBSPAWNS.put(21267, Arrays.asList(21270, 100, 100, -1)); // Cave Ant Larva -> Cave Ant Soldier (always polymorphs)
		MOBSPAWNS.put(21271, Arrays.asList(21272, 66, 10, 1)); // Cave Ant -> Cave Ant Soldier
		MOBSPAWNS.put(21272, Arrays.asList(21273, 33, 5, 2)); // Cave Ant -> Cave Ant Soldier// Cave Ant Soldier -> Cave Noble Ant
		MOBSPAWNS.put(21521, Arrays.asList(21522, 100, 30, -1)); // Claws of Splendor
		MOBSPAWNS.put(21527, Arrays.asList(21528, 100, 30, -1)); // Anger of Splendor
		MOBSPAWNS.put(21533, Arrays.asList(21534, 100, 30, -1)); // Alliance of Splendor
		MOBSPAWNS.put(21537, Arrays.asList(21538, 100, 30, -1)); // Fang of Splendor
	}

	private static final NpcStringId[] MOBTEXTS =
	{
		NpcStringId.ID_1000407,
		NpcStringId.ID_1000408,
		NpcStringId.ID_1000406,
		NpcStringId.ID_1000411,
		NpcStringId.ID_1000410,
		NpcStringId.ID_1000409,
		NpcStringId.ID_1000414,
		NpcStringId.ID_1000413,
		NpcStringId.ID_1000412,
	};

	public PolymorphingOnAttack()
	{
		super("ai/group");
	}

	@Override
	protected void registerNpcs()
	{
		addEventIds(MOBSPAWNS.keySet(), EventHandler.ATTACKED);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.isVisible() && !npc.isDead())
		{
			final List<Integer> tmp = MOBSPAWNS.get(npc.getNpcId());
			if (tmp != null)
			{
				if (npc.getStatus().getHpRatio() * 100 <= tmp.get(1) && Rnd.get(100) < tmp.get(2))
				{
					if (tmp.get(3) >= 0)
					{
						npc.broadcastNpcSay(Rnd.get(MOBTEXTS));
					}

					npc.getSpawn().onDecay(npc);
					npc.deleteMe();

					final Npc newNpc = addSpawn(tmp.get(0), npc, false, 0, true);
					newNpc.forceAttack(attacker, 200);
				}
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
}