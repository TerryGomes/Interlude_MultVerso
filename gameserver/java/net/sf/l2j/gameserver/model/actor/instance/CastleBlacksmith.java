package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.NpcTalkCond;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class CastleBlacksmith extends Folk
{
	public CastleBlacksmith(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!Config.ALLOW_MANOR)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/npcdefault.htm");
			html.replace("%objectId%", getObjectId());
			html.replace("%npcname%", getName());
			player.sendPacket(html);
			return;
		}
		
		if (getNpcTalkCond(player) != NpcTalkCond.OWNER)
			return;
		
		if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			showChatWindow(player, val);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		if (!Config.ALLOW_MANOR)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/npcdefault.htm");
			html.replace("%objectId%", getObjectId());
			html.replace("%npcname%", getName());
			player.sendPacket(html);
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		final NpcTalkCond condition = getNpcTalkCond(player);
		if (condition == NpcTalkCond.NONE)
			html.setFile("data/html/castleblacksmith/castleblacksmith-no.htm");
		else if (condition == NpcTalkCond.UNDER_SIEGE)
			html.setFile("data/html/castleblacksmith/castleblacksmith-busy.htm");
		else
		{
			if (val == 0)
				html.setFile("data/html/castleblacksmith/castleblacksmith.htm");
			else
				html.setFile("data/html/castleblacksmith/castleblacksmith-" + val + ".htm");
		}
		html.replace("%objectId%", getObjectId());
		html.replace("%npcname%", getName());
		html.replace("%castleid%", getCastle().getCastleId());
		player.sendPacket(html);
	}
	
	@Override
	protected NpcTalkCond getNpcTalkCond(Player player)
	{
		if (getCastle() != null && player.getClan() != null)
		{
			if (getCastle().getSiege().isInProgress())
				return NpcTalkCond.UNDER_SIEGE;
			
			if (getCastle().getOwnerId() == player.getClanId() && player.hasClanPrivileges(Clan.CP_CS_MANOR_ADMIN))
				return NpcTalkCond.OWNER;
		}
		return NpcTalkCond.NONE;
	}
}