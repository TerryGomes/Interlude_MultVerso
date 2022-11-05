package net.sf.l2j.gameserver.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.DocumentSkill.Skill;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.ScriptEventType;
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
			return true;
		
		if (o instanceof Quest)
		{
			Quest q = (Quest) o;
			if (_id > 0 && _id == q._id)
				return getName().equals(q.getName());
			
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
			return null;
		
		// Check player's quest conditions.
		final QuestState st = player.getQuestList().getQuestState(getName());
		if (st == null)
			return null;
		
		// Check quest's condition.
		if (st.getCond() != cond)
			return null;
		
		// Player is in range?
		if (!player.isIn3DRadius(npc, Config.PARTY_RANGE))
			return null;
		
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
			return null;
		
		// Check player's quest conditions.
		final QuestState st = player.getQuestList().getQuestState(getName());
		if (st == null)
			return null;
		
		// Check variable and its value.
		final String toCheck = st.get(variable);
		if (toCheck == null || !value.equalsIgnoreCase(toCheck))
			return null;
		
		// Player is in range?
		if (!player.isIn3DRadius(npc, Config.PARTY_RANGE))
			return null;
		
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
		if (leaderQs == null)
			return null;
		
		// Check quest's condition.
		if (leaderQs.getCond() != cond)
			return null;
		
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
			return null;
		
		// Check variable and its value.
		final String toCheck = leaderQs.get(variable);
		if (toCheck == null || !value.equalsIgnoreCase(toCheck))
			return null;
		
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
		if (leaderQs == null)
			return null;
		
		// State correct?
		if (leaderQs.getState() != state)
			return null;
		
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
			return Collections.emptyList();
		
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
				list.add(st);
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
			return Collections.emptyList();
		
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
				list.add(st);
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
			return null;
		
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
			return null;
		
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
			return null;
		
		// Check player's quest conditions.
		final QuestState st = player.getQuestList().getQuestState(getName());
		if (st == null)
			return null;
		
		// Check quest's state.
		if (st.getState() != state)
			return null;
		
		// Player is in range?
		if (!player.isIn3DRadius(npc, Config.PARTY_RANGE))
			return null;
		
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
			return Collections.emptyList();
		
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
				list.add(st);
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
			return null;
		
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
		if (player == null)
			return null;
		
		// Player killer must be in range with npc.
		if (npc != null && !player.isIn3DRadius(npc, Config.PARTY_RANGE))
			return null;
		
		// If player is the leader, retrieves directly the qS and bypass others checks
		if (player.isClanLeader())
			return player.getQuestList().getQuestState(getName());
		
		// Verify if the player got a clan
		final Clan clan = player.getClan();
		if (clan == null)
			return null;
		
		// Verify if the leader is online
		final Player leader = clan.getLeader().getPlayerInstance();
		if (leader == null)
			return null;
		
		// Verify if the leader is on the radius of the npc.
		if (npc != null && !leader.isIn3DRadius(npc, Config.PARTY_RANGE))
			return null;
		
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
			return false;
		
		// Player hasn't a clan.
		final Clan clan = player.getClan();
		if (clan == null)
			return false;
		
		// Retrieve sponsor clan member object.
		final ClanMember member = clan.getClanMember(sponsorId);
		if (member != null && member.isOnline())
		{
			// The sponsor is online, retrieve player instance and check distance.
			final Player sponsor = member.getPlayerInstance();
			if (sponsor != null && player.isIn3DRadius(sponsor, 1500))
				return true;
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
			return null;
		
		// Player hasn't a clan.
		final Clan clan = player.getClan();
		if (clan == null)
			return null;
		
		// Retrieve apprentice clan member object.
		final ClanMember member = clan.getClanMember(apprenticeId);
		if (member != null && member.isOnline())
		{
			// The apprentice is online, retrieve player instance and check distance.
			final Player academic = member.getPlayerInstance();
			if (academic != null && player.isIn3DRadius(academic, 1500))
				return academic;
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
			return false;
		
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
			return;
		
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
				return null;
			
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
				z = GeoEngine.getInstance().getHeight(x, y, z);
			
			// Create spawn.
			final Spawn spawn = new Spawn(template);
			spawn.setLoc(x, y, z, heading);
			spawn.setRespawnState(false);
			
			// Spawn NPC.
			final Npc npc = spawn.doSpawn(isSummonSpawn);
			if (despawnDelay > 0)
				npc.scheduleDespawn(despawnDelay);
			
			return npc;
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't spawn npcId {} for {}.", npcId, toString());
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
			return;
		
		// Add items to player's inventory.
		final ItemInstance item = player.getInventory().addItem("Quest", itemId, itemCount, player, player);
		if (item == null)
			return;
		
		// Set enchant level for the item.
		if (enchantLevel > 0)
			item.setEnchantLevel(enchantLevel);
		
		// Send message to the client.
		if (itemId == 57)
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA).addItemNumber(itemCount));
		else
		{
			if (itemCount > 1)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addItemNumber(itemCount));
			else
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
			return;
		
		if (template.isStackable())
		{
			// Find item in player's inventory.
			final ItemInstance item = player.getInventory().getItemByItemId(itemId);
			if (item == null)
				return;
			
			// Tests on count value and set correct value if necessary.
			if (itemCount < 0 || itemCount > item.getCount())
				itemCount = item.getCount();
			
			// Disarm item, if equipped.
			if (item.isEquipped())
			{
				InventoryUpdate iu = new InventoryUpdate();
				for (ItemInstance disarmed : player.getInventory().unequipItemInBodySlotAndRecord(item))
					iu.addModifiedItem(disarmed);
				
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
					break;
				
				// Disarm item, if equipped.
				if (item.isEquipped())
				{
					InventoryUpdate iu = new InventoryUpdate();
					for (ItemInstance disarmed : player.getInventory().unequipItemInBodySlotAndRecord(item))
						iu.addModifiedItem(disarmed);
					
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
			return true;
		
		int amount = 0;
		switch (type)
		{
			case DROP_DIVMOD:
				dropChance *= Config.RATE_QUEST_DROP;
				amount = count * (dropChance / DropData.MAX_CHANCE);
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance % DropData.MAX_CHANCE)
					amount += count;
				break;
			
			case DROP_FIXED_RATE:
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
					amount = (int) (count * Config.RATE_QUEST_DROP);
				break;
			
			case DROP_FIXED_COUNT:
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance * Config.RATE_QUEST_DROP)
					amount = count;
				break;
			
			case DROP_FIXED_BOTH:
				if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
					amount = count;
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
				return false;
			
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
				continue;
			
			final int count = info[1];
			
			int dropChance = info[3];
			int amount = 0;
			
			switch (type)
			{
				case DROP_DIVMOD:
					dropChance *= Config.RATE_QUEST_DROP;
					amount = count * (dropChance / DropData.MAX_CHANCE);
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance % DropData.MAX_CHANCE)
						amount += count;
					break;
				
				case DROP_FIXED_RATE:
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
						amount = (int) (count * Config.RATE_QUEST_DROP);
					break;
				
				case DROP_FIXED_COUNT:
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance * Config.RATE_QUEST_DROP)
						amount = count;
					break;
				
				case DROP_FIXED_BOTH:
					if (Rnd.get(DropData.MAX_CHANCE) < dropChance)
						amount = count;
					break;
			}
			
			if (amount > 0)
			{
				// Limit count to reach required amount.
				if (neededCount > 0)
					amount = ((currentCount + amount) >= neededCount) ? neededCount - currentCount : amount;
				
				// Inventory slot check.
				if (!player.getInventory().validateCapacityByItemId(itemId, amount))
					continue;
				
				// Give items to the player.
				giveItems(player, itemId, amount, 0);
				
				// Send sound.
				sendSound = true;
			}
			
			// Illimited needed count or current count being inferior to needed count means the state isn't reached.
			if (neededCount <= 0 || ((currentCount + amount) < neededCount))
				reached = false;
		}
		
		// Play the sound.
		if (sendSound)
			playSound(player, (reached) ? SOUND_MIDDLE : SOUND_ITEMGET);
		
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
			giveItems(player, itemId, (int) (itemCount * Config.RATE_QUEST_REWARD_ADENA), 0);
		else
			giveItems(player, itemId, (int) (itemCount * Config.RATE_QUEST_REWARD), 0);
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
			return;
		
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
		player.addExpAndSp((long) (exp * Config.RATE_QUEST_REWARD_XP), (int) (sp * Config.RATE_QUEST_REWARD_SP));
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
		player.sendPacket(new TutorialShowHtml(HtmCache.getInstance().getHtmForce("data/html/script/feature/Tutorial/" + html)));
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
			handler.useSkill(caster, skill, targets);
		else
			skill.useSkill(caster, targets);
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
			return;
		
		final Player player = creature.getActingPlayer();
		if (player == null)
			return;
		
		if (result.endsWith(".htm") || result.endsWith(".html"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc == null ? 0 : npc.getObjectId());
			if (isRealQuest())
				html.setFile("./data/html/script/quest/" + getName() + "/" + result);
			else
				html.setFile("./data/html/script/" + getDescr() + "/" + getName() + "/" + result);
			
			if (npc != null)
				html.replace("%objectId%", npc.getObjectId());
			
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (result.startsWith("<html>"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc == null ? 0 : npc.getObjectId());
			html.setHtml(result);
			
			if (npc != null)
				html.replace("%objectId%", npc.getObjectId());
			
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
			player.sendMessage(result);
	}
	
	/**
	 * @param fileName : The filename to send.
	 * @return The {@link String} content of the given quest/script/AI html.
	 */
	public final String getHtmlText(String fileName)
	{
		if (isRealQuest())
			return HtmCache.getInstance().getHtmForce("./data/html/script/quest/" + getName() + "/" + fileName);
		
		return HtmCache.getInstance().getHtmForce("./data/html/script/" + getDescr() + "/" + getName() + "/" + fileName);
	}
	
	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} id and {@link ScriptEventType}.
	 * @param npcId : The id of the {@link Npc}.
	 * @param eventType : The type of {@link ScriptEventType} to be registered.
	 */
	public final void addEventId(int npcId, ScriptEventType eventType)
	{
		final NpcTemplate t = NpcData.getInstance().getTemplate(npcId);
		if (t != null)
			t.addQuestEvent(eventType, this);
	}
	
	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} ids and {@link ScriptEventType}.
	 * @param npcIds : The ids of the {@link Npc}.
	 * @param eventType : The type of {@link ScriptEventType} to be registered.
	 */
	public final void addEventIds(int[] npcIds, ScriptEventType eventType)
	{
		for (int id : npcIds)
		{
			final NpcTemplate t = NpcData.getInstance().getTemplate(id);
			if (t != null)
				t.addQuestEvent(eventType, this);
		}
	}
	
	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} id and {@link ScriptEventType}s.
	 * @param npcId : The id of the {@link Npc}.
	 * @param eventTypes : Types of {@link ScriptEventType}s to be registered.
	 */
	public final void addEventIds(int npcId, ScriptEventType... eventTypes)
	{
		final NpcTemplate t = NpcData.getInstance().getTemplate(npcId);
		if (t != null)
			for (ScriptEventType eventType : eventTypes)
				t.addQuestEvent(eventType, this);
	}
	
	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} ids and {@link ScriptEventType}s.
	 * @param npcIds : The ids of the {@link Npc}.
	 * @param eventTypes : Types of {@link ScriptEventType}s to be registered.
	 */
	public final void addEventIds(int[] npcIds, ScriptEventType... eventTypes)
	{
		for (int id : npcIds)
			addEventIds(id, eventTypes);
	}
	
	/**
	 * Register this {@link Quest} to {@link Npc}'s events by defined {@link Npc} ids and {@link ScriptEventType}s.
	 * @param npcIds : The ids of the {@link Npc}.
	 * @param eventTypes : Types of {@link ScriptEventType}s to be registered.
	 */
	public final void addEventIds(Iterable<Integer> npcIds, ScriptEventType... eventTypes)
	{
		for (int id : npcIds)
			addEventIds(id, eventTypes);
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which may start it.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addStartNpc(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.QUEST_START);
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to being under attack event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addAttackId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_ATTACK);
	}
	
	/**
	 * Quest event listener for {@link Npc} being under attack.
	 * @param npc : Attacked {@link Npc} instance.
	 * @param attacker : The {@link Creature} who attacks the {@link Npc}.
	 * @param damage : The amount of given damage.
	 * @param skill : The skill used to attack the {@link Npc} (can be null).
	 */
	public final void notifyAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAttack(npc, attacker, damage, skill);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, attacker, res);
	}
	
	/**
	 * Attack quest event for {@link Creature} attacking the {@link Npc}.
	 * @param npc : Attacked {@link Npc}.
	 * @param attacker : Attacking {@link Creature}.
	 * @param damage : Given damage.
	 * @param skill : The {@link L2Skill} used to attack the {@link Npc}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to attacking {@link Player} (his {@link Summon}) event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addAttackActId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_ATTACK_ACT);
	}
	
	/**
	 * Quest event listener for {@link Player} (his {@link Summon}) being under attack by {@link Npc}.
	 * @param npc : Attacking {@link Npc}.
	 * @param victim : Attacked {@link Player} (his {@link Summon}).
	 */
	public final void notifyAttackAct(Npc npc, Player victim)
	{
		String res = null;
		try
		{
			res = onAttackAct(npc, victim);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, victim, res);
	}
	
	/**
	 * Attack act quest event for {@link Npc} attacking {@link Player} (his {@link Summon}).
	 * @param npc : Attacking {@link Npc}.
	 * @param victim : Attacked {@link Player}(his {@link Summon}).
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onAttackAct(Npc npc, Player victim)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link Player} (his {@link Summon}) gaining aggro event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addAggroRangeEnterId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_AGGRO);
	}
	
	/**
	 * Quest event listener for {@link Player} (his {@link Summon}) entering {@link Npc}'s aggro range.
	 * @param npc : Noticing {@link Npc}.
	 * @param player : Entering {@link Player} (his {@link Summon}).
	 * @param isPet : Marks {@link Player}'s {@link Summon} is entering.
	 */
	public final void notifyAggro(Npc npc, Player player, boolean isPet)
	{
		ThreadPool.execute(() ->
		{
			String res = null;
			try
			{
				res = onAggro(npc, player, isPet);
			}
			catch (Exception e)
			{
				LOGGER.warn(toString(), e);
				return;
			}
			showResult(npc, player, res);
		});
	}
	
	/**
	 * Aggro range enter quest event for {@link Player} (his {@link Summon}) entering {@link Npc}'s aggro range.
	 * @param npc : Noticing {@link Npc}.
	 * @param player : Entering {@link Player} (his {@link Summon}).
	 * @param isPet : Marks {@link Player}'s {@link Summon} is entering.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onAggro(Npc npc, Player player, boolean isPet)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link Npc} seeing other {@link Creature} within 400 range.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addCreatureSeeId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_CREATURE_SEE);
	}
	
	/**
	 * Quest event listener for {@link Npc} seeing other {@link Creature} within 400 range.
	 * @param npc : Seeing {@link Npc}.
	 * @param creature : Seen {@link Creature}.
	 */
	public final void notifyCreatureSee(Npc npc, Creature creature)
	{
		String res = null;
		try
		{
			res = onCreatureSee(npc, creature);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, creature, res);
	}
	
	/**
	 * Creature see quest event for {@link Npc} seeing a {@link Creature} within 400 range.
	 * @param npc : Seeing {@link Npc}.
	 * @param creature : Seen {@link Creature}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onCreatureSee(Npc npc, Creature creature)
	{
		return null;
	}
	
	/**
	 * Quest event listener for {@link Player} (his {@link Summon}) being killed by a {@link Creature}.
	 * @param killer : Killing {@link Creature}.
	 * @param player : Killed {@link Player}.
	 */
	public final void notifyDeath(Creature killer, Player player)
	{
		String res = null;
		try
		{
			res = onDeath(killer, player);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult((killer instanceof Npc) ? (Npc) killer : null, player, res);
	}
	
	/**
	 * Quest event for {@link Player} (his {@link Summon}) being killed by a {@link Creature}.
	 * @param killer : Killing {@link Creature}.
	 * @param player : Killed {@link Player}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onDeath(Creature killer, Player player)
	{
		return null;
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
	 * Quest event listener for {@link Player} entering the world.
	 * @param player : Entering {@link Player}.
	 */
	public final void notifyEnterWorld(Player player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(null, player, res);
	}
	
	/**
	 * Quest event for {@link Player} entering the world.
	 * @param player : Entering {@link Player}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onEnterWorld(Player player)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link ZoneType}, which will respond to {@link Creature} entering it.
	 * @param zoneIds : The ids of the {@link ZoneType}.
	 */
	public final void addEnterZoneId(int... zoneIds)
	{
		for (int zoneId : zoneIds)
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
				zone.addQuestEvent(ScriptEventType.ON_ENTER_ZONE, this);
		}
	}
	
	/**
	 * Quest event listener for {@link Creature} entering the {@link ZoneType}.
	 * @param character : Entering {@link Creature}.
	 * @param zone : Specified {@link ZoneType}.
	 */
	public final void notifyEnterZone(Creature character, ZoneType zone)
	{
		String res = null;
		try
		{
			res = onEnterZone(character, zone);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(null, character, res);
	}
	
	/**
	 * Quest event for {@link Creature} entering the {@link ZoneType}.
	 * @param character : Entering {@link Creature}.
	 * @param zone : Specified {@link ZoneType}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onEnterZone(Creature character, ZoneType zone)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link ZoneType}, which will respond to {@link Player} leaving it.
	 * @param zoneIds : The ids of the {@link ZoneType}.
	 */
	public final void addExitZoneId(int... zoneIds)
	{
		for (int zoneId : zoneIds)
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
				zone.addQuestEvent(ScriptEventType.ON_EXIT_ZONE, this);
		}
	}
	
	/**
	 * Quest event listener for {@link Creature} leaving the {@link ZoneType}.
	 * @param character : Leaving {@link Creature}.
	 * @param zone : Specified {@link ZoneType}.
	 */
	public final void notifyExitZone(Creature character, ZoneType zone)
	{
		String res = null;
		try
		{
			res = onExitZone(character, zone);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(null, character, res);
	}
	
	/**
	 * Quest event for {@link Creature} leaving the {@link ZoneType}.
	 * @param character : Leaving {@link Creature}.
	 * @param zone : Specified {@link ZoneType}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onExitZone(Creature character, ZoneType zone)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to faction call event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addFactionCallId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_FACTION_CALL);
	}
	
	/**
	 * Quest event listener for {@link Attackable} performing faction call event on another {@link Attackable}.
	 * @param caller : The {@link Attackable} calling for assistance.
	 * @param called : The {@link Attackable} called by {@link Attackable} caller to assist.
	 * @param target : The {@link Creature} target affected by caller/called.
	 */
	public final void notifyFactionCall(Attackable caller, Attackable called, Creature target)
	{
		String res = null;
		try
		{
			res = onFactionCall(caller, called, target);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(caller, target, res);
	}
	
	/**
	 * Quest event for {@link Attackable} performing faction call event on another {@link Attackable}.
	 * @param caller : The {@link Attackable} calling for assistance.
	 * @param called : The {@link Attackable} called by {@link Attackable} caller to assist.
	 * @param target : The {@link Creature} target affected by caller/called.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onFactionCall(Attackable caller, Attackable called, Creature target)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will override initial dialog with this {@link Quest}.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addFirstTalkId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_FIRST_TALK);
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
			showResult(npc, player, res);
		else
			player.sendPacket(ActionFailed.STATIC_PACKET);
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
	 * Register this {@link Quest} to the {@link Item}, which will respond to {@link Item} use.
	 * @param itemIds : The ids of the {@link Item}.
	 */
	public final void addItemUse(int... itemIds)
	{
		for (int itemId : itemIds)
		{
			Item t = ItemData.getInstance().getTemplate(itemId);
			if (t != null)
				t.addQuestEvent(this);
		}
	}
	
	/**
	 * Quest event listener for {@link Item} being used by {@link Player}.
	 * @param item : {@link Item} being used.
	 * @param player : {@link Player} using it.
	 * @param target : {@link Player}'s target.
	 */
	public final void notifyItemUse(ItemInstance item, Player player, WorldObject target)
	{
		String res = null;
		try
		{
			res = onItemUse(item, player, target);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(null, player, res);
	}
	
	/**
	 * Quest event for {@link Item} being used by {@link Player}.
	 * @param item : {@link Item} being used.
	 * @param player : {@link Player} using it.
	 * @param target : {@link Player}'s target.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onItemUse(ItemInstance item, Player player, WorldObject target)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to kill event.
	 * @param killIds : The ids of the {@link Npc}.
	 */
	public final void addKillId(int... killIds)
	{
		addEventIds(killIds, ScriptEventType.ON_KILL);
	}
	
	/**
	 * Quest event listener for {@link Npc} being killed by {@link Creature}.
	 * @param npc : Killed {@link Npc}.
	 * @param killer : Killer {@link Creature}.
	 */
	public final void notifyKill(Npc npc, Creature killer)
	{
		String res = null;
		try
		{
			res = onKill(npc, killer);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, killer, res);
	}
	
	/**
	 * Quest event for {@link Npc} being killed by {@link Creature}.
	 * @param npc : Killed {@link Npc}.
	 * @param killer : Killer {@link Creature}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onKill(Npc npc, Creature killer)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its spawn event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addSpawnId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_SPAWN);
	}
	
	/**
	 * Quest event listener for {@link Npc} being spawned into the world.
	 * @param npc : Spawned {@link Npc}.
	 */
	public final void notifySpawn(Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (Exception e)
		{
			LOGGER.error(toString(), e);
		}
	}
	
	/**
	 * Quest event for {@link Npc} being spawned into the world.
	 * @param npc : Spawned {@link Npc}.
	 * @return No purpose.
	 */
	public String onSpawn(Npc npc)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to its decay event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addDecayId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_DECAY);
	}
	
	/**
	 * Quest event listener for {@link Npc} being decayed from the world.
	 * @param npc : Decayed {@link Npc}.
	 */
	public final void notifyDecay(Npc npc)
	{
		try
		{
			onDecay(npc);
		}
		catch (Exception e)
		{
			LOGGER.error(toString(), e);
		}
	}
	
	/**
	 * Quest event for {@link Npc} being decayed from the world.
	 * @param npc : Decayed {@link Npc}.
	 * @return No purpose.
	 */
	public String onDecay(Npc npc)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to seeing other skill casted event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addSkillSeeId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_SKILL_SEE);
	}
	
	/**
	 * Quest event listener for {@link Npc} seeing a skill casted by {@link Player} (his {@link Summon}).
	 * @param npc : Noticing {@link Npc}.
	 * @param caster : {@link Player} casting the {@link Skill}.
	 * @param skill : Casted {@link Skill}.
	 * @param targets : Affected targets.
	 * @param isPet : Marks {@link Player}'s {@link Summon} is casting.
	 */
	public final void notifySkillSee(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		ThreadPool.execute(() ->
		{
			String res = null;
			try
			{
				res = onSkillSee(npc, caster, skill, targets, isPet);
			}
			catch (Exception e)
			{
				LOGGER.warn(toString(), e);
				return;
			}
			showResult(npc, caster, res);
		});
	}
	
	/**
	 * Quest event for {@link Npc} seeing a skill casted by {@link Player} (his {@link Summon}).
	 * @param npc : Noticing {@link Npc}.
	 * @param caster : {@link Player} casting the {@link Skill}.
	 * @param skill : Casted {@link Skill}.
	 * @param targets : Affected targets.
	 * @param isPet : Marks {@link Player}'s {@link Summon} is casting.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onSkillSee(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to itself casting skill event.
	 * @param npcIds : The ids of the {@link Npc}.
	 */
	public final void addSpellFinishedId(int... npcIds)
	{
		addEventIds(npcIds, ScriptEventType.ON_SPELL_FINISHED);
	}
	
	/**
	 * Quest event listener for {@link Npc} casting a skill on {@link Player} (his {@link Summon}).
	 * @param npc : Casting {@link Npc}.
	 * @param player : Target {@link Player} (his {@link Summon}).
	 * @param skill : Casted {@link Skill}.
	 */
	public final void notifySpellFinished(Npc npc, Player player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(npc, player, skill);
		}
		catch (Exception e)
		{
			LOGGER.warn(toString(), e);
			return;
		}
		showResult(npc, player, res);
	}
	
	/**
	 * Quest event for {@link Npc} casting a skill on {@link Player} (his {@link Summon}).
	 * @param npc : Casting {@link Npc}.
	 * @param player : Target {@link Player} (his {@link Summon}).
	 * @param skill : Casted {@link Skill}.
	 * @return Either text message, html message or html file. Null when not defined.
	 */
	public String onSpellFinished(Npc npc, Player player, L2Skill skill)
	{
		return null;
	}
	
	/**
	 * Register this {@link Quest} to the {@link Npc}, which will respond to {@link Player} talk event.
	 * @param talkIds : The ids of the {@link Npc}.
	 */
	public final void addTalkId(int... talkIds)
	{
		addEventIds(talkIds, ScriptEventType.ON_TALK);
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
	 * Register this {@link Quest} to the {@link Castle}, which will respond to siege status change event.
	 * @param castleId : The id of the {@link Castle}.
	 */
	public final void addSiegeNotify(int castleId)
	{
		// Castle always have a Siege, just register it.
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		if (castle != null)
			castle.getSiege().addQuestEvent(this);
	}
	
	/**
	 * Quest event for {@link Castle} having a {@link Siege} status change event.
	 * @param siege : Notified {@link Siege}.
	 */
	public void onSiegeEvent(Siege siege)
	{
	}
	
	/**
	 * Register this {@link Quest} to be notified by time change (each in-game minute).
	 */
	public final void addGameTimeNotify()
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
}