package net.sf.l2j.gameserver.scripting.script.siegablehall;

import java.util.HashMap;

import net.sf.l2j.gameserver.model.location.SpawnLocation;

public final class WildBeastReserve extends FlagWar
{
	public WildBeastReserve()
	{
		super("siegablehall", BEAST_FARM);
	}

	@Override
	protected void registerNpcs()
	{
		ROYAL_FLAG = 35606;
		FLAG_RED = 35607; // White flag
		FLAG_YELLOW = 35608; // Red flag
		FLAG_GREEN = 35609; // Blue flag
		FLAG_BLUE = 35610; // Green flag
		FLAG_PURPLE = 35611; // Black flag

		ALLY_1 = 35618;
		ALLY_2 = 35619;
		ALLY_3 = 35620;
		ALLY_4 = 35621;
		ALLY_5 = 35622;

		TELEPORT_1 = 35612;

		MESSENGER = 35627;

		FLAG_COORDS = new SpawnLocation[7];
		FLAG_COORDS[0] = new SpawnLocation(56963, -92211, -1303, 60611);
		FLAG_COORDS[1] = new SpawnLocation(58090, -91641, -1303, 47274);
		FLAG_COORDS[2] = new SpawnLocation(58908, -92556, -1303, 34450);
		FLAG_COORDS[3] = new SpawnLocation(58336, -93600, -1303, 21100);
		FLAG_COORDS[4] = new SpawnLocation(57152, -93360, -1303, 8400);
		FLAG_COORDS[5] = new SpawnLocation(59116, -93251, -1302, 31000);
		FLAG_COORDS[6] = new SpawnLocation(56432, -92864, -1303, 64000);

		OUTTER_DOORS_TO_OPEN = new int[2];
		OUTTER_DOORS_TO_OPEN[0] = 21150003;
		OUTTER_DOORS_TO_OPEN[1] = 21150004;

		INNER_DOORS_TO_OPEN = new int[2];
		INNER_DOORS_TO_OPEN[0] = 21150001;
		INNER_DOORS_TO_OPEN[1] = 21150002;

		QUEST_REWARD = 0;
		CENTER = new SpawnLocation(57762, -92696, -1359, 0);

		_data = new HashMap<>(6);

		super.registerNpcs();
	}

	@Override
	public String getFlagHtml(int flag)
	{
		switch (flag)
		{
			case 35607:
				return "messenger_flag1.htm";

			case 35608:
				return "messenger_flag2.htm";

			case 35609:
				return "messenger_flag3.htm";

			case 35610:
				return "messenger_flag4.htm";

			case 35611:
				return "messenger_flag5.htm";
		}
		return null;
	}

	@Override
	public String getAllyHtml(int ally)
	{
		switch (ally)
		{
			case 35618:
				return "messenger_ally1result.htm";

			case 35619:
				return "messenger_ally2result.htm";

			case 35620:
				return "messenger_ally3result.htm";

			case 35621:
				return "messenger_ally4result.htm";

			case 35622:
				return "messenger_ally5result.htm";
		}
		return null;
	}

	@Override
	public boolean canPayRegistration()
	{
		return false;
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