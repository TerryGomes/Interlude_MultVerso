package net.sf.l2j.gameserver.model.spawn;

import java.io.InvalidClassException;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;

/**
 * This class manages the spawn and respawn of a single {@link Npc} at given {@link SpawnLocation}.
 */
public final class Spawn extends ASpawn
{
	private final SpawnLocation _loc = new SpawnLocation(0, 0, 0, 0);

	private boolean _respawnEnabled;
	private Npc _npc;

	public Spawn(int id) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		super(id);
	}

	public Spawn(NpcTemplate template) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		super(template);
	}

	@Override
	public final void setRespawnState(boolean respawn)
	{
		_respawnEnabled = respawn;
	}

	@Override
	public final boolean getRespawnState()
	{
		return _respawnEnabled;
	}

	@Override
	public SpawnLocation getSpawnLocation()
	{
		// Create spawn location (this object is directly assigned to Npc, while Spawn is keeping its own).
		final SpawnLocation loc = _loc.clone();

		// FIXME Temporary fix: when the spawn Z and geo Z differs more than 200, use spawn Z coordinate.
		int z = GeoEngine.getInstance().getHeight(loc);
		if (Math.abs(z - loc.getZ()) <= 200)
		{
			loc.setZ(z);
		}

		// Get random heading, if not defined.
		if (loc.getHeading() < 0)
		{
			loc.setHeading(Rnd.get(65536));
		}

		return loc;
	}

	@Override
	public Location getRandomWalkLocation(Npc npc, int offset)
	{
		// Get location object (spawn location).
		final Location loc = _loc.clone();

		// Generate random location based on offset.
		loc.addRandomOffset(offset);

		// Validate location using geodata.
		loc.set(GeoEngine.getInstance().getValidLocation(npc, loc));
		return loc;
	}

	@Override
	public boolean isInMyTerritory(WorldObject worldObject)
	{
		return worldObject.getPosition().isIn3DRadius(_loc, Config.MAX_DRIFT_RANGE);
	}

	@Override
	public Npc doSpawn(boolean isSummonSpawn)
	{
		// Spawn NPC.
		_npc = super.doSpawn(isSummonSpawn);
		if (_npc == null)
		{
			LOGGER.warn("Can not spawn id {} from loc {}.", getNpcId(), _loc);
			// Add Spawn to SpawnManager.
		}
		else
		{
			SpawnManager.getInstance().addSpawn(this);
		}

		return _npc;
	}

	@Override
	public void doDelete()
	{
		if (_npc == null)
		{
			return;
		}

		// Reset spawn data.
		if (_spawnData != null)
		{
			_spawnData.setStatus((byte) -1);
		}

		// Delete privates which were manually spawned via createOnePrivate / createOnePrivateEx.
		if (_npc.isMaster())
		{
			_npc.getMinions().forEach(Npc::deleteMe);
		}

		// Set respawn state to false, before the NPC is deleted.
		setRespawnState(false);

		// Cancel respawn task and delete NPC.
		_npc.cancelRespawn();
		_npc.deleteMe();
		_npc = null;
	}

	@Override
	public void onDecay(Npc npc)
	{
		// NPC can be respawned -> calculate the random time and schedule respawn.
		if (getRespawnState() && getRespawnDelay() > 0)
		{
			// Calculate the random delay.
			final long respawnDelay = calculateRespawnDelay() * 1000;

			// Check spawn data and set respawn.
			if (_spawnData != null)
			{
				_spawnData.setRespawn(respawnDelay);
			}

			// Schedule respawn.
			npc.scheduleRespawn(respawnDelay);
		}
		// Npc can't be respawned, it disappears permanently -> Remove Spawn from SpawnManager.
		else
		{
			SpawnManager.getInstance().deleteSpawn(this);
		}
	}

	@Override
	public String toString()
	{
		return "Spawn [id=" + getNpcId() + "]";
	}

	@Override
	public String getDescription()
	{
		return "Location: " + _loc;
	}

	@Override
	public void updateSpawnData()
	{
		if (_spawnData == null)
		{
			return;
		}

		_spawnData.setStats(_npc);
	}

	/**
	 * Sets the {@link SpawnLocation} of this {@link Spawn}.
	 * @param loc : The SpawnLocation to set.
	 */
	public void setLoc(SpawnLocation loc)
	{
		_loc.set(loc);
	}

	/**
	 * Sets the {@link SpawnLocation} of this {@link Spawn} using separate coordinates.
	 * @param x : X coordinate.
	 * @param y : Y coordinate.
	 * @param z : Z coordinate.
	 * @param heading : Heading.
	 */
	public void setLoc(int x, int y, int z, int heading)
	{
		_loc.set(x, y, z, heading);
	}

	/**
	 * @return the X coordinate of the {@link SpawnLocation}.
	 */
	public int getLocX()
	{
		return _loc.getX();
	}

	/**
	 * @return the Y coordinate of the {@link SpawnLocation}.
	 */
	public int getLocY()
	{
		return _loc.getY();
	}

	/**
	 * @return the Z coordinate of the {@link SpawnLocation}.
	 */
	public int getLocZ()
	{
		return _loc.getZ();
	}

	/**
	 * @return the heading coordinate of the {@link SpawnLocation}.
	 */
	public int getHeading()
	{
		return _loc.getHeading();
	}

	/**
	 * @return the {@link Npc} instance of this {@link Spawn}.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
}