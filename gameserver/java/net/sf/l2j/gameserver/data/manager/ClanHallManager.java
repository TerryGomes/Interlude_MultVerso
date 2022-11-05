package net.sf.l2j.gameserver.data.manager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.enums.SpawnType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.clanhall.Auction;
import net.sf.l2j.gameserver.model.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.clanhall.ClanHallFunction;
import net.sf.l2j.gameserver.model.clanhall.SiegableHall;
import net.sf.l2j.gameserver.model.entity.ClanHallSiege;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.zone.type.ClanHallZone;

import org.w3c.dom.Document;

/**
 * Loads and store {@link ClanHall}s informations, along their associated {@link Auction}s (if existing), using database and XML informations.
 */
public class ClanHallManager implements IXmlReader
{
	private static final String LOAD_CLANHALLS = "SELECT * FROM clanhall";
	private static final String LOAD_FUNCTIONS = "SELECT * FROM clanhall_functions WHERE hall_id = ?";
	
	private final Map<Integer, ClanHall> _clanHalls = new HashMap<>();
	
	protected ClanHallManager()
	{
		// Build ClanHalls objects with static data.
		load();
		
		// Add dynamic data.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_CLANHALLS);
			PreparedStatement ps2 = con.prepareStatement(LOAD_FUNCTIONS);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int id = rs.getInt("id");
				
				final ClanHall ch = _clanHalls.get(id);
				if (ch == null)
					continue;
				
				// Find the related zone, and associate it with the Clan Hall.
				final ClanHallZone zone = ZoneManager.getInstance().getAllZones(ClanHallZone.class).stream().filter(z -> z.getResidenceId() == id).findFirst().orElse(null);
				if (zone == null)
					LOGGER.warn("No existing ClanHallZone for ClanHall {}.", id);
				
				// A default bid exists, it means it's a regular Clan Hall. Generate an Auction.
				if (ch.getDefaultBid() > 0)
					ch.setAuction(new Auction(ch, rs.getInt("sellerBid"), rs.getString("sellerName"), rs.getString("sellerClanName"), rs.getLong("endDate")));
				// No default bid ; it's actually a Siegable Hall.
				else
				{
					// Test siege date, registered as end date.
					long nextSiege = rs.getLong("endDate");
					if (nextSiege - System.currentTimeMillis() < 0)
						((SiegableHall) ch).updateNextSiege();
					else
					{
						final Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(nextSiege);
						
						((SiegableHall) ch).setNextSiegeDate(cal);
					}
				}
				
				// Feed the zone.
				ch.setZone(zone);
				
				final int ownerId = rs.getInt("ownerId");
				if (ownerId > 0)
				{
					final Clan clan = ClanTable.getInstance().getClan(ownerId);
					if (clan == null)
					{
						ch.free();
						continue;
					}
					
					// Set Clan variable.
					clan.setClanHallId(id);
					
					// Set ClanHall variables.
					ch.setOwnerId(ownerId);
					ch.setPaidUntil(rs.getLong("paidUntil"));
					ch.setPaid(rs.getBoolean("paid"));
					
					// Initialize the fee task.
					ch.initializeFeeTask();
					
					// Load related ClanHallFunctions.
					ps2.setInt(1, id);
					
					try (ResultSet rs2 = ps2.executeQuery())
					{
						while (rs2.next())
							ch.getFunctions().put(rs2.getInt("type"), new ClanHallFunction(ch, rs2.getInt("type"), rs2.getInt("lvl"), rs2.getInt("lease"), rs2.getLong("rate"), rs2.getLong("endTime")));
					}
					ps2.clearParameters();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load clan hall data.", e);
		}
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/clanHalls.xml");
		LOGGER.info("Loaded {} clan halls and {} siegable clan halls.", _clanHalls.size(), getSiegableHalls().size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "clanhall", chNode ->
		{
			final StatSet set = parseAttributes(chNode);
			final ClanHall ch = (set.containsKey("siegeLength")) ? new SiegableHall(set) : new ClanHall(set);
			
			forEach(chNode, "spawns", spawnsNode -> forEach(spawnsNode, "spawn", spawnNode -> ch.addSpawn(parseEnum(spawnNode.getAttributes(), SpawnType.class, "type"), parseLocation(spawnNode))));
			
			_clanHalls.put(set.getInteger("id"), ch);
		}));
	}
	
	/**
	 * @param id : The ClanHall id to retrieve.
	 * @return a {@link ClanHall} by its id.
	 */
	public final ClanHall getClanHall(int id)
	{
		return _clanHalls.get(id);
	}
	
	/**
	 * @param id : The ClanHall id to retrieve.
	 * @return a {@link SiegableHall} by its id.
	 */
	public SiegableHall getSiegableHall(int id)
	{
		final ClanHall ch = _clanHalls.get(id);
		return (ch instanceof SiegableHall) ? (SiegableHall) ch : null;
	}
	
	/**
	 * @return a {@link Map} with all {@link ClanHall}s.
	 */
	public final Map<Integer, ClanHall> getClanHalls()
	{
		return _clanHalls;
	}
	
	/**
	 * @return a {@link List} with all {@link SiegableHall}s.
	 */
	public List<SiegableHall> getSiegableHalls()
	{
		return _clanHalls.values().stream().filter(SiegableHall.class::isInstance).map(SiegableHall.class::cast).collect(Collectors.toList());
	}
	
	/**
	 * @return a {@link List} with all auctionable {@link ClanHall}s.
	 */
	public final List<ClanHall> getAuctionableClanHalls()
	{
		final List<ClanHall> list = new ArrayList<>();
		for (ClanHall ch : _clanHalls.values())
		{
			// No Auction has been registered for this ClanHall - continue.
			final Auction auction = ch.getAuction();
			if (auction == null)
				continue;
			
			// ClanHall is owned, but no Auction has been registered by the owner - continue.
			if (ch.getOwnerId() > 0 && auction.getSeller() == null)
				continue;
			
			list.add(ch);
		}
		return list;
	}
	
	/**
	 * @param location : The location name used as parameter.
	 * @return a {@link List} with all {@link ClanHall}s which are in a given location.
	 */
	public final List<ClanHall> getClanHallsByLocation(String location)
	{
		return _clanHalls.values().stream().filter(ch -> ch.getLocation().equalsIgnoreCase(location)).collect(Collectors.toList());
	}
	
	/**
	 * @param clan : The {@link Clan} to check.
	 * @return the {@link ClanHall} owned by the Clan, or null otherwise.
	 */
	public final ClanHall getClanHallByOwner(Clan clan)
	{
		return _clanHalls.values().stream().filter(ch -> ch.getOwnerId() == clan.getClanId()).findFirst().orElse(null);
	}
	
	/**
	 * @param id : The ClanHall id used as reference.
	 * @return the {@link Auction} associated to a {@link ClanHall}, or null if not existing.
	 */
	public final Auction getAuction(int id)
	{
		final ClanHall ch = _clanHalls.get(id);
		return (ch == null) ? null : ch.getAuction();
	}
	
	public final ClanHallSiege getActiveSiege(Creature creature)
	{
		for (ClanHall ch : _clanHalls.values())
		{
			if (!(ch instanceof SiegableHall))
				continue;
			
			final SiegableHall sh = (SiegableHall) ch;
			if (sh.getSiegeZone().isActive() && sh.getSiegeZone().isInsideZone(creature))
				return sh.getSiege();
		}
		return null;
	}
	
	public final boolean isClanParticipating(Clan clan)
	{
		for (SiegableHall hall : getSiegableHalls())
		{
			if (hall.getSiege() != null && hall.getSiege().getAttackerClans().contains(clan))
				return true;
		}
		return false;
	}
	
	public final void onServerShutDown()
	{
		for (SiegableHall hall : getSiegableHalls())
		{
			// Rainbow springs has his own attackers table
			if (hall.getId() == 62 || hall.getSiege() == null)
				continue;
			
			hall.getSiege().saveAttackers();
		}
	}
	
	public static ClanHallManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallManager INSTANCE = new ClanHallManager();
	}
}