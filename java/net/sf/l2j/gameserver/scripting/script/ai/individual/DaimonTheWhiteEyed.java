package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.ClassType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;

/**
 * This Npc buffs or debuffs (with an equal 50% chance) the {@link Player} upon interaction, based on {@link Player}'s {@link ClassType}.<br>
 * <br>
 * The 30 seconds timer only affect chat ability.
 */
public class DaimonTheWhiteEyed extends AttackableAIScript
{
	private static final NpcStringId[] DEBUFF_CHAT =
	{
		NpcStringId.ID_1000458,
		NpcStringId.ID_1000459,
		NpcStringId.ID_1000460
	};

	private static final NpcStringId[] BUFF_CHAT =
	{
		NpcStringId.ID_1000461,
		NpcStringId.ID_1000462,
		NpcStringId.ID_1000463
	};

	public DaimonTheWhiteEyed()
	{
		super("ai/individual");
	}

	@Override
	protected void registerNpcs()
	{
		addFirstTalkId(31705);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("6543"))
		{
			npc.setScriptValue(0);
		}

		return super.onTimer(name, npc, player);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (Rnd.nextBoolean())
		{
			if (npc.isScriptValue(0))
			{
				npc.broadcastNpcSay(Rnd.get(DEBUFF_CHAT));
				npc.setScriptValue(1);

				startQuestTimer("6543", npc, null, 30000);
			}

			if (player.getClassId().getType() == ClassType.FIGHTER)
			{
				npc.getAI().tryToCast(player, 1206, 19);
			}
			else
			{
				npc.getAI().tryToCast(player, 1083, 17);
			}
		}
		else
		{
			if (npc.isScriptValue(0))
			{
				npc.broadcastNpcSay(Rnd.get(BUFF_CHAT));
				npc.setScriptValue(1);

				startQuestTimer("6543", npc, null, 30000);
			}

			if (player.getClassId().getType() == ClassType.FIGHTER)
			{
				npc.getAI().tryToCast(player, 1086, 2);
			}
			else
			{
				npc.getAI().tryToCast(player, 1059, 3);
			}
		}
		return null;
	}
}