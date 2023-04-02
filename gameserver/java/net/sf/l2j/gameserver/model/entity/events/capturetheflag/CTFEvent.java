package net.sf.l2j.gameserver.model.entity.events.capturetheflag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.logging.CLogger;
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
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
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
import net.sf.l2j.gameserver.skills.L2Skill;

public class CTFEvent
{
	private static final CLogger LOGGER = new CLogger(CTFEvent.class.getName());

	/** html path **/
	private static final String htmlPath = "mods/events/ctf/";

	/**
	 * The teams of the CTFEvent<br>
	 */
	private static CTFEventTeam[] _teams = new CTFEventTeam[2];

	/**
	 * The state of the CTFEvent<br>
	 */
	private static EventState _state = EventState.INACTIVE;

	/**
	 * The spawn of the participation npc<br>
	 */
	private static Spawn _npcSpawn = null;

	/**
	 * the npc instance of the participation npc<br>
	 */
	private static Npc _lastNpcSpawn = null;

	/**
	 * The spawn of Team1 flag<br>
	 */
	private static Spawn _flag1Spawn = null;

	/**
	 * the npc instance Team1 flag<br>
	 */
	private static Npc _lastFlag1Spawn = null;

	/**
	 * The spawn of Team2 flag<br>
	 */
	private static Spawn _flag2Spawn = null;

	/**
	 * the npc instance of Team2 flag<br>
	 */
	private static Npc _lastFlag2Spawn = null;

	/**
	 * the Team 1 flag carrier Player<br>
	 */
	private static Player _team1Carrier = null;

	/**
	 * the Team 2 flag carrier Player<br>
	 */
	private static Player _team2Carrier = null;

	/**
	 * the Team 1 flag carrier right hand item<br>
	 */
	private static ItemInstance _team1CarrierRHand = null;

	/**
	 * the Team 2 flag carrier right hand item<br>
	 */
	private static ItemInstance _team2CarrierRHand = null;

	/**
	 * the Team 1 flag carrier left hand item<br>
	 */
	private static ItemInstance _team1CarrierLHand = null;

	/**
	 * the Team 2 flag carrier left hand item<br>
	 */
	private static ItemInstance _team2CarrierLHand = null;

	/**
	 * No instance of this class!<br>
	 */
	private CTFEvent()
	{
	}

	/**
	 * Teams initializing<br>
	 */
	public static void init()
	{
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.CTF_ID);
		_teams[0] = new CTFEventTeam(Config.CTF_EVENT_TEAM_1_NAME, Config.CTF_EVENT_TEAM_1_COORDINATES);
		_teams[1] = new CTFEventTeam(Config.CTF_EVENT_TEAM_2_NAME, Config.CTF_EVENT_TEAM_2_COORDINATES);
	}

	/**
	 * Starts the participation of the CTFEvent<br>
	 * 1. Get L2NpcTemplate by Config.CTF_EVENT_PARTICIPATION_NPC_ID<br>
	 * 2. Try to spawn a new npc of it<br>
	 * <br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startParticipation()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.CTF_EVENT_PARTICIPATION_NPC_ID);

		if (tmpl == null)
		{
			LOGGER.warn("CTFEventEngine: L2EventManager is a NullPointer -> Invalid npc id in configs?");
			return false;
		}

		try
		{
			_npcSpawn = new Spawn(tmpl);
			_npcSpawn.setLoc(Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[0], Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[1], Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[2], Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(60000);
			_npcSpawn.setRespawnState(false);

			SpawnManager.getInstance().addSpawn(_npcSpawn);
			_lastNpcSpawn = _npcSpawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.warn("CTFEventEngine: exception: " + e.getMessage(), e);
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
	 * Starts the CTFEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in Configs<br>
	 * 3. Abort if not enought participants(return false)<br>
	 * 4. Set state EventState.STARTED<br>
	 * 5. Teleport all participants to team spot<br>
	 * <br>
	 * @return boolean: true if success, otherwise false<br>
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

		Player player;
		Iterator<Player> iter;
		if (needParticipationFee())
		{
			iter = allParticipants.values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!hasParticipationFee(player))
				{
					iter.remove();
				}
			}
		}

		int balance[] =
		{
			0,
			0
		}, priority = 0, highestLevelPlayerId;

		// TODO: allParticipants should be sorted by level instead of using highestLevelPcInstanceOf for every fetch
		while (!allParticipants.isEmpty())
		{
			// Priority team gets one player
			highestLevelPlayerId = highestLevelPcInstanceOf(allParticipants);
			Player highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getStatus().getLevel();

			// Exiting if no more players
			if (allParticipants.isEmpty())
			{
				break;
			}

			// The other team gets one player
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
		if ((_teams[0].getParticipatedPlayerCount() < Config.CTF_EVENT_MIN_PLAYERS_IN_TEAMS) || (_teams[1].getParticipatedPlayerCount() < Config.CTF_EVENT_MIN_PLAYERS_IN_TEAMS))
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);

			// Cleanup of teams
			_teams[0].cleanMe();
			_teams[1].cleanMe();

			// Unspawn the event NPC
			unSpawnNpc();
			AntiFeedManager.getInstance().clear(AntiFeedManager.CTF_ID);
			return false;
		}

		if (needParticipationFee())
		{
			iter = _teams[0].getParticipatedPlayers().values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!payParticipationFee(player))
				{
					iter.remove();
				}
			}

			iter = _teams[1].getParticipatedPlayers().values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!payParticipationFee(player))
				{
					iter.remove();
				}
			}
		}

		// Spawn Flag Quarters
		spawnFirstHeadQuarters();
		spawnSecondHeadQuarters();

		// Closes all doors specified in Configs for CTF
		closeDoors(Config.CTF_DOORS_IDS_TO_CLOSE);

		// Set state STARTED
		setState(EventState.STARTED);

		// Iterate over all teams
		for (CTFEventTeam team : _teams)
		{
			// Iterate over all participated player instances in this team
			for (Player playerInstance : team.getParticipatedPlayers().values())
			{
				if (playerInstance != null)
				{
					new CTFEventTeleporter(playerInstance, team.getCoordinates(), false, false); // Teleporter implements Runnable and starts itself
				}
			}
		}

		return true;
	}

	/**
	 * Calculates the CTFEvent reward<br>
	 * 1. If both teams are at a tie(points equals), send it as system message to all participants, if one of the teams have 0 participants left online abort rewarding<br>
	 * 2. Wait till teams are not at a tie anymore<br>
	 * 3. Set state EvcentState.REWARDING<br>
	 * 4. Reward team with more points<br>
	 * 5. Show win html to wining team participants<br>
	 * <br>
	 * @return String: winning team name<br>
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
				return "CTF Event: Event has ended. No team won due to inactivity!";
			}

			sysMsgToAllParticipants("Event has ended, both teams have tied.");
			if (Config.CTF_REWARD_TEAM_TIE)
			{
				rewardTeam(_teams[0]);
				rewardTeam(_teams[1]);
				return "CTF Event: Event has ended with both teams tying.";
			}

			return "CTF Event: Event has ended with both teams tying.";
		}

		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);

		// Get team which has more points
		CTFEventTeam team = _teams[_teams[0].getPoints() > _teams[1].getPoints() ? 0 : 1];
		rewardTeam(team);
		return "CTF Event: Event finish. Team " + team.getName() + " won with " + team.getPoints() + " points.";
	}

	private static void rewardTeam(CTFEventTeam team)
	{
		// Iterate over all participated player instances of the winning team
		for (Player player : team.getParticipatedPlayers().values())
		{
			if (player == null)
			{
				continue;
			}

			SystemMessage systemMessage = null;

			// Iterate over all CTF event rewards
			for (int[] reward : Config.CTF_EVENT_REWARDS)
			{
				PcInventory inv = player.getInventory();

				// Check for stackable item, non stackabe items need to be added one by one
				if (ItemData.getInstance().getTemplate(reward[0]).isStackable())
				{
					inv.addItem("CTF Event", reward[0], reward[1], player, player);

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
						inv.addItem("CTF Event", reward[0], 1, player, player);
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
	 * Stops the CTFEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove CTF npc from world<br>
	 * 3. Open doors specified in Configs<br>
	 * 4. Teleport all participants back to participation npc location<br>
	 * 5. Teams cleaning<br>
	 * 6. Set state EventState.INACTIVE<br>
	 */
	public static void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);

		// Unspawn event npc
		unSpawnNpc();

		// Opens all doors specified in Configs for CTF
		openDoors(Config.CTF_DOORS_IDS_TO_CLOSE);

		// Closes all doors specified in Configs for CTF
		closeDoors(Config.CTF_DOORS_IDS_TO_OPEN);

		// Reset flag carriers
		if (_team1Carrier != null)
		{
			removeFlagCarrier(_team1Carrier);
		}

		if (_team2Carrier != null)
		{
			removeFlagCarrier(_team2Carrier);
		}

		// Iterate over all teams
		for (CTFEventTeam team : _teams)
		{
			for (Player player : team.getParticipatedPlayers().values())
			{
				// Check for nullpointer
				if (player != null)
				{
					new CTFEventTeleporter(player, Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES, false, false); // Teleport back.
				}
			}
		}

		// Cleanup of teams
		_teams[0].cleanMe();
		_teams[1].cleanMe();

		// Set state INACTIVE
		setState(EventState.INACTIVE);
		AntiFeedManager.getInstance().clear(AntiFeedManager.CTF_ID);
	}

	/**
	 * Adds a player to a CTFEvent team<br>
	 * 1. Calculate the id of the team in which the player should be added<br>
	 * 2. Add the player to the calculated team<br>
	 * <br>
	 * @param player as Player<br>
	 * @return boolean: true if success, otherwise false<br>
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
	 * Removes a CTFEvent player from it's team<br>
	 * 1. Get team id of the player<br>
	 * 2. Remove player from it's team<br>
	 * <br>
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

	public static boolean needParticipationFee()
	{
		return (Config.CTF_EVENT_PARTICIPATION_FEE[0] != 0) && (Config.CTF_EVENT_PARTICIPATION_FEE[1] != 0);
	}

	public static boolean hasParticipationFee(Player player)
	{
		return player.getInventory().getItemCount(Config.CTF_EVENT_PARTICIPATION_FEE[0], -1) >= Config.CTF_EVENT_PARTICIPATION_FEE[1];
	}

	public static boolean payParticipationFee(Player player)
	{
		return player.destroyItemByItemId("CTF Participation Fee", Config.CTF_EVENT_PARTICIPATION_FEE[0], Config.CTF_EVENT_PARTICIPATION_FEE[1], _lastNpcSpawn, true);
	}

	public static String getParticipationFee()
	{
		int itemId = Config.CTF_EVENT_PARTICIPATION_FEE[0];
		int itemNum = Config.CTF_EVENT_PARTICIPATION_FEE[1];

		if (itemId == 0 || itemNum == 0)
		{
			return "-";
		}

		return String.valueOf(itemNum) + " " + ItemData.getInstance().getTemplate(itemId).getName();
	}

	/**
	 * Send a SystemMessage to all participated players<br>
	 * 1. Send the message to all players of team number one<br>
	 * 2. Send the message to all players of team number two<br>
	 * <br>
	 * @param message as String<br>
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
	 * Close doors specified in Configs
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
	 * Open doors specified in Configs
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

	private static void spawnFirstHeadQuarters()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.CTF_EVENT_TEAM_1_HEADQUARTERS_ID);

		if (tmpl == null)
		{
			LOGGER.warn("CTFEventEngine: First Head Quater is a NullPointer -> Invalid npc id in configs?");
			return;
		}

		try
		{
			_flag1Spawn = new Spawn(tmpl);
			_flag1Spawn.setLoc(Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[0], Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[1], Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[2], Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[3]);
			_flag1Spawn.setRespawnDelay(60000);
			_flag1Spawn.setRespawnState(false);

			SpawnManager.getInstance().addSpawn(_flag1Spawn);

			_lastFlag1Spawn = _flag1Spawn.doSpawn(false);
			_lastFlag1Spawn.setTitle(Config.CTF_EVENT_TEAM_1_NAME);
		}
		catch (Exception e)
		{
			LOGGER.warn("SpawnFirstHeadQuaters: exception: " + e.getMessage(), e);
			return;
		}
	}

	private static void spawnSecondHeadQuarters()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.CTF_EVENT_TEAM_2_HEADQUARTERS_ID);

		if (tmpl == null)
		{
			LOGGER.warn("CTFEventEngine: Second Head Quater is a NullPointer -> Invalid npc id in configs?");
			return;
		}

		try
		{
			_flag2Spawn = new Spawn(tmpl);
			_flag2Spawn.setLoc(Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[0], Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[1], Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[2], Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[3]);
			_flag2Spawn.setRespawnDelay(60000);
			_flag2Spawn.setRespawnState(false);

			SpawnManager.getInstance().addSpawn(_flag2Spawn);

			_lastFlag2Spawn = _flag2Spawn.doSpawn(false);
			_lastFlag2Spawn.setTitle(Config.CTF_EVENT_TEAM_2_NAME);
		}
		catch (Exception e)
		{
			LOGGER.warn("SpawnSecondHeadQuaters: exception: " + e.getMessage(), e);
			return;
		}
	}

	/**
	 * UnSpawns the CTFEvent npc
	 */
	private static void unSpawnNpc()
	{
		// Delete the npc
		_lastNpcSpawn.deleteMe();
		SpawnManager.getInstance().deleteSpawn((Spawn) _lastNpcSpawn.getSpawn());

		// Stop respawning of the npc
		_npcSpawn = null;
		_lastNpcSpawn = null;

		// Remove flags
		if (_lastFlag1Spawn != null)
		{
			_lastFlag1Spawn.deleteMe();
			_lastFlag2Spawn.deleteMe();
			SpawnManager.getInstance().deleteSpawn((Spawn) _lastFlag1Spawn.getSpawn());
			SpawnManager.getInstance().deleteSpawn((Spawn) _lastFlag2Spawn.getSpawn());
			_flag1Spawn = null;
			_flag2Spawn = null;
			_lastFlag1Spawn = null;
			_lastFlag2Spawn = null;
		}
	}

	/**
	 * Called when a player logs in<br>
	 * <br>
	 * @param player as Player<br>
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
		new CTFEventTeleporter(player, _teams[teamId].getCoordinates(), true, false);
	}

	/**
	 * Called when a player logs out<br>
	 * <br>
	 * @param player as Player<br>
	 */
	public static void onLogout(Player player)
	{
		if ((player != null) && (isStarting() || isStarted() || isParticipating()))
		{
			if (removeParticipant(player.getObjectId()))
			{
				player.teleportTo((Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)) - 50, (Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)) - 50, Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);
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

		if (command.equals("ctf_event_participation"))
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
			else if ((playerLevel < Config.CTF_EVENT_MIN_LVL) || (playerLevel > Config.CTF_EVENT_MAX_LVL))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(Config.CTF_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(Config.CTF_EVENT_MAX_LVL));
				}
			}
			else if ((_teams[0].getParticipatedPlayerCount() == Config.CTF_EVENT_MAX_PLAYERS_IN_TEAMS) && (_teams[1].getParticipatedPlayerCount() == Config.CTF_EVENT_MAX_PLAYERS_IN_TEAMS))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "TeamsFull.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(Config.CTF_EVENT_MAX_PLAYERS_IN_TEAMS));
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
		else if (command.equals("ctf_event_remove_participation"))
		{
			removeParticipant(player.getObjectId());
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Unregistered.htm"));
			player.sendPacket(npcHtmlMessage);
		}
	}

	/**
	 * Called on every onAction in L2PcIstance<br>
	 * <br>
	 * @param player
	 * @param objectId
	 * @return boolean: true if player is allowed to target, otherwise false
	 */
	public static boolean onAction(Player player, int objectId)
	{
		if ((player == null) || !isStarted() || player.isGM())
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

		if ((playerTeamId != -1) && (targetedPlayerTeamId != -1) && (playerTeamId == targetedPlayerTeamId) && (player.getObjectId() != objectId) && !Config.CTF_EVENT_TARGET_TEAM_MEMBERS_ALLOWED)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		return true;
	}

	/**
	 * Called on every scroll use<br>
	 * <br>
	 * @param objectId
	 * @return boolean: true if player is allowed to use scroll, otherwise false
	 */
	public static boolean onScrollUse(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId) && !Config.CTF_EVENT_SCROLL_ALLOWED)
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

		if (isPlayerParticipant(objectId) && !Config.CTF_EVENT_POTIONS_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Called on every escape use(thanks to nbd)
	 * @param objectId
	 * @return boolean: true if player is not in CTF event, otherwise false
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

		if (isPlayerParticipant(objectId) && !Config.CTF_EVENT_SUMMON_BY_ITEM_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Is called when a player is killed<br>
	 * <br>
	 * @param killer as Creature<br>
	 * @param player as Player<br>
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

		new CTFEventTeleporter(player, _teams[killedTeamId].getCoordinates(), false, false);

		if (killer == null)
		{
			return;
		}

		Player killerPlayerInstance = null;

		if (killer instanceof Pet || killer instanceof Summon)
		{
			killerPlayerInstance = ((Summon) killer).getOwner();
			if (killerPlayerInstance == null)
			{
				return;
			}
		}
		else if (killer instanceof Player)
		{
			killerPlayerInstance = (Player) killer;
		}
		else
		{
			return;
		}

		byte killerTeamId = getParticipantTeamId(killerPlayerInstance.getObjectId());
		if ((killerTeamId != -1) && (killedTeamId != -1) && (killerTeamId != killedTeamId))
		{
			sysMsgToAllParticipants(killerPlayerInstance.getName() + " Hunted Player " + player.getName() + "!");
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
			if (Config.CTF_EVENT_MAGE_BUFFS != null && !Config.CTF_EVENT_MAGE_BUFFS.isEmpty())
			{
				for (int i : Config.CTF_EVENT_MAGE_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, Config.CTF_EVENT_MAGE_BUFFS.get(i));
					if (skill != null)
					{
						skill.getEffects(player, player);
					}
				}
			}
		}
		else if (Config.CTF_EVENT_FIGHTER_BUFFS != null && !Config.CTF_EVENT_FIGHTER_BUFFS.isEmpty())
		{
			for (int i : Config.CTF_EVENT_FIGHTER_BUFFS.keySet())
			{
				L2Skill skill = SkillTable.getInstance().getInfo(i, Config.CTF_EVENT_FIGHTER_BUFFS.get(i));
				if (skill != null)
				{
					skill.getEffects(player, player);
				}
			}
		}

		removeParty(player);
	}

	/**
	 * Sets the CTFEvent state<br>
	 * <br>
	 * @param state as EventState<br>
	 */
	private static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}

	/**
	 * Is CTFEvent inactive?<br>
	 * <br>
	 * @return boolean: true if event is inactive(waiting for next event cycle), otherwise false<br>
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
	 * Is CTFEvent in inactivating?<br>
	 * <br>
	 * @return boolean: true if event is in inactivating progress, otherwise false<br>
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
	 * Is CTFEvent in participation?<br>
	 * <br>
	 * @return boolean: true if event is in participation progress, otherwise false<br>
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
	 * Is CTFEvent starting?<br>
	 * <br>
	 * @return boolean: true if event is starting up(setting up fighting spot, teleport players etc.), otherwise false<br>
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
	 * Is CTFEvent started?<br>
	 * <br>
	 * @return boolean: true if event is started, otherwise false<br>
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
	 * Is CTFEvent rewarding?<br>
	 * <br>
	 * @return boolean: true if event is currently rewarding, otherwise false<br>
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
	 * @return CTFEventTeam: team of the given playerObjectId, if not in event null
	 */
	public static CTFEventTeam getParticipantTeam(int objectId)
	{
		return (_teams[0].containsPlayer(objectId) ? _teams[0] : (_teams[1].containsPlayer(objectId) ? _teams[1] : null));
	}

	/**
	 * Returns the enemy team of a player, if player is not participant it returns null
	 * @param objectId
	 * @return CTFEventTeam: enemy team of the given playerObjectId, if not in event null
	 */
	public static CTFEventTeam getParticipantEnemyTeam(int objectId)
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
	 * Returns participated player count<br>
	 * <br>
	 * @return int: amount of players registered in the event<br>
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
	 * Returns teams names<br>
	 * <br>
	 * @return String[]: names of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
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
	 * Returns player count of both teams<br>
	 * <br>
	 * @return int[]: player count of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
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
	 * @return int[]: points of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
	 */
	public static int[] getTeamsPoints()
	{
		return new int[]
		{
			_teams[0].getPoints(),
			_teams[1].getPoints()
		};
	}

	/**
	 * Used when carrier scores, dies or game ends
	 * @param player Player
	 */
	public static void removeFlagCarrier(Player player)
	{
		// un-equip - destroy flag
		if (player.getInventory().hasItemIn(Paperdoll.RHAND))
		{
			if (player.getInventory().hasItemIn(Paperdoll.RHAND))
			{
				player.getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_R_HAND);
			}
		}
		else
		{
			player.getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_LR_HAND);
			if (player.getInventory().hasItemIn(Paperdoll.LHAND))
			{
				player.getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_L_HAND);
			}
		}
		player.destroyItemByItemId("ctf", getEnemyTeamFlagId(player), 1, player, false);

		// unblock inventory
		player.getInventory().unblock();

		// re-equip player items
		final ItemInstance carrierRHand = _teams[0].containsPlayer(player.getObjectId()) ? _team1CarrierRHand : _team2CarrierRHand;
		final ItemInstance carrierLHand = _teams[0].containsPlayer(player.getObjectId()) ? _team1CarrierLHand : _team2CarrierLHand;
		if ((carrierRHand != null) && (player.getInventory().getItemByItemId(carrierRHand.getItemId()) != null))
		{
			player.getInventory().equipItem(carrierRHand);
		}

		if ((carrierLHand != null) && (player.getInventory().getItemByItemId(carrierLHand.getItemId()) != null))
		{
			player.getInventory().equipItem(carrierLHand);
		}

		setCarrierUnequippedWeapons(player, null, null);

		// flag carrier removal
		if (_teams[0].containsPlayer(player.getObjectId()))
		{
			_team1Carrier = null;
		}
		else
		{
			_team2Carrier = null;
		}

		// show re-equipped weapons
		player.broadcastUserInfo();
	}

	/**
	 * Assign the Ctf team flag carrier
	 * @param player Player
	 */
	public static void setTeamCarrier(Player player)
	{
		if (_teams[0].containsPlayer(player.getObjectId()))
		{
			_team1Carrier = player;
		}
		else
		{
			_team2Carrier = player;
		}
	}

	/**
	 * @param player Player
	 * @return the team carrier Player
	 */
	public static Player getTeamCarrier(Player player)
	{
		// check if team carrier has disconnected
		if (((_teams[0].containsPlayer(player.getObjectId()) == true) && (_team1Carrier != null) && (!_team1Carrier.isOnline() || ((_teams[1].containsPlayer(player.getObjectId()) == true) && (_team2Carrier != null) && (!_team2Carrier.isOnline())))))
		{
			player.destroyItemByItemId("ctf", getEnemyTeamFlagId(player), 1, player, false);
			return null;
		}

		// return team carrier
		return (_teams[0].containsPlayer(player.getObjectId()) ? _team1Carrier : _team2Carrier);
	}

	/**
	 * @param player Player
	 * @return the enemy team carrier Player
	 */
	public static Player getEnemyCarrier(Player player)
	{
		// check if enemy carrier has disconnected
		if (((_teams[0].containsPlayer(player.getObjectId()) == true) && (_team2Carrier != null) && (!_team2Carrier.isOnline() || ((_teams[1].containsPlayer(player.getObjectId()) == true) && (_team1Carrier != null) && (!_team1Carrier.isOnline())))))
		{
			player.destroyItemByItemId("ctf", getEnemyTeamFlagId(player), 1, player, false);
			return null;
		}

		// return enemy carrier
		return (_teams[0].containsPlayer(player.getObjectId()) ? _team2Carrier : _team1Carrier);
	}

	/**
	 * @param player Player
	 * @return true if player is the carrier
	 */
	public static boolean playerIsCarrier(Player player)
	{
		return ((player == _team1Carrier) || (player == _team2Carrier)) ? true : false;
	}

	/**
	 * @param player L2ItemInstance
	 * @return int The enemy flag id
	 */
	public static int getEnemyTeamFlagId(Player player)
	{
		return (_teams[0].containsPlayer(player.getObjectId()) ? Config.CTF_EVENT_TEAM_2_FLAG : Config.CTF_EVENT_TEAM_1_FLAG);
	}

	/**
	 * Stores the carrier equipped weapons
	 * @param player Player
	 * @param itemRight L2ItemInstance
	 * @param itemLeft L2ItemInstance
	 */
	public static void setCarrierUnequippedWeapons(Player player, ItemInstance itemRight, ItemInstance itemLeft)
	{
		if (_teams[0].containsPlayer(player.getObjectId()))
		{
			_team1CarrierRHand = itemRight;
			_team1CarrierLHand = itemLeft;
		}
		else
		{
			_team2CarrierRHand = itemRight;
			_team2CarrierLHand = itemLeft;
		}
	}

	/**
	 * Broadcast a message to all participant screens
	 * @param message String
	 * @param duration int (in seconds)
	 */
	public static void broadcastScreenMessage(String message, int duration)
	{
		for (CTFEventTeam team : _teams)
		{
			for (Player playerInstance : team.getParticipatedPlayers().values())
			{
				if (playerInstance != null)
				{
					playerInstance.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				}
			}
		}
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
}