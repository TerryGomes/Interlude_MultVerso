package net.sf.l2j.gameserver.model.spawn;

import java.io.InvalidClassException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.geometry.AShape;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;

/**
 * This class manages the spawn and respawn of {@link Npc}s defined by {@link NpcMaker} in a territory based system.<br>
 * The {@link SpawnLocation} can be:
 * <ul>
 * <li>Fixed coordinates.
 * <li>Random one of defined coordinates.
 * <li>Random coordinate from a {@link Territory} of linked {@link NpcMaker}.
 * </ul>
 */
public final class MultiSpawn extends ASpawn
{
	private static final int RANDOM_SPAWN_LOOP_LIMIT = 20;
	private static final int RANDOM_WALK_LOOP_LIMIT = 3;

	private final NpcMaker _npcMaker;
	private final int _total;
	private final int[][] _coords;

	private final Set<Npc> _npcs = ConcurrentHashMap.newKeySet();

	public MultiSpawn(NpcMaker npcMaker, NpcTemplate template, int total, int respawnDelay, int respawnRandom, int[][] coords, SpawnData spawnData) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		super(template);

		_respawnDelay = Math.max(0, respawnDelay);
		_respawnRandom = Math.min(respawnDelay, Math.max(0, respawnRandom));

		_npcMaker = npcMaker;
		_coords = coords;
		_spawnData = spawnData;

		// Database name is specified -> single spawn (ignore total value, only 1 instance of NPC may exist).
		if (_spawnData != null)
		{
			_total = 1;
		}
		else if (_coords != null)
		{
			_total = total;
			// Coordinates not specified -> random spawn.
		}
		else
		{
			_total = (int) Math.round(total * Config.SPAWN_MULTIPLIER);
		}
	}

	@Override
	public final void setRespawnState(boolean respawn)
	{
		// Respawn state of MultiSpawn is linked with NpcMaker.
		_npcMaker.setRespawnState(respawn);
	}

	@Override
	public final boolean getRespawnState()
	{
		// Respawn state of MultiSpawn is linked with NpcMaker.
		return _npcMaker.getRespawnState();
	}

	@Override
	public SpawnLocation getSpawnLocation()
	{
		// "anywhere", spawn is random, generate random coordinates from territory.
		if (_coords == null)
		{
			SpawnLocation loc = null;
			for (int loop = 0; loop < RANDOM_SPAWN_LOOP_LIMIT; loop++)
			{
				// Generate random location.
				loc = _npcMaker.getTerritory().getRandomLocation();
				// validate it, using banned territory
				if ((loc == null) || (_npcMaker.getBannedTerritory() != null && _npcMaker.getBannedTerritory().isInside(loc)))
				{
					continue;
				}

				return loc;
			}

			// TODO debug purpose, delete me later on
			LOGGER.warn("Can not generate random spawn location for npc {}, maker {}.", getNpcId(), _npcMaker.getName());

			// No location properly generated, return last random.
			return loc;
		}

		// "fixed", spawn is defined by one set of coordinates.
		if (_coords.length == 1)
		{
			final SpawnLocation loc = new SpawnLocation(_coords[0][0], _coords[0][1], _coords[0][2], _coords[0][3]);
			loc.setZ(GeoEngine.getInstance().getHeight(loc));
			return loc;
		}

		// "fixed_random", spawn is defined by more sets of coordinates, pick one random.
		int chance = Rnd.get(100);
		for (int[] coord : _coords)
		{
			chance -= coord[4];
			if (chance < 0)
			{
				final SpawnLocation loc = new SpawnLocation(coord[0], coord[1], coord[2], Rnd.get(65536));
				loc.setZ(GeoEngine.getInstance().getHeight(loc));
				return loc;
			}
		}

		// Should never happen.
		return null;
	}

	@Override
	public Location getRandomWalkLocation(Npc npc, int offset)
	{
		// Generate a new Location object based on Npc position.
		final Location loc = npc.getPosition().clone();

		// Npc position is out of the territory, return a random location based on NpcMaker's Territory.
		final AShape shape = _npcMaker.getTerritory().getShape(loc);
		if (shape == null)
		{
			return _npcMaker.getTerritory().getRandomLocation();
		}

		// Attempt three times to find a random Location matching the offset and banned territory.
		for (int loop = 0; loop < RANDOM_WALK_LOOP_LIMIT; loop++)
		{
			// Generate random location based on offset. Reset each attempt to current Npc position.
			loc.set(npc.getPosition());
			loc.addRandomOffset(offset);

			// Validate location using NpcMaker's territory.
			// Validate location using NpcMaker's banned territory.
			if (!_npcMaker.getTerritory().isInside(loc) || (_npcMaker.getBannedTerritory() != null && _npcMaker.getBannedTerritory().isInside(loc)))
			{
				continue;
			}

			// Validate location using geodata.
			loc.set(GeoEngine.getInstance().getValidLocation(npc, loc));
			return loc;
		}

		// We didn't find a valid Location ; find the current AShape associated to the Npc position, and aim for its center.
		loc.set(GeoEngine.getInstance().getValidLocation(npc, shape.getCenter().getX(), shape.getCenter().getY(), npc.getZ()));

		return loc;
	}

	@Override
	public boolean isInMyTerritory(WorldObject worldObject)
	{
		final Location loc = worldObject.getPosition().clone();

		// Check location using NpcMaker's banned territory.
		if (_npcMaker.getBannedTerritory() != null && _npcMaker.getBannedTerritory().isInside(loc))
		{
			return false;
		}

		// Check location using NpcMaker's territory.
		return _npcMaker.getTerritory().isInside(loc);
	}

	@Override
	public Npc doSpawn(boolean isSummonSpawn)
	{
		final Npc npc = super.doSpawn(isSummonSpawn);
		if (npc == null)
		{
			LOGGER.warn("Can not spawn id {} from maker {}.", getNpcId(), _npcMaker.getName());
			return null;
		}

		_npcs.add(npc);
		return npc;
	}

	@Override
	public void onSpawn(Npc npc)
	{
		synchronized (_npcMaker)
		{
			// Notify NpcMaker.
			_npcMaker.onSpawn(npc);
		}
	}

	@Override
	public void doDelete()
	{
		// Reset spawn data.
		if (_spawnData != null)
		{
			_spawnData.setStatus((byte) -1);
		}

		// Delete privates which were manually spawned via createOnePrivate / createOnePrivateEx.
		for (Npc npc : _npcs)
		{
			if (npc.isMaster())
			{
				npc.getMinions().forEach(Npc::deleteMe);
			}
		}

		// Cancel respawn tasks and delete NPCs.
		_npcs.stream().peek(Npc::cancelRespawn).forEach(Npc::deleteMe);
		_npcs.clear();
	}

	@Override
	public void onDie(Npc npc)
	{
		synchronized (_npcMaker)
		{
			if (getRespawnState() && getRespawnDelay() > 0)
			{
				// Calculate the random delay.
				final long respawnDelay = calculateRespawnDelay() * 1000;

				// Check spawn data and set respawn.
				if (_spawnData != null)
				{
					_spawnData.setRespawn(respawnDelay);
				}

				doSave();
			}
		}
	}

	@Override
	public void onDecay(Npc npc)
	{
		synchronized (_npcMaker)
		{
			// Notify NpcMaker.
			_npcMaker.onDecay(npc);

			if (getRespawnState() && getRespawnDelay() > 0)
			{
				// Calculate the random delay.
				final long respawnDelay = calculateRespawnDelay() * 1000;

				// Schedule respawn.
				npc.scheduleRespawn(respawnDelay);
			}
			else
			{
				// Respawn is disabled, delete NPC.
				_npcs.remove(npc);
			}
		}
	}

	@Override
	public void doRespawn(Npc npc)
	{
		synchronized (_npcMaker)
		{
			if (getRespawnState() && getRespawnDelay() > 0)
			{
				final MultiSpawn spawn = _npcMaker.onRespawn(this);
				if (spawn == this)
				{
					// Same spawn is picked, respawn this NPC.
					super.doRespawn(npc);
				}
				else
				{
					// Another spawn is picked, delete this NPC and spawn new NPC.
					_npcs.remove(npc);
					spawn.doSpawn(false);
				}
			}
			else
			{
				// Respawn was allowed, when NPC died -> respawn was scheduled.
				// Respawn is not allowed now -> delete NPC.
				_npcs.remove(npc);
			}
		}
	}

	@Override
	public String toString()
	{
		return "MultiSpawn [id=" + getNpcId() + "]";
	}

	@Override
	public String getDescription()
	{
		return "NpcMaker: " + _npcMaker.getName();
	}

	@Override
	public void updateSpawnData()
	{
		if (_spawnData == null)
		{
			return;
		}

		_npcs.stream().forEach(npc -> _spawnData.setStats(npc));
	}

	public NpcMaker getNpcMaker()
	{
		return _npcMaker;
	}

	public int[][] getCoords()
	{
		return _coords;
	}

	public int getTotal()
	{
		return _total;
	}

	public Set<Npc> getNpcs()
	{
		return _npcs;
	}

	public int getNpcsAmount()
	{
		return _npcs.size();
	}

	public long getSpawned()
	{
		return _npcs.stream().filter(n -> !n.isDecayed()).count();
	}

	public long getDecayed()
	{
		return _npcs.stream().filter(n -> n.isDecayed()).count();
	}

	/**
	 * Respawns all {@link Npc}s of this {@link MultiSpawn}.
	 */
	public void doRespawn()
	{
		_npcs.stream().filter(Npc::isDecayed).forEach(npc -> doRespawn(npc));
	}
}