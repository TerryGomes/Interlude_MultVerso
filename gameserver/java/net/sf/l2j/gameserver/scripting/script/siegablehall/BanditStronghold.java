package net.sf.l2j.gameserver.scripting.script.siegablehall;

import java.util.HashMap;

import net.sf.l2j.gameserver.model.location.SpawnLocation;

/**
 * In order to participate in a battle to occupy the Brigand’s Hideaway clan hall, the head of a clan of level 4 or above must complete the Brigand’s Hideaway quest within a certain time before the clan hall battle is started. Only the first five clans that complete the hideaway quest first can
 * participate. The number of clan members that can participate per clan is limited to 18 people. The quest to participate in the Brigand’s Hideaway clan hall battle is only valid for participating in the clan hall battle on that date. Having completed the quest previously does not make it possible
 * to participate in the clan battle.<br>
 * <br>
 * Once the decision is made to participate in the clan battle, the clan leader must register the 18 clan members and select the NPC that he will protect by conversing with the herald NPC. Clans that currently occupy the clan hall must register the 18 clan members to participate in the defense.
 */
public final class BanditStronghold extends FlagWar
{
	public BanditStronghold()
	{
		super("siegablehall", BANDIT_STRONGHOLD);
	}

	@Override
	protected void registerNpcs()
	{
		ROYAL_FLAG = 35422;
		FLAG_RED = 35423;
		FLAG_YELLOW = 35424;
		FLAG_GREEN = 35425;
		FLAG_BLUE = 35426;
		FLAG_PURPLE = 35427;

		ALLY_1 = 35428;
		ALLY_2 = 35429;
		ALLY_3 = 35430;
		ALLY_4 = 35431;
		ALLY_5 = 35432;

		TELEPORT_1 = 35560;

		MESSENGER = 35437;

		OUTTER_DOORS_TO_OPEN = new int[2];
		OUTTER_DOORS_TO_OPEN[0] = 22170001;
		OUTTER_DOORS_TO_OPEN[1] = 22170002;

		INNER_DOORS_TO_OPEN = new int[2];
		INNER_DOORS_TO_OPEN[0] = 22170003;
		INNER_DOORS_TO_OPEN[1] = 22170004;

		FLAG_COORDS = new SpawnLocation[7];
		FLAG_COORDS[0] = new SpawnLocation(83699, -17468, -1774, 19048);
		FLAG_COORDS[1] = new SpawnLocation(82053, -17060, -1784, 5432);
		FLAG_COORDS[2] = new SpawnLocation(82142, -15528, -1799, 58792);
		FLAG_COORDS[3] = new SpawnLocation(83544, -15266, -1770, 44976);
		FLAG_COORDS[4] = new SpawnLocation(84609, -16041, -1769, 35816);
		FLAG_COORDS[5] = new SpawnLocation(81981, -15708, -1858, 60392);
		FLAG_COORDS[6] = new SpawnLocation(84375, -17060, -1860, 27712);

		QUEST_REWARD = 5009;
		CENTER = new SpawnLocation(82882, -16280, -1894, 0);

		_data = new HashMap<>(6);

		super.registerNpcs();
	}

	@Override
	public String getFlagHtml(int flag)
	{
		switch (flag)
		{
			case 35423:
				return "messenger_flag1.htm";

			case 35424:
				return "messenger_flag2.htm";

			case 35425:
				return "messenger_flag3.htm";

			case 35426:
				return "messenger_flag4.htm";

			case 35427:
				return "messenger_flag5.htm";
		}
		return null;
	}

	@Override
	public String getAllyHtml(int ally)
	{
		switch (ally)
		{
			case 35428:
				return "messenger_ally1result.htm";

			case 35429:
				return "messenger_ally2result.htm";

			case 35430:
				return "messenger_ally3result.htm";

			case 35431:
				return "messenger_ally4result.htm";

			case 35432:
				return "messenger_ally5result.htm";
		}
		return null;
	}

	@Override
	public void spawnNpcs()
	{
	}

	@Override
	public void unspawnNpcs()
	{
	}
}