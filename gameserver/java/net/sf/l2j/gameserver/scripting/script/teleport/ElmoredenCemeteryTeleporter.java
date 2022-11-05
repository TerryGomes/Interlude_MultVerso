package net.sf.l2j.gameserver.scripting.script.teleport;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.scripting.Quest;

public class ElmoredenCemeteryTeleporter extends Quest
{
	// Items
	private static final int USED_GRAVE_PASS = 7261;
	private static final int ANTIQUE_BROOCH = 7262;
	
	// Locations
	private static final Location FOUR_SEPULCHERS_LOC = new Location(178127, -84435, -7215);
	private static final Location IMPERIAL_TOMB_LOC = new Location(186699, -75915, -2826);
	
	public ElmoredenCemeteryTeleporter()
	{
		super(-1, "teleport");
		
		addTalkId(31919, 31920);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		
		if (event.equalsIgnoreCase("4S"))
		{
			if (player.getInventory().hasItems(ANTIQUE_BROOCH))
			{
				player.teleportTo(FOUR_SEPULCHERS_LOC, 0);
			}
			else if (player.getInventory().hasItems(USED_GRAVE_PASS))
			{
				takeItems(player, USED_GRAVE_PASS, 1);
				player.teleportTo(FOUR_SEPULCHERS_LOC, 0);
			}
			else
				htmltext = npc.getNpcId() + "-1.htm";
		}
		else if (event.equalsIgnoreCase("IT"))
		{
			if (player.getInventory().hasItems(ANTIQUE_BROOCH))
			{
				player.teleportTo(IMPERIAL_TOMB_LOC, 0);
			}
			else if (player.getInventory().hasItems(USED_GRAVE_PASS))
			{
				takeItems(player, USED_GRAVE_PASS, 1);
				player.teleportTo(IMPERIAL_TOMB_LOC, 0);
			}
			else
				htmltext = npc.getNpcId() + "-1.htm";
		}
		
		return htmltext;
	}
}