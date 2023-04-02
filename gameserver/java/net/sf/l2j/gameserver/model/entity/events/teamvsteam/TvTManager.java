package net.sf.l2j.gameserver.model.entity.events.teamvsteam;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;

public class TvTManager
{
	private static final CLogger LOGGER = new CLogger(TvTManager.class.getName());

	/**
	 * Task for event cycles<br>
	 */
	private TvTStartTask _task;

	/**
	 * New instance only by getInstance()<br>
	 */
	protected TvTManager()
	{
		if (Config.TVT_EVENT_ENABLED)
		{
			TvTEvent.init();

			scheduleEventStart();
			LOGGER.info("Team vs Team Engine: is Started.");
		}
		else
		{
			LOGGER.info("Team vs Team Engine: is disabled.");
		}
	}

	/**
	 * Starts TvTStartTask
	 */
	public void scheduleEventStart()
	{
		try
		{
			final Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;

			for (String timeOfDay : Config.TVT_EVENT_INTERVAL)
			{
				// Creating a Calendar object from the specified interval value
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);

				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));

				// If the date is in the past, make it the next day (Example: Checking for "1:00", when the time is 23:57.)
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}

				// Check for the test date to be the minimum (smallest in the specified list)
				if ((nextStartTime == null) || (testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis()))
				{
					nextStartTime = testStartTime;
				}
			}

			if (nextStartTime != null)
			{
				_task = new TvTStartTask(nextStartTime.getTimeInMillis());
				ThreadPool.execute(_task);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("TvTEventEngine: Error figuring out a start time. Check TvTEventInterval in config file.");
		}
	}

	/**
	 * Method to start participation
	 */
	public void startReg()
	{
		if (!TvTEvent.startParticipation())
		{
			World.announceToOnlinePlayers("Team vs Team: Event was cancelled.");
			scheduleEventStart();
		}
		else
		{
			World.announceToOnlinePlayers("Team vs Team: Joinable in " + Config.TVT_NPC_LOC_NAME + "!");

			if (Config.EVENT_COMMANDS)
			{
				World.announceToOnlinePlayers("Team vs Team: Command: .tvtjoin / .tvtleave / .tvtinfo");
			}

			if (Config.ALLOW_TVT_DLG)
			{
				for (Player players : World.getInstance().getPlayers())
				{
					try
					{
						if (OlympiadManager.getInstance().isRegistered(players) || players.isAlikeDead() || players.isTeleporting() || players.isDead() || players.isInObserverMode() || players.isInStoreMode() || TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(players.getObjectId()) || players.isInsideZone(ZoneId.CASTLE) || players.isInsideZone(ZoneId.SIEGE) || players.getClassId() == ClassId.BISHOP || players.getClassId() == ClassId.CARDINAL || players.getClassId() == ClassId.SHILLIEN_ELDER || players.getClassId() == ClassId.SHILLIEN_SAINT || players.getClassId() == ClassId.ELVEN_ELDER || players.getClassId() == ClassId.EVAS_SAINT || players.getClassId() == ClassId.PROPHET || players.getClassId() == ClassId.HIEROPHANT)
						{
							continue;
						}

						ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_S1);
						confirm.addString("Do you wish to Join TvT Event?");
						confirm.addTime(45000);
						confirm.addRequesterId(players.getObjectId());
						players.sendPacket(confirm);
						players = null;
					}
					catch (Throwable e)
					{
						e.printStackTrace();
					}
				}
			}

			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + (60000L * Config.TVT_EVENT_PARTICIPATION_TIME));
			ThreadPool.execute(_task);
		}
	}

	/**
	 * Method to start the fight
	 */
	public void startEvent()
	{
		if (!TvTEvent.startFight())
		{
			World.announceToOnlinePlayers("Team vs Team: Event cancelled due to lack of Participation.");
			scheduleEventStart();
		}
		else
		{
			TvTEvent.sysMsgToAllParticipants("Teleporting participants in " + Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + (60000L * Config.TVT_EVENT_RUNNING_TIME));
			ThreadPool.execute(_task);
		}
	}

	/**
	 * Method to end the event and reward
	 */
	public void endEvent()
	{
		World.announceToOnlinePlayers(TvTEvent.calculateRewards());
		TvTEvent.sysMsgToAllParticipants("Teleporting back to town in " + Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		TvTEvent.stopFight();

		scheduleEventStart();
	}

	public void skipDelay()
	{
		if (_task.nextRun.cancel(false))
		{
			_task.setStartTime(System.currentTimeMillis());
			ThreadPool.execute(_task);
		}
	}

	/**
	 * Class for TvT cycles
	 */
	class TvTStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;

		public TvTStartTask(long startTime)
		{
			_startTime = startTime;
		}

		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}

		@Override
		public void run()
		{
			int delay = (int) Math.round((_startTime - System.currentTimeMillis()) / 1000.0);

			if (delay > 0)
			{
				announce(delay);
			}

			int nextMsg = 0;
			if (delay > 3600)
			{
				nextMsg = delay - 3600;
			}
			else if (delay > 1800)
			{
				nextMsg = delay - 1800;
			}
			else if (delay > 900)
			{
				nextMsg = delay - 900;
			}
			else if (delay > 600)
			{
				nextMsg = delay - 600;
			}
			else if (delay > 300)
			{
				nextMsg = delay - 300;
			}
			else if (delay > 60)
			{
				nextMsg = delay - 60;
			}
			else if (delay > 5)
			{
				nextMsg = delay - 5;
			}
			else if (delay > 0)
			{
				nextMsg = delay;
			}
			else // start
			if (TvTEvent.isInactive())
			{
				startReg();
			}
			else if (TvTEvent.isParticipating())
			{
				startEvent();
			}
			else
			{
				endEvent();
			}

			if (delay > 0)
			{
				nextRun = ThreadPool.schedule(this, nextMsg * 1000);
			}
		}

		private void announce(long time)
		{
			if ((time >= 3600) && ((time % 3600) == 0))
			{
				if (TvTEvent.isParticipating())
				{
					World.announceToOnlinePlayers("Team vs Team: " + (time / 60 / 60) + " hour(s) until registration is closed!");
				}
				else if (TvTEvent.isStarted())
				{
					TvTEvent.sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
				}
			}
			else if (time >= 60)
			{
				if (TvTEvent.isParticipating())
				{
					World.announceToOnlinePlayers("Team vs Team: " + (time / 60) + " minute(s) until registration is closed!");
				}
				else if (TvTEvent.isStarted())
				{
					TvTEvent.sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
				}
			}
			else if (TvTEvent.isParticipating())
			{
				World.announceToOnlinePlayers("Team vs Team: " + time + " second(s) until registration is closed!");
			}
			else if (TvTEvent.isStarted())
			{
				TvTEvent.sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
			}
		}
	}

	public static final TvTManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final TvTManager INSTANCE = new TvTManager();
	}
}
