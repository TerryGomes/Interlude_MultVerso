package net.sf.l2j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.olympiad.enums.OlympiadPeriod;

/**
 * This class contains global server configuration.<br>
 * It has static final fields initialized from configuration files.
 */
public final class Config
{
	private static final CLogger LOGGER = new CLogger(Config.class.getName());
	// --------------------------------------------------
	// Those "hidden" settings haven't configs to avoid admins to fuck their server
	// You still can experiment changing values here. But don't say I didn't warn you.
	// --------------------------------------------------
	/** Reserve Host on LoginServerThread */
	public static boolean RESERVE_HOST_ON_LOGIN = false; // default false
	
	/** MMO settings */
	public static int SELECTOR_SLEEP_TIME = 20; // default 20
	public static int MAX_SEND_PER_PASS = 80; // default 80
	public static int MAX_READ_PER_PASS = 80; // default 80
	public static int HELPER_BUFFER_COUNT = 20; // default 20
	
	/**
	 * Initialize {@link ExProperties} from specified configuration file.
	 * @param filename : File name to be loaded.
	 * @return ExProperties : Initialized {@link ExProperties}.
	 */
	public static final ExProperties initProperties(String filename)
	{
		final ExProperties result = new ExProperties();
		
		try
		{
			result.load(new File(filename));
		}
		catch (Exception e)
		{
			LOGGER.error("An error occured loading '{}' config.", e, filename);
		}
		
		return result;
	}
	
	/**
	 * Loads offline shop settings
	 */
	
	// --------------------------------------------------
	// Offline
	// --------------------------------------------------
	
	public static boolean OFFLINE_TRADE_ENABLE;
	public static boolean OFFLINE_CRAFT_ENABLE;
	public static boolean OFFLINE_MODE_IN_PEACE_ZONE;
	public static boolean OFFLINE_MODE_NO_DAMAGE;
	public static boolean RESTORE_OFFLINERS;
	public static int OFFLINE_MAX_DAYS;
	public static boolean OFFLINE_DISCONNECT_FINISHED;
	public static boolean OFFLINE_SLEEP_EFFECT;
	public static boolean RESTORE_STORE_ITEMS;
	public static final String OFFLINE_FILE = "config/offlineshop.properties";
	
	private static final void loadOfflineShop()
	{
		final ExProperties offline = initProperties(Config.OFFLINE_FILE);
		Config.OFFLINE_TRADE_ENABLE = offline.getProperty("OfflineTradeEnable", false);
		Config.OFFLINE_CRAFT_ENABLE = offline.getProperty("OfflineCraftEnable", false);
		Config.OFFLINE_MODE_IN_PEACE_ZONE = offline.getProperty("OfflineModeInPeaceZone", false);
		Config.OFFLINE_MODE_NO_DAMAGE = offline.getProperty("OfflineModeNoDamage", false);
		Config.RESTORE_OFFLINERS = offline.getProperty("RestoreOffliners", false);
		Config.OFFLINE_MAX_DAYS = offline.getProperty("OfflineMaxDays", 10);
		Config.OFFLINE_DISCONNECT_FINISHED = offline.getProperty("OfflineDisconnectFinished", true);
		Config.OFFLINE_SLEEP_EFFECT = offline.getProperty("OfflineSleepEffect", true);
		Config.RESTORE_STORE_ITEMS = offline.getProperty("RestoreStoreItems", false);
	}
	
	/**
	 * Loads clan and clan hall settings.
	 */
	
	// --------------------------------------------------
	// Clans settings
	// --------------------------------------------------
	
	/** Clans */
	public static int CLAN_JOIN_DAYS;
	public static int CLAN_CREATE_DAYS;
	public static int CLAN_DISSOLVE_DAYS;
	public static int ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int MAX_NUM_OF_CLANS_IN_ALLY;
	public static int CLAN_MEMBERS_FOR_WAR;
	public static int CLAN_WAR_PENALTY_WHEN_ENDED;
	public static boolean MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	
	/** Manor */
	public static int MANOR_REFRESH_TIME;
	public static int MANOR_REFRESH_MIN;
	public static int MANOR_APPROVE_TIME;
	public static int MANOR_APPROVE_MIN;
	public static int MANOR_MAINTENANCE_MIN;
	public static int MANOR_SAVE_PERIOD_RATE;
	
	/** Clan Hall function */
	public static long CH_TELE_FEE_RATIO;
	public static int CH_TELE1_FEE;
	public static int CH_TELE2_FEE;
	public static long CH_SUPPORT_FEE_RATIO;
	public static int CH_SUPPORT1_FEE;
	public static int CH_SUPPORT2_FEE;
	public static int CH_SUPPORT3_FEE;
	public static int CH_SUPPORT4_FEE;
	public static int CH_SUPPORT5_FEE;
	public static int CH_SUPPORT6_FEE;
	public static int CH_SUPPORT7_FEE;
	public static int CH_SUPPORT8_FEE;
	public static long CH_MPREG_FEE_RATIO;
	public static int CH_MPREG1_FEE;
	public static int CH_MPREG2_FEE;
	public static int CH_MPREG3_FEE;
	public static int CH_MPREG4_FEE;
	public static int CH_MPREG5_FEE;
	public static long CH_HPREG_FEE_RATIO;
	public static int CH_HPREG1_FEE;
	public static int CH_HPREG2_FEE;
	public static int CH_HPREG3_FEE;
	public static int CH_HPREG4_FEE;
	public static int CH_HPREG5_FEE;
	public static int CH_HPREG6_FEE;
	public static int CH_HPREG7_FEE;
	public static int CH_HPREG8_FEE;
	public static int CH_HPREG9_FEE;
	public static int CH_HPREG10_FEE;
	public static int CH_HPREG11_FEE;
	public static int CH_HPREG12_FEE;
	public static int CH_HPREG13_FEE;
	public static long CH_EXPREG_FEE_RATIO;
	public static int CH_EXPREG1_FEE;
	public static int CH_EXPREG2_FEE;
	public static int CH_EXPREG3_FEE;
	public static int CH_EXPREG4_FEE;
	public static int CH_EXPREG5_FEE;
	public static int CH_EXPREG6_FEE;
	public static int CH_EXPREG7_FEE;
	public static long CH_ITEM_FEE_RATIO;
	public static int CH_ITEM1_FEE;
	public static int CH_ITEM2_FEE;
	public static int CH_ITEM3_FEE;
	public static long CH_CURTAIN_FEE_RATIO;
	public static int CH_CURTAIN1_FEE;
	public static int CH_CURTAIN2_FEE;
	public static long CH_FRONT_FEE_RATIO;
	public static int CH_FRONT1_FEE;
	public static int CH_FRONT2_FEE;
	
	// --------------------------------------------------
	// Castle Settings
	// --------------------------------------------------
	public static long CS_TELE_FEE_RATIO;
	public static int CS_TELE1_FEE;
	public static int CS_TELE2_FEE;
	public static long CS_MPREG_FEE_RATIO;
	public static int CS_MPREG1_FEE;
	public static int CS_MPREG2_FEE;
	public static int CS_MPREG3_FEE;
	public static int CS_MPREG4_FEE;
	public static long CS_HPREG_FEE_RATIO;
	public static int CS_HPREG1_FEE;
	public static int CS_HPREG2_FEE;
	public static int CS_HPREG3_FEE;
	public static int CS_HPREG4_FEE;
	public static int CS_HPREG5_FEE;
	public static long CS_EXPREG_FEE_RATIO;
	public static int CS_EXPREG1_FEE;
	public static int CS_EXPREG2_FEE;
	public static int CS_EXPREG3_FEE;
	public static int CS_EXPREG4_FEE;
	public static long CS_SUPPORT_FEE_RATIO;
	public static int CS_SUPPORT1_FEE;
	public static int CS_SUPPORT2_FEE;
	public static int CS_SUPPORT3_FEE;
	public static int CS_SUPPORT4_FEE;
	
	public static final String CLANS_FILE = "config/clans.properties";
	
	private static final void loadClans()
	{
		final ExProperties clans = initProperties(Config.CLANS_FILE);
		
		Config.CLAN_JOIN_DAYS = clans.getProperty("DaysBeforeJoinAClan", 5);
		Config.CLAN_CREATE_DAYS = clans.getProperty("DaysBeforeCreateAClan", 10);
		Config.MAX_NUM_OF_CLANS_IN_ALLY = clans.getProperty("MaxNumOfClansInAlly", 3);
		Config.CLAN_MEMBERS_FOR_WAR = clans.getProperty("ClanMembersForWar", 15);
		Config.CLAN_WAR_PENALTY_WHEN_ENDED = clans.getProperty("ClanWarPenaltyWhenEnded", 5);
		Config.CLAN_DISSOLVE_DAYS = clans.getProperty("DaysToPassToDissolveAClan", 7);
		Config.ALLY_JOIN_DAYS_WHEN_LEAVED = clans.getProperty("DaysBeforeJoinAllyWhenLeaved", 1);
		Config.ALLY_JOIN_DAYS_WHEN_DISMISSED = clans.getProperty("DaysBeforeJoinAllyWhenDismissed", 1);
		Config.ACCEPT_CLAN_DAYS_WHEN_DISMISSED = clans.getProperty("DaysBeforeAcceptNewClanWhenDismissed", 1);
		Config.CREATE_ALLY_DAYS_WHEN_DISSOLVED = clans.getProperty("DaysBeforeCreateNewAllyWhenDissolved", 10);
		Config.MEMBERS_CAN_WITHDRAW_FROM_CLANWH = clans.getProperty("MembersCanWithdrawFromClanWH", false);
		
		Config.MANOR_REFRESH_TIME = clans.getProperty("ManorRefreshTime", 20);
		Config.MANOR_REFRESH_MIN = clans.getProperty("ManorRefreshMin", 0);
		Config.MANOR_APPROVE_TIME = clans.getProperty("ManorApproveTime", 6);
		Config.MANOR_APPROVE_MIN = clans.getProperty("ManorApproveMin", 0);
		Config.MANOR_MAINTENANCE_MIN = clans.getProperty("ManorMaintenanceMin", 6);
		Config.MANOR_SAVE_PERIOD_RATE = clans.getProperty("ManorSavePeriodRate", 2) * 3600000;
		
		Config.CH_TELE_FEE_RATIO = clans.getProperty("ClanHallTeleportFunctionFeeRatio", 86400000L);
		Config.CH_TELE1_FEE = clans.getProperty("ClanHallTeleportFunctionFeeLvl1", 7000);
		Config.CH_TELE2_FEE = clans.getProperty("ClanHallTeleportFunctionFeeLvl2", 14000);
		Config.CH_SUPPORT_FEE_RATIO = clans.getProperty("ClanHallSupportFunctionFeeRatio", 86400000L);
		Config.CH_SUPPORT1_FEE = clans.getProperty("ClanHallSupportFeeLvl1", 17500);
		Config.CH_SUPPORT2_FEE = clans.getProperty("ClanHallSupportFeeLvl2", 35000);
		Config.CH_SUPPORT3_FEE = clans.getProperty("ClanHallSupportFeeLvl3", 49000);
		Config.CH_SUPPORT4_FEE = clans.getProperty("ClanHallSupportFeeLvl4", 77000);
		Config.CH_SUPPORT5_FEE = clans.getProperty("ClanHallSupportFeeLvl5", 147000);
		Config.CH_SUPPORT6_FEE = clans.getProperty("ClanHallSupportFeeLvl6", 252000);
		Config.CH_SUPPORT7_FEE = clans.getProperty("ClanHallSupportFeeLvl7", 259000);
		Config.CH_SUPPORT8_FEE = clans.getProperty("ClanHallSupportFeeLvl8", 364000);
		Config.CH_MPREG_FEE_RATIO = clans.getProperty("ClanHallMpRegenerationFunctionFeeRatio", 86400000L);
		Config.CH_MPREG1_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl1", 14000);
		Config.CH_MPREG2_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl2", 26250);
		Config.CH_MPREG3_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl3", 45500);
		Config.CH_MPREG4_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl4", 96250);
		Config.CH_MPREG5_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl5", 140000);
		Config.CH_HPREG_FEE_RATIO = clans.getProperty("ClanHallHpRegenerationFunctionFeeRatio", 86400000L);
		Config.CH_HPREG1_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl1", 4900);
		Config.CH_HPREG2_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl2", 5600);
		Config.CH_HPREG3_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl3", 7000);
		Config.CH_HPREG4_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl4", 8166);
		Config.CH_HPREG5_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl5", 10500);
		Config.CH_HPREG6_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl6", 12250);
		Config.CH_HPREG7_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl7", 14000);
		Config.CH_HPREG8_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl8", 15750);
		Config.CH_HPREG9_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl9", 17500);
		Config.CH_HPREG10_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl10", 22750);
		Config.CH_HPREG11_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl11", 26250);
		Config.CH_HPREG12_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl12", 29750);
		Config.CH_HPREG13_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl13", 36166);
		Config.CH_EXPREG_FEE_RATIO = clans.getProperty("ClanHallExpRegenerationFunctionFeeRatio", 86400000L);
		Config.CH_EXPREG1_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl1", 21000);
		Config.CH_EXPREG2_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl2", 42000);
		Config.CH_EXPREG3_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl3", 63000);
		Config.CH_EXPREG4_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl4", 105000);
		Config.CH_EXPREG5_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl5", 147000);
		Config.CH_EXPREG6_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl6", 163331);
		Config.CH_EXPREG7_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl7", 210000);
		Config.CH_ITEM_FEE_RATIO = clans.getProperty("ClanHallItemCreationFunctionFeeRatio", 86400000L);
		Config.CH_ITEM1_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl1", 210000);
		Config.CH_ITEM2_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl2", 490000);
		Config.CH_ITEM3_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl3", 980000);
		Config.CH_CURTAIN_FEE_RATIO = clans.getProperty("ClanHallCurtainFunctionFeeRatio", 86400000L);
		Config.CH_CURTAIN1_FEE = clans.getProperty("ClanHallCurtainFunctionFeeLvl1", 2002);
		Config.CH_CURTAIN2_FEE = clans.getProperty("ClanHallCurtainFunctionFeeLvl2", 2625);
		Config.CH_FRONT_FEE_RATIO = clans.getProperty("ClanHallFrontPlatformFunctionFeeRatio", 86400000L);
		Config.CH_FRONT1_FEE = clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", 3031);
		Config.CH_FRONT2_FEE = clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", 9331);
		
		Config.CS_TELE_FEE_RATIO = clans.getProperty("CastleTeleportFunctionFeeRatio", 604800000L);
		Config.CS_TELE1_FEE = clans.getProperty("CastleTeleportFunctionFeeLvl1", 7000);
		Config.CS_TELE2_FEE = clans.getProperty("CastleTeleportFunctionFeeLvl2", 14000);
		Config.CS_SUPPORT_FEE_RATIO = clans.getProperty("CastleSupportFunctionFeeRatio", 86400000L);
		Config.CS_SUPPORT1_FEE = clans.getProperty("CastleSupportFeeLvl1", 7000);
		Config.CS_SUPPORT2_FEE = clans.getProperty("CastleSupportFeeLvl2", 21000);
		Config.CS_SUPPORT3_FEE = clans.getProperty("CastleSupportFeeLvl3", 37000);
		Config.CS_SUPPORT4_FEE = clans.getProperty("CastleSupportFeeLvl4", 52000);
		Config.CS_MPREG_FEE_RATIO = clans.getProperty("CastleMpRegenerationFunctionFeeRatio", 86400000L);
		Config.CS_MPREG1_FEE = clans.getProperty("CastleMpRegenerationFeeLvl1", 2000);
		Config.CS_MPREG2_FEE = clans.getProperty("CastleMpRegenerationFeeLvl2", 6500);
		Config.CS_MPREG3_FEE = clans.getProperty("CastleMpRegenerationFeeLvl3", 13750);
		Config.CS_MPREG4_FEE = clans.getProperty("CastleMpRegenerationFeeLvl4", 20000);
		Config.CS_HPREG_FEE_RATIO = clans.getProperty("CastleHpRegenerationFunctionFeeRatio", 86400000L);
		Config.CS_HPREG1_FEE = clans.getProperty("CastleHpRegenerationFeeLvl1", 1000);
		Config.CS_HPREG2_FEE = clans.getProperty("CastleHpRegenerationFeeLvl2", 1500);
		Config.CS_HPREG3_FEE = clans.getProperty("CastleHpRegenerationFeeLvl3", 2250);
		Config.CS_HPREG4_FEE = clans.getProperty("CastleHpRegenerationFeeLvl4", 3270);
		Config.CS_HPREG5_FEE = clans.getProperty("CastleHpRegenerationFeeLvl5", 5166);
		Config.CS_EXPREG_FEE_RATIO = clans.getProperty("CastleExpRegenerationFunctionFeeRatio", 86400000L);
		Config.CS_EXPREG1_FEE = clans.getProperty("CastleExpRegenerationFeeLvl1", 9000);
		Config.CS_EXPREG2_FEE = clans.getProperty("CastleExpRegenerationFeeLvl2", 15000);
		Config.CS_EXPREG3_FEE = clans.getProperty("CastleExpRegenerationFeeLvl3", 21000);
		Config.CS_EXPREG4_FEE = clans.getProperty("CastleExpRegenerationFeeLvl4", 30000);
	}
	
	/**
	 * Loads event settings.<br>
	 * Such as olympiad, seven signs festival, four sepulchures, dimensional rift, weddings, lottery, fishing championship.
	 */
	
	// --------------------------------------------------
	// Events settings
	// --------------------------------------------------
	
	/** SevenSigns Festival */
	public static boolean SEVEN_SIGNS_BYPASS_PREREQUISITES;
	public static int FESTIVAL_MIN_PLAYER;
	public static int MAXIMUM_PLAYER_CONTRIB;
	public static long FESTIVAL_MANAGER_START;
	public static long FESTIVAL_LENGTH;
	public static long FESTIVAL_CYCLE_LENGTH;
	public static long FESTIVAL_FIRST_SPAWN;
	public static long FESTIVAL_FIRST_SWARM;
	public static long FESTIVAL_SECOND_SPAWN;
	public static long FESTIVAL_SECOND_SWARM;
	public static long FESTIVAL_CHEST_SPAWN;
	
	/** Four Sepulchers */
	public static int FS_TIME_ENTRY;
	public static int FS_TIME_END;
	public static int FS_PARTY_MEMBER_COUNT;
	
	/** dimensional rift */
	public static int RIFT_MIN_PARTY_SIZE;
	public static int RIFT_SPAWN_DELAY;
	public static int RIFT_MAX_JUMPS;
	public static int RIFT_AUTO_JUMPS_TIME_MIN;
	public static int RIFT_AUTO_JUMPS_TIME_MAX;
	public static int RIFT_ENTER_COST_RECRUIT;
	public static int RIFT_ENTER_COST_SOLDIER;
	public static int RIFT_ENTER_COST_OFFICER;
	public static int RIFT_ENTER_COST_CAPTAIN;
	public static int RIFT_ENTER_COST_COMMANDER;
	public static int RIFT_ENTER_COST_HERO;
	public static double RIFT_BOSS_ROOM_TIME_MULTIPLY;
	
	/** Lottery */
	public static int LOTTERY_PRIZE;
	public static int LOTTERY_TICKET_PRICE;
	public static double LOTTERY_5_NUMBER_RATE;
	public static double LOTTERY_4_NUMBER_RATE;
	public static double LOTTERY_3_NUMBER_RATE;
	public static int LOTTERY_2_AND_1_NUMBER_PRIZE;
	
	/** Fishing tournament */
	public static boolean ALLOW_FISH_CHAMPIONSHIP;
	public static int FISH_CHAMPIONSHIP_REWARD_ITEM;
	public static int FISH_CHAMPIONSHIP_REWARD_1;
	public static int FISH_CHAMPIONSHIP_REWARD_2;
	public static int FISH_CHAMPIONSHIP_REWARD_3;
	public static int FISH_CHAMPIONSHIP_REWARD_4;
	public static int FISH_CHAMPIONSHIP_REWARD_5;
	
	public static int EVENT_MEDAL_COUNT;
	public static int EVENT_MEDAL_CHANCE;
	
	public static int GLITTERING_MEDAL_COUNT;
	public static int GLITTERING_MEDAL_CHANCE;
	
	public static int STAR_CHANCE;
	public static int BEAD_CHANCE;
	public static int FIR_CHANCE;
	public static int FLOWER_CHANCE;
	
	public static int STAR_COUNT;
	public static int BEAD_COUNT;
	public static int FIR_COUNT;
	public static int FLOWER_COUNT;
	
	public static boolean EVENT_COMMANDS;
	
	public static final String EVENTS_FILE = "config/events.properties";
	
	private static final void loadEvents()
	{
		final ExProperties events = initProperties(Config.EVENTS_FILE);
		
		Config.SEVEN_SIGNS_BYPASS_PREREQUISITES = events.getProperty("SevenSignsBypassPrerequisites", false);
		Config.FESTIVAL_MIN_PLAYER = MathUtil.limit(events.getProperty("FestivalMinPlayer", 5), 2, 9);
		Config.MAXIMUM_PLAYER_CONTRIB = events.getProperty("MaxPlayerContrib", 1000000);
		Config.FESTIVAL_MANAGER_START = events.getProperty("FestivalManagerStart", 120000L);
		Config.FESTIVAL_LENGTH = events.getProperty("FestivalLength", 1080000L);
		Config.FESTIVAL_CYCLE_LENGTH = events.getProperty("FestivalCycleLength", 2280000L);
		Config.FESTIVAL_FIRST_SPAWN = events.getProperty("FestivalFirstSpawn", 120000L);
		Config.FESTIVAL_FIRST_SWARM = events.getProperty("FestivalFirstSwarm", 300000L);
		Config.FESTIVAL_SECOND_SPAWN = events.getProperty("FestivalSecondSpawn", 540000L);
		Config.FESTIVAL_SECOND_SWARM = events.getProperty("FestivalSecondSwarm", 720000L);
		Config.FESTIVAL_CHEST_SPAWN = events.getProperty("FestivalChestSpawn", 900000L);
		
		Config.FS_TIME_ENTRY = events.getProperty("EntryTime", 55);
		Config.FS_TIME_END = events.getProperty("EndTime", 50);
		Config.FS_PARTY_MEMBER_COUNT = MathUtil.limit(events.getProperty("NeededPartyMembers", 4), 2, 9);
		
		Config.RIFT_MIN_PARTY_SIZE = events.getProperty("RiftMinPartySize", 2);
		Config.RIFT_MAX_JUMPS = events.getProperty("MaxRiftJumps", 4);
		Config.RIFT_SPAWN_DELAY = events.getProperty("RiftSpawnDelay", 10000);
		Config.RIFT_AUTO_JUMPS_TIME_MIN = events.getProperty("AutoJumpsDelayMin", 480);
		Config.RIFT_AUTO_JUMPS_TIME_MAX = events.getProperty("AutoJumpsDelayMax", 600);
		Config.RIFT_ENTER_COST_RECRUIT = events.getProperty("RecruitCost", 18);
		Config.RIFT_ENTER_COST_SOLDIER = events.getProperty("SoldierCost", 21);
		Config.RIFT_ENTER_COST_OFFICER = events.getProperty("OfficerCost", 24);
		Config.RIFT_ENTER_COST_CAPTAIN = events.getProperty("CaptainCost", 27);
		Config.RIFT_ENTER_COST_COMMANDER = events.getProperty("CommanderCost", 30);
		Config.RIFT_ENTER_COST_HERO = events.getProperty("HeroCost", 33);
		Config.RIFT_BOSS_ROOM_TIME_MULTIPLY = events.getProperty("BossRoomTimeMultiply", 1.);
		
		Config.LOTTERY_PRIZE = events.getProperty("LotteryPrize", 50000);
		Config.LOTTERY_TICKET_PRICE = events.getProperty("LotteryTicketPrice", 2000);
		Config.LOTTERY_5_NUMBER_RATE = events.getProperty("Lottery5NumberRate", 0.6);
		Config.LOTTERY_4_NUMBER_RATE = events.getProperty("Lottery4NumberRate", 0.2);
		Config.LOTTERY_3_NUMBER_RATE = events.getProperty("Lottery3NumberRate", 0.2);
		Config.LOTTERY_2_AND_1_NUMBER_PRIZE = events.getProperty("Lottery2and1NumberPrize", 200);
		
		Config.ALLOW_FISH_CHAMPIONSHIP = events.getProperty("AllowFishChampionship", true);
		Config.FISH_CHAMPIONSHIP_REWARD_ITEM = events.getProperty("FishChampionshipRewardItemId", 57);
		Config.FISH_CHAMPIONSHIP_REWARD_1 = events.getProperty("FishChampionshipReward1", 800000);
		Config.FISH_CHAMPIONSHIP_REWARD_2 = events.getProperty("FishChampionshipReward2", 500000);
		Config.FISH_CHAMPIONSHIP_REWARD_3 = events.getProperty("FishChampionshipReward3", 300000);
		Config.FISH_CHAMPIONSHIP_REWARD_4 = events.getProperty("FishChampionshipReward4", 200000);
		Config.FISH_CHAMPIONSHIP_REWARD_5 = events.getProperty("FishChampionshipReward5", 100000);
		
		Config.EVENT_MEDAL_COUNT = events.getProperty("EventMedalCount", 1);
		Config.EVENT_MEDAL_CHANCE = events.getProperty("EventMedalChance", 40);
		
		Config.GLITTERING_MEDAL_COUNT = events.getProperty("GlitteringMedalCount", 1);
		Config.GLITTERING_MEDAL_CHANCE = events.getProperty("GlitteringMedalChance", 2);
		
		Config.STAR_CHANCE = events.getProperty("StarChance", 20);
		Config.BEAD_CHANCE = events.getProperty("BeadChance", 20);
		Config.FIR_CHANCE = events.getProperty("FirChance", 50);
		Config.FLOWER_CHANCE = events.getProperty("FlowerChance", 5);
		
		Config.STAR_COUNT = events.getProperty("StarCount", 1);
		Config.BEAD_COUNT = events.getProperty("BeadCount", 1);
		Config.FIR_COUNT = events.getProperty("FirCount", 1);
		Config.FLOWER_COUNT = events.getProperty("FlowerCount", 1);
		
		Config.EVENT_COMMANDS = events.getProperty("AllowEventCommands", false);
		
	}
	
	/**
	 * Loads geoengine settings.
	 */
	
	// --------------------------------------------------
	// GeoEngine
	// --------------------------------------------------
	
	/** Geodata */
	public static String GEODATA_PATH;
	// public static String GEODATA_TYPE;
	
	/** Path checking */
	public static int PART_OF_CHARACTER_HEIGHT;
	public static int MAX_OBSTACLE_HEIGHT;
	
	/** Path finding */
	public static String PATHFIND_BUFFERS;
	public static int MOVE_WEIGHT;
	public static int MOVE_WEIGHT_DIAG;
	public static int OBSTACLE_WEIGHT;
	public static int OBSTACLE_WEIGHT_DIAG;
	public static int HEURISTIC_WEIGHT;
	public static int HEURISTIC_WEIGHT_DIAG;
	public static int MAX_ITERATIONS;
	public static boolean ENABLE_GEODATA;
	
	public static final String GEOENGINE_FILE = "config/geoengine.properties";
	
	private static final void loadGeoengine()
	{
		final ExProperties geoengine = initProperties(Config.GEOENGINE_FILE);
		
		ENABLE_GEODATA = geoengine.getProperty("EnableGeoData", false);
		
		Config.GEODATA_PATH = geoengine.getProperty("GeoDataPath", "./data/geodata/");
		// Config.GEODATA_TYPE = geoengine.getProperty("GeoDataType", "L2OFF");
		
		Config.PART_OF_CHARACTER_HEIGHT = geoengine.getProperty("PartOfCharacterHeight", 75);
		Config.MAX_OBSTACLE_HEIGHT = geoengine.getProperty("MaxObstacleHeight", 32);
		
		Config.PATHFIND_BUFFERS = geoengine.getProperty("PathFindBuffers", "500x10;1000x10;3000x5;5000x3;10000x3");
		Config.MOVE_WEIGHT = geoengine.getProperty("MoveWeight", 10);
		Config.MOVE_WEIGHT_DIAG = geoengine.getProperty("MoveWeightDiag", 14);
		Config.OBSTACLE_WEIGHT = geoengine.getProperty("ObstacleWeight", 30);
		Config.OBSTACLE_WEIGHT_DIAG = (int) (OBSTACLE_WEIGHT * Math.sqrt(2));
		Config.HEURISTIC_WEIGHT = geoengine.getProperty("HeuristicWeight", 12);
		Config.HEURISTIC_WEIGHT_DIAG = geoengine.getProperty("HeuristicWeightDiag", 18);
		Config.MAX_ITERATIONS = geoengine.getProperty("MaxIterations", 3500);
	}
	
	/**
	 * Loads hex ID settings.
	 */
	
	// --------------------------------------------------
	// HexID
	// --------------------------------------------------
	
	public static int SERVER_ID;
	public static byte[] HEX_ID;
	public static final String HEXID_FILE = "config/hexid.txt";
	
	private static final void loadHexID()
	{
		final ExProperties hexid = initProperties(Config.HEXID_FILE);
		
		Config.SERVER_ID = Integer.parseInt(hexid.getProperty("ServerID"));
		Config.HEX_ID = new BigInteger(hexid.getProperty("HexID"), 16).toByteArray();
	}
	
	/**
	 * Saves hex ID file.
	 * @param serverId : The ID of server.
	 * @param hexId : The hex ID of server.
	 */
	public static final void saveHexid(int serverId, String hexId)
	{
		saveHexid(serverId, hexId, HEXID_FILE);
	}
	
	/**
	 * Saves hexID file.
	 * @param serverId : The ID of server.
	 * @param hexId : The hexID of server.
	 * @param filename : The file name.
	 */
	public static final void saveHexid(int serverId, String hexId, String filename)
	{
		try
		{
			final File file = new File(filename);
			file.createNewFile();
			
			final Properties hexSetting = new Properties();
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			
			try (OutputStream out = new FileOutputStream(file))
			{
				hexSetting.store(out, "the hexID to auth into login");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to save hex ID to '{}' file.", e, filename);
		}
	}
	
	/**
	 * Loads NPC settings.<br>
	 * Such as champion monsters, NPC buffer, class master, wyvern, raid bosses and grand bosses, AI.
	 */
	
	// --------------------------------------------------
	// NPCs / Monsters
	// --------------------------------------------------
	
	/** Spawn */
	public static double SPAWN_MULTIPLIER;
	public static String[] SPAWN_EVENTS;
	
	/** Champion Mod */
	public static int CHAMPION_FREQUENCY;
	public static boolean CHAMPION_DEEPBLUE_DROP_RULES;
	public static int CHAMP_MIN_LVL;
	public static int CHAMP_MAX_LVL;
	public static int CHAMPION_HP;
	public static double CHAMPION_HP_REGEN;
	public static double CHAMPION_RATE_XP;
	public static double CHAMPION_RATE_SP;
	public static double PREMIUM_CHAMPION_RATE_XP;
	public static double PREMIUM_CHAMPION_RATE_SP;
	public static int CHAMPION_REWARDS;
	public static int PREMIUM_CHAMPION_REWARDS;
	public static int CHAMPION_ADENAS_REWARDS;
	public static int PREMIUM_CHAMPION_ADENAS_REWARDS;
	public static double CHAMPION_ATK;
	public static double CHAMPION_SPD_ATK;
	public static int CHAMPION_REWARD;
	public static int CHAMPION_REWARD_ID;
	public static int CHAMPION_REWARD_QTY;
	public static int CHAMPION_AURA;
	
	/** Class Master */
	public static boolean ALLOW_ENTIRE_TREE;
	public static ClassMasterSettings CLASS_MASTER_SETTINGS;
	
	/** Wedding Manager */
	public static int WEDDING_PRICE;
	public static boolean WEDDING_SAMESEX;
	public static boolean WEDDING_FORMALWEAR;
	
	/** Scheme Buffer */
	public static int BUFFER_MAX_SCHEMES;
	public static int BUFFER_STATIC_BUFF_COST;
	
	/** Wyvern Manager */
	public static int WYVERN_REQUIRED_LEVEL;
	public static int WYVERN_REQUIRED_CRYSTALS;
	
	/** Misc */
	public static boolean FREE_TELEPORT;
	public static int LVL_FREE_TELEPORT;
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	public static boolean MOB_AGGRO_IN_PEACEZONE;
	public static boolean SHOW_NPC_LVL;
	public static boolean SHOW_NPC_CREST;
	public static boolean SHOW_SUMMON_CREST;
	
	/** Raid Boss */
	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_DEFENCE_MULTIPLIER;
	public static int RAID_MINION_RESPAWN_TIMER;
	
	public static boolean RAID_DISABLE_CURSE;
	
	/** Grand Boss */
	public static int SPAWN_INTERVAL_ANTHARAS;
	public static int RANDOM_SPAWN_TIME_ANTHARAS;
	public static int WAIT_TIME_ANTHARAS;
	
	public static int SPAWN_INTERVAL_BAIUM;
	public static int RANDOM_SPAWN_TIME_BAIUM;
	
	public static int SPAWN_INTERVAL_FRINTEZZA;
	public static int RANDOM_SPAWN_TIME_FRINTEZZA;
	public static int WAIT_TIME_FRINTEZZA;
	
	public static int SPAWN_INTERVAL_SAILREN;
	public static int RANDOM_SPAWN_TIME_SAILREN;
	public static int WAIT_TIME_SAILREN;
	
	public static int SPAWN_INTERVAL_VALAKAS;
	public static int RANDOM_SPAWN_TIME_VALAKAS;
	public static int WAIT_TIME_VALAKAS;
	
	public static int SPAWN_INTERVAL_DR_CHAOS;
	public static int RANDOM_SPAWN_TIME_DR_CHAOS;
	
	// High Priestess van Halter
	public static int SPAWN_INTERVAL_HALTER;
	public static int RANDOM_SPAWN_TIME_HALTER;
	
	/** AI */
	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static int RANDOM_WALK_RATE;
	public static int MAX_DRIFT_RANGE;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;
	
	public static final String NPCS_FILE = "config/npcs.properties";
	
	private static final void loadNpcs()
	{
		final ExProperties npcs = initProperties(Config.NPCS_FILE);
		
		Config.SPAWN_MULTIPLIER = npcs.getProperty("SpawnMultiplier", 1.);
		Config.SPAWN_EVENTS = npcs.getProperty("SpawnEvents", new String[]
		{
			"extra_mob",
			"18age",
			"start_weapon",
		});
		
		Config.CHAMPION_FREQUENCY = npcs.getProperty("ChampionFrequency", 0);
		Config.CHAMPION_DEEPBLUE_DROP_RULES = npcs.getProperty("UseChampionDeepBlueDropRules", false);
		Config.CHAMP_MIN_LVL = npcs.getProperty("ChampionMinLevel", 20);
		Config.CHAMP_MAX_LVL = npcs.getProperty("ChampionMaxLevel", 70);
		Config.CHAMPION_HP = npcs.getProperty("ChampionHp", 8);
		Config.CHAMPION_HP_REGEN = npcs.getProperty("ChampionHpRegen", 1.);
		Config.CHAMPION_RATE_XP = npcs.getProperty("ChampionRateXp", 1.);
		Config.CHAMPION_RATE_SP = npcs.getProperty("ChampionRateSp", 1.);
		Config.PREMIUM_CHAMPION_RATE_XP = npcs.getProperty("PremiumChampionRateXp", 1.);
		Config.PREMIUM_CHAMPION_RATE_SP = npcs.getProperty("PremiumChampionRateSp", 1.);
		Config.CHAMPION_REWARDS = npcs.getProperty("ChampionRewards", 1);
		Config.PREMIUM_CHAMPION_REWARDS = npcs.getProperty("PremiumChampionRewards", 1);
		Config.CHAMPION_ADENAS_REWARDS = npcs.getProperty("ChampionAdenasRewards", 1);
		Config.PREMIUM_CHAMPION_ADENAS_REWARDS = npcs.getProperty("PremiumChampionAdenasRewards", 1);
		Config.CHAMPION_ATK = npcs.getProperty("ChampionAtk", 1.);
		Config.CHAMPION_SPD_ATK = npcs.getProperty("ChampionSpdAtk", 1.);
		Config.CHAMPION_REWARD = npcs.getProperty("ChampionRewardItem", 0);
		Config.CHAMPION_REWARD_ID = npcs.getProperty("ChampionRewardItemID", 6393);
		Config.CHAMPION_REWARD_QTY = npcs.getProperty("ChampionRewardItemQty", 1);
		Config.CHAMPION_AURA = npcs.getProperty("ChampionAura", 0);
		
		Config.ALLOW_ENTIRE_TREE = npcs.getProperty("AllowEntireTree", false);
		Config.CLASS_MASTER_SETTINGS = new ClassMasterSettings(npcs.getProperty("ConfigClassMaster"));
		
		Config.WEDDING_PRICE = npcs.getProperty("WeddingPrice", 1000000);
		Config.WEDDING_SAMESEX = npcs.getProperty("WeddingAllowSameSex", false);
		Config.WEDDING_FORMALWEAR = npcs.getProperty("WeddingFormalWear", true);
		
		Config.BUFFER_MAX_SCHEMES = npcs.getProperty("BufferMaxSchemesPerChar", 4);
		Config.BUFFER_STATIC_BUFF_COST = npcs.getProperty("BufferStaticCostPerBuff", -1);
		
		Config.WYVERN_REQUIRED_LEVEL = npcs.getProperty("RequiredStriderLevel", 55);
		Config.WYVERN_REQUIRED_CRYSTALS = npcs.getProperty("RequiredCrystalsNumber", 10);
		
		Config.FREE_TELEPORT = npcs.getProperty("FreeTeleport", false);
		Config.LVL_FREE_TELEPORT = npcs.getProperty("LvlFreeTeleport", 40);
		Config.ANNOUNCE_MAMMON_SPAWN = npcs.getProperty("AnnounceMammonSpawn", true);
		Config.MOB_AGGRO_IN_PEACEZONE = npcs.getProperty("MobAggroInPeaceZone", true);
		Config.SHOW_NPC_LVL = npcs.getProperty("ShowNpcLevel", false);
		Config.SHOW_NPC_CREST = npcs.getProperty("ShowNpcCrest", false);
		Config.SHOW_SUMMON_CREST = npcs.getProperty("ShowSummonCrest", false);
		
		Config.RAID_HP_REGEN_MULTIPLIER = npcs.getProperty("RaidHpRegenMultiplier", 1.);
		Config.RAID_MP_REGEN_MULTIPLIER = npcs.getProperty("RaidMpRegenMultiplier", 1.);
		Config.RAID_DEFENCE_MULTIPLIER = npcs.getProperty("RaidDefenceMultiplier", 1.);
		Config.RAID_MINION_RESPAWN_TIMER = npcs.getProperty("RaidMinionRespawnTime", 300000);
		
		Config.RAID_DISABLE_CURSE = npcs.getProperty("DisableRaidCurse", false);
		
		Config.SPAWN_INTERVAL_ANTHARAS = npcs.getProperty("AntharasSpawnInterval", 264);
		Config.RANDOM_SPAWN_TIME_ANTHARAS = npcs.getProperty("AntharasRandomSpawn", 72);
		Config.WAIT_TIME_ANTHARAS = npcs.getProperty("AntharasWaitTime", 30) * 60000;
		
		Config.SPAWN_INTERVAL_BAIUM = npcs.getProperty("BaiumSpawnInterval", 168);
		Config.RANDOM_SPAWN_TIME_BAIUM = npcs.getProperty("BaiumRandomSpawn", 48);
		
		Config.SPAWN_INTERVAL_FRINTEZZA = npcs.getProperty("FrintezzaSpawnInterval", 48);
		Config.RANDOM_SPAWN_TIME_FRINTEZZA = npcs.getProperty("FrintezzaRandomSpawn", 8);
		Config.WAIT_TIME_FRINTEZZA = npcs.getProperty("FrintezzaWaitTime", 1) * 60000;
		
		Config.SPAWN_INTERVAL_SAILREN = npcs.getProperty("SailrenSpawnInterval", 36);
		Config.RANDOM_SPAWN_TIME_SAILREN = npcs.getProperty("SailrenRandomSpawn", 24);
		Config.WAIT_TIME_SAILREN = npcs.getProperty("SailrenWaitTime", 5) * 60000;
		
		Config.SPAWN_INTERVAL_VALAKAS = npcs.getProperty("ValakasSpawnInterval", 264);
		Config.RANDOM_SPAWN_TIME_VALAKAS = npcs.getProperty("ValakasRandomSpawn", 72);
		Config.WAIT_TIME_VALAKAS = npcs.getProperty("ValakasWaitTime", 30) * 60000;
		
		Config.SPAWN_INTERVAL_DR_CHAOS = npcs.getProperty("DrChaosSpawnInterval", 36);
		Config.RANDOM_SPAWN_TIME_DR_CHAOS = npcs.getProperty("DrChaosRandomSpawn", 24);
		
		Config.SPAWN_INTERVAL_HALTER = npcs.getProperty("HalterSpawnInterval", 36);
		Config.RANDOM_SPAWN_TIME_HALTER = npcs.getProperty("HalterRandomSpawn", 24);
		
		Config.GUARD_ATTACK_AGGRO_MOB = npcs.getProperty("GuardAttackAggroMob", false);
		Config.RANDOM_WALK_RATE = npcs.getProperty("RandomWalkRate", 30);
		Config.MAX_DRIFT_RANGE = npcs.getProperty("MaxDriftRange", 200);
		Config.MIN_NPC_ANIMATION = npcs.getProperty("MinNPCAnimation", 20);
		Config.MAX_NPC_ANIMATION = npcs.getProperty("MaxNPCAnimation", 40);
		Config.MIN_MONSTER_ANIMATION = npcs.getProperty("MinMonsterAnimation", 10);
		Config.MAX_MONSTER_ANIMATION = npcs.getProperty("MaxMonsterAnimation", 40);
	}
	
	/**
	 * Loads player settings.<br>
	 * Such as stats, inventory/warehouse, enchant, augmentation, karma, party, admin, petition, skill learn.
	 */
	
	// --------------------------------------------------
	// Players
	// --------------------------------------------------
	
	/** Misc */
	public static boolean EFFECT_CANCELING;
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static int PLAYER_SPAWN_PROTECTION;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	public static double RESPAWN_RESTORE_HP;
	public static int MAX_PVTSTORE_SLOTS_DWARF;
	public static int MAX_PVTSTORE_SLOTS_OTHER;
	public static boolean DEEPBLUE_DROP_RULES;
	public static boolean ALLOW_DELEVEL;
	public static int DEATH_PENALTY_CHANCE;
	
	/** Inventory & WH */
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_PET;
	public static int MAX_ITEM_IN_PACKET;
	public static double WEIGHT_LIMIT;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int FREIGHT_SLOTS;
	public static boolean REGION_BASED_FREIGHT;
	public static int FREIGHT_PRICE;
	
	/** Enchant */
	public static Map<Integer, Double> ENCHANT_CHANCE_WEAPON;
	public static Map<Integer, Double> ENCHANT_CHANCE_ARMOR;
	public static Map<Integer, Double> BLESSED_ENCHANT_CHANCE_WEAPON;
	public static Map<Integer, Double> BLESSED_ENCHANT_CHANCE_ARMOR;
	public static Map<Integer, Double> CRYSTAL_ENCHANT_CHANCE_WEAPON;
	public static Map<Integer, Double> CRYSTAL_ENCHANT_CHANCE_ARMOR;
	public static int ENCHANT_MAX_WEAPON;
	public static int ENCHANT_MAX_ARMOR;
	public static int ENCHANT_SAFE_MAX;
	public static int ENCHANT_SAFE_MAX_FULL;
	public static int ENCHANT_FAILED_VALUE;
	
	/** Augmentations */
	public static int AUGMENTATION_NG_SKILL_CHANCE;
	public static int AUGMENTATION_NG_GLOW_CHANCE;
	public static int AUGMENTATION_MID_SKILL_CHANCE;
	public static int AUGMENTATION_MID_GLOW_CHANCE;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int AUGMENTATION_TOP_SKILL_CHANCE;
	public static int AUGMENTATION_TOP_GLOW_CHANCE;
	public static int AUGMENTATION_BASESTAT_CHANCE;
	
	/** Karma & PvP */
	public static boolean KARMA_PLAYER_CAN_SHOP;
	public static boolean KARMA_PLAYER_CAN_USE_GK;
	public static boolean KARMA_PLAYER_CAN_TELEPORT;
	public static boolean KARMA_PLAYER_CAN_TRADE;
	public static boolean KARMA_PLAYER_CAN_USE_WH;
	
	public static boolean KARMA_DROP_GM;
	public static boolean KARMA_AWARD_PK_KILL;
	public static int KARMA_PK_LIMIT;
	
	public static int[] KARMA_NONDROPPABLE_PET_ITEMS;
	public static int[] KARMA_NONDROPPABLE_ITEMS;
	
	public static int PVP_NORMAL_TIME;
	public static int PVP_PVP_TIME;
	
	/** Party */
	public static String PARTY_XP_CUTOFF_METHOD;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int PARTY_RANGE;
	
	/** GMs & Admin Stuff */
	public static int DEFAULT_ACCESS_LEVEL;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_STARTUP_BLOCK_ALL;
	public static boolean GM_STARTUP_AUTO_LIST;
	
	/** petitions */
	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;
	
	/** Crafting **/
	public static boolean IS_CRAFTING_ENABLED;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean BLACKSMITH_USE_RECIPES;
	
	/** Skills & Classes **/
	public static boolean AUTO_LEARN_SKILLS;
	public static int LVL_AUTO_LEARN_SKILLS;
	public static boolean MAGIC_FAILURES;
	public static int PERFECT_SHIELD_BLOCK_RATE;
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean SP_BOOK_NEEDED;
	public static boolean ES_SP_BOOK_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean SUBCLASS_WITHOUT_QUESTS;
	
	/** Buffs */
	public static boolean STORE_SKILL_COOLTIME;
	public static int MAX_BUFFS_AMOUNT;
	
	public static final String PLAYERS_FILE = "config/players.properties";
	
	private static final void loadPlayers()
	{
		final ExProperties players = initProperties(Config.PLAYERS_FILE);
		
		Config.EFFECT_CANCELING = players.getProperty("CancelLesserEffect", true);
		Config.HP_REGEN_MULTIPLIER = players.getProperty("HpRegenMultiplier", 1.);
		Config.MP_REGEN_MULTIPLIER = players.getProperty("MpRegenMultiplier", 1.);
		Config.CP_REGEN_MULTIPLIER = players.getProperty("CpRegenMultiplier", 1.);
		Config.PLAYER_SPAWN_PROTECTION = players.getProperty("PlayerSpawnProtection", 0);
		Config.PLAYER_FAKEDEATH_UP_PROTECTION = players.getProperty("PlayerFakeDeathUpProtection", 5);
		Config.RESPAWN_RESTORE_HP = players.getProperty("RespawnRestoreHP", 0.7);
		Config.MAX_PVTSTORE_SLOTS_DWARF = players.getProperty("MaxPvtStoreSlotsDwarf", 5);
		Config.MAX_PVTSTORE_SLOTS_OTHER = players.getProperty("MaxPvtStoreSlotsOther", 4);
		Config.DEEPBLUE_DROP_RULES = players.getProperty("UseDeepBlueDropRules", true);
		Config.ALLOW_DELEVEL = players.getProperty("AllowDelevel", true);
		Config.DEATH_PENALTY_CHANCE = players.getProperty("DeathPenaltyChance", 20);
		
		Config.INVENTORY_MAXIMUM_NO_DWARF = players.getProperty("MaximumSlotsForNoDwarf", 80);
		Config.INVENTORY_MAXIMUM_DWARF = players.getProperty("MaximumSlotsForDwarf", 100);
		Config.INVENTORY_MAXIMUM_PET = players.getProperty("MaximumSlotsForPet", 12);
		Config.MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, INVENTORY_MAXIMUM_DWARF);
		Config.WEIGHT_LIMIT = players.getProperty("WeightLimit", 1.);
		Config.WAREHOUSE_SLOTS_NO_DWARF = players.getProperty("MaximumWarehouseSlotsForNoDwarf", 100);
		Config.WAREHOUSE_SLOTS_DWARF = players.getProperty("MaximumWarehouseSlotsForDwarf", 120);
		Config.WAREHOUSE_SLOTS_CLAN = players.getProperty("MaximumWarehouseSlotsForClan", 150);
		Config.FREIGHT_SLOTS = players.getProperty("MaximumFreightSlots", 20);
		Config.REGION_BASED_FREIGHT = players.getProperty("RegionBasedFreight", true);
		Config.FREIGHT_PRICE = players.getProperty("FreightPrice", 1000);
		
		Config.ENCHANT_CHANCE_WEAPON = new HashMap<>();
		String[] property = players.getProperty("EnchantChanceWeapon", (String[]) null, ",");
		for (String data : property)
		{
			String[] enchant = data.split("-");
			Config.ENCHANT_CHANCE_WEAPON.put(Integer.parseInt(enchant[0]), Double.parseDouble(enchant[1]));
		}
		Config.ENCHANT_CHANCE_ARMOR = new HashMap<>();
		property = players.getProperty("EnchantChanceArmor", (String[]) null, ",");
		for (String data : property)
		{
			String[] enchant = data.split("-");
			Config.ENCHANT_CHANCE_ARMOR.put(Integer.valueOf(Integer.parseInt(enchant[0])), Double.valueOf(Double.parseDouble(enchant[1])));
		}
		Config.BLESSED_ENCHANT_CHANCE_WEAPON = new HashMap<>();
		property = players.getProperty("BlessedEnchantChanceWeapon", (String[]) null, ",");
		for (String data : property)
		{
			String[] enchant = data.split("-");
			Config.BLESSED_ENCHANT_CHANCE_WEAPON.put(Integer.valueOf(Integer.parseInt(enchant[0])), Double.valueOf(Double.parseDouble(enchant[1])));
		}
		Config.BLESSED_ENCHANT_CHANCE_ARMOR = new HashMap<>();
		property = players.getProperty("BlessedEnchantChanceArmor", (String[]) null, ",");
		for (String data : property)
		{
			String[] enchant = data.split("-");
			Config.BLESSED_ENCHANT_CHANCE_ARMOR.put(Integer.valueOf(Integer.parseInt(enchant[0])), Double.valueOf(Double.parseDouble(enchant[1])));
		}
		Config.CRYSTAL_ENCHANT_CHANCE_WEAPON = new HashMap<>();
		property = players.getProperty("CrystalEnchantChanceWeapon", (String[]) null, ",");
		for (String data : property)
		{
			String[] enchant = data.split("-");
			Config.CRYSTAL_ENCHANT_CHANCE_WEAPON.put(Integer.valueOf(Integer.parseInt(enchant[0])), Double.valueOf(Double.parseDouble(enchant[1])));
		}
		Config.CRYSTAL_ENCHANT_CHANCE_ARMOR = new HashMap<>();
		property = players.getProperty("CrystalEnchantChanceArmor", (String[]) null, ",");
		for (String data : property)
		{
			String[] enchant = data.split("-");
			Config.CRYSTAL_ENCHANT_CHANCE_ARMOR.put(Integer.valueOf(Integer.parseInt(enchant[0])), Double.valueOf(Double.parseDouble(enchant[1])));
		}
		Config.ENCHANT_MAX_WEAPON = players.getProperty("EnchantMaxWeapon", 0);
		Config.ENCHANT_MAX_ARMOR = players.getProperty("EnchantMaxArmor", 0);
		Config.ENCHANT_SAFE_MAX = players.getProperty("EnchantSafeMax", 3);
		Config.ENCHANT_SAFE_MAX_FULL = players.getProperty("EnchantSafeMaxFull", 4);
		Config.ENCHANT_FAILED_VALUE = players.getProperty("EnchantFailedValue", 0);
		
		Config.AUGMENTATION_NG_SKILL_CHANCE = players.getProperty("AugmentationNGSkillChance", 15);
		Config.AUGMENTATION_NG_GLOW_CHANCE = players.getProperty("AugmentationNGGlowChance", 0);
		Config.AUGMENTATION_MID_SKILL_CHANCE = players.getProperty("AugmentationMidSkillChance", 30);
		Config.AUGMENTATION_MID_GLOW_CHANCE = players.getProperty("AugmentationMidGlowChance", 40);
		Config.AUGMENTATION_HIGH_SKILL_CHANCE = players.getProperty("AugmentationHighSkillChance", 45);
		Config.AUGMENTATION_HIGH_GLOW_CHANCE = players.getProperty("AugmentationHighGlowChance", 70);
		Config.AUGMENTATION_TOP_SKILL_CHANCE = players.getProperty("AugmentationTopSkillChance", 60);
		Config.AUGMENTATION_TOP_GLOW_CHANCE = players.getProperty("AugmentationTopGlowChance", 100);
		Config.AUGMENTATION_BASESTAT_CHANCE = players.getProperty("AugmentationBaseStatChance", 1);
		
		Config.KARMA_PLAYER_CAN_SHOP = players.getProperty("KarmaPlayerCanShop", false);
		Config.KARMA_PLAYER_CAN_USE_GK = players.getProperty("KarmaPlayerCanUseGK", false);
		Config.KARMA_PLAYER_CAN_TELEPORT = players.getProperty("KarmaPlayerCanTeleport", true);
		Config.KARMA_PLAYER_CAN_TRADE = players.getProperty("KarmaPlayerCanTrade", true);
		Config.KARMA_PLAYER_CAN_USE_WH = players.getProperty("KarmaPlayerCanUseWareHouse", true);
		Config.KARMA_DROP_GM = players.getProperty("CanGMDropEquipment", false);
		Config.KARMA_AWARD_PK_KILL = players.getProperty("AwardPKKillPVPPoint", true);
		Config.KARMA_PK_LIMIT = players.getProperty("MinimumPKRequiredToDrop", 5);
		Config.KARMA_NONDROPPABLE_PET_ITEMS = players.getProperty("ListOfPetItems", new int[]
		{
			2375,
			3500,
			3501,
			3502,
			4422,
			4423,
			4424,
			4425,
			6648,
			6649,
			6650
		});
		Config.KARMA_NONDROPPABLE_ITEMS = players.getProperty("ListOfNonDroppableItemsForPK", new int[]
		{
			1147,
			425,
			1146,
			461,
			10,
			2368,
			7,
			6,
			2370,
			2369
		});
		
		Config.PVP_NORMAL_TIME = players.getProperty("PvPVsNormalTime", 40000);
		Config.PVP_PVP_TIME = players.getProperty("PvPVsPvPTime", 20000);
		
		Config.PARTY_XP_CUTOFF_METHOD = players.getProperty("PartyXpCutoffMethod", "level");
		Config.PARTY_XP_CUTOFF_PERCENT = players.getProperty("PartyXpCutoffPercent", 3.);
		Config.PARTY_XP_CUTOFF_LEVEL = players.getProperty("PartyXpCutoffLevel", 20);
		Config.PARTY_RANGE = players.getProperty("PartyRange", 1500);
		
		Config.DEFAULT_ACCESS_LEVEL = players.getProperty("DefaultAccessLevel", 0);
		Config.GM_HERO_AURA = players.getProperty("GMHeroAura", false);
		Config.GM_STARTUP_INVULNERABLE = players.getProperty("GMStartupInvulnerable", false);
		Config.GM_STARTUP_INVISIBLE = players.getProperty("GMStartupInvisible", false);
		Config.GM_STARTUP_BLOCK_ALL = players.getProperty("GMStartupBlockAll", false);
		Config.GM_STARTUP_AUTO_LIST = players.getProperty("GMStartupAutoList", true);
		
		Config.PETITIONING_ALLOWED = players.getProperty("PetitioningAllowed", true);
		Config.MAX_PETITIONS_PER_PLAYER = players.getProperty("MaxPetitionsPerPlayer", 5);
		Config.MAX_PETITIONS_PENDING = players.getProperty("MaxPetitionsPending", 25);
		
		Config.IS_CRAFTING_ENABLED = players.getProperty("CraftingEnabled", true);
		Config.DWARF_RECIPE_LIMIT = players.getProperty("DwarfRecipeLimit", 50);
		Config.COMMON_RECIPE_LIMIT = players.getProperty("CommonRecipeLimit", 50);
		Config.BLACKSMITH_USE_RECIPES = players.getProperty("BlacksmithUseRecipes", true);
		
		Config.AUTO_LEARN_SKILLS = players.getProperty("AutoLearnSkills", false);
		Config.LVL_AUTO_LEARN_SKILLS = players.getProperty("LvlAutoLearnSkills", 40);
		Config.MAGIC_FAILURES = players.getProperty("MagicFailures", true);
		Config.PERFECT_SHIELD_BLOCK_RATE = players.getProperty("PerfectShieldBlockRate", 5);
		Config.LIFE_CRYSTAL_NEEDED = players.getProperty("LifeCrystalNeeded", true);
		Config.SP_BOOK_NEEDED = players.getProperty("SpBookNeeded", true);
		Config.ES_SP_BOOK_NEEDED = players.getProperty("EnchantSkillSpBookNeeded", true);
		Config.DIVINE_SP_BOOK_NEEDED = players.getProperty("DivineInspirationSpBookNeeded", true);
		Config.SUBCLASS_WITHOUT_QUESTS = players.getProperty("SubClassWithoutQuests", false);
		
		Config.MAX_BUFFS_AMOUNT = players.getProperty("MaxBuffsAmount", 20);
		Config.STORE_SKILL_COOLTIME = players.getProperty("StoreSkillCooltime", true);
	}
	
	/**
	 * Loads siege settings.
	 */
	
	// --------------------------------------------------
	// Sieges
	// --------------------------------------------------
	
	public static int SIEGE_LENGTH;
	public static int MINIMUM_CLAN_LEVEL;
	public static int MAX_ATTACKERS_NUMBER;
	public static int MAX_DEFENDERS_NUMBER;
	public static int ATTACKERS_RESPAWN_DELAY;
	
	public static int CH_MINIMUM_CLAN_LEVEL;
	public static int CH_MAX_ATTACKERS_NUMBER;
	
	public static final String SIEGE_FILE = "config/siege.properties";
	
	private static final void loadSieges()
	{
		final ExProperties sieges = initProperties(Config.SIEGE_FILE);
		
		Config.SIEGE_LENGTH = sieges.getProperty("SiegeLength", 120);
		Config.MINIMUM_CLAN_LEVEL = sieges.getProperty("SiegeClanMinLevel", 4);
		Config.MAX_ATTACKERS_NUMBER = sieges.getProperty("AttackerMaxClans", 10);
		Config.MAX_DEFENDERS_NUMBER = sieges.getProperty("DefenderMaxClans", 10);
		Config.ATTACKERS_RESPAWN_DELAY = sieges.getProperty("AttackerRespawn", 10000);
		
		Config.CH_MINIMUM_CLAN_LEVEL = sieges.getProperty("ChSiegeClanMinLevel", 4);
		Config.CH_MAX_ATTACKERS_NUMBER = sieges.getProperty("ChAttackerMaxClans", 10);
	}
	
	/**
	 * Loads gameserver settings.<br>
	 * IP addresses, database, rates, feature enabled/disabled, misc.
	 */
	
	// --------------------------------------------------
	// Server
	// --------------------------------------------------
	
	public static String HOSTNAME;
	public static String GAMESERVER_HOSTNAME;
	public static int GAMESERVER_PORT;
	public static String GAMESERVER_LOGIN_HOSTNAME;
	public static int GAMESERVER_LOGIN_PORT;
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	public static boolean USE_BLOWFISH_CIPHER;
	
	/** Access to database */
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	
	public static String CNAME_TEMPLATE;
	public static String TITLE_TEMPLATE;
	public static String PET_NAME_TEMPLATE;
	
	/** serverList & Test */
	public static boolean SERVER_LIST_BRACKET;
	public static boolean SERVER_LIST_CLOCK;
	public static int SERVER_LIST_AGE;
	public static boolean SERVER_LIST_TESTSERVER;
	public static boolean SERVER_LIST_PVPSERVER;
	public static boolean SERVER_GMONLY;
	
	/** clients related */
	public static int DELETE_DAYS;
	public static int MAXIMUM_ONLINE_USERS;
	
	/** Auto-loot */
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_HERBS;
	public static boolean AUTO_LOOT_RAID;
	
	/** Items Management */
	public static boolean ALLOW_DISCARDITEM;
	public static boolean MULTIPLE_ITEM_DROP;
	public static int HERB_AUTO_DESTROY_TIME;
	public static int ITEM_AUTO_DESTROY_TIME;
	public static int EQUIPABLE_ITEM_AUTO_DESTROY_TIME;
	public static Map<Integer, Integer> SPECIAL_ITEM_DESTROY_TIME;
	public static int PLAYER_DROPPED_ITEM_MULTIPLIER;
	
	/** Allow types */
	public static boolean ALLOW_FREIGHT;
	public static boolean ALLOW_WAREHOUSE;
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	public static boolean ALLOW_LOTTERY;
	public static boolean ALLOW_WATER;
	public static boolean ALLOW_BOAT;
	public static boolean ALLOW_CURSED_WEAPONS;
	public static boolean ALLOW_MANOR;
	public static boolean ENABLE_FALLING_DAMAGE;
	
	/** Debug & Dev */
	public static boolean NO_SPAWNS;
	public static boolean DEVELOPER;
	public static boolean PACKET_HANDLER_DEBUG;
	
	/** Deadlock Detector */
	public static boolean DEADLOCK_DETECTOR;
	public static int DEADLOCK_CHECK_INTERVAL;
	public static boolean RESTART_ON_DEADLOCK;
	
	/** Logs */
	public static boolean LOG_CHAT;
	public static boolean LOG_ITEMS;
	public static boolean GMAUDIT;
	
	/** Community Board */
	public static boolean ENABLE_COMMUNITY_BOARD;
	public static String BBS_DEFAULT;
	
	/** Flood Protectors */
	public static int ROLL_DICE_TIME;
	public static int HERO_VOICE_TIME;
	public static int SUBCLASS_TIME;
	public static int DROP_ITEM_TIME;
	public static int SERVER_BYPASS_TIME;
	public static int MULTISELL_TIME;
	public static int MANUFACTURE_TIME;
	public static int MANOR_TIME;
	public static int SENDMAIL_TIME;
	public static int CHARACTER_SELECT_TIME;
	public static int GLOBAL_CHAT_TIME;
	public static int TRADE_CHAT_TIME;
	public static int SOCIAL_TIME;
	public static int ITEM_TIME;
	public static int ACTION_TIME;
	
	/** ThreadPool */
	public static int SCHEDULED_THREAD_POOL_COUNT;
	public static int THREADS_PER_SCHEDULED_THREAD_POOL;
	public static int INSTANT_THREAD_POOL_COUNT;
	public static int THREADS_PER_INSTANT_THREAD_POOL;
	
	/** Misc */
	public static boolean L2WALKER_PROTECTION;
	public static boolean SERVER_NEWS;
	public static int ZONE_TOWN;
	public static int CONFIG_PROTOCOL;
	public static final String SERVER_FILE = "config/server.properties";
	
	private static final void loadServer()
	{
		final ExProperties server = initProperties(Config.SERVER_FILE);
		
		Config.HOSTNAME = server.getProperty("Hostname", "*");
		Config.GAMESERVER_HOSTNAME = server.getProperty("GameserverHostname");
		Config.GAMESERVER_PORT = server.getProperty("GameserverPort", 7777);
		Config.GAMESERVER_LOGIN_HOSTNAME = server.getProperty("LoginHost", "127.0.0.1");
		Config.GAMESERVER_LOGIN_PORT = server.getProperty("LoginPort", 9014);
		Config.REQUEST_ID = server.getProperty("RequestServerID", 0);
		Config.ACCEPT_ALTERNATE_ID = server.getProperty("AcceptAlternateID", true);
		Config.USE_BLOWFISH_CIPHER = server.getProperty("UseBlowfishCipher", true);
		
		Config.DATABASE_URL = server.getProperty("URL", "jdbc:mariadb://localhost/acis");
		Config.DATABASE_LOGIN = server.getProperty("Login", "root");
		Config.DATABASE_PASSWORD = server.getProperty("Password", "");
		Config.DATABASE_MAX_CONNECTIONS = server.getProperty("MaximumDbConnections", 10);
		
		Config.CNAME_TEMPLATE = server.getProperty("CnameTemplate", ".*");
		Config.TITLE_TEMPLATE = server.getProperty("TitleTemplate", ".*");
		Config.PET_NAME_TEMPLATE = server.getProperty("PetNameTemplate", ".*");
		
		Config.SERVER_LIST_BRACKET = server.getProperty("ServerListBrackets", false);
		Config.SERVER_LIST_CLOCK = server.getProperty("ServerListClock", false);
		Config.SERVER_GMONLY = server.getProperty("ServerGMOnly", false);
		Config.SERVER_LIST_AGE = server.getProperty("ServerListAgeLimit", 0);
		Config.SERVER_LIST_TESTSERVER = server.getProperty("TestServer", false);
		Config.SERVER_LIST_PVPSERVER = server.getProperty("PvpServer", true);
		
		Config.DELETE_DAYS = server.getProperty("DeleteCharAfterDays", 7);
		Config.MAXIMUM_ONLINE_USERS = server.getProperty("MaximumOnlineUsers", 100);
		
		Config.AUTO_LOOT = server.getProperty("AutoLoot", false);
		Config.AUTO_LOOT_HERBS = server.getProperty("AutoLootHerbs", false);
		Config.AUTO_LOOT_RAID = server.getProperty("AutoLootRaid", false);
		
		Config.ALLOW_DISCARDITEM = server.getProperty("AllowDiscardItem", true);
		Config.MULTIPLE_ITEM_DROP = server.getProperty("MultipleItemDrop", true);
		Config.HERB_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyHerbTime", 15) * 1000;
		Config.ITEM_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyItemTime", 600) * 1000;
		Config.EQUIPABLE_ITEM_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyEquipableItemTime", 0) * 1000;
		Config.SPECIAL_ITEM_DESTROY_TIME = new HashMap<>();
		String[] data = server.getProperty("AutoDestroySpecialItemTime", (String[]) null, ",");
		if (data != null)
		{
			for (String itemData : data)
			{
				String[] item = itemData.split("-");
				Config.SPECIAL_ITEM_DESTROY_TIME.put(Integer.parseInt(item[0]), Integer.parseInt(item[1]) * 1000);
			}
		}
		Config.PLAYER_DROPPED_ITEM_MULTIPLIER = server.getProperty("PlayerDroppedItemMultiplier", 1);
		
		Config.ALLOW_FREIGHT = server.getProperty("AllowFreight", true);
		Config.ALLOW_WAREHOUSE = server.getProperty("AllowWarehouse", true);
		Config.ALLOW_WEAR = server.getProperty("AllowWear", true);
		Config.WEAR_DELAY = server.getProperty("WearDelay", 5);
		Config.WEAR_PRICE = server.getProperty("WearPrice", 10);
		Config.ALLOW_LOTTERY = server.getProperty("AllowLottery", true);
		Config.ALLOW_WATER = server.getProperty("AllowWater", true);
		Config.ALLOW_MANOR = server.getProperty("AllowManor", true);
		Config.ALLOW_BOAT = server.getProperty("AllowBoat", true);
		Config.ALLOW_CURSED_WEAPONS = server.getProperty("AllowCursedWeapons", true);
		
		Config.ENABLE_FALLING_DAMAGE = server.getProperty("EnableFallingDamage", true);
		
		Config.NO_SPAWNS = server.getProperty("NoSpawns", false);
		Config.DEVELOPER = server.getProperty("Developer", false);
		Config.PACKET_HANDLER_DEBUG = server.getProperty("PacketHandlerDebug", false);
		
		Config.DEADLOCK_DETECTOR = server.getProperty("DeadLockDetector", false);
		Config.DEADLOCK_CHECK_INTERVAL = server.getProperty("DeadLockCheckInterval", 20);
		Config.RESTART_ON_DEADLOCK = server.getProperty("RestartOnDeadlock", false);
		
		Config.LOG_CHAT = server.getProperty("LogChat", false);
		Config.LOG_ITEMS = server.getProperty("LogItems", false);
		Config.GMAUDIT = server.getProperty("GMAudit", false);
		
		Config.ENABLE_COMMUNITY_BOARD = server.getProperty("EnableCommunityBoard", false);
		Config.BBS_DEFAULT = server.getProperty("BBSDefault", "_bbshome");
		
		Config.ROLL_DICE_TIME = server.getProperty("RollDiceTime", 4200);
		Config.HERO_VOICE_TIME = server.getProperty("HeroVoiceTime", 10000);
		Config.SUBCLASS_TIME = server.getProperty("SubclassTime", 2000);
		Config.DROP_ITEM_TIME = server.getProperty("DropItemTime", 1000);
		Config.SERVER_BYPASS_TIME = server.getProperty("ServerBypassTime", 100);
		Config.MULTISELL_TIME = server.getProperty("MultisellTime", 100);
		Config.MANUFACTURE_TIME = server.getProperty("ManufactureTime", 300);
		Config.MANOR_TIME = server.getProperty("ManorTime", 3000);
		Config.SENDMAIL_TIME = server.getProperty("SendMailTime", 10000);
		Config.CHARACTER_SELECT_TIME = server.getProperty("CharacterSelectTime", 3000);
		Config.GLOBAL_CHAT_TIME = server.getProperty("GlobalChatTime", 0);
		Config.TRADE_CHAT_TIME = server.getProperty("TradeChatTime", 0);
		Config.SOCIAL_TIME = server.getProperty("SocialTime", 2000);
		Config.ITEM_TIME = server.getProperty("ItemTime", 100);
		Config.ACTION_TIME = server.getProperty("ActionTime", 2000);
		
		Config.SCHEDULED_THREAD_POOL_COUNT = server.getProperty("ScheduledThreadPoolCount", -1);
		Config.THREADS_PER_SCHEDULED_THREAD_POOL = server.getProperty("ThreadsPerScheduledThreadPool", 4);
		Config.INSTANT_THREAD_POOL_COUNT = server.getProperty("InstantThreadPoolCount", -1);
		Config.THREADS_PER_INSTANT_THREAD_POOL = server.getProperty("ThreadsPerInstantThreadPool", 2);
		
		Config.L2WALKER_PROTECTION = server.getProperty("L2WalkerProtection", false);
		Config.ZONE_TOWN = server.getProperty("ZoneTown", 0);
		Config.SERVER_NEWS = server.getProperty("ShowServerNews", false);
		
		Config.CONFIG_PROTOCOL = server.getProperty("Protocol", 846);
		
	}
	
	/** Rate control */
	public static double RATE_XP;
	public static double RATE_SP;
	public static double RATE_PARTY_XP;
	public static double RATE_PARTY_SP;
	public static double RATE_DROP_CURRENCY;
	public static double RATE_DROP_ITEMS;
	public static double RATE_DROP_ITEMS_BY_RAID;
	public static double RATE_DROP_ITEMS_BY_GRAND;
	public static double RATE_DROP_SPOIL;
	
	public static double PREMIUM_RATE_XP;
	public static double PREMIUM_RATE_SP;
	public static double PREMIUM_RATE_DROP_CURRENCY;
	public static double PREMIUM_RATE_DROP_SPOIL;
	public static double PREMIUM_RATE_DROP_ITEMS;
	public static double PREMIUM_RATE_DROP_ITEMS_BY_RAID;
	public static double PREMIUM_RATE_DROP_ITEMS_BY_GRAND;
	
	public static double PREMIUM_RATE_QUEST_DROP;
	public static double PREMIUM_RATE_QUEST_REWARD;
	public static double PREMIUM_RATE_QUEST_REWARD_XP;
	public static double PREMIUM_RATE_QUEST_REWARD_SP;
	public static double PREMIUM_RATE_QUEST_REWARD_ADENA;
	
	public static double RATE_DROP_HERBS;
	public static int RATE_DROP_MANOR;
	
	public static double RATE_QUEST_DROP;
	public static double RATE_QUEST_REWARD;
	public static double RATE_QUEST_REWARD_XP;
	public static double RATE_QUEST_REWARD_SP;
	public static double RATE_QUEST_REWARD_ADENA;
	
	public static double RATE_KARMA_EXP_LOST;
	public static double RATE_SIEGE_GUARDS_PRICE;
	
	public static int PLAYER_DROP_LIMIT;
	public static int PLAYER_RATE_DROP;
	public static int PLAYER_RATE_DROP_ITEM;
	public static int PLAYER_RATE_DROP_EQUIP;
	public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
	
	public static int KARMA_DROP_LIMIT;
	public static int KARMA_RATE_DROP;
	public static int KARMA_RATE_DROP_ITEM;
	public static int KARMA_RATE_DROP_EQUIP;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON;
	
	public static double PET_XP_RATE;
	public static int PET_FOOD_RATE;
	public static double SINEATER_XP_RATE;
	
	public static final String RATES_FILE = "config/rates.properties";
	
	private static final void loadRates()
	{
		final ExProperties rates = initProperties(Config.RATES_FILE);
		Config.RATE_XP = rates.getProperty("RateXp", 1.);
		Config.RATE_SP = rates.getProperty("RateSp", 1.);
		Config.RATE_PARTY_XP = rates.getProperty("RatePartyXp", 1.);
		Config.RATE_PARTY_SP = rates.getProperty("RatePartySp", 1.);
		Config.RATE_DROP_CURRENCY = rates.getProperty("RateDropCurency", 1.);
		Config.RATE_DROP_ITEMS = rates.getProperty("RateDropItems", 1.);
		Config.RATE_DROP_ITEMS_BY_RAID = rates.getProperty("RateRaidDropItems", 1.);
		Config.RATE_DROP_ITEMS_BY_GRAND = rates.getProperty("RateGrandDropItems", 1.);
		Config.RATE_DROP_SPOIL = rates.getProperty("RateDropSpoil", 1.);
		
		Config.PREMIUM_RATE_XP = rates.getProperty("PremiumRateXp", 2.);
		Config.PREMIUM_RATE_SP = rates.getProperty("PremiumRateSp", 2.);
		Config.PREMIUM_RATE_DROP_CURRENCY = rates.getProperty("PremiumRateDropCurency", 2.);
		Config.PREMIUM_RATE_DROP_SPOIL = rates.getProperty("PremiumRateDropSpoil", 2.);
		Config.PREMIUM_RATE_DROP_ITEMS = rates.getProperty("PremiumRateDropItems", 2.);
		Config.PREMIUM_RATE_DROP_ITEMS_BY_RAID = rates.getProperty("PremiumRateRaidDropItems", 2.);
		Config.PREMIUM_RATE_DROP_ITEMS_BY_GRAND = rates.getProperty("PremiumRateGrandDropItems", 2.);
		
		Config.PREMIUM_RATE_QUEST_DROP = rates.getProperty("PremiumRateQuestDrop", 2.);
		Config.PREMIUM_RATE_QUEST_REWARD = rates.getProperty("PremiumRateQuestReward", 2.);
		Config.PREMIUM_RATE_QUEST_REWARD_XP = rates.getProperty("PremiumRateQuestRewardXP", 2.);
		Config.PREMIUM_RATE_QUEST_REWARD_SP = rates.getProperty("PremiumRateQuestRewardSP", 2.);
		Config.PREMIUM_RATE_QUEST_REWARD_ADENA = rates.getProperty("PremiumRateQuestRewardAdena", 2.);
		
		Config.RATE_DROP_HERBS = rates.getProperty("RateDropHerbs", 1.);
		Config.RATE_DROP_MANOR = rates.getProperty("RateDropManor", 1);
		Config.RATE_QUEST_DROP = rates.getProperty("RateQuestDrop", 1.);
		Config.RATE_QUEST_REWARD = rates.getProperty("RateQuestReward", 1.);
		Config.RATE_QUEST_REWARD_XP = rates.getProperty("RateQuestRewardXP", 1.);
		Config.RATE_QUEST_REWARD_SP = rates.getProperty("RateQuestRewardSP", 1.);
		Config.RATE_QUEST_REWARD_ADENA = rates.getProperty("RateQuestRewardAdena", 1.);
		Config.RATE_KARMA_EXP_LOST = rates.getProperty("RateKarmaExpLost", 1.);
		Config.RATE_SIEGE_GUARDS_PRICE = rates.getProperty("RateSiegeGuardsPrice", 1.);
		Config.PLAYER_DROP_LIMIT = rates.getProperty("PlayerDropLimit", 3);
		Config.PLAYER_RATE_DROP = rates.getProperty("PlayerRateDrop", 5);
		Config.PLAYER_RATE_DROP_ITEM = rates.getProperty("PlayerRateDropItem", 70);
		Config.PLAYER_RATE_DROP_EQUIP = rates.getProperty("PlayerRateDropEquip", 25);
		Config.PLAYER_RATE_DROP_EQUIP_WEAPON = rates.getProperty("PlayerRateDropEquipWeapon", 5);
		Config.PET_XP_RATE = rates.getProperty("PetXpRate", 1.);
		Config.PET_FOOD_RATE = rates.getProperty("PetFoodRate", 1);
		Config.SINEATER_XP_RATE = rates.getProperty("SinEaterXpRate", 1.);
		Config.KARMA_DROP_LIMIT = rates.getProperty("KarmaDropLimit", 10);
		Config.KARMA_RATE_DROP = rates.getProperty("KarmaRateDrop", 70);
		Config.KARMA_RATE_DROP_ITEM = rates.getProperty("KarmaRateDropItem", 50);
		Config.KARMA_RATE_DROP_EQUIP = rates.getProperty("KarmaRateDropEquip", 40);
		Config.KARMA_RATE_DROP_EQUIP_WEAPON = rates.getProperty("KarmaRateDropEquipWeapon", 10);
	}
	
	// --------------------------------------------------
	// MULTVERSO-ACIS
	// --------------------------------------------------
	
	/** Infinity SS and Arrows */
	public static boolean INFINITY_SS;
	public static boolean INFINITY_ARROWS;
	
	/** Olympiad Period */
	public static boolean OLY_USE_CUSTOM_PERIOD_SETTINGS;
	public static OlympiadPeriod OLY_PERIOD;
	public static int OLY_PERIOD_MULTIPLIER;
	
	public static boolean ENABLE_MODIFY_SKILL_DURATION;
	public static HashMap<Integer, Integer> SKILL_DURATION_LIST;
	
	public static String GLOBAL_CHAT;
	public static String TRADE_CHAT;
	public static int CHAT_ALL_LEVEL;
	public static int CHAT_TELL_LEVEL;
	public static int CHAT_SHOUT_LEVEL;
	public static int CHAT_TRADE_LEVEL;
	
	public static boolean ENABLE_MENU;
	public static boolean ENABLE_ONLINE_COMMAND;
	
	public static boolean BOTS_PREVENTION;
	public static int KILLS_COUNTER;
	public static int KILLS_COUNTER_RANDOMIZATION;
	public static int VALIDATION_TIME;
	public static int PUNISHMENT;
	public static int PUNISHMENT_TIME;
	
	public static boolean USE_PREMIUM_SERVICE;
	public static boolean ALTERNATE_DROP_LIST;
	public static boolean ENABLE_SKIPPING;
	
	public static boolean ATTACK_PTS;
	public static boolean SUBCLASS_SKILLS;
	public static boolean GAME_SUBCLASS_EVERYWHERE;
	
	public static boolean SHOW_NPC_INFO;
	public static boolean ALLOW_GRAND_BOSSES_TELEPORT;
	
	// chatfilter
	public static List<String> FILTER_LIST;
	
	public static boolean USE_SAY_FILTER;
	public static String CHAT_FILTER_CHARS;
	
	public static boolean CABAL_BUFFER;
	public static boolean SUPER_HASTE;
	
	public static String RESTRICTED_CHAR_NAMES;
	public static List<String> LIST_RESTRICTED_CHAR_NAMES = new ArrayList<>();
	
	public static int FAKE_ONLINE_AMOUNT;
	
	public static String BUFFS_CATEGORY;
	public static ArrayList<String> PREMIUM_BUFFS_CATEGORY = new ArrayList<>();
	
	public static boolean ANTIFEED_ENABLE;
	public static boolean ANTIFEED_DUALBOX;
	public static boolean ANTIFEED_DISCONNECTED_AS_DUALBOX;
	public static int ANTIFEED_INTERVAL;
	
	public static int DUALBOX_CHECK_MAX_PLAYERS_PER_IP;
	public static int DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP;
	public static Map<Integer, Integer> DUALBOX_CHECK_WHITELIST;
	
	public static List<Integer> AUTO_LOOT_ITEM_IDS;
	
	public static final String MULTVERSO_FILE = "config/multverso.properties";
	public static final String CHAT_FILTER_FILE = "config/chatfilter.txt";
	
	private static final void loadMultVerso()
	{
		final ExProperties multVerso = initProperties(Config.MULTVERSO_FILE);
		Config.INFINITY_SS = multVerso.getProperty("InfinitySS", false);
		Config.INFINITY_ARROWS = multVerso.getProperty("InfinityArrows", false);
		
		Config.OLY_USE_CUSTOM_PERIOD_SETTINGS = multVerso.getProperty("OlyUseCustomPeriodSettings", false);
		Config.OLY_PERIOD = OlympiadPeriod.valueOf(multVerso.getProperty("OlyPeriod", "MONTH"));
		Config.OLY_PERIOD_MULTIPLIER = multVerso.getProperty("OlyPeriodMultiplier", 1);
		
		Config.ENABLE_MODIFY_SKILL_DURATION = multVerso.getProperty("EnableModifySkillDuration", false);
		if (Config.ENABLE_MODIFY_SKILL_DURATION)
		{
			Config.SKILL_DURATION_LIST = new HashMap<>();
			String[] propertySplit = multVerso.getProperty("SkillDurationList", "").split(";");
			
			for (String skill : propertySplit)
			{
				String[] skillSplit = skill.split(",");
				if (skillSplit.length != 2)
				{
					LOGGER.warn("[SkillDurationList]: invalid config property -> SkillDurationList \"" + skill + "\"");
				}
				else
				{
					try
					{
						SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						nfe.printStackTrace();
						
						if (!skill.equals(""))
						{
							LOGGER.warn("[SkillDurationList]: invalid config property -> SkillList \"" + skillSplit[0] + "\"" + skillSplit[1]);
						}
					}
				}
			}
		}
		
		Config.GLOBAL_CHAT = multVerso.getProperty("GlobalChat", "ON");
		Config.TRADE_CHAT = multVerso.getProperty("TradeChat", "ON");
		Config.CHAT_ALL_LEVEL = multVerso.getProperty("AllChatLevel", 1);
		Config.CHAT_TELL_LEVEL = multVerso.getProperty("TellChatLevel", 1);
		Config.CHAT_SHOUT_LEVEL = multVerso.getProperty("ShoutChatLevel", 1);
		Config.CHAT_TRADE_LEVEL = multVerso.getProperty("TradeChatLevel", 1);
		
		Config.ENABLE_MENU = multVerso.getProperty("EnableMenu", false);
		Config.ENABLE_ONLINE_COMMAND = multVerso.getProperty("EnabledOnlineCommand", false);
		
		Config.BOTS_PREVENTION = multVerso.getProperty("EnableBotsPrevention", false);
		Config.KILLS_COUNTER = multVerso.getProperty("KillsCounter", 60);
		Config.KILLS_COUNTER_RANDOMIZATION = multVerso.getProperty("KillsCounterRandomization", 50);
		Config.VALIDATION_TIME = multVerso.getProperty("ValidationTime", 60);
		Config.PUNISHMENT = multVerso.getProperty("Punishment", 0);
		Config.PUNISHMENT_TIME = multVerso.getProperty("PunishmentTime", 60);
		
		Config.USE_PREMIUM_SERVICE = multVerso.getProperty("UsePremiumServices", false);
		Config.ALTERNATE_DROP_LIST = multVerso.getProperty("AlternateDropList", false);
		Config.ENABLE_SKIPPING = multVerso.getProperty("EnableSkippingItems", false);
		
		Config.ATTACK_PTS = multVerso.getProperty("AttackPTS", true);
		Config.SUBCLASS_SKILLS = multVerso.getProperty("SubClassSkills", false);
		Config.GAME_SUBCLASS_EVERYWHERE = multVerso.getProperty("SubclassEverywhere", false);
		
		Config.SHOW_NPC_INFO = multVerso.getProperty("ShowNpcInfo", false);
		Config.ALLOW_GRAND_BOSSES_TELEPORT = multVerso.getProperty("AllowGrandBossesTeleport", false);
		
		Config.USE_SAY_FILTER = multVerso.getProperty("UseChatFilter", false);
		Config.CHAT_FILTER_CHARS = multVerso.getProperty("ChatFilterChars", "^_^");
		
		try
		{
			FILTER_LIST = Files.lines(Paths.get(CHAT_FILTER_FILE), StandardCharsets.UTF_8).map(String::trim).filter(line -> (!line.isEmpty() && (line.charAt(0) != '#'))).collect(Collectors.toList());
			LOGGER.info("Loaded " + FILTER_LIST.size() + " Filter Words.");
		}
		catch (IOException e)
		{
			LOGGER.warn("Error while loading chat filter words!", e);
		}
		
		Config.CABAL_BUFFER = multVerso.getProperty("CabalBuffer", false);
		Config.SUPER_HASTE = multVerso.getProperty("SuperHaste", false);
		
		Config.RESTRICTED_CHAR_NAMES = multVerso.getProperty("ListOfRestrictedCharNames", "");
		Config.LIST_RESTRICTED_CHAR_NAMES = new ArrayList<>();
		for (String name : RESTRICTED_CHAR_NAMES.split(","))
		{
			Config.LIST_RESTRICTED_CHAR_NAMES.add(name.toLowerCase());
		}
		
		Config.FAKE_ONLINE_AMOUNT = multVerso.getProperty("FakeOnlineAmount", 1);
		
		Config.BUFFS_CATEGORY = multVerso.getProperty("PremiumBuffsCategory", "");
		Config.PREMIUM_BUFFS_CATEGORY = new ArrayList<>();
		for (String buffs : BUFFS_CATEGORY.split(","))
		{
			Config.PREMIUM_BUFFS_CATEGORY.add(buffs);
		}
		
		Config.ANTIFEED_ENABLE = multVerso.getProperty("AntiFeedEnable", false);
		Config.ANTIFEED_DUALBOX = multVerso.getProperty("AntiFeedDualbox", true);
		Config.ANTIFEED_DISCONNECTED_AS_DUALBOX = multVerso.getProperty("AntiFeedDisconnectedAsDualbox", true);
		Config.ANTIFEED_INTERVAL = multVerso.getProperty("AntiFeedInterval", 120) * 1000;
		
		Config.DUALBOX_CHECK_MAX_PLAYERS_PER_IP = multVerso.getProperty("DualboxCheckMaxPlayersPerIP", 0);
		Config.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP = multVerso.getProperty("DualboxCheckMaxOlympiadParticipantsPerIP", 0);
		String[] propertySplit = multVerso.getProperty("DualboxCheckWhitelist", "127.0.0.1,0").split(";");
		Config.DUALBOX_CHECK_WHITELIST = new HashMap<>(propertySplit.length);
		for (String entry : propertySplit)
		{
			String[] entrySplit = entry.split(",");
			if (entrySplit.length != 2)
			{
				LOGGER.warn("DualboxCheck[Config.load()]: invalid config property -> DualboxCheckWhitelist \"", entry, "\"");
			}
			else
			{
				try
				{
					int num = Integer.parseInt(entrySplit[1]);
					num = num == 0 ? -1 : num;
					DUALBOX_CHECK_WHITELIST.put(InetAddress.getByName(entrySplit[0]).hashCode(), num);
				}
				catch (UnknownHostException e)
				{
					LOGGER.warn("DualboxCheck[Config.load()]: invalid address -> DualboxCheckWhitelist \"", entrySplit[0], "\"");
				}
				catch (NumberFormatException e)
				{
					LOGGER.warn("DualboxCheck[Config.load()]: invalid number -> DualboxCheckWhitelist \"", entrySplit[1], "\"");
				}
			}
		}
		
		String[] autoLootItemIds = multVerso.getProperty("AutoLootItemIds", "0").split(",");
		Config.AUTO_LOOT_ITEM_IDS = new ArrayList<>(autoLootItemIds.length);
		for (String item : autoLootItemIds)
		{
			Integer itm = 0;
			try
			{
				itm = Integer.parseInt(item);
			}
			catch (NumberFormatException nfe)
			{
				LOGGER.warn("Auto loot item ids: Wrong ItemId passed: " + item);
			}
			
			if (itm != 0)
			{
				Config.AUTO_LOOT_ITEM_IDS.add(itm);
			}
		}
	}
	
	/**
	 * Loads loginserver settings.<br>
	 * IP addresses, database, account, misc.
	 */
	
	// --------------------------------------------------
	// Loginserver
	// --------------------------------------------------
	
	public static String LOGINSERVER_HOSTNAME;
	public static int LOGINSERVER_PORT;
	
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static boolean ACCEPT_NEW_GAMESERVER;
	
	public static boolean SHOW_LICENCE;
	
	public static boolean AUTO_CREATE_ACCOUNTS;
	
	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;
	public static final String LOGINSERVER_FILE = "config/loginserver.properties";
	
	private static final void loadLogin()
	{
		final ExProperties server = initProperties(Config.LOGINSERVER_FILE);
		
		Config.HOSTNAME = server.getProperty("Hostname", "localhost");
		Config.LOGINSERVER_HOSTNAME = server.getProperty("LoginserverHostname", "*");
		Config.LOGINSERVER_PORT = server.getProperty("LoginserverPort", 2106);
		Config.GAMESERVER_LOGIN_HOSTNAME = server.getProperty("LoginHostname", "*");
		Config.GAMESERVER_LOGIN_PORT = server.getProperty("LoginPort", 9014);
		Config.LOGIN_TRY_BEFORE_BAN = server.getProperty("LoginTryBeforeBan", 3);
		Config.LOGIN_BLOCK_AFTER_BAN = server.getProperty("LoginBlockAfterBan", 600);
		Config.ACCEPT_NEW_GAMESERVER = server.getProperty("AcceptNewGameServer", false);
		Config.SHOW_LICENCE = server.getProperty("ShowLicence", true);
		
		Config.DATABASE_URL = server.getProperty("URL", "jdbc:mariadb://localhost/acis");
		Config.DATABASE_LOGIN = server.getProperty("Login", "root");
		Config.DATABASE_PASSWORD = server.getProperty("Password", "");
		Config.DATABASE_MAX_CONNECTIONS = server.getProperty("MaximumDbConnections", 5);
		
		Config.AUTO_CREATE_ACCOUNTS = server.getProperty("AutoCreateAccounts", true);
		
		Config.FLOOD_PROTECTION = server.getProperty("EnableFloodProtection", true);
		Config.FAST_CONNECTION_LIMIT = server.getProperty("FastConnectionLimit", 15);
		Config.NORMAL_CONNECTION_TIME = server.getProperty("NormalConnectionTime", 700);
		Config.FAST_CONNECTION_TIME = server.getProperty("FastConnectionTime", 350);
		Config.MAX_CONNECTION_PER_IP = server.getProperty("MaxConnectionPerIP", 50);
	}
	
	/** Olympiad */
	public static int OLY_START_TIME;
	public static int OLY_MIN;
	public static long OLY_CPERIOD;
	public static long OLY_BATTLE;
	public static long OLY_WPERIOD;
	public static long OLY_VPERIOD;
	public static int OLY_WAIT_TIME;
	public static int OLY_WAIT_BATTLE;
	public static int OLY_WAIT_END;
	public static int OLY_START_POINTS;
	public static int OLY_WEEKLY_POINTS;
	public static int OLY_MIN_MATCHES;
	public static int OLY_CLASSED;
	public static int OLY_NONCLASSED;
	public static IntIntHolder[] OLY_CLASSED_REWARD;
	public static IntIntHolder[] OLY_NONCLASSED_REWARD;
	public static int OLY_GP_PER_POINT;
	public static int OLY_HERO_POINTS;
	public static int OLY_RANK1_POINTS;
	public static int OLY_RANK2_POINTS;
	public static int OLY_RANK3_POINTS;
	public static int OLY_RANK4_POINTS;
	public static int OLY_RANK5_POINTS;
	public static int OLY_MAX_POINTS;
	public static int OLY_DIVIDER_CLASSED;
	public static int OLY_DIVIDER_NON_CLASSED;
	public static boolean OLY_ANNOUNCE_GAMES;
	public static int OLY_ENCHANT_LIMIT;
	public static final String OLYMPIAD_FILE = "config/olympiad.properties";
	
	private static final void loadOlympiad()
	{
		final ExProperties olympiad = initProperties(Config.OLYMPIAD_FILE);
		
		Config.OLY_START_TIME = olympiad.getProperty("OlyStartTime", 18);
		Config.OLY_MIN = olympiad.getProperty("OlyMin", 0);
		Config.OLY_CPERIOD = olympiad.getProperty("OlyCPeriod", 21600000L);
		Config.OLY_BATTLE = olympiad.getProperty("OlyBattle", 180000L);
		Config.OLY_WPERIOD = olympiad.getProperty("OlyWPeriod", 604800000L);
		Config.OLY_VPERIOD = olympiad.getProperty("OlyVPeriod", 86400000L);
		Config.OLY_WAIT_TIME = olympiad.getProperty("OlyWaitTime", 30);
		Config.OLY_WAIT_BATTLE = olympiad.getProperty("OlyWaitBattle", 60);
		Config.OLY_WAIT_END = olympiad.getProperty("OlyWaitEnd", 40);
		Config.OLY_START_POINTS = olympiad.getProperty("OlyStartPoints", 18);
		Config.OLY_WEEKLY_POINTS = olympiad.getProperty("OlyWeeklyPoints", 3);
		Config.OLY_MIN_MATCHES = olympiad.getProperty("OlyMinMatchesToBeClassed", 5);
		Config.OLY_CLASSED = olympiad.getProperty("OlyClassedParticipants", 5);
		Config.OLY_NONCLASSED = olympiad.getProperty("OlyNonClassedParticipants", 9);
		Config.OLY_CLASSED_REWARD = olympiad.parseIntIntList("OlyClassedReward", "6651-50");
		Config.OLY_NONCLASSED_REWARD = olympiad.parseIntIntList("OlyNonClassedReward", "6651-30");
		Config.OLY_GP_PER_POINT = olympiad.getProperty("OlyGPPerPoint", 1000);
		Config.OLY_HERO_POINTS = olympiad.getProperty("OlyHeroPoints", 300);
		Config.OLY_RANK1_POINTS = olympiad.getProperty("OlyRank1Points", 100);
		Config.OLY_RANK2_POINTS = olympiad.getProperty("OlyRank2Points", 75);
		Config.OLY_RANK3_POINTS = olympiad.getProperty("OlyRank3Points", 55);
		Config.OLY_RANK4_POINTS = olympiad.getProperty("OlyRank4Points", 40);
		Config.OLY_RANK5_POINTS = olympiad.getProperty("OlyRank5Points", 30);
		Config.OLY_MAX_POINTS = olympiad.getProperty("OlyMaxPoints", 10);
		Config.OLY_DIVIDER_CLASSED = olympiad.getProperty("OlyDividerClassed", 3);
		Config.OLY_DIVIDER_NON_CLASSED = olympiad.getProperty("OlyDividerNonClassed", 5);
		Config.OLY_ANNOUNCE_GAMES = olympiad.getProperty("OlyAnnounceGames", true);
		Config.OLY_ENCHANT_LIMIT = olympiad.getProperty("OlyMaxEnchant", -1);
	}
	
	public static boolean CTF_EVENT_ENABLED;
	public static String[] CTF_EVENT_INTERVAL;
	public static int CTF_EVENT_PARTICIPATION_TIME;
	public static int CTF_EVENT_RUNNING_TIME;
	public static String CTF_NPC_LOC_NAME;
	public static int CTF_EVENT_PARTICIPATION_NPC_ID;
	public static int CTF_EVENT_TEAM_1_HEADQUARTERS_ID;
	public static int CTF_EVENT_TEAM_2_HEADQUARTERS_ID;
	public static int CTF_EVENT_TEAM_1_FLAG;
	public static int CTF_EVENT_TEAM_2_FLAG;
	public static int CTF_EVENT_CAPTURE_SKILL;
	public static int[] CTF_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] CTF_EVENT_PARTICIPATION_FEE = new int[2];
	public static int CTF_EVENT_MIN_PLAYERS_IN_TEAMS;
	public static int CTF_EVENT_MAX_PLAYERS_IN_TEAMS;
	public static int CTF_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int CTF_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static String CTF_EVENT_TEAM_1_NAME;
	public static int[] CTF_EVENT_TEAM_1_COORDINATES = new int[3];
	public static String CTF_EVENT_TEAM_2_NAME;
	public static int[] CTF_EVENT_TEAM_2_COORDINATES = new int[3];
	public static int[] CTF_EVENT_TEAM_1_FLAG_COORDINATES = new int[4];
	public static int[] CTF_EVENT_TEAM_2_FLAG_COORDINATES = new int[4];
	public static List<int[]> CTF_EVENT_REWARDS;
	public static boolean CTF_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
	public static boolean CTF_EVENT_SCROLL_ALLOWED;
	public static boolean CTF_EVENT_POTIONS_ALLOWED;
	public static boolean CTF_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> CTF_DOORS_IDS_TO_OPEN;
	public static List<Integer> CTF_DOORS_IDS_TO_CLOSE;
	public static boolean CTF_REWARD_TEAM_TIE;
	public static byte CTF_EVENT_MIN_LVL;
	public static byte CTF_EVENT_MAX_LVL;
	public static int CTF_EVENT_EFFECTS_REMOVAL;
	public static Map<Integer, Integer> CTF_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> CTF_EVENT_MAGE_BUFFS;
	public static final String CAPTURE_THE_FLAG_FILE = "./config/Events/CaptureTheFlag.properties";
	
	private static final void loadCaptureTheFlag()
	{
		final ExProperties captureTheFlag = initProperties(Config.CAPTURE_THE_FLAG_FILE);
		
		Config.CTF_EVENT_ENABLED = captureTheFlag.getProperty("CTFEventEnabled", false);
		Config.CTF_EVENT_INTERVAL = captureTheFlag.getProperty("CTFEventInterval", "20:00").split(",");
		Config.CTF_EVENT_PARTICIPATION_TIME = captureTheFlag.getProperty("CTFEventParticipationTime", 3600);
		Config.CTF_EVENT_RUNNING_TIME = captureTheFlag.getProperty("CTFEventRunningTime", 1800);
		Config.CTF_NPC_LOC_NAME = captureTheFlag.getProperty("CTFNpcLocName", "Giran Town");
		Config.CTF_EVENT_PARTICIPATION_NPC_ID = captureTheFlag.getProperty("CTFEventParticipationNpcId", 0);
		Config.CTF_EVENT_TEAM_1_HEADQUARTERS_ID = captureTheFlag.getProperty("CTFEventFirstTeamHeadquartersId", 0);
		Config.CTF_EVENT_TEAM_2_HEADQUARTERS_ID = captureTheFlag.getProperty("CTFcaptureTheFlagecondTeamHeadquartersId", 0);
		Config.CTF_EVENT_TEAM_1_FLAG = captureTheFlag.getProperty("CTFEventFirstTeamFlag", 0);
		Config.CTF_EVENT_TEAM_2_FLAG = captureTheFlag.getProperty("CTFcaptureTheFlagecondTeamFlag", 0);
		Config.CTF_EVENT_CAPTURE_SKILL = captureTheFlag.getProperty("CTFEventCaptureSkillId", 0);
		
		if (Config.CTF_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			Config.CTF_EVENT_ENABLED = false;
			LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventParticipationNpcId");
		}
		else
		{
			String[] ctfNpcCoords = captureTheFlag.getProperty("CTFEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (ctfNpcCoords.length < 3)
			{
				Config.CTF_EVENT_ENABLED = false;
				LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventParticipationNpcCoordinates");
			}
			else
			{
				Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
				Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
				Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
				Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
				if (ctfNpcCoords.length == 4)
				{
					Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(ctfNpcCoords[3]);
				}
				
				Config.CTF_EVENT_REWARDS = new ArrayList<>();
				Config.CTF_DOORS_IDS_TO_OPEN = new ArrayList<>();
				Config.CTF_DOORS_IDS_TO_CLOSE = new ArrayList<>();
				Config.CTF_EVENT_TEAM_1_COORDINATES = new int[3];
				Config.CTF_EVENT_TEAM_2_COORDINATES = new int[3];
				
				Config.CTF_EVENT_MIN_PLAYERS_IN_TEAMS = captureTheFlag.getProperty("CTFEventMinPlayersInTeams", 1);
				Config.CTF_EVENT_MAX_PLAYERS_IN_TEAMS = captureTheFlag.getProperty("CTFEventMaxPlayersInTeams", 20);
				Config.CTF_EVENT_MIN_LVL = Byte.parseByte(captureTheFlag.getProperty("CTFEventMinPlayerLevel", "1"));
				Config.CTF_EVENT_MAX_LVL = Byte.parseByte(captureTheFlag.getProperty("CTFEventMaxPlayerLevel", "80"));
				Config.CTF_EVENT_RESPAWN_TELEPORT_DELAY = captureTheFlag.getProperty("CTFEventRespawnTeleportDelay", 20);
				Config.CTF_EVENT_START_LEAVE_TELEPORT_DELAY = captureTheFlag.getProperty("CTFcaptureTheFlagtartLeaveTeleportDelay", 20);
				Config.CTF_EVENT_EFFECTS_REMOVAL = captureTheFlag.getProperty("CTFEventEffectsRemoval", 0);
				Config.CTF_EVENT_TEAM_1_NAME = captureTheFlag.getProperty("CTFEventTeam1Name", "Team1");
				Config.CTF_EVENT_TEAM_2_NAME = captureTheFlag.getProperty("CTFEventTeam2Name", "Team2");
				ctfNpcCoords = captureTheFlag.getProperty("CTFEventTeam1Coordinates", "0,0,0").split(",");
				if (ctfNpcCoords.length < 3)
				{
					Config.CTF_EVENT_ENABLED = false;
					LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventTeam1Coordinates");
				}
				else
				{
					Config.CTF_EVENT_TEAM_1_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
					Config.CTF_EVENT_TEAM_1_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
					Config.CTF_EVENT_TEAM_1_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
					ctfNpcCoords = captureTheFlag.getProperty("CTFEventTeam2Coordinates", "0,0,0").split(",");
					if (ctfNpcCoords.length < 3)
					{
						Config.CTF_EVENT_ENABLED = false;
						LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventTeam2Coordinates");
					}
					else
					{
						Config.CTF_EVENT_TEAM_2_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
						Config.CTF_EVENT_TEAM_2_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
						Config.CTF_EVENT_TEAM_2_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
						
						if (Config.CTF_EVENT_TEAM_1_HEADQUARTERS_ID == 0)
						{
							Config.CTF_EVENT_ENABLED = false;
							LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventFirstTeamHeadquartersId");
						}
						else
						{
							ctfNpcCoords = captureTheFlag.getProperty("CTFEventTeam1FlagCoordinates", "0,0,0").split(",");
							if (ctfNpcCoords.length < 3)
							{
								Config.CTF_EVENT_ENABLED = false;
								LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventTeam1FlagCoordinates");
							}
							else
							{
								Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES = new int[4];
								Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
								Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
								Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
								if (ctfNpcCoords.length == 4)
								{
									Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[3] = Integer.parseInt(ctfNpcCoords[3]);
								}
							}
							
							if (Config.CTF_EVENT_TEAM_2_HEADQUARTERS_ID == 0)
							{
								Config.CTF_EVENT_ENABLED = false;
								LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFcaptureTheFlagecondTeamHeadquartersId");
							}
							else
							{
								ctfNpcCoords = captureTheFlag.getProperty("CTFEventTeam2FlagCoordinates", "0,0,0").split(",");
								if (ctfNpcCoords.length < 3)
								{
									Config.CTF_EVENT_ENABLED = false;
									LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventTeam2FlagCoordinates");
								}
								else
								{
									Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES = new int[4];
									Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
									Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
									Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
									if (ctfNpcCoords.length == 4)
									{
										Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[3] = Integer.parseInt(ctfNpcCoords[3]);
									}
								}
								
								ctfNpcCoords = captureTheFlag.getProperty("CTFEventParticipationFee", "0,0").split(",");
								try
								{
									Config.CTF_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(ctfNpcCoords[0]);
									Config.CTF_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(ctfNpcCoords[1]);
								}
								catch (NumberFormatException nfe)
								{
									if (ctfNpcCoords.length > 0)
									{
										LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventParticipationFee");
									}
								}
								ctfNpcCoords = captureTheFlag.getProperty("CTFEventReward", "57,100000").split(";");
								for (String reward : ctfNpcCoords)
								{
									String[] rewardSplit = reward.split(",");
									if (rewardSplit.length != 2)
									{
										LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventReward");
									}
									else
									{
										try
										{
											Config.CTF_EVENT_REWARDS.add(new int[]
											{
												Integer.parseInt(rewardSplit[0]),
												Integer.parseInt(rewardSplit[1])
											});
										}
										catch (NumberFormatException nfe)
										{
											if (!reward.isEmpty())
											{
												LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventReward");
											}
										}
									}
								}
							}
						}
						
						Config.CTF_EVENT_TARGET_TEAM_MEMBERS_ALLOWED = captureTheFlag.getProperty("CTFEventTargetTeamMembersAllowed", true);
						Config.CTF_EVENT_SCROLL_ALLOWED = captureTheFlag.getProperty("CTFcaptureTheFlagcrollsAllowed", false);
						Config.CTF_EVENT_POTIONS_ALLOWED = captureTheFlag.getProperty("CTFEventPotionsAllowed", false);
						Config.CTF_EVENT_SUMMON_BY_ITEM_ALLOWED = captureTheFlag.getProperty("CTFcaptureTheFlagummonByItemAllowed", false);
						Config.CTF_REWARD_TEAM_TIE = captureTheFlag.getProperty("CTFRewardTeamTie", false);
						ctfNpcCoords = captureTheFlag.getProperty("CTFDoorsToOpen", "").split(";");
						for (String door : ctfNpcCoords)
						{
							try
							{
								Config.CTF_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
							}
							catch (NumberFormatException nfe)
							{
								if (!door.isEmpty())
								{
									LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFDoorsToOpen");
								}
							}
						}
						
						ctfNpcCoords = captureTheFlag.getProperty("CTFDoorsToClose", "").split(";");
						for (String door : ctfNpcCoords)
						{
							try
							{
								Config.CTF_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
							}
							catch (NumberFormatException nfe)
							{
								if (!door.isEmpty())
								{
									LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFDoorsToClose");
								}
							}
						}
						
						ctfNpcCoords = captureTheFlag.getProperty("CTFEventFighterBuffs", "").split(";");
						if (!ctfNpcCoords[0].isEmpty())
						{
							Config.CTF_EVENT_FIGHTER_BUFFS = new HashMap<>(ctfNpcCoords.length);
							for (String skill : ctfNpcCoords)
							{
								String[] skillSplit = skill.split(",");
								if (skillSplit.length != 2)
								{
									LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventFighterBuffs");
								}
								else
								{
									try
									{
										Config.CTF_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
									}
									catch (NumberFormatException nfe)
									{
										if (!skill.isEmpty())
										{
											LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventFighterBuffs");
										}
									}
								}
							}
						}
						
						ctfNpcCoords = captureTheFlag.getProperty("CTFEventMageBuffs", "").split(";");
						if (!ctfNpcCoords[0].isEmpty())
						{
							Config.CTF_EVENT_MAGE_BUFFS = new HashMap<>(ctfNpcCoords.length);
							for (String skill : ctfNpcCoords)
							{
								String[] skillSplit = skill.split(",");
								if (skillSplit.length != 2)
								{
									LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventMageBuffs");
								}
								else
								{
									try
									{
										Config.CTF_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
									}
									catch (NumberFormatException nfe)
									{
										if (!skill.isEmpty())
										{
											LOGGER.warn("CTFEventEngine[Config.load()]: invalid config property -> CTFEventMageBuffs");
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static boolean LM_EVENT_ENABLED;
	public static String[] LM_EVENT_INTERVAL;
	public static Long LM_EVENT_PARTICIPATION_TIME;
	public static int LM_EVENT_RUNNING_TIME;
	public static String LM_NPC_LOC_NAME;
	public static int LM_EVENT_PARTICIPATION_NPC_ID;
	public static short LM_EVENT_PLAYER_CREDITS;
	public static int[] LM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] LM_EVENT_PARTICIPATION_FEE = new int[2];
	public static int LM_EVENT_MIN_PLAYERS;
	public static int LM_EVENT_MAX_PLAYERS;
	public static int LM_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int LM_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static List<int[]> LM_EVENT_PLAYER_COORDINATES;
	public static List<int[]> LM_EVENT_REWARDS;
	public static boolean LM_EVENT_SCROLL_ALLOWED;
	public static boolean LM_EVENT_POTIONS_ALLOWED;
	public static boolean LM_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> LM_DOORS_IDS_TO_OPEN;
	public static List<Integer> LM_DOORS_IDS_TO_CLOSE;
	public static boolean LM_REWARD_PLAYERS_TIE;
	public static byte LM_EVENT_MIN_LVL;
	public static byte LM_EVENT_MAX_LVL;
	public static int LM_EVENT_EFFECTS_REMOVAL;
	public static Map<Integer, Integer> LM_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> LM_EVENT_MAGE_BUFFS;
	public static String DISABLE_ID_CLASSES_STRING;
	public static List<Integer> DISABLE_ID_CLASSES;
	public static final String LAST_EVENT_FILE = "config/Events/LastEvent.properties";
	
	private static final void loadLastEvent()
	{
		
		final ExProperties LastEvent = initProperties(Config.LAST_EVENT_FILE);
		Config.LM_EVENT_ENABLED = LastEvent.getProperty("LMEventEnabled", false);
		Config.LM_EVENT_INTERVAL = LastEvent.getProperty("LMEventInterval", "8:00,14:00,20:00,2:00").split(",");
		String[] timeParticipation2 = LastEvent.getProperty("LMEventParticipationTime", "01:00:00").split(":");
		Long timeLM = 0L;
		timeLM += Long.parseLong(timeParticipation2[0]) * 3600L;
		timeLM += Long.parseLong(timeParticipation2[1]) * 60L;
		timeLM += Long.parseLong(timeParticipation2[2]);
		Config.LM_EVENT_PARTICIPATION_TIME = timeLM * 1000L;
		Config.LM_EVENT_RUNNING_TIME = LastEvent.getProperty("LMEventRunningTime", 1800);
		Config.LM_NPC_LOC_NAME = LastEvent.getProperty("LMNpcLocName", "Giran Town");
		Config.LM_EVENT_PARTICIPATION_NPC_ID = LastEvent.getProperty("LMEventParticipationNpcId", 0);
		short credits = Short.parseShort(LastEvent.getProperty("LMEventPlayerCredits", "1"));
		Config.LM_EVENT_PLAYER_CREDITS = (credits > 0 ? credits : 1);
		if (Config.LM_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			Config.LM_EVENT_ENABLED = false;
			LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventParticipationNpcId");
		}
		else
		{
			String[] propertySplitLM = LastEvent.getProperty("LMEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (propertySplitLM.length < 3)
			{
				Config.LM_EVENT_ENABLED = false;
				LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventParticipationNpcCoordinates");
			}
			else if (Config.LM_EVENT_ENABLED)
			{
				Config.LM_EVENT_REWARDS = new ArrayList<>();
				Config.LM_DOORS_IDS_TO_OPEN = new ArrayList<>();
				Config.LM_DOORS_IDS_TO_CLOSE = new ArrayList<>();
				Config.LM_EVENT_PLAYER_COORDINATES = new ArrayList<>();
				
				Config.LM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
				Config.LM_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplitLM[0]);
				Config.LM_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplitLM[1]);
				Config.LM_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplitLM[2]);
				
				if (propertySplitLM.length == 4)
				{
					Config.LM_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(propertySplitLM[3]);
				}
				Config.LM_EVENT_MIN_PLAYERS = LastEvent.getProperty("LMEventMinPlayers", 1);
				Config.LM_EVENT_MAX_PLAYERS = LastEvent.getProperty("LMEventMaxPlayers", 20);
				Config.LM_EVENT_MIN_LVL = (byte) LastEvent.getProperty("LMEventMinPlayerLevel", 1);
				Config.LM_EVENT_MAX_LVL = (byte) LastEvent.getProperty("LMEventMaxPlayerLevel", 80);
				Config.LM_EVENT_RESPAWN_TELEPORT_DELAY = LastEvent.getProperty("LMEventRespawnTeleportDelay", 20);
				Config.LM_EVENT_START_LEAVE_TELEPORT_DELAY = LastEvent.getProperty("LMLastEventtartLeaveTeleportDelay", 20);
				Config.LM_EVENT_EFFECTS_REMOVAL = LastEvent.getProperty("LMEventEffectsRemoval", 0);
				
				propertySplitLM = LastEvent.getProperty("LMEventParticipationFee", "0,0").split(",");
				try
				{
					Config.LM_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplitLM[0]);
					Config.LM_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplitLM[1]);
				}
				catch (NumberFormatException nfe)
				{
					if (propertySplitLM.length > 0)
					{
						LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventParticipationFee");
					}
				}
				
				propertySplitLM = LastEvent.getProperty("LMEventReward", "57,100000;5575,5000").split("\\;");
				for (String reward : propertySplitLM)
				{
					String[] rewardSplit2 = reward.split("\\,");
					try
					{
						LM_EVENT_REWARDS.add(new int[]
						{
							Integer.parseInt(rewardSplit2[0]),
							Integer.parseInt(rewardSplit2[1])
						});
					}
					catch (NumberFormatException nfe)
					{
						LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LM_EVENT_REWARDS");
					}
				}
				
				propertySplitLM = LastEvent.getProperty("LMEventPlayerCoordinates", "0,0,0").split(";");
				for (String coordPlayer : propertySplitLM)
				{
					String[] coordSplit = coordPlayer.split(",");
					if (coordSplit.length != 3)
					{
						LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventPlayerCoordinates \"" + coordPlayer + "\"");
					}
					else
					{
						try
						{
							Config.LM_EVENT_PLAYER_COORDINATES.add(new int[]
							{
								Integer.parseInt(coordSplit[0]),
								Integer.parseInt(coordSplit[1]),
								Integer.parseInt(coordSplit[2])
							});
						}
						catch (NumberFormatException nfe)
						{
							if (!coordPlayer.isEmpty())
							{
								LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventPlayerCoordinates \"" + coordPlayer + "\"");
							}
						}
					}
				}
				
				Config.LM_EVENT_SCROLL_ALLOWED = LastEvent.getProperty("LMLastEventcrollsAllowed", false);
				Config.LM_EVENT_POTIONS_ALLOWED = LastEvent.getProperty("LMEventPotionsAllowed", false);
				Config.LM_EVENT_SUMMON_BY_ITEM_ALLOWED = LastEvent.getProperty("LMLastEventummonByItemAllowed", false);
				Config.LM_REWARD_PLAYERS_TIE = LastEvent.getProperty("LMRewardPlayersTie", false);
				
				Config.DISABLE_ID_CLASSES_STRING = LastEvent.getProperty("LMDisabledForClasses");
				Config.DISABLE_ID_CLASSES = new ArrayList<>();
				for (String class_id : Config.DISABLE_ID_CLASSES_STRING.split(","))
				{
					Config.DISABLE_ID_CLASSES.add(Integer.parseInt(class_id));
				}
				
				propertySplitLM = LastEvent.getProperty("LMDoorsToOpen", "").split(";");
				for (String door : propertySplitLM)
				{
					try
					{
						Config.LM_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
					}
					catch (NumberFormatException nfe)
					{
						if (!door.isEmpty())
						{
							LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMDoorsToOpen \"" + door + "\"");
						}
					}
				}
				
				propertySplitLM = LastEvent.getProperty("LMDoorsToClose", "").split(";");
				for (String door : propertySplitLM)
				{
					try
					{
						Config.LM_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
					}
					catch (NumberFormatException nfe)
					{
						if (!door.isEmpty())
						{
							LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMDoorsToClose \"" + door + "\"");
						}
					}
				}
				
				propertySplitLM = LastEvent.getProperty("LMEventFighterBuffs", "").split(";");
				if (!propertySplitLM[0].isEmpty())
				{
					Config.LM_EVENT_FIGHTER_BUFFS = new HashMap<>(propertySplitLM.length);
					for (String skill : propertySplitLM)
					{
						String[] skillSplit = skill.split(",");
						if (skillSplit.length != 2)
						{
							LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventFighterBuffs \"" + skill + "\"");
						}
						else
						{
							try
							{
								LM_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
							}
							catch (NumberFormatException nfe)
							{
								if (!skill.isEmpty())
								{
									LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventFighterBuffs \"" + skill + "\"");
								}
							}
						}
					}
				}
				
				propertySplitLM = LastEvent.getProperty("LMEventMageBuffs", "").split(";");
				if (!propertySplitLM[0].isEmpty())
				{
					Config.LM_EVENT_MAGE_BUFFS = new HashMap<>(propertySplitLM.length);
					for (String skill : propertySplitLM)
					{
						String[] skillSplit = skill.split(",");
						if (skillSplit.length != 2)
						{
							LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventMageBuffs \"" + skill + "\"");
						}
						else
						{
							try
							{
								Config.LM_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
							}
							catch (NumberFormatException nfe)
							{
								if (!skill.isEmpty())
								{
									LOGGER.warn("LMEventEngine[Config.load()]: invalid config property -> LMEventMageBuffs \"" + skill + "\"");
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static boolean DM_EVENT_ENABLED;
	public static String[] DM_EVENT_INTERVAL;
	public static Long DM_EVENT_PARTICIPATION_TIME;
	public static int DM_EVENT_RUNNING_TIME;
	public static String DM_NPC_LOC_NAME;
	public static int DM_EVENT_PARTICIPATION_NPC_ID;
	public static int[] DM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] DM_EVENT_PARTICIPATION_FEE = new int[2];
	public static int DM_EVENT_MIN_PLAYERS;
	public static int DM_EVENT_MAX_PLAYERS;
	public static int DM_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int DM_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static List<int[]> DM_EVENT_PLAYER_COORDINATES;
	public static Map<Integer, List<int[]>> DM_EVENT_REWARDS;
	public static int DM_REWARD_FIRST_PLAYERS;
	public static boolean DM_SHOW_TOP_RANK;
	public static int DM_TOP_RANK;
	public static boolean DM_EVENT_SCROLL_ALLOWED;
	public static boolean DM_EVENT_POTIONS_ALLOWED;
	public static boolean DM_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> DM_DOORS_IDS_TO_OPEN;
	public static List<Integer> DM_DOORS_IDS_TO_CLOSE;
	public static boolean DM_REWARD_PLAYERS_TIE;
	public static byte DM_EVENT_MIN_LVL;
	public static byte DM_EVENT_MAX_LVL;
	public static int DM_EVENT_EFFECTS_REMOVAL;
	public static Map<Integer, Integer> DM_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> DM_EVENT_MAGE_BUFFS;
	
	public static final String DEATHMATCH_FILE = "./config/Events/Deathmatch.properties";
	
	private static final void loadDeathmatch()
	{
		final ExProperties deathmatch = initProperties(Config.DEATHMATCH_FILE);
		
		Config.DM_EVENT_ENABLED = deathmatch.getProperty("DMEventEnabled", false);
		Config.DM_EVENT_INTERVAL = deathmatch.getProperty("DMEventInterval", "8:00,14:00,20:00,2:00").split(",");
		String[] timeParticipation = deathmatch.getProperty("DMEventParticipationTime", "01:00:00").split(":");
		Long timeDM = 0L;
		timeDM += Long.parseLong(timeParticipation[0]) * 3600L;
		timeDM += Long.parseLong(timeParticipation[1]) * 60L;
		timeDM += Long.parseLong(timeParticipation[2]);
		Config.DM_EVENT_PARTICIPATION_TIME = timeDM * 1000L;
		Config.DM_EVENT_RUNNING_TIME = deathmatch.getProperty("DMEventRunningTime", 1800);
		Config.DM_NPC_LOC_NAME = deathmatch.getProperty("DMNpcLocName", "Giran Town");
		Config.DM_EVENT_PARTICIPATION_NPC_ID = deathmatch.getProperty("DMEventParticipationNpcId", 0);
		Config.DM_SHOW_TOP_RANK = deathmatch.getProperty("DMShowTopRank", false);
		Config.DM_TOP_RANK = deathmatch.getProperty("DMTopRank", 10);
		if (Config.DM_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			Config.DM_EVENT_ENABLED = false;
			LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventParticipationNpcId");
		}
		else
		{
			String[] propertySplit = deathmatch.getProperty("DMEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (propertySplit.length < 3)
			{
				Config.DM_EVENT_ENABLED = false;
				LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventParticipationNpcCoordinates");
			}
			else if (Config.DM_EVENT_ENABLED)
			{
				Config.DM_EVENT_REWARDS = new HashMap<>();
				Config.DM_DOORS_IDS_TO_OPEN = new ArrayList<>();
				Config.DM_DOORS_IDS_TO_CLOSE = new ArrayList<>();
				Config.DM_EVENT_PLAYER_COORDINATES = new ArrayList<>();
				
				DM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
				Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
				Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
				Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
				
				if (propertySplit.length == 4)
				{
					Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(propertySplit[3]);
				}
				Config.DM_EVENT_MIN_PLAYERS = deathmatch.getProperty("DMEventMinPlayers", 1);
				Config.DM_EVENT_MAX_PLAYERS = deathmatch.getProperty("DMEventMaxPlayers", 20);
				Config.DM_EVENT_MIN_LVL = (byte) deathmatch.getProperty("DMEventMinPlayerLevel", 1);
				Config.DM_EVENT_MAX_LVL = (byte) deathmatch.getProperty("DMEventMaxPlayerLevel", 80);
				Config.DM_EVENT_RESPAWN_TELEPORT_DELAY = deathmatch.getProperty("DMEventRespawnTeleportDelay", 20);
				Config.DM_EVENT_START_LEAVE_TELEPORT_DELAY = deathmatch.getProperty("DMdeathmatchtartLeaveTeleportDelay", 20);
				Config.DM_EVENT_EFFECTS_REMOVAL = deathmatch.getProperty("DMEventEffectsRemoval", 0);
				
				propertySplit = deathmatch.getProperty("DMEventParticipationFee", "0,0").split(",");
				try
				{
					Config.DM_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplit[0]);
					Config.DM_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplit[1]);
				}
				catch (NumberFormatException nfe)
				{
					if (propertySplit.length > 0)
					{
						LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventParticipationFee");
					}
				}
				
				Config.DM_REWARD_FIRST_PLAYERS = deathmatch.getProperty("DMRewardFirstPlayers", 3);
				
				propertySplit = deathmatch.getProperty("DMEventReward", "57,100000;5575,5000|57,50000|57,25000").split("\\|");
				int i = 1;
				if (Config.DM_REWARD_FIRST_PLAYERS < propertySplit.length)
				{
					LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMRewardFirstPlayers < DMEventReward");
				}
				else
				{
					for (String pos : propertySplit)
					{
						List<int[]> value = new ArrayList<>();
						String[] rewardSplit = pos.split("\\;");
						for (String rewards : rewardSplit)
						{
							String[] reward = rewards.split("\\,");
							if (reward.length != 2)
							{
								LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventReward \"" + pos + "\"");
							}
							else
							{
								try
								{
									value.add(new int[]
									{
										Integer.parseInt(reward[0]),
										Integer.parseInt(reward[1])
									});
								}
								catch (NumberFormatException nfe)
								{
									LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventReward \"" + pos + "\"");
								}
							}
							
							try
							{
								if (value.isEmpty())
								{
									Config.DM_EVENT_REWARDS.put(i, Config.DM_EVENT_REWARDS.get(i - 1));
								}
								else
								{
									Config.DM_EVENT_REWARDS.put(i, value);
								}
							}
							catch (Exception e)
							{
								LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventReward array index out of bounds (1)");
								e.printStackTrace();
							}
							i++;
						}
					}
					
					int countPosRewards = Config.DM_EVENT_REWARDS.size();
					if (countPosRewards < Config.DM_REWARD_FIRST_PLAYERS)
					{
						for (i = countPosRewards + 1; i <= DM_REWARD_FIRST_PLAYERS; i++)
						{
							try
							{
								Config.DM_EVENT_REWARDS.put(i, DM_EVENT_REWARDS.get(i - 1));
							}
							catch (Exception e)
							{
								LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventReward array index out of bounds (2)");
								e.printStackTrace();
							}
						}
					}
				}
				
				propertySplit = deathmatch.getProperty("DMEventPlayerCoordinates", "0,0,0").split(";");
				for (String coordPlayer : propertySplit)
				{
					String[] coordSplit = coordPlayer.split(",");
					if (coordSplit.length != 3)
					{
						LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventPlayerCoordinates \"" + coordPlayer + "\"");
					}
					else
					{
						try
						{
							Config.DM_EVENT_PLAYER_COORDINATES.add(new int[]
							{
								Integer.parseInt(coordSplit[0]),
								Integer.parseInt(coordSplit[1]),
								Integer.parseInt(coordSplit[2])
							});
						}
						catch (NumberFormatException nfe)
						{
							if (!coordPlayer.isEmpty())
							{
								LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventPlayerCoordinates \"" + coordPlayer + "\"");
							}
						}
					}
				}
				
				Config.DM_EVENT_SCROLL_ALLOWED = deathmatch.getProperty("DMdeathmatchcrollsAllowed", false);
				Config.DM_EVENT_POTIONS_ALLOWED = deathmatch.getProperty("DMEventPotionsAllowed", false);
				Config.DM_EVENT_SUMMON_BY_ITEM_ALLOWED = deathmatch.getProperty("DMdeathmatchummonByItemAllowed", false);
				Config.DM_REWARD_PLAYERS_TIE = deathmatch.getProperty("DMRewardPlayersTie", false);
				
				Config.DISABLE_ID_CLASSES_STRING = deathmatch.getProperty("DMDisabledForClasses");
				Config.DISABLE_ID_CLASSES = new ArrayList<>();
				for (String class_id : DISABLE_ID_CLASSES_STRING.split(","))
				{
					Config.DISABLE_ID_CLASSES.add(Integer.parseInt(class_id));
				}
				
				propertySplit = deathmatch.getProperty("DMDoorsToOpen", "").split(";");
				for (String door : propertySplit)
				{
					try
					{
						Config.DM_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
					}
					catch (NumberFormatException nfe)
					{
						if (!door.isEmpty())
						{
							LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMDoorsToOpen \"" + door + "\"");
						}
					}
				}
				
				propertySplit = deathmatch.getProperty("DMDoorsToClose", "").split(";");
				for (String door : propertySplit)
				{
					try
					{
						Config.DM_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
					}
					catch (NumberFormatException nfe)
					{
						if (!door.isEmpty())
						{
							LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMDoorsToClose \"" + door + "\"");
						}
					}
				}
				
				propertySplit = deathmatch.getProperty("DMEventFighterBuffs", "").split(";");
				if (!propertySplit[0].isEmpty())
				{
					Config.DM_EVENT_FIGHTER_BUFFS = new HashMap<>(propertySplit.length);
					for (String skill : propertySplit)
					{
						String[] skillSplit = skill.split(",");
						if (skillSplit.length != 2)
						{
							LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventFighterBuffs \"" + skill + "\"");
						}
						else
						{
							try
							{
								Config.DM_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
							}
							catch (NumberFormatException nfe)
							{
								if (!skill.isEmpty())
								{
									LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventFighterBuffs \"" + skill + "\"");
								}
							}
						}
					}
				}
				
				propertySplit = deathmatch.getProperty("DMEventMageBuffs", "").split(";");
				if (!propertySplit[0].isEmpty())
				{
					Config.DM_EVENT_MAGE_BUFFS = new HashMap<>(propertySplit.length);
					for (String skill : propertySplit)
					{
						String[] skillSplit = skill.split(",");
						if (skillSplit.length != 2)
						{
							LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventMageBuffs \"" + skill + "\"");
						}
						else
						{
							try
							{
								Config.DM_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
							}
							catch (NumberFormatException nfe)
							{
								if (!skill.isEmpty())
								{
									LOGGER.warn("DMEventEngine[Config.load()]: invalid config property -> DMEventMageBuffs \"" + skill + "\"");
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static boolean TVT_EVENT_ENABLED;
	public static String[] TVT_EVENT_INTERVAL;
	public static int TVT_EVENT_PARTICIPATION_TIME;
	public static int TVT_EVENT_RUNNING_TIME;
	public static String TVT_NPC_LOC_NAME;
	public static int TVT_EVENT_PARTICIPATION_NPC_ID;
	public static int[] TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] TVT_EVENT_PARTICIPATION_FEE = new int[2];
	public static int TVT_EVENT_MIN_PLAYERS_IN_TEAMS;
	public static int TVT_EVENT_MAX_PLAYERS_IN_TEAMS;
	public static int TVT_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int TVT_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static String TVT_EVENT_TEAM_1_NAME;
	public static int[] TVT_EVENT_TEAM_1_COORDINATES = new int[3];
	public static String TVT_EVENT_TEAM_2_NAME;
	public static int[] TVT_EVENT_TEAM_2_COORDINATES = new int[3];
	public static List<int[]> TVT_EVENT_REWARDS;
	public static boolean TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
	public static boolean TVT_EVENT_SCROLL_ALLOWED;
	public static boolean TVT_EVENT_POTIONS_ALLOWED;
	public static boolean TVT_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> TVT_DOORS_IDS_TO_OPEN;
	public static List<Integer> TVT_DOORS_IDS_TO_CLOSE;
	public static boolean TVT_REWARD_TEAM_TIE;
	public static byte TVT_EVENT_MIN_LVL;
	public static byte TVT_EVENT_MAX_LVL;
	public static int TVT_EVENT_EFFECTS_REMOVAL;
	public static Map<Integer, Integer> TVT_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> TVT_EVENT_MAGE_BUFFS;
	public static boolean TVT_REWARD_PLAYER;
	public static String TVT_EVENT_ON_KILL;
	public static String DISABLE_ID_CLASSES_STRING_TVT;
	public static List<Integer> DISABLE_ID_CLASSES_TVT;
	public static boolean ALLOW_TVT_DLG;
	public static int TVT_EVENT_MAX_PARTICIPANTS_PER_IP;
	public static final String TVT_EVENT_FILE = "config/Events/TvtEvent.properties";
	
	private static final void loadTvtEvent()
	{
		final ExProperties TvtEvent = initProperties(Config.TVT_EVENT_FILE);
		
		Config.TVT_EVENT_ENABLED = TvtEvent.getProperty("TvTEventEnabled", false);
		Config.TVT_EVENT_INTERVAL = TvtEvent.getProperty("TvTEventInterval", "20:00").split(",");
		Config.TVT_EVENT_PARTICIPATION_TIME = TvtEvent.getProperty("TvTEventParticipationTime", 3600);
		Config.TVT_EVENT_RUNNING_TIME = TvtEvent.getProperty("TvTEventRunningTime", 1800);
		Config.TVT_NPC_LOC_NAME = TvtEvent.getProperty("TvTNpcLocName", "Giran Town");
		Config.TVT_EVENT_PARTICIPATION_NPC_ID = TvtEvent.getProperty("TvTEventParticipationNpcId", 0);
		
		if (Config.TVT_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			Config.TVT_EVENT_ENABLED = false;
			LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventParticipationNpcId");
		}
		else
		{
			String[] propertySplitTvT = TvtEvent.getProperty("TvTEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (propertySplitTvT.length < 3)
			{
				Config.TVT_EVENT_ENABLED = false;
				LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventParticipationNpcCoordinates");
			}
			else
			{
				Config.TVT_EVENT_REWARDS = new ArrayList<>();
				Config.TVT_DOORS_IDS_TO_OPEN = new ArrayList<>();
				Config.TVT_DOORS_IDS_TO_CLOSE = new ArrayList<>();
				Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
				Config.TVT_EVENT_TEAM_1_COORDINATES = new int[3];
				Config.TVT_EVENT_TEAM_2_COORDINATES = new int[3];
				Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplitTvT[0]);
				Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplitTvT[1]);
				Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplitTvT[2]);
				if (propertySplitTvT.length == 4)
				{
					Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(propertySplitTvT[3]);
				}
				Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS = TvtEvent.getProperty("TvTEventMinPlayersInTeams", 1);
				Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS = TvtEvent.getProperty("TvTEventMaxPlayersInTeams", 20);
				Config.TVT_EVENT_MIN_LVL = Byte.parseByte(TvtEvent.getProperty("TvTEventMinPlayerLevel", "1"));
				Config.TVT_EVENT_MAX_LVL = Byte.parseByte(TvtEvent.getProperty("TvTEventMaxPlayerLevel", "80"));
				Config.TVT_EVENT_RESPAWN_TELEPORT_DELAY = TvtEvent.getProperty("TvTEventRespawnTeleportDelay", 20);
				Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY = TvtEvent.getProperty("TvTTvtEventtartLeaveTeleportDelay", 20);
				Config.TVT_EVENT_EFFECTS_REMOVAL = TvtEvent.getProperty("TvTEventEffectsRemoval", 0);
				Config.TVT_EVENT_TEAM_1_NAME = TvtEvent.getProperty("TvTEventTeam1Name", "Team1");
				propertySplitTvT = TvtEvent.getProperty("TvTEventTeam1Coordinates", "0,0,0").split(",");
				if (propertySplitTvT.length < 3)
				{
					Config.TVT_EVENT_ENABLED = false;
					LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventTeam1Coordinates");
				}
				else
				{
					Config.TVT_EVENT_TEAM_1_COORDINATES[0] = Integer.parseInt(propertySplitTvT[0]);
					Config.TVT_EVENT_TEAM_1_COORDINATES[1] = Integer.parseInt(propertySplitTvT[1]);
					Config.TVT_EVENT_TEAM_1_COORDINATES[2] = Integer.parseInt(propertySplitTvT[2]);
					Config.TVT_EVENT_TEAM_2_NAME = TvtEvent.getProperty("TvTEventTeam2Name", "Team2");
					propertySplitTvT = TvtEvent.getProperty("TvTEventTeam2Coordinates", "0,0,0").split(",");
					if (propertySplitTvT.length < 3)
					{
						Config.TVT_EVENT_ENABLED = false;
						LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventTeam2Coordinates");
					}
					else
					{
						Config.TVT_EVENT_TEAM_2_COORDINATES[0] = Integer.parseInt(propertySplitTvT[0]);
						Config.TVT_EVENT_TEAM_2_COORDINATES[1] = Integer.parseInt(propertySplitTvT[1]);
						Config.TVT_EVENT_TEAM_2_COORDINATES[2] = Integer.parseInt(propertySplitTvT[2]);
						propertySplitTvT = TvtEvent.getProperty("TvTEventParticipationFee", "0,0").split(",");
						
						try
						{
							Config.TVT_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplitTvT[0]);
							Config.TVT_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplitTvT[1]);
						}
						catch (NumberFormatException nfe)
						{
							if (propertySplitTvT.length > 0)
							{
								LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventParticipationFee");
							}
						}
						
						propertySplitTvT = TvtEvent.getProperty("TvTEventReward", "57,100000").split(";");
						for (String reward : propertySplitTvT)
						{
							String[] rewardSplit = reward.split(",");
							if (rewardSplit.length != 2)
							{
								LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventReward \"" + reward + "\"");
							}
							else
							{
								try
								{
									Config.TVT_EVENT_REWARDS.add(new int[]
									{
										Integer.parseInt(rewardSplit[0]),
										Integer.parseInt(rewardSplit[1])
									});
								}
								catch (NumberFormatException nfe)
								{
									if (!reward.isEmpty())
									{
										LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventReward \"" + reward + "\"");
									}
								}
							}
						}
						
						Config.TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED = TvtEvent.getProperty("TvTEventTargetTeamMembersAllowed", true);
						Config.TVT_EVENT_SCROLL_ALLOWED = TvtEvent.getProperty("TvTTvtEventcrollsAllowed", false);
						Config.TVT_EVENT_POTIONS_ALLOWED = TvtEvent.getProperty("TvTEventPotionsAllowed", false);
						Config.TVT_EVENT_SUMMON_BY_ITEM_ALLOWED = TvtEvent.getProperty("TvTTvtEventummonByItemAllowed", false);
						Config.TVT_REWARD_TEAM_TIE = TvtEvent.getProperty("TvTRewardTeamTie", false);
						propertySplitTvT = TvtEvent.getProperty("TvTDoorsToOpen", "").split(";");
						for (String door : propertySplitTvT)
						{
							try
							{
								Config.TVT_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
							}
							catch (NumberFormatException nfe)
							{
								if (!door.isEmpty())
								{
									LOGGER.warn("TvTEventEngine: invalid config property -> TvTDoorsToOpen \"" + door + "\"");
								}
							}
						}
						
						propertySplitTvT = TvtEvent.getProperty("TvTDoorsToClose", "").split(";");
						for (String door : propertySplitTvT)
						{
							try
							{
								Config.TVT_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
							}
							catch (NumberFormatException nfe)
							{
								if (!door.isEmpty())
								{
									LOGGER.warn("TvTEventEngine: invalid config property -> TvTDoorsToClose \"" + door + "\"");
								}
							}
						}
						
						propertySplitTvT = TvtEvent.getProperty("TvTEventFighterBuffs", "").split(";");
						if (!propertySplitTvT[0].isEmpty())
						{
							Config.TVT_EVENT_FIGHTER_BUFFS = new HashMap<>(propertySplitTvT.length);
							for (String skill : propertySplitTvT)
							{
								String[] skillSplit = skill.split(",");
								if (skillSplit.length != 2)
								{
									LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventFighterBuffs \"" + skill + "\"");
								}
								else
								{
									try
									{
										Config.TVT_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
									}
									catch (NumberFormatException nfe)
									{
										if (!skill.isEmpty())
										{
											LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventFighterBuffs \"" + skill + "\"");
										}
									}
								}
							}
						}
						
						propertySplitTvT = TvtEvent.getProperty("TvTEventMageBuffs", "").split(";");
						if (!propertySplitTvT[0].isEmpty())
						{
							Config.TVT_EVENT_MAGE_BUFFS = new HashMap<>(propertySplitTvT.length);
							for (String skill : propertySplitTvT)
							{
								String[] skillSplit = skill.split(",");
								if (skillSplit.length != 2)
								{
									LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventMageBuffs \"" + skill + "\"");
								}
								else
								{
									try
									{
										TVT_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
									}
									catch (NumberFormatException nfe)
									{
										if (!skill.isEmpty())
										{
											LOGGER.warn("TvTEventEngine: invalid config property -> TvTEventMageBuffs \"" + skill + "\"");
										}
									}
								}
							}
						}
						
						Config.TVT_REWARD_PLAYER = TvtEvent.getProperty("TvTRewardOnlyKillers", false);
						
						Config.TVT_EVENT_ON_KILL = TvtEvent.getProperty("TvTEventOnKill", "pmteam");
						Config.DISABLE_ID_CLASSES_STRING_TVT = TvtEvent.getProperty("TvTDisabledForClasses");
						Config.DISABLE_ID_CLASSES_TVT = new ArrayList<>();
						for (String class_id : DISABLE_ID_CLASSES_STRING_TVT.split(","))
						{
							Config.DISABLE_ID_CLASSES_TVT.add(Integer.parseInt(class_id));
						}
						
						Config.ALLOW_TVT_DLG = TvtEvent.getProperty("AllowDlgTvTInvite", false);
						Config.TVT_EVENT_MAX_PARTICIPANTS_PER_IP = TvtEvent.getProperty("TvTEventMaxParticipantsPerIP", 0);
					}
				}
			}
		}
		
	}
	
	public static boolean ALLOW_LIGHT_USE_HEAVY;
	public static String NOTALLOWCLASS;
	public static List<Integer> NOTALLOWEDUSEHEAVY;
	
	public static boolean ALLOW_HEAVY_USE_LIGHT;
	public static String NOTALLOWCLASSE;
	public static List<Integer> NOTALLOWEDUSELIGHT;
	public static boolean ALT_DISABLE_BOW_CLASSES;
	public static String DISABLE_BOW_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_BOW_CLASSES = new ArrayList<>();
	public static boolean ALT_DISABLE_DAGGER_CLASSES;
	public static String DISABLE_DAGGER_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_DAGGER_CLASSES = new ArrayList<>();
	public static boolean ALT_DISABLE_SWORD_CLASSES;
	public static String DISABLE_SWORD_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_SWORD_CLASSES = new ArrayList<>();
	public static boolean ALT_DISABLE_BLUNT_CLASSES;
	public static String DISABLE_BLUNT_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_BLUNT_CLASSES = new ArrayList<>();
	public static boolean ALT_DISABLE_DUAL_CLASSES;
	public static String DISABLE_DUAL_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_DUAL_CLASSES = new ArrayList<>();
	public static boolean ALT_DISABLE_POLE_CLASSES;
	public static String DISABLE_POLE_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_POLE_CLASSES = new ArrayList<>();
	public static boolean ALT_DISABLE_BIGSWORD_CLASSES;
	public static String DISABLE_BIGSWORD_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_BIGSWORD_CLASSES = new ArrayList<>();
	
	public static final String PROTECAO_PERSONAGEM_FILE = "config/RestricaoPersonagem.properties";
	
	private static final void loadLoginRestricaoPersonagem()
	{
		final ExProperties restricaoPersonagem = initProperties(Config.PROTECAO_PERSONAGEM_FILE);
		
		Config.HOSTNAME = restricaoPersonagem.getProperty("Hostname", "localhost");
		
		ALLOW_HEAVY_USE_LIGHT = restricaoPersonagem.getProperty("AllowHeavyUseLight", false);
		NOTALLOWCLASSE = restricaoPersonagem.getProperty("NotAllowedUseLight", "");
		NOTALLOWEDUSELIGHT = new ArrayList<>();
		for (String classId : NOTALLOWCLASSE.split(","))
		{
			NOTALLOWEDUSELIGHT.add(Integer.parseInt(classId));
		}
		ALLOW_LIGHT_USE_HEAVY = restricaoPersonagem.getProperty("AllowLightUseHeavy", false);
		NOTALLOWCLASS = restricaoPersonagem.getProperty("NotAllowedUseHeavy", "");
		NOTALLOWEDUSEHEAVY = new ArrayList<>();
		for (String classId : NOTALLOWCLASS.split(","))
		{
			NOTALLOWEDUSEHEAVY.add(Integer.parseInt(classId));
		}
		
		ALT_DISABLE_BOW_CLASSES = restricaoPersonagem.getProperty("AltDisableBow", false);
		DISABLE_BOW_CLASSES_STRING = restricaoPersonagem.getProperty("DisableBowForClasses", "");
		DISABLE_BOW_CLASSES = new ArrayList<>();
		for (String class_id : DISABLE_BOW_CLASSES_STRING.split(","))
		{
			if (!class_id.equals(""))
			{
				DISABLE_BOW_CLASSES.add(Integer.parseInt(class_id));
			}
		}
		ALT_DISABLE_DAGGER_CLASSES = restricaoPersonagem.getProperty("AltDisableDagger", false);
		DISABLE_DAGGER_CLASSES_STRING = restricaoPersonagem.getProperty("DisableDaggerForClasses", "");
		DISABLE_DAGGER_CLASSES = new ArrayList<>();
		for (String class_id : DISABLE_DAGGER_CLASSES_STRING.split(","))
		{
			if (!class_id.equals(""))
			{
				DISABLE_DAGGER_CLASSES.add(Integer.parseInt(class_id));
			}
		}
		ALT_DISABLE_SWORD_CLASSES = restricaoPersonagem.getProperty("AltDisableSword", false);
		DISABLE_SWORD_CLASSES_STRING = restricaoPersonagem.getProperty("DisableSwordForClasses", "");
		DISABLE_SWORD_CLASSES = new ArrayList<>();
		for (String class_id : DISABLE_SWORD_CLASSES_STRING.split(","))
		{
			if (!class_id.equals(""))
			{
				DISABLE_SWORD_CLASSES.add(Integer.parseInt(class_id));
			}
		}
		ALT_DISABLE_BLUNT_CLASSES = restricaoPersonagem.getProperty("AltDisableBlunt", false);
		DISABLE_BLUNT_CLASSES_STRING = restricaoPersonagem.getProperty("DisableBluntForClasses", "");
		DISABLE_BLUNT_CLASSES = new ArrayList<>();
		for (String class_id : DISABLE_BLUNT_CLASSES_STRING.split(","))
		{
			if (!class_id.equals(""))
			{
				DISABLE_BLUNT_CLASSES.add(Integer.parseInt(class_id));
			}
		}
		ALT_DISABLE_DUAL_CLASSES = restricaoPersonagem.getProperty("AltDisableDual", false);
		DISABLE_DUAL_CLASSES_STRING = restricaoPersonagem.getProperty("DisableDualForClasses", "");
		DISABLE_DUAL_CLASSES = new ArrayList<>();
		for (String class_id : DISABLE_DUAL_CLASSES_STRING.split(","))
		{
			if (!class_id.equals(""))
			{
				DISABLE_DUAL_CLASSES.add(Integer.parseInt(class_id));
			}
		}
		ALT_DISABLE_POLE_CLASSES = restricaoPersonagem.getProperty("AltDisablePolle", false);
		DISABLE_POLE_CLASSES_STRING = restricaoPersonagem.getProperty("DisablePolleForClasses", "");
		DISABLE_POLE_CLASSES = new ArrayList<>();
		for (String class_id : DISABLE_POLE_CLASSES_STRING.split(","))
		{
			if (!class_id.equals(""))
			{
				DISABLE_POLE_CLASSES.add(Integer.parseInt(class_id));
			}
		}
		
	}
	
	public static final void loadGameServer()
	{
		LOGGER.info("Loading gameserver configuration files.");
		
		// fixados Eventos
		loadOlympiad();
		
		loadCaptureTheFlag();
		loadDeathmatch();
		loadLastEvent();
		loadTvtEvent();
		
		// offline settings
		loadOfflineShop();
		// clans settings
		loadClans();
		// events settings
		loadEvents();
		// geoengine settings
		loadGeoengine();
		// hexID
		loadHexID();
		// NPCs/monsters settings
		loadNpcs();
		// players settings
		loadPlayers();
		// siege settings
		loadSieges();
		// server settings
		loadServer();
		// rates settings
		loadRates();
		
		// multVerso settings
		loadMultVerso();
		loadLoginRestricaoPersonagem();
		
	}
	
	public static final void loadLoginServer()
	{
		LOGGER.info("Loading loginserver configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadAccountManager()
	{
		LOGGER.info("Loading account manager configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadGameServerRegistration()
	{
		LOGGER.info("Loading gameserver registration configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final class ClassMasterSettings
	{
		private final Map<Integer, Boolean> _allowedClassChange;
		private final Map<Integer, List<IntIntHolder>> _claimItems;
		private final Map<Integer, List<IntIntHolder>> _rewardItems;
		
		public ClassMasterSettings(String configLine)
		{
			_allowedClassChange = new HashMap<>(3);
			_claimItems = new HashMap<>(3);
			_rewardItems = new HashMap<>(3);
			
			if (configLine != null)
			{
				parseConfigLine(configLine.trim());
			}
		}
		
		private void parseConfigLine(String configLine)
		{
			StringTokenizer st = new StringTokenizer(configLine, ";");
			while (st.hasMoreTokens())
			{
				// Get allowed class change.
				int job = Integer.parseInt(st.nextToken());
				
				_allowedClassChange.put(job, true);
				
				List<IntIntHolder> items = new ArrayList<>();
				
				// Parse items needed for class change.
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						items.add(new IntIntHolder(Integer.parseInt(st3.nextToken()), Integer.parseInt(st3.nextToken())));
					}
				}
				
				// Feed the map, and clean the list.
				_claimItems.put(job, items);
				items = new ArrayList<>();
				
				// Parse gifts after class change.
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						items.add(new IntIntHolder(Integer.parseInt(st3.nextToken()), Integer.parseInt(st3.nextToken())));
					}
				}
				
				_rewardItems.put(job, items);
			}
		}
		
		public boolean isAllowed(int job)
		{
			if (_allowedClassChange == null)
			{
				return false;
			}
			
			if (_allowedClassChange.containsKey(job))
			{
				return _allowedClassChange.get(job);
			}
			
			return false;
		}
		
		public List<IntIntHolder> getRewardItems(int job)
		{
			return _rewardItems.get(job);
		}
		
		public List<IntIntHolder> getRequiredItems(int job)
		{
			return _claimItems.get(job);
		}
	}
}