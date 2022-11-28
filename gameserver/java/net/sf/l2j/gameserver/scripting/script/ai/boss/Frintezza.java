package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.enums.skills.FlyType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.group.CommandChannel;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceled;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Frintezza extends AttackableAIScript
{
	private static BossZone FRINTEZZA_LAIR = ZoneManager.getInstance().getZoneById(110012, BossZone.class);

	private static final int[][] INVADE_LOC = new int[][]
	{
		{
			174102,
			-76039,
			-5105
		},
		{
			173235,
			-76884,
			-5105
		},
		{
			175003,
			-76933,
			-5105
		},
		{
			174196,
			-76190,
			-5105
		},
		{
			174013,
			-76120,
			-5105
		},
		{
			173263,
			-75161,
			-5105
		}
	};

	private static final int[][] SKILL = new int[][]
	{
		{
			5015,
			1,
			5000
		},
		{
			5015,
			4,
			5000
		},
		{
			5015,
			2,
			5000
		},
		{
			5015,
			5,
			5000
		},
		{
			5018,
			1,
			10000
		},
		{
			5016,
			1,
			5000
		},
		{
			5015,
			3,
			5000
		},
		{
			5015,
			6,
			5000
		},
		{
			5018,
			2,
			10000
		},
		{
			5019,
			1,
			10000
		},
		{
			5016,
			1,
			5000
		}
	};

	private static final int[][] MOB_LOCS = new int[][]
	{
		{
			18328,
			172894,
			-76019,
			-5107,
			243
		},
		{
			18328,
			174095,
			-77279,
			-5107,
			16216
		},
		{
			18328,
			174111,
			-74833,
			-5107,
			49043
		},
		{
			18328,
			175344,
			-76042,
			-5107,
			32847
		},
		{
			18330,
			173489,
			-76227,
			-5134,
			63565
		},
		{
			18330,
			173498,
			-75724,
			-5107,
			58498
		},
		{
			18330,
			174365,
			-76745,
			-5107,
			22424
		},
		{
			18330,
			174570,
			-75584,
			-5107,
			31968
		},
		{
			18330,
			174613,
			-76179,
			-5107,
			31471
		},
		{
			18332,
			173620,
			-75981,
			-5107,
			4588
		},
		{
			18332,
			173630,
			-76340,
			-5107,
			62454
		},
		{
			18332,
			173755,
			-75613,
			-5107,
			57892
		},
		{
			18332,
			173823,
			-76688,
			-5107,
			2411
		},
		{
			18332,
			174000,
			-75411,
			-5107,
			54718
		},
		{
			18332,
			174487,
			-75555,
			-5107,
			33861
		},
		{
			18332,
			174517,
			-76471,
			-5107,
			21893
		},
		{
			18332,
			174576,
			-76122,
			-5107,
			31176
		},
		{
			18332,
			174600,
			-75841,
			-5134,
			35927
		},
		{
			18329,
			173481,
			-76043,
			-5107,
			61312
		},
		{
			18329,
			173539,
			-75678,
			-5107,
			59524
		},
		{
			18329,
			173584,
			-76386,
			-5107,
			3041
		},
		{
			18329,
			173773,
			-75420,
			-5107,
			51115
		},
		{
			18329,
			173777,
			-76650,
			-5107,
			12588
		},
		{
			18329,
			174585,
			-76510,
			-5107,
			21704
		},
		{
			18329,
			174623,
			-75571,
			-5107,
			40141
		},
		{
			18329,
			174744,
			-76240,
			-5107,
			29202
		},
		{
			18329,
			174769,
			-75895,
			-5107,
			29572
		},
		{
			18333,
			173861,
			-76011,
			-5107,
			383
		},
		{
			18333,
			173872,
			-76461,
			-5107,
			8041
		},
		{
			18333,
			173898,
			-75668,
			-5107,
			51856
		},
		{
			18333,
			174422,
			-75689,
			-5107,
			42878
		},
		{
			18333,
			174460,
			-76355,
			-5107,
			27311
		},
		{
			18333,
			174483,
			-76041,
			-5107,
			30947
		},
		{
			18331,
			173515,
			-76184,
			-5107,
			6971
		},
		{
			18331,
			173516,
			-75790,
			-5134,
			3142
		},
		{
			18331,
			173696,
			-76675,
			-5107,
			6757
		},
		{
			18331,
			173766,
			-75502,
			-5134,
			60827
		},
		{
			18331,
			174473,
			-75321,
			-5107,
			37147
		},
		{
			18331,
			174493,
			-76505,
			-5107,
			34503
		},
		{
			18331,
			174568,
			-75654,
			-5134,
			41661
		},
		{
			18331,
			174584,
			-76263,
			-5107,
			31729
		},
		{
			18339,
			173892,
			-81592,
			-5123,
			50849
		},
		{
			18339,
			173958,
			-81820,
			-5123,
			7459
		},
		{
			18339,
			174128,
			-81805,
			-5150,
			21495
		},
		{
			18339,
			174245,
			-81566,
			-5123,
			41760
		},
		{
			18334,
			173264,
			-81529,
			-5072,
			1646
		},
		{
			18334,
			173265,
			-81656,
			-5072,
			441
		},
		{
			18334,
			173267,
			-81889,
			-5072,
			0
		},
		{
			18334,
			173271,
			-82015,
			-5072,
			65382
		},
		{
			18334,
			174867,
			-81655,
			-5073,
			32537
		},
		{
			18334,
			174868,
			-81890,
			-5073,
			32768
		},
		{
			18334,
			174869,
			-81485,
			-5073,
			32315
		},
		{
			18334,
			174871,
			-82017,
			-5073,
			33007
		},
		{
			18335,
			173074,
			-80817,
			-5107,
			8353
		},
		{
			18335,
			173128,
			-82702,
			-5107,
			5345
		},
		{
			18335,
			173181,
			-82544,
			-5107,
			65135
		},
		{
			18335,
			173191,
			-80981,
			-5107,
			6947
		},
		{
			18335,
			174859,
			-80889,
			-5134,
			24103
		},
		{
			18335,
			174924,
			-82666,
			-5107,
			38710
		},
		{
			18335,
			174947,
			-80733,
			-5107,
			22449
		},
		{
			18335,
			175096,
			-82724,
			-5107,
			42205
		},
		{
			18336,
			173435,
			-80512,
			-5107,
			65215
		},
		{
			18336,
			173440,
			-82948,
			-5107,
			417
		},
		{
			18336,
			173443,
			-83120,
			-5107,
			1094
		},
		{
			18336,
			173463,
			-83064,
			-5107,
			286
		},
		{
			18336,
			173465,
			-80453,
			-5107,
			174
		},
		{
			18336,
			173465,
			-83006,
			-5107,
			2604
		},
		{
			18336,
			173468,
			-82889,
			-5107,
			316
		},
		{
			18336,
			173469,
			-80570,
			-5107,
			65353
		},
		{
			18336,
			173469,
			-80628,
			-5107,
			166
		},
		{
			18336,
			173492,
			-83121,
			-5107,
			394
		},
		{
			18336,
			173493,
			-80683,
			-5107,
			0
		},
		{
			18336,
			173497,
			-80510,
			-5134,
			417
		},
		{
			18336,
			173499,
			-82947,
			-5107,
			0
		},
		{
			18336,
			173521,
			-83063,
			-5107,
			316
		},
		{
			18336,
			173523,
			-82889,
			-5107,
			128
		},
		{
			18336,
			173524,
			-80627,
			-5134,
			65027
		},
		{
			18336,
			173524,
			-83007,
			-5107,
			0
		},
		{
			18336,
			173526,
			-80452,
			-5107,
			64735
		},
		{
			18336,
			173527,
			-80569,
			-5134,
			65062
		},
		{
			18336,
			174602,
			-83122,
			-5107,
			33104
		},
		{
			18336,
			174604,
			-82949,
			-5107,
			33184
		},
		{
			18336,
			174609,
			-80514,
			-5107,
			33234
		},
		{
			18336,
			174609,
			-80684,
			-5107,
			32851
		},
		{
			18336,
			174629,
			-80627,
			-5107,
			33346
		},
		{
			18336,
			174632,
			-80570,
			-5107,
			32896
		},
		{
			18336,
			174632,
			-83066,
			-5107,
			32768
		},
		{
			18336,
			174635,
			-82893,
			-5107,
			33594
		},
		{
			18336,
			174636,
			-80456,
			-5107,
			32065
		},
		{
			18336,
			174639,
			-83008,
			-5107,
			33057
		},
		{
			18336,
			174660,
			-80512,
			-5107,
			33057
		},
		{
			18336,
			174661,
			-83121,
			-5107,
			32768
		},
		{
			18336,
			174663,
			-82948,
			-5107,
			32768
		},
		{
			18336,
			174664,
			-80685,
			-5107,
			32676
		},
		{
			18336,
			174687,
			-83008,
			-5107,
			32520
		},
		{
			18336,
			174691,
			-83066,
			-5107,
			32961
		},
		{
			18336,
			174692,
			-80455,
			-5107,
			33202
		},
		{
			18336,
			174692,
			-80571,
			-5107,
			32768
		},
		{
			18336,
			174693,
			-80630,
			-5107,
			32994
		},
		{
			18336,
			174693,
			-82889,
			-5107,
			32622
		},
		{
			18337,
			172837,
			-82382,
			-5107,
			58363
		},
		{
			18337,
			172867,
			-81123,
			-5107,
			64055
		},
		{
			18337,
			172883,
			-82495,
			-5107,
			64764
		},
		{
			18337,
			172916,
			-81033,
			-5107,
			7099
		},
		{
			18337,
			172940,
			-82325,
			-5107,
			58998
		},
		{
			18337,
			172946,
			-82435,
			-5107,
			58038
		},
		{
			18337,
			172971,
			-81198,
			-5107,
			14768
		},
		{
			18337,
			172992,
			-81091,
			-5107,
			9438
		},
		{
			18337,
			173032,
			-82365,
			-5107,
			59041
		},
		{
			18337,
			173064,
			-81125,
			-5107,
			5827
		},
		{
			18337,
			175014,
			-81173,
			-5107,
			26398
		},
		{
			18337,
			175061,
			-82374,
			-5107,
			43290
		},
		{
			18337,
			175096,
			-81080,
			-5107,
			24719
		},
		{
			18337,
			175169,
			-82453,
			-5107,
			37672
		},
		{
			18337,
			175172,
			-80972,
			-5107,
			32315
		},
		{
			18337,
			175174,
			-82328,
			-5107,
			41760
		},
		{
			18337,
			175197,
			-81157,
			-5107,
			27617
		},
		{
			18337,
			175245,
			-82547,
			-5107,
			40275
		},
		{
			18337,
			175249,
			-81075,
			-5107,
			28435
		},
		{
			18337,
			175292,
			-82432,
			-5107,
			42225
		},
		{
			18338,
			173014,
			-82628,
			-5107,
			11874
		},
		{
			18338,
			173033,
			-80920,
			-5107,
			10425
		},
		{
			18338,
			173095,
			-82520,
			-5107,
			49152
		},
		{
			18338,
			173115,
			-80986,
			-5107,
			9611
		},
		{
			18338,
			173144,
			-80894,
			-5107,
			5345
		},
		{
			18338,
			173147,
			-82602,
			-5107,
			51316
		},
		{
			18338,
			174912,
			-80825,
			-5107,
			24270
		},
		{
			18338,
			174935,
			-80899,
			-5107,
			18061
		},
		{
			18338,
			175016,
			-82697,
			-5107,
			39533
		},
		{
			18338,
			175041,
			-80834,
			-5107,
			25420
		},
		{
			18338,
			175071,
			-82549,
			-5107,
			39163
		},
		{
			18338,
			175154,
			-82619,
			-5107,
			36345
		}
	};

	private static final int FRINTEZZA = 29045;
	private static final int SCARLET1 = 29046;
	private static final int SCARLET2 = 29047;
	private static final int TELEPORTATION_CUBIC_LOC = 29061;
	private static final int EVIL_SPIRIT = 29048;
	private static final int EVIL_SPIRIT_2 = 29049;
	private static final int DUMMY = 29052;
	private static final int SCARLET_DUMMY = 29053;
	private static final int GUIDE = 32011;

	private static final int FRINTEZZA_SCROLL = 8073; // Frintezza's Magic Force Field Removal Scroll.
	private static final int DEWDROP_OF_DESTRUCTION = 8556; // Dewdrop of Destruction

	public static final byte DORMANT = 0;
	public static final byte WAITING = 1;
	public static final byte FIGHTING = 2;
	public static final byte DEAD = 3;

	private static long _timeTracker = 0L;
	private static int _angle = 0;
	private static int _heading = 0;
	private static int _locCycle = 0;
	private static int _bomber = 0;
	private static int _checkDie = 0;
	private static int _onCheck = 0;
	private static int _onSong = 0;
	private static int _abnormal = 0;

	private static int _onMorph = 0;
	private static int _scarlet_x = 0;
	private static int _scarlet_y = 0;
	private static int _scarlet_z = 0;
	private static int _scarlet_h = 0;
	private static int _weakScarlet_x = 0;
	private static int _weakScarlet_y = 0;
	private static int _weakScarlet_z = 0;
	private static int _secondMorph = 0;
	private static int _thirdMorph = 0;

	private static int _killHallAlarmDevice = 0;
	private static int _killDarkChoirPlayer = 0;
	private static int _killDarkChoirCaptain = 0;
	private static int _soulBreakArrowUse = 0;

	private GrandBoss _frintezza, _weakScarlet, _strongScarlet, _activeScarlet;
	private Monster _demon1, _demon2, _demon3, _demon4, _portrait1, _portrait2, _portrait3, _portrait4;
	private Npc _frintezzaDummy, _overheadDummy, _portraitDummy1, _portraitDummy3, _scarletDummy;

	private static List<Player> _playersInside = new CopyOnWriteArrayList<>();
	private static List<Attackable> _minions = new CopyOnWriteArrayList<>();

	private Set<Npc> _roomMobs1 = ConcurrentHashMap.newKeySet();
	private Set<Npc> _roomMobs2 = ConcurrentHashMap.newKeySet();

	public Frintezza()
	{
		super("ai/boss");

		StatSet info = GrandBossManager.getInstance().getStatSet(FRINTEZZA);
		int status = GrandBossManager.getInstance().getBossStatus(FRINTEZZA);

		if (status == DEAD)
		{
			long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
			if (temp > 0)
			{
				startQuestTimer("frintezza_unlock", null, null, temp);
			}
			else
			{
				GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
			}
		}
		else if (status != DORMANT)
		{
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
		}
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(SCARLET1, SCARLET2, FRINTEZZA, 18328, 18329, 18330, 18331, 18332, 18333, 18334, 18335, 18336, 18337, 18338, 18339, EVIL_SPIRIT, EVIL_SPIRIT_2, 29050, 29051);
		addMyDying(SCARLET1, SCARLET2, FRINTEZZA, 18328, 18329, 18330, 18331, 18332, 18333, 18334, 18335, 18336, 18337, 18338, 18339, EVIL_SPIRIT, EVIL_SPIRIT_2, 29050, 29051);
		addSeeSpell(FRINTEZZA, EVIL_SPIRIT, EVIL_SPIRIT_2);
		addQuestStart(GUIDE, TELEPORTATION_CUBIC_LOC);
		addTalkId(GUIDE, TELEPORTATION_CUBIC_LOC);
		addZoneExit(110012);
	}

	@Override
	public void onZoneExit(Creature character, ZoneType zone)
	{
		if (character instanceof Player)
		{
			final Player cha = (Player) character;
			cha.stopAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
			cha.stopAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
			cha.enableAllSkills();
			cha.setIsImmobilized(false);
			cha.setIsParalyzed(false);
		}
		super.onZoneExit(character, zone);
	}

	@Override
	public String onTimer(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("waiting"))
		{
			startQuestTimer("close", null, null, 27000L);
			startQuestTimer("camera_1", null, null, 30000L);
			FRINTEZZA_LAIR.broadcastPacket(new Earthquake(new Location(174232, -88020, -5116), 45, 27));
		}
		else if (event.equalsIgnoreCase("room1_spawn"))
		{
			CreatureSay cs = new CreatureSay(0, SayType.SHOUT, "Hall Alarm Device", "Intruders! Sound the alarm!");
			FRINTEZZA_LAIR.broadcastPacket(cs);
			for (int i = 0; i <= 17; i++)
			{
				_roomMobs1.add(addSpawn(MOB_LOCS[i][0], MOB_LOCS[i][1], MOB_LOCS[i][2], MOB_LOCS[i][3], MOB_LOCS[i][4], false, 0L, false));
			}
		}
		else if (event.equalsIgnoreCase("room1_spawn2"))
		{
			for (int i = 18; i <= 26; i++)
			{
				_roomMobs1.add(addSpawn(MOB_LOCS[i][0], MOB_LOCS[i][1], MOB_LOCS[i][2], MOB_LOCS[i][3], MOB_LOCS[i][4], false, 0L, false));
			}
		}
		else if (event.equalsIgnoreCase("room1_spawn3"))
		{
			for (int i = 27; i <= 32; i++)
			{
				_roomMobs1.add(addSpawn(MOB_LOCS[i][0], MOB_LOCS[i][1], MOB_LOCS[i][2], MOB_LOCS[i][3], MOB_LOCS[i][4], false, 0L, false));
			}
		}
		else if (event.equalsIgnoreCase("room1_spawn4"))
		{
			for (int i = 33; i <= 40; i++)
			{
				_roomMobs1.add(addSpawn(MOB_LOCS[i][0], MOB_LOCS[i][1], MOB_LOCS[i][2], MOB_LOCS[i][3], MOB_LOCS[i][4], false, 0L, false));
			}
		}
		else if (event.equalsIgnoreCase("room2_spawn"))
		{
			for (int i = 41; i <= 44; i++)
			{
				_roomMobs2.add(addSpawn(MOB_LOCS[i][0], MOB_LOCS[i][1], MOB_LOCS[i][2], MOB_LOCS[i][3], MOB_LOCS[i][4], false, 0L, false));
			}
		}
		else if (event.equalsIgnoreCase("room2_spawn2"))
		{
			for (int i = 45; i <= 131; i++)
			{
				_roomMobs2.add(addSpawn(MOB_LOCS[i][0], MOB_LOCS[i][1], MOB_LOCS[i][2], MOB_LOCS[i][3], MOB_LOCS[i][4], false, 0L, false));
			}
		}
		else if (event.equalsIgnoreCase("room1_del"))
		{
			for (Npc mob : _roomMobs1)
			{
				if (mob != null)
				{
					mob.deleteMe();
				}
			}
			_roomMobs1.clear();
		}
		else if (event.equalsIgnoreCase("room2_del"))
		{
			for (Npc mob : _roomMobs2)
			{
				if (mob != null)
				{
					mob.deleteMe();
				}
			}
			_roomMobs2.clear();
		}
		else if (event.equalsIgnoreCase("room3_del"))
		{
			if (_demon1 != null)
			{
				_demon1.deleteMe();
			}

			if (_demon2 != null)
			{
				_demon2.deleteMe();
			}

			if (_demon3 != null)
			{
				_demon3.deleteMe();
			}

			if (_demon4 != null)
			{
				_demon4.deleteMe();
			}

			if (_portrait1 != null)
			{
				_portrait1.deleteMe();
			}

			if (_portrait2 != null)
			{
				_portrait2.deleteMe();
			}

			if (_portrait3 != null)
			{
				_portrait3.deleteMe();
			}

			if (_portrait4 != null)
			{
				_portrait4.deleteMe();
			}

			if (_frintezza != null)
			{
				_frintezza.deleteMe();
			}

			if (_weakScarlet != null)
			{
				_weakScarlet.deleteMe();
			}

			if (_strongScarlet != null)
			{
				_strongScarlet.deleteMe();
			}

			_demon1 = null;
			_demon2 = null;
			_demon3 = null;
			_demon4 = null;
			_portrait1 = null;
			_portrait2 = null;
			_portrait3 = null;
			_portrait4 = null;
			_frintezza = null;
			_weakScarlet = null;
			_strongScarlet = null;
			_activeScarlet = null;
		}
		else if (event.equalsIgnoreCase("clean"))
		{
			_timeTracker = 0L;
			_locCycle = 0;
			_checkDie = 0;
			_onCheck = 0;
			_abnormal = 0;
			_onMorph = 0;
			_secondMorph = 0;
			_thirdMorph = 0;
			_killHallAlarmDevice = 0;
			_killDarkChoirPlayer = 0;
			_killDarkChoirCaptain = 0;
			_playersInside.clear();
		}
		else if (event.equalsIgnoreCase("close"))
		{
			for (int i = 25150051; i <= 25150058; i++)
			{
				DoorData.getInstance().getDoor(i).closeMe();
			}

			for (int i = 25150061; i <= 25150070; i++)
			{
				DoorData.getInstance().getDoor(i).closeMe();
			}

			DoorData.getInstance().getDoor(25150042).closeMe();
			DoorData.getInstance().getDoor(25150043).closeMe();
			DoorData.getInstance().getDoor(25150045).closeMe();
			DoorData.getInstance().getDoor(25150046).closeMe();
		}
		else if (event.equalsIgnoreCase("camera_1"))
		{
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, FIGHTING);
			_frintezzaDummy = addSpawn(DUMMY, 174240, -89805, -5022, 16048, false, 0L, false);
			_frintezzaDummy.setInvul(true);
			_frintezzaDummy.setIsImmobilized(true);

			_overheadDummy = addSpawn(DUMMY, 174232, -88020, -5110, 16384, false, 0L, false);
			_overheadDummy.setInvul(true);
			_overheadDummy.setIsImmobilized(true);
			_overheadDummy.setCollisionHeight(600.0D);
			FRINTEZZA_LAIR.broadcastPacket(new AbstractNpcInfo.NpcInfo(_overheadDummy, null));

			_portraitDummy1 = addSpawn(DUMMY, 172550, -87890, -5100, 16048, false, 0L, false);
			_portraitDummy1.setIsImmobilized(true);
			_portraitDummy1.setInvul(true);

			_portraitDummy3 = addSpawn(DUMMY, 175950, -87890, -5100, 16048, false, 0L, false);
			_portraitDummy3.setIsImmobilized(true);
			_portraitDummy3.setInvul(true);

			_scarletDummy = addSpawn(SCARLET_DUMMY, 174232, -88020, -5110, 16384, false, 0L, false);
			_scarletDummy.setInvul(true);
			_scarletDummy.setIsImmobilized(true);

			startQuestTimer("stop_pc", npc, null, 0L);
			startQuestTimer("camera_2", _overheadDummy, null, 1000L);
		}
		else if (event.equalsIgnoreCase("camera_2"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0));
			startQuestTimer("camera_2b", _overheadDummy, null, 0L);
		}
		else if (event.equalsIgnoreCase("camera_2b"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0));
			startQuestTimer("camera_3", _overheadDummy, null, 0L);
		}
		else if (event.equalsIgnoreCase("camera_3"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 300, 90, -10, 6500, 7000, 0, 0, 1, 0));
			_frintezza = (GrandBoss) addSpawn(FRINTEZZA, 174240, -89805, -5022, 16384, false, 0L, false);
			GrandBossManager.getInstance().addBoss(_frintezza);
			_frintezza.setIsImmobilized(true);
			_frintezza.setInvul(true);
			_frintezza.disableAllSkills();

			_demon2 = (Monster) addSpawn(29051, 175876, -88713, -5100, 28205, false, 0L, false);
			_demon2.setIsImmobilized(true);
			_demon2.disableAllSkills();

			_demon3 = (Monster) addSpawn(29051, 172608, -88702, -5100, 64817, false, 0L, false);
			_demon3.setIsImmobilized(true);
			_demon3.disableAllSkills();

			_demon1 = (Monster) addSpawn(29050, 175833, -87165, -5100, 35048, false, 0L, false);
			_demon1.setIsImmobilized(true);
			_demon1.disableAllSkills();

			_demon4 = (Monster) addSpawn(29050, 172634, -87165, -5100, 57730, false, 0L, false);
			_demon4.setIsImmobilized(true);
			_demon4.disableAllSkills();
			startQuestTimer("camera_4", _overheadDummy, null, 6500L);
		}
		else if (event.equalsIgnoreCase("camera_4"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezzaDummy.getObjectId(), 1800, 90, 8, 6500, 7000, 0, 0, 1, 0));
			startQuestTimer("camera_5", _frintezzaDummy, null, 900L);
		}
		else if (event.equalsIgnoreCase("camera_5"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezzaDummy.getObjectId(), 140, 90, 10, 2500, 4500, 0, 0, 1, 0));
			startQuestTimer("camera_5b", _frintezzaDummy, null, 4000L);
		}
		else if (event.equalsIgnoreCase("camera_5b"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 40, 75, -10, 0, 1000, 0, 0, 1, 0));
			startQuestTimer("camera_6", _frintezza, null, 0L);
		}
		else if (event.equalsIgnoreCase("camera_6"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 40, 75, -10, 0, 12000, 0, 0, 1, 0));
			startQuestTimer("camera_7", _frintezza, null, 1350L);
		}
		else if (event.equalsIgnoreCase("camera_7"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_frintezza, 2));
			startQuestTimer("camera_8", _frintezza, null, 7000L);
		}
		else if (event.equalsIgnoreCase("camera_8"))
		{
			startQuestTimer("camera_9", _frintezza, null, 1000L);
			_frintezzaDummy.deleteMe();
			_frintezzaDummy = null;
		}
		else if (event.equalsIgnoreCase("camera_9"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_demon2, 1));
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_demon3, 1));
			startQuestTimer("camera_9b", _frintezza, null, 400L);
		}
		else if (event.equalsIgnoreCase("camera_9b"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_demon1, 1));
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_demon4, 1));
			for (Creature pc : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
			{
				if (pc.getX() < 174232)
				{
					pc.broadcastPacket(new SpecialCamera(_portraitDummy1.getObjectId(), 1000, 118, 0, 100, 1000, 0, 0, 1, 0));
					continue;
				}

				pc.broadcastPacket(new SpecialCamera(_portraitDummy3.getObjectId(), 1000, 62, 0, 100, 1000, 0, 0, 1, 0));
			}
			startQuestTimer("camera_9c", _frintezza, null, 0L);
		}
		else if (event.equalsIgnoreCase("camera_9c"))
		{
			for (Creature pc : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
			{
				if (pc.getX() < 174232)
				{
					pc.broadcastPacket(new SpecialCamera(_portraitDummy1.getObjectId(), 1000, 118, 0, 100, 10000, 0, 0, 1, 0));
					continue;
				}

				pc.broadcastPacket(new SpecialCamera(_portraitDummy3.getObjectId(), 1000, 62, 0, 100, 10000, 0, 0, 1, 0));
			}
			startQuestTimer("camera_10", _frintezza, null, 2000L);
		}
		else if (event.equalsIgnoreCase("camera_10"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 240, 90, 0, 0, 1000, 0, 0, 1, 0));
			startQuestTimer("camera_11", _frintezza, null, 0L);
		}
		else if (event.equalsIgnoreCase("camera_11"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 240, 90, 25, 5500, 8000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_frintezza, 3));
			_portraitDummy1.deleteMe();
			_portraitDummy3.deleteMe();
			_portraitDummy1 = null;
			_portraitDummy3 = null;
			startQuestTimer("camera_11b", _frintezza, null, 4100L);
			startQuestTimer("camera_12", _frintezza, null, 2500L);
		}
		else if (event.equalsIgnoreCase("camera_11b"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5006, 1, 34000, 0));
		}
		else if (event.equalsIgnoreCase("camera_12"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_13", _frintezza, null, 700L);
		}
		else if (event.equalsIgnoreCase("camera_13"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_14", _frintezza, null, 1300L);
		}
		else if (event.equalsIgnoreCase("camera_14"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 120, 180, 45, 1500, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_16", _frintezza, null, 1500L);
		}
		else if (event.equalsIgnoreCase("camera_16"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 520, 135, 45, 8000, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_17", _frintezza, null, 7500L);
		}
		else if (event.equalsIgnoreCase("camera_17"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 1500, 110, 25, 10000, 13000, 0, 0, 1, 0));
			startQuestTimer("camera_18", _frintezza, null, 9500L);
		}
		else if (event.equalsIgnoreCase("camera_18"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 930, 160, -20, 0, 1000, 0, 0, 1, 0));
			startQuestTimer("camera_18b", _overheadDummy, null, 0L);
		}
		else if (event.equalsIgnoreCase("camera_18b"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 600, 180, -25, 0, 10000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_scarletDummy, _scarletDummy, 5004, 1, 5800, 0));
			_weakScarlet = (GrandBoss) addSpawn(SCARLET1, 174232, -88020, -5114, 20458, false, 0L, true);
			_weakScarlet.setInvul(true);
			_weakScarlet.setIsImmobilized(true);
			_weakScarlet.disableAllSkills();
			_activeScarlet = _weakScarlet;
			startQuestTimer("camera_19", _scarletDummy, null, 2400L);
			startQuestTimer("camera_19b", _scarletDummy, null, 5000L);
			startQuestTimer("camera_19c", _scarletDummy, null, 6300L);
		}
		else if (event.equalsIgnoreCase("camera_19"))
		{
			_weakScarlet.teleportTo(174232, -88020, -5114, 0);
		}
		else if (event.equalsIgnoreCase("camera_19b"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_scarletDummy.getObjectId(), 800, 180, 10, 1000, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_20", _scarletDummy, null, 2100L);
		}
		else if (event == "camera_19c")
		{
			throwUp(npc, 500.0D, SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5004, 1));
		}
		else if (event.equalsIgnoreCase("camera_20"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_weakScarlet.getObjectId(), 300, 60, 8, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_21", _weakScarlet, null, 2000L);
		}
		else if (event.equalsIgnoreCase("camera_21"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_weakScarlet.getObjectId(), 500, 90, 10, 3000, 5000, 0, 0, 1, 0));
			startQuestTimer("camera_22", _weakScarlet, null, 3000L);
		}
		else if (event.equalsIgnoreCase("camera_22"))
		{
			_portrait2 = (Monster) addSpawn(EVIL_SPIRIT_2, 175876, -88713, -5000, 28205, false, 0L, false);
			_portrait2.setIsImmobilized(true);
			_portrait2.disableAllSkills();

			_portrait3 = (Monster) addSpawn(EVIL_SPIRIT_2, 172608, -88702, -5000, 64817, false, 0L, false);
			_portrait3.setIsImmobilized(true);
			_portrait3.disableAllSkills();

			_portrait1 = (Monster) addSpawn(EVIL_SPIRIT, 175833, -87165, -5000, 35048, false, 0L, false);
			_portrait1.setIsImmobilized(true);
			_portrait1.disableAllSkills();

			_portrait4 = (Monster) addSpawn(EVIL_SPIRIT, 172634, -87165, -5000, 57730, false, 0L, false);
			_portrait4.setIsImmobilized(true);
			_portrait4.disableAllSkills();

			_overheadDummy.deleteMe();
			_scarletDummy.deleteMe();
			_overheadDummy = null;
			_scarletDummy = null;
			startQuestTimer("camera_23", _weakScarlet, null, 2000L);
			startQuestTimer("start_pc", _weakScarlet, null, 2000L);
			startQuestTimerAtFixedRate("songs_play", _frintezza, null, (10000 + Rnd.get(10000)));
			startQuestTimer("skill01", _weakScarlet, null, (10000 + Rnd.get(10000)));
		}
		else if (event.equalsIgnoreCase("camera_23"))
		{
			_demon1.setIsImmobilized(false);
			_demon2.setIsImmobilized(false);
			_demon3.setIsImmobilized(false);
			_demon4.setIsImmobilized(false);

			_demon1.enableAllSkills();
			_demon2.enableAllSkills();
			_demon3.enableAllSkills();
			_demon4.enableAllSkills();

			_portrait1.setIsImmobilized(false);
			_portrait2.setIsImmobilized(false);
			_portrait3.setIsImmobilized(false);
			_portrait4.setIsImmobilized(false);

			_portrait1.enableAllSkills();
			_portrait2.enableAllSkills();
			_portrait3.enableAllSkills();
			_portrait4.enableAllSkills();

			_weakScarlet.setInvul(false);
			_weakScarlet.setIsImmobilized(false);
			_weakScarlet.enableAllSkills();
			_weakScarlet.setMove();

			startQuestTimer("spawn_minion", _portrait1, null, 20000L);
			startQuestTimer("spawn_minion", _portrait2, null, 20000L);
			startQuestTimer("spawn_minion", _portrait3, null, 20000L);
			startQuestTimer("spawn_minion", _portrait4, null, 20000L);

			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5006, 1, 34000, 0));
		}
		else if (event.equalsIgnoreCase("stop_pc"))
		{
			for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
			{
				cha.abortAll(true);
				cha.disableAllSkills();
				cha.setTarget(null);
				cha.setInvul(true);
				cha.setIsImmobilized(true);
				cha.getAI().tryToIdle();
			}
		}
		else if (event.equalsIgnoreCase("start_pc"))
		{
			for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
			{
				cha.enableAllSkills();
				cha.setIsImmobilized(false);
				cha.setInvul(false);
			}
		}
		else if (event.equalsIgnoreCase("stop_npc"))
		{
			for (Npc mob : FRINTEZZA_LAIR.getKnownTypeInside(Npc.class))
			{
				if (mob.getNpcId() != FRINTEZZA)
				{
					mob.abortAll(false);
					mob.disableAllSkills();
					mob.setInvul(true);
					mob.setIsImmobilized(true);
					mob.getAI().tryToIdle();
				}
			}
		}
		else if (event.equalsIgnoreCase("start_npc"))
		{
			for (Npc mob : FRINTEZZA_LAIR.getKnownTypeInside(Npc.class))
			{
				if (mob.getNpcId() != FRINTEZZA)
				{
					mob.enableAllSkills();
					mob.isRunning();
					mob.setInvul(false);
					mob.setIsImmobilized(false);
				}
			}
		}
		else if (event.equalsIgnoreCase("morph_end"))
		{
			_onMorph = 0;
		}
		else if (event.equalsIgnoreCase("morph_01"))
		{
			_heading = npc.getHeading();
			if (npc.getHeading() < 32768)
			{
				_angle = Math.abs(180 - (int) (_heading / 182.044444444D));
			}
			else
			{
				_angle = Math.abs(540 - (int) (_heading / 182.044444444D));
			}

			_weakScarlet_x = _weakScarlet.getX();
			_weakScarlet_y = _weakScarlet.getY();
			_weakScarlet_z = _weakScarlet.getZ();
			_weakScarlet.teleportTo(_weakScarlet_x, _weakScarlet_y, _weakScarlet_z + 50, 0);
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_weakScarlet.getObjectId(), 250, _angle, 12, 3000, 11000, 0, 0, 1, 0));
			startQuestTimer("morph_02", _weakScarlet, null, 3000L);
		}
		else if (event.equalsIgnoreCase("morph_02"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_weakScarlet, 1));
			_weakScarlet.setRightHandItemId(7903);
			startQuestTimer("morph_03", _weakScarlet, null, 1500L);
		}
		else if (event.equalsIgnoreCase("morph_03"))
		{
			startQuestTimer("morph_04", _weakScarlet, null, 1500L);
		}
		else if (event.equalsIgnoreCase("morph_04"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_weakScarlet, 4));

			L2Skill skill = SkillTable.getInstance().getInfo(5017, 1);
			if (skill != null)
			{
				skill.getEffects(_weakScarlet, _weakScarlet);
			}

			startQuestTimer("morph_end", _weakScarlet, null, 3000L);
			startQuestTimer("start_pc", _weakScarlet, player, 1000L);
			startQuestTimer("start_npc", _weakScarlet, player, 1000L);
			startQuestTimerAtFixedRate("songs_play", _frintezza, null, (10000 + Rnd.get(10000)));
			startQuestTimer("skill02", _weakScarlet, null, (10000 + Rnd.get(10000)));

			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5006, 1, 34000, 0));
		}
		else if (event.equalsIgnoreCase("morph_05a"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_frintezza, 4));
		}
		else if (event.equalsIgnoreCase("morph_05"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 250, 120, 15, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_06", _frintezza, null, 0L);
		}
		else if (event.equalsIgnoreCase("morph_06"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 250, 120, 15, 4000, 10000, 0, 0, 1, 0));
			_scarlet_x = _weakScarlet.getX();
			_scarlet_y = _weakScarlet.getY();
			_scarlet_z = _weakScarlet.getZ();
			_scarlet_h = _weakScarlet.getHeading();
			_weakScarlet.deleteMe();
			_weakScarlet = null;
			_activeScarlet = null;
			_weakScarlet = (GrandBoss) addSpawn(SCARLET1, _scarlet_x, _scarlet_y, _scarlet_z + 50, _scarlet_h, false, 0L, false);
			_weakScarlet.setInvul(true);
			_weakScarlet.setIsImmobilized(true);
			_weakScarlet.disableAllSkills();
			_weakScarlet.setRightHandItemId(7903);
			startQuestTimer("morph_07", _weakScarlet, null, 4000L);
		}
		else if (event.equalsIgnoreCase("morph_07"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5006, 1, 34000, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 500, 70, 15, 3000, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_08", _frintezza, null, 3000L);
		}
		else if (event.equalsIgnoreCase("morph_08"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 2500, 90, 12, 3000, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_09", _frintezza, null, 3000L);
		}
		else if (event.equalsIgnoreCase("morph_09"))
		{
			_heading = npc.getHeading();
			if (_heading < 32768)
			{
				_angle = Math.abs(180 - (int) (_heading / 182.044444444D));
			}
			else
			{
				_angle = Math.abs(540 - (int) (_heading / 182.044444444D));
			}

			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_weakScarlet.getObjectId(), 250, _angle, 12, 0, 1000, 0, 0, 1, 0));
			startQuestTimer("morph_10", _weakScarlet, null, 0L);
		}
		else if (event.equalsIgnoreCase("morph_10"))
		{
			_heading = npc.getHeading();
			if (_heading < 32768)
			{
				_angle = Math.abs(180 - (int) (_heading / 182.044444444D));
			}
			else
			{
				_angle = Math.abs(540 - (int) (_heading / 182.044444444D));
			}

			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_weakScarlet.getObjectId(), 250, _angle, 12, 500, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_11", _weakScarlet, null, 500L);
		}
		else if (event.equalsIgnoreCase("morph_11"))
		{
			_heading = npc.getHeading();
			if (_heading < 32768)
			{
				_angle = Math.abs(180 - (int) (_heading / 182.044444444D));
			}
			else
			{
				_angle = Math.abs(540 - (int) (_heading / 182.044444444D));
			}

			_weakScarlet.doDie(_weakScarlet);
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_weakScarlet.getObjectId(), 450, _angle, 14, 7200, 8000, 0, 0, 1, 0));
			startQuestTimer("morph_12", _weakScarlet, null, 6250L);
			startQuestTimer("morph_13", _weakScarlet, null, 7200L);
		}
		else if (event.equalsIgnoreCase("morph_12"))
		{
			_weakScarlet.deleteMe();
			_weakScarlet = null;
		}
		else if (event.equalsIgnoreCase("morph_13"))
		{
			_heading = npc.getHeading();
			if (_heading < 32768)
			{
				_angle = Math.abs(180 - (int) (_heading / 182.044444444D));
			}
			else
			{
				_angle = Math.abs(540 - (int) (_heading / 182.044444444D));
			}

			_strongScarlet = (GrandBoss) addSpawn(SCARLET2, _scarlet_x, _scarlet_y, _scarlet_z, _scarlet_h, false, 0L, false);
			_strongScarlet.setInvul(true);
			_strongScarlet.setIsImmobilized(true);
			_strongScarlet.disableAllSkills();
			_activeScarlet = _strongScarlet;
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_strongScarlet.getObjectId(), 450, _angle, 12, 3000, 20000, 0, 0, 1, 0));
			startQuestTimer("morph_14", _strongScarlet, null, 3000L);
		}
		else if (event.equalsIgnoreCase("morph_14"))
		{
			startQuestTimer("morph_15", _strongScarlet, null, 11100L);
		}
		else if (event.equalsIgnoreCase("morph_15"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_strongScarlet, 2));

			L2Skill skill = SkillTable.getInstance().getInfo(5017, 1);
			if (skill != null)
			{
				skill.getEffects(_strongScarlet, _strongScarlet);
			}

			startQuestTimer("morph_end", _strongScarlet, null, 9000L);
			startQuestTimer("start_pc", _strongScarlet, null, 3000L);
			startQuestTimer("start_npc", _strongScarlet, null, 6000L);
			startQuestTimerAtFixedRate("songs_play", _frintezza, null, (10000 + Rnd.get(10000)));
			startQuestTimer("skill03", _strongScarlet, null, (10000 + Rnd.get(10000)));
		}
		else if (event.equalsIgnoreCase("morph_16"))
		{
			_heading = npc.getHeading();
			if (_heading < 32768)
			{
				_angle = Math.abs(180 - (int) (_heading / 182.044444444D));
			}
			else
			{
				_angle = Math.abs(540 - (int) (_heading / 182.044444444D));
			}

			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_strongScarlet.getObjectId(), 300, _angle - 180, 5, 0, 7000, 0, 0, 1, 0));
			startQuestTimer("morph_17", _strongScarlet, null, 0L);
		}
		else if (event.equalsIgnoreCase("morph_17"))
		{
			_heading = npc.getHeading();
			if (_heading < 32768)
			{
				_angle = Math.abs(180 - (int) (_heading / 182.044444444D));
			}
			else
			{
				_angle = Math.abs(540 - (int) (_heading / 182.044444444D));
			}

			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_strongScarlet.getObjectId(), 200, _angle, 85, 7500, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_17b", _frintezza, null, 7400L);
			startQuestTimer("morph_18", _frintezza, null, 7500L);
		}
		else if (event.equalsIgnoreCase("morph_17b"))
		{
			_frintezza.doDie(_frintezza);
		}
		else if (event.equalsIgnoreCase("morph_18"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 100, 120, 5, 0, 7000, 0, 0, 1, 0));
			startQuestTimer("morph_19", _frintezza, null, 0L);
		}
		else if (event.equalsIgnoreCase("morph_19"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 100, 90, 5, 7000, 15000, 0, 0, 1, 0));
			startQuestTimer("morph_20", _frintezza, null, 7000L);
		}
		else if (event.equalsIgnoreCase("morph_20"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezza.getObjectId(), 900, 90, 25, 7000, 10000, 0, 0, 1, 0));
			startQuestTimer("rooms_del", npc, null, 0L);
			startQuestTimer("spawn_cubes", _frintezza, null, 7000L);
			startQuestTimer("start_pc", _frintezza, null, 7000L);
		}
		else if (event.equalsIgnoreCase("songs_play"))
		{
			_soulBreakArrowUse = 0;
			if (_frintezza != null && !_frintezza.isDead() && _onMorph == 0)
			{
				_onSong = Rnd.get(1, 5);
				String SongName = "";
				switch (_onSong)
				{
					case 1:
						SongName = "Frintezza's Healing Rhapsody";
						break;
					case 2:
						SongName = "Frintezza's Rampaging Opus";
						break;
					case 3:
						SongName = "Frintezza's Power Concerto";
						break;
					case 4:
						SongName = "Frintezza's Plagued Concerto";
						break;
					case 5:
						SongName = "Frintezza's Psycho Symphony";
						break;
					default:
						SongName = "Frintezza's Song";
						break;
				}

				FRINTEZZA_LAIR.broadcastPacket(new ExShowScreenMessage(SongName, 6000));
				if (_onSong == 1 && _thirdMorph == 1 && _strongScarlet.getStatus().getHp() < _strongScarlet.getStatus().getMaxHp() * 0.6D && Rnd.get(100) < 80)
				{
					FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, 1, 32000, 0));
					startQuestTimer("songs_effect", _frintezza, null, 5000L);
					startQuestTimerAtFixedRate("songs_play", _frintezza, null, (32000 + Rnd.get(10000)));
				}
				else if (_onSong == 2 || _onSong == 3)
				{
					FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, _onSong, 32000, 0));
					startQuestTimer("songs_effect", _frintezza, null, 5000L);
					startQuestTimerAtFixedRate("songs_play", _frintezza, null, (32000 + Rnd.get(10000)));
				}
				else if (_onSong == 4 && _secondMorph == 1)
				{
					FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, 4, 31000, 0));
					startQuestTimer("songs_effect", _frintezza, null, 5000L);
					startQuestTimerAtFixedRate("songs_play", _frintezza, null, (31000 + Rnd.get(10000)));
				}
				else if (_onSong == 5 && _thirdMorph == 1 && _abnormal == 0)
				{
					_abnormal = 1;
					FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, 5, 35000, 0));
					startQuestTimer("songs_effect", _frintezza, null, 5000L);
					startQuestTimerAtFixedRate("songs_play", _frintezza, null, (35000 + Rnd.get(10000)));
				}
				else
				{
					startQuestTimerAtFixedRate("songs_play", _frintezza, null, (5000 + Rnd.get(50000)));
				}
			}
		}
		else if (event.equalsIgnoreCase("songs_effect"))
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5008, _onSong);
			if (skill == null)
			{
				return null;
			}

			if (_onSong == 1 || _onSong == 2 || _onSong == 3)
			{
				if (_frintezza != null && !_frintezza.isDead() && _activeScarlet != null && !_activeScarlet.isDead())
				{
					skill.getEffects(_frintezza, _activeScarlet);
				}
			}
			else if (_onSong == 4)
			{
				for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
				{
					if (Rnd.get(100) < 80)
					{
						skill.getEffects(_frintezza, cha);
						cha.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5008, 4));
					}
				}
			}
			else if (_onSong == 5)
			{
				for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
				{
					if (Rnd.get(100) < 70)
					{
						cha.abortAll(true);
						cha.disableAllSkills();
						cha.setIsParalyzed(true);
						cha.setIsImmobilized(true);
						cha.getAI().tryToIdle();
						skill.getEffects(_frintezza, cha);
						cha.startAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
						cha.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5008, 5));
					}
				}
				startQuestTimer("stop_effect", _frintezza, null, 25000L);
			}
		}
		else if (event.equalsIgnoreCase("stop_effect"))
		{
			for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
			{
				cha.stopAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
				cha.stopAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
				cha.enableAllSkills();
				cha.setIsImmobilized(false);
				cha.setIsParalyzed(false);
			}
			_abnormal = 0;
		}
		else if (event.equalsIgnoreCase("attack_stop"))
		{
			cancelQuestTimers("skill01");
			cancelQuestTimers("skill02");
			cancelQuestTimers("skill03");
			cancelQuestTimers("songs_play");
			cancelQuestTimers("songs_effect");
			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillCanceled(_frintezza.getObjectId()));
		}
		else if (event.equalsIgnoreCase("check_hp"))
		{
			if (npc.isDead())
			{
				_onMorph = 1;
				FRINTEZZA_LAIR.broadcastPacket(new PlaySound("BS01_D"));
				startQuestTimer("attack_stop", _frintezza, null, 0L);
				startQuestTimer("stop_pc", npc, null, 0L);
				startQuestTimer("stop_npc", npc, null, 0L);
				startQuestTimer("morph_16", npc, null, 0L);
			}
			else
			{
				_checkDie += 10;
				if (_checkDie < 3000)
				{
					startQuestTimer("check_hp", npc, null, 10L);
				}
				else
				{
					_onCheck = 0;
					_checkDie = 0;
				}
			}
		}
		else if (event.equalsIgnoreCase("skill01"))
		{
			if (_weakScarlet != null && !_weakScarlet.isDead() && _secondMorph == 0 && _thirdMorph == 0 && _onMorph == 0)
			{
				int i = Rnd.get(0, 1);
				L2Skill skill = SkillTable.getInstance().getInfo(SKILL[i][0], SKILL[i][1]);
				if (skill != null)
				{
					_weakScarlet.getMove().stop();
					_weakScarlet.getCast().isCastingNow();
					_weakScarlet.getAI().tryToCast(player, skill);
				}
				startQuestTimer("skill01", npc, null, (SKILL[i][2] + 5000 + Rnd.get(10000)));
			}
		}
		else if (event.equalsIgnoreCase("skill02"))
		{
			if (_weakScarlet != null && !_weakScarlet.isDead() && _secondMorph == 1 && _thirdMorph == 0 && _onMorph == 0)
			{
				int i = 0;
				if (_abnormal == 0)
				{
					i = Rnd.get(2, 5);
				}
				else
				{
					i = Rnd.get(2, 4);
				}

				L2Skill skill = SkillTable.getInstance().getInfo(SKILL[i][0], SKILL[i][1]);
				if (skill != null)
				{
					_weakScarlet.getMove().stop();
					_weakScarlet.getCast().isCastingNow();
					_weakScarlet.getAI().tryToCast(player, skill);
				}
				startQuestTimer("skill02", npc, null, (SKILL[i][2] + 5000 + Rnd.get(10000)));
				if (i == 5)
				{
					_abnormal = 1;
					startQuestTimer("float_effect", _weakScarlet, null, 4000L);
				}
			}
		}
		else if (event.equalsIgnoreCase("skill03"))
		{
			if (_strongScarlet != null && !_strongScarlet.isDead() && _secondMorph == 1 && _thirdMorph == 1 && _onMorph == 0)
			{
				int i = 0;
				if (_abnormal == 0)
				{
					i = Rnd.get(6, 10);
				}
				else
				{
					i = Rnd.get(6, 9);
				}

				L2Skill skill = SkillTable.getInstance().getInfo(SKILL[i][0], SKILL[i][1]);
				if (skill != null)
				{
					_strongScarlet.getMove().stop();
					_strongScarlet.getCast().isCastingNow();
					_strongScarlet.getAI().tryToCast(player, skill);
				}

				startQuestTimer("skill03", npc, null, (SKILL[i][2] + 5000 + Rnd.get(10000)));
				if (i == 10)
				{
					_abnormal = 1;
					startQuestTimer("float_effect", npc, null, 3000L);
				}
			}
		}
		else if (event.equalsIgnoreCase("float_effect"))
		{
			if (npc.getCast().isCastingNow())
			{
				startQuestTimer("float_effect", npc, null, 500L);
			}
			else
			{
				for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
				{
					if (cha.getFirstEffect(5016) != null)
					{
						cha.abortAll(true);
						cha.disableAllSkills();
						cha.setIsParalyzed(true);
						cha.setIsImmobilized(true);
						cha.getAI().tryToIdle();
						cha.startAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
					}
				}
				startQuestTimer("stop_effect", npc, null, 25000L);
			}
		}
		else if (event.equalsIgnoreCase("action"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(npc, 1));
		}
		else if (event.equalsIgnoreCase("bomber"))
		{
			_bomber = 0;
		}
		else if (event.equalsIgnoreCase("room_final"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new CreatureSay(npc.getObjectId(), SayType.SHOUT, null, "Exceeded his time limit, challenge failed!"));
			FRINTEZZA_LAIR.oustAllPlayers();
			cancelQuestTimers("waiting");
			cancelQuestTimers("frintezza_despawn");
			startQuestTimer("clean", npc, null, 1000L);
			startQuestTimer("close", npc, null, 1000L);
			startQuestTimer("room1_del", npc, null, 1000L);
			startQuestTimer("room2_del", npc, null, 1000L);
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
		}
		else if (event.equalsIgnoreCase("frintezza_despawn"))
		{
			// Inactivity task - 30min
			if (_timeTracker + 1800000L < System.currentTimeMillis())
			{
				// Stop all tasks.
				cancelQuestTimers("waiting");
				cancelQuestTimers("room_final");
				cancelQuestTimers("spawn_minion");
				startQuestTimer("clean", npc, null, 1000L);
				startQuestTimer("close", npc, null, 1000L);
				startQuestTimer("attack_stop", npc, null, 1000L);
				startQuestTimer("room1_del", npc, null, 1000L);
				startQuestTimer("room2_del", npc, null, 1000L);
				startQuestTimer("room3_del", npc, null, 1000L);
				startQuestTimer("minions_despawn", npc, null, 1000L);

				// Set it as asleep.
				GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);

				cancelQuestTimers("frintezza_despawn");

				// Kick all Players.
				FRINTEZZA_LAIR.oustAllPlayers();
			}
			else
			{
				startQuestTimer("frintezza_despawn", null, null, 60000);
			}
		}
		else if (event.equalsIgnoreCase("minions_despawn"))
		{
			for (int i = 0; i < _minions.size(); i++)
			{
				Attackable mob = _minions.get(i);
				if (mob != null)
				{
					mob.decayMe();
				}
			}
			_minions.clear();
		}
		else if (event.equalsIgnoreCase("spawn_minion"))
		{
			if (npc != null && !npc.isDead() && _frintezza != null && !_frintezza.isDead())
			{
				Npc mob = addSpawn(npc.getNpcId() + 2, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0L, false);
				startQuestTimer("action", mob, null, 200L);
				startQuestTimer("spawn_minion", npc, null, 18000L);
			}
		}
		else if (event.equalsIgnoreCase("rooms_del"))
		{
			for (Npc mob : _roomMobs1)
			{
				mob.deleteMe();
			}

			for (Npc mob : _roomMobs2)
			{
				mob.deleteMe();
			}

			for (Npc mob : FRINTEZZA_LAIR.getKnownTypeInside(Npc.class))
			{
				mob.deleteMe();
			}

			_roomMobs1.clear();
			_roomMobs2.clear();

			if (_frintezza != null)
			{
				_frintezza.deleteMe();
				_frintezza = null;
			}
		}
		else if (event.equalsIgnoreCase("spawn_cubes"))
		{
			addSpawn(TELEPORTATION_CUBIC_LOC, 174232, -88020, -5114, 16384, false, 900000L, false);
		}
		else if (event.equalsIgnoreCase("frintezza_unlock"))
		{
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
		}
		else if (event.equalsIgnoreCase("remove_players"))
		{
			FRINTEZZA_LAIR.oustAllPlayers();
		}

		return super.onTimer(event, npc, player);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getNpcId() == TELEPORTATION_CUBIC_LOC)
		{
			int x = 150037 + Rnd.get(500);
			int y = -57720 + Rnd.get(500);
			player.teleportTo(x, y, -2976, 0);
			return null;
		}

		String htmltext = "";
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == DEAD)
		{
			htmltext = "<html><body>There is nothing beyond the Magic Force Field. Come back later.<br>(You may not enter because Frintezza is not inside the Imperial Tomb.)</body></html>";
		}
		else if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == DORMANT)
		{
			boolean party_check_success = true;
			if (!player.isGM())
			{
				if (!player.isInParty() || !player.getParty().isLeader(player) || player.getParty().getCommandChannel() == null || player.getParty().getCommandChannel().getLeader() != player)
				{
					htmltext = "<html><body>No reaction. Contact must be initiated by the Command Channel Leader.</body></html>";
					party_check_success = false;
				}
				else if (player.getParty().getCommandChannel().getParties().size() < 1 || player.getParty().getCommandChannel().getParties().size() > 2)
				{
					htmltext = "<html><body>Your command channel needs to have at least 4 parties and a maximum of 5.</body></html>";
					party_check_success = false;
				}
			}

			if (party_check_success)
			{
				int FRINTEZZA_TEST_MODE = 0;
				if (FRINTEZZA_TEST_MODE == 1)
				{
					startQuestTimer("camera_1", npc, null, 5000L);
					player.teleportTo(174232, -88020, -5110, 0);
				}
				else if (player.getInventory().getItemByItemId(FRINTEZZA_SCROLL) == null)
				{
					htmltext = "<html><body>You dont have required item.</body></html>";
				}
				else
				{
					player.destroyItemByItemId("Quest", FRINTEZZA_SCROLL, 1, player, true);
					GrandBossManager.getInstance().setBossStatus(FRINTEZZA, WAITING);
					startQuestTimer("close", null, null, 0L);
					startQuestTimer("room1_spawn", null, null, 5000L);
					startQuestTimer("room_final", null, null, 2100000L);
					startQuestTimer("frintezza_despawn", null, null, 60000L);
					_timeTracker = System.currentTimeMillis();

					if (player.isGM())
					{
						if (player.getParty() != null)
						{
							CommandChannel CC = player.getParty().getCommandChannel();
							if (CC != null)
							{
								for (Party party : CC.getParties())
								{
									if (party == null)
									{
										continue;
									}

									synchronized (_playersInside)
									{
										for (Player member : party.getMembers())
										{
											if (member == null || member.getStatus().getLevel() < 74 || !member.isIn3DRadius(npc, 700))
											{
												continue;
											}

											if (_playersInside.size() > 45)
											{
												member.sendMessage("The number of challenges have been full, so can not enter.");
												break;
											}

											_playersInside.add(member);
											FRINTEZZA_LAIR.allowPlayerEntry(member, 300);
											member.teleportTo(INVADE_LOC[_locCycle][0] + Rnd.get(50), INVADE_LOC[_locCycle][1] + Rnd.get(50), INVADE_LOC[_locCycle][2], 0);
										}

										if (_playersInside.size() > 45)
										{
											break;
										}
									}

									_locCycle++;
									if (_locCycle >= 6)
									{
										_locCycle = 1;
									}
								}
							}
							else
							{
								Party party = player.getParty();
								for (Player member : party.getMembers())
								{
									if (member == null || member.getStatus().getLevel() < 74 || !member.isIn3DRadius(npc, 700))
									{
										continue;
									}

									synchronized (_playersInside)
									{
										if (_playersInside.size() > 45)
										{
											member.sendMessage("The number of challenges have been full, so can not enter.");
											break;
										}

										_playersInside.add(member);
									}

									FRINTEZZA_LAIR.allowPlayerEntry(member, 300);
									member.teleportTo(INVADE_LOC[_locCycle][0] + Rnd.get(50), INVADE_LOC[_locCycle][1] + Rnd.get(50), INVADE_LOC[_locCycle][2], 0);
								}

								_locCycle++;
								if (_locCycle >= 6)
								{
									_locCycle = 1;
								}
							}
						}
						else if (player.isIn3DRadius(npc, 700))
						{
							synchronized (_playersInside)
							{
								_playersInside.add(player);
							}

							player.teleportTo(INVADE_LOC[_locCycle][0] + Rnd.get(50), INVADE_LOC[_locCycle][1] + Rnd.get(50), INVADE_LOC[_locCycle][2], 0);
						}
					}
					else
					{
						CommandChannel CC = player.getParty().getCommandChannel();
						for (Party party : CC.getParties())
						{
							if (party == null)
							{
								continue;
							}

							synchronized (_playersInside)
							{
								for (Player member : party.getMembers())
								{
									if (member == null || member.getStatus().getLevel() < 74 || !member.isIn3DRadius(npc, 700))
									{
										continue;
									}

									if (_playersInside.size() > 45)
									{
										member.sendMessage("The number of challenges have been full, so can not enter.");
										break;
									}

									_playersInside.add(member);
									FRINTEZZA_LAIR.allowPlayerEntry(member, 300);
									member.teleportTo(INVADE_LOC[_locCycle][0] + Rnd.get(50), INVADE_LOC[_locCycle][1] + Rnd.get(50), INVADE_LOC[_locCycle][2], 0);
								}

								if (_playersInside.size() > 45)
								{
									break;
								}
							}

							_locCycle++;
							if (_locCycle >= 6)
							{
								_locCycle = 1;
							}
						}
					}
				}
			}
		}
		else
		{
			htmltext = "<html><body>Someone else is already inside the Magic Force Field. Try again later.</body></html>";
		}
		return htmltext;
	}

	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (targets.length > 0 && targets[0] == npc)
		{
			if (npc == _frintezza)
			{
				npc.getStatus().setHp(npc.getStatus().getMaxHp(), false);
			}

			switch (skill.getId())
			{
				case 2234:
					if (_frintezza != null && targets[0] == npc && npc.getNpcId() == FRINTEZZA && _soulBreakArrowUse == 1)
					{
						FRINTEZZA_LAIR.broadcastPacket(new SocialAction(npc, 2));
					}

					if (_frintezza != null && targets[0] == npc && npc.getNpcId() == FRINTEZZA && _soulBreakArrowUse == 0)
					{
						if (Rnd.get(100) < 100)
						{
							FRINTEZZA_LAIR.broadcastPacket(new MagicSkillCanceled(_frintezza.getObjectId()));
							cancelQuestTimers("songs_play");
							cancelQuestTimers("songs_effect");
							startQuestTimer("stop_effect", _frintezza, null, 0L);
							npc.getCast().stop();
							FRINTEZZA_LAIR.broadcastPacket(new MagicSkillCanceled(_frintezza.getObjectId()));

							for (Creature pc : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
							{
								pc.stopSkillEffects(5008);
							}

							startQuestTimerAtFixedRate("songs_play", _frintezza, null, (60000 + Rnd.get(60000)));
							npc.broadcastNpcSay("Musical performance as temporarily interrupted.");
							_soulBreakArrowUse = 1;
						}
					}
					break;

				case 2276:
					if ((_frintezza != null && targets[0] == npc && npc.getNpcId() == EVIL_SPIRIT) || (_frintezza != null && targets[0] == npc && npc.getNpcId() == EVIL_SPIRIT_2))
					{
						npc.doDie(caster);
						npc.broadcastNpcSay("I was destroyed by Dewdrop of Destruction.");
					}
					break;
			}
		}
		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		_timeTracker = System.currentTimeMillis();
		if (npc.getNpcId() == FRINTEZZA)
		{
			npc.getStatus().setHpMp(npc.getStatus().getMaxHp(), 0.0D);
			return;
		}

		if (npc.getNpcId() == SCARLET1 && _secondMorph == 0 && _thirdMorph == 0 && _onMorph == 0 && npc.getStatus().getHp() < npc.getStatus().getMaxHp() * 0.75D && GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			startQuestTimer("attack_stop", _frintezza, null, 2000L);
			_secondMorph = 1;
			_onMorph = 1;
			startQuestTimer("stop_pc", npc, null, 1000L);
			startQuestTimer("stop_npc", npc, null, 1000L);
			startQuestTimer("morph_01", npc, null, 1100L);
		}
		else if (npc.getNpcId() == SCARLET1 && _secondMorph == 1 && _thirdMorph == 0 && _onMorph == 0 && npc.getStatus().getHp() < npc.getStatus().getMaxHp() * 0.5D && GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			startQuestTimer("attack_stop", _frintezza, null, 0L);
			_thirdMorph = 1;
			_onMorph = 1;
			startQuestTimer("stop_pc", npc, null, 1000L);
			startQuestTimer("stop_npc", npc, null, 1000L);
			startQuestTimer("morph_05a", npc, null, 2000L);
			startQuestTimer("morph_05", npc, null, 2100L);
		}
		else if (npc.getNpcId() == SCARLET2 && _secondMorph == 1 && _thirdMorph == 1 && _onCheck == 0 && damage >= npc.getStatus().getHp() && GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			_onCheck = 1;
			startQuestTimer("check_hp", npc, null, 0L);
		}
		else if ((npc.getNpcId() == 29050 || npc.getNpcId() == 29051) && _bomber == 0)
		{
			if (npc.getStatus().getHp() < npc.getStatus().getMaxHp() * 0.1D)
			{
				if (Rnd.get(100) < 30)
				{
					_bomber = 1;
					startQuestTimer("bomber", npc, null, 3000L);
					L2Skill sk = SkillTable.getInstance().getInfo(5011, 1);
					if (sk != null)
					{
						npc.getAI().tryToCast(npc, sk);
					}
				}
			}
		}

		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		Player player = killer.getActingPlayer();
		if (player == null)
		{
			return;
		}

		if (npc.getNpcId() == SCARLET2)
		{
			FRINTEZZA_LAIR.broadcastPacket(new PlaySound(1, "BS01_D", npc));
			startQuestTimer("stop_pc", null, null, 0L);
			startQuestTimer("stop_npc", npc, null, 0L);
			startQuestTimer("morph_16", npc, null, 0L);

			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DEAD);
			long respawnTime = Config.SPAWN_INTERVAL_FRINTEZZA + Rnd.get(-Config.RANDOM_SPAWN_TIME_FRINTEZZA, Config.RANDOM_SPAWN_TIME_FRINTEZZA);
			respawnTime *= 3600000L;

			cancelQuestTimers("spawn_minion");
			cancelQuestTimers("frintezza_despawn");
			cancelQuestTimers("skill01");
			cancelQuestTimers("skill02");
			cancelQuestTimers("skill03");
			cancelQuestTimers("songs_play");
			cancelQuestTimers("songs_effect");
			startQuestTimer("clean", npc, null, 30000);
			startQuestTimer("close", null, null, 0L);
			startQuestTimer("minions_despawn", npc, null, 1000L);
			startQuestTimer("remove_players", null, null, 900000L);
			startQuestTimer("frintezza_unlock", null, null, respawnTime);
			StatSet info = GrandBossManager.getInstance().getStatSet(FRINTEZZA);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(FRINTEZZA, info);
		}
		else if (npc.getNpcId() == 18328)
		{
			if (Rnd.get(100) < 33)
			{
				player.addItem("Quest", DEWDROP_OF_DESTRUCTION, 1, npc, true);
			}

			_killHallAlarmDevice++;
			if (_killHallAlarmDevice == 4)
			{
				for (int i = 25150051; i <= 25150058; i++)
				{
					DoorData.getInstance().getDoor(i).openMe();
				}

				FRINTEZZA_LAIR.broadcastPacket(new CreatureSay(npc.getObjectId(), SayType.SHOUT, npc.getName(), "De-activate the alarm."));
				startQuestTimer("room1_del", npc, null, 100L);
				startQuestTimer("room2_spawn", npc, null, 100L);
				DoorData.getInstance().getDoor(25150042).openMe();
				DoorData.getInstance().getDoor(25150043).openMe();
			}
		}
		else if (npc.getNpcId() == 18333)
		{
			if (Rnd.get(100) < 10)
			{
				player.addItem("Quest", DEWDROP_OF_DESTRUCTION, 1, npc, true);
			}
		}
		else if (npc.getNpcId() == 18339)
		{
			_killDarkChoirPlayer++;
			if (_killDarkChoirPlayer == 4)
			{
				DoorData.getInstance().getDoor(25150042).closeMe();
				DoorData.getInstance().getDoor(25150043).closeMe();
				for (int i = 25150061; i <= 25150070; i++)
				{
					DoorData.getInstance().getDoor(i).openMe();
				}

				startQuestTimer("room2_spawn2", npc, null, 1000L);
			}
		}
		else if (npc.getNpcId() == 18334)
		{
			_killDarkChoirCaptain++;
			if (_killDarkChoirCaptain == 8)
			{
				startQuestTimer("room2_del", npc, null, 100L);
				DoorData.getInstance().getDoor(25150045).openMe();
				DoorData.getInstance().getDoor(25150046).openMe();
				startQuestTimer("waiting", null, null, Config.WAIT_TIME_FRINTEZZA);
				cancelQuestTimers("room_final");
			}
		}
		super.onMyDying(npc, killer);
	}

	private static void throwUp(Creature attacker, double range, SystemMessage msg)
	{
		int mx = attacker.getX(), my = attacker.getY();
		for (Creature target : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
		{
			if ((target == attacker) || (target instanceof Npc && isFrintezzaFriend(((Npc) target).getNpcId())))
			{
				continue;
			}

			double dx = (target.getX() - mx);
			double dy = (target.getY() - my);
			if (dx == 0.0D && dy == 0.0D)
			{
				dx = dy = range / 2.0D;
			}

			double aa = range / Math.sqrt(dx * dx + dy * dy);
			if (aa > 1.0D)
			{
				int x = mx + (int) (dx * aa);
				int y = my + (int) (dy * aa);
				int z = target.getZ();
				target.getAI().tryToIdle();
				target.getAttack().stop();
				target.getCast().stop();
				target.broadcastPacket(new FlyToLocation(target, x, y, z, FlyType.THROW_UP));
				target.setXYZ(x, y, z);
				target.broadcastPacket(new ValidateLocation(target));

				if (msg != null)
				{
					target.sendPacket(msg);
				}

				if (target instanceof Player)
				{
					((Player) target).standUp();
				}
			}
		}
	}

	private static boolean isFrintezzaFriend(int npcId)
	{
		return (npcId >= FRINTEZZA && npcId <= SCARLET_DUMMY);
	}
}