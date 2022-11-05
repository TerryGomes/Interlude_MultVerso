package net.sf.l2j.gameserver.scripting.script.teleport;

import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.enums.TeleportType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;

public class NoblesseTeleporter extends Quest
{
	public NoblesseTeleporter()
	{
		super(-1, "teleport");
		
		addTalkId(30006, 30059, 30080, 30134, 30146, 30177, 30233, 30256, 30320, 30540, 30576, 30836, 30848, 30878, 30899, 31275, 31320, 31964);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		TeleportData.getInstance().showTeleportList(player, npc, TeleportType.valueOf(event.toUpperCase()));
		
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		return (player.isNoble()) ? "noble.htm" : "nobleteleporter-no.htm";
	}
}