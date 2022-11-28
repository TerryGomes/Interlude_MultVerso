package net.sf.l2j.gameserver.model.spawn;

import java.io.InvalidClassException;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;

/**
 * This class manages the spawn and respawn of a single {@link Npc} at given {@link SpawnLocation}.
 */
public final class MinionSpawn extends ASpawn
{
	private final SpawnLocation _loc = new SpawnLocation(0, 0, 0, 0);

	private Npc _npc;

	private final Npc _master;

	public MinionSpawn(int id, Npc master) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		super(id);

		_master = master;
	}

	public MinionSpawn(NpcTemplate template, Npc master) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		super(template);

		_master = master;
	}

	@Override
	public final boolean getRespawnState()
	{
		return false;
	}

	@Override
	public final void setRespawnState(boolean respawn)
	{
	}

	@Override
	public SpawnLocation getSpawnLocation()
	{
		// If one SpawnLocation is set, we use it.
		if (!_loc.equals(SpawnLocation.DUMMY_SPAWNLOC))
		{
			return _loc;
		}

		// Calculate offsets.
		final int minOffset = (int) (_master.getCollisionRadius() + 30);
		final int maxOffset = (int) (100 + _template.getCollisionRadius() + _master.getCollisionRadius());

		// Generate a Location based on offsets and master Location.
		final SpawnLocation loc = _master.getPosition().clone();
		loc.addRandomOffsetBetween(minOffset, maxOffset);
		loc.set(GeoEngine.getInstance().getValidLocation(_master, loc));

		return loc;
	}

	@Override
	public Location getRandomWalkLocation(Npc npc, int offset)
	{
		Location loc = null;

		final Npc master = npc.getMaster();
		if (master != null)
		{
			if (master.isRunning())
			{
				npc.forceRunStance();
			}
			else
			{
				npc.forceWalkStance();
			}

			final int maxOffset = (int) (100 + npc.getCollisionRadius() + master.getCollisionRadius());
			if (npc.distance3D(master) > maxOffset)
			{
				final int minOffset = (int) (master.getCollisionRadius() + 30);

				loc = master.getPosition().clone();
				loc.addRandomOffsetBetween(minOffset, maxOffset);
				loc.set(GeoEngine.getInstance().getValidLocation(master, loc));
			}
		}
		else
		{
			loc = npc.getPosition().clone();
			loc.addRandomOffset(offset);
			loc.set(GeoEngine.getInstance().getValidLocation(npc, loc));
		}

		return loc;
	}

	@Override
	public boolean isInMyTerritory(WorldObject worldObject)
	{
		return true;
	}

	@Override
	public void doDelete()
	{
		if (_npc == null)
		{
			return;
		}

		// Cancel respawn task and delete NPC.
		_npc.cancelRespawn();
		_npc.deleteMe();
		_npc = null;
	}

	@Override
	public void onDecay(Npc npc)
	{
	}

	@Override
	public void doRespawn(Npc npc)
	{
		// Don't spawn the minion if master just died.
		if (_master == null || _master.isDead())
		{
			return;
		}

		npc.cancelRespawn();

		if (npc.isDecayed()) // TODO add back getRespawnDelay() > 0 check once all respawn times are properly parsed on NPC templates
		{
			npc.refreshID();

			initializeAndSpawn(npc);
		}
	}

	@Override
	public long calculateRespawnDelay()
	{
		int respawnTime = _respawnDelay;

		if (_respawnRandom > 0)
		{
			respawnTime += Rnd.get(-_respawnRandom, _respawnRandom);
		}

		return respawnTime;
	}

	@Override
	public String toString()
	{
		return "Spawn [id=" + getNpcId() + "]";
	}

	@Override
	public String getDescription()
	{
		return "Master: " + _master;
	}

	@Override
	public void updateSpawnData()
	{
	}

	@Override
	public Npc doSpawn(boolean isSummonSpawn)
	{
		try
		{
			// Call the constructor and create Npc instance.
			final Npc npc = (Npc) _constructor.newInstance(IdFactory.getInstance().getNextId(), _template);

			// Assign ASpawn to Npc instance, set summon animation.
			npc.setSpawn(this);
			npc.setShowSummonAnimation(isSummonSpawn);

			if (_master != null)
			{
				// Register the master.
				npc.setMaster(_master);

				if (_master.isRaidBoss() && npc instanceof Monster)
				{
					((Monster) npc).setRaidRelated();
				}
			}

			// Initialize Npc and spawn it.
			return initializeAndSpawn(npc);
		}
		catch (Exception e)
		{
			LOGGER.warn("Error during spawn, NPC id={}", e, _template.getNpcId());
			return null;
		}
	}

	@Override
	public Npc initializeAndSpawn(Npc npc)
	{
		// Reset effects and status, script value.
		npc.stopAllEffects();
		npc.setScriptValue(0);

		// Make Npc alive.
		npc.setIsDead(false);
		npc.setDecayed(false);

		// Reset regeneration flags.
		npc.getStatus().initializeValues();

		// By default, generate random spawn location. If location does not exist, there's a problem.
		SpawnLocation loc = getSpawnLocation();
		if (loc == null)
		{
			LOGGER.warn("{} misses location informations.", this);
			return null;
		}

		// By default, reset HP and MP.
		double maxHp = npc.getStatus().getMaxHp();
		double maxMp = npc.getStatus().getMaxMp();

		// Set HP and MP.
		npc.getStatus().setHpMp(maxHp, maxMp);

		// Set spawn location and spawn Npc.
		npc.setSpawnLocation(loc);
		npc.spawnMe(loc);

		return npc;
	}

	/**
	 * @return the {@link Npc} instance of this {@link MinionSpawn}.
	 */
	public Npc getNpc()
	{
		return _npc;
	}

	/**
	 * @return the {@link Npc} master of this {@link MinionSpawn}.
	 */
	public Npc getMaster()
	{
		return _master;
	}

	public void setLoc(SpawnLocation loc)
	{
		_loc.set(loc);
	}

	public void setLoc(int x, int y, int z, int heading)
	{
		_loc.set(x, y, z, heading);
	}
}