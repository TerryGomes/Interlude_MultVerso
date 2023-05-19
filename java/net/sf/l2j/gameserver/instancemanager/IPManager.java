package net.sf.l2j.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.clientpackets.CloseGame;

/**
 * @author TerryMaster Pro Jr
 */
public class IPManager
{
	private static final Logger _log = Logger.getLogger(IPManager.class.getName());
	
	public IPManager()
	{
		_log.log(Level.INFO, "IPManager - Loaded.");
	}
	
	private static boolean multiboxKickTask(Player activeChar, Integer numberBox, Collection<Player> world)
	{
		Map<String, List<Player>> ipMap = new HashMap<>();
		for (Player player : world)
		{
			if (player.getClient() == null || player.getClient().isDetached())
			{
				continue;
			}
			String ip = activeChar.getClient().getConnection().getInetAddress().getHostAddress();
			String playerIp = player.getClient().getConnection().getInetAddress().getHostAddress();
			
			if (ip.equals(playerIp))
			{
				if (ipMap.get(ip) == null)
				{
					ipMap.put(ip, new ArrayList<Player>());
				}
				
				ipMap.get(ip).add(player);
				
				if (ipMap.get(ip).size() >= numberBox)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean validBox(Player activeChar, Integer numberBox, Collection<Player> world, Boolean forcedLogOut)
	{
		if (multiboxKickTask(activeChar, numberBox, world))
		{
			if (forcedLogOut)
			{
				GameClient client = activeChar.getClient();
				_log.warning("Multibox Protection: " + client.getConnection().getInetAddress().getHostAddress() + " was trying to use over " + numberBox + " clients!");
				System.out.println("DUAL BOX IP: " + activeChar.getName() + " Disconnected..");
				activeChar.sendMessage("I'm sorry, but multibox is not allowed here.");
				// activeChar.startAbnormalEffect(2048);
				// activeChar.startAbnormalEffect(AbnormalEffect.ROOT);
				ThreadPool.schedule(new CloseGame(activeChar, 10), 0L);
			}
			return true;
		}
		return false;
	}
	
	private static class SingletonHolder
	{
		protected static final IPManager _instance = new IPManager();
	}
	
	public static final IPManager getInstance()
	{
		return SingletonHolder._instance;
	}
}