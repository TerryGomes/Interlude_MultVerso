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
				{
					page = Integer.parseInt(param);
				}
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
		// Load static htm.
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.isLang() + "admin/bk.htm");

		int row = 0;

		// Generate data.
		final Pagination<Bookmark> list = new Pagination<>(BookmarkTable.getInstance().getBookmarks(player.getObjectId()).stream(), page, PAGE_LIMIT_15);
		for (Bookmark bk : list)
		{
			list.append(((row % 2) == 0 ? "<table width=280 bgcolor=000000><tr>" : "<table width=280><tr>"));
			list.append("<td width=230><a action=\"bypass -h admin_teleport ", bk.getX(), " ", bk.getY(), " ", bk.getZ(), "\">", bk.getName(), " (", bk.getX(), " ", bk.getY(), " ", bk.getZ(), ")", "</a></td><td width=50><a action=\"bypass -h admin_delbk ", bk.getName(), "\">Remove</a></td>");
			list.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");

			row++;
		}

		list.generateSpace(20);
		list.generatePages("bypass admin_bk %page%");

		html.replace("%content%", list.getContent());
		player.sendPacket(html);
	}
}