package net.sf.l2j.gameserver.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.DocumentSkill.Skill;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.model.spawn.MinionSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TutorialCloseHtml;
import net.sf.l2j.gameserver.network.serverpackets.TutorialEnableClientEvent;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowHtml;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowQuestionMark;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class Quest
{
	protected static final CLogger LOGGER = new CLogger(Quest.class.getName());

	private static final String HTML_NONE_AVAILABLE = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String HTML_ALREADY_COMPLETED = "<html><body>This quest has already been completed.</body></html>";
	private static final String HTML_TOO_MUCH_QUESTS = "<html><body>You have already accepted the maximum number of quests. No more than 25 quests may be undertaken simultaneously.<br>For quest information, enter Alt+U.</body></html>";

	public static final byte DROP_DIVMOD = 0;
	public static final byte DROP_FIXED_RATE = 1;
	public static final byte DROP_FIXED_COUNT = 2;
	public static final byte DROP_FIXED_BOTH = 3;

	public static final String SOUND_ACCEPT = "ItemSound.quest_accept";
	public static final String SOUND_ITEMGET = "ItemSound.quest_itemget";
	public static final String SOUND_MIDDLE = "ItemSound.quest_middle";
	public static final String SOUND_FINISH = "ItemSound.quest_finish";
	public static final String SOUND_GIVEUP = "ItemSound.quest_giveup";
	public static final String SOUND_JACKPOT = "ItemSound.quest_jackpot";
	public static final String SOUND_FANFARE = "ItemSound.quest_fanfare_2";
	public static final String SOUND_BEFORE_BATTLE = "Itemsound.quest_before_battle";
	public static final String SOUND_TUTORIAL = "ItemSound.quest_tutorial";

	private final Set<QuestTimer> _timers = ConcurrentHashMap.newKeySet();

	private final int _id;
	private final String _descr;

	private int[] _itemsIds;

	private boolean _isOnEnterWorld;
	private boolean _isOnDeath;

	/**
	 * Create a script/quest using quest id and description.
	 * @param id : The id of the quest, -1 for scripts, AIs, etc.
	 * @param descr : String for the description of the quest.
	 */
	public Quest(int id, String descr)
	{
		_id = id;
		_descr = descr;
	}

	@Override
	public boolean equals(Object o)
	{
		// core AIs are available only in one instance (in the list of event of NpcTemplate)
		if (o instanceof AttackableAIScript && this instanceof AttackableAIScript)
		{
			return true;
		}

		if (o instanceof Quest)
		{
			Quest q = (Quest) o;
			if (_id > 0 && _id == q._id)
			{
				return getName().equals(q.getName());
			}

			// Scripts may have same names, while being in different sub-package
			return getClass().getName().equals(q.getClass().getName());
		}

		return false;
	}

	@Override
	public String toString()
	{
		return _id + " " + _descr;
	}

	/**
	 * @return The name of the script.
	 */
	public final String getName()
	{
		return getClass().getSimpleName();
	}

	/**
	 * @return The quest identifier.
	 */
	public int getQuestId()
	{
		return _id;
	}

	/**
	 * @return True for a quest script and false for any custom script (script, AI, etc).
	 */
	public boolean isRealQuest()
	{
		return _id > 0;
	}

	/**
	 * @return The description of the quest.
	 */
	public String getDescr()
	{
		return _descr;
	}

	/**
	 * @return An array of registered quest items ids. Those items are automatically destroyed in case a {@link Player} aborts or finishes this {@link Quest}.
	 */
	public int[] getItemsIds()
	{
		return _itemsIds;
	}

	/**
	 * Register all items ids that are automatically destroyed in case a {@link Player} aborts or finishes this {@link Quest}.
	 * @param itemIds : The item ids referenced to be destroyed.
	 */
	public void setItemsIds(int... itemIds)
	{
		_itemsIds = itemIds;
	}

	/**
	 * @return True if this {@link Quest} triggers on {@link Player} entering world event.
	 */
	public boolean isTriggeredOnEnterWorld()
	{
		return _isOnEnterWorld;
	}

	/**
	 * Set this {@link Quest} to notify {@link Player} entering world event.
	 */
	public void setTriggeredOnEnterWorld()
	{
		_isOnEnterWorld = true;
	}

	/**
	 * @return True if this {@link Quest} triggers on {@link Player} or its {@link Summon} dying event.
	 */
	public boolean isTriggeredOnDeath()
	{
		return _isOnDeath;
	}

	/**
	 * Set this {@link Quest} to notify {@link Player} or its {@link Summon} dying event.
	 */
	public void setTriggeredOnDeath()
	{
		_isOnDeath = true;
	}

	/**
	 * Add a new {@link QuestState} related to this {@link Quest} for the {@link Player} set as parameter to the database, and return it.
	 * @param player : The {@link Player} used as parameter.
	 * @return A newly created {@link QuestState}.
	 */
	public QuestState newQuestState(Player player)
	{
		return new QuestState(player, this);
	}

	/**
	 * Check a {@link Player}'s {@link QuestState} condition. {@link Player} must be within Config.PARTY_RANGE distance from the {@link Npc}. If {@link Npc} is null, distance condition is ignored.
	 * @param player : The {@link Player} who acted towards the {@link Npc}.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param cond : Quest condition value that must be satisfied.
	 * @return The {@link QuestState} of that {@link Player}.
	 */
	public QuestState checkPlayerCondition(Player player, Npc npc, int cond)
	{
		// No valid player or npc instance is passed, there is nothing to check.
		if (player == null || npc == null)
		{
			return null;
		}

		// Check player's quest conditions.
		final QuestState st = player.getQuestList().getQuestState(getName());
		

		// Check quest's condition.
		// Player is in range?
		if ((st == null) || (st.getCond() != cond) || !player.isIn3DRadius(npc, Config.PARTY_RANGE))
		{
			return null;
		}

		return st;
	}

	/**
	 * Check a {@link Player}'s {@link QuestState} condition. {@link Player} must be within Config.PARTY_RANGE distance from the {@link Npc}. If {@link Npc} is null, distance condition is ignored.
	 * @param player : The {@link Player} who acted towards the {@link Npc}.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param variable : A tuple specifying a quest condition that must be satisfied.
	 * @param value : A tuple specifying a quest condition that must be satisfied.
	 * @return The {@link QuestState} of that {@link Player}.
	 */
	public QuestState checkPlayerVariable(Player player, Npc npc, String variable, String value)
	{
		// No valid player or npc instance is passed, there is nothing to check.
		if (player == null || npc == null)
		{
			return null;
		}

		// Check player's quest conditions.
		final QuestState st = player.getQuestList().getQuestState(getName());
		if (st == null)
		{
			return null;
		}

		// Check variable and its value.
		final String toCheck = st.get(variable);
		// Player is in range?
		if (toCheck == null || !value.equalsIgnoreCase(toCheck) || !player.isIn3DRadius(npc, Config.PARTY_RANGE))
		{
			return null;
		}

		return st;
	}

	/**
	 * Check a {@link Player}'s {@link Clan} leader {@link QuestState} condition. Both of them must be within Config.PARTY_RANGE distance from the {@link Npc}. If {@link Npc} is null, distance condition is ignored.
	 * @param player : The {@link Player} who acted towards the {@link Npc}.
	 * @param npc : The {@link Npc} used to compare distance (optional).
	 * @param cond : Quest condition value that must be satisfied.
	 * @return The {@link QuestState} of that {@link Player}'s {@link Clan} leader, if existing - otherwise, null.
	 */
	public QuestState checkClanLeaderCondition(Player player, Npc npc, int cond)
	{
		// Check player's quest conditions.
		final QuestState leaderQs = getClanLeaderQuestState(player, npc);
		// Check quest's condition.
		if ((leaderQs == null) || (leaderQs.getCond() != cond))
		{
			return null;
		}

		return leaderQs;
	}

	/**
	 * Check a {@link Player}'s {@link Clan} leader {@link QuestState} condition. Both of them must be within Config.PARTY_RANGE distance from the {@link Npc}. If {@link Npc} is null, distance condition is ignored.
	 * @param player : The {@link Player} who acted towards the {@link Npc}.
	 * @param npc : The {@link Npc} used to compare distance (optional).
	 * @param variable : A tuple specifying a quest condition that must be satisfied.
	 * @param value : A tuple specifying a quest condition that must be satisfied.
	 * @return The {@link QuestState} of that {@link Player}'s {@link Clan} leader, if existing - otherwise, null.
	 */
	public QuestState checkClanLeaderVariable(Player player, Npc npc, String variable, String value)
	{
		// Check player's quest conditions.
		final QuestState leaderQs = getClanLeaderQuestState(player, npc);
		if (leaderQs == null)
		{
			return null;
		}

		// Check variable and its value.
		final String toCheck = leaderQs.get(variable);
		if (toCheck == null || !value.equalsIgnoreCase(toCheck))
		{
			return null;
		}

		return leaderQs;
	}

	/**
	 * Check a {@link Player}'s {@link Clan} leader {@link QuestState} condition. Both of them must be within Config.PARTY_RANGE distance from the {@link Npc}. If {@link Npc} is null, distance condition is ignored.
	 * @param player : The {@link Player} who acted towards the {@link Npc}.
	 * @param npc : The {@link Npc} used to compare distance (optional).
	 * @param state : The {@link QuestStatus} state to be matched.
	 * @return The {@link QuestState} of that {@link Player}'s {@link Clan} leader, if existing - otherwise, null.
	 */
	public QuestState checkClanLeaderState(Player player, Npc npc, QuestStatus state)
	{
		// Check player's quest conditions.
		final QuestState leaderQs = getClanLeaderQuestState(player, npc);
		// State correct?
		if ((leaderQs == null) || (leaderQs.getState() != state))
		{
			return null;
		}

		return leaderQs;
	}

	/**
	 * Auxiliary function for party quests.<br>
	 * <br>
	 * Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : The {@link Player} whose {@link Party} is to be checked.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param cond : Quest condition value that must be satisfied for a party member to be considered.
	 * @return The {@link List} of party members, that matches the specified condition, empty list if none matches.
	 */
	public List<QuestState> getPartyMembers(Player player, Npc npc, int cond)
	{
		if (player == null)
		{
			return Collections.emptyList();
		}

		final Party party = player.getParty();
		if (party == null)
		{
			final QuestState st = checkPlayerCondition(player, npc, cond);
			return (st != null) ? Arrays.asList(st) : Collections.emptyList();
		}

		final List<QuestState> list = new ArrayList<>();
		for (Player member : party.getMembers())
		{
			final QuestState st = checkPlayerCondition(member, npc, cond);
			if (st != null)
			{
				list.add(st);
			}
		}
		return list;
	}

	/**
	 * Auxiliary function for party quests.<br>
	 * <br>
	 * Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : The {@link Player} whose {@link Party} is to be checked.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param var : A tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value : A tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return The {@link List} of party members, that matches the specified condition, empty list if none matches. If the var is null, empty list is returned (i.e. no condition is applied).
	 */
	public List<QuestState> getPartyMembers(Player player, Npc npc, String var, String value)
	{
		if (player == null)
		{
			return Collections.emptyList();
		}

		final Party party = player.getParty();
		if (party == null)
		{
			final QuestState st = checkPlayerVariable(player, npc, var, value);
			return (st != null) ? Arrays.asList(st) : Collections.emptyList();
		}

		final List<QuestState> list = new ArrayList<>();
		for (Player member : party.getMembers())
		{
			final QuestState st = checkPlayerVariable(member, npc, var, value);
			if (st != null)
			{
				list.add(st);
			}
		}
		return list;
	}

	/**
	 * Auxiliary function for party quests.<br>
	 * <br>
	 * Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : The {@link Player} whose {@link Party} is to be checked.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param cond : Quest condition value that must be satisfied.
	 * @return The {@link QuestState} of random party member, that matches the specified condition, or null if no match.
	 */
	public QuestState getRandomPartyMember(Player player, Npc npc, int cond)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
		{
			return null;
		}

		// Return random candidate.
		return Rnd.get(getPartyMembers(player, npc, cond));
	}

	/**
	 * Auxiliary function for party quests.<br>
	 * <br>
	 * Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : The {@link Player} whose {@link Party} is to be checked.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param var : A tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value : A tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return The {@link QuestState} of random party member, that matches the specified condition, or null if no match. If the var is null, null is returned (i.e. no condition is applied).
	 */
	public QuestState getRandomPartyMember(Player player, Npc npc, String var, String value)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
		{
			return null;
		}

		// Return random candidate.
		return Rnd.get(getPartyMembers(player, npc, var, value));
	}

	/**
	 * Check the {@link Player}'s {@link QuestState} state. {@link Player} must be within Config.PARTY_RANGE distance from the {@link Npc}.
	 * @param player : The {@link Player} who acted towards the {@link Npc}.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param state : The {@link QuestState} state to be matched.
	 * @return The {@link QuestState} of that {@link Player}.
	 */
	public QuestState checkPlayerState(Player player, Npc npc, QuestStatus state)
	{
		// No valid player or npc instance is passed, there is nothing to check.
		if (player == null || npc == null)
		{
			return null;
		}

		// Check player's quest conditions.
		final QuestState st = player.getQuestList().getQuestState(getName());
		

		// Check quest's state.
		// Player is in range?
		if ((st == null) || (st.getState() != state) || !player.isIn3DRadius(npc, Config.PARTY_RANGE))
		{
			return null;
		}

		return st;
	}

	/**
	 * Auxiliary function for party quests.<br>
	 * <br>
	 * Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : The {@link Player} whose {@link Party} is to be checked.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param state : The {@link QuestState} state to be matched by every party member.
	 * @return {@link List} of party members, that matches the specified {@link QuestState} state, empty list if none matches.
	 */
	public List<QuestState> getPartyMembersState(Player player, Npc npc, QuestStatus state)
	{
		if (player == null)
		{
			return Collections.emptyList();
		}

		final Party party = player.getParty();
		if (party == null)
		{
			final QuestState st = checkPlayerState(player, npc, state);
			return (st != null) ? Arrays.asList(st) : Collections.emptyList();
		}

		final List<QuestState> list = new ArrayList<>();
		for (Player member : party.getMembers())
		{
			final QuestState st = checkPlayerState(member, npc, state);
			if (st != null)
			{
				list.add(st);
			}
		}
		return list;
	}

	/**
	 * Auxiliary function for party quests.<br>
	 * <br>
	 * Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : The {@link Player} whose {@link Party} is to be checked.
	 * @param npc : The {@link Npc} used to compare distance.
	 * @param state : The {@link QuestState} state to be matched by every party member.
	 * @return The {@link QuestState} of random party member, that matches the specified {@link QuestState} state, or null if no match.
	 */
	public QuestState getRandomPartyMemberState(Player player, Npc npc, QuestStatus state)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
		{
			return null;
		}

		// Return random candidate.
		return Rnd.get(getPartyMembersState(player, npc, state));
	}

	/**
	 * Check a {@link Player}'s {@link Clan} leader {@link QuestState} state. Both of them must be within Config.PARTY_RANGE distance from the {@link Npc}. If {@link Npc} is null, distance condition is ignored.
	 * @param player : The {@link Player} who acted towards the {@link Npc}.
	 * @param npc : The {@link Npc} used to compare distance (optional).
	 * @return The {@link QuestState} of that {@link Player}'s {@link Clan} leader, if existing and online - otherwise, null.
	 */
	public QuestState getClanLeaderQuestState(Player player, Npc npc)
	{
		// No valid player instance is passed, there is nothing to check.
		// Player killer must be in range with npc.
		if ((player == null) || (npc != null && !player.isIn3DRadius(npc, Config.PARTY_RANGE)))
		{
			return null;
		}

		// If player is the leader, retrieves directly the qS and bypass others checks
		if (player.isClanLeader())
		{
			return player.getQuestList().getQuestState(getName());
		}

		// Verify if the player got a clan
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return null;
		}

		// Verify if the leader is online
		final Player leader = clan.getLeader().getPlayerInstance();
		if (leader == null)
		{
			return null;
		}

		// Verify if the leader is on the radius of the npc.
		if (npc != null && !leader.isIn3DRadius(npc, Config.PARTY_RANGE))
		{
			return null;
		}

		return leader.getQuestList().getQuestState(getName());
	}

	/**
	 * @param player : The {@link Player} instance to check.
	 * @return True if the given {@link Player} got an online {@link Clan} member sponsor in a 1500 radius range.
	 */
	public static boolean getSponsor(Player player)
	{
		// Player hasn't a sponsor.
		final int sponsorId = player.getSponsor();
		if (sponsorId == 0)
		{
			return false;
		}

		// Player hasn't a clan.
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return false;
		}

		// Retrieve sponsor clan member object.
		final ClanMember member = clan.getClanMember(sponsorId);
		if (member != null && member.isOnline())
		{
			// The sponsor is online, retrieve player instance and check distance.
			final Player sponsor = member.getPlayerInstance();
			if (sponsor != null && player.isIn3DRadius(sponsor, 1500))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * @param player : The {@link Player} instance to check.
	 * @return The {@link Clan} apprentice of the given {@link Player}. He must be online, and in a 1500 radius range.
	 */
	public static Player getApprentice(Player player)
	{
		// Player hasn't an apprentice.
		final int apprenticeId = player.getApprentice();
		if (apprenticeId == 0)
		{
			return null;
		}

		// Player hasn't a clan.
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return null;
		}

		// Retrieve apprentice clan member object.
		final ClanMember member = clan.getClanMember(apprenticeId);
		if (member != null && member.isOnline())
		{
			// The apprentice is online, retrieve player instance and check distance.
			final Player academic = member.getPlayerInstance();
			if (academic != null && player.isIn3DRadius(academic, 1500))
			{
				return academic;
			}
		}

		return null;
	}

	/**
	 * Add new {@link QuestTimer}, if it doesn't exist already.<br>
	 * <br>
	 * The time is fired only once, after the time is elapsed.
	 * @param name : The name of the timer (can't be null).
	 * @param npc : The {@link Npc} associated with the timer (optional, can be null).
	 * @param player : The {@link Player} associated with the timer (optional, can be null).
	 * @param time : Time in milliseconds to fire the timer (initially).
	 * @return True, if new {@link QuestTimer} has been created. False, if already exists.
	 */
	public final boolean startQuestTimer(String name, Npc npc, Player player, long time)
	{
		return startQuestTimerAtFixedRate(name, npc, player, time, 0);
	}

	/**
	 * Add new {@link QuestTimer}, if it doesn't exist already.<br>
	 * <br>
	 * The timer is repeatable, it fires each period.
	 * @param name : The name of the timer (can't be null).
	 * @param npc : The {@link Npc} associated with the timer (optional, can be null).
	 * @param player : The {@link Player} associated with the timer (optional, can be null).
	 * @param period : Time in milliseconds to fire the timer repeatedly (optional, can be 0).
	 * @return True, if new {@link QuestTimer} has been created. False, if already exists.
	 */
	public final boolean startQuestTimerAtFixedRate(String name, Npc npc, Player player, long period)
	{
		return startQuestTimerAtFixedRate(name, npc, player, period, period);
	}

	/**
	 * Add new {@link QuestTimer}, if it doesn't exist already.<br>
	 * <br>
	 * The timer is repeatable, it fires after initial time is elapsed and than each period.
	 * @param name : The name of the timer (can't be null).
	 * @param npc : The {@link Npc} associated with the timer (optional, can be null).
	 * @param player : The {@link Player} associated with the timer (optional, can be null).
	 * @param initial : Time in milliseconds to fire the timer (initially).
	 * @param period : Time in milliseconds to fire the timer repeatedly after initial tick (optional, can be 0).
	 * @return True, if new {@link QuestTimer} has been created. False, if already exists.
	 */
	public final boolean startQuestTimerAtFixedRate(String name, Npc npc, Player player, long initial, long period)
	{
		// Name must exist.
		if (name == null)
		{
			LOGGER.warn("Script {} adding timer without name.", toString());
			return false;
		}

		// Check if specific timer already exists. If so, return.
		if (_timers.stream().anyMatch(qt -> qt.getName().equals(name) && qt.getNpc() == npc && qt.getPlayer() == player))
		{
			return false;
		}

		// Add new timer.
		_timers.add(new QuestTimer(this, name, npc, player, initial, period));
		return true;
	}

	/**
	 * @param name : The name of the timer.
	 * @param npc : The {@link Npc} associated with the timer (optional, can be null).
	 * @param player : The {@link Player} associated with the timer (optional, can be null).
	 * @return The {@link QuestTimer} of the given parameters, null if not exists.
	 */
	public final QuestTimer getQuestTimer(String name, Npc npc, Player player)
	{
		// Find the timer and return.
		return _timers.stream().filter(qt -> qt.getName().equals(name) && qt.getNpc() == npc && qt.getPlayer() == player).findFirst().orElse(null);
	}

	/**
	 * Cancel all {@link QuestTimer}s, regardless timer name, {@link Npc} and {@link Player}.
	 */
	public final void cancelQuestTimers()
	{
		// Cancel all quest timers.
		_timers.stream().forEach(QuestTimer::cancel);
	}

	/**
	 * Cancel all {@link QuestTimer}s by given timer name, regardless {@link Npc} and {@link Player}.
	 * @param name : The matching name of the timer (should not be null).
	 */
	public final void cancelQuestTimers(String name)
	{
		// Cancel all quest timers with given name.
		_timers.stream().filter(qt -> qt.getName().equals(name)).forEach(QuestTimer::cancel);
	}

	/**
	 * Cancel all {@link QuestTimer}s by given {@link Npc}, regardless timer name and {@link Player}.
	 * @param npc : The matching {@link Npc} associated with the timer (should not be null).
	 */
	public final void cancelQuestTimers(Npc npc)
	{
		// Cancel all quest timers with given Npc.
		_timers.stream().filter(qt -> qt.getNpc() == npc).forEach(QuestTimer::cancel);
	}

	/**
	 * Cancel all {@link QuestTimer}s by given {@link Player}, regardless timer name and {@link Npc}.
	 * @param player : The matching {@link Player} associated with the timer (should not be null).
	 */
	public final void cancelQuestTimers(Player player)
	{
		// Cancel all quest timers with given Player.
		_timers.stream().filter(qt -> qt.getPlayer() == player).forEach(QuestTimer::cancel);
	}

	/**
	 * Cancel all {@link QuestTimer}s by given timer name and {@link Npc}, regardless {@link Player}.
	 * @param name : The matching name of the timer.
	 * @param npc : The matching {@link Npc} associated with the timer (should not be null).
	 */
	public final void cancelQuestTimers(String name, Npc npc)
	{
		// Cancel all quest timers with given name and Npc.
		_timers.stream().filter(qt -> qt.getName().equals(name) && qt.getNpc() == npc).forEach(QuestTimer::cancel);
	}

	/**
	 * Cancel all {@link QuestTimer}s by given timer name and {@link Player}, regardless {@link Npc}.
	 * @param name : The matching name of the timer.
	 * @param player : The matching {@link Player} associated with the timer (should not be null).
	 */
	public final void cancelQuestTimers(String name, Player player)
	{
		// Cancel all quest timers with given name and Player.
		_timers.stream().filter(qt -> qt.getName().equals(name) && qt.getPlayer() == player).forEach(QuestTimer::cancel);
	}

	/**
	 * Cancel {@link QuestTimer} by given parameters.
	 * @param name : The matching name of the timer.
	 * @param npc : The matching {@link Npc} associated with the timer (should not be null).
	 * @param player : The matching {@link Player} associated with the timer (should not be null).
	 */
	public final void cancelQuestTimer(String name, Npc npc, Player player)
	{
		// Cancel all quest timers with given name, Npc and Player (only one exists though).
		_timers.stream().filter(qt -> qt.getName().equals(name) && qt.getNpc() == npc && qt.getPlayer() == player).forEach(QuestTimer::cancel);
	}

	/**
	 * Remove {@link QuestTimer} from the {@link Quest}. Used for timers, which are being terminated.
	 * @param timer : The {@link QuestTimer}.
	 */
	public final void removeQuestTimer(QuestTimer timer)
	{
		// Timer does not exist, return.
		if (timer == null)
		{
			return;
		}

		// Remove timer from the list.
		_timers.remove(timer);
	}

	/**
	 * Spawn a temporary {@link Npc}, based on provided {@link StatSet} informations.<br>
	 * <br>
	 * This {@link Npc} is registered into {@link GrandBossManager}, his HP/MP restored, and is forced to run.
	 * @param npcId : The {@link Npc} template to spawn.
	 * @param set : The {@link StatSet} used to retrieve informations.
	 * @return The spawned {@link Npc}, null if some problem occurs.
	 */
	public Npc addGrandBossSpawn(int npcId, StatSet set)
	{
		// Generate and spawn a Npc based on StatsSet informations.
		final Npc npc = addSpawn(npcId, set.getInteger("loc_x"), set.getInteger("loc_y"), set.getInteger("loc_z"), set.getInteger("heading"), false, 0, false);

		// Add the Npc into the GrandBossManager.
		GrandBossManager.getInstance().addBoss((GrandBoss) npc);

		// Set HP/MP based on StatsSet informations.
		npc.getStatus().setHpMp(set.getDouble("currentHP"), set.getDouble("currentMP"));

		// Force the Npc to run.
		npc.forceRunStance();

		return npc;
	}

	/**
	 * Spawns temporary (quest) {@link Npc} on the location of a {@link Creature}.
	 * @param npcId : The {@link Npc} template to spawn.
	 * @param cha : The {@link Creature} on whose position to spawn.
	 * @param randomOffset : Allow random offset coordinates.
	 * @param despawnDelay : Define despawn delay in milliseconds, 0 for none.
	 * @param isSummonSpawn : If true, spawn with animation (if any exists).
	 * @return The spawned {@link Npc}, null if some problem occurs.
	 */
	public Npc addSpawn(int npcId, Creature cha, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
	}

	/**
	 * Spawns temporary (quest) {@link Npc} on the {@link SpawnLocation}.
	 * @param npcId : The {@link Npc} template to spawn.
	 * @param loc : The {@link SpawnLocation} to spawn on.
	 * @param randomOffset : Allow random offset coordinates.
	 * @param despawnDelay : Define despawn delay in milliseconds, 0 for none.
	 * @param isSummonSpawn : If true, spawn with animation (if any exists).
	 * @return The spawned {@link Npc}, null if some problem occurs.
	 */
	public Npc addSpawn(int npcId, SpawnLocation loc, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
	}

	/**
	 * Spawns temporary (quest) {@link Npc} on the coordinates.
	 * @param npcId : The {@link Npc} template to spawn.
	 * @param x : The X coordinate.
	 * @param y : The Y coordinate.
	 * @param z : The Z coordinate.
	 * @param heading : The heading.
	 * @param randomOffset : Allow random offset coordinates.
	 * @param despawnDelay : Define despawn delay in milliseconds, 0 for none.
	 * @param isSummonSpawn : If true, spawn with animation (if any exists).
	 * @return The spawned {@link Npc}, null if some problem occurs.
	 */
	public Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		try
		{
			// Get NPC template.
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			if (template == null)
			{
				return null;
			}

			// Get spawn location.
			if (randomOffset)
			{
				// Get new coordinates.
				final int nx = x + Rnd.get(-100, 100);
				final int ny = y + Rnd.get(-100, 100);

				// Validate new coordinates.
				final Location loc = GeoEngine.getInstance().getValidLocation(x, y, z, nx, ny, z, null);
				x = loc.getX();
				y = loc.getY();
				z = loc.getZ();
			}
			else
			{
				z = GeoEngine.getInstance().getHeight(x, y, z);
			}

			// Create spawn.
			final Spawn spawn = new Spawn(template);
			spawn.setLoc(x, y, z + 20, heading);

			// Spawn NPC.
			final Npc npc = spawn.doSpawn(isSummonSpawn);
			if (despawnDelay > 0)
			{
				npc.scheduleDespawn(despawnDelay);
			}

			return npc;
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't spawn npcId {} for {}.", e, npcId, toString());
			return null;
		}
	}

	/**
	 * Instantly spawn a {@link Npc} based on npcId parameter, near another {@link Npc} which is considered its master.
	 * @param master : The {@link Npc} which is considered its master.
	 * @param npcId : The npcId to retrieve as {@link NpcTemplate}.
	 * @param despawnDelay : Define despawn delay in milliseconds, 0 for none.
	 * @param isSummonSpawn : If true, spawn with animation (if any exists).
	 * @return The spawned {@link Npc}, null if some problem occurs.
	 */
	public Npc createOnePrivate(Npc master, int npcId, long despawnDelay, boolean isSummonSpawn)
	{
		try
		{
			// Get NPC template.
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			if (template == null)
			{
				return null;
			}

			// Create the Spawn.
			final MinionSpawn spawn = new MinionSpawn(template, master);

			// Spawn the Npc.
			final Npc npc = spawn.doSpawn(isSummonSpawn);
			if (despawnDelay > 0)
			{
				npc.scheduleDespawn(despawnDelay);
			}

			// Register the Npc as master's private.
			master.getMinions().add(npc);

			return npc;
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't spawn npcId {} for {}.", e, npcId, toString());
			return null;
		}
	}

	/**
	 * Instantly spawn a {@link Npc} based on npcId parameter in a defined location (x/y/z).
	 * @param master : The {@link Npc} which is considered its master.
	 * @param npcId : The npcId to retrieve as {@link NpcTemplate}.
	 * @param x : The X coord used to spawn the {@link Npc}.
	 * @param y : The Y coord used to spawn the {@link Npc}.
	 * @param z : The Z coord used to spawn the {@link Npc}.
	 * @param heading : The heading used to spawn the {@link Npc}.
	 * @param despawnDelay : Define despawn delay in milliseconds, 0 for none.
	 * @param isSummonSpawn : If true, spawn with animation (if any exists).
	 * @return The spawned {@link Npc}, null if some problem occurs.
	 */
	public Npc createOnePrivateEx(Npc master, int npcId, int x, int y, int z, int heading, long despawnDelay, boolean isSummonSpawn)
	{
		try
		{
			// Get NPC template.
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			if (template == null)
			{
				return null;
			}

			// Create the Spawn.
			final MinionSpawn spawn = new MinionSpawn(template, master);
			spawn.setLoc(x, y, z, heading);

			// Spawn the Npc.
			final Npc npc = spawn.doSpawn(isSummonSpawn);
			if (despawnDelay > 0)
			{
				npc.scheduleDespawn(despawnDelay);
			}

			// Register the Npc as master's private.
			master.getMinions().add(npc);

			return npc;
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't spawn npcId {} for {}.", e, npcId, toString());
			return null;
		}
	}

	/**
	 * Give items to the {@link Player}'s inventory.
	 * @param player : The {@link Player} to give items.
	 * @param itemId : Identifier of the item.
	 * @param itemCount : Quantity of items to add.
	 */
	public static void giveItems(Player player, int itemId, int itemCount)
	{
		giveItems(player, itemId, itemCount, 0);
	}

	/**
	 * Give items to the {@link Player}'s inventory.
	 * @param player : The {@link Player} to give items.
	 * @param itemId : Identifier of the item.
	 * @param itemCount : Quantity of items to add.
	 * @param enchantLevel : Enchant level of items to add.
	 */
	public static void giveItems(Player player, int itemId, int itemCount, int enchantLevel)
	{
		// Incorrect amount.
		if (itemCount <= 0)
		{
			return;
		}

		// Add items to player's inventory.
		final ItemInstance item = player.getInventory().addItem("Quest", itemId, itemCount, player, player);
		if (item == null)
		{
			return;
		}

		// Set enchant level for the item.
		if (enchantLevel > 0)
		{
			item.setEnchantLevel(enchantLevel);
		}

		// Send message to the client.
		if (itemId == 57)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA).addItemNumber(itemCount));
		}
		else if (itemCount > 1)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addItemNumber(itemCount));
		}
		else
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
		}

		// Send status update packet.
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusType.CUR_LOAD, player.getCurrentWeight());
		player.sendPacket(su);
	}

	/**
	 * Remove items from the {@link Player}'s inventory.
	 * @param player : The {@link Player} to remove items.
	 * @param itemId : Identifier of the item.
	 * @param itemCount : Quantity of items to destroy.
	 */
	public static void takeItems(Player player, int itemId, int itemCount)
	{
		// Get item template.
		final Item template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			return;
		}

		if (template.isStackable())
		{
			// Find item in player's inventory.
			final ItemInstance item = player.getInventory().getItemByItemId(itemId);
			if (item == null)
			{
				return;
			}

			// Tests on count value and set correct value if necessary.
			if (itemCount < 0 || itemCount > item.getCount())
			{
				itemCount = item.getCount();
			}

			// Disarm item, if equipped.
			if (item.isEquipped())
			{
				InventoryUpdate iu = new InventoryUpdate();
				for (ItemInstance disarmed : player.getInventory().unequipItemInBodySlotAndRecord(item))
				{
					iu.addModifiedItem(disarmed);
				}

				player.sendPacket(iu);
				player.broadcastUserInfo();
			}

			// Destroy the quantity of items wanted.
			player.destroyItemByItemId("Quest", itemId, itemCount, player, true);
		}
		else
		{
			// Find items in player's inventory and remove required amount.
			int removed = 0;
			for (ItemInstance item : player.getInventory().getItemsByItemId(itemId))
			{
				// Check removed amount.
				if (itemCount >= 0 && removed == itemCount)
				{
					break;
				}

				// Disarm item, if equipped.
				if (item.isEquipped())
				{
					InventoryUpdate iu = new InventoryUpdate();
					for (ItemInstance disarmed : player.getInventory().unequipItemInBodySlotAndRecord(item))
					{
						iu.addModifiedItem(disarmed);
					}

					player.sendPacket(iu);
					player.broadcastUserInfo();
				}

				// Destroy the quantity of items wanted.
				player.destroyItem("Quest", item, player, true);
				removed++;
			}
		}
	}

	/**
	 * Drop items to the {@link Player}'s inventory. Rate is 100%, amount is affected by Config.RATE_QUEST_DROP.
	 * @param player : The {@link Player} to drop items.
	 * @param itemId : Identifier of the item to be dropped.
	 * @param count : Quantity of items to be dropped.
	 * @param neededCount : Quantity of items needed to complete the task. If set to 0, unlimited amount is collected.
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public static boolean dropItemsAlways(Player player, int itemId, int count, int neededCount)
	{
		return dropItems(player, itemId, count, neededCount, DropData.MAX_CHANCE, DROP_FIXED_RATE);
	}

	/**
	 * Drop items to the {@link Player}'s inventory. Rate and amount is affected by DIVMOD of Config.RATE_QUEST_DROP.
	 * @param player : The {@link Player} to drop items.
	 * @param itemId : Identifier of the item to be dropped.
	 * @param count : Quantity of items to be dropped.
	 * @param neededCount : Quantity of items needed to complete the task. If set to 0, unlimited amount is collected.
	 * @param dropChance : Item drop rate (100% chance is defined by the L2DropData.MAX_CHANCE = 1.000.000).
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public static boolean dropItems(Player player, int itemId, int count, int neededCount, int dropChance)
	{
		return dropItems(player, itemId, count, neededCount, dropChance, DROP_DIVMOD);
	}

	/**
	 * Drop items to the {@link Player}'s inventory.
	 * @param player : The {@link Player} to drop items.
	 * @param itemId : Identifier of the item to be dropped.
	 * @param count : Quantity of items to be dropped.
	 * @param neededCount : Quantity of items needed to complete the task. If set to 0, unlimited amount is collected.
	 * @param dropChance : Item drop rate (100% chance is defined by the L2DropData.MAX_CHANCE = 1.000.000).
	 * @param type : Item drop behavior: DROP_DIVMOD (rate and), DROP_FIXED_RATE, DROP_FIXED_COUNT or DROP_FIXED_BOTH.
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public static boolean dropItems(Player player, int itemId, int count, int neededCount, int dropChance, byte type)
	{
		// Get current amount of item.
		final int currentCount = player.getInventory().getItemCount(itemId);

		// Required amount reached already?
		if (neededCount > 0 && currentCount >= neededCount)
		{
			return true;
		}

		int amount = 0;
		switch (type)
		{
			case DROP_DIVMOD:
				dropChance *= player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_DROP : Config.RATE_QUEST_DROP;
				amount = count * (dropChance / DropData.MAX_CHANCE);
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance % DropData.MAX_CHANCE)
				{
					amount += count;
				}
				break;

			case DROP_FIXED_RATE:
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
				{
					amount = (int) (count * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_DROP : Config.RATE_QUEST_DROP));
				}
				break;

			case DROP_FIXED_COUNT:
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_DROP : Config.RATE_QUEST_DROP))
				{
					amount = count;
				}
				break;

			case DROP_FIXED_BOTH:
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
				{
					amount = count;
				}
				break;
		}

		boolean reached = false;
		if (amount > 0)
		{
			// Limit count to reach required amount.
			if (neededCount > 0)
			{
				reached = (currentCount + amount) >= neededCount;
				amount = (reached) ? neededCount - currentCount : amount;
			}

			// Inventory slot check.
			if (!player.getInventory().validateCapacityByItemId(itemId, amount))
			{
				return false;
			}

			// Give items to the player.
			giveItems(player, itemId, amount, 0);

			// Play the sound.
			playSound(player, reached ? SOUND_MIDDLE : SOUND_ITEMGET);
		}

		return neededCount > 0 && reached;
	}

	/**
	 * Drop multiple items to the {@link Player}'s inventory. Rate and amount is affected by DIVMOD of Config.RATE_QUEST_DROP.
	 * @param player : The {@link Player} to drop items.
	 * @param rewardsInfos : Infos regarding drops (itemId, count, neededCount, dropChance).
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public static boolean dropMultipleItems(Player player, int[][] rewardsInfos)
	{
		return dropMultipleItems(player, rewardsInfos, DROP_DIVMOD);
	}

	/**
	 * Drop items to the {@link Player}'s inventory.
	 * @param player : The {@link Player} to drop items.
	 * @param rewardsInfos : Infos regarding drops (itemId, count, neededCount, dropChance).
	 * @param type : Item drop behavior: DROP_DIVMOD (rate and), DROP_FIXED_RATE, DROP_FIXED_COUNT or DROP_FIXED_BOTH.
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public static boolean dropMultipleItems(Player player, int[][] rewardsInfos, byte type)
	{
		// Used for the sound.
		boolean sendSound = false;

		// Used for the reached state.
		boolean reached = true;

		// For each reward type, calculate the probability of drop.
		for (int[] info : rewardsInfos)
		{
			final int itemId = info[0];
			final int currentCount = player.getInventory().getItemCount(itemId);
			final int neededCount = info[2];

			// Required amount reached already?
			if (neededCount > 0 && currentCount >= neededCount)
			{
				continue;
			}

			final int count = info[1];

			int dropChance = info[3];
			int amount = 0;

			switch (type)
			{
				case DROP_DIVMOD:
					dropChance *= player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_DROP : Config.RATE_QUEST_DROP;
					amount = count * (dropChance / DropData.MAX_CHANCE);
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance % DropData.MAX_CHANCE)
					{
						amount += count;
					}
					break;

				case DROP_FIXED_RATE:
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
					{
						amount = (int) (count * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_DROP : Config.RATE_QUEST_DROP));
					}
					break;

				case DROP_FIXED_COUNT:
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_DROP : Config.RATE_QUEST_DROP))
					{
						amount = count;
					}
					break;

				case DROP_FIXED_BOTH:
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
					{
						amount = count;
					}
					break;
			}

			if (amount > 0)
			{
				// Limit count to reach required amount.
				if (neededCount > 0)
				{
					amount = ((currentCount + amount) >= neededCount) ? neededCount - currentCount : amount;
				}

				// Inventory slot check.
				if (!player.getInventory().validateCapacityByItemId(itemId, amount))
				{
					continue;
				}

				// Give items to the player.
				giveItems(player, itemId, amount, 0);

				// Send sound.
				sendSound = true;
			}

			// Illimited needed count or current count being inferior to needed count means the state isn't reached.
			if (neededCount <= 0 || ((currentCount + amount) < neededCount))
			{
				reached = false;
			}
		}

		// Play the sound.
		if (sendSound)
		{
			playSound(player, (reached) ? SOUND_MIDDLE : SOUND_ITEMGET);
		}

		return reached;
	}

	/**
	 * Reward {@link Player} with items. The amount is affected by Config.RATE_QUEST_REWARD or Config.RATE_QUEST_REWARD_ADENA.
	 * @param player : The {@link Player} to reward items.
	 * @param itemId : Identifier of the item.
	 * @param itemCount : Quantity of item to reward before applying multiplier.
	 */
	public static void rewardItems(Player player, int itemId, int itemCount)
	{
		if (itemId == 57)
		{
			giveItems(player, itemId, (int) (itemCount * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_REWARD_ADENA : Config.RATE_QUEST_REWARD_ADENA)), 0);
		}
		else
		{
			giveItems(player, itemId, (int) (itemCount * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_REWARD : Config.RATE_QUEST_REWARD)), 0);
		}
	}

	/**
	 * Reward ss or sps for beginners.
	 * @param player : The {@link Player} to reward shots.
	 * @param ssCount : The count of ss to reward.
	 * @param spsCount : The count of ss to reward.
	 */
	public void rewardNewbieShots(Player player, int ssCount, int spsCount)
	{
		// Don't process if not a newbie or already rewarded.
		if (!player.isNewbie(true) || player.getMemos().containsKey(getName() + "_OneTimeQuestFlag"))
		{
			return;
		}

		// Reward ss or sps, according Player class. Call Tutorial voice 26 or 27.
		final boolean isMage = player.isMageClass() && player.getClassId() != ClassId.ORC_MYSTIC && player.getClassId() != ClassId.ORC_SHAMAN;
		if (spsCount > 0 && isMage)
		{
			showQuestionMark(player, 26);

			rewardItems(player, 5790, spsCount);
			playTutorialVoice(player, "tutorial_voice_027");
		}
		else if (ssCount > 0 && !isMage)
		{
			showQuestionMark(player, 26);

			rewardItems(player, 5789, ssCount);
			playTutorialVoice(player, "tutorial_voice_026");
		}

		// Set the Memo for further usage.
		player.getMemos().set(getName() + "_OneTimeQuestFlag", true);
	}

	/**
	 * Reward {@link Player} with EXP and SP. The amount is affected by Config.RATE_QUEST_REWARD_XP and Config.RATE_QUEST_REWARD_SP.
	 * @param player : The {@link Player} to add EXP and SP.
	 * @param exp : Experience amount.
	 * @param sp : Skill point amount.
	 */
	public static void rewardExpAndSp(Player player, long exp, int sp)
	{
		player.addExpAndSp((long) (exp * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_REWARD_XP : Config.RATE_QUEST_REWARD_XP)), (int) (sp * (player.getPremiumService() == 1 ? Config.PREMIUM_RATE_QUEST_REWARD_SP : Config.RATE_QUEST_REWARD_SP)));
	}

	/**
	 * Send a packet in order to play sound at client terminal.
	 * @param player : The {@link Player} to play sound.
	 * @param sound : The sound name to be played.
	 */
	public static void playSound(Player player, String sound)
	{
		player.sendPacket(new PlaySound(sound));
	}

	public static void showQuestionMark(Player player, int number)
	{
		player.sendPacket(new TutorialShowQuestionMark(number));
	}

	public static void playTutorialVoice(Player player, String voice)
	{
		player.sendPacket(new PlaySound(2, voice, player));
	}

	public static void showTutorialHTML(Player player, String html)
	{
		player.sendPacket(new TutorialShowHtml(HtmCache.getInstance().getHtmForce(player.isLang() + "script/feature/Tutorial/" + html)));
	}

	public static void closeTutorialHtml(Player player)
	{
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}

	public static void onTutorialClientEvent(Player player, int number)
	{
		player.sendPacket(new TutorialEnableClientEvent(number));
	}

	public static void callSkill(Creature caster, Creature target, L2Skill skill)
	{
		// Send animation.
		caster.broadcastPacket(new MagicSkillUse(caster, target, skill.getId(), skill.getLevel(), skill.getHitTime(), 0));

		// Define target.
		final Creature[] targets = new Creature[]
		{
			target
		};

		// Handle the effect.
		final ISkillHandler handler = SkillHandler.getInstance().getHandler(skill.getSkillType());
		if (handler != null)
		{
			handler.useSkill(caster, skill, targets, null);
		}
		else
		{
			skill.useSkill(caster, targets);
		}
	}

	/**
	 * @return The default html page "You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements."
	 */
	public static final String getNoQuestMsg()
	{
		return HTML_NONE_AVAILABLE;
	}

	/**
	 * @return The default html page "This quest has already been completed."
	 */
	public static final String getAlreadyCompletedMsg()
	{
		return HTML_ALREADY_COMPLETED;
	}

	/**
	 * @return The default html page "You have already accepted the maximum number of quests. No more than 25 quests may be undertaken simultaneously. For quest information, enter Alt+U."
	 */
	public static final String getTooMuchQuestsMsg()
	{
		return HTML_TOO_MUCH_QUESTS;
	}

	/**
	 * Show a message to {@link Player}.
	 * @param npc : The {@link Npc} which gives the result, null in case of random scripts.
	 * @param creature : The {@link Creature} to whom the result is dedicated. May be {@link Summon}.
	 * @param result : The result message:
	 *            <ul>
	 *            <li><u>Ends with {@code .html}:</u> A HTML file to be shown in a dialog box.</li>
	 *            <li><u>Starts with {@code <html>}:</u> The html text to be shown in a dialog box.</li>
	 *            <li><u>otherwise:</u> The message to be shown in a chat box.</li>
	 *            </ul>
	 */
	private void showResult(Npc npc, Creature creature, String result)
	{
		if (creature == null || result == null || result.isEmpty())
		{
			return;
		}

		final Player player = creature.getActingPlayer();
		if (player == null)
		{
			return;
		}

		if (result.endsWith(".htm") || result.endsWith(".html"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc == null ? 0 : npc.getObjectId());
			if (isRealQuest())
			{
				html.setFile(player.isLang() + "script/quest/" + getName() + "/" + result);
			}
			else
			{
				html.setFile(player.isLang() + "script/" + getDescr() + "/" + getName() + "/" + result);
			}

			if (npc != null)
			{
				html.replace("%objectId%", npc.getObjectId());
			}

			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (result.startsWith("<html>"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc == null ? 0 : npc.getObjectId());
			html.setHtml(result);

			if (npc != null)
			{
				html.replace("%objectId%", npc.getObjectId());
			}

			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.sendMessage(result);
		}
	}

	/**
	 * @param player
	 * @param fileName : The filename to send.
	 * @return The {@link String} content of the given quest/script/AI html.
	 */
	public final String getHtmlText(Player player, String fileName)
	{
		if (isRealQuest())
		{
			return HtmCache.getInstance().getHtmForce(player.isLang() + "script/quest/" + getName() + "/" + fileName);
		}

		return HtmCache.getInstance().getHtmForce(player.isLang() + "script/" + getDescr() + "/" + getName() + "/" + fileName);
	}

	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} id and {@link EventHandler}.
	 * @param npcId : The id of the {@link Npc}.
	 * @param eventType : The type of {@link EventHandler} to be registered.
	 */
	public final void addEventId(int npcId, EventHandler eventType)
	{
		final NpcTemplate t = NpcData.getInstance().getTemplate(npcId);
		if (t != null)
		{
			t.addQuestEvent(eventType, this);
		}
	}

	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} ids and {@link EventHandler}.
	 * @param npcIds : The ids of the {@link Npc}.
	 * @param eventType : The type of {@link EventHandler} to be registered.
	 */
	public final void addEventIds(int[] npcIds, EventHandler eventType)
	{
		for (int id : npcIds)
		{
			final NpcTemplate t = NpcData.getInstance().getTemplate(id);
			if (t != null)
			{
				t.addQuestEvent(eventType, this);
			}
		}
	}

	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} id and {@link EventHandler}s.
	 * @param npcId : The id of the {@link Npc}.
	 * @param eventTypes : Types of {@link EventHandler}s to be registered.
	 */
	public final void addEventIds(int npcId, EventHandler... eventTypes)
	{
		final NpcTemplate t = NpcData.getInstance().getTemplate(npcId);
		if (t != null)
		{
			for (EventHandler eventType : eventTypes)
			{
				t.addQuestEvent(eventType, this);
			}
		}
	}

	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} ids and {@link EventHandler}s.
	 * @param npcIds : The ids of the {@link Npc}.
	 * @param eventTypes : Types of {@link EventHandler}s to be registered.
	 */
	public final void addEventIds(int[] npcIds, EventHandler... eventTypes)
	{
		for (int id : npcIds)
		{
			addEventIds(id, eventTypes);
		}
	}

	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} ids and {@link EventHandler}s.
	 * @param npcIds : The ids of the {@link Npc}.
	 * @param eventTypes : Types of {@link EventHandler}s to be registered.
	 */
	public final void addEventIds(Iterable<Integer> npcIds, EventHandler... eventTypes)
	{
		for (int id : npcIds)
		{
			addEventIds(id, eventTypes);
		}
	}

	/**
	 * Quest event listener for {@link Player} (his {@link Summon}) reacting on various generic events:
	 * <ul>
	 * <li>{@link Npc} bypasses.</li>
	 * <li>{@link QuestTimer} events.</li>
	 * <li>Tutorial quest events (low HP, death, specific item pick-up, specific client packets).</li>
	 * </ul>
	 * @param event : The name of the event.
	 * @param npc : The interacted {@link Npc} (can be null).
	 * @param player : The interacted {@link Player} (his {@link Summon}).
	 */
	public final void notifyEvent(String event, Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onAdvEvent(event, npc, player);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, player, res);
	}

	/**
	 * Generic quest event for {@link Player} (his {@link Summon}) reacting on various generic events:
	 * <ul>
	 * <li>{@link Npc} bypasses.</li>
	 * <li>{@link QuestTimer} events.</li>
	 * <li>Tutorial quest events (low HP, death, specific item pick-up, specific client packets).</li>
	 * </ul>
	 * @param event : The name the event.
	 * @param npc : The interacted {@link Npc} (can be null).
	 * @param player : The interacted {@link Player} (his {@link Summon}).
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		return null;
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to being under attack event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addAttacked(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.ATTACKED);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to being under attack event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addAttacked(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.ATTACKED);
	}

	/**
	 * Attack quest event for {@link Creature} attacking the {@link Npc}.
	 * @param npc : Attacked {@link Npc}.
	 * @param attacker : Attacking {@link Creature}.
	 * @param damage : Given damage.
	 * @param skill : The {@link L2Skill} used to attack the {@link Npc}.
	 */
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to attacking {@link Player} (his {@link Summon}) event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addAttackFinished(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.ATTACK_FINISHED);
	}

	/**
	 * Attack act quest event for {@link Npc} attacking {@link Player} (his {@link Summon}).
	 * @param npc : Attacking {@link Npc}.
	 * @param player : Attacked {@link Player}(his {@link Summon}).
	 */
	public void onAttackFinished(Npc npc, Player player)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to ON_CLAN_ATTACKED event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addClanAttacked(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.CLAN_ATTACKED);
	}

	/**
	 * Quest event for {@link Attackable} performing ON_CLAN_ATTACKED event on another {@link Attackable}.
	 * @param caller : The {@link Attackable} calling for assistance.
	 * @param called : The {@link Attackable} called by {@link Attackable} caller to assist.
	 * @param attacker : The {@link Creature} attacker affected by caller/called.
	 * @param damage : The damage done to the {@link Attackable} caller.
	 */
	public void onClanAttacked(Attackable caller, Attackable called, Creature attacker, int damage)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to CLAN_DIED event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addClanDied(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.CLAN_DIED);
	}

	/**
	 * Quest event for {@link Npc} performing CLAN_DIED event on another {@link Npc}.
	 * @param caller : The {@link Npc} calling for assistance.
	 * @param called : The {@link Npc} called by {@link Npc} caller to assist.
	 */
	public void onClanDied(Npc caller, Npc called)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its spawn event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addCreated(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.CREATED);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its spawn event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addCreated(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.CREATED);
	}

	/**
	 * Quest event for {@link Npc} being spawned into the world.
	 * @param npc : Spawned {@link Npc}.
	 */
	public void onCreated(Npc npc)
	{
	}

	/**
	 * Quest event for {@link Player} (his {@link Summon}) being killed by a {@link Creature}.
	 * @param killer : Killing {@link Creature}.
	 * @param player : Killed {@link Player}.
	 */
	public void onDeath(Creature killer, Player player)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its decay event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addDecayed(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.DECAYED);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its decay event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addDecayed(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.DECAYED);
	}

	/**
	 * Quest event for {@link Npc} being decayed from the world.
	 * @param npc : The decayed {@link Npc}.
	 */
	public void onDecayed(Npc npc)
	{
	}

	/**
	 * Add this quest to the list of quests that triggers, when door opens/closes.
	 * @param doorIds : A serie of door ids.
	 */
	public void addDoorChange(int... doorIds)
	{
		for (int doorId : doorIds)
		{
			final Door door = DoorData.getInstance().getDoor(doorId);
			if (door != null)
			{
				door.addQuestEvent(this);
			}
		}
	}

	public void onDoorChange(Door door)
	{
	}

	/**
	 * Quest event for {@link Player} entering the world.
	 * @param player : Entering {@link Player}.
	 */
	public void onEnterWorld(Player player)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will override initial dialog with this {@link Quest}.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addFirstTalkId(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.FIRST_TALK);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will override initial dialog with this {@link Quest}.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addFirstTalkId(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.FIRST_TALK);
	}

	/**
	 * Quest event listener for {@link Npc} having initial dialog by this {@link Quest}.
	 * @param npc : Talked {@link Npc}.
	 * @param player : Talker {@link Player}.
	 */
	public final void notifyFirstTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}

		// if the quest returns text to display, display it.
		if (res != null && res.length() > 0)
		{
			showResult(npc, player, res);
		}
		else
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	/**
	 * Quest event for {@link Npc} having initial dialog by this {@link Quest}.
	 * @param npc : Talked {@link Npc}.
	 * @param player : Talker {@link Player}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}

	/**
	 * Register this {@link Quest} to be notified by time change (each in-game minute).
	 */
	public final void addGameTime()
	{
		GameTimeTaskManager.getInstance().addQuestEvent(this);
	}

	/**
	 * Quest event for time change (each in-game minute).
	 * @param gameTime : The current game time. Range 0-1439 minutes per game day corresponds 00:00-23:59 time.
	 */
	public void onGameTime(int gameTime)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Item}, which will respond to {@link Item} use.
	 * @param itemIds : The ids of the {@link Item}.
	 */
	public final void addItemUse(int... itemIds)
	{
		for (int itemId : itemIds)
		{
			final Item item = ItemData.getInstance().getTemplate(itemId);
			if (item != null)
			{
				item.addQuestEvent(this);
			}
		}
	}

	/**
	 * Quest event for {@link Item} being used by {@link Player}.
	 * @param item : {@link Item} being used.
	 * @param player : {@link Player} using it.
	 * @param target : {@link Player}'s target.
	 */
	public void onItemUse(ItemInstance item, Player player, WorldObject target)
	{
	}

	/**
	 * Add the quest to an array of {@link NpcMaker} names.
	 * @param names : A serie of names.
	 */
	public void addMakerNpcsKilledByName(String... names)
	{
		for (String name : names)
		{
			SpawnManager.getInstance().addQuestEventByName(name, this);
		}
	}

	/**
	 * Add the quest to an array of {@link NpcMaker} names.
	 * @param names : A collection of names.
	 */
	public void addMakerNpcsKilledByName(Iterable<String> names)
	{
		for (String name : names)
		{
			SpawnManager.getInstance().addQuestEventByName(name, this);
		}
	}

	/**
	 * Add the quest to an array of {@link NpcMaker} event names.
	 * @param events : A serie of event names.
	 */
	public void addMakerNpcsKilledByEvent(String... events)
	{
		for (String event : events)
		{
			SpawnManager.getInstance().addQuestEventByEvent(event, this);
		}
	}

	/**
	 * Add the quest to an array of {@link NpcMaker} event names.
	 * @param events : A collection of event names.
	 */
	public void addMakerNpcsKilledByEvent(Iterable<String> events)
	{
		for (String event : events)
		{
			SpawnManager.getInstance().addQuestEventByEvent(event, this);
		}
	}

	/**
	 * Quest event for last {@link Npc} being killed in {@link NpcMaker}.
	 * @param maker : The notified {@link NpcMaker}.
	 * @param npc : The last killed {@link Npc}.
	 */
	public void onMakerNpcsKilled(NpcMaker maker, Npc npc)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to kill event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addMyDying(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.MY_DYING);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to kill event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addMyDying(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.MY_DYING);
	}

	/**
	 * Quest event for {@link Npc} being killed by {@link Creature}.
	 * @param npc : Killed {@link Npc}.
	 * @param killer : Killer {@link Creature}.
	 */
	public void onMyDying(Npc npc, Creature killer)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link EventHandler#NO_DESIRE} event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addNoDesire(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.NO_DESIRE);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link EventHandler#NO_DESIRE} event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addNoDesireId(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.NO_DESIRE);
	}

	/**
	 * Quest event for {@link Npc} reacting to {@link EventHandler#NO_DESIRE} event.
	 * @param npc : The {@link Npc} to affect.
	 */
	public void onNoDesire(Npc npc)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its out of territory event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addOutOfTerritory(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.OUT_OF_TERRITORY);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its out of territory event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addOutOfTerritory(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.OUT_OF_TERRITORY);
	}

	/**
	 * Quest event for {@link Npc} being out of territory.
	 * @param npc : The {@link Npc} which is out of territory.
	 */
	public void onOutOfTerritory(Npc npc)
	{
	}

	/**
	 * Register this {@link Quest} to npcIds which will respond to {@link EventHandler#PARTY_ATTACKED}.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addPartyAttacked(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.PARTY_ATTACKED);
	}

	/**
	 * Quest event for {@link Npc} requesting party help from another {@link Npc}.
	 * @param caller : {@link Npc} requester.
	 * @param called : {@link Npc} requested.
	 * @param target : The {@link Creature} target affected by caller/called.
	 * @param damage : The damage done to the {@link Npc} caller.
	 */
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
	}

	/**
	 * Register this {@link Quest} to npcIds which will respond to {@link EventHandler#PARTY_DIED}.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addPartyDied(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.PARTY_DIED);
	}

	/**
	 * Quest event for {@link Npc} requesting party kill from another {@link Npc}.
	 * @param caller : {@link Npc} requester.
	 * @param called : {@link Npc} requested.
	 */
	public void onPartyDied(Npc caller, Npc called)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which may start it.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addQuestStart(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.QUEST_START);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link Npc} seeing other {@link Creature} within 400 range.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addSeeCreature(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.SEE_CREATURE);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link Npc} seeing other {@link Creature} within 400 range.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addSeeCreature(Collection<Integer> npcIds)
	{
		addEventIds(npcIds, EventHandler.SEE_CREATURE);
	}

	/**
	 * Creature see quest event for {@link Npc} seeing a {@link Creature} within 400 range.
	 * @param npc : Seeing {@link Npc}.
	 * @param creature : Seen {@link Creature}.
	 */
	public void onSeeCreature(Npc npc, Creature creature)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its out of territory event.
	 * @param ids : The ids of the {@link Npc}.
	 */
	public final void addSeeItem(int... ids)
	{
		addEventIds(ids, EventHandler.SEE_ITEM);
	}

	/**
	 * Quest event for {@link Npc} seeing particular {@link ItemInstance}s.
	 * @param npc : The {@link Npc} which is out of territory.
	 * @param quantity : The quantity of items to check.
	 * @param items : The {@link List} of {@link ItemInstance}s to check.
	 */
	public void onSeeItem(Npc npc, int quantity, List<ItemInstance> items)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to seeing other skill casted event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addSeeSpell(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.SEE_SPELL);
	}

	/**
	 * Quest event for {@link Npc} seeing a skill casted by {@link Player} (his {@link Summon}).
	 * @param npc : Noticing {@link Npc}.
	 * @param caster : {@link Player} casting the {@link Skill}.
	 * @param skill : Casted {@link Skill}.
	 * @param targets : Affected targets.
	 * @param isPet : Marks {@link Player}'s {@link Summon} is casting.
	 */
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Castle}, which will respond to siege status change event.
	 * @param castleId : The id of the {@link Castle}.
	 */
	public final void addSiegeNotify(int castleId)
	{
		// Castle always have a Siege, just register it.
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		if (castle != null)
		{
			castle.getSiege().addQuestEvent(this);
		}
	}

	/**
	 * Quest event for {@link Castle} having a {@link Siege} status change event.
	 * @param siege : Notified {@link Siege}.
	 */
	public void onSiegeEvent(Siege siege)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link Player} talk event.
	 * @param talkIds : The ids of the {@link Npc}.
	 */
	public final void addTalkId(int... talkIds)
	{
		addEventIds(talkIds, EventHandler.TALKED);
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link Player} talk event.
	 * @param talkIds : The ids of the {@link Npc}.
	 */
	public final void addTalkId(Collection<Integer> talkIds)
	{
		addEventIds(talkIds, EventHandler.TALKED);
	}

	/**
	 * Quest event listener for {@link Npc} reacting on {@link Player} talking about this {@link Quest}.
	 * @param npc : Talked {@link Npc}.
	 * @param player : Talking {@link Player}.
	 */
	public final void notifyTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onTalk(npc, player);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, player, res);
	}

	/**
	 * Quest event for {@link Npc} reacting on {@link Player} talking about this {@link Quest}.
	 * @param npc : Talked {@link Npc}.
	 * @param player : Talking {@link Player}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onTalk(Npc npc, Player player)
	{
		return null;
	}

	/**
	 * Quest event listener for {@link QuestTimer} ticking (repeating) or expiring (non-repeating).
	 * @param name : The name of the timer.
	 * @param npc : The {@link Npc} associated with the timer (optional, can be null).
	 * @param player : The {@link Player} associated with the timer (optional, can be null).
	 */
	public final void notifyTimer(String name, Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onTimer(name, npc, player);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, player, res);
	}

	/**
	 * Quest event for {@link QuestTimer} ticking (repeating) or expiring (non-repeating).
	 * @param name : The name of the timer.
	 * @param npc : The {@link Npc} associated with the timer (optional, can be null).
	 * @param player : The {@link Player} associated with the timer (optional, can be null).
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onTimer(String name, Npc npc, Player player)
	{
		return null;
	}

	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to itself casting skill event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addUseSkillFinished(int... npcIds)
	{
		addEventIds(npcIds, EventHandler.USE_SKILL_FINISHED);
	}

	/**
	 * Quest event for {@link Npc} casting a skill on {@link Player} (his {@link Summon}).
	 * @param npc : Casting {@link Npc}.
	 * @param player : Target {@link Player} (his {@link Summon}).
	 * @param skill : Casted {@link Skill}.
	 */
	public void onUseSkillFinished(Npc npc, Player player, L2Skill skill)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link ZoneType}, which will respond to {@link Creature} entering it.
	 * @param zoneIds : The ids of the {@link ZoneType}.
	 */
	public final void addZoneEnter(int... zoneIds)
	{
		for (int zoneId : zoneIds)
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
			{
				zone.addQuestEvent(EventHandler.ZONE_ENTER, this);
			}
		}
	}

	/**
	 * Quest event for {@link Creature} entering the {@link ZoneType}.
	 * @param creature : Entering {@link Creature}.
	 * @param zone : Specified {@link ZoneType}.
	 */
	public void onZoneEnter(Creature creature, ZoneType zone)
	{
	}

	/**
	 * Register this {@link Quest} to the {@link ZoneType}, which will respond to {@link Player} leaving it.
	 * @param zoneIds : The ids of the {@link ZoneType}.
	 */
	public final void addZoneExit(int... zoneIds)
	{
		for (int zoneId : zoneIds)
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
			{
				zone.addQuestEvent(EventHandler.ZONE_EXIT, this);
			}
		}
	}

	/**
	 * Quest event for {@link Creature} leaving the {@link ZoneType}.
	 * @param creature : Leaving {@link Creature}.
	 * @param zone : Specified {@link ZoneType}.
	 */
	public void onZoneExit(Creature creature, ZoneType zone)
	{
	}
}