package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.sql.BookmarkTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Bookmark;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminBookmark implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_bk",
		"admin_delbk"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		int page = 1;
		
		if (command.startsWith("admin_bk"))
		{
			if (st.hasMoreTokens())
			{
				final String param = st.nextToken();
				if (StringUtil.isDigit(param))
					page = Integer.parseInt(param);
				else
				{
					if (param.length() > 15)
					{
						player.sendMessage("The bookmark name is too long.");
						return;
					}
					
					if (BookmarkTable.getInstance().isExisting(param, player.getObjectId()))
					{
						player.sendMessage("The bookmark name already exists.");
						return;
					}
					
					BookmarkTable.getInstance().saveBookmark(param, player);
				}
			}
		}
		else if (command.startsWith("admin_delbk"))
		{
			if (!st.hasMoreTokens())
			{
				player.sendMessage("The command delbk must be followed by a valid name.");
				return;
			}
			
			final String param = st.nextToken();
			
			if (!BookmarkTable.getInstance().isExisting(param, player.getObjectId()))
			{
				player.sendMessage("That bookmark doesn't exist.");
				return;
			}
			
			BookmarkTable.getInstance().deleteBookmark(param, player.getObjectId());
		}
		showBookmarks(player, page);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Show the basic HTM fed with generated data.
	 * @param player : The {@link Player} to test.
	 * @param page : The page id to show.
	 */
	private static void showBookmarks(Player player, int page)
	{
		final Pagination<Bookmark> list = new Pagination<>(BookmarkTable.getInstance().getBookmarks(player.getObjectId()).stream(), page, PAGE_LIMIT_18);
		
		// Load static htm.
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/bk.htm");
		
		final StringBuilder sb = new StringBuilder(2000);
		sb.append("<table width=270><tr><td width=225></td><td width=45></td></tr>");
		
		if (list.isEmpty())
			sb.append("<tr><td>No bookmarks are currently registered.</td></tr></table>");
		else
		{
			for (Bookmark bk : list)
				StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_teleport ", bk.getX(), " ", bk.getY(), " ", bk.getZ(), "\">", bk.getName(), " (", bk.getX(), " ", bk.getY(), " ", bk.getZ(), ")", "</a></td><td><a action=\"bypass -h admin_delbk ", bk.getName(), "\">Remove</a></td></tr>");
			
			sb.append("</table>");
			
			list.generateSpace(sb);
			list.generatePages(sb, "bypass admin_bk %page%");
		}
		
		html.replace("%content%", sb.toString());
		player.sendPacket(html);
	}
}