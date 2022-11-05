package net.sf.l2j.gameserver.model.actor.container.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class BlockList
{
	private static final CLogger LOGGER = new CLogger(BlockList.class.getName());
	
	private static final Map<Integer, List<Integer>> OFFLINE_LIST = new HashMap<>();
	
	private static final String LOAD_BLOCKLIST = "SELECT friend_id FROM character_friends WHERE char_id = ? AND relation = 1";
	private static final String INSERT_BLOCKED_USER = "INSERT INTO character_friends (char_id, friend_id, relation) VALUES (?, ?, 1)";
	private static final String DELETE_BLOCKED_USER = "DELETE FROM character_friends WHERE char_id = ? AND friend_id = ? AND relation = 1";
	
	private final Player _owner;
	private List<Integer> _blockList;
	
	private boolean _isBlockingAll;
	
	public BlockList(Player owner)
	{
		_owner = owner;
		
		_blockList = OFFLINE_LIST.get(owner.getObjectId());
		if (_blockList == null)
			_blockList = loadList(_owner.getObjectId());
	}
	
	public boolean isBlockingAll()
	{
		return _isBlockingAll;
	}
	
	public void setInBlockingAll(boolean isBlockingAll)
	{
		_isBlockingAll = isBlockingAll;
		
		_owner.sendPacket(new EtcStatusUpdate(_owner));
	}
	
	private synchronized void addToBlockList(int target)
	{
		_blockList.add(target);
		
		updateInDB(target, true);
	}
	
	private synchronized void removeFromBlockList(int target)
	{
		_blockList.remove(Integer.valueOf(target));
		
		updateInDB(target, false);
	}
	
	public void playerLogout()
	{
		OFFLINE_LIST.put(_owner.getObjectId(), _blockList);
	}
	
	private static List<Integer> loadList(int objectId)
	{
		final List<Integer> list = new ArrayList<>();
		
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(LOAD_BLOCKLIST))
			{
				ps.setInt(1, objectId);
				
				try (ResultSet rset = ps.executeQuery())
				{
					while (rset.next())
					{
						final int friendId = rset.getInt("friend_id");
						if (friendId == objectId)
							continue;
						
						list.add(friendId);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load blocklist for {}.", e, objectId);
		}
		return list;
	}
	
	private void updateInDB(int targetId, boolean state)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement((state) ? INSERT_BLOCKED_USER : DELETE_BLOCKED_USER))
		{
			ps.setInt(1, _owner.getObjectId());
			ps.setInt(2, targetId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't add/remove block player.", e);
		}
	}
	
	public boolean isInBlockList(Player target)
	{
		return _blockList.contains(target.getObjectId());
	}
	
	public boolean isInBlockList(int targetId)
	{
		return _blockList.contains(targetId);
	}
	
	public List<Integer> getBlockList()
	{
		return _blockList;
	}
	
	public static void addToBlockList(Player listOwner, int targetId)
	{
		if (listOwner == null)
			return;
		
		final String targetName = PlayerInfoTable.getInstance().getPlayerName(targetId);
		
		listOwner.getBlockList().addToBlockList(targetId);
		listOwner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_ADDED_TO_YOUR_IGNORE_LIST).addString(targetName));
		
		final Player targetPlayer = World.getInstance().getPlayer(targetId);
		if (targetPlayer != null)
			targetPlayer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addString(listOwner.getName()));
	}
	
	public static void removeFromBlockList(Player listOwner, int targetId)
	{
		if (listOwner == null)
			return;
		
		if (listOwner.getBlockList().getBlockList().contains(targetId))
		{
			listOwner.getBlockList().removeFromBlockList(targetId);
			listOwner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST).addString(PlayerInfoTable.getInstance().getPlayerName(targetId)));
		}
	}
	
	public static void sendListToOwner(Player listOwner)
	{
		int i = 1;
		listOwner.sendPacket(SystemMessageId.BLOCK_LIST_HEADER);
		
		for (int playerId : listOwner.getBlockList().getBlockList())
			listOwner.sendMessage((i++) + ". " + PlayerInfoTable.getInstance().getPlayerName(playerId));
		
		listOwner.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
	}
	
	/**
	 * @param ownerId object id of owner block list
	 * @param targetId object id of potential blocked player
	 * @return true if blocked
	 */
	public static boolean isInBlockList(int ownerId, int targetId)
	{
		final Player player = World.getInstance().getPlayer(ownerId);
		if (player != null)
			return player.getBlockList().isInBlockList(targetId);
		
		final List<Integer> list = OFFLINE_LIST.computeIfAbsent(ownerId, l -> loadList(ownerId));
		return list.contains(targetId);
	}
}