package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class VanHalter extends AttackableAIScript
{
	protected static final CLogger LOGGER = new CLogger(VanHalter.class.getName());

	private static final BossZone VANHALTER_LAIR = ZoneManager.getInstance().getZoneById(110016, BossZone.class);

	protected Map<Integer, List<Player>> _bleedingPlayers = new ConcurrentHashMap<>();

	protected List<Spawn> _royalGuardSpawn = new ArrayList<>();
	protected List<Spawn> _royalGuardCaptainSpawn = new ArrayList<>();
	protected List<Spawn> _royalGuardHelperSpawn = new ArrayList<>();
	protected List<Spawn> _triolRevelationSpawn = new ArrayList<>();
	protected List<Spawn> _triolRevelationAlive = new ArrayList<>();
	protected List<Spawn> _guardOfAltarSpawn = new ArrayList<>();
	public Map<Integer, Spawn> _cameraMarkerSpawn = new ConcurrentHashMap<>();
	protected Spawn _ritualOfferingSpawn = null;
	protected Spawn _ritualSacrificeSpawn = null;
	protected Spawn _vanHalterSpawn = null;

	protected List<Npc> _royalGuard = new ArrayList<>();
	protected List<Npc> _royalGuardCaptain = new ArrayList<>();
	protected List<Npc> _royalGuardHepler = new ArrayList<>();
	protected List<Npc> _triolRevelation = new ArrayList<>();
	protected List<Npc> _guardOfAltar = new ArrayList<>();
	protected Map<Integer, Npc> _cameraMarker = new ConcurrentHashMap<>();
	protected List<Door> _doorOfAltar = new ArrayList<>();
	protected List<Door> _doorOfSacrifice = new ArrayList<>();
	protected Npc _ritualOffering = null;
	protected Npc _ritualSacrifice = null;
	protected GrandBoss _vanHalter = null;

	protected ScheduledFuture<?> _movieTask = null;
	protected ScheduledFuture<?> _closeDoorOfAltarTask = null;
	protected ScheduledFuture<?> _openDoorOfAltarTask = null;
	protected ScheduledFuture<?> _lockUpDoorOfAltarTask = null;
	protected ScheduledFuture<?> _callRoyalGuardHelperTask = null;
	protected ScheduledFuture<?> _timeUpTask = null;
	protected ScheduledFuture<?> _intervalTask = null;
	protected ScheduledFuture<?> _halterEscapeTask = null;
	protected ScheduledFuture<?> _setBleedTask = null;

	boolean _isLocked = false;
	boolean _isHalterSpawned = false;
	boolean _isSacrificeSpawned = false;
	boolean _isCaptainSpawned = false;
	boolean _isHelperCalled = false;

	private static final int ANDREAS_VAN_HALTER = 29062;
	private static final int ANDREAS_CAPTAIN = 22188;

	private static final byte WAITING = 0;
	private static final byte FIGHT = 1;
	private static final byte DEAD = 2;

	private static final int[] TRIOLS =
	{
		32058,
		32059,
		32060,
		32061,
		32062,
		32063,
		32064,
		32065,
		32066
	};

	public VanHalter()
	{
		super("ai/boss");

		_isLocked = false;
		_isCaptainSpawned = false;
		_isHelperCalled = false;
		_isHalterSpawned = false;

		_doorOfAltar.add(DoorData.getInstance().getDoor(19160014));
		_doorOfAltar.add(DoorData.getInstance().getDoor(19160015));
		openDoorOfAltar(true);
		_doorOfSacrifice.add(DoorData.getInstance().getDoor(19160016));
		_doorOfSacrifice.add(DoorData.getInstance().getDoor(19160017));
		closeDoorOfSacrifice();

		loadRoyalGuard();
		loadTriolRevelation();
		loadRoyalGuardCaptain();
		loadRoyalGuardHelper();
		loadGuardOfAltar();
		loadVanHalter();
		loadRitualOffering();
		loadRitualSacrifice();

		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}

		_timeUpTask = ThreadPool.schedule(new TimeUp(), 21600);

		if (_setBleedTask != null)
		{
			_setBleedTask.cancel(false);
		}

		_setBleedTask = ThreadPool.schedule(new Bleeding(), 2000L);

		StatSet info = GrandBossManager.getInstance().getStatSet(ANDREAS_VAN_HALTER);
		int _state = GrandBossManager.getInstance().getBossStatus(ANDREAS_VAN_HALTER);
		if (_state == WAITING)
		{
			setupAltar(false);
		}
		else if (_state == FIGHT)
		{
			setupAltar(true);
		}
		else if (_state == DEAD)
		{
			long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
			if (temp > 0)
			{
				_intervalTask = ThreadPool.schedule(new Init(false), temp);
			}
			else
			{
				setupAltar(false);
			}
		}

		_cameraMarkerSpawn.clear();
		try
		{
			NpcTemplate template = NpcData.getInstance().getTemplate(13014);

			Spawn tempSpawn = new Spawn(template);
			tempSpawn.setLoc(-16397, -54119, -10445, 16384);
			_cameraMarkerSpawn.put(1, tempSpawn);

			tempSpawn.setLoc(-16397, -54119, -10051, 16384);
			_cameraMarkerSpawn.put(2, tempSpawn);

			tempSpawn.setLoc(-16397, -54119, -9741, 16384);
			_cameraMarkerSpawn.put(3, tempSpawn);

			tempSpawn.setLoc(-16397, -54119, -9394, 16384);
			_cameraMarkerSpawn.put(4, tempSpawn);

			tempSpawn.setLoc(-16397, -54119, -8739, 16384);
			_cameraMarkerSpawn.put(5, tempSpawn);

			SpawnManager.getInstance().addSpawn(tempSpawn);
		}
		catch (Exception e)
		{
			LOGGER.warn("VanHalterManager: Error in spawning mobs." + e.getMessage(), e);
		}

		if (Config.DEVELOPER)
		{
			LOGGER.info("VanHalterManager : State of High Priestess van Halter is " + _state + ".");
		}
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(ANDREAS_VAN_HALTER);
		addMyDying(ANDREAS_VAN_HALTER, ANDREAS_CAPTAIN);

		for (int TRIOL : TRIOLS)
		{
			addMyDying(TRIOL);
		}
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		int npcId = npc.getNpcId();
		if (npcId == ANDREAS_VAN_HALTER)
		{
			int maxHp = npc.getStatus().getMaxHp();
			double curHp = npc.getStatus().getHp();

			if (((curHp / maxHp) * 100) <= 20)
			{
				callRoyalGuardHelper();
			}
		}

		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		int npcId = npc.getNpcId();

		if (npcId == 32058 || npcId == 32059 || npcId == 32060 || npcId == 32061 || npcId == 32062 || npcId == 32063 || npcId == 32064 || npcId == 32065 || npcId == 32066)
		{
			removeBleeding(npcId);
		}

		checkTriolRevelationDestroy();

		if (npcId == ANDREAS_CAPTAIN)
		{
			checkRoyalGuardCaptainDestroy();
		}

		if (npcId == ANDREAS_VAN_HALTER)
		{
			GrandBossManager.getInstance().setBossStatus(ANDREAS_VAN_HALTER, DEAD);
			enterInterval();
		}

		super.onMyDying(npc, killer);
	}

	protected void loadRoyalGuard()
	{
		_royalGuardSpawn.clear();

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid between ? and ? ORDER BY id"))
		{
			statement.setInt(1, 22175);
			statement.setInt(2, 22176);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template != null)
					{
						Spawn spawnDat = new Spawn(template);
						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

						SpawnManager.getInstance().addSpawn(spawnDat);
						_royalGuardSpawn.add(spawnDat);
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadRoyalGuard: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadRoyalGuard: Loaded " + _royalGuardSpawn.size() + " Royal Guard spawn locations.");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			LOGGER.warn("VanHalterManager.loadRoyalGuard: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnRoyalGuard()
	{
		if (!_royalGuard.isEmpty())
		{
			deleteRoyalGuard();
		}

		for (Spawn rgs : _royalGuardSpawn)
		{
			rgs.setRespawnState(true);
			_royalGuard.add(rgs.doSpawn(true));
		}
	}

	protected void deleteRoyalGuard()
	{
		for (Npc rg : _royalGuard)
		{
			rg.getSpawn().setRespawnState(false);
			rg.getSpawn().doDelete();
			rg.deleteMe();
		}

		_royalGuard.clear();
	}

	protected void loadTriolRevelation()
	{
		_triolRevelationSpawn.clear();

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid between ? and ? ORDER BY id"))
		{
			statement.setInt(1, 32058);
			statement.setInt(2, 32068);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template != null)
					{
						Spawn spawnDat = new Spawn(template);
						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

						SpawnManager.getInstance().addSpawn(spawnDat);
						_triolRevelationSpawn.add(spawnDat);
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadTriolRevelation: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadTriolRevelation: Loaded " + _triolRevelationSpawn.size() + " Triol's Revelation spawn locations.");
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("VanHalterManager.loadTriolRevelation: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnTriolRevelation()
	{
		if (!_triolRevelation.isEmpty())
		{
			deleteTriolRevelation();
		}

		for (Spawn trs : _triolRevelationSpawn)
		{
			Monster triol = (Monster) trs.doSpawn(true);
			triol.setIsParalyzed(true);
			_triolRevelation.add(triol);

			if ((trs.getNpcId() != 32067) && (trs.getNpcId() != 32068))
			{
				_triolRevelationAlive.add(trs);
			}
		}
	}

	protected void deleteTriolRevelation()
	{
		for (Npc tr : _triolRevelation)
		{
			tr.getSpawn().setRespawnState(false);
			tr.deleteMe();
		}
		_triolRevelation.clear();
		_bleedingPlayers.clear();
	}

	protected void loadRoyalGuardCaptain()
	{
		_royalGuardCaptainSpawn.clear();

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id"))
		{
			statement.setInt(1, 22188);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template != null)
					{
						Spawn spawnDat = new Spawn(template);

						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

						SpawnManager.getInstance().addSpawn(spawnDat);
						_royalGuardCaptainSpawn.add(spawnDat);
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadRoyalGuardCaptain: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadRoyalGuardCaptain: Loaded " + _royalGuardCaptainSpawn.size() + " Royal Guard Captain spawn locations.");
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("VanHalterManager.loadRoyalGuardCaptain: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnRoyalGuardCaptain()
	{
		if (!_royalGuardCaptain.isEmpty())
		{
			deleteRoyalGuardCaptain();
		}

		for (Spawn trs : _royalGuardCaptainSpawn)
		{
			trs.setRespawnState(true);
			_royalGuardCaptain.add(trs.doSpawn(true));
		}

		_isCaptainSpawned = true;
	}

	protected void deleteRoyalGuardCaptain()
	{
		for (Npc tr : _royalGuardCaptain)
		{
			tr.getSpawn().setRespawnState(false);
			tr.deleteMe();
		}

		_royalGuardCaptain.clear();
	}

	protected void loadRoyalGuardHelper()
	{
		_royalGuardHelperSpawn.clear();

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id"))
		{
			statement.setInt(1, 22191);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template != null)
					{
						Spawn spawnDat = new Spawn(template);
						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

						SpawnManager.getInstance().addSpawn(spawnDat);
						_royalGuardHelperSpawn.add(spawnDat);
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadRoyalGuardHelper: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadRoyalGuardHelper: Loaded " + _royalGuardHelperSpawn.size() + " Royal Guard Helper spawn locations.");
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("VanHalterManager.loadRoyalGuardHelper: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnRoyalGuardHepler()
	{
		for (Spawn trs : _royalGuardHelperSpawn)
		{
			trs.setRespawnState(true);
			_royalGuardHepler.add(trs.doSpawn(true));
		}
	}

	protected void deleteRoyalGuardHepler()
	{
		for (Npc tr : _royalGuardHepler)
		{
			tr.getSpawn().setRespawnState(false);
			tr.deleteMe();
		}
		_royalGuardHepler.clear();
	}

	protected void loadGuardOfAltar()
	{
		_guardOfAltarSpawn.clear();

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id"))
		{
			statement.setInt(1, 32051);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template != null)
					{
						Spawn spawnDat = new Spawn(template);
						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						SpawnManager.getInstance().addSpawn(spawnDat);
						_guardOfAltarSpawn.add(spawnDat);
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadGuardOfAltar: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadGuardOfAltar: Loaded " + _guardOfAltarSpawn.size() + " Guard Of Altar spawn locations.");
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("VanHalterManager.loadGuardOfAltar: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnGuardOfAltar()
	{
		if (!_guardOfAltar.isEmpty())
		{
			deleteGuardOfAltar();
		}

		for (Spawn trs : _guardOfAltarSpawn)
		{
			trs.setRespawnState(true);
			_guardOfAltar.add(trs.doSpawn(true));
		}
	}

	protected void deleteGuardOfAltar()
	{
		for (Npc tr : _guardOfAltar)
		{
			tr.getSpawn().setRespawnState(false);
			tr.deleteMe();
		}

		_guardOfAltar.clear();
	}

	protected void loadVanHalter()
	{
		_vanHalterSpawn = null;

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id"))
		{
			statement.setInt(1, 29062);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template1 = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template1 != null)
					{
						Spawn spawnDat = new Spawn(template1);
						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						SpawnManager.getInstance().addSpawn(spawnDat);
						_vanHalterSpawn = spawnDat;
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadVanHalter: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadVanHalter: Loaded High Priestess van Halter spawn locations.");
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("VanHalterManager.loadVanHalter: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnVanHalter()
	{
		_vanHalter = (GrandBoss) _vanHalterSpawn.doSpawn(true);
		_vanHalter.setIsImmobilized(true);
		_vanHalter.setInvul(true);
		_isHalterSpawned = true;
		GrandBossManager.getInstance().addBoss(_vanHalter);
	}

	protected void deleteVanHalter()
	{
		if (_vanHalter == null)
		{
			return;
		}

		_vanHalter.setIsImmobilized(false);
		_vanHalter.setInvul(false);
		_vanHalter.getSpawn().setRespawnState(false);
		_vanHalter.deleteMe();
	}

	protected void loadRitualOffering()
	{
		_ritualOfferingSpawn = null;

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id,count,npc_templateid,locx,locy,locz,heading,respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid=? ORDER BY id"))
		{
			statement.setInt(1, 32038);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template != null)
					{
						Spawn spawnDat = new Spawn(template);
						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						SpawnManager.getInstance().addSpawn(spawnDat);
						_ritualOfferingSpawn = spawnDat;
						continue;
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadRitualOffering: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadRitualOffering: Loaded Ritual Offering spawn locations.");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			LOGGER.warn("VanHalterManager.loadRitualOffering: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnRitualOffering()
	{
		_ritualOffering = _ritualOfferingSpawn.doSpawn(true);
		_ritualOffering.setIsImmobilized(true);
		_ritualOffering.setInvul(true);
		_ritualOffering.setIsParalyzed(true);
	}

	protected void deleteRitualOffering()
	{
		if (_ritualOffering == null)
		{
			return;
		}

		_ritualOffering.setIsImmobilized(false);
		_ritualOffering.setInvul(false);
		_ritualOffering.setIsParalyzed(false);
		_ritualOffering.getSpawn().setRespawnState(false);
		_ritualOffering.deleteMe();
	}

	protected void loadRitualSacrifice()
	{
		_ritualSacrificeSpawn = null;

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id,count,npc_templateid,locx,locy,locz,heading,respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid=? ORDER BY id"))
		{
			statement.setInt(1, 22195);

			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(rset.getInt("npc_templateid"));
					if (template != null)
					{
						Spawn spawnDat = new Spawn(template);
						spawnDat.setLoc(rset.getInt("locx"), rset.getInt("locy"), rset.getInt("locz"), rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						SpawnManager.getInstance().addSpawn(spawnDat);
						_ritualSacrificeSpawn = spawnDat;
					}
					else
					{
						LOGGER.warn("VanHalterManager.loadRitualSacrifice: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
			}

			if (Config.DEVELOPER)
			{
				LOGGER.info("VanHalterManager.loadRitualSacrifice: Loaded Ritual Sacrifice spawn locations.");
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("VanHalterManager.loadRitualSacrifice: Spawn could not be initialized: " + e);
		}
	}

	protected void spawnRitualSacrifice()
	{
		_ritualSacrifice = _ritualSacrificeSpawn.doSpawn(true);
		_ritualSacrifice.setIsImmobilized(true);
		_ritualSacrifice.setInvul(true);
		_isSacrificeSpawned = true;
	}

	protected void deleteRitualSacrifice()
	{
		if (!_isSacrificeSpawned)
		{
			return;
		}

		_ritualSacrifice.getSpawn().setRespawnState(false);
		_ritualSacrifice.deleteMe();
		_isSacrificeSpawned = false;
	}

	protected void spawnCameraMarker()
	{
		_cameraMarker.clear();
		for (int i = 1; i <= _cameraMarkerSpawn.size(); i++)
		{
			_cameraMarker.put(i, _cameraMarkerSpawn.get(i).doSpawn(true));
			_cameraMarker.get(i).getSpawn().setRespawnState(false);
			_cameraMarker.get(i).setIsImmobilized(true);
		}
	}

	protected void deleteCameraMarker()
	{
		if (_cameraMarker.isEmpty())
		{
			return;
		}

		for (int i = 1; i <= _cameraMarker.size(); i++)
		{
			_cameraMarker.get(i).deleteMe();
		}

		_cameraMarker.clear();
	}

	protected void openDoorOfAltar(boolean loop)
	{
		for (Door door : _doorOfAltar)
		{
			try
			{
				door.openMe();
			}
			catch (Exception e)
			{
				LOGGER.error(e.getMessage(), e);
			}
		}

		if (loop)
		{
			_isLocked = false;

			if (_closeDoorOfAltarTask != null)
			{
				_closeDoorOfAltarTask.cancel(false);
			}
			_closeDoorOfAltarTask = null;
			_closeDoorOfAltarTask = ThreadPool.schedule(new CloseDoorOfAltar(), 180000);
		}
		else
		{
			if (_closeDoorOfAltarTask != null)
			{
				_closeDoorOfAltarTask.cancel(false);
			}
			_closeDoorOfAltarTask = null;
		}
	}

	protected class OpenDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			openDoorOfAltar(true);
		}
	}

	protected void closeDoorOfAltar(boolean loop)
	{
		for (Door door : _doorOfAltar)
		{
			door.closeMe();
		}

		if (loop)
		{
			if (_openDoorOfAltarTask != null)
			{
				_openDoorOfAltarTask.cancel(false);
			}

			_openDoorOfAltarTask = null;
			_openDoorOfAltarTask = ThreadPool.schedule(new OpenDoorOfAltar(), 180000);
		}
		else
		{
			if (_openDoorOfAltarTask != null)
			{
				_openDoorOfAltarTask.cancel(false);
			}
			_openDoorOfAltarTask = null;
		}
	}

	protected class CloseDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			closeDoorOfAltar(true);
		}
	}

	protected void openDoorOfSacrifice()
	{
		for (Door door : _doorOfSacrifice)
		{
			try
			{
				door.openMe();
			}
			catch (Exception e)
			{
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	protected void closeDoorOfSacrifice()
	{
		for (Door door : _doorOfSacrifice)
		{
			try
			{
				door.closeMe();
			}
			catch (Exception e)
			{
				LOGGER.warn(e.getMessage(), e);
			}
		}
	}

	public void checkTriolRevelationDestroy()
	{
		if (_isCaptainSpawned)
		{
			return;
		}

		boolean isTriolRevelationDestroyed = true;
		for (Spawn tra : _triolRevelationAlive)
		{
			if (!tra.getNpc().isDead())
			{
				isTriolRevelationDestroyed = false;
			}
		}

		if (isTriolRevelationDestroyed)
		{
			spawnRoyalGuardCaptain();
		}
	}

	public void checkRoyalGuardCaptainDestroy()
	{
		if (!_isHalterSpawned)
		{
			return;
		}

		deleteRoyalGuard();
		deleteRoyalGuardCaptain();
		spawnGuardOfAltar();

		_vanHalter.setIsImmobilized(true);
		_vanHalter.setInvul(true);
		spawnCameraMarker();

		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;

		_movieTask = ThreadPool.schedule(new Movie(1), 20000);
	}

	protected void combatBeginning()
	{
		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = ThreadPool.schedule(new TimeUp(), 7200000);
	}

	public void callRoyalGuardHelper()
	{
		if (!_isHelperCalled)
		{
			_isHelperCalled = true;
			_halterEscapeTask = ThreadPool.schedule(new HalterEscape(), 500);
			_callRoyalGuardHelperTask = ThreadPool.schedule(new CallRoyalGuardHelper(), 1000);
		}
	}

	protected class CallRoyalGuardHelper implements Runnable
	{
		@Override
		public void run()
		{
			spawnRoyalGuardHepler();

			if ((_royalGuardHepler.size() <= 6) && !_vanHalter.isDead())
			{
				if (_callRoyalGuardHelperTask != null)
				{
					_callRoyalGuardHelperTask.cancel(false);
				}

				_callRoyalGuardHelperTask = ThreadPool.schedule(new CallRoyalGuardHelper(), 10000);
			}
			else
			{
				if (_callRoyalGuardHelperTask != null)
				{
					_callRoyalGuardHelperTask.cancel(false);
				}

				_callRoyalGuardHelperTask = null;
			}
		}
	}

	protected class HalterEscape implements Runnable
	{
		@Override
		public void run()
		{
			if ((_royalGuardHepler.size() <= 6) && !_vanHalter.isDead())
			{
				if (_vanHalter.isAfraid())
				{
					_vanHalter.stopEffects(EffectType.FEAR);
				}
				else
				{
					_vanHalter.fleeFrom(_ritualOffering, 500);
					if (_vanHalter.getZ() >= -10476)
					{
						Location pos = new Location(-16397, -53308, -10448);
						if (!((_vanHalter.getX() == pos.getX()) && (_vanHalter.getY() == pos.getY())))
						{
							_vanHalter.stopEffects(EffectType.FEAR);
						}
						else
						{
							_vanHalter.getAI().tryToMoveTo(pos, null);
						}
					}
					else if (_vanHalter.getX() >= -16397)
					{
						Location pos = new Location(-15548, -54830, -10475);
						_vanHalter.getAI().tryToMoveTo(pos, null);
					}
					else
					{
						Location pos = new Location(-17248, -54830, -10475);
						_vanHalter.getAI().tryToMoveTo(pos, null);
					}
				}

				if (_halterEscapeTask != null)
				{
					_halterEscapeTask.cancel(false);
				}

				_halterEscapeTask = ThreadPool.schedule(new HalterEscape(), 5000);
			}
			else
			{
				_vanHalter.stopEffects(EffectType.FEAR);
				if (_halterEscapeTask != null)
				{
					_halterEscapeTask.cancel(false);
				}

				_halterEscapeTask = null;
			}
		}
	}

	protected void addBleeding()
	{
		L2Skill bleed = SkillTable.getInstance().getInfo(4615, 12);

		for (Npc tr : _triolRevelation)
		{
			ArrayList<Player> bpc = new ArrayList<>();

			for (Player pc : tr.getKnownTypeInRadius(Player.class, 500))
			{
				if (pc.getFirstEffect(bleed) == null)
				{
					bleed.getEffects(tr, pc);
					tr.broadcastPacket(new MagicSkillUse(tr, pc, bleed.getId(), 12, 1, 1));
				}

				bpc.add(pc);
			}
			_bleedingPlayers.remove(tr.getNpcId());
			_bleedingPlayers.put(tr.getNpcId(), bpc);
		}
	}

	public void removeBleeding(int npcId)
	{
		if (_bleedingPlayers.get(npcId) == null)
		{
			return;
		}

		for (Player pc : _bleedingPlayers.get(npcId))
		{
			if (pc.getFirstEffect(EffectType.DMG_OVER_TIME) != null)
			{
				pc.stopEffects(EffectType.DMG_OVER_TIME);
			}
		}
		_bleedingPlayers.remove(npcId);
	}

	protected class Bleeding implements Runnable
	{
		@Override
		public void run()
		{
			addBleeding();

			if (_setBleedTask != null)
			{
				_setBleedTask.cancel(false);
			}

			_setBleedTask = ThreadPool.schedule(new Bleeding(), 2000);
		}
	}

	public void enterInterval()
	{
		if (_callRoyalGuardHelperTask != null)
		{
			_callRoyalGuardHelperTask.cancel(false);
		}
		_callRoyalGuardHelperTask = null;

		if (_closeDoorOfAltarTask != null)
		{
			_closeDoorOfAltarTask.cancel(false);
		}
		_closeDoorOfAltarTask = null;

		if (_halterEscapeTask != null)
		{
			_halterEscapeTask.cancel(false);
		}
		_halterEscapeTask = null;

		if (_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}
		_intervalTask = null;

		if (_lockUpDoorOfAltarTask != null)
		{
			_lockUpDoorOfAltarTask.cancel(false);
		}
		_lockUpDoorOfAltarTask = null;

		if (_movieTask != null)
		{
			_movieTask.cancel(false);
		}
		_movieTask = null;

		if (_openDoorOfAltarTask != null)
		{
			_openDoorOfAltarTask.cancel(false);
		}
		_openDoorOfAltarTask = null;

		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;

		deleteVanHalter();
		deleteRoyalGuardHepler();
		deleteRoyalGuardCaptain();
		deleteRoyalGuard();
		deleteRitualOffering();
		deleteRitualSacrifice();
		deleteGuardOfAltar();

		if (_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}

		if (GrandBossManager.getInstance().getBossStatus(ANDREAS_VAN_HALTER) == DEAD)
		{
			int respawnTime = Config.SPAWN_INTERVAL_HALTER + Rnd.get(-Config.RANDOM_SPAWN_TIME_HALTER, Config.RANDOM_SPAWN_TIME_HALTER);
			StatSet info = GrandBossManager.getInstance().getStatSet(ANDREAS_VAN_HALTER);
			info.set("respawn_time", (System.currentTimeMillis() + respawnTime));
			GrandBossManager.getInstance().setStatSet(ANDREAS_VAN_HALTER, info);
			openDoorOfAltar(false);
			openDoorOfSacrifice();
		}
		else if (GrandBossManager.getInstance().getBossStatus(ANDREAS_VAN_HALTER) == FIGHT)
		{
			_intervalTask = ThreadPool.schedule(new Init(false), 3600000);
			VANHALTER_LAIR.oustAllPlayers();
			closeDoorOfAltar(false);
			closeDoorOfSacrifice();
		}
	}

	private class Init implements Runnable
	{
		public boolean isFighting;

		public Init(boolean fight)
		{
			isFighting = fight;
		}

		@Override
		public void run()
		{
			setupAltar(isFighting);
		}
	}

	public void setupAltar(boolean isFighting)
	{
		if (_callRoyalGuardHelperTask != null)
		{
			_callRoyalGuardHelperTask.cancel(false);
		}
		_callRoyalGuardHelperTask = null;

		if (_closeDoorOfAltarTask != null)
		{
			_closeDoorOfAltarTask.cancel(false);
		}
		_closeDoorOfAltarTask = null;

		if (_halterEscapeTask != null)
		{
			_halterEscapeTask.cancel(false);
		}
		_halterEscapeTask = null;

		if (_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}
		_intervalTask = null;

		if (_lockUpDoorOfAltarTask != null)
		{
			_lockUpDoorOfAltarTask.cancel(false);
		}
		_lockUpDoorOfAltarTask = null;

		if (_movieTask != null)
		{
			_movieTask.cancel(false);
		}
		_movieTask = null;

		if (_openDoorOfAltarTask != null)
		{
			_openDoorOfAltarTask.cancel(false);
		}
		_openDoorOfAltarTask = null;

		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;

		deleteVanHalter();
		deleteTriolRevelation();
		deleteRoyalGuardHepler();
		deleteRoyalGuardCaptain();
		deleteRoyalGuard();
		deleteRitualSacrifice();
		deleteRitualOffering();
		deleteGuardOfAltar();
		deleteCameraMarker();

		_isLocked = false;
		_isCaptainSpawned = false;
		_isHelperCalled = false;
		_isHalterSpawned = false;

		VANHALTER_LAIR.oustAllPlayers();

		closeDoorOfSacrifice();
		openDoorOfAltar(true);

		spawnTriolRevelation();
		spawnRoyalGuard();
		spawnRitualOffering();
		spawnVanHalter();

		if (isFighting)
		{
			StatSet info = GrandBossManager.getInstance().getStatSet(ANDREAS_VAN_HALTER);
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");
			ThreadPool.schedule(() ->
			{
				try
				{
					_vanHalter.getStatus().setHpMp(hp, mp);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}, 100L);

			if (_timeUpTask != null)
			{
				_timeUpTask.cancel(false);
			}
			_timeUpTask = ThreadPool.schedule(new TimeUp(), 7200000);
		}
	}

	protected class TimeUp implements Runnable
	{
		@Override
		public void run()
		{
			enterInterval();
		}
	}

	private class Movie implements Runnable
	{
		private final int _distance = 6502500;
		private final int _taskId;

		public Movie(int taskId)
		{
			_taskId = taskId;
		}

		@Override
		public void run()
		{
			_vanHalter.getPosition().setHeading(16384);
			_vanHalter.setTarget(_ritualOffering);

			switch (_taskId)
			{
				case 1:
					GrandBossManager.getInstance().setBossStatus(ANDREAS_VAN_HALTER, WAITING);
					openDoorOfSacrifice();

					CreatureSay cs = new CreatureSay(0, SayType.SHOUT, "Altar's Gatekeeper", "The door of the 3rd floor in the altar has opened.");
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						pc.sendPacket(cs);
					}

					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 50, 90, 0, 0, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(2), 16L);
					break;
				case 2:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(5)) <= _distance)
						{
							_cameraMarker.get(5).broadcastPacket(new SpecialCamera(_cameraMarker.get(5).getObjectId(), 1842, 100, -3, 0, 15000, 0, 500, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(3), 1L);
					break;
				case 3:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(5)) <= _distance)
						{
							_cameraMarker.get(5).broadcastPacket(new SpecialCamera(_cameraMarker.get(5).getObjectId(), 1861, 97, -10, 1500, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(4), 1500L);
					break;
				case 4:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(4)) <= _distance)
						{
							_cameraMarker.get(4).broadcastPacket(new SpecialCamera(_cameraMarker.get(4).getObjectId(), 1876, 97, 12, 0, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(5), 1L);
					break;
				case 5:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(4)) <= _distance)
						{
							_cameraMarker.get(4).broadcastPacket(new SpecialCamera(_cameraMarker.get(4).getObjectId(), 1839, 94, 0, 1500, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(6), 1500L);
					break;
				case 6:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(3)) <= _distance)
						{
							_cameraMarker.get(3).broadcastPacket(new SpecialCamera(_cameraMarker.get(3).getObjectId(), 1872, 94, 15, 0, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(7), 1L);
					break;
				case 7:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(3)) <= _distance)
						{
							_cameraMarker.get(3).broadcastPacket(new SpecialCamera(_cameraMarker.get(3).getObjectId(), 1839, 92, 0, 1500, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(8), 1500L);
					break;
				case 8:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(2)) <= _distance)
						{
							_cameraMarker.get(2).broadcastPacket(new SpecialCamera(_cameraMarker.get(2).getObjectId(), 1872, 92, 15, 0, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(9), 1L);
					break;
				case 9:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(2)) <= _distance)
						{
							_cameraMarker.get(2).broadcastPacket(new SpecialCamera(_cameraMarker.get(2).getObjectId(), 1839, 90, 5, 1500, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(10), 1500L);
					break;
				case 10:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(1)) <= _distance)
						{
							_cameraMarker.get(1).broadcastPacket(new SpecialCamera(_cameraMarker.get(1).getObjectId(), 1872, 90, 5, 0, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(11), 1L);
					break;
				case 11:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_cameraMarker.get(1)) <= _distance)
						{
							_cameraMarker.get(1).broadcastPacket(new SpecialCamera(_cameraMarker.get(1).getObjectId(), 2002, 90, 2, 1500, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(12), 2000L);
					break;
				case 12:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 50, 90, 10, 0, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(13), 1000L);
					break;
				case 13:
					L2Skill skill = SkillTable.getInstance().getInfo(1168, 7);

					_ritualOffering.setInvul(false);
					_vanHalter.setTarget(_ritualOffering);
					_vanHalter.getCast().doCast(skill, _ritualOffering, null);

					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(14), 4700L);
					break;
				case 14:
					_ritualOffering.setInvul(false);
					_ritualOffering.getStatus().reduceHp(_ritualOffering.getStatus().getMaxHp() + 1, _vanHalter);

					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(15), 4300L);
					break;
				case 15:
					spawnRitualSacrifice();
					deleteRitualOffering();

					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 100, 90, 15, 1500, 15000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(16), 2000L);
					break;
				case 16:
					for (Player pc : VANHALTER_LAIR.getKnownTypeInside(Player.class))
					{
						if (pc.distance2D(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 5200, 90, -10, 9500, 6000, 0, 0, 1, 0));
						}
					}
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(17), 6000L);
					break;
				case 17:
					deleteRitualSacrifice();
					deleteCameraMarker();

					_vanHalter.setInvul(false);
					_vanHalter.setIsImmobilized(false);

					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPool.schedule(new Movie(18), 1000L);
					break;
				case 18:
					combatBeginning();
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
			}
		}
	}
}