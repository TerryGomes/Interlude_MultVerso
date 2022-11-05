package net.sf.l2j.gameserver.scripting.script.feature;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.scripting.Quest;

public class MissQueen extends Quest
{
	// Rewards
	private static final int TRAINEES_COUPON = 7832;
	private static final int TRAVELERS_COUPON = 7833;
	
	// Miss Queen locations
	private static final SpawnLocation[] LOCATIONS =
	{
		new SpawnLocation(116224, -181728, -1378, 0),
		new SpawnLocation(114885, -178092, -832, 0),
		new SpawnLocation(45472, 49312, -3072, 53000),
		new SpawnLocation(47648, 51296, -2994, 38500),
		new SpawnLocation(11340, 15972, -4582, 14000),
		new SpawnLocation(10968, 17540, -4572, 55000),
		new SpawnLocation(-14048, 123184, -3120, 32000),
		new SpawnLocation(-44979, -113508, -199, 32000),
		new SpawnLocation(-84119, 243254, -3730, 8000),
		new SpawnLocation(-84356, 242176, -3730, 24500),
		new SpawnLocation(-82032, 150160, -3127, 16500)
	};
	
	public MissQueen()
	{
		super(-1, "feature");
		
		// Spawn the 11 NPCs.
		for (SpawnLocation loc : LOCATIONS)
			addSpawn(31760, loc, false, 0, false);
		
		addTalkId(31760);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		if (event.equalsIgnoreCase("newbie_coupon"))
		{
			if (player.getClassId().getLevel() == 0 && player.getStatus().getLevel() >= 6 && player.getStatus().getLevel() <= 25 && player.getPkKills() <= 0)
			{
				if (player.getMemos().containsKey("MissQueen_Trainees"))
					htmltext = "31760-01.htm";
				else
				{
					htmltext = "31760-02.htm";
					player.getMemos().set("MissQueen_Trainees", true);
					giveItems(player, TRAINEES_COUPON, 1);
				}
			}
			else
				htmltext = "31760-03.htm";
		}
		else if (event.equalsIgnoreCase("traveller_coupon"))
		{
			if (player.getClassId().getLevel() == 1 && player.getStatus().getLevel() >= 6 && player.getStatus().getLevel() <= 25 && player.getPkKills() <= 0)
			{
				if (player.getMemos().containsKey("MissQueen_Traveler"))
					htmltext = "31760-04.htm";
				else
				{
					htmltext = "31760-05.htm";
					player.getMemos().set("MissQueen_Traveler", true);
					giveItems(player, TRAVELERS_COUPON, 1);
				}
			}
			else
				htmltext = "31760-06.htm";
		}
		
		return htmltext;
	}
}