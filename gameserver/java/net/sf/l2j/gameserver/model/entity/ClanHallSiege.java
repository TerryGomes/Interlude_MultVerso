package net.sf.l2j.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.SiegeStatus;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.clanhall.SiegableHall;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;

public abstract class ClanHallSiege extends AttackableAIScript implements Siegable
{
	protected static final CLogger LOGGER = new CLogger(ClanHallSiege.class.getName());

	private static final String SELECT_ATTACKERS = "SELECT attacker_id FROM clanhall_siege_attackers WHERE clanhall_id = ?";
	private static final String INSERT_ATTACKERS = "INSERT INTO clanhall_siege_attackers VALUES (?,?)";
	private static final String DELETE_ATTACKERS = "DELETE FROM clanhall_siege_attackers WHERE clanhall_id = ?";

	public static final int FORTRESS_OF_RESISTANCE = 21;
	public static final int DEVASTATED_CASTLE = 34;
	public static final int BANDIT_STRONGHOLD = 35;
	public static final int RAINBOW_SPRINGS = 62;
	public static final int BEAST_FARM = 63;
	public static final int FORTRESS_OF_DEAD = 64;

	protected final List<Clan> _attackers = new CopyOnWriteArrayList<>();

	public SiegableHall _hall;
	public ScheduledFuture<?> _siegeTask;

	protected boolean _missionAccomplished;
	protected boolean _wasPreviouslyOwned;

	protected ClanHallSiege(String descr, final int hallId)
	{
		super(descr);

		_hall = ClanHallManager.getInstance().getSiegableHall(hallId);
		_hall.setSiege(this);

		_siegeTask = ThreadPool.schedule(this::prepareSiege, _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);

		loadAttackers();

		LOGGER.info("{} siege scheduled for {}.", _hall.getName(), getSiegeDate().getTime());
	}

	public abstract Clan getWinner();

	public abstract void spawnNpcs();

	public abstract void unspawnNpcs();

	@Override
	public Npc getFlag(Clan clan)
	{
		return (clan != null) ? clan.getFlag() : null;
	}

	@Override
	public List<Clan> getAttackerClans()
	{
		return _attackers;
	}

	@Override
	public boolean checkSide(Clan clan, SiegeSide type)
	{
		return clan != null && type == SiegeSide.ATTACKER && _attackers.contains(clan);
	}

	@Override
	public boolean checkSides(Clan clan, SiegeSide... types)
	{
		if (clan == null)
		{
			return false;
		}

		for (SiegeSide type : types)
		{
			if (type == SiegeSide.ATTACKER)
			{
				return _attackers.contains(clan);
			}
		}
		return false;
	}

	@Override
	public boolean checkSides(Clan clan)
	{
		return clan != null && _attackers.contains(clan);
	}

	@Override
	public List<Clan> getDefenderClans()
	{
		return Collections.emptyList();
	}

	@Override
	public Calendar getSiegeDate()
	{
		return _hall.getSiegeDate();
	}

	@Override
	public void startSiege()
	{
		// Fortress of Resistance doesn't have attacker list.
		if (_attackers.isEmpty() && _hall.getId() != 21)
		{
			_hall.updateNextSiege();

			_siegeTask = ThreadPool.schedule(this::prepareSiege, _hall.getSiegeDate().getTimeInMillis());

			_hall.updateSiegeStatus(SiegeStatus.REGISTRATION_OVER);

			World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST).addFortId(_hall.getId()));
			return;
		}

		_hall.spawnDoor();

		spawnNpcs();

		// Banish everyone out of the ClanHallZone (which explains the -1 as value).
		_hall.getZone().banishForeigners(-1);
		_hall.getSiegeZone().setActive(true);

		final byte state = 1;
		for (Clan clan : _attackers)
		{
			for (Player player : clan.getOnlineMembers())
			{
				player.setSiegeState(state);
				player.broadcastUserInfo();
			}
		}

		_hall.updateSiegeStatus(SiegeStatus.IN_PROGRESS);

		_siegeTask = ThreadPool.schedule(this::endSiege, _hall.getSiegeLength());
	}

	@Override
	public void endSiege()
	{
		unspawnNpcs();

		World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_ENDED).addFortId(_hall.getId()));

		final Clan winner = getWinner();

		if (_missionAccomplished && (winner != null))
		{
			_hall.setOwner(winner);

			winner.setClanHallId(_hall.getId());

			World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_VICTORIOUS_OVER_S2_S_SIEGE).addString(winner.getName()).addFortId(_hall.getId()));
		}
		else
		{
			World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.SIEGE_S1_DRAW).addFortId(_hall.getId()));
		}

		_missionAccomplished = false;

		_hall.getSiegeZone().setActive(false);

		_hall.updateNextSiege();
		_hall.spawnDoor(false);
		_hall.banishForeigners();

		final byte state = 0;
		for (Clan clan : _attackers)
		{
			clan.setFlag(null);

			for (Player player : clan.getOnlineMembers())
			{
				player.setSiegeState(state);
				player.broadcastUserInfo();
			}
		}

		// Update pvp flag for winners when siege zone becomes inactive
		for (Player player : _hall.getSiegeZone().getKnownTypeInside(Player.class))
		{
			player.updatePvPStatus();
		}

		_attackers.clear();

		_siegeTask = ThreadPool.schedule(this::prepareSiege, _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		LOGGER.info("Siege of {} scheduled for {}.", _hall.getName(), _hall.getSiegeDate().getTime());

		_hall.updateSiegeStatus(SiegeStatus.REGISTRATION_OPENED);
	}

	public boolean canPlantFlag()
	{
		return true;
	}

	public boolean doorIsAutoAttackable()
	{
		return true;
	}

	public List<Player> getAttackersInZone()
	{
		final List<Player> attackers = new ArrayList<>();
		for (Player player : _hall.getSiegeZone().getKnownTypeInside(Player.class))
		{
			final Clan clan = player.getClan();
			if (clan != null && _attackers.contains(clan))
			{
				attackers.add(player);
			}
		}
		return attackers;
	}

	public void instantSiege()
	{
		prepareSiege(0);
	}

	protected void prepareSiege()
	{
		prepareSiege(3600000);
	}

	private void prepareSiege(long delay)
	{
		if (_hall.getOwnerId() > 0)
		{
			final Clan clan = ClanTable.getInstance().getClan(_hall.getOwnerId());
			if (clan != null)
			{
				_attackers.add(clan);
			}

			_wasPreviouslyOwned = true;
		}
		else
		{
			_wasPreviouslyOwned = false;
		}

		_hall.free();
		_hall.banishForeigners();
		_hall.updateSiegeStatus(SiegeStatus.REGISTRATION_OVER);

		_siegeTask = ThreadPool.schedule(this::startSiege, delay);

		World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.REGISTRATION_TERM_FOR_S1_ENDED).addFortId(_hall.getId()));
	}

	public void cancelSiegeTask()
	{
		if (_siegeTask != null)
		{
			_siegeTask.cancel(false);
		}
	}

	public void loadAttackers()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ATTACKERS))
		{
			ps.setInt(1, _hall.getId());

			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final Clan clan = ClanTable.getInstance().getClan(rset.getInt("attacker_id"));
					if (clan != null)
					{
						_attackers.add(clan);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't load {} siege attackers.", e, getName());
		}
	}

	public final void saveAttackers()
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(DELETE_ATTACKERS))
			{
				ps.setInt(1, _hall.getId());
				ps.execute();
			}

			if (!_attackers.isEmpty())
			{
				try (PreparedStatement insert = con.prepareStatement(INSERT_ATTACKERS))
				{
					for (Clan clan : _attackers)
					{
						insert.setInt(1, _hall.getId());
						insert.setInt(2, clan.getClanId());
						insert.addBatch();
					}
					insert.executeBatch();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't save {} siege attackers.", e, getName());
		}
	}

	public final void updateAttackers()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_ATTACKERS))
		{
			ps.setInt(1, _hall.getId());
			ps.execute();

			if (!_attackers.isEmpty())
			{
				try (PreparedStatement insert = con.prepareStatement(INSERT_ATTACKERS))
				{
					for (Clan clan : _attackers)
					{
						insert.setInt(1, _hall.getId());
						insert.setInt(2, clan.getClanId());
						insert.executeUpdate();
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("{}: Couldnt save attacker list!", getName(), e);
		}
	}
}