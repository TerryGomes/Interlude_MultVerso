package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminKnownlist implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_knownlist"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		if (command.startsWith("admin_knownlist"))
		{
			// Pick potential player's target or the player himself.
			final WorldObject targetWorldObject = getTarget(WorldObject.class, player, true);
			
			int page = 1;
			
			if (st.hasMoreTokens())
			{
				try
				{
					page = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					player.sendMessage("Usage: //knownlist [page]");
				}
			}
			
			showKnownlist(player, targetWorldObject, page);
		}
	}
	
	private static void showKnownlist(Player player, WorldObject worldObject, int page)
	{
		final Pagination<WorldObject> list = new Pagination<>(worldObject.getKnownType(WorldObject.class).stream(), page, PAGE_LIMIT_18);
		
		// Load static Htm.
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/knownlist.htm");
		html.replace("%target%", worldObject.getName());
		
		final StringBuilder sb = new StringBuilder(2000);
		sb.append("<table width=270><tr><td width=150></td><td width=120></td></tr>");
		
		// Generate data.
		if (list.isEmpty())
			sb.append("<tr><td>No objects in vicinity.</td></tr>");
		else
		{
			for (WorldObject wo : list)
				StringUtil.append(sb, "<tr><td>", wo.getName(), "</td><td>", wo.getClass().getSimpleName(), "</td></tr>");
		}
		
		sb.append("</table>");
		
		list.generateSpace(sb);
		list.generatePages(sb, "bypass admin_knownlist %page%");
		
		html.replace("%content%", sb.toString());
		
		player.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}