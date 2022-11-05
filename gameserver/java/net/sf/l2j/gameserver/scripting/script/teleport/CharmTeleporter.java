package net.sf.l2j.gameserver.scripting.script.teleport;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;

public class CharmTeleporter extends Quest
{
	private static final int WHIRPY = 30540;
	private static final int TAMIL = 30576;
	
	private static final int ORC_GATEKEEPER_CHARM = 1658;
	private static final int DWARF_GATEKEEPER_TOKEN = 1659;
	
	public CharmTeleporter()
	{
		super(-1, "teleport");
		
		addTalkId(WHIRPY, TAMIL);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		
		int npcId = npc.getNpcId();
		if (npcId == WHIRPY)
		{
			if (player.getInventory().hasItems(DWARF_GATEKEEPER_TOKEN))
			{
				takeItems(player, DWARF_GATEKEEPER_TOKEN, 1);
				player.teleportTo(-80826, 149775, -3043, 0);
			}
			else
				htmltext = "30540-01.htm";
		}
		else if (npcId == TAMIL)
		{
			if (player.getInventory().hasItems(ORC_GATEKEEPER_CHARM))
			{
				takeItems(player, ORC_GATEKEEPER_CHARM, 1);
				player.teleportTo(-80826, 149775, -3043, 0);
			}
			else
				htmltext = "30576-01.htm";
		}
		
		return htmltext;
	}
}