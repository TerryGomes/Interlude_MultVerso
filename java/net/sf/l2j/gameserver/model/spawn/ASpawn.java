package net.sf.l2j.gameserver.model.spawn;

import java.io.InvalidClassException;
import java.lang.reflect.Constructor;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;

/**
 * This class is a mother-class for all spawns.
 */
public abstract class ASpawn
{
	protected static final CLogger LOGGER = new CLogger(ASpawn.class.getName());

	private static final String INSTANCE_PACKAGE = "net.sf.l2j.gameserver.model.actor.instance.";

	protected final NpcTemplate _template;
	protected final Constructor<?> _constructor;

	protected int _respawnDelay;
	protected int _respawnRandom;

	protected SpawnData _spawnData;

	protected ASpawn(int id) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		// Set the template of a spawn.
		_template = NpcData.getInstance().getTemplate(id);
		if (_template == null)
		{
			_constructor = null;
			return;
		}

		// Check for Npc class.
		Class<?> clazz = Class.forName(INSTANCE_PACKAGE + _template.getType());
		if (!Npc.class.isAssignableFrom(clazz))
		{
			throw new InvalidClassException("Template type " + _template.getType() + " is not child of Npc.");
		}

		// Create the generic constructor.
		_constructor = clazz.getConstructor(int.class, NpcTemplate.class);
	}

	protected ASpawn(NpcTemplate template) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		// Set the template of a spawn.
		_template = template;
		if (_template == null)
		{
			_constructor = null;
			return;
		}

		// Check for Npc class.
		Class<?> clazz = Class.forName(INSTANCE_PACKAGE + _template.getType());
		if (!Npc.class.isAssignableFrom(clazz))
		{
			throw new InvalidClassException("Template type " + _template.getType() + " is not child of Npc.");
		}

		// Create the generic constructor.
		_constructor = clazz.getConstructor(int.class, NpcTemplate.class);
	}

	/**
	 * @return the {@link NpcTemplate} associated to this {@link ASpawn}.
	 */
	public final NpcTemplate getTemplate()
	{
		return _template;
	}

	/**
	 * @return the npc id of the {@link NpcTemplate}.
	 */
	public final int getNpcId()
	{
		return _template.getNpcId();
	}

	/**
	 * Set the respawn delay, representing the respawn time of this {@link ASpawn}. It can't be less than 0.
	 * @param delay : Respawn delay in seconds.
	 */
	public final void setRespawnDelay(int delay)
	{
		_respawnDelay = Math.max(0, delay);
	}

	/**
	 * @return the respawn delay of this {@link ASpawn} in seconds.
	 */
	public final int getRespawnDelay()
	{
		return _respawnDelay;
	}

	/**
	 * Set the respawn random delay of this {@link ASpawn}. It can't be less than 0 and no more than respawn delay.
	 * @param random : Random respawn delay in seconds.
	 */
	public final void setRespawnRandom(int random)
	{
		_respawnRandom = Math.min(_respawnDelay, Math.max(0, random));
	}

	/**
	 * @return the respawn delay of this {@link ASpawn} in seconds.
	 */
	public final int getRespawnRandom()
	{
		return _respawnRandom;
	}

	/**
	 * @return Calculated respawn delay in seconds, using respawn delay and respawn random values.
	 */
	public long calculateRespawnDelay()
	{
		int respawnTime = _respawnDelay;

		if (_respawnRandom > 0)
		{
			respawnTime += Rnd.get(-_respawnRandom, _respawnRandom);
		}

		return respawnTime;
	}

	/**
	 * @return The {@link SpawnData} of this {@link ASpawn}.
	 */
	public final SpawnData getSpawnData()
	{
		return _spawnData;
	}

	/**
	 * @param respawn : Enables or disable respawn state of this {@link ASpawn}.
	 */
	public abstract void setRespawnState(boolean respawn);

	/**
	 * @return The respawn state of this {@link ASpawn}.
	 */
	public abstract boolean getRespawnState();

	/**
	 * @return The {@link SpawnLocation} of this {@link ASpawn}.
	 */
	public abstract SpawnLocation getSpawnLocation();

	/**
	 * @param npc : The {@link Npc} of this {@link ASpawn}.
	 * @param offset : The random walk offset.
	 * @return The random walk {@link Location} of this {@link Npc}.
	 */
	public abstract Location getRandomWalkLocation(Npc npc, int offset);

	/**
	 * @param worldObject : The {@link WorldObject} to test.
	 * @return True when the {@link WorldObject} is in this {@link ASpawn} area, false otherwise.
	 */
	public abstract boolean isInMyTerritory(WorldObject worldObject);

	/**
	 * @return The closer description of this {@link ASpawn}. Use together with {@code toString()} to get complete information.
	 */
	public abstract String getDescription();

	/**
	 * Updates {@link SpawnData} of this {@link ASpawn}.
	 */
	public abstract void updateSpawnData();

	/**
	 * Create the {@link Npc} and spawns it into the world.
	 * <ul>
	 * <li>Create {@link Npc} from defined {@link NpcTemplate} and <u>type</u>, using new object id.</li>
	 * <li>Link the {@link Npc} to this {@link ASpawn}</li>
	 * <li>Initialize parameters and create {@link Npc} in the world (see <code>initializeAndSpawn(Npc npc)</code>).</li>
	 * </ul>
	 * @param isSummonSpawn : When true, summon magic circle will appear when spawning into the world.
	 * @return The spawned {@link Npc}.
	 */
	public Npc doSpawn(boolean isSummonSpawn)
	{
		try
		{
			// Call the constructor and create Npc instance.
			final Npc npc = (Npc) _constructor.newInstance(IdFactory.getInstance().getNextId(), _template);

			// Assign ASpawn to Npc instance, set summon animation.
			npc.setSpawn(this);
			npc.setShowSummonAnimation(isSummonSpawn);

			// Initialize Npc and spawn it.
			return initializeAndSpawn(npc);
		}
		catch (Exception e)
		{
			LOGGER.warn("Error during spawn, NPC id={}", e, _template.getNpcId());
			return null;
		}
	}

	/**
	 * {@link Npc} has spawned.
	 * @param npc : The {@link Npc}.
	 */
	public void onSpawn(Npc npc)
	{
	}

	/**
	 * Delete the {@link Npc}(s) of this {@link ASpawn}.
	 */
	public abstract void doDelete();

	/**
	 * {@link Npc} has decayed. Create a respawn task to be launched after the fixed + random delay. Respawn is only possible when respawn enabled.
	 * @param npc : The {@link Npc}.
	 */
	public void onDecay(Npc npc)
	{
	}

	/**
	 * Refreshes object id of {@link Npc} and spawns it.
	 * @param npc : The {@link Npc} to be respawned.
	 */
	public void doRespawn(Npc npc)
	{
		npc.cancelRespawn();

		// Cancel respawn (reset the respawn time and/or update status).
		if (_spawnData != null)
		{
			_spawnData.cancelRespawn();
		}

		if (getRespawnState() && getRespawnDelay() > 0 && npc.isDecayed())
		{
			npc.refreshID();

			initializeAndSpawn(npc);
		}
	}

	/**
	 * Teleport all {@link Player}s located in this {@link ASpawn} to specific coords x/y/z.
	 * @param x : The X parameter used as teleport location.
	 * @param y : The Y parameter used as teleport location.
	 * @param z : The Z parameter used as teleport location.
	 * @param offset : An offset used to randomize final teleport location.
	 */
	public void instantTeleportInMyTerritory(int x, int y, int z, int offset)
	{
		World.getInstance().getPlayers().stream().filter(p -> p.isOnline() && isInMyTerritory(p)).forEach(p -> p.teleportTo(x, y, z, offset));
	}

	/**
	 * Teleport all {@link Player}s located in this {@link ASpawn} to specific coords x/y/z.
	 * @param x : The X parameter used as teleport location.
	 * @param y : The Y parameter used as teleport location.
	 * @param z : The Z parameter used as teleport location.
	 */
	public void instantTeleportInMyTerritory(int x, int y, int z)
	{
		instantTeleportInMyTerritory(x, y, z, 0);
	}

	/**
	 * Teleport all {@link Player}s located in this {@link ASpawn} to a specific {@link Location}.
	 * @see #instantTeleportInMyTerritory(int, int, int)
	 * @param loc : The {@link Location} used as coords.
	 * @param offset : An offset used to randomize final teleport location.
	 */
	public void instantTeleportInMyTerritory(Location loc, int offset)
	{
		instantTeleportInMyTerritory(loc.getX(), loc.getY(), loc.getZ(), offset);
	}

	/**
	 * Teleport all {@link Player}s located in this {@link ASpawn} to a specific {@link Location}.
	 * @see #instantTeleportInMyTerritory(int, int, int)
	 * @param loc : The {@link Location} used as coords.
	 */
	public void instantTeleportInMyTerritory(Location loc)
	{
		instantTeleportInMyTerritory(loc.getX(), loc.getY(), loc.getZ(), 0);
	}

	/**
	 * Initializes the {@link Npc} based on data of this {@link ASpawn} and spawn it into the world.
	 * <ul>
	 * <li>Check {@link SpawnLocation} of this spawn. Spawn canceled, if no location defined.</li>
	 * <li>Validate {@link SpawnLocation} and get correct Z coordinate from geodata.</li>
	 * <li>Reset {@link Npc} parameters (for re-spawning of existing {@link Npc}s)</li>
	 * <li>Check and set champion status, if allowed.</li>
	 * <li>Make {@link Npc} alive.</li>
	 * <li>Spawn {@link Npc} to the world at given {@link SpawnLocation}.</li>
	 * </ul>
	 * @param npc : {@link Npc} to be initialized and spawned.
	 * @return Spawned {@link Npc}. Null, if no {@link SpawnLocation} is found.
	 */
	public Npc initializeAndSpawn(Npc npc)
	{
		// NPC has spawn data and respawn time has not passed -> will be dead.
		if (_spawnData != null && _spawnData.checkDead())
		{
			// Clear HP.
			npc.getStatus().setHp(0);

			// Make NPC dead.
			npc.setIsDead(true);
			npc.setDecayed(true);

			// Set respawn task.
			npc.scheduleRespawn(_spawnData.getRespawnTime() - System.currentTimeMillis());
		}
		// NPC has no spawn data or respawn time has passed -> will become alive.
		else
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

			// NPC has spawn data, is still alive:
			// -> update spawn location and HP, MP from saved values
			// Otherwise spawn data is not initialized or the NPC became alive.
			// -> do nothing, use generated spawn location and reset HP and MP
			if (_spawnData != null && _spawnData.checkAlive(loc, maxHp, maxMp))
			{
				// Load spawn location.
				loc = _spawnData;

				// Load HP and MP.
				maxHp = _spawnData.getCurrentHp();
				maxMp = _spawnData.getCurrentMp();
			}

			// Set HP and MP.
			npc.getStatus().setHpMp(maxHp, maxMp);

			// when champion mod is enabled, try to make NPC a champion
			if (Config.CHAMPION_FREQUENCY > 0)
			{
				// It can't be a Raid, a Raid minion nor a minion. Quest mobs and chests are disabled too.
				if (npc instanceof Monster && !getTemplate().cantBeChampion() && npc.getStatus().getLevel() >= Config.CHAMP_MIN_LVL && npc.getStatus().getLevel() <= Config.CHAMP_MAX_LVL && !npc.isRaidRelated() && !npc.hasMaster())
				{
					((Monster) npc).setChampion(Rnd.get(100) < Config.CHAMPION_FREQUENCY);
				}
			}

			// Set spawn location and spawn Npc.
			npc.setSpawnLocation(loc);
			npc.spawnMe(loc);
		}

		return npc;
	}

	public void onDie(Npc npc)
	{
	}

	public void doSave()
	{
		if (_spawnData != null)
		{
			SpawnManager.getInstance().save(_spawnData);
		}
	}
}