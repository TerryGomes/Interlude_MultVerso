package net.sf.l2j.gameserver.model.entity.events.teamvsteam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.AntiFeedManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.EventState;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.skills.L2Skill;

public class TvTEvent
{
	private static final CLogger LOGGER = new CLogger(TvTEvent.class.getName());

	/** html path **/
	private static final String htmlPath = "mods/events/tvt/";

	/** The teams of the TvTEvent */
	private static TvTEventTeam[] _teams = new TvTEventTeam[2];

	/** The state of the TvTEvent */
	private static EventState _state = EventState.INACTIVE;

	/** The spawn of the participation npc */
	private static Spawn _npcSpawn = null;

	/** the npc instance of the participation npc */
	private static Npc _lastNpcSpawn = null;

	/**
	 * No instance of this class!
	 */
	private TvTEvent()
	{
	}

	/**
	 * Teams initializing
	 */
	public static void init()
	{
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.TVT_ID);
		_teams[0] = new TvTEventTeam(Config.TVT_EVENT_TEAM_1_NAME, Config.TVT_EVENT_TEAM_1_COORDINATES);
		_teams[1] = new TvTEventTeam(Config.TVT_EVENT_TEAM_2_NAME, Config.TVT_EVENT_TEAM_2_COORDINATES);
	}

	/**
	 * Starts the participation of the TvTEvent 1. Get L2NpcTemplate by TvTProperties.TVT_EVENT_PARTICIPATION_NPC_ID 2. Try to spawn a new npc of it
	 * @return boolean: true if success, otherwise false
	 */
	public static boolean startParticipation()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.TVT_EVENT_PARTICIPATION_NPC_ID);

		if (tmpl == null)
		{
			LOGGER.warn("TvTEventEngine: L2EventManager is a NullPointer -> Invalid npc id in configs?");
			return false;
		}

		try
		{
			_npcSpawn = new Spawn(tmpl);

			_npcSpawn.setLoc(Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0], Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1], Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2], Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(1);

			// later no need to delete spawn from db, we don't store it (false)
			SpawnManager.getInstance().addSpawn(_npcSpawn);

			_lastNpcSpawn = _npcSpawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.warn("TvTEventEngine: exception: " + e.getMessage(), e);
			return false;
		}

		setState(EventState.PARTICIPATING);
		return true;
	}

	private static int highestLevelPcInstanceOf(Map<Integer, Player> players)
	{
		int maxLevel = Integer.MIN_VALUE, maxLevelId = -1;
		for (Player player : players.values())
		{
			if (player.getStatus().getLevel() >= maxLevel)
			{
				maxLevel = player.getStatus().getLevel();
				maxLevelId = player.getObjectId();
			}
		}
		return maxLevelId;
	}

	/**
	 * Starts the TvTEvent fight 1. Set state EventState.STARTING 2. Close doors specified in configs 3. Abort if not enought participants(return false) 4. Set state EventState.STARTED 5. Teleport all participants to team spot
	 * @return boolean: true if success, otherwise false
	 */
	public static boolean startFight()
	{
		// Set state to STARTING
		setState(EventState.STARTING);

		// Randomize and balance team distribution
		Map<Integer, Player> allParticipants = new HashMap<>();
		allParticipants.putAll(_teams[0].getParticipatedPlayers());
		allParticipants.putAll(_teams[1].getParticipatedPlayers());
		_teams[0].cleanMe();
		_teams[1].cleanMe();

		int balance[] =
		{
			0,
			0
		}, priority = 0, highestLevelPlayerId;

		Player highestLevelPlayer;
		// XXX: allParticipants should be sorted by level instead of using highestLevelPcInstanceOf for every fetch
		while (!allParticipants.isEmpty())
		{
			// Priority team gets one player
			highestLevelPlayerId = highestLevelPcInstanceOf(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getStatus().getLevel();

			// Exiting if no more players
			if (allParticipants.isEmpty())
			{
				break;
			}
			
			// The other team gets one player
			// XXX: Code not dry
			priority = 1 - priority;
			highestLevelPlayerId = highestLevelPcInstanceOf(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getStatus().getLevel();

			// Recalculating priority
			priority = balance[0] > balance[1] ? 1 : 0;
		}

		// Check for enought participants
		if ((_teams[0].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS) || (_teams[1].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS))
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);

			// Cleanup of teams
			_teams[0].cleanMe();
			_teams[1].cleanMe();

			// Unspawn the event NPC
			unSpawnNpc();
			AntiFeedManager.getInstance().clear(AntiFeedManager.TVT_ID);
			return false;
		}

		// Closes all doors specified in configs for tvt
		closeDoors(Config.TVT_DOORS_IDS_TO_CLOSE);

		// Set state STARTED
		setState(EventState.STARTED);

		// Iterate over all teams
		for (TvTEventTeam team : _teams)
		{
			// Iterate over all participated player instances in this team
			for (Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
				{
					// Teleporter implements Runnable and starts itself
					new TvTEventTeleporter(player, team.getCoordinates(), false, false);
					if (Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("title") || Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
					{
						player._originalTitle = player.getTitle();
						player.setTitle("Kills: " + player.getPointScore());
						player.broadcastTitleInfo();
					}
					player.sendPacket(new ExShowScreenMessage("TvT Afk system is started, if you stay Afk you will be kicked!", 6000));
				}
			}
		}

		return true;
	}

	/**
	 * Calculates the TvTEvent reward 1. If both teams are at a tie(points equals), send it as system message to all participants, if one of the teams have 0 participants left online abort rewarding 2. Wait till teams are not at a tie anymore 3. Set state EvcentState.REWARDING 4. Reward team with
	 * more points 5. Show win html to wining team participants
	 * @return String: winning team name
	 */
	public static String calculateRewards()
	{
		if (_teams[0].getPoints() == _teams[1].getPoints())
		{
			// Check if one of the teams have no more players left
			if ((_teams[0].getParticipatedPlayerCount() == 0) || (_teams[1].getParticipatedPlayerCount() == 0))
			{
				// set state to rewarding
				setState(EventState.REWARDING);
				// return here, the fight can't be completed
				return "Team vs Team: Event has ended. No team won due to inactivity!";
			}

			// Both teams have equals points
			sysMsgToAllParticipants("Event has ended, both teams have tied.");
			if (Config.TVT_REWARD_TEAM_TIE)
			{
				rewardTeam(_teams[0]);
				rewardTeam(_teams[1]);
				return "Team vs Team: Event has ended with both teams tying.";
			}
			return "Team vs Team: Event has ended with both teams tying.";
		}

		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);

		// Get team which has more points
		TvTEventTeam team = _teams[_teams[0].getPoints() > _teams[1].getPoints() ? 0 : 1];
		rewardTeam(team);
		return "Team vs Team: Event finish! Team " + team.getName() + " won with " + team.getPoints() + " kills!";
	}

	private static void rewardTeam(TvTEventTeam team)
	{
		// Iterate over all participated player instances of the winning team
		for (Player player : team.getParticipatedPlayers().values())
		{
			if (player == null)
			{
				continue;
			}

			// Checks if the player scored points.
			if (Config.TVT_REWARD_PLAYER)
			{
				if (!team.onScoredPlayer(player.getObjectId()))
				{
					continue;
				}
			}
			
			SystemMessage systemMessage = null;

			// Iterate over all tvt event rewards
			for (int[] reward : Config.TVT_EVENT_REWARDS)
			{
				PcInventory inv = player.getInventory();

				if (player.getPremServiceData() == 1)
				{
					// Check for stackable item, non stackabe items need to be added one by one
					if (ItemData.getInstance().getTemplate(reward[0]).isStackable())
					{
						inv.addItem("Team vs Team:", reward[0], (int) (reward[1] * Config.PREMIUM_RATE_DROP_ITEMS), player, player);

						if (reward[1] > 1)
						{
							systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
							systemMessage.addItemName(reward[0]);
							systemMessage.addItemNumber(reward[1]);
						}
						else
						{
							systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
							systemMessage.addItemName(reward[0]);
						}

						player.sendPacket(systemMessage);
					}
					else
					{
						for (int i = 0; i < reward[1]; ++i)
						{
							inv.addItem("Team vs Team:", reward[0], (int) (1 * Config.PREMIUM_RATE_DROP_ITEMS), player, player);
							systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
							systemMessage.addItemName(reward[0]);
							player.sendPacket(systemMessage);
						}
					}
				}
				else if (ItemData.getInstance().getTemplate(reward[0]).isStackable())
				{
					inv.addItem("Team vs Team:", reward[0], reward[1], player, player);

					if (reward[1] > 1)
					{
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
						systemMessage.addItemName(reward[0]);
						systemMessage.addItemNumber(reward[1]);
					}
					else
					{
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						systemMessage.addItemName(reward[0]);
					}

					player.sendPacket(systemMessage);
				}
				else
				{
					for (int i = 0; i < reward[1]; ++i)
					{
						inv.addItem("Team vs Team:", reward[0], 1, player, player);
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						systemMessage.addItemName(reward[0]);
						player.sendPacket(systemMessage);
					}
				}
			}

			StatusUpdate statusUpdate = new StatusUpdate(player);
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

			statusUpdate.addAttribute(StatusType.CUR_LOAD, player.getCurrentWeight());
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Reward.htm"));
			player.sendPacket(statusUpdate);
			player.sendPacket(npcHtmlMessage);
		}
	}

	/**
	 * Stops the TvTEvent fight 1. Set state EventState.INACTIVATING 2. Remove tvt npc from world 3. Open doors specified in configs 4. Teleport all participants back to participation npc location 5. Teams cleaning 6. Set state EventState.INACTIVE
	 */
	public static void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);

		// Unspawn event npc
		unSpawnNpc();

		// Opens all doors specified in configs for tvt
		openDoors(Config.TVT_DOORS_IDS_TO_CLOSE);

		// Iterate over all teams
		for (TvTEventTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				// Check for nullpointer
				if (player != null)
				{
					new TvTEventTeleporter(player, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
					if (Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("title") || Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
					{
						ThreadPool.schedule(new Runnable()
						{
							@Override
							public void run()
							{
								player.setTitle(player._originalTitle);
								player.broadcastTitleInfo();
								player.clearPoints();
							}
						}, Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY * 1000);
					}
				}
			}
		}

		// Cleanup of teams
		_teams[0].cleanMe();
		_teams[1].cleanMe();

		// Set state INACTIVE
		setState(EventState.INACTIVE);
		AntiFeedManager.getInstance().clear(AntiFeedManager.TVT_ID);
	}

	/**
	 * Adds a player to a TvTEvent team 1. Calculate the id of the team in which the player should be added 2. Add the player to the calculated team
	 * @param player as Player
	 * @return boolean: true if success, otherwise false
	 */
	public static synchronized boolean addParticipant(Player player)
	{
		if (player == null)
		{
			return false;
		}

		byte teamId = 0;

		// Check to which team the player should be added
		if (_teams[0].getParticipatedPlayerCount() == _teams[1].getParticipatedPlayerCount())
		{
			teamId = (byte) (Rnd.get(2));
		}
		else
		{
			teamId = (byte) (_teams[0].getParticipatedPlayerCount() > _teams[1].getParticipatedPlayerCount() ? 1 : 0);
		}

		return _teams[teamId].addPlayer(player);
	}

	/**
	 * Removes a TvTEvent player from it's team 1. Get team id of the player 2. Remove player from it's team
	 * @param objectId
	 * @return boolean: true if success, otherwise false
	 */
	public static boolean removeParticipant(int objectId)
	{
		// Get the teamId of the player
		byte teamId = getParticipantTeamId(objectId);

		// Check if the player is participant
		if (teamId != -1)
		{
			// Remove the player from team
			_teams[teamId].removePlayer(objectId);
			return true;
		}

		return false;
	}

	public static boolean payParticipationFee(Player player)
	{
		int itemId = Config.TVT_EVENT_PARTICIPATION_FEE[0];
		int itemNum = Config.TVT_EVENT_PARTICIPATION_FEE[1];
		if (itemId == 0 || itemNum == 0)
		{
			return true;
		}

		if (player.getInventory().getItemCount(itemId, -1) < itemNum)
		{
			return false;
		}

		return player.destroyItemByItemId("TvT Participation Fee", itemId, itemNum, _lastNpcSpawn, true);
	}

	public static String getParticipationFee()
	{
		int itemId = Config.TVT_EVENT_PARTICIPATION_FEE[0];
		int itemNum = Config.TVT_EVENT_PARTICIPATION_FEE[1];

		if ((itemId == 0) || (itemNum == 0))
		{
			return "-";
		}

		return ItemData.getInstance().getTemplate(itemId).getName();
	}

	/**
	 * Send a SystemMessage to all participated players 1. Send the message to all players of team number one 2. Send the message to all players of team number two
	 * @param message as String
	 */
	public static void sysMsgToAllParticipants(String message)
	{
		CreatureSay cs = new CreatureSay(0, SayType.PARTY, "Event Manager", message);

		for (Player player : _teams[0].getParticipatedPlayers().values())
		{
			if (player != null)
			{
				player.sendPacket(cs);
			}
		}

		for (Player player : _teams[1].getParticipatedPlayers().values())
		{
			if (player != null)
			{
				player.sendPacket(cs);
			}
		}
	}

	/**
	 * Close doors specified in configs
	 * @param doors
	 */
	private static void closeDoors(List<Integer> doors)
	{
		for (int doorId : doors)
		{
			Door door = DoorData.getInstance().getDoor(doorId);

			if (door != null)
			{
				door.closeMe();
			}
		}
	}

	/**
	 * Open doors specified in configs
	 * @param doors
	 */
	private static void openDoors(List<Integer> doors)
	{
		for (int doorId : doors)
		{
			Door door = DoorData.getInstance().getDoor(doorId);

			if (door != null)
			{
				door.openMe();
			}
		}
	}

	/**
	 * UnSpawns the TvTEvent npc
	 */
	private static void unSpawnNpc()
	{
		// Delete the npc
		_lastNpcSpawn.deleteMe();
		SpawnManager.getInstance().deleteSpawn((Spawn) _lastNpcSpawn.getSpawn());

		// Stop respawning of the npc
		_npcSpawn = null;
		_lastNpcSpawn = null;
	}

	/**
	 * Called when a player logs in
	 * @param player as Player
	 */
	public static void onLogin(Player player)
	{
		if ((player == null) || (!isStarting() && !isStarted()))
		{
			return;
		}

		byte teamId = getParticipantTeamId(player.getObjectId());

		if (teamId == -1)
		{
			return;
		}

		_teams[teamId].addPlayer(player);
		new TvTEventTeleporter(player, _teams[teamId].getCoordinates(), true, false);
	}

	/**
	 * Called when a player logs out
	 * @param player as Player
	 */
	public static void onLogout(Player player)
	{
		if ((player != null) && (isStarting() || isStarted() || isParticipating()))
		{
			if (removeParticipant(player.getObjectId()))
			{
				player.teleportTo((Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)) - 50, (Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)) - 50, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);
				player.setTeam(TeamType.NONE);
			}

			if (Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("title") || Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				player.setTitle(player._originalTitle);
				player.broadcastTitleInfo();
			}
		}
	}

	/**
	 * Called on every bypass by npc of type L2TvTEventNpc Needs synchronization cause of the max player check
	 * @param command as String
	 * @param player as Player
	 */
	public static synchronized void onBypass(String command, Player player)
	{
		if (player == null || !isParticipating())
		{
			return;
		}

		final String htmContent;

		if (command.equals("tvt_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			int playerLevel = player.getStatus().getLevel();

			if (player.isCursedWeaponEquipped())
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "CursedWeaponEquipped.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (OlympiadManager.getInstance().isRegistered(player))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Olympiad.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (player.getKarma() > 0)
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Karma.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (Config.DISABLE_ID_CLASSES_TVT.contains(player.getClassId().getId()))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Class.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if ((playerLevel < Config.TVT_EVENT_MIN_LVL) || (playerLevel > Config.TVT_EVENT_MAX_LVL))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(Config.TVT_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(Config.TVT_EVENT_MAX_LVL));
				}
			}
			else if ((_teams[0].getParticipatedPlayerCount() == Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS) && (_teams[1].getParticipatedPlayerCount() == Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "TeamsFull.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS));
				}
			}
			else if ((Config.TVT_EVENT_MAX_PARTICIPANTS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.TVT_ID, player, Config.TVT_EVENT_MAX_PARTICIPANTS_PER_IP))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "IPRestriction.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, Config.TVT_EVENT_MAX_PARTICIPANTS_PER_IP)));
				}
			}
			else if (!payParticipationFee(player))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "ParticipationFee.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%fee%", getParticipationFee());
				}
			}
			else if (addParticipant(player))
			{
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Registered.htm"));
			}
			else
			{
				return;
			}

			player.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("tvt_event_remove_participation"))
		{
			removeParticipant(player.getObjectId());

			if (Config.TVT_EVENT_MAX_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.TVT_ID, player);
			}

			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Unregistered.htm"));
			player.sendPacket(npcHtmlMessage);
		}
	}

	/**
	 * Called on every onAction in L2PcIstance
	 * @param player
	 * @param objectId
	 * @return boolean: true if player is allowed to target, otherwise false
	 */
	public static boolean onAction(Player player, int objectId)
	{
		if (player == null || !isStarted() || player.isGM())
		{
			return true;
		}

		byte playerTeamId = getParticipantTeamId(player.getObjectId());
		byte targetedPlayerTeamId = getParticipantTeamId(objectId);

		if (((playerTeamId != -1) && (targetedPlayerTeamId == -1)) || ((playerTeamId == -1) && (targetedPlayerTeamId != -1)))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if ((playerTeamId != -1) && (targetedPlayerTeamId != -1) && (playerTeamId == targetedPlayerTeamId) && (player.getObjectId() != objectId) && !Config.TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		return true;
	}

	/**
	 * Called on every scroll use
	 * @param objectId
	 * @return boolean: true if player is allowed to use scroll, otherwise false
	 */
	public static boolean onScrollUse(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId) && !Config.TVT_EVENT_SCROLL_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Called on every potion use
	 * @param objectId
	 * @return boolean: true if player is allowed to use potions, otherwise false
	 */
	public static boolean onPotionUse(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId) && !Config.TVT_EVENT_POTIONS_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Called on every escape use(thanks to nbd)
	 * @param objectId
	 * @return boolean: true if player is not in tvt event, otherwise false
	 */
	public static boolean onEscapeUse(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId))
		{
			return false;
		}

		return true;
	}

	/**
	 * Called on every summon item use
	 * @param objectId
	 * @return boolean: true if player is allowed to summon by item, otherwise false
	 */
	public static boolean onItemSummon(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId) && !Config.TVT_EVENT_SUMMON_BY_ITEM_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Is called when a player is killed
	 * @param killer as L2Character
	 * @param player as Player
	 */
	public static void onKill(Creature killer, Player player)
	{
		if (player == null || !isStarted())
		{
			return;
		}

		byte killedTeamId = getParticipantTeamId(player.getObjectId());

		if (killedTeamId == -1)
		{
			return;
		}

		new TvTEventTeleporter(player, _teams[killedTeamId].getCoordinates(), false, false);

		if (killer == null)
		{
			return;
		}

		if ((killer instanceof Pet) || (killer instanceof Summon))
		{
			player = ((Summon) killer).getOwner();

			if (player == null)
			{
				return;
			}
		}
		else if (killer instanceof Player)
		{
			player = (Player) killer;
		}
		else
		{
			return;
		}

		byte killerTeamId = getParticipantTeamId(player.getObjectId());
		if (killerTeamId != -1 && killedTeamId != -1 && killerTeamId != killedTeamId)
		{
			TvTEventTeam killerTeam = _teams[killerTeamId];

			killerTeam.increasePoints();
			killerTeam.increasePoints(player.getObjectId());
			player.setPvpKills(player.getPvpKills() + 1);
			player.sendPacket(new UserInfo(player));

			if (Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("pm"))
			{
				sysMsgToAllParticipants(player.getName() + " Hunted Player " + player.getName() + "!");
			}
			else if (Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("title"))
			{
				player.increasePointScore();
				player.setTitle("Kills: " + player.getPointScore());
				player.broadcastTitleInfo();
			}
			else if (Config.TVT_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				sysMsgToAllParticipants(player.getName() + " Hunted Player " + player.getName() + "!");
				player.increasePointScore();
				player.setTitle("Kills: " + player.getPointScore());
				player.broadcastTitleInfo();
			}
		}
	}

	/**
	 * Called on Appearing packet received (player finished teleporting)
	 * @param player
	 */
	public static void onTeleported(Player player)
	{
		if (!isStarted() || (player == null) || !isPlayerParticipant(player.getObjectId()))
		{
			return;
		}

		if (player.isMageClass())
		{
			if (Config.TVT_EVENT_MAGE_BUFFS != null && !Config.TVT_EVENT_MAGE_BUFFS.isEmpty())
			{
				for (int i : Config.TVT_EVENT_MAGE_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, Config.TVT_EVENT_MAGE_BUFFS.get(i));
					if (skill != null)
					{
						skill.getEffects(player, player);
					}
				}
			}
		}
		else if (Config.TVT_EVENT_FIGHTER_BUFFS != null && !Config.TVT_EVENT_FIGHTER_BUFFS.isEmpty())
		{
			for (int i : Config.TVT_EVENT_FIGHTER_BUFFS.keySet())
			{
				L2Skill skill = SkillTable.getInstance().getInfo(i, Config.TVT_EVENT_FIGHTER_BUFFS.get(i));
				if (skill != null)
				{
					skill.getEffects(player, player);
				}
			}
		}
		removeParty(player);

		// AFK started
		TvTAntiAfk.getInstance();
	}

	/**
	 * Sets the TvTEvent state
	 * @param state as EventState
	 */
	private static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}

	/**
	 * Is TvTEvent inactive?
	 * @return boolean: true if event is inactive(waiting for next event cycle), otherwise false
	 */
	public static boolean isInactive()
	{
		boolean isInactive;

		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}

		return isInactive;
	}

	/**
	 * Is TvTEvent in inactivating?
	 * @return boolean: true if event is in inactivating progress, otherwise false
	 */
	public static boolean isInactivating()
	{
		boolean isInactivating;

		synchronized (_state)
		{
			isInactivating = _state == EventState.INACTIVATING;
		}

		return isInactivating;
	}

	/**
	 * Is TvTEvent in participation?
	 * @return boolean: true if event is in participation progress, otherwise false
	 */
	public static boolean isParticipating()
	{
		boolean isParticipating;

		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}

		return isParticipating;
	}

	/**
	 * Is TvTEvent starting?
	 * @return boolean: true if event is starting up(setting up fighting spot, teleport players etc.), otherwise false
	 */
	public static boolean isStarting()
	{
		boolean isStarting;

		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}

		return isStarting;
	}

	/**
	 * Is TvTEvent started?
	 * @return boolean: true if event is started, otherwise false
	 */
	public static boolean isStarted()
	{
		boolean isStarted;

		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}

		return isStarted;
	}

	/**
	 * Is TvTEvent rewarding?
	 * @return boolean: true if event is currently rewarding, otherwise false
	 */
	public static boolean isRewarding()
	{
		boolean isRewarding;

		synchronized (_state)
		{
			isRewarding = _state == EventState.REWARDING;
		}

		return isRewarding;
	}

	/**
	 * Returns the team id of a player, if player is not participant it returns -1
	 * @param objectId
	 * @return byte: team name of the given playerName, if not in event -1
	 */
	public static byte getParticipantTeamId(int objectId)
	{
		return (byte) (_teams[0].containsPlayer(objectId) ? 0 : (_teams[1].containsPlayer(objectId) ? 1 : -1));
	}

	/**
	 * Returns the team of a player, if player is not participant it returns null
	 * @param objectId
	 * @return TvTEventTeam: team of the given playerObjectId, if not in event null
	 */
	public static TvTEventTeam getParticipantTeam(int objectId)
	{
		return (_teams[0].containsPlayer(objectId) ? _teams[0] : (_teams[1].containsPlayer(objectId) ? _teams[1] : null));
	}

	/**
	 * Returns the enemy team of a player, if player is not participant it returns null
	 * @param objectId
	 * @return TvTEventTeam: enemy team of the given playerObjectId, if not in event null
	 */
	public static TvTEventTeam getParticipantEnemyTeam(int objectId)
	{
		return (_teams[0].containsPlayer(objectId) ? _teams[1] : (_teams[1].containsPlayer(objectId) ? _teams[0] : null));
	}

	/**
	 * Returns the team coordinates in which the player is in, if player is not in a team return null
	 * @param objectId
	 * @return int[]: coordinates of teams, 2 elements, index 0 for team 1 and index 1 for team 2
	 */
	public static int[] getParticipantTeamCoordinates(int objectId)
	{
		return _teams[0].containsPlayer(objectId) ? _teams[0].getCoordinates() : (_teams[1].containsPlayer(objectId) ? _teams[1].getCoordinates() : null);
	}

	/**
	 * Is given player participant of the event?
	 * @param objectId
	 * @return boolean: true if player is participant, ohterwise false
	 */
	public static boolean isPlayerParticipant(int objectId)
	{
		if (!isParticipating() && !isStarting() && !isStarted())
		{
			return false;
		}

		return _teams[0].containsPlayer(objectId) || _teams[1].containsPlayer(objectId);
	}

	/**
	 * Returns participated player count
	 * @return int: amount of players registered in the event
	 */
	public static int getParticipatedPlayersCount()
	{
		if (!isParticipating() && !isStarting() && !isStarted())
		{
			return 0;
		}

		return _teams[0].getParticipatedPlayerCount() + _teams[1].getParticipatedPlayerCount();
	}

	/**
	 * Returns teams names
	 * @return String[]: names of teams, 2 elements, index 0 for team 1 and index 1 for team 2
	 */
	public static String[] getTeamNames()
	{
		return new String[]
		{
			_teams[0].getName(),
			_teams[1].getName()
		};
	}

	/**
	 * Returns player count of both teams
	 * @return int[]: player count of teams, 2 elements, index 0 for team 1 and index 1 for team 2
	 */
	public static int[] getTeamsPlayerCounts()
	{
		return new int[]
		{
			_teams[0].getParticipatedPlayerCount(),
			_teams[1].getParticipatedPlayerCount()
		};
	}

	/**
	 * Returns points count of both teams
	 * @return int[]: points of teams, 2 elements, index 0 for team 1 and index 1 for team 2
	 */
	public static int[] getTeamsPoints()
	{
		return new int[]
		{
			_teams[0].getPoints(),
			_teams[1].getPoints()
		};
	}

	public static void removeParty(Player player)
	{
		if (player.getParty() != null)
		{
			Party party = player.getParty();
			party.removePartyMember(player, MessageType.LEFT);
		}
	}

	public static List<Player> allParticipants()
	{
		List<Player> players = new ArrayList<>();
		players.addAll(_teams[0].getParticipatedPlayers().values());
		players.addAll(_teams[1].getParticipatedPlayers().values());
		return players;
	}

	public static class TvTAntiAfk
	{
		// Delay between location checks , Default 60000 ms (1 minute)
		private final int checkDelay = 60000;

		private static ArrayList<String> TvTPlayerList = new ArrayList<>();
		private static String[] splitter;
		private static int x, y, z, sameLoc;
		private static Player _player;

		private TvTAntiAfk()
		{
			ThreadPool.scheduleAtFixedRate(new AntiAfk(), 20000, checkDelay);
		}

		private class AntiAfk implements Runnable
		{
			@Override
			public void run()
			{
				if (TvTEvent.isStarted())
				{
					synchronized (TvTEvent._teams)
					{
						// Iterate over all teams
						for (TvTEventTeam team : TvTEvent._teams)
						{
							// Iterate over all participated player instances in this team
							for (Player player : team.getParticipatedPlayers().values())
							{
								if (player != null && player.isOnline() && !player.isDead() /* && !playerInstance.isPhantom() && !playerInstance.isGM() */ && !player.isImmobilized() && !player.isParalyzed())
								{
									_player = player;
									addTvTSpawnInfo(player.getName(), player.getX(), player.getY(), player.getZ());
								}
							}
						}
					}
				}
				else
				{
					TvTPlayerList.clear();
				}
			}
		}

		private static void addTvTSpawnInfo(String name, int _x, int _y, int _z)
		{
			if (!checkTvTSpawnInfo(name))
			{
				String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
				TvTPlayerList.add(temp);
			}
			else
			{
				Object[] elements = TvTPlayerList.toArray();
				for (int i = 0; i < elements.length; i++)
				{
					splitter = ((String) elements[i]).split(":");
					String nameVal = splitter[0];
					if (name.equals(nameVal))
					{
						getTvTSpawnInfo(name);
						if (_x == x && _y == y && _z == z && _player.getAttack().isAttackingNow() == false && _player.getCast().isCastingNow() == false && _player.isOnline() == true && _player.isParalyzed() == false)
						{
							++sameLoc;
							if (sameLoc >= 4)// Kick after 4 same x/y/z, location checks
							{
								// kick here
								TvTPlayerList.remove(i);
								onLogout(_player);
								kickedMsg(_player);
								return;
							}
							else
							{
								TvTPlayerList.remove(i);
								String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":" + sameLoc;
								TvTPlayerList.add(temp);
								return;
							}
						}
						TvTPlayerList.remove(i);
						String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
						TvTPlayerList.add(temp);
					}
				}
			}
		}

		private static boolean checkTvTSpawnInfo(String name)
		{
			Object[] elements = TvTPlayerList.toArray();
			for (int i = 0; i < elements.length; i++)
			{
				splitter = ((String) elements[i]).split(":");
				String nameVal = splitter[0];
				if (name.equals(nameVal))
				{
					return true;
				}
			}
			return false;
		}

		private static void getTvTSpawnInfo(String name)
		{
			Object[] elements = TvTPlayerList.toArray();
			for (int i = 0; i < elements.length; i++)
			{
				splitter = ((String) elements[i]).split(":");
				String nameVal = splitter[0];
				if (name.equals(nameVal))
				{
					x = Integer.parseInt(splitter[1]);
					y = Integer.parseInt(splitter[2]);
					z = Integer.parseInt(splitter[3]);
					sameLoc = Integer.parseInt(splitter[4]);
				}
			}
		}

		private static void kickedMsg(Player player)
		{
			player.sendPacket(new ExShowScreenMessage("You're kicked out of the TvT by staying afk!", 6000));
		}

		public static final TvTAntiAfk getInstance()
		{
			return SingletonHolder.INSTANCE;
		}

		private static class SingletonHolder
		{
			protected static final TvTAntiAfk INSTANCE = new TvTAntiAfk();
		}
	}
}