package net.sf.l2j.gameserver.model.entity.events.deathmatch;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;

public class DMManager
{
	private static final CLogger LOGGER = new CLogger(DMManager.class.getName());

	private DMStartTask _task;

	private DMManager()
	{
		if (Config.DM_EVENT_ENABLED)
		{
			DMEvent.init();

			scheduleEventStart();
			LOGGER.info("Deathmatch Engine: is Started.");
		}
		else
		{
			LOGGER.info("Deathmatch Engine: Engine is disabled.");
		}
	}

	public static DMManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public void scheduleEventStart()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			for (String timeOfDay : Config.DM_EVENT_INTERVAL)
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
				if (nextStartTime == null || testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis())
				{
					nextStartTime = testStartTime;
				}
			}

			if (nextStartTime != null)
			{
				_task = new DMStartTask(nextStartTime.getTimeInMillis());
				ThreadPool.execute(_task);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("DMEventEngine: Error figuring out a start time. Check DMEventInterval in config file.");
		}
	}

	public void startReg()
	{
		if (!DMEvent.startParticipation())
		{
			World.announceToOnlinePlayers("Deathmatch: Event was cancelled.");
			scheduleEventStart();
		}
		else
		{
			World.announceToOnlinePlayers("Deathmatch: Joinable in " + Config.DM_NPC_LOC_NAME + "!");

			if (Config.EVENT_COMMANDS)
			{
				World.announceToOnlinePlayers("Deathmatch: Command: .dmjoin / .dmleave / .dminfo");
			}

			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + Config.DM_EVENT_PARTICIPATION_TIME);
			ThreadPool.execute(_task);
		}
	}

	public void startEvent()
	{
		if (!DMEvent.startFight())
		{
			World.announceToOnlinePlayers("Deathmatch: Event cancelled due to lack of Participation.");
			scheduleEventStart();
		}
		else
		{
			DMEvent.sysMsgToAllParticipants("Teleporting in " + Config.DM_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + 60000L * Config.DM_EVENT_RUNNING_TIME);
			ThreadPool.execute(_task);
		}
	}

	public void endEvent()
	{
		World.announceToOnlinePlayers(DMEvent.calculateRewards());
		DMEvent.sysMsgToAllParticipants("Teleporting back town in " + Config.DM_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		DMEvent.stopFight();

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

	class DMStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;

		public DMStartTask(long startTime)
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
			if (DMEvent.isInactive())
			{
				startReg();
			}
			else if (DMEvent.isParticipating())
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
			if (time >= 3600 && time % 3600 == 0)
			{
				if (DMEvent.isParticipating())
				{
					World.announceToOnlinePlayers("Deathmatch: " + (time / 60 / 60) + " hour(s) until registration is closed!");
				}
				else if (DMEvent.isStarted())
				{
					DMEvent.sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
				}
			}
			else if (time >= 60)
			{
				if (DMEvent.isParticipating())
				{
					World.announceToOnlinePlayers("Deathmatch: " + (time / 60) + " minute(s) until registration is closed!");
				}
				else if (DMEvent.isStarted())
				{
					DMEvent.sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
				}
			}
			else if (DMEvent.isParticipating())
			{
				World.announceToOnlinePlayers("Deathmatch: " + time + " second(s) until registration is closed!");
			}
			else if (DMEvent.isStarted())
			{
				DMEvent.sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
			}
		}
	}

	private static class SingletonHolder
	{
		protected static final DMManager _instance = new DMManager();
	}
}