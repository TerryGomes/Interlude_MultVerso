package net.sf.l2j.gameserver.scripting.script.teleport;

import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;

public class PaganTeleporter extends Quest
{
	// Items
	private static final int VISITOR_MARK = 8064;
	private static final int PAGAN_MARK = 8067;
	
	public PaganTeleporter()
	{
		super(-1, "teleport");
		
		addTalkId(32034, 32035, 32036, 32037, 32039, 32040);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		
		switch (npc.getNpcId())
		{
			case 32034:
				if (player.getInventory().hasItems(VISITOR_MARK) || player.getInventory().hasItems(PAGAN_MARK))
				{
					DoorData.getInstance().getDoor(19160001).openMe();
					htmltext = "FadedMark.htm";
				}
				else
					htmltext = "32034-1.htm";
				break;
			
			case 32035:
				DoorData.getInstance().getDoor(19160001).openMe();
				htmltext = "FadedMark.htm";
				break;
			
			case 32036:
				if (!player.getInventory().hasItems(PAGAN_MARK))
					htmltext = "32036-1.htm";
				else
				{
					DoorData.getInstance().getDoor(19160010).openMe();
					DoorData.getInstance().getDoor(19160011).openMe();
					htmltext = "32036-2.htm";
				}
				break;
			
			case 32037:
				DoorData.getInstance().getDoor(19160010).openMe();
				DoorData.getInstance().getDoor(19160011).openMe();
				htmltext = "FadedMark.htm";
				break;
			
			case 32039:
				player.teleportTo(-12766, -35840, -10856, 0);
				break;
			
			case 32040:
				player.teleportTo(34962, -49758, -763, 0);
				break;
		}
		return htmltext;
	}
}