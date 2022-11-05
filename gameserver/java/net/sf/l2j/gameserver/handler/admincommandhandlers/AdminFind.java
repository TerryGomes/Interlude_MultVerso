package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminFind implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_find",
		"admin_list"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.startsWith("admin_find"))
		{
			final int paramCount = st.countTokens();
			if (paramCount == 0)
			{
				listAllPlayers(player, 1);
				return;
			}
			
			String param = null;
			String nameIpOrPage = null;
			
			if (paramCount == 1)
				nameIpOrPage = st.nextToken();
			else if (paramCount == 2)
			{
				param = st.nextToken();
				nameIpOrPage = st.nextToken();
			}
			
			if (nameIpOrPage != null && StringUtil.isDigit(nameIpOrPage))
			{
				listAllPlayers(player, Integer.parseInt(nameIpOrPage));
				return;
			}
			
			if (param == null)
			{
				listAllPlayers(player, 1);
				return;
			}
			
			switch (param)
			{
				case "player":
					try
					{
						listPlayersPerName(player, nameIpOrPage);
					}
					catch (Exception e)
					{
						player.sendMessage("Usage: //find player name");
						listAllPlayers(player, 1);
					}
					break;
				
				case "ip":
					try
					{
						listPlayersPerIp(player, nameIpOrPage);
					}
					catch (Exception e)
					{
						player.sendMessage("Usage: //find ip 111.222.333.444");
						listAllPlayers(player, 1);
					}
					break;
				
				case "account":
					try
					{
						listPlayersPerAccount(player, nameIpOrPage);
					}
					catch (Exception e)
					{
						player.sendMessage("Usage: //find account name");
						listAllPlayers(player, 1);
					}
					break;
				
				case "dualbox":
					try
					{
						final int multibox = Integer.parseInt(nameIpOrPage);
						if (multibox < 1)
						{
							player.sendMessage("Usage: //find dualbox [number > 0]");
							return;
						}
						
						listDualbox(player, multibox);
					}
					catch (Exception e)
					{
						listDualbox(player, 2);
					}
					break;
			}
		}
		else if (command.startsWith("admin_list"))
		{
			try
			{
				listAllPlayers(player, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				player.sendMessage("Usage: //list page");
			}
		}
	}
	
	/**
	 * Find all {@link Player}s and paginate them, then send back the results to the {@link Player}.
	 * @param player : The {@link Player} to send back results.
	 * @param page : The page to show.
	 */
	private static void listAllPlayers(Player player, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/charlist.htm");
		
		final Pagination<Player> list = new Pagination<>(World.getInstance().getPlayers().stream(), page, PAGE_LIMIT_15);
		
		final StringBuilder sb = new StringBuilder(2000);
		for (Player targetPlayer : list)
			StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_debug ", targetPlayer.getName(), "\">", targetPlayer.getName(), "</a></td><td>", targetPlayer.getTemplate().getClassName(), "</td><td>", targetPlayer.getStatus().getLevel(), "</td></tr>");
		
		html.replace("%players%", sb.toString());
		
		sb.setLength(0);
		
		list.generateSpace(sb);
		list.generatePages(sb, "bypass admin_list %page%");
		
		html.replace("%pages%", sb.toString());
		player.sendPacket(html);
	}
	
	/**
	 * Find all {@link Player}s using their names and related to a tested {@link String}, and send back the results to the {@link Player}.
	 * @param player : The {@link Player} to send back results.
	 * @param partOfName : The {@link String} name - or part of it - to search.
	 */
	private static void listPlayersPerName(Player player, String partOfName)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/charfind.htm");
		
		int charactersFound = 0;
		
		final StringBuilder sb = new StringBuilder();
		for (Player worldPlayer : World.getInstance().getPlayers())
		{
			String name = worldPlayer.getName();
			if (name.toLowerCase().contains(partOfName.toLowerCase()))
			{
				charactersFound++;
				StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_debug ", name, "\">", name, "</a></td><td>", worldPlayer.getTemplate().getClassName(), "</td><td>", worldPlayer.getStatus().getLevel(), "</td></tr>");
			}
			
			if (charactersFound > 20)
				break;
		}
		html.replace("%results%", sb.toString());
		
		// Cleanup sb.
		sb.setLength(0);
		
		// Second use of sb.
		if (charactersFound == 0)
			sb.append("s. Please try again.");
		else if (charactersFound > 20)
		{
			html.replace("%number%", " more than 20.");
			sb.append("s.<br>Please refine your search to see all of the results.");
		}
		else if (charactersFound == 1)
			sb.append(".");
		else
			sb.append("s.");
		
		html.replace("%number%", charactersFound);
		html.replace("%end%", sb.toString());
		player.sendPacket(html);
	}
	
	/**
	 * List all {@link Player}s attached to an IP and send results to the {@link Player} set as parameter.
	 * @param player : The {@link Player} who requested the action.
	 * @param ipAdress : The {@link String} used as tested IP.
	 * @throws IllegalArgumentException if the IP is malformed.
	 */
	private static void listPlayersPerIp(Player player, String ipAdress) throws IllegalArgumentException
	{
		boolean findDisconnected = false;
		
		if (ipAdress.equals("disconnected"))
			findDisconnected = true;
		else
		{
			if (!ipAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
				throw new IllegalArgumentException("Malformed IPv4 number");
		}
		
		int charactersFound = 0;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/ipfind.htm");
		
		final StringBuilder sb = new StringBuilder(1000);
		for (Player worldPlayer : World.getInstance().getPlayers())
		{
			final GameClient client = worldPlayer.getClient();
			if (client.isDetached())
			{
				if (!findDisconnected)
					continue;
			}
			else
			{
				if (findDisconnected)
					continue;
				
				if (!client.getConnection().getInetAddress().getHostAddress().equals(ipAdress))
					continue;
			}
			
			StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_debug ", worldPlayer.getName(), "\">", worldPlayer.getName(), "</a></td><td>", worldPlayer.getTemplate().getClassName(), "</td><td>", worldPlayer.getStatus().getLevel(), "</td></tr>");
			
			if (charactersFound++ > 20)
				break;
		}
		
		if (charactersFound > 20)
			html.replace("%number%", "more than 20");
		else
			html.replace("%number%", charactersFound);
		
		html.replace("%ip%", ipAdress);
		html.replace("%results%", sb.toString());
		player.sendPacket(html);
	}
	
	/**
	 * List all characters names attached to an ONLINE {@link Player} name and send results to the {@link Player} set as parameter.
	 * @param player : The {@link Player} who requested the action.
	 * @param name : The {@link String} name to test.
	 */
	private static void listPlayersPerAccount(Player player, String name)
	{
		final Player worldPlayer = World.getInstance().getPlayer(name);
		if (worldPlayer == null)
		{
			player.sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/accountinfo.htm");
		html.replace("%name%", name);
		html.replace("%characters%", String.join("<br1>", worldPlayer.getAccountChars().values()));
		html.replace("%account%", worldPlayer.getAccountName());
		player.sendPacket(html);
	}
	
	/**
	 * Test multiboxing {@link Player}s and send results to the {@link Player} set as parameter.
	 * @param player : The {@link Player} who requested the action.
	 * @param multibox : The tested value to trigger multibox.
	 */
	private static void listDualbox(Player player, int multibox)
	{
		final Map<String, List<Player>> ips = new HashMap<>();
		final Map<String, Integer> dualboxIPs = new HashMap<>();
		
		for (Player worldPlayer : World.getInstance().getPlayers())
		{
			final GameClient client = worldPlayer.getClient();
			if (client == null || client.isDetached())
				continue;
			
			final String ip = client.getConnection().getInetAddress().getHostAddress();
			
			final List<Player> list = ips.computeIfAbsent(ip, k -> new ArrayList<>());
			list.add(worldPlayer);
			
			if (list.size() >= multibox)
			{
				Integer count = dualboxIPs.get(ip);
				if (count == null)
					dualboxIPs.put(ip, multibox);
				else
					dualboxIPs.put(ip, count++);
			}
		}
		
		final List<String> keys = dualboxIPs.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).map(Map.Entry::getKey).collect(Collectors.toList());
		
		final StringBuilder sb = new StringBuilder();
		for (String dualboxIP : keys)
			StringUtil.append(sb, "<a action=\"bypass -h admin_find ip ", dualboxIP, "\">", dualboxIP, " (", dualboxIPs.get(dualboxIP), ")</a><br1>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/dualbox.htm");
		html.replace("%multibox%", multibox);
		html.replace("%results%", sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}