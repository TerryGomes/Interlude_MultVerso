package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_zone"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		if (!st.hasMoreTokens())
		{
			showHtml(player);
			return;
		}
		
		switch (st.nextToken().toLowerCase())
		{
			case "show":
				try
				{
					final ExServerPrimitive debug = player.getDebugPacket("ZONE");
					debug.reset();
					
					final String param = st.nextToken().toLowerCase();
					switch (param)
					{
						case "all":
							for (ZoneType zone : player.getZones(false))
								zone.visualizeZone(debug, player.getZ());
							
							debug.sendTo(player);
							
							showHtml(player);
							break;
						
						case "clear":
							debug.sendTo(player);
							
							showHtml(player);
							break;
						
						default:
							ZoneManager.getInstance().getZoneById(Integer.parseInt(param)).visualizeZone(debug, player.getZ());
							
							debug.sendTo(player);
							break;
					}
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid parameter for //zone show.");
				}
				break;
			
			default:
				showHtml(player);
				break;
		}
	}
	
	private static void showHtml(Player player)
	{
		int x = player.getX();
		int y = player.getY();
		int rx = (x - World.WORLD_X_MIN) / World.TILE_SIZE + World.TILE_X_MIN;
		int ry = (y - World.WORLD_Y_MIN) / World.TILE_SIZE + World.TILE_Y_MIN;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/zone.htm");
		
		html.replace("%MAPREGION%", "[x:" + MapRegionData.getMapRegionX(x) + " y:" + MapRegionData.getMapRegionY(y) + "]");
		html.replace("%GEOREGION%", rx + "_" + ry);
		html.replace("%CLOSESTTOWN%", MapRegionData.getInstance().getClosestTownName(x, y));
		html.replace("%CURRENTLOC%", x + ", " + y + ", " + player.getZ());
		
		final StringBuilder sb = new StringBuilder(100);
		
		for (ZoneId zoneId : ZoneId.VALUES)
		{
			if (player.isInsideZone(zoneId))
				StringUtil.append(sb, zoneId, "<br1>");
		}
		html.replace("%ZONES%", sb.toString());
		
		// Reset the StringBuilder for another use.
		sb.setLength(0);
		
		for (ZoneType zoneType : World.getInstance().getRegion(x, y).getZones())
		{
			if (zoneType.isCharacterInZone(player))
				StringUtil.append(sb, zoneType.getId(), " ");
		}
		html.replace("%ZLIST%", sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}