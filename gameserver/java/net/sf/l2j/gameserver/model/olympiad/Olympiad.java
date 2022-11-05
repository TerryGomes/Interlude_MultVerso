package net.sf.l2j.gameserver.model.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.OlympiadState;
import net.sf.l2j.gameserver.enums.OlympiadType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.zone.type.OlympiadStadiumZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Olympiad
{
	protected static final CLogger LOGGER = new CLogger(Olympiad.class.getName());
	
	private final Map<Integer, StatSet> _nobles = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _rankRewards = new HashMap<>();
	
	private static final String SELECT_OLYMPIAD_DATA = "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id = 0";
	private static final String INSERT_OLYMPIAD_DATA = "INSERT INTO olympiad_data (id, current_cycle, period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, validation_end=?, next_weekly_change=?";
	
	private static final String SELECT_OLYMPIAD_NOBLES = "SELECT olympiad_nobles.char_id, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM olympiad_nobles, characters WHERE characters.obj_Id = olympiad_nobles.char_id";
	private static final String INSERT_OR_UPDATE_OLYMPIAD_NOBLES = "INSERT INTO olympiad_nobles (`char_id`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`, `competitions_drawn`) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE olympiad_points=VALUES(olympiad_points), competitions_done=VALUES(competitions_done), competitions_won=VALUES(competitions_won), competitions_lost=VALUES(competitions_lost), competitions_drawn=VALUES(competitions_drawn)";
	private static final String TRUNCATE_OLYMPIAD_NOBLES = "TRUNCATE olympiad_nobles";
	
	private static final String SELECT_CLASSIFIED_NOBLES = "SELECT char_id from olympiad_nobles_eom WHERE competitions_done >= ? ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC";
	private static final String SELECT_CLASS_LEADER = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.obj_Id = olympiad_nobles_eom.char_id AND olympiad_nobles_eom.class_id = ? AND olympiad_nobles_eom.competitions_done >= ? ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	
	private static final String SELECT_MONTH_OLYMPIAD_POINTS = "SELECT olympiad_points FROM olympiad_nobles_eom WHERE char_id = ?";
	private static final String INSERT_MONTH_OLYMPIAD = "INSERT INTO olympiad_nobles_eom SELECT char_id, class_id, olympiad_points, competitions_done, competitions_won, competitions_lost, competitions_drawn FROM olympiad_nobles";
	private static final String TRUNCATE_MONTH_OLYMPIAD = "TRUNCATE olympiad_nobles_eom";
	
	public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";
	
	public static final String CHAR_ID = "char_id";
	public static final String CLASS_ID = "class_id";
	public static final String CHAR_NAME = "char_name";
	public static final String POINTS = "olympiad_points";
	public static final String COMP_DONE = "competitions_done";
	public static final String COMP_WON = "competitions_won";
	public static final String COMP_LOST = "competitions_lost";
	public static final String COMP_DRAWN = "competitions_drawn";
	
	protected long _olympiadEnd;
	protected long _validationEnd;
	
	protected OlympiadState _period;
	protected long _nextWeeklyChange;
	protected int _currentCycle;
	private long _compEnd;
	private Calendar _compStart;
	protected boolean _isInCompPeriod;
	
	protected ScheduledFuture<?> _competitionStartTask;
	protected ScheduledFuture<?> _competitionEndTask;
	protected ScheduledFuture<?> _olympiadEndTask;
	protected ScheduledFuture<?> _weeklyTask;
	protected ScheduledFuture<?> _validationEndTask;
	protected ScheduledFuture<?> _gameManagerTask;
	protected ScheduledFuture<?> _gameAnnouncerTask;
	
	protected Olympiad()
	{
		load();
		
		if (_period == OlympiadState.COMPETITION)
			init();
	}
	
	public StatSet getNobleStats(int objectId)
	{
		return _nobles.get(objectId);
	}
	
	/**
	 * @param objectId : The {@link Player} objectId to affect.
	 * @param set : The {@link StatSet} to set.
	 * @return The old {@link StatSet} if the {@link Player} objectId was already present, or null otherwise.
	 */
	public StatSet addNobleStats(int objectId, StatSet set)
	{
		return _nobles.put(objectId, set);
	}
	
	public int getNoblePoints(int objId)
	{
		final StatSet set = _nobles.get(objId);
		return (set == null) ? 0 : set.getInteger(POINTS);
	}
	
	public boolean isOlympiadEnd()
	{
		return _period == OlympiadState.VALIDATION;
	}
	
	public boolean isInCompPeriod()
	{
		return _isInCompPeriod;
	}
	
	public int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	private void load()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_OLYMPIAD_DATA);
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				_currentCycle = rs.getInt("current_cycle");
				_period = Enum.valueOf(OlympiadState.class, rs.getString("period"));
				_olympiadEnd = rs.getLong("olympiad_end");
				_validationEnd = rs.getLong("validation_end");
				_nextWeeklyChange = rs.getLong("next_weekly_change");
			}
			else
			{
				_currentCycle = 1;
				_period = OlympiadState.COMPETITION;
				_olympiadEnd = 0;
				_validationEnd = 0;
				_nextWeeklyChange = 0;
				
				LOGGER.info("Couldn't load Olympiad data, default values are used.");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load Olympiad data.", e);
		}
		
		switch (_period)
		{
			case COMPETITION:
				if (_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis())
					setNewOlympiadEnd();
				else
					scheduleWeeklyChange();
				break;
			
			case VALIDATION:
				if (_validationEnd > Calendar.getInstance().getTimeInMillis())
				{
					// Process rank rewards.
					processRankRewards();
					
					_validationEndTask = ThreadPool.schedule(this::validationEnd, getMillisToValidationEnd());
				}
				else
				{
					_currentCycle++;
					_period = OlympiadState.COMPETITION;
					
					deleteNobles();
					setNewOlympiadEnd();
				}
				break;
		}
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_OLYMPIAD_NOBLES);
			ResultSet rset = ps.executeQuery())
		{
			while (rset.next())
			{
				final StatSet set = new StatSet();
				set.set(CLASS_ID, rset.getInt(CLASS_ID));
				set.set(CHAR_NAME, rset.getString(CHAR_NAME));
				set.set(POINTS, rset.getInt(POINTS));
				set.set(COMP_DONE, rset.getInt(COMP_DONE));
				set.set(COMP_WON, rset.getInt(COMP_WON));
				set.set(COMP_LOST, rset.getInt(COMP_LOST));
				set.set(COMP_DRAWN, rset.getInt(COMP_DRAWN));
				
				addNobleStats(rset.getInt(CHAR_ID), set);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load noblesse data.", e);
		}
		
		synchronized (this)
		{
			long milliToEnd;
			if (_period == OlympiadState.COMPETITION)
				milliToEnd = getMillisToOlympiadEnd();
			else
				milliToEnd = getMillisToValidationEnd();
			
			LOGGER.info("{} minutes until Olympiad period ends.", Math.round(milliToEnd / 60000));
			
			if (_period == OlympiadState.COMPETITION)
			{
				milliToEnd = getMillisToWeekChange();
				LOGGER.info("Next weekly Olympiad change is in {} minutes.", Math.round(milliToEnd / 60000));
			}
		}
		
		LOGGER.info("Loaded {} nobles.", _nobles.size());
	}
	
	/**
	 * Calculate and store ranks rewards for all classified {@link Player}s nobles.
	 */
	public void processRankRewards()
	{
		_rankRewards.clear();
		
		final Map<Integer, Integer> temporaryRanks = new HashMap<>();
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_CLASSIFIED_NOBLES))
		{
			ps.setInt(1, Config.OLY_MIN_MATCHES);
			
			try (ResultSet rs = ps.executeQuery())
			{
				int place = 1;
				while (rs.next())
					temporaryRanks.put(rs.getInt(CHAR_ID), place++);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load Olympiad ranks.", e);
		}
		
		final int size = temporaryRanks.size();
		
		int rank1 = (int) Math.round(size * 0.01);
		int rank2 = (int) Math.round(size * 0.10);
		int rank3 = (int) Math.round(size * 0.25);
		int rank4 = (int) Math.round(size * 0.50);
		
		if (rank1 == 0)
		{
			rank1 = 1;
			rank2++;
			rank3++;
			rank4++;
		}
		
		for (Entry<Integer, Integer> temporaryRank : temporaryRanks.entrySet())
		{
			final int objectId = temporaryRank.getKey();
			final int place = temporaryRank.getValue();
			
			if (place <= rank1)
				_rankRewards.put(objectId, 1);
			else if (place <= rank2)
				_rankRewards.put(objectId, 2);
			else if (place <= rank3)
				_rankRewards.put(objectId, 3);
			else if (place <= rank4)
				_rankRewards.put(objectId, 4);
			else
				_rankRewards.put(objectId, 5);
		}
	}
	
	private void init()
	{
		if (_period == OlympiadState.VALIDATION)
			return;
		
		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, Config.OLY_START_TIME);
		_compStart.set(Calendar.MINUTE, Config.OLY_MIN);
		_compStart.set(Calendar.SECOND, 0);
		
		_compEnd = _compStart.getTimeInMillis() + Config.OLY_CPERIOD;
		
		if (_olympiadEndTask != null)
			_olympiadEndTask.cancel(true);
		
		_olympiadEndTask = ThreadPool.schedule(this::olympiadEnd, getMillisToOlympiadEnd());
		
		synchronized (this)
		{
			long milliToStart = getMillisToCompBegin();
			
			double numSecs = (milliToStart / 1000) % 60;
			double countDown = ((milliToStart / 1000) - numSecs) / 60;
			int numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			int numHours = (int) Math.floor(countDown % 24);
			int numDays = (int) Math.floor((countDown - numHours) / 24);
			
			LOGGER.info("Olympiad competition period starts in {} days, {} hours and {} mins.", numDays, numHours, numMins);
			LOGGER.info("Olympiad event starts/started @ {}.", _compStart.getTime());
		}
		
		_competitionStartTask = ThreadPool.schedule(() ->
		{
			if (isOlympiadEnd())
				return;
			
			_isInCompPeriod = true;
			
			World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED));
			LOGGER.info("Olympiad game started.");
			
			_gameManagerTask = ThreadPool.scheduleAtFixedRate(OlympiadGameManager.getInstance(), 30000, 30000);
			
			if (Config.OLY_ANNOUNCE_GAMES)
			{
				_gameAnnouncerTask = ThreadPool.scheduleAtFixedRate(() ->
				{
					for (OlympiadGameTask task : OlympiadGameManager.getInstance().getOlympiadTasks())
					{
						if (!task.needAnnounce())
							continue;
						
						final AbstractOlympiadGame game = task.getGame();
						if (game == null)
							continue;
						
						String announcement;
						if (game.getType() == OlympiadType.NON_CLASSED)
							announcement = "Olympiad class-free individual match is going to begin in Arena " + (game.getStadiumId() + 1) + " in a moment.";
						else
							announcement = "Olympiad class individual match is going to begin in Arena " + (game.getStadiumId() + 1) + " in a moment.";
						
						for (OlympiadManagerNpc manager : OlympiadManagerNpc.getInstances())
							manager.broadcastNpcShout(announcement);
					}
				}, 30000, 500);
			}
			
			long regEnd = getMillisToCompEnd() - 600000;
			if (regEnd > 0)
				ThreadPool.schedule(() -> World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_REGISTRATION_PERIOD_ENDED)), regEnd);
			
			_competitionEndTask = ThreadPool.schedule(() ->
			{
				if (isOlympiadEnd())
					return;
				
				_isInCompPeriod = false;
				World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED));
				LOGGER.info("Olympiad game ended.");
				
				while (OlympiadGameManager.getInstance().isBattleStarted()) // cleared in game manager
				{
					// wait 1 minutes for end of pendings games
					try
					{
						Thread.sleep(60000);
					}
					catch (InterruptedException e)
					{
					}
				}
				
				if (_gameManagerTask != null)
				{
					_gameManagerTask.cancel(false);
					_gameManagerTask = null;
				}
				
				if (_gameAnnouncerTask != null)
				{
					_gameAnnouncerTask.cancel(false);
					_gameAnnouncerTask = null;
				}
				
				// Save current Olympiad status.
				saveOlympiadStatus();
				
				init();
			}, getMillisToCompEnd());
		}, getMillisToCompBegin());
	}
	
	private long getMillisToOlympiadEnd()
	{
		return (_olympiadEnd - Calendar.getInstance().getTimeInMillis());
	}
	
	public void manualSelectHeroes()
	{
		if (_olympiadEndTask != null)
			_olympiadEndTask.cancel(true);
		
		_olympiadEndTask = ThreadPool.schedule(this::olympiadEnd, 0);
	}
	
	private long getMillisToValidationEnd()
	{
		if (_validationEnd > Calendar.getInstance().getTimeInMillis())
			return (_validationEnd - Calendar.getInstance().getTimeInMillis());
		
		return 10L;
	}
	
	private void setNewOlympiadEnd()
	{
		World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_STARTED).addNumber(_currentCycle));
		
		Calendar currentTime = Calendar.getInstance();
		currentTime.add(Calendar.MONTH, 1);
		currentTime.set(Calendar.DAY_OF_MONTH, 1);
		currentTime.set(Calendar.AM_PM, Calendar.AM);
		currentTime.set(Calendar.HOUR, 12);
		currentTime.set(Calendar.MINUTE, 0);
		currentTime.set(Calendar.SECOND, 0);
		_olympiadEnd = currentTime.getTimeInMillis();
		
		_nextWeeklyChange = Calendar.getInstance().getTimeInMillis() + Config.OLY_WPERIOD;
		scheduleWeeklyChange();
	}
	
	private long getMillisToCompBegin()
	{
		if (_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && _compEnd > Calendar.getInstance().getTimeInMillis())
			return 10L;
		
		if (_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
			return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
		
		return setNewCompBegin();
	}
	
	private long setNewCompBegin()
	{
		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, Config.OLY_START_TIME);
		_compStart.set(Calendar.MINUTE, Config.OLY_MIN);
		_compStart.set(Calendar.SECOND, 0);
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		
		_compEnd = _compStart.getTimeInMillis() + Config.OLY_CPERIOD;
		
		LOGGER.info("New Olympiad schedule @ {}.", _compStart.getTime());
		
		return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}
	
	protected long getMillisToCompEnd()
	{
		return _compEnd - Calendar.getInstance().getTimeInMillis();
	}
	
	private long getMillisToWeekChange()
	{
		if (_nextWeeklyChange > Calendar.getInstance().getTimeInMillis())
			return (_nextWeeklyChange - Calendar.getInstance().getTimeInMillis());
		
		return 10L;
	}
	
	/**
	 * Add Config.OLY_WEEKLY_POINTS to registered {@link Player} every Config.OLY_WPERIOD.
	 */
	private void scheduleWeeklyChange()
	{
		_weeklyTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			_nextWeeklyChange = Calendar.getInstance().getTimeInMillis() + Config.OLY_WPERIOD;
			
			if (_period == OlympiadState.VALIDATION)
				return;
			
			for (StatSet set : _nobles.values())
				set.set(POINTS, set.getInteger(POINTS) + Config.OLY_WEEKLY_POINTS);
			
			LOGGER.info("Added weekly Olympiad points to nobles.");
		}, getMillisToWeekChange(), Config.OLY_WPERIOD);
	}
	
	public boolean playerInStadia(Player player)
	{
		return ZoneManager.getInstance().getZone(player, OlympiadStadiumZone.class) != null;
	}
	
	/**
	 * Save noblesse data to database
	 */
	private void saveNobleData()
	{
		if (_nobles.isEmpty())
			return;
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_OR_UPDATE_OLYMPIAD_NOBLES))
		{
			for (Map.Entry<Integer, StatSet> noble : _nobles.entrySet())
			{
				final StatSet set = noble.getValue();
				if (set == null)
					continue;
				
				ps.setInt(1, noble.getKey());
				ps.setInt(2, set.getInteger(CLASS_ID));
				ps.setInt(3, set.getInteger(POINTS));
				ps.setInt(4, set.getInteger(COMP_DONE));
				ps.setInt(5, set.getInteger(COMP_WON));
				ps.setInt(6, set.getInteger(COMP_LOST));
				ps.setInt(7, set.getInteger(COMP_DRAWN));
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't save Olympiad nobles data.", e);
		}
	}
	
	/**
	 * Save current olympiad status and update noblesse table in database
	 */
	public void saveOlympiadStatus()
	{
		saveNobleData();
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_OLYMPIAD_DATA))
		{
			ps.setInt(1, _currentCycle);
			ps.setString(2, _period.toString());
			ps.setLong(3, _olympiadEnd);
			ps.setLong(4, _validationEnd);
			ps.setLong(5, _nextWeeklyChange);
			ps.setInt(6, _currentCycle);
			ps.setString(7, _period.toString());
			ps.setLong(8, _olympiadEnd);
			ps.setLong(9, _validationEnd);
			ps.setLong(10, _nextWeeklyChange);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't save Olympiad status.", e);
		}
	}
	
	public List<String> getClassLeaderBoard(int classId)
	{
		final List<String> names = new ArrayList<>();
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_CLASS_LEADER))
		{
			ps.setInt(1, classId);
			ps.setInt(2, Config.OLY_MIN_MATCHES);
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					names.add(rs.getString(CHAR_NAME));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load Olympiad leaders.", e);
		}
		return names;
	}
	
	public int getNoblessePasses(Player player, boolean clear)
	{
		if (player == null || _period != OlympiadState.VALIDATION)
			return 0;
		
		final Integer rankReward = _rankRewards.get(player.getObjectId());
		if (rankReward == null)
			return 0;
		
		final StatSet set = _nobles.get(player.getObjectId());
		if (set == null || set.getInteger(POINTS) == 0)
			return 0;
		
		if (clear)
			set.set(POINTS, 0);
		
		int points = (player.isHero() || HeroManager.getInstance().isInactiveHero(player.getObjectId())) ? Config.OLY_HERO_POINTS : 0;
		
		switch (rankReward)
		{
			case 1:
				points += Config.OLY_RANK1_POINTS;
				break;
			case 2:
				points += Config.OLY_RANK2_POINTS;
				break;
			case 3:
				points += Config.OLY_RANK3_POINTS;
				break;
			case 4:
				points += Config.OLY_RANK4_POINTS;
				break;
			default:
				points += Config.OLY_RANK5_POINTS;
		}
		
		points *= Config.OLY_GP_PER_POINT;
		return points;
	}
	
	public int getLastNobleOlympiadPoints(int objId)
	{
		int result = 0;
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_MONTH_OLYMPIAD_POINTS))
		{
			ps.setInt(1, objId);
			
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
					result = rs.getInt("olympiad_points");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load last Olympiad points.", e);
		}
		return result;
	}
	
	protected void deleteNobles()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(TRUNCATE_OLYMPIAD_NOBLES))
		{
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't delete Olympiad nobles.", e);
		}
		_nobles.clear();
	}
	
	private void olympiadEnd()
	{
		World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED).addNumber(_currentCycle));
		
		if (_weeklyTask != null)
			_weeklyTask.cancel(true);
		
		saveNobleData();
		
		_period = OlympiadState.VALIDATION;
		
		HeroManager.getInstance().resetData();
		HeroManager.getInstance().computeNewHeroes();
		
		// Save current Olympiad status.
		saveOlympiadStatus();
		
		// Update monthly data.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(TRUNCATE_MONTH_OLYMPIAD);
			PreparedStatement ps2 = con.prepareStatement(INSERT_MONTH_OLYMPIAD))
		{
			ps.execute();
			ps2.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't update monthly Olympiad nobles.", e);
		}
		
		// Process rank rewards AFTER updating monthly data.
		processRankRewards();
		
		_validationEnd = Calendar.getInstance().getTimeInMillis() + Config.OLY_VPERIOD;
		_validationEndTask = ThreadPool.schedule(this::validationEnd, getMillisToValidationEnd());
	}
	
	private void validationEnd()
	{
		_period = OlympiadState.COMPETITION;
		_currentCycle++;
		
		deleteNobles();
		setNewOlympiadEnd();
		init();
	}
	
	public static Olympiad getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Olympiad INSTANCE = new Olympiad();
	}
}