package net.sf.l2j.gameserver.model.entity.events.capturetheflag;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;

public class CTFManager
{
	private static final CLogger LOGGER = new CLogger(CTFManager.class.getName());

	/**
	 * Task for event cycles<br>
	 */
	private CTFStartTask _task;

	/**
	 * New instance only by getInstance()<br>
	 */
	protected CTFManager()
	{
		if (Config.CTF_EVENT_ENABLED)
		{
			// Cannot start if both teams have same name
			if (Config.CTF_EVENT_TEAM_1_NAME != Config.CTF_EVENT_TEAM_2_NAME)
			{
				CTFEvent.init();
				scheduleEventStart();
				LOGGER.info("Capture The Flag Engine: is Started.");
			}
			else
			{
				LOGGER.info("Capture The Flag Engine: is uninitiated. Cannot start if both teams have same name!");
			}
		}
		else
		{
			LOGGER.info("Capture The Flag Engine: is disabled.");
		}
	}

	/**
	 * Starts CTFStartTask
	 */
	public void scheduleEventStart()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;

			for (String timeOfDay : Config.CTF_EVENT_INTERVAL)
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
				_task = new CTFStartTask(nextStartTime.getTimeInMillis());
				ThreadPool.execute(_task);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("CTFEventEngine[CTFManager.scheduleEventStart()]: Error figuring out a start time. Check CTFEventInterval in config file.");
		}
	}

	/**
	 * Method to start participation
	 */
	public void startReg()
	{
		if (!CTFEvent.startParticipation())
		{
			World.announceToOnlinePlayers("CTF Event: Event was cancelled.");
			scheduleEventStart();
		}
		else
		{
			World.announceToOnlinePlayers("CTF Event: Joinable in " + Config.CTF_NPC_LOC_NAME + "!");

			if (Config.EVENT_COMMANDS)
			{
				World.announceToOnlinePlayers("CTF Event: Command: .ctfjoin / .ctfleave / .ctfinfo");
			}

			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + (60000L * Config.CTF_EVENT_PARTICIPATION_TIME));
			ThreadPool.execute(_task);
		}
	}

	/**
	 * Method to start the fight
	 */
	public void startEvent()
	{
		if (!CTFEvent.startFight())
		{
			World.announceToOnlinePlayers("CTF Event: Event cancelled due to lack of Participation.");
			scheduleEventStart();
		}
		else
		{
			CTFEvent.sysMsgToAllParticipants("Teleporting in " + Config.CTF_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + (60000L * Config.CTF_EVENT_RUNNING_TIME));
			ThreadPool.execute(_task);
		}
	}

	/**
	 * Method to end the event and reward
	 */
	public void endEvent()
	{
		World.announceToOnlinePlayers(CTFEvent.calculateRewards());
		CTFEvent.sysMsgToAllParticipants("Teleporting back town in " + Config.CTF_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		CTFEvent.stopFight();

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
	 * Class for CTF cycles
	 */
	class CTFStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;

		public CTFStartTask(long startTime)
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
			if (CTFEvent.isInactive())
			{
				startReg();
			}
			else if (CTFEvent.isParticipating())
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
				if (CTFEvent.isParticipating())
				{
					World.announceToOnlinePlayers("CTF Event: " + (time / 60 / 60) + " hour(s) until registration is closed!");
				}
				else if (CTFEvent.isStarted())
				{
					CTFEvent.sysMsgToAllParticipants((time / 60 / 60) + " hour(s) until event is finished!");
				}
			}
			else if (time >= 60)
			{
				if (CTFEvent.isParticipating())
				{
					World.announceToOnlinePlayers("CTF Event: " + (time / 60) + " minute(s) until registration is closed!");
				}
				else if (CTFEvent.isStarted())
				{
					CTFEvent.sysMsgToAllParticipants((time / 60) + " minute(s) until the event is finished!");
				}
			}
			else if (CTFEvent.isParticipating())
			{
				World.announceToOnlinePlayers("CTF Event: " + time + " second(s) until registration is closed!");
			}
			else if (CTFEvent.isStarted())
			{
				CTFEvent.sysMsgToAllParticipants(time + " second(s) until the event is finished!");
			}
		}
	}

	public static final CTFManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final CTFManager INSTANCE = new CTFManager();
	}
}