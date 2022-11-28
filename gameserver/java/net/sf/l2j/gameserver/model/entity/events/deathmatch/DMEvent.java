package net.sf.l2j.gameserver.model.entity.events.deathmatch;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class DMEvent
{
	private static final CLogger LOGGER = new CLogger(DMEvent.class.getName());

	/** html path **/
	private static final String htmlPath = "mods/events/dm/";

	/**
	 * The state of the DMEvent<br>
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

	private static Map<Integer, DMPlayer> _dmPlayer = new HashMap<>();

	public DMEvent()
	{
	}

	/**
	 * DM initializing<br>
	 */
	public static void init()
	{
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.DM_ID);
	}

	/**
	 * Sets the DMEvent state<br>
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
	 * Is DMEvent inactive?<br>
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
	 * Is DMEvent in inactivating?<br>
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
	 * Is DMEvent in participation?<br>
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
	 * Is DMEvent starting?<br>
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
	 * Is DMEvent started?<br>
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
	 * Is DMEvent rewadrding?<br>
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
	 * UnSpawns the DMEvent npc
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
	 * Starts the participation of the DMEvent<br>
	 * 1. Get L2NpcTemplate by Config.DM_EVENT_PARTICIPATION_NPC_ID<br>
	 * 2. Try to spawn a new npc of it<br>
	 * <br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startParticipation()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.DM_EVENT_PARTICIPATION_NPC_ID);

		if (tmpl == null)
		{
			LOGGER.warn("DMEventEngine[DMEvent.startParticipation()]: L2NpcTemplate is a NullPointer -> Invalid npc id in configs?");
			return false;
		}

		try
		{
			_npcSpawn = new Spawn(tmpl);
			_npcSpawn.setLoc(Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[0], Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[1], Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[2], Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(1);
			_npcSpawn.setRespawnState(false);

			// later no need to delete spawn from db, we don't store it (false)
			SpawnManager.getInstance().addSpawn(_npcSpawn);

			_lastNpcSpawn = _npcSpawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.warn("DMEventEngine[DMEvent.startParticipation()]: exception: " + e.getMessage(), e);
			return false;
		}

		setState(EventState.PARTICIPATING);
		return true;
	}

	/**
	 * Starts the DMEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in configs<br>
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

		// Check the number of participants
		if (_dmPlayer.size() < Config.DM_EVENT_MIN_PLAYERS)
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);

			// Cleanup of participants
			_dmPlayer.clear();

			// Unspawn the event NPC
			unSpawnNpc();
			AntiFeedManager.getInstance().clear(AntiFeedManager.DM_ID);
			return false;
		}

		// Closes all doors specified in configs for dm
		closeDoors(Config.DM_DOORS_IDS_TO_CLOSE);

		// Set state STARTED
		setState(EventState.STARTED);

		for (DMPlayer player : _dmPlayer.values())
		{
			if (player != null)
			{
				new DMEventTeleporter(player.getPlayer(), false, false); // Teleporter implements Runnable and starts itself
			}
		}

		return true;
	}

	public static TreeSet<DMPlayer> orderPosition(Collection<DMPlayer> listPlayer)
	{
		TreeSet<DMPlayer> players = new TreeSet<>(new Comparator<DMPlayer>()
		{
			@Override
			public int compare(DMPlayer p1, DMPlayer p2)
			{
				Integer c1 = Integer.valueOf(p2.getPoints() - p1.getPoints());
				Integer c2 = Integer.valueOf(p1.getDeath() - p2.getDeath());
				Integer c3 = p1.getHexCode().compareTo(p2.getHexCode());

				if (c1 == 0)
				{
					if (c2 == 0)
					{
						return c3;
					}

					return c2;
				}
				return c1;
			}
		});
		players.addAll(listPlayer);
		return players;
	}

	/**
	 * Calculates the DMEvent reward<br>
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
		TreeSet<DMPlayer> players = orderPosition(_dmPlayer.values());
		String msg = "";
		for (int i = 0; i < Config.DM_REWARD_FIRST_PLAYERS; i++)
		{
			if (players.isEmpty())
			{
				break;
			}

			DMPlayer player = players.first();

			if (player.getPoints() == 0)
			{
				break;
			}

			rewardPlayer(player, i + 1);
			players.remove(player);
			int playerPointPrev = player.getPoints();

			while (!players.isEmpty())
			{
				if (player.getPoints() != playerPointPrev)
				{
					break;
				}

				rewardPlayer(player, i + 1);
				players.remove(player);
				msg += " Player: " + player.getPlayer().getName();
				msg += " Killed: " + player.getPoints();
				msg += "\n";

				if (!Config.DM_REWARD_PLAYERS_TIE)
				{
					break;
				}
			}
		}

		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);

		return "Deathmatch: ended, thanks to everyone who participated!\nWinner(s):\n" + msg;
	}

	private static void rewardPlayer(DMPlayer player, int pos)
	{
		Player activeChar = player.getPlayer();

		if (activeChar == null)
		{
			return;
		}

		SystemMessage systemMessage = null;

		List<int[]> rewards = Config.DM_EVENT_REWARDS.get(pos);

		for (int[] reward : rewards)
		{
			PcInventory inv = activeChar.getInventory();

			// Check for stackable item, non stackabe items need to be added one by one
			if (ItemData.getInstance().getTemplate(reward[0]).isStackable())
			{
				inv.addItem("Deathmatch", reward[0], reward[1], activeChar, activeChar);

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

				activeChar.sendPacket(systemMessage);
			}
			else
			{
				for (int i = 0; i < reward[1]; ++i)
				{
					inv.addItem("Deathmatch", reward[0], 1, activeChar, activeChar);
					systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
					systemMessage.addItemName(reward[0]);
					activeChar.sendPacket(systemMessage);
				}
			}
		}

		StatusUpdate statusUpdate = new StatusUpdate(activeChar);
		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

		statusUpdate.addAttribute(StatusType.CUR_LOAD, activeChar.getCurrentWeight());
		npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(activeChar.isLang() + htmlPath + "Reward.htm"));
		activeChar.sendPacket(statusUpdate);
		activeChar.sendPacket(npcHtmlMessage);
	}

	/**
	 * Stops the DMEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove DM npc from world<br>
	 * 3. Open doors specified in configs<br>
	 * 4. Send Top Rank<br>
	 * 5. Teleport all participants back to participation npc location<br>
	 * 6. List players cleaning<br>
	 * 7. Set state EventState.INACTIVE<br>
	 */
	public static void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);

		// Unspawn event npc
		unSpawnNpc();

		// Opens all doors specified in configs for DM
		openDoors(Config.DM_DOORS_IDS_TO_CLOSE);

		// Closes all doors specified in Configs for DM
		closeDoors(Config.DM_DOORS_IDS_TO_OPEN);

		String[] topPositions;
		String htmltext = "";
		if (Config.DM_SHOW_TOP_RANK)
		{
			topPositions = getFirstPosition(Config.DM_TOP_RANK);
			Boolean c = true;
			String c1 = "D9CC46";
			String c2 = "FFFFFF";
			if (topPositions != null)
			{
				for (int i = 0; i < topPositions.length; i++)
				{
					String color = (c ? c1 : c2);
					String[] row = topPositions[i].split("\\,");
					htmltext += "<tr>";
					htmltext += "<td width=\"35\" align=\"center\"><font color=\"" + color + "\">" + String.valueOf(i + 1) + "</font></td>";
					htmltext += "<td width=\"100\" align=\"left\"><font color=\"" + color + "\">" + row[0] + "</font></td>";
					htmltext += "<td width=\"125\" align=\"right\"><font color=\"" + color + "\">" + row[1] + "</font></td>";
					htmltext += "</tr>";
					c = !c;
				}
			}
		}

		for (DMPlayer player : _dmPlayer.values())
		{
			if (player != null)
			{
				Player activeChar = player.getPlayer();
				if (Config.DM_SHOW_TOP_RANK)
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
					npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(activeChar.isLang() + htmlPath + "TopRank.htm"));
					npcHtmlMessage.replace("%toprank%", htmltext);
					player.getPlayer().sendPacket(npcHtmlMessage);
				}

				new DMEventTeleporter(player.getPlayer(), Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
			}
		}

		// Cleanup list
		_dmPlayer = new HashMap<>();

		// Set state INACTIVE
		setState(EventState.INACTIVE);
		AntiFeedManager.getInstance().clear(AntiFeedManager.DM_ID);
	}

	/**
	 * Adds a player to a DMEvent<br>
	 * @param player as Player<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static synchronized boolean addParticipant(Player player)
	{
		if ((player == null) || isPlayerParticipant(player))
		{
			return false;
		}

		String hexCode = hexToString(generateHex(16));
		_dmPlayer.put(player.getObjectId(), new DMPlayer(player, hexCode));
		return true;
	}

	public static boolean isPlayerParticipant(Player player)
	{
		if (player == null)
		{
			return false;
		}

		try
		{
			if (_dmPlayer.containsKey(player.getObjectId()))
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}

	public static boolean isPlayerParticipant(int objectId)
	{
		Player player = World.getInstance().getPlayer(objectId);
		if (player == null)
		{
			return false;
		}

		return isPlayerParticipant(player);
	}

	/**
	 * Removes a DMEvent player<br>
	 * @param player as Player<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean removeParticipant(Player player)
	{
		if ((player == null) || !isPlayerParticipant(player))
		{
			return false;
		}

		try
		{
			_dmPlayer.remove(player.getObjectId());
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}

	public static boolean payParticipationFee(Player player)
	{
		int itemId = Config.DM_EVENT_PARTICIPATION_FEE[0];
		int itemNum = Config.DM_EVENT_PARTICIPATION_FEE[1];
		if (itemId == 0 || itemNum == 0)
		{
			return true;
		}

		if (player.getInventory().getItemCount(itemId, -1) < itemNum)
		{
			return false;
		}

		return player.destroyItemByItemId("DM Participation Fee", itemId, itemNum, _lastNpcSpawn, true);
	}

	public static String getParticipationFee()
	{
		int itemId = Config.DM_EVENT_PARTICIPATION_FEE[0];
		int itemNum = Config.DM_EVENT_PARTICIPATION_FEE[1];

		if (itemId == 0 || itemNum == 0)
		{
			return "-";
		}

		return String.valueOf(itemNum) + " " + ItemData.getInstance().getTemplate(itemId).getName();
	}

	/**
	 * Send a SystemMessage to all participated players<br>
	 * @param message as String<br>
	 */
	public static void sysMsgToAllParticipants(String message)
	{
		CreatureSay cs = new CreatureSay(0, SayType.PARTY, "Event Manager", message);

		for (DMPlayer player : _dmPlayer.values())
		{
			if (player != null)
			{
				player.getPlayer().sendPacket(cs);
			}
		}
	}

	/**
	 * Called when a player logs in<br>
	 * <br>
	 * @param player as Player<br>
	 */
	public static void onLogin(Player player)
	{
		if (player == null || (!isStarting() && !isStarted()) || !isPlayerParticipant(player))
		{
			return;
		}

		new DMEventTeleporter(player, false, false);
	}

	/**
	 * Called when a player logs out<br>
	 * <br>
	 * @param player as Player<br>
	 */
	public static void onLogout(Player player)
	{
		if (player != null && (isStarting() || isStarted() || isParticipating()))
		{
			if (removeParticipant(player))
			{
				player.teleportTo(Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101) - 50, Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101) - 50, Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);
			}
		}
	}

	/**
	 * Called on every bypass by npc of type DMEventNpc<br>
	 * Needs synchronization cause of the max player check<br>
	 * <br>
	 * @param command as String<br>
	 * @param player as Player<br>
	 */
	public static synchronized void onBypass(String command, Player player)
	{
		if (player == null || !isParticipating())
		{
			return;
		}

		final String htmContent;

		if (command.equals("dm_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

			if (player.isCursedWeaponEquipped())
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "CursedWeaponEquipped.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (player.isInOlympiadMode())
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
			else if (Config.DISABLE_ID_CLASSES.contains(player.getClassId().getId()))
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Class.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (player.getStatus().getLevel() < Config.DM_EVENT_MIN_LVL || player.getStatus().getLevel() > Config.DM_EVENT_MAX_LVL)
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(Config.DM_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(Config.DM_EVENT_MAX_LVL));
				}
			}
			else if (_dmPlayer.size() == Config.DM_EVENT_MAX_PLAYERS)
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Full.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(Config.DM_EVENT_MAX_PLAYERS));
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
			else if (isPlayerParticipant(player) || addParticipant(player))
			{
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Registered.htm"));
			}
			else
			{
				return;
			}

			player.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("dm_event_remove_participation"))
		{
			if (isPlayerParticipant(player))
			{
				removeParticipant(player);

				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(player.isLang() + htmlPath + "Unregistered.htm"));
				player.sendPacket(npcHtmlMessage);
			}
		}
	}

	/**
	 * Called on every onAction in L2PcIstance<br>
	 * <br>
	 * @param player as Player<br>
	 * @param targetedPlayerObjectId
	 * @return boolean: true if player is allowed to target, otherwise false<br>
	 */
	public static boolean onAction(Player player, int targetedPlayerObjectId)
	{
		if (player == null || !isStarted() || player.isGM())
		{
			return true;
		}

		if (!isPlayerParticipant(player) && isPlayerParticipant(targetedPlayerObjectId))
		{
			return false;
		}

		if (isPlayerParticipant(player) && !isPlayerParticipant(targetedPlayerObjectId))
		{
			return false;
		}

		return true;
	}

	/**
	 * Called on every scroll use<br>
	 * <br>
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to use scroll, otherwise false<br>
	 */
	public static boolean onScrollUse(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId) && !Config.DM_EVENT_SCROLL_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Called on every potion use<br>
	 * <br>
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to use potions, otherwise false<br>
	 */
	public static boolean onPotionUse(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId) && !Config.DM_EVENT_POTIONS_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Called on every escape use<br>
	 * <br>
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is not in DM Event, otherwise false<br>
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
	 * Called on every summon item use<br>
	 * <br>
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to summon by item, otherwise false<br>
	 */
	public static boolean onItemSummon(int objectId)
	{
		if (!isStarted())
		{
			return true;
		}

		if (isPlayerParticipant(objectId) && !Config.DM_EVENT_SUMMON_BY_ITEM_ALLOWED)
		{
			return false;
		}

		return true;
	}

	/**
	 * Is called when a player is killed<br>
	 * <br>
	 * @param killer as L2Character<br>
	 * @param player as Player<br>
	 */
	public static void onKill(Creature killer, Player player)
	{
		if (player == null || !isStarted() || !isPlayerParticipant(player.getObjectId()))
		{
			return;
		}

		new DMEventTeleporter(player, false, false);

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

		if (isPlayerParticipant(killerPlayerInstance))
		{
			_dmPlayer.get(killerPlayerInstance.getObjectId()).increasePoints();
			sysMsgToAllParticipants(killerPlayerInstance.getName() + " Hunted Player " + player.getName() + "!");

			_dmPlayer.get(player.getObjectId()).increaseDeath();
		}
	}

	/**
	 * Called on Appearing packet received (player finished teleporting)<br>
	 * <br>
	 * @param player
	 */
	public static void onTeleported(Player player)
	{
		if (!isStarted() || player == null || !isPlayerParticipant(player.getObjectId()))
		{
			return;
		}

		if (player.isMageClass())
		{
			if (Config.DM_EVENT_MAGE_BUFFS != null && !Config.DM_EVENT_MAGE_BUFFS.isEmpty())
			{
				for (int i : Config.DM_EVENT_MAGE_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, Config.DM_EVENT_MAGE_BUFFS.get(i));
					if (skill != null)
					{
						skill.getEffects(player, player);
					}
				}
			}
		}
		else if (Config.DM_EVENT_FIGHTER_BUFFS != null && !Config.DM_EVENT_FIGHTER_BUFFS.isEmpty())
		{
			for (int i : Config.DM_EVENT_FIGHTER_BUFFS.keySet())
			{
				L2Skill skill = SkillTable.getInstance().getInfo(i, Config.DM_EVENT_FIGHTER_BUFFS.get(i));
				if (skill != null)
				{
					skill.getEffects(player, player);
				}
			}
		}

		removeParty(player);
	}

	public static int getPlayerCounts()
	{
		return _dmPlayer.size();
	}

	public static String[] getFirstPosition(int countPos)
	{
		TreeSet<DMPlayer> players = orderPosition(_dmPlayer.values());
		String text = "";
		for (int j = 0; j < countPos; j++)
		{
			if (players.isEmpty())
			{
				break;
			}

			DMPlayer player = players.first();

			if (player.getPoints() == 0)
			{
				break;
			}

			text += player.getPlayer().getName() + "," + String.valueOf(player.getPoints()) + ";";
			players.remove(player);

			int playerPointPrev = player.getPoints();

			if (!Config.DM_REWARD_PLAYERS_TIE)
			{
				continue;
			}

			while (!players.isEmpty())
			{
				player = players.first();
				if (player.getPoints() != playerPointPrev)
				{
					break;
				}

				text += player.getPlayer().getName() + "," + String.valueOf(player.getPoints()) + ";";
				players.remove(player);
			}
		}

		if (text != "")
		{
			return text.split("\\;");
		}

		return null;
	}

	public static void removeParty(Player player)
	{
		if (player.getParty() != null)
		{
			Party party = player.getParty();
			party.removePartyMember(player, MessageType.LEFT);
		}
	}

	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Rnd.nextBytes(array);
		return array;
	}

	public static String hexToString(byte[] hex)
	{
		return new BigInteger(hex).toString(16);
	}

	public static Map<Integer, Player> allParticipants()
	{
		Map<Integer, Player> all = new HashMap<>();
		if (getPlayerCounts() > 0)
		{
			for (DMPlayer dp : _dmPlayer.values())
			{
				all.put(dp.getPlayer().getObjectId(), dp.getPlayer());
			}
			return all;
		}
		return all;
	}
}