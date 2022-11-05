package net.sf.l2j.gameserver.scripting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import net.sf.l2j.commons.data.MemoSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowQuestMark;
import net.sf.l2j.gameserver.network.serverpackets.QuestList;

/**
 * A container holding one {@link Player}'s {@link Quest} progress. It extends {@link MemoSet}.<br>
 * <br>
 * The main variables for a {@link QuestState} are :
 * <ul>
 * <li>state : the current state of the {@link Quest}, which can be CREATED, STARTED, COMPLETED ;</li>
 * <li>flags : allow to bypass one or multiple client-side {@link Quest} log(s) ;</li>
 * <li>cond : help server-side to trigger events, help client-side to show the correct {@link Quest} log.</li>
 * </ul>
 */
public final class QuestState extends MemoSet
{
	private static final long serialVersionUID = 1L;
	
	protected static final CLogger LOGGER = new CLogger(QuestState.class.getName());
	
	private static final String QUEST_SET_VAR = "INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=VALUES(value)";
	private static final String QUEST_DEL_VAR = "DELETE FROM character_quests WHERE charId=? AND name=? AND var=?";
	private static final String QUEST_DELETE = "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String QUEST_COMPLETE = "DELETE FROM character_quests WHERE charId=? AND name=? AND var<>'<state>'";
	
	public static final String COND = "<cond>";
	public static final String FLAGS = "<flags>";
	public static final String STATE = "<state>";
	
	private final Player _player;
	private final Quest _quest;
	
	/**
	 * Constructor of the new {@link Player}'s {@link QuestState} with {@link QuestStatus} CREATED.
	 * @param quest : The {@link Quest} associated with the {@link QuestState}.
	 * @param player : The {@link Player} carrying the {@link Quest}.
	 */
	public QuestState(Player player, Quest quest)
	{
		_player = player;
		_quest = quest;
		
		put(STATE, String.valueOf(QuestStatus.CREATED));
		
		_player.getQuestList().add(this);
	}
	
	@Override
	public boolean getBool(final String key)
	{
		return getBool(key, false);
	}
	
	@Override
	public int getInteger(final String key)
	{
		return getInteger(key, 0);
	}
	
	@Override
	public long getLong(final String key)
	{
		return getLong(key, 0);
	}
	
	@Override
	public double getDouble(final String key)
	{
		return getDouble(key, 0);
	}
	
	@Override
	protected void onSet(String key, String value)
	{
		// Set variable to database.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(QUEST_SET_VAR))
		{
			ps.setInt(1, _player.getObjectId());
			ps.setString(2, _quest.getName());
			ps.setString(3, key);
			ps.setString(4, value);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't set quest {} variable {}.", e, _quest.getName(), key);
		}
	}
	
	@Override
	protected void onUnset(String key)
	{
		// Remove variable from database.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(QUEST_DEL_VAR))
		{
			ps.setInt(1, _player.getObjectId());
			ps.setString(2, _quest.getName());
			ps.setString(3, key);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't remove quest {} variable {}.", e, _quest.getName(), key);
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof QuestState))
			return false;
		
		if (!_quest.isRealQuest())
			return false;
		
		final QuestState qs = (QuestState) o;
		if (_player != qs._player)
			return false;
		
		return _quest.getQuestId() == qs._quest.getQuestId();
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(_player.getObjectId(), _quest.getQuestId());
	}
	
	/**
	 * Load variable and value from SQL data row.
	 * @param rs : The loaded {@link ResultSet} carrying variable and value data.
	 * @throws SQLException : When value reading fails.
	 */
	public void loadFromDB(ResultSet rs) throws SQLException
	{
		// Get variable and value and check valid.
		final String variable = rs.getString("var");
		final String value = rs.getString("value");
		if (variable == null || variable.isEmpty() || value == null || value.isEmpty())
			return;
		
		// Parse special variable types.
		put(variable, value);
	}
	
	/**
	 * @return The {@link Player} associated to this {@link QuestState}.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * @return The {@link Quest} associated to this {@link QuestState}.
	 */
	public Quest getQuest()
	{
		return _quest;
	}
	
	/**
	 * @return The {@link QuestStatus} associated to this {@link QuestState}.
	 */
	public QuestStatus getState()
	{
		return getEnum(STATE, QuestStatus.class, null);
	}
	
	/**
	 * @return True if the {@link QuestState} is under QuestStatus.CREATED, or false otherwise.
	 */
	public boolean isCreated()
	{
		return (getState() == QuestStatus.CREATED);
	}
	
	/**
	 * @return True if the {@link QuestState} is under QuestStatus.COMPLETED, or false otherwise.
	 */
	public boolean isCompleted()
	{
		return (getState() == QuestStatus.COMPLETED);
	}
	
	/**
	 * @return True if the {@link QuestState} is under QuestStatus.STARTED, or false otherwise.
	 */
	public boolean isStarted()
	{
		return (getState() == QuestStatus.STARTED);
	}
	
	/**
	 * Set the {@link QuestStatus} of the {@link QuestState}.
	 * @param state : The desired {@link QuestStatus}.
	 */
	public void setState(QuestStatus state)
	{
		// Get current state and skip of no change.
		if (getState() == state)
			return;
		
		// Set new state.
		set(STATE, state);
	}
	
	/**
	 * @return The condition value of the {@link Player}'s {@link Quest}.
	 */
	public int getCond()
	{
		return getInteger(COND, 0);
	}
	
	/**
	 * Set condition value of the {@link Player}'s {@link Quest}.<br>
	 * Internally handles the progression of the quest so that it is ready for sending appropriate packets to the client<BR>
	 * <BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Check if the new progress number resets the quest to a previous (smaller) step</LI>
	 * <LI>If not, check if quest progress steps have been skipped</LI>
	 * <LI>If skipped, prepare the variable completedStateFlags appropriately to be ready for sending to clients</LI>
	 * <LI>If no steps were skipped, flags do not need to be prepared...</LI>
	 * <LI>If the passed step resets the quest to a previous step, reset such that steps after the parameter are not considered, while skipped steps before the parameter, if any, maintain their info</LI>
	 * @param cond : int indicating the step number for the current quest progress (as will be shown to the client)
	 */
	public void setCond(int cond)
	{
		// Get current condition and skip of no change.
		final int previous = getCond();
		if (cond == previous)
			return;
		
		// Set new condition.
		set(COND, cond);
		
		// cond 0 and 1 do not need completedStateFlags. Also, if cond > 1, the 1st step must
		// always exist (i.e. it can never be skipped). So if cond is 2, we can still safely
		// assume no steps have been skipped.
		// Finally, more than 31 steps CANNOT be supported in any way with skipping.
		int flags = 0;
		if (cond < 3 || cond > 31)
			unset(FLAGS);
		else
			flags = getInteger(FLAGS, 0);
		
		// case 1: No steps have been skipped so far...
		if (flags == 0)
		{
			// check if this step also doesn't skip anything. If so, no further work is needed
			// also, in this case, no work is needed if the state is being reset to a smaller value
			// in those cases, skip forward to informing the client about the change...
			
			// ELSE, if we just now skipped for the first time...prepare the flags!!!
			if (cond > (previous + 1))
			{
				// set the most significant bit to 1 (indicates that there exist skipped states)
				// also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter
				// what the cond says)
				flags = 0x80000001;
				
				// since no flag had been skipped until now, the least significant bits must all
				// be set to 1, up until "old" number of bits.
				flags |= ((1 << previous) - 1);
				
				// now, just set the bit corresponding to the passed cond to 1 (current step)
				flags |= (1 << (cond - 1));
				
				set(FLAGS, flags);
			}
		}
		// case 2: There were exist previously skipped steps
		else
		{
			// if this is a push back to a previous step, clear all completion flags ahead
			if (cond < previous)
			{
				flags &= ((1 << cond) - 1); // note, this also unsets the flag indicating that there exist skips
				
				// now, check if this resulted in no steps being skipped any more
				if (flags == ((1 << cond) - 1))
					unset(FLAGS);
				else
				{
					// set the most significant bit back to 1 again, to correctly indicate that this skips states.
					// also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter
					// what the cond says)
					flags |= 0x80000001;
					
					set(FLAGS, flags);
				}
			}
			// if this moves forward, it changes nothing on previously skipped steps...so just mark this
			// state and we are done
			else
			{
				flags |= (1 << (cond - 1));
				
				set(FLAGS, flags);
			}
		}
		
		if (_quest.isRealQuest())
		{
			_player.sendPacket(new QuestList(_player));
			_player.sendPacket(new ExShowQuestMark(_quest.getQuestId()));
		}
	}
	
	/**
	 * @return The {@link Quest}'s flags or condition values.
	 */
	public int getFlags()
	{
		// Return quest flags, if present. Otherwise return quest condition.
		return getInteger(FLAGS, getCond());
	}
	
	/**
	 * Exit a {@link Quest}, destroying elements used by it (quest items, and progression).
	 * @param repeatable : If true, we delete progression and if false, we set the {@link QuestStatus} to QuestStatus.COMPLETED.
	 */
	public void exitQuest(boolean repeatable)
	{
		if (!isStarted())
			return;
		
		// Remove quest variables.
		clear();
		
		// Remove/Complete quest.
		if (repeatable)
			_player.getQuestList().remove(this);
		else
			setState(QuestStatus.COMPLETED);
		
		if (_quest.isRealQuest())
			_player.sendPacket(new QuestList(_player));
		
		// Remove registered quest items.
		int[] itemIds = _quest.getItemsIds();
		if (itemIds != null)
		{
			for (int itemId : itemIds)
				Quest.takeItems(_player, itemId, -1);
		}
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement((repeatable) ? QUEST_DELETE : QUEST_COMPLETE))
		{
			ps.setInt(1, _player.getObjectId());
			ps.setString(2, _quest.getName());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't delete quest.", e);
		}
	}
}