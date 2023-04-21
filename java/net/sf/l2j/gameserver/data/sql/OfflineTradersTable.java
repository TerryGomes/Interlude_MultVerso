package net.sf.l2j.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.OperateType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.craft.ManufactureItem;
import net.sf.l2j.gameserver.model.trade.TradeItem;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.GameClient.GameClientState;

public final class OfflineTradersTable
{
	private static final CLogger LOGGER = new CLogger(OfflineTradersTable.class.getName());

	// SQL DEFINITIONS
	private static final String SAVE_OFFLINE_STATUS = "INSERT INTO character_offline_trade (charId, time, type, title) VALUES (?, ?, ?, ?)";
	private static final String SAVE_ITEMS = "INSERT INTO character_offline_trade_items (charId, item, count, price, enchant) VALUES (?, ?, ?, ?, ?)";
	private static final String CLEAR_OFFLINE_TABLE = "DELETE FROM character_offline_trade";
	private static final String CLEAR_OFFLINE_TABLE_ITEMS = "DELETE FROM character_offline_trade_items";
	private static final String LOAD_OFFLINE_STATUS = "SELECT * FROM character_offline_trade";
	private static final String LOAD_OFFLINE_ITEMS = "SELECT * FROM character_offline_trade_items WHERE charId=?";

	public static void storeOffliners()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement save_offline_status = con.prepareStatement(SAVE_OFFLINE_STATUS);
			PreparedStatement save_items = con.prepareStatement(SAVE_ITEMS))
		{
			try (Statement stm = con.createStatement())
			{
				stm.execute(CLEAR_OFFLINE_TABLE);
				stm.execute(CLEAR_OFFLINE_TABLE_ITEMS);
			}

			for (Player pc : World.getInstance().getPlayers())
			{
				try
				{
					if (pc.getOperateType() != OperateType.NONE && (pc.getClient() == null || pc.getClient().isDetached()))
					{
						save_offline_status.setInt(1, pc.getObjectId());
						save_offline_status.setLong(2, pc.getOfflineStartTime());
						save_offline_status.setInt(3, pc.getOperateType().getId());

						String title = null;
						switch (pc.getOperateType())
						{
							case BUY:
								if (!Config.OFFLINE_TRADE_ENABLE)
								{
									continue;
								}

								title = pc.getBuyList().getTitle();
								for (final TradeItem i : pc.getBuyList())
								{
									save_items.setInt(1, pc.getObjectId());
									save_items.setInt(2, i.getItem().getItemId());
									save_items.setLong(3, i.getQuantity());
									save_items.setLong(4, i.getPrice());
									save_items.setLong(5, i.getEnchant());
									save_items.executeUpdate();
									save_items.clearParameters();
								}
								break;

							case SELL:
							case PACKAGE_SELL:
								if (!Config.OFFLINE_TRADE_ENABLE)
								{
									continue;
								}

								title = pc.getSellList().getTitle();
								pc.getSellList().updateItems();
								for (final TradeItem i : pc.getSellList())
								{
									save_items.setInt(1, pc.getObjectId());
									save_items.setInt(2, i.getObjectId());
									save_items.setLong(3, i.getQuantity());
									save_items.setLong(4, i.getPrice());
									save_items.setLong(5, i.getEnchant());
									save_items.executeUpdate();
									save_items.clearParameters();
								}
								break;

							case MANUFACTURE:
								if (!Config.OFFLINE_CRAFT_ENABLE)
								{
									continue;
								}

								title = pc.getManufactureList().getStoreName();
								for (final ManufactureItem i : pc.getManufactureList())
								{
									save_items.setInt(1, pc.getObjectId());
									save_items.setInt(2, i.getId());
									save_items.setLong(3, 0);
									save_items.setLong(4, i.getValue());
									save_items.setLong(5, 0);
									save_items.executeUpdate();
									save_items.clearParameters();
								}
								break;
						}

						save_offline_status.setString(4, title);
						save_offline_status.executeUpdate();
						save_offline_status.clearParameters();
					}
				}
				catch (Exception e)
				{
					LOGGER.error("error while saving offline trader " + pc.getObjectId() + ".", e);
				}
			}

			LOGGER.info(OfflineTradersTable.class.getSimpleName() + ": offline traders stored.");
		}
		catch (Exception e)
		{
			LOGGER.error("error while saving offline traders.", e);
		}
	}

	public static void saveOfflineTraders(Player pc)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(CLEAR_OFFLINE_TABLE);
			PreparedStatement stm2 = con.prepareStatement(CLEAR_OFFLINE_TABLE_ITEMS))
		{
			ps.execute();
			stm2.execute();

			try (PreparedStatement save_offline_status = con.prepareStatement(SAVE_OFFLINE_STATUS);
				PreparedStatement save_items = con.prepareStatement(SAVE_ITEMS))
			{
				save_offline_status.setInt(1, pc.getObjectId());
				save_offline_status.setLong(2, pc.getOfflineStartTime());
				save_offline_status.setInt(3, pc.getOperateType().getId());

				String title = null;
				switch (pc.getOperateType())
				{
					case BUY:
						title = pc.getBuyList().getTitle();
						for (final TradeItem i : pc.getBuyList())
						{
							save_items.setInt(1, pc.getObjectId());
							save_items.setInt(2, i.getItem().getItemId());
							save_items.setLong(3, i.getQuantity());
							save_items.setLong(4, i.getPrice());
							save_items.setLong(5, i.getEnchant());
							save_items.addBatch();
						}
						save_items.executeBatch();
						break;

					case SELL:
					case PACKAGE_SELL:
						title = pc.getSellList().getTitle();
						pc.getSellList().updateItems();
						for (final TradeItem i : pc.getSellList())
						{
							save_items.setInt(1, pc.getObjectId());
							save_items.setInt(2, i.getObjectId());
							save_items.setLong(3, i.getQuantity());
							save_items.setLong(4, i.getPrice());
							save_items.setLong(5, i.getEnchant());
							save_items.addBatch();
						}
						save_items.executeBatch();
						break;

					case MANUFACTURE:
						title = pc.getManufactureList().getStoreName();
						for (final ManufactureItem i : pc.getManufactureList())
						{
							save_items.setInt(1, pc.getObjectId());
							save_items.setInt(2, i.getId());
							save_items.setLong(3, 0);
							save_items.setLong(4, i.getValue());
							save_items.setLong(5, 0);
							save_items.addBatch();
						}
						save_items.executeBatch();
						break;
				}
				save_offline_status.setString(4, title);
				save_offline_status.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("error while saving offline traders.", e);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("error while clear table offline traders.", e);
		}
	}

	public static void restoreOfflineTraders()
	{
		try (Connection con = ConnectionPool.getConnection();
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(LOAD_OFFLINE_STATUS))
		{
			int nTraders = 0;
			while (rs.next())
			{
				final long time = rs.getLong("time");
				if (Config.OFFLINE_MAX_DAYS > 0)
				{
					final Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(time);
					cal.add(Calendar.DAY_OF_YEAR, Config.OFFLINE_MAX_DAYS);
					if (cal.getTimeInMillis() <= System.currentTimeMillis())
					{
						continue;
					}
				}

				OperateType type = null;
				for (OperateType t : OperateType.values())
				{
					if (t.getId() == rs.getInt("type"))
					{
						type = t;
						break;
					}
				}

				if (type == null)
				{
					LOGGER.warn("PrivateStoreType with id " + rs.getInt("type") + " could not be found.");
					continue;
				}

				if (type == OperateType.NONE)
				{
					continue;
				}

				final Player player = Player.restore(rs.getInt("charId"));
				if (player == null)
				{
					continue;
				}

				try (PreparedStatement stm_items = con.prepareStatement(LOAD_OFFLINE_ITEMS))
				{
					player.isRunning();
					player.sitDown();
					player.setOnlineStatus(true, false);

					World.getInstance().addPlayer(player);

					final GameClient client = new GameClient(null);
					client.setDetached(true);
					player.setClient(client);
					client.setPlayer(player);
					client.setAccountName(player.getAccountNamePlayer());
					player.setOnlineStatus(true, true);
					client.setState(GameClientState.IN_GAME);
					player.setOfflineStartTime(time);
					player.spawnMe();

					LoginServerThread.getInstance().addClient(player.getAccountName(), client);

					stm_items.setInt(1, player.getObjectId());
					try (ResultSet items = stm_items.executeQuery())
					{
						switch (type)
						{
							case BUY:
								while (items.next())
								{
									player.getBuyList().addItemByItemId(items.getInt(2), items.getInt(3), items.getInt(4), items.getInt(5));
								}

								player.getBuyList().setTitle(rs.getString("title"));
								break;

							case SELL:
							case PACKAGE_SELL:
								while (items.next())
								{
									player.getSellList().addItem(items.getInt(2), items.getInt(3), items.getInt(4));
								}

								player.getSellList().setTitle(rs.getString("title"));
								player.getSellList().setPackaged(type == OperateType.PACKAGE_SELL);
								break;

							case MANUFACTURE:
								while (items.next())
								{
									player.getManufactureList().add(new ManufactureItem(items.getInt(2), items.getInt(4)));
								}

								player.getManufactureList().setStoreName(rs.getString("title"));
								break;
						}
					}

					if (Config.OFFLINE_SLEEP_EFFECT)
					{
						player.startAbnormalEffect(Integer.decode("0x80"));
					}

					player.setOperateType(type);
					player.restoreEffects();
					player.broadcastUserInfo();

					nTraders++;
				}
				catch (Exception e)
				{
					LOGGER.error("error loading trader " + player.getObjectId() + ".", e);
					player.deleteMe();
				}
			}

			LOGGER.info(OfflineTradersTable.class.getSimpleName() + ": loaded " + nTraders + " offline traders.");

			try (Statement stm1 = con.createStatement())
			{
				stm1.execute(CLEAR_OFFLINE_TABLE);
				stm1.execute(CLEAR_OFFLINE_TABLE_ITEMS);
			}
		}
		catch (final SQLException e)
		{
			LOGGER.warn(OfflineTradersTable.class.getSimpleName() + ": error while loading offline traders.", e);
		}
	}

	public static boolean offlineMode(final Player player)
	{
		if (player.isInOlympiadMode() || player.isFestivalParticipant() || player.isInJail() || player.getBoat() != null)
		{
			return false;
		}

		boolean canSetShop = false;
		switch (player.getOperateType())
		{
			case SELL:
			case PACKAGE_SELL:
			case BUY:
				canSetShop = Config.OFFLINE_TRADE_ENABLE;
				break;
			case MANUFACTURE:
				canSetShop = Config.OFFLINE_CRAFT_ENABLE;
				break;
		}

		if (Config.OFFLINE_MODE_IN_PEACE_ZONE && !player.isInsideZone(ZoneId.PEACE))
		{
			canSetShop = false;
		}

		return canSetShop;
	}
}