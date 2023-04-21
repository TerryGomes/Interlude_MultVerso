package net.sf.l2j.gameserver.scripting.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.model.zone.type.EffectZone;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Q620_FourGoblets extends ScheduledQuest
{
	private static final String QUEST_NAME = "Q620_FourGoblets";

	// NPCs
	private static final int GHOST_OF_WIGOTH_1 = 31452; // wigoth_ghost_a
	private static final int NAMELESS_SPIRIT = 31453; // printessa_spirit
	private static final int GHOST_OF_WIGOTH_2 = 31454; // wigoth_ghost_b

	private static final int GHOST_CHAMBERLAIN_OF_ELMOREDEN = 31919; // el_lord_chamber_ghost

	private static final int KEY_BOX = 31455; // keybox_basic

	// Monsters
	private static final int STATUE_OF_PROTECTION = 18243; // r6_guard_statue

	// Items
	private static final int BROKEN_RELIC_PART = 7254;
	private static final int SEALED_BOX = 7255;

	private static final int GOBLET_OF_ALECTIA = 7256;
	private static final int GOBLET_OF_TISHAS = 7257;
	private static final int GOBLET_OF_MEKARA = 7258;
	private static final int GOBLET_OF_MORIGUL = 7259;

	private static final int CHAPEL_KEY = 7260;

	private static final int ENTRANCE_PASS_TO_THE_SEPULCHER = 7075;
	private static final int USED_GRAVE_PASS = 7261;

	// Rewards
	private static final int ANTIQUE_BROOCH = 7262;
	private static final int[] RCP_REWARDS = new int[]
	{
		6881,
		6883,
		6885,
		6887,
		6891,
		6893,
		6895,
		6897,
		6899,
		7580,
	};

	// Skills
	private static final int CHARM_OF_CORNER_EFFECT = 4628; // Level 1
	private static final int STATUE_OF_PROTECTION_EFFECT = 4383; // Level 1

	// Other
	private static final int ENTRY_TO_START = 5 * 60 * 1000; // 5 minutes
	private static final int PASSED_PERIOD = 5 * 60 * 1000; // 5 minutes

	// Data
	private static final Map<Integer, Boolean> SEPULCHER_BUSY = new HashMap<>(4);
	static
	{
		SEPULCHER_BUSY.put(31921, false); // conquerors_keeper
		SEPULCHER_BUSY.put(31922, false); // lords_keeper
		SEPULCHER_BUSY.put(31923, false); // savants_keeper
		SEPULCHER_BUSY.put(31924, false); // magistrates_keeper
	}

	private static final Map<Integer, Location> SEPULCHER_MANAGERS = new HashMap<>(4);
	static
	{
		SEPULCHER_MANAGERS.put(31921, new Location(181528, -85583, -7216)); // conquerors_keeper
		SEPULCHER_MANAGERS.put(31922, new Location(179849, -88990, -7216)); // lords_keeper
		SEPULCHER_MANAGERS.put(31923, new Location(173216, -86195, -7216)); // savants_keeper
		SEPULCHER_MANAGERS.put(31924, new Location(175615, -82365, -7216)); // magistrates_keeper
	}

	private static final Map<Integer, Integer> HALL_GATEKEEPER_DOORS = new HashMap<>(20);
	static
	{
		HALL_GATEKEEPER_DOORS.put(31925, 25150012); // conq_barons_lock
		HALL_GATEKEEPER_DOORS.put(31926, 25150013); // conq_viscounts_lock
		HALL_GATEKEEPER_DOORS.put(31927, 25150014); // conq_counts_lock
		HALL_GATEKEEPER_DOORS.put(31928, 25150015); // conq_marquis_lock
		HALL_GATEKEEPER_DOORS.put(31929, 25150016); // conq_dukes_lock

		HALL_GATEKEEPER_DOORS.put(31930, 25150002); // lords_barons_lock
		HALL_GATEKEEPER_DOORS.put(31931, 25150003); // lords_viscounts_lock
		HALL_GATEKEEPER_DOORS.put(31932, 25150004); // lords_counts_lock
		HALL_GATEKEEPER_DOORS.put(31933, 25150005); // lords_marquis_lock
		HALL_GATEKEEPER_DOORS.put(31934, 25150006); // lords_dukes_lock

		HALL_GATEKEEPER_DOORS.put(31935, 25150032); // sav_barons_lock
		HALL_GATEKEEPER_DOORS.put(31936, 25150033); // sav_viscounts_lock
		HALL_GATEKEEPER_DOORS.put(31937, 25150034); // sav_counts_lock
		HALL_GATEKEEPER_DOORS.put(31938, 25150035); // sav_marquis_lock
		HALL_GATEKEEPER_DOORS.put(31939, 25150036); // sav_dukes_lock

		HALL_GATEKEEPER_DOORS.put(31940, 25150022); // mag_barons_lock
		HALL_GATEKEEPER_DOORS.put(31941, 25150023); // mag_viscounts_lock
		HALL_GATEKEEPER_DOORS.put(31942, 25150024); // mag_counts_lock
		HALL_GATEKEEPER_DOORS.put(31943, 25150025); // mag_marquis_lock
		HALL_GATEKEEPER_DOORS.put(31944, 25150026); // mag_dukes_lock
	}

	private static final int[] HALL_GATEKEEPER_MONSTERS = new int[]
	{
		// Have walk speed but no run speed.
		18244,
		18245,
		18246,
	};

	private static final Map<Integer, HallGatekeeperSpawn> HALL_GATEKEEPER_MONSTER_SPAWNS = new HashMap<>(20);
	static
	{
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31925, new HallGatekeeperSpawn(180338, -88978, -7216, 173222, -85777, -7216, 175612, -81930, -7216)); // conq_barons_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31926, new HallGatekeeperSpawn(182153, -88980, -7216, 173207, -83935, -7216, 175608, -80107, -7216)); // conq_viscounts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31927, new HallGatekeeperSpawn(183959, -88978, -7216, 173208, -82123, -7216, 175605, -78305, -7216)); // conq_counts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31928, new HallGatekeeperSpawn(185766, -88974, -7216, 173198, -80306, -7216, 175601, -76491, -7216)); // conq_marquis_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31929, new HallGatekeeperSpawn(187583, -88972, -7216, 173197, -78501, -7216, 175596, -74683, -7216)); // conq_dukes_lock

		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31930, new HallGatekeeperSpawn(182044, -85579, -7216, 173222, -85777, -7216, 175612, -81930, -7216)); // lords_barons_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31931, new HallGatekeeperSpawn(183857, -85576, -7216, 173207, -83935, -7216, 175608, -80107, -7216)); // lords_viscounts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31932, new HallGatekeeperSpawn(185669, -85577, -7216, 173208, -82123, -7216, 175605, -78305, -7216)); // lords_counts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31933, new HallGatekeeperSpawn(187474, -85560, -7216, 173198, -80306, -7216, 175601, -76491, -7216)); // lords_marquis_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31934, new HallGatekeeperSpawn(189289, -85573, -7216, 173197, -78501, -7216, 175596, -74683, -7216)); // lords_dukes_lock

		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31935, new HallGatekeeperSpawn(182044, -85579, -7216, 180338, -88978, -7216, 175612, -81930, -7216)); // sav_barons_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31936, new HallGatekeeperSpawn(183857, -85576, -7216, 182153, -88980, -7216, 175608, -80107, -7216)); // sav_viscounts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31937, new HallGatekeeperSpawn(185669, -85577, -7216, 183959, -88978, -7216, 175605, -78305, -7216)); // sav_counts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31938, new HallGatekeeperSpawn(187474, -85560, -7216, 185766, -88974, -7216, 175601, -76491, -7216)); // sav_marquis_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31939, new HallGatekeeperSpawn(189289, -85573, -7216, 187583, -88972, -7216, 175596, -74683, -7216)); // sav_dukes_lock

		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31940, new HallGatekeeperSpawn(182044, -85579, -7216, 180338, -88978, -7216, 173222, -85777, -7216)); // mag_barons_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31941, new HallGatekeeperSpawn(183857, -85576, -7216, 182153, -88980, -7216, 173207, -83935, -7216)); // mag_viscounts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31942, new HallGatekeeperSpawn(185669, -85577, -7216, 183959, -88978, -7216, 173208, -82123, -7216)); // mag_counts_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31943, new HallGatekeeperSpawn(187474, -85560, -7216, 185766, -88974, -7216, 173198, -80306, -7216)); // mag_marquis_lock
		HALL_GATEKEEPER_MONSTER_SPAWNS.put(31944, new HallGatekeeperSpawn(189289, -85573, -7216, 187583, -88972, -7216, 173197, -78501, -7216)); // mag_dukes_lock
	}

	private static final Map<Integer, String> MYSTERIOUS_BOX_SPAWNS = new HashMap<>(24);
	static
	{
		MYSTERIOUS_BOX_SPAWNS.put(31921, "four_sepulchers_1_a_box"); // conquerors_keeper
		MYSTERIOUS_BOX_SPAWNS.put(31925, "four_sepulchers_1_b_box"); // conq_barons_lock
		MYSTERIOUS_BOX_SPAWNS.put(31926, "four_sepulchers_1_c_box"); // conq_viscounts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31927, "four_sepulchers_1_d_box"); // conq_counts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31928, "four_sepulchers_1_e_box"); // conq_marquis_lock
		MYSTERIOUS_BOX_SPAWNS.put(31929, "four_sepulchers_1_f_#4"); // conq_dukes_lock

		MYSTERIOUS_BOX_SPAWNS.put(31922, "four_sepulchers_2_a_box"); // lords_keeper
		MYSTERIOUS_BOX_SPAWNS.put(31930, "four_sepulchers_2_b_box"); // lords_barons_lock
		MYSTERIOUS_BOX_SPAWNS.put(31931, "four_sepulchers_2_c_box"); // lords_viscounts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31932, "four_sepulchers_2_d_box"); // lords_counts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31933, "four_sepulchers_2_e_box"); // lords_marquis_lock
		MYSTERIOUS_BOX_SPAWNS.put(31934, "four_sepulchers_2_f_#4"); // lords_dukes_lock

		MYSTERIOUS_BOX_SPAWNS.put(31923, "four_sepulchers_3_a_box"); // savants_keeper
		MYSTERIOUS_BOX_SPAWNS.put(31935, "four_sepulchers_3_b_box"); // sav_barons_lock
		MYSTERIOUS_BOX_SPAWNS.put(31936, "four_sepulchers_3_c_box"); // sav_viscounts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31937, "four_sepulchers_3_d_box"); // sav_counts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31938, "four_sepulchers_3_e_box"); // sav_marquis_lock
		MYSTERIOUS_BOX_SPAWNS.put(31939, "four_sepulchers_3_f_#4"); // sav_dukes_lock

		MYSTERIOUS_BOX_SPAWNS.put(31924, "four_sepulchers_4_a_box"); // magistrates_keeper
		MYSTERIOUS_BOX_SPAWNS.put(31940, "four_sepulchers_4_b_box"); // mag_barons_lock
		MYSTERIOUS_BOX_SPAWNS.put(31941, "four_sepulchers_4_c_box"); // mag_viscounts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31942, "four_sepulchers_4_d_box"); // mag_counts_lock
		MYSTERIOUS_BOX_SPAWNS.put(31943, "four_sepulchers_4_e_box"); // mag_marquis_lock
		MYSTERIOUS_BOX_SPAWNS.put(31944, "four_sepulchers_4_f_#4"); // mag_dukes_lock
	}

	private static final Map<Integer, String> MONSTER_SPAWNS = new HashMap<>(20);
	static
	{
		MONSTER_SPAWNS.put(31468, "four_sepulchers_1_a_#3"); // conq_barons_triggerbox
		MONSTER_SPAWNS.put(31469, "four_sepulchers_1_b_#2_1"); // conq_vis_triggerbox
		MONSTER_SPAWNS.put(31470, "four_sepulchers_1_c_#2"); // conq_counts_triggerbox
		MONSTER_SPAWNS.put(31471, "four_sepulchers_1_d_#3"); // conq_marquis_triggerbox
		MONSTER_SPAWNS.put(31472, "four_sepulchers_1_e_#2_1"); // conq_dukes_triggerbox

		MONSTER_SPAWNS.put(31473, "four_sepulchers_2_a_#3"); // lords_barons_triggerbox
		MONSTER_SPAWNS.put(31474, "four_sepulchers_2_b_#2_1"); // lords_vis_triggerbox
		MONSTER_SPAWNS.put(31475, "four_sepulchers_2_c_#2"); // lords_counts_triggerbox
		MONSTER_SPAWNS.put(31476, "four_sepulchers_2_d_#3"); // lords_marq_triggerbox
		MONSTER_SPAWNS.put(31477, "four_sepulchers_2_e_#2_1"); // lords_dukes_triggerbox

		MONSTER_SPAWNS.put(31478, "four_sepulchers_3_a_#3"); // sav_barons_triggerbox
		MONSTER_SPAWNS.put(31479, "four_sepulchers_3_b_#2_1"); // sav_vis_triggerbox
		MONSTER_SPAWNS.put(31480, "four_sepulchers_3_c_#2"); // sav_counts_triggerbox
		MONSTER_SPAWNS.put(31481, "four_sepulchers_3_d_#3"); // sav_marquis_triggerbox
		MONSTER_SPAWNS.put(31482, "four_sepulchers_3_e_#2_1"); // sav_dukes_triggerbox

		MONSTER_SPAWNS.put(31483, "four_sepulchers_4_a_#3"); // mag_barons_triggerbox
		MONSTER_SPAWNS.put(31484, "four_sepulchers_4_b_#2_1"); // mag_vis_triggerbox
		MONSTER_SPAWNS.put(31485, "four_sepulchers_4_c_#2"); // mag_counts_triggerbox
		MONSTER_SPAWNS.put(31486, "four_sepulchers_4_d_#3"); // mag_marquis_triggerbox
		MONSTER_SPAWNS.put(31487, "four_sepulchers_4_e_#2_1"); // mag_dukes_triggerbox
	}

	private static final Map<String, String> MONSTER_NEXT_SPAWN = new HashMap<>(24);
	static
	{
		MONSTER_NEXT_SPAWN.put("four_sepulchers_1_b_1_1", "four_sepulchers_1_b_1_2"); // 1st_type1_b1 -> 1st_type1_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_1_b_1_2", "four_sepulchers_1_b_1_3"); // 1st_type1_b2 -> 1st_type1_b3
		MONSTER_NEXT_SPAWN.put("four_sepulchers_1_b_2_1", "four_sepulchers_1_b_2_2"); // 1st_type2_b1 -> 1st_type2_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_1_b_2_2", "four_sepulchers_1_b_2_3"); // 1st_type2_b2 -> 1st_type2_b3
		MONSTER_NEXT_SPAWN.put("four_sepulchers_1_e_1_1", "four_sepulchers_1_e_1_2"); // 1st_type1_e -> 1st_type1_boss_e
		MONSTER_NEXT_SPAWN.put("four_sepulchers_1_e_2_1", "four_sepulchers_1_e_2_2"); // 1st_type2_e -> 1st_type2_boss_e

		MONSTER_NEXT_SPAWN.put("four_sepulchers_2_b_1_1", "four_sepulchers_2_b_1_2"); // 2nd_type1_b1 -> 2nd_type1_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_2_b_1_2", "four_sepulchers_2_b_1_3"); // 2nd_type1_b2 -> 2nd_type1_b3
		MONSTER_NEXT_SPAWN.put("four_sepulchers_2_b_2_1", "four_sepulchers_2_b_2_2"); // 2nd_type2_b1 -> 2nd_type2_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_2_b_2_2", "four_sepulchers_2_b_2_3"); // 2nd_type2_b2 -> 2nd_type2_b3
		MONSTER_NEXT_SPAWN.put("four_sepulchers_2_e_1_1", "four_sepulchers_2_e_1_2"); // 2nd_type1_e -> 2nd_type1_boss_e
		MONSTER_NEXT_SPAWN.put("four_sepulchers_2_e_2_1", "four_sepulchers_2_e_2_2"); // 2nd_type2_e -> 2nd_type2_boss_e

		MONSTER_NEXT_SPAWN.put("four_sepulchers_3_b_1_1", "four_sepulchers_3_b_1_2"); // 3rd_type1_b1 -> 3rd_type1_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_3_b_1_2", "four_sepulchers_3_b_1_3"); // 3rd_type1_b2 -> 3rd_type1_b3
		MONSTER_NEXT_SPAWN.put("four_sepulchers_3_b_2_1", "four_sepulchers_3_b_2_2"); // 3rd_type2_b1 -> 3rd_type2_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_3_b_2_2", "four_sepulchers_3_b_2_3"); // 3rd_type2_b2 -> 3rd_type2_b3
		MONSTER_NEXT_SPAWN.put("four_sepulchers_3_e_1_1", "four_sepulchers_3_e_1_2"); // 3rd_type1_e -> 3rd_type1_boss_e
		MONSTER_NEXT_SPAWN.put("four_sepulchers_3_e_2_1", "four_sepulchers_3_e_2_2"); // 3rd_type2_e -> 3rd_type2_boss_e

		MONSTER_NEXT_SPAWN.put("four_sepulchers_4_b_1_1", "four_sepulchers_4_b_1_2"); // 4th_type1_b1 -> 4th_type1_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_4_b_1_2", "four_sepulchers_4_b_1_3"); // 4th_type1_b2 -> 4th_type1_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_4_b_2_1", "four_sepulchers_4_b_2_2"); // 4th_type2_b1 -> 4th_type2_b2
		MONSTER_NEXT_SPAWN.put("four_sepulchers_4_b_2_2", "four_sepulchers_4_b_2_3"); // 4th_type2_b2 -> 4th_type2_b3
		MONSTER_NEXT_SPAWN.put("four_sepulchers_4_e_1_1", "four_sepulchers_4_e_1_2"); // 4th_type1_e -> 4th_type1_boss_e
		MONSTER_NEXT_SPAWN.put("four_sepulchers_4_e_2_1", "four_sepulchers_4_e_2_2"); // 4th_type2_e -> 4th_type2_boss_e
	}

	private static final Map<String, String> HALISHA_BOX_SPAWN = new HashMap<>(16);
	static
	{
		HALISHA_BOX_SPAWN.put("four_sepulchers_1_f_1", "four_sepulchers_1_f_treasure"); // 1st_boss_type1 -> 1st_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_1_f_2", "four_sepulchers_1_f_treasure"); // 1st_boss_type2 -> 1st_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_1_f_3", "four_sepulchers_1_f_treasure"); // 1st_boss_type3 -> 1st_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_1_f_4", "four_sepulchers_1_f_treasure"); // 1st_boss_type4 -> 1st_boss_treasure

		HALISHA_BOX_SPAWN.put("four_sepulchers_2_f_1", "four_sepulchers_2_f_treasure"); // 2nd_boss_type1 -> 2nd_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_2_f_2", "four_sepulchers_2_f_treasure"); // 2nd_boss_type2 -> 2nd_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_2_f_3", "four_sepulchers_2_f_treasure"); // 2nd_boss_type3 -> 2nd_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_2_f_4", "four_sepulchers_2_f_treasure"); // 2nd_boss_type4 -> 2nd_boss_treasure

		HALISHA_BOX_SPAWN.put("four_sepulchers_3_f_1", "four_sepulchers_3_f_treasure"); // 3rd_boss_type1 -> 3rd_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_3_f_2", "four_sepulchers_3_f_treasure"); // 3rd_boss_type2 -> 3rd_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_3_f_3", "four_sepulchers_3_f_treasure"); // 3rd_boss_type3 -> 3rd_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_3_f_4", "four_sepulchers_3_f_treasure"); // 3rd_boss_type4 -> 3rd_boss_treasure

		HALISHA_BOX_SPAWN.put("four_sepulchers_4_f_1", "four_sepulchers_4_f_treasure"); // 4th_boss_type1 -> 4th_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_4_f_2", "four_sepulchers_4_f_treasure"); // 4th_boss_type2 -> 4th_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_4_f_3", "four_sepulchers_4_f_treasure"); // 4th_boss_type3 -> 4th_boss_treasure
		HALISHA_BOX_SPAWN.put("four_sepulchers_4_f_4", "four_sepulchers_4_f_treasure"); // 4th_boss_type4 -> 4th_boss_treasure
	}

	private static final Map<Integer, Integer> VICTIM_SPAWNS = new HashMap<>(8);
	static
	{
		VICTIM_SPAWNS.put(18150, 18158); // r31_mission_roomboss1 -> r31_roomboss1
		VICTIM_SPAWNS.put(18151, 18159); // r31_mission_roomboss2 -> r31_roomboss2

		VICTIM_SPAWNS.put(18152, 18160); // r32_mission_roomboss1 -> r32_roomboss1
		VICTIM_SPAWNS.put(18153, 18161); // r32_mission_roomboss2 -> r32_roomboss2

		VICTIM_SPAWNS.put(18154, 18162); // r33_mission_roomboss1 -> r33_roomboss1
		VICTIM_SPAWNS.put(18155, 18163); // r33_mission_roomboss2 -> r33_roomboss2

		VICTIM_SPAWNS.put(18156, 18164); // r34_mission_roomboss1 -> r34_roomboss1
		VICTIM_SPAWNS.put(18157, 18165); // r34_mission_roomboss2 -> r34_roomboss2
	}

	private static final int[] VICTIM_ATTACKERS = new int[]
	{
		18166, // r3_warrior
		18167, // r3_warrior_longatk1_h
		18168, // r3_warrior_longatk2
		18169, // r3_warrior_selfbuff
	};

	private static final int[] VICTIM_BUFFS = new int[]
	{
		4384,
		4385,
		4386,
		4387,
	};

	private static final Map<Integer, Integer> STATUE_TYPE = new HashMap<>(16);
	static
	{
		STATUE_TYPE.put(18196, 0); // r41_controller_weakness
		STATUE_TYPE.put(18197, 1); // r41_controller_pddown
		STATUE_TYPE.put(18198, 2); // r41_controller_poison
		STATUE_TYPE.put(18199, 3); // r41_controller_nonheal

		STATUE_TYPE.put(18200, 0); // r42_controller_weakness
		STATUE_TYPE.put(18201, 1); // r42_controller_pddown
		STATUE_TYPE.put(18202, 2); // r42_controller_poison
		STATUE_TYPE.put(18203, 3); // r42_controller_nonheal

		STATUE_TYPE.put(18204, 0); // r43_controller_weakness
		STATUE_TYPE.put(18205, 1); // r43_controller_pddown
		STATUE_TYPE.put(18206, 2); // r43_controller_poison
		STATUE_TYPE.put(18207, 3); // r43_controller_nonheal

		STATUE_TYPE.put(18208, 0); // r44_controller_weakness
		STATUE_TYPE.put(18209, 1); // r44_controller_pddown
		STATUE_TYPE.put(18210, 2); // r44_controller_poison
		STATUE_TYPE.put(18211, 3); // r44_controller_nonheal
	}

	private static final Map<Integer, Integer> STATUE_ZONE = new HashMap<>(16);
	static
	{
		STATUE_ZONE.put(18196, 60000); // r41_controller_weakness -> conquerors_weakness
		STATUE_ZONE.put(18197, 60001); // r41_controller_pddown -> conquerors_pddown
		STATUE_ZONE.put(18198, 60002); // r41_controller_poison -> conquerors_poison
		STATUE_ZONE.put(18199, 60003); // r41_controller_nonheal -> conquerors_nonheal

		STATUE_ZONE.put(18200, 60010); // r42_controller_weakness -> lords_weakness
		STATUE_ZONE.put(18201, 60011); // r42_controller_pddown -> lords_pddown
		STATUE_ZONE.put(18202, 60012); // r42_controller_poison -> lords_poison
		STATUE_ZONE.put(18203, 60013); // r42_controller_nonheal -> lords_nonheal

		STATUE_ZONE.put(18204, 60020); // r43_controller_weakness -> savants_weakness
		STATUE_ZONE.put(18205, 60021); // r43_controller_pddown -> savants_pddown
		STATUE_ZONE.put(18206, 60022); // r43_controller_poison -> savants_poison
		STATUE_ZONE.put(18207, 60023); // r43_controller_nonheal -> savants_nonheal

		STATUE_ZONE.put(18208, 60030); // r44_controller_weakness -> magistrates_weakness
		STATUE_ZONE.put(18209, 60031); // r44_controller_pddown -> magistrates_pddown
		STATUE_ZONE.put(18210, 60032); // r44_controller_poison -> magistrates_poison
		STATUE_ZONE.put(18211, 60033); // r44_controller_nonheal -> magistrates_nonheal
	}

	private static final int[] ENCHANT_WEAPON_ON_ATTACK = new int[]
	{
		18166, // r3_warrior
		18167, // r3_warrior_longatk1_h
		18168, // r3_warrior_longatk2
		18169, // r3_warrior_selfbuff

		18187, // r4_warrior
		18188, // r4_warrior_longatk1_h
		18189, // r4_warrior_longatk2
		18190, // r4_warrior_selfbuff
	};

	private static final int[] KEY_BOX_DROPLIST = new int[]
	{
		18120, // r11_roomboss_strong
		18121, // r11_roomboss_weak
		18122, // r11_roomboss_teleport
		18141, // r21_scarab_roombosss
		18158, // r31_roomboss1
		18159, // r31_roomboss2
		18173, // r41_roomboss_strong
		18174, // r41_roomboss_weak
		18175, // r41_roomboss_teleport
		18212, // r51_roomboss_clanbuff1
		18213, // r51_roomboss_clanbuff2

		18123, // r12_roomboss_strong
		18124, // r12_roomboss_weak
		18125, // r12_roomboss_teleport
		18142, // r22_scarab_roombosss
		18160, // r32_roomboss1
		18161, // r32_roomboss2
		18176, // r42_roomboss_strong
		18177, // r42_roomboss_weak
		18178, // r42_roomboss_teleport
		18214, // r52_roomboss_clanbuff1
		18215, // r52_roomboss_clanbuff2

		18126, // r13_roomboss_strong
		18127, // r13_roomboss_weak
		18128, // r13_roomboss_teleport
		18143, // r23_scarab_roombosss
		18162, // r33_roomboss1
		18163, // r33_roomboss2
		18179, // r43_roomboss_strong
		18180, // r43_roomboss_weak
		18181, // r43_roomboss_teleport
		18216, // r53_roomboss_clanbuff1
		18217, // r53_roomboss_clanbuff2

		18129, // r14_roomboss_strong
		18130, // r14_roomboss_weak
		18131, // r14_roomboss_teleport
		18144, // r24_scarab_roombosss
		18164, // r34_roomboss1
		18165, // r34_roomboss2
		18182, // r44_roomboss_strong
		18183, // r44_roomboss_weak
		18184, // r44_roomboss_teleport
		18218, // r54_roomboss_clanbuff1
		18219, // r54_roomboss_clanbuff2
	};

	// Note: when drop chance is higher then 100%, more items may drop (e.g. 151% -> 51% to drop 2 items, 49% to drop 1)
	private static final Map<Integer, Integer> SEALED_BOX_DROPLIST = new HashMap<>();
	static
	{
		SEALED_BOX_DROPLIST.put(18120, 1510000); // r11_roomboss_strong
		SEALED_BOX_DROPLIST.put(18121, 1440000); // r11_roomboss_weak
		SEALED_BOX_DROPLIST.put(18122, 1100000); // r11_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18123, 1510000); // r12_roomboss_strong
		SEALED_BOX_DROPLIST.put(18124, 1440000); // r12_roomboss_weak
		SEALED_BOX_DROPLIST.put(18125, 1100000); // r12_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18126, 1510000); // r13_roomboss_strong
		SEALED_BOX_DROPLIST.put(18127, 1440000); // r13_roomboss_weak
		SEALED_BOX_DROPLIST.put(18128, 1100000); // r13_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18129, 1510000); // r14_roomboss_strong
		SEALED_BOX_DROPLIST.put(18130, 1440000); // r14_roomboss_weak
		SEALED_BOX_DROPLIST.put(18131, 1100000); // r14_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18132, 1540000); // r1_beatle_healer
		SEALED_BOX_DROPLIST.put(18133, 1420000); // r1_scorpion_warrior
		SEALED_BOX_DROPLIST.put(18134, 1070000); // r1_warrior_longatk1_h
		SEALED_BOX_DROPLIST.put(18135, 1420000); // r1_warrior_longatk2
		SEALED_BOX_DROPLIST.put(18136, 1420000); // r1_warrior_selfbuff
		SEALED_BOX_DROPLIST.put(18137, 1060000); // r1_wizard_h
		SEALED_BOX_DROPLIST.put(18138, 1410000); // r1_wizard_clanbuff
		SEALED_BOX_DROPLIST.put(18139, 1390000); // r1_wizard_debuff
		SEALED_BOX_DROPLIST.put(18140, 1410000); // r1_wizard_selfbuff
		SEALED_BOX_DROPLIST.put(18141, 900000); // r21_scarab_roombosss
		SEALED_BOX_DROPLIST.put(18142, 900000); // r22_scarab_roombosss
		SEALED_BOX_DROPLIST.put(18143, 900000); // r23_scarab_roombosss
		SEALED_BOX_DROPLIST.put(18144, 900000); // r24_scarab_roombosss
		SEALED_BOX_DROPLIST.put(18145, 760000); // r2_wizard_clanbuff
		SEALED_BOX_DROPLIST.put(18146, 780000); // r2_warrior_longatk2
		SEALED_BOX_DROPLIST.put(18147, 730000); // r2_wizard
		SEALED_BOX_DROPLIST.put(18148, 850000); // r2_warrior
		SEALED_BOX_DROPLIST.put(18149, 750000); // r2_bomb
		SEALED_BOX_DROPLIST.put(18166, 1080000); // r3_warrior
		SEALED_BOX_DROPLIST.put(18167, 1070000); // r3_warrior_longatk1_h
		SEALED_BOX_DROPLIST.put(18168, 1100000); // r3_warrior_longatk2
		SEALED_BOX_DROPLIST.put(18169, 1060000); // r3_warrior_selfbuff
		SEALED_BOX_DROPLIST.put(18170, 1070000); // r3_wizard_h
		SEALED_BOX_DROPLIST.put(18171, 1110000); // r3_wizard_clanbuff
		SEALED_BOX_DROPLIST.put(18172, 1060000); // r3_wizard_selfbuff
		SEALED_BOX_DROPLIST.put(18173, 1170000); // r41_roomboss_strong
		SEALED_BOX_DROPLIST.put(18174, 1450000); // r41_roomboss_weak
		SEALED_BOX_DROPLIST.put(18175, 1100000); // r41_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18176, 1170000); // r42_roomboss_strong
		SEALED_BOX_DROPLIST.put(18177, 1450000); // r42_roomboss_weak
		SEALED_BOX_DROPLIST.put(18178, 1100000); // r42_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18179, 1170000); // r43_roomboss_strong
		SEALED_BOX_DROPLIST.put(18180, 1450000); // r43_roomboss_weak
		SEALED_BOX_DROPLIST.put(18181, 1100000); // r43_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18182, 1170000); // r44_roomboss_strong
		SEALED_BOX_DROPLIST.put(18183, 1450000); // r44_roomboss_weak
		SEALED_BOX_DROPLIST.put(18184, 1100000); // r44_roomboss_teleport
		SEALED_BOX_DROPLIST.put(18185, 1460000); // r4_healer_srddmagic
		SEALED_BOX_DROPLIST.put(18186, 1470000); // r4_hearler_srdebuff
		SEALED_BOX_DROPLIST.put(18187, 1420000); // r4_warrior
		SEALED_BOX_DROPLIST.put(18188, 1070000); // r4_warrior_longatk1_h
		SEALED_BOX_DROPLIST.put(18189, 1420000); // r4_warrior_longatk2
		SEALED_BOX_DROPLIST.put(18190, 1420000); // r4_warrior_selfbuff
		SEALED_BOX_DROPLIST.put(18191, 1060000); // r4_wizard_h
		SEALED_BOX_DROPLIST.put(18192, 1410000); // r4_wizard_clanbuff
		SEALED_BOX_DROPLIST.put(18193, 1390000); // r4_wizard_debuff
		SEALED_BOX_DROPLIST.put(18194, 1420000); // r4_wizard_selfbuff
		SEALED_BOX_DROPLIST.put(18195, 1080000); // r4_bomb
		SEALED_BOX_DROPLIST.put(18212, 4500000); // r51_roomboss_clanbuff1
		SEALED_BOX_DROPLIST.put(18213, 4500000); // r51_roomboss_clanbuff2
		SEALED_BOX_DROPLIST.put(18214, 4500000); // r52_roomboss_clanbuff1
		SEALED_BOX_DROPLIST.put(18215, 4500000); // r52_roomboss_clanbuff2
		SEALED_BOX_DROPLIST.put(18216, 4500000); // r53_roomboss_clanbuff1
		SEALED_BOX_DROPLIST.put(18217, 4500000); // r53_roomboss_clanbuff2
		SEALED_BOX_DROPLIST.put(18218, 4500000); // r54_roomboss_clanbuff1
		SEALED_BOX_DROPLIST.put(18219, 4500000); // r54_roomboss_clanbuff2
		SEALED_BOX_DROPLIST.put(18220, 1470000); // r5_healer1
		SEALED_BOX_DROPLIST.put(18221, 1510000); // r5_healer2
		SEALED_BOX_DROPLIST.put(18222, 1430000); // r5_warrior
		SEALED_BOX_DROPLIST.put(18223, 1070000); // r5_warrior_longatk1_h
		SEALED_BOX_DROPLIST.put(18224, 1440000); // r5_warrior_longatk2
		SEALED_BOX_DROPLIST.put(18225, 1430000); // r5_warrior_sbuff
		SEALED_BOX_DROPLIST.put(18226, 1060000); // r5_wizard_h
		SEALED_BOX_DROPLIST.put(18227, 1820000); // r5_wizard_clanbuff
		SEALED_BOX_DROPLIST.put(18228, 1360000); // r5_wizard_debuff
		SEALED_BOX_DROPLIST.put(18229, 1410000); // r5_wizard_slefbuff
		SEALED_BOX_DROPLIST.put(18230, 1580000); // r5_bomb
	}

	private static final Map<Integer, Integer> HALISHA_GOBLETS = new HashMap<>(4);
	static
	{
		HALISHA_GOBLETS.put(25339, GOBLET_OF_ALECTIA); // halisha_alectia
		HALISHA_GOBLETS.put(25342, GOBLET_OF_TISHAS); // halisha_tishas
		HALISHA_GOBLETS.put(25346, GOBLET_OF_MEKARA); // halisha_mekara
		HALISHA_GOBLETS.put(25349, GOBLET_OF_MORIGUL); // halisha_morigul
	}

	// Local containers.
	private boolean _entry;
	private final Set<Npc> _managers = ConcurrentHashMap.newKeySet();
	private final Set<String> _spawnedEvents = ConcurrentHashMap.newKeySet();
	private final Set<Npc> _spawnedNpcs = ConcurrentHashMap.newKeySet();

	public Q620_FourGoblets()
	{
		super(620, "Four Goblets");

		setItemsIds(SEALED_BOX, CHAPEL_KEY, USED_GRAVE_PASS);

		addQuestStart(NAMELESS_SPIRIT);
		addTalkId(NAMELESS_SPIRIT, GHOST_OF_WIGOTH_1, GHOST_OF_WIGOTH_2, GHOST_CHAMBERLAIN_OF_ELMOREDEN);

		addAttacked(ENCHANT_WEAPON_ON_ATTACK);
		addAttacked(VICTIM_SPAWNS.keySet());
		addAttacked(STATUE_OF_PROTECTION);
		addClanAttacked(STATUE_OF_PROTECTION);
		addCreated(SEPULCHER_MANAGERS.keySet());
		addCreated(VICTIM_SPAWNS.keySet());
		addCreated(STATUE_TYPE.keySet());
		addCreated(STATUE_OF_PROTECTION);
		addFirstTalkId(KEY_BOX);
		addFirstTalkId(MONSTER_SPAWNS.keySet());
		addMyDying(KEY_BOX_DROPLIST);
		addMyDying(SEALED_BOX_DROPLIST.keySet());
		addMyDying(VICTIM_SPAWNS.keySet());
		addMyDying(STATUE_TYPE.keySet());
		addMyDying(HALISHA_GOBLETS.keySet());
		addSeeCreature(VICTIM_SPAWNS.keySet());
		addTalkId(SEPULCHER_MANAGERS.keySet());
		addTalkId(HALL_GATEKEEPER_DOORS.keySet());

		addMakerNpcsKilledByEvent(MONSTER_NEXT_SPAWN.keySet());
	}

	@Override
	protected boolean init()
	{
		// Spawn Hall Gatekeepers.
		// 1st path - Conqueror's gatekeepers
		addSpawn(31925, 182727, -85493, -7200, 32768, false, 0, false); // conq_barons_lock
		addSpawn(31926, 184547, -85479, -7200, 32768, false, 0, false); // conq_viscounts_lock
		addSpawn(31927, 186349, -85473, -7200, 32768, false, 0, false); // conq_counts_lock
		addSpawn(31928, 188154, -85463, -7200, 32768, false, 0, false); // conq_marquis_lock
		addSpawn(31929, 189947, -85466, -7200, 32768, false, 0, false); // conq_dukes_lock

		// 2nd path - Lord's gatekeepers
		addSpawn(31930, 181030, -88868, -7200, 32768, false, 0, false); // lords_barons_lock
		addSpawn(31931, 182809, -88856, -7200, 32768, false, 0, false); // lords_viscounts_lock
		addSpawn(31932, 184626, -88859, -7200, 32768, false, 0, false); // lords_counts_lock
		addSpawn(31933, 186438, -88858, -7200, 32768, false, 0, false); // lords_marquis_lock
		addSpawn(31934, 188236, -88854, -7200, 32768, false, 0, false); // lords_dukes_lock

		// 3rd path - Savant's gatekeepers
		addSpawn(31935, 173102, -85105, -7200, 49152, false, 0, false); // sav_barons_lock
		addSpawn(31936, 173101, -83280, -7200, 49152, false, 0, false); // sav_viscounts_lock
		addSpawn(31937, 173103, -81479, -7200, 49152, false, 0, false); // sav_counts_lock
		addSpawn(31938, 173086, -79698, -7200, 49152, false, 0, false); // sav_marquis_lock
		addSpawn(31939, 173083, -77896, -7200, 49152, false, 0, false); // sav_dukes_lock

		// 4th path - Magistrate's gatekeepers
		addSpawn(31940, 175497, -81265, -7200, 49152, false, 0, false); // mag_barons_lock
		addSpawn(31941, 175495, -79468, -7200, 49152, false, 0, false); // mag_viscounts_lock
		addSpawn(31942, 175488, -77652, -7200, 49152, false, 0, false); // mag_counts_lock
		addSpawn(31943, 175489, -75856, -7200, 49152, false, 0, false); // mag_marquis_lock
		addSpawn(31944, 175478, -74049, -7200, 49152, false, 0, false); // mag_dukes_lock

		// Set the entrance condition.
		if (isStarted())
		{
			_entry = false;
		}
		else
		{
			long delta = (getStartTime() - System.currentTimeMillis()) - ENTRY_TO_START;
			if (delta > 0)
			{
				_entry = false;
				startQuestTimer("entry", null, null, delta);
			}
			else
			{
				_entry = true;
			}
		}

		return true;
	}

	@Override
	protected void onStart()
	{
		_entry = false;

		// Spawn first Mysterious Box.
		SEPULCHER_BUSY.entrySet().forEach(e ->
		{
			if (e.getValue())
			{
				spawnEventNpcs(MYSTERIOUS_BOX_SPAWNS.get(e.getKey()));
			}
		});

		startQuestTimerAtFixedRate("passed", null, null, PASSED_PERIOD);

		if (Config.DEVELOPER)
		{
			LOGGER.info(getName() + ": Four Sepulchers started.");
		}
	}

	@Override
	protected void onEnd()
	{
		_managers.forEach(m -> m.broadcastNpcShout(NpcStringId.ID_1000457));

		stop();

		startQuestTimer("entry", null, null, (getStartTime() - System.currentTimeMillis()) - ENTRY_TO_START);

		if (Config.DEVELOPER)
		{
			LOGGER.info(getName() + ": Four Sepulchers ended.");
		}
	}

	@Override
	public void stop()
	{
		cancelQuestTimers();

		for (Location loc : SEPULCHER_MANAGERS.values())
		{
			ZoneManager.getInstance().getZone(loc.getX(), loc.getY(), loc.getZ(), BossZone.class).oustAllPlayers();
		}

		for (int doorId : HALL_GATEKEEPER_DOORS.values())
		{
			DoorData.getInstance().getDoor(doorId).closeMe();
		}

		for (int zoneId : STATUE_ZONE.values())
		{
			final EffectZone zone = ZoneManager.getInstance().getZoneById(zoneId, EffectZone.class);
			if (zone != null)
			{
				zone.editStatus(false);
			}
		}

		for (String event : _spawnedEvents)
		{
			SpawnManager.getInstance().despawnEventNpcs(event, false);
		}
		_spawnedEvents.clear();

		for (Npc npc : _spawnedNpcs)
		{
			npc.deleteMe();
		}
		_spawnedNpcs.clear();

		SEPULCHER_BUSY.replaceAll((npcId, busy) -> busy = false);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getNpcId();

		// Nameless Spirit
		if (event.equalsIgnoreCase("31453-13.htm"))
		{
			st.setState(QuestStatus.STARTED);
			if (player.getInventory().hasItems(ANTIQUE_BROOCH))
			{
				st.setCond(2);
				htmltext = "31453-19.htm";
			}
			else
			{
				st.setCond(1);
			}
			playSound(player, SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31453-16.htm"))
		{
			if (player.getInventory().hasItems(GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL))
			{
				st.setCond(2);
				playSound(player, SOUND_MIDDLE);
				takeItems(player, GOBLET_OF_ALECTIA, -1);
				takeItems(player, GOBLET_OF_TISHAS, -1);
				takeItems(player, GOBLET_OF_MEKARA, -1);
				takeItems(player, GOBLET_OF_MORIGUL, -1);
				giveItems(player, ANTIQUE_BROOCH, 1);
			}
			else
			{
				return null;
			}
		}
		else if (event.equalsIgnoreCase("31453-18.htm"))
		{
			playSound(player, SOUND_GIVEUP);
			st.exitQuest(true);
		}
		// Ghost of Wigoth 1
		else if (event.equalsIgnoreCase("31452-06.htm"))
		{
			takeItems(player, CHAPEL_KEY, -1);
			player.teleportTo(169592, -91006, -2912, 0);
		}
		// Ghost of Wigoth 2
		else if (event.equalsIgnoreCase("31454-14.htm"))
		{
			if (player.getInventory().hasItems(SEALED_BOX))
			{
				takeItems(player, SEALED_BOX, 1);

				if (openSealedBox(player))
				{
					htmltext = "31454-13.htm";
				}
			}
			else
			{
				return null;
			}
		}
		else if (StringUtil.isDigit(event))
		{
			// If event is a simple digit, parse it to get an integer, then test the reward list.
			final int id = Integer.parseInt(event);
			if (ArraysUtil.contains(RCP_REWARDS, id) && player.getInventory().getItemCount(BROKEN_RELIC_PART) >= 1000)
			{
				takeItems(player, BROKEN_RELIC_PART, 1000);
				giveItems(player, id, 1);
				htmltext = "31454-17.htm";
			}
			else
			{
				return null;
			}
		}
		// Ghost Chamberlain of Elmoreden
		else if (event.equalsIgnoreCase("31919-06.htm"))
		{
			if (player.getInventory().hasItems(SEALED_BOX))
			{
				takeItems(player, SEALED_BOX, 1);

				// Note: Ghost Chamberlain of Elmoreden has 50% chance to succeed compared to Ghost of Wigoth.
				if (Rnd.nextBoolean())
				{
					htmltext = "31919-05.htm";
				}
				else if (openSealedBox(player))
				{
					htmltext = "31919-03.htm";
				}
				else
				{
					htmltext = "31919-04.htm";
				}
			}
		}
		// Sepulcher Managers
		else if (SEPULCHER_MANAGERS.containsKey(npcId) && event.equalsIgnoreCase("Enter"))
		{
			synchronized (this)
			{
				// Check Four Sepulchers entry allowed.
				if (!_entry)
				{
					return npcId + "-02.htm";
				}

				// Check party member count.
				final Party party = player.getParty();
				if (party == null || party.getMembersCount() < Config.FS_PARTY_MEMBER_COUNT)
				{
					return npcId + "-04.htm";
				}

				// Check player is party leader.
				if (!party.isLeader(player))
				{
					return npcId + "-03.htm";
				}

				// Check party members' have Entrance Pass to the Sepulcher and quest.
				for (Player member : party.getMembers())
				{
					if (!member.getInventory().hasItems(ENTRANCE_PASS_TO_THE_SEPULCHER))
					{
						return getHtmlText(player, npcId + "-05.htm").replace("%member%", member.getName());
					}

					QuestState mst = member.getQuestList().getQuestState(QUEST_NAME);
					if (mst == null || !mst.isStarted())
					{
						return getHtmlText(player, npcId + "-06.htm").replace("%member%", member.getName());
					}
				}

				// Check Sepulcher is free.
				if (SEPULCHER_BUSY.put(npcId, true))
				{
					return npcId + "-07.htm";
				}

				// Handle items and teleport party.
				for (Player member : party.getMembers())
				{
					if (!MathUtil.checkIfInRange(1000, npc, member, true))
					{
						continue;
					}

					QuestState mst = member.getQuestList().getQuestState(QUEST_NAME);
					if (mst == null)
					{
						continue;
					}

					mst.set("completed", false);

					takeItems(member, ENTRANCE_PASS_TO_THE_SEPULCHER, 1);
					if (!member.getInventory().hasItems(ANTIQUE_BROOCH))
					{
						giveItems(member, USED_GRAVE_PASS, 1);
					}
					takeItems(member, CHAPEL_KEY, -1);

					final Location loc = SEPULCHER_MANAGERS.get(npcId);
					ZoneManager.getInstance().getZone(loc.getX(), loc.getY(), loc.getZ(), BossZone.class).allowPlayerEntry(member, 30);
					member.instantTeleportTo(loc, 80);
				}

				return npcId + "-08.htm";
			}
		}

		return htmltext;
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		final int npcId = npc.getNpcId();

		if (ArraysUtil.contains(ENCHANT_WEAPON_ON_ATTACK, npcId))
		{
			if (npc.isScriptValue(0) && npc.getStatus().getHpRatio() < 0.5d)
			{
				npc.setScriptValue(1);
				npc.setEnchantEffect(15);
				npc.updateAbnormalEffect();
			}
		}
		else if (VICTIM_SPAWNS.containsKey(npcId))
		{
			if (!npc.isMoving())
			{
				npc.fleeFrom(attacker, Config.MAX_DRIFT_RANGE);
			}
		}
		else if (npcId == STATUE_OF_PROTECTION)
		{
			if (Rnd.get(100) < 33 && attacker == ((Attackable) npc).getAggroList().getMostHatedCreature())
			{
				npc.getAI().tryToCast(npc, STATUE_OF_PROTECTION_EFFECT, 1);
			}
		}
	}

	@Override
	public void onClanAttacked(Attackable caller, Attackable called, Creature attacker, int damage)
	{
		final int npcId = called.getNpcId();

		if (npcId == STATUE_OF_PROTECTION)
		{
			if (Rnd.get(100) < 33)
			{
				called.getAI().tryToCast(called, STATUE_OF_PROTECTION_EFFECT, 1);
			}
		}
	}

	@Override
	public void onCreated(Npc npc)
	{
		final int npcId = npc.getNpcId();

		// Sepulcher Manager - Register it for announcement purposes.
		if (SEPULCHER_MANAGERS.containsKey(npcId))
		{
			_managers.add(npc);
		}
		else if (VICTIM_SPAWNS.containsKey(npcId))
		{
			// Start 5min survival timer and 5s shout timer.
			startQuestTimer("survived", npc, null, 5 * 60 * 1000);
			startQuestTimerAtFixedRate("action", npc, null, 5 * 1000);

			npc.disableCoreAi(true);
			npc.forceRunStance();
		}
		// Charm of Corner - Start zone effect or schedule it.
		else if (STATUE_TYPE.containsKey(npcId))
		{
			switch (STATUE_TYPE.get(npcId))
			{
				case 0:
					npc.broadcastNpcShout(NpcStringId.ID_1010473);
					startQuestTimer("statue", npc, null, 60 * 1000);
					break;
				case 1:
					npc.broadcastNpcShout(NpcStringId.ID_1010474);
					startQuestTimer("statue", npc, null, 2 * 60 * 1000);
					break;
				case 2:
					startStatueEffect(npc);
					break;
				case 3:
					npc.broadcastNpcShout(NpcStringId.ID_1010475);
					startQuestTimer("statue", npc, null, 3 * 60 * 1000);
					break;
			}
		}
		// Statue of Protection - Petrify itself.
		else if (npcId == STATUE_OF_PROTECTION)
		{
			// Apply petrification for 6 minutes (given by skill).
			FrequentSkill.FAKE_PETRIFICATION.getSkill().getEffects(npc, npc);
			// FIXME: The petrification is not visible (dark color)
			// npc.updateAbnormalEffect();
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final int npcId = npc.getNpcId();

		// Mysterious Box - spawn monster.
		if (MONSTER_SPAWNS.containsKey(npcId))
		{
			if (isStarted() && npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				spawnEventNpcs(MONSTER_SPAWNS.get(npcId));
			}

			npc.deleteMe();
		}
		// Key Box - give the Chapel Key.
		else if (npcId == KEY_BOX)
		{
			if (isStarted() && npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				giveItems(player, CHAPEL_KEY, 1);
			}

			npc.deleteMe();
		}

		return null;
	}

	@Override
	public void onMakerNpcsKilled(NpcMaker maker, Npc npc)
	{
		final String event = maker.getEvent();

		if (MONSTER_NEXT_SPAWN.containsKey(event))
		{
			maker.deleteAll();
			_spawnedEvents.remove(event);

			spawnEventNpcs(MONSTER_NEXT_SPAWN.get(event));
		}
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		final int npcId = npc.getNpcId();
		final Player player = killer.getActingPlayer();

		// Monster spawning Key Box.
		if (ArraysUtil.contains(KEY_BOX_DROPLIST, npcId))
		{
			final Npc keyBox = addSpawn(KEY_BOX, npc, false, 0, false);
			_spawnedNpcs.add(keyBox);
		}

		// The victim is killed (3rd room).
		if (VICTIM_SPAWNS.containsKey(npcId))
		{
			cancelQuestTimers(npc);

			// Not saved, spawn room boss.
			if (npc.getScriptValue() == 0)
			{
				final Npc boss = addSpawn(VICTIM_SPAWNS.get(npcId), npc, false, 0, false);
				_spawnedNpcs.add(boss);
			}
		}

		// Monster dropping Sealed Box.
		if (SEALED_BOX_DROPLIST.containsKey(npcId))
		{
			final QuestState st = getRandomPartyMemberState(player, npc, QuestStatus.STARTED);
			if (st != null)
			{
				dropItems(st.getPlayer(), SEALED_BOX, 1, 0, SEALED_BOX_DROPLIST.get(npcId));
			}
		}

		// Charm of Corner - Stop zone effect.
		if (STATUE_TYPE.containsKey(npcId))
		{
			stopStatueEffect(npc);
		}

		// Shadow of Halisha raid boss dropping goblet.
		if (HALISHA_GOBLETS.containsKey(npcId))
		{
			final int goblet = HALISHA_GOBLETS.get(npcId);
			for (QuestState mst : getPartyMembersState(player, npc, QuestStatus.STARTED))
			{
				mst.set("completed", true);

				final Player member = mst.getPlayer();
				if (mst.getCond() == 2 || member.getInventory().hasItems(goblet))
				{
					continue;
				}

				giveItems(member, goblet, 1);
				playSound(member, SOUND_ITEMGET);
			}

			if (!isStarted())
			{
				return;
			}

			// Spawn Ghost of Wigoth (4S).
			final Npc wigoth = addSpawn(GHOST_OF_WIGOTH_1, npc, false, 0, false);
			_spawnedNpcs.add(wigoth);

			// Spawn Halisha's Treasure Boxes.
			int count = getHalishaTreasureBoxCount();
			if (count > 0 && npc.getSpawn() instanceof MultiSpawn)
			{
				String event = ((MultiSpawn) npc.getSpawn()).getNpcMaker().getEvent();
				event = HALISHA_BOX_SPAWN.get(event);
				_spawnedEvents.add(event);
				SpawnManager.getInstance().spawnEventNpcs(event, false, false, count);
				startQuestTimer(event, null, null, 5 * 60 * 1000);
			}
		}
	}

	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		final int npcId = npc.getNpcId();

		// Victim (3rd room).
		if (VICTIM_SPAWNS.containsKey(npcId))
		{
			if (creature instanceof Player)
			{
				if (npc.getScriptValue() == 0)
				{
					// TODO onSeeCreature must be called only when newly seen creature...right now it shouts both ID_1010483 and ID_1010484 at the same time
					// Note: The Victim runs onSeeCreature approx each 25-35s on L2OFF, even though all players and Victim are standing still close together
					npc.broadcastNpcSay(NpcStringId.ID_1010483, creature.getName());
				}
				else
				{
					npc.getAI().tryToCast(creature, Rnd.get(VICTIM_BUFFS), 1);
				}

				npc.getAI().tryToFollow(creature, false);
			}
			else if (creature instanceof Attackable)
			{
				Attackable attacker = (Attackable) creature;
				if (Rnd.get(100) < 80 && ArraysUtil.contains(VICTIM_ATTACKERS, attacker.getNpcId()))
				{
					if (Rnd.get(100) < 80)
					{
						attacker.getAggroList().addDamageHate(npc, 0, 300);
					}
					else
					{
						attacker.getAggroList().clear();
						attacker.getAggroList().addDamageHate(npc, 0, 1000);
					}
				}
			}
		}
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		String htmltext = getNoQuestMsg();
		if (st == null)
		{
			return htmltext;
		}

		switch (st.getState())
		{
			case CREATED:
				htmltext = (player.getStatus().getLevel() >= 74) ? "31453-01.htm" : "31453-12.htm";
				break;

			case STARTED:
				final int npcId = npc.getNpcId();
				final int cond = st.getCond();
				switch (npcId)
				{
					case NAMELESS_SPIRIT:
						if (cond == 1)
						{
							htmltext = player.getInventory().hasItems(GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL) ? "31453-15.htm" : "31453-14.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31453-17.htm";
						}
						break;

					case GHOST_OF_WIGOTH_1:
						if (cond == 1)
						{
							if (player.getInventory().hasItems(GOBLET_OF_ALECTIA, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL, GOBLET_OF_TISHAS))
							{
								htmltext = "31452-04.htm";
							}
							else if (Math.min(1, player.getInventory().getItemCount(GOBLET_OF_ALECTIA)) + Math.min(1, player.getInventory().getItemCount(GOBLET_OF_MEKARA)) + Math.min(1, player.getInventory().getItemCount(GOBLET_OF_MORIGUL)) + Math.min(1, player.getInventory().getItemCount(GOBLET_OF_TISHAS)) == 3)
							{
								htmltext = "31452-02.htm";
							}
							else
							{
								htmltext = "31452-01.htm";
							}
						}
						if (cond == 2)
						{
							htmltext = "31452-05.htm";
						}
						break;

					case GHOST_OF_WIGOTH_2:
						if (st.getBool("completed", false))
						{
							// Get base option depending on Antique Brooch and Goblets.
							int index = 5;
							if (cond == 2)
							{
								index = 9;
							}
							else if (player.getInventory().hasItems(GOBLET_OF_ALECTIA, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL, GOBLET_OF_TISHAS))
							{
								index = 1;
							}

							// Get specific option depending Sealed Boxes and Broken Relic Parts.
							if (player.getInventory().hasItems(SEALED_BOX))
							{
								index += 1;
							}
							if (player.getInventory().getItemCount(BROKEN_RELIC_PART) >= 1000)
							{
								index += 2;
							}

							htmltext = String.format("31454-%02d.htm", index);
						}
						break;

					case GHOST_CHAMBERLAIN_OF_ELMOREDEN:
						htmltext = "31919-01.htm";
						break;

					default:
						if (SEPULCHER_MANAGERS.containsKey(npcId))
						{
							htmltext = npcId + "-01.htm";
						}
						else if (HALL_GATEKEEPER_DOORS.containsKey(npcId))
						{
							if (!isStarted())
							{
								htmltext = null;
							}
							else if (player.getInventory().hasItems(CHAPEL_KEY))
							{
								// Take Chapel Key.
								takeItems(player, CHAPEL_KEY, 1);

								// Open door and schedule close in 15s.
								final int doorId = HALL_GATEKEEPER_DOORS.get(npcId);
								final Door door = DoorData.getInstance().getDoor(doorId);
								door.openMe();
								startQuestTimer("close", npc, null, 15000);

								// Spawn next Mysterious Box or Halisha raid boss.
								spawnEventNpcs(MYSTERIOUS_BOX_SPAWNS.get(npcId));

								// Spawn 3 monsters (bomb, debuff, heal) to enemy paths randomly and shout.
								HallGatekeeperSpawn hgs = HALL_GATEKEEPER_MONSTER_SPAWNS.get(npcId);
								int index = Rnd.get(3);
								for (int monsterId : HALL_GATEKEEPER_MONSTERS)
								{
									Npc monster = addSpawn(monsterId, hgs.getLocation(index), false, 0, false);
									_spawnedNpcs.add(monster);

									index++;
									if (index == 3)
									{
										index = 0;
									}
								}
								npc.broadcastNpcShout(NpcStringId.ID_1000502);

								htmltext = null;
							}
							else
							{
								htmltext = npcId + "-01.htm";
							}
						}
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		// Sepulcher managers - entrance is allowed.
		if (name.equalsIgnoreCase("entry"))
		{
			_managers.forEach(m -> m.broadcastNpcShout(NpcStringId.ID_1000500));
			_managers.forEach(m -> m.broadcastNpcShout(NpcStringId.ID_1000501));

			_entry = true;
			return null;
		}
		// Sepulcher managers - information about passed time.
		else if (name.equalsIgnoreCase("passed"))
		{
			if (isStarted())
			{
				long passed = (System.currentTimeMillis() - getStartTime() + 1000) / 60000;
				String message = String.format("%s %d %s", NpcStringId.ID_1000455.getMessage(), passed, NpcStringId.ID_1000456.getMessage());
				_managers.forEach(m -> m.broadcastNpcShout(message));
			}
			return null;
		}
		// Halisha's Treasure Boxes - despawn all spawned.
		else if (HALISHA_BOX_SPAWN.containsValue(name))
		{
			SpawnManager.getInstance().despawnEventNpcs(name, false);
			_spawnedEvents.remove(name);
			return null;
		}

		final int npcId = npc.getNpcId();

		// Hall Gatekeepers - automatically close the door.
		if (HALL_GATEKEEPER_DOORS.containsKey(npcId) && name.equalsIgnoreCase("close"))
		{
			final int doorId = HALL_GATEKEEPER_DOORS.get(npcId);
			final Door door = DoorData.getInstance().getDoor(doorId);
			door.closeMe();
		}
		// Victim (3rd room).
		else if (VICTIM_SPAWNS.containsKey(npcId))
		{
			if (npc.isDead())
			{
				cancelQuestTimers(npc);
				return null;
			}

			// Action.
			if (name.equalsIgnoreCase("action"))
			{
				if (npc.getScriptValue() == 0)
				{
					npc.broadcastNpcSay(NpcStringId.ID_1010484);
				}

				npc.lookNeighbor(300);
			}
			// Survived.
			else if (name.equalsIgnoreCase("survived"))
			{
				// Saved, spawn Key Box.
				final Npc keyBox = addSpawn(KEY_BOX, npc, false, 0, false);
				_spawnedNpcs.add(keyBox);

				npc.broadcastNpcSay(NpcStringId.ID_1000503);
				npc.setScriptValue(1);
			}
		}
		// Charm of Corner - Start zone effect.
		else if (STATUE_TYPE.containsKey(npcId) && name.equalsIgnoreCase("statue"))
		{
			if (npc.isScriptValue(1))
			{
				npc.broadcastPacket(new MagicSkillUse(npc, CHARM_OF_CORNER_EFFECT, 1, 500, 1000));
			}
			else
			{
				startStatueEffect(npc);
			}
		}

		return null;
	}

	/**
	 * Spawns event NPCs by given event name.<br>
	 * Replaces the random pattern in event with real number. E.g. #3 -> 1, 2 or 3.
	 * @param event : Event name.
	 */
	private final void spawnEventNpcs(String event)
	{
		// Build event name (generate random number, when required).
		int pos = event.indexOf("#");
		if (pos >= 0)
		{
			int rnd = Integer.parseInt(event.substring(pos + 1, pos + 2));
			event = event.replace("#" + rnd, "" + (Rnd.get(rnd) + 1));
		}

		// Save the event name and spawn it.
		_spawnedEvents.add(event);
		SpawnManager.getInstance().spawnEventNpcs(event, true, false);
	}

	/**
	 * Start a statue effect over the area.
	 * @param npc : Npc to start the effect.
	 */
	private final void startStatueEffect(Npc npc)
	{
		final int npcId = npc.getNpcId();

		// Notify.
		switch (STATUE_TYPE.get(npcId))
		{
			case 0:
				npc.broadcastNpcShout(NpcStringId.ID_1010476);
				break;
			case 1:
				npc.broadcastNpcShout(NpcStringId.ID_1010477);
				break;
			case 2:
				npc.broadcastNpcShout(NpcStringId.ID_1010472);
				break;
			case 3:
				npc.broadcastNpcShout(NpcStringId.ID_1010478);
				break;
		}

		// Start effect of the zone.
		final EffectZone zone = ZoneManager.getInstance().getZoneById(STATUE_ZONE.get(npcId), EffectZone.class);
		if (zone != null)
		{
			zone.editStatus(true);
		}

		// Add visual effect to the statue and start task to keep it.
		npc.broadcastPacket(new MagicSkillUse(npc, CHARM_OF_CORNER_EFFECT, 1, 500, 1000));
		startQuestTimerAtFixedRate("statue", npc, null, 3 * 1000);
		npc.setScriptValue(1);
	}

	/**
	 * Stop a statue effect over the area.
	 * @param npc : Npc to stop the effect.
	 */
	private final void stopStatueEffect(Npc npc)
	{
		final int npcId = npc.getNpcId();

		// Notify.
		switch (STATUE_TYPE.get(npcId))
		{
			case 0:
				npc.broadcastNpcShout(NpcStringId.ID_1010480);
				break;
			case 1:
				npc.broadcastNpcShout(NpcStringId.ID_1010481);
				break;
			case 2:
				npc.broadcastNpcShout(NpcStringId.ID_1010479);
				break;
			case 3:
				npc.broadcastNpcShout(NpcStringId.ID_1010482);
				break;
		}

		// Stop effect of the zone.
		final EffectZone zone = ZoneManager.getInstance().getZoneById(STATUE_ZONE.get(npcId), EffectZone.class);
		if (zone != null)
		{
			zone.editStatus(false);
		}

		// Cancel statue visual of the statue of task keeping it.
		cancelQuestTimer("statue", npc, null);
	}

	/**
	 * @return The amount of Halisha Treasure Box to be spawned. Increases with speed of Four Sepulcher completion.
	 */
	private int getHalishaTreasureBoxCount()
	{
		// Get elapsed time.
		long minutes = System.currentTimeMillis() - getStartTime();
		minutes /= 60 * 1000;

		// Calculate amount.
		int random = Rnd.get(10);
		if (minutes < 12)
		{
			return 157 + random;
		}
		else if (minutes < 14)
		{
			return 145 + random;
		}
		else if (minutes < 16)
		{
			return 134 + random;
		}
		else if (minutes < 18)
		{
			return 123 + random;
		}
		else if (minutes < 22)
		{
			return 113 + random;
		}
		else if (minutes < 24)
		{
			return 103 + random;
		}
		else if (minutes < 26)
		{
			return 94 + random;
		}
		else if (minutes < 28)
		{
			return 85 + random;
		}
		else if (minutes < 30)
		{
			return 76 + random;
		}
		else if (minutes < 32)
		{
			return 68 + random;
		}
		else if (minutes < 34)
		{
			return 60 + random;
		}
		else if (minutes < 36)
		{
			return 52 + random;
		}
		else if (minutes < 38)
		{
			return 45 + random;
		}
		else if (minutes < 40)
		{
			return 39 + random;
		}
		else if (minutes < 42)
		{
			return 32 + random;
		}
		else if (minutes < 44)
		{
			return 26 + random;
		}
		else if (minutes < 46)
		{
			return 20 + random;
		}
		else if (minutes < 48)
		{
			return 15 + random;
		}
		else if (minutes < 50)
		{
			return 10 + random;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * Gives reward to a {@link Player}.
	 * @param player : The {@link Player} to be rewarded.
	 * @return boolean : True, when there was a reward.
	 */
	private static final boolean openSealedBox(Player player)
	{
		boolean result = false;
		int group = Rnd.get(5);

		if (group == 0)
		{
			result = true;

			giveItems(player, 57, 10000);
		}
		else if (group == 1)
		{
			if (Rnd.get(1000) < 848)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 43)
				{
					giveItems(player, 1884, 42);
				}
				else if (i < 66)
				{
					giveItems(player, 1895, 36);
				}
				else if (i < 184)
				{
					giveItems(player, 1876, 4);
				}
				else if (i < 250)
				{
					giveItems(player, 1881, 6);
				}
				else if (i < 287)
				{
					giveItems(player, 5549, 8);
				}
				else if (i < 484)
				{
					giveItems(player, 1874, 1);
				}
				else if (i < 681)
				{
					giveItems(player, 1889, 1);
				}
				else if (i < 799)
				{
					giveItems(player, 1877, 1);
				}
				else if (i < 902)
				{
					giveItems(player, 1894, 1);
				}
				else
				{
					giveItems(player, 4043, 1);
				}
			}

			if (Rnd.get(1000) < 323)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 335)
				{
					giveItems(player, 1888, 1);
				}
				else if (i < 556)
				{
					giveItems(player, 4040, 1);
				}
				else if (i < 725)
				{
					giveItems(player, 1890, 1);
				}
				else if (i < 872)
				{
					giveItems(player, 5550, 1);
				}
				else if (i < 962)
				{
					giveItems(player, 1893, 1);
				}
				else if (i < 986)
				{
					giveItems(player, 4046, 1);
				}
				else
				{
					giveItems(player, 4048, 1);
				}
			}
		}
		else if (group == 2)
		{
			if (Rnd.get(1000) < 847)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 148)
				{
					giveItems(player, 1878, 8);
				}
				else if (i < 175)
				{
					giveItems(player, 1882, 24);
				}
				else if (i < 273)
				{
					giveItems(player, 1879, 4);
				}
				else if (i < 322)
				{
					giveItems(player, 1880, 6);
				}
				else if (i < 357)
				{
					giveItems(player, 1885, 6);
				}
				else if (i < 554)
				{
					giveItems(player, 1875, 1);
				}
				else if (i < 685)
				{
					giveItems(player, 1883, 1);
				}
				else if (i < 803)
				{
					giveItems(player, 5220, 1);
				}
				else if (i < 901)
				{
					giveItems(player, 4039, 1);
				}
				else
				{
					giveItems(player, 4044, 1);
				}
			}

			if (Rnd.get(1000) < 251)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 350)
				{
					giveItems(player, 1887, 1);
				}
				else if (i < 587)
				{
					giveItems(player, 4042, 1);
				}
				else if (i < 798)
				{
					giveItems(player, 1886, 1);
				}
				else if (i < 922)
				{
					giveItems(player, 4041, 1);
				}
				else if (i < 966)
				{
					giveItems(player, 1892, 1);
				}
				else if (i < 996)
				{
					giveItems(player, 1891, 1);
				}
				else
				{
					giveItems(player, 4047, 1);
				}
			}
		}
		else if (group == 3)
		{
			if (Rnd.get(1000) < 31)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 223)
				{
					giveItems(player, 730, 1);
				}
				else if (i < 893)
				{
					giveItems(player, 948, 1);
				}
				else
				{
					giveItems(player, 960, 1);
				}
			}

			if (Rnd.get(1000) < 5)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 202)
				{
					giveItems(player, 729, 1);
				}
				else if (i < 928)
				{
					giveItems(player, 947, 1);
				}
				else
				{
					giveItems(player, 959, 1);
				}
			}
		}
		else if (group == 4)
		{
			if (Rnd.get(1000) < 329)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 88)
				{
					giveItems(player, 6698, 1);
				}
				else if (i < 185)
				{
					giveItems(player, 6699, 1);
				}
				else if (i < 238)
				{
					giveItems(player, 6700, 1);
				}
				else if (i < 262)
				{
					giveItems(player, 6701, 1);
				}
				else if (i < 292)
				{
					giveItems(player, 6702, 1);
				}
				else if (i < 356)
				{
					giveItems(player, 6703, 1);
				}
				else if (i < 420)
				{
					giveItems(player, 6704, 1);
				}
				else if (i < 482)
				{
					giveItems(player, 6705, 1);
				}
				else if (i < 554)
				{
					giveItems(player, 6706, 1);
				}
				else if (i < 576)
				{
					giveItems(player, 6707, 1);
				}
				else if (i < 640)
				{
					giveItems(player, 6708, 1);
				}
				else if (i < 704)
				{
					giveItems(player, 6709, 1);
				}
				else if (i < 777)
				{
					giveItems(player, 6710, 1);
				}
				else if (i < 799)
				{
					giveItems(player, 6711, 1);
				}
				else if (i < 863)
				{
					giveItems(player, 6712, 1);
				}
				else if (i < 927)
				{
					giveItems(player, 6713, 1);
				}
				else
				{
					giveItems(player, 6714, 1);
				}
			}

			if (Rnd.get(1000) < 54)
			{
				result = true;
				int i = Rnd.get(1000);

				if (i < 100)
				{
					giveItems(player, 6688, 1);
				}
				else if (i < 198)
				{
					giveItems(player, 6689, 1);
				}
				else if (i < 298)
				{
					giveItems(player, 6690, 1);
				}
				else if (i < 398)
				{
					giveItems(player, 6691, 1);
				}
				else if (i < 499)
				{
					giveItems(player, 7579, 1);
				}
				else if (i < 601)
				{
					giveItems(player, 6693, 1);
				}
				else if (i < 703)
				{
					giveItems(player, 6694, 1);
				}
				else if (i < 801)
				{
					giveItems(player, 6695, 1);
				}
				else if (i < 902)
				{
					giveItems(player, 6696, 1);
				}
				else
				{
					giveItems(player, 6697, 1);
				}
			}
		}

		return result;
	}

	/**
	 * Contains information for Hall Gatekeeper to spawn monster in other Four Sepulcher rooms.
	 */
	private static class HallGatekeeperSpawn
	{
		private final SpawnLocation[] _locs = new SpawnLocation[3];

		private HallGatekeeperSpawn(int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3)
		{
			_locs[0] = new SpawnLocation(x1, y1, z1, -1);
			_locs[1] = new SpawnLocation(x2, y2, z2, -1);
			_locs[2] = new SpawnLocation(x3, y3, z3, -1);
		}

		private SpawnLocation getLocation(int index)
		{
			return _locs[index];
		}
	}
}