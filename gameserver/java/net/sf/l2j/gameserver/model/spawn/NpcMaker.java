package net.sf.l2j.gameserver.model.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.scripting.Quest;

public class NpcMaker
{
	public static enum SpawnType
	{
		/**
		 * Spawns all NPCs, until each group's "total" NPC parameter is reached.<br>
		 * On respawn spawns itself.
		 */
		ALL,

		/**
		 * Spawns randomly NPCs, until the "maximum_npc" is reached.<br>
		 * The group's "total" NPC parameter does not take effect.<br>
		 * On respawn tries to spawn another NPC.
		 */
		RANDOM,

		/**
		 * Spawns all NPCs or random group, until group's "total" NPC parameter is reached.<br>
		 * On respawn spawns itself.
		 */
		RANDOM_GROUP,
	}

	private final String _name;
	private final Territory _territory;
	private final Territory _bannedTerritory;
	private final SpawnType _spawnType;
	private final int _maximumNpc;
	private final String _event;
	private final boolean _onStart;
	private List<MultiSpawn> _spawns;

	private boolean _respawnState;
	private int _npcs;

	private List<Quest> _questEvents = Collections.emptyList();

	/**
	 * Implicit {@link NpcMaker} constructor.
	 * @param set : Stats of the {@link NpcMaker}.
	 */
	public NpcMaker(StatSet set)
	{
		_name = set.getString("name", null);

		_territory = set.getObject("t", Territory.class);
		_bannedTerritory = set.getObject("bt", Territory.class);

		_spawnType = set.getEnum("spawn", SpawnType.class);
		_maximumNpc = (int) Math.round(set.getInteger("maximumNpcs") * Config.SPAWN_MULTIPLIER);
		_event = set.getString("event", null);
		_onStart = set.getBool("onStart", _event == null);

		_spawns = null;
		_respawnState = false;
		_npcs = 0;
	}

	/**
	 * @return the name of the {@link NpcMaker}.
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * @return the {@link Territory} of the {@link NpcMaker}.
	 */
	public final Territory getTerritory()
	{
		return _territory;
	}

	/**
	 * @return the banned {@link Territory} of the {@link NpcMaker}.
	 */
	public final Territory getBannedTerritory()
	{
		return _bannedTerritory;
	}

	/**
	 * @return the {@link SpawnType} of the {@link NpcMaker}.
	 */
	public final SpawnType getSpawnType()
	{
		return _spawnType;
	}

	/**
	 * @return the maximum amount of NPCs allowed for the {@link NpcMaker}.
	 */
	public final int getMaximumNpc()
	{
		return _maximumNpc;
	}

	/**
	 * @return the event name of the {@link NpcMaker}, used to spawn/despawn special groups of NPCs.
	 */
	public final String getEvent()
	{
		return _event;
	}

	/**
	 * @return true, if the {@link NpcMaker} is to be spawned on server start.
	 */
	public final boolean isOnStart()
	{
		return _onStart;
	}

	/**
	 * Sets the {@link MultiSpawn} to the {@link NpcMaker}. Used only for creation of {@link NpcMaker} by {@link SpawnManager}.
	 * @param spawns : {@link List} of all {@link MultiSpawn}.
	 */
	public final void setSpawns(List<MultiSpawn> spawns)
	{
		_spawns = spawns;
	}

	/**
	 * @return the {@link List} of all {@link MultiSpawn} of the {@link NpcMaker}.
	 */
	public final List<MultiSpawn> getSpawns()
	{
		return _spawns;
	}

	/**
	 * @param respawn : Enables or disable respawn state of this {@link NpcMaker}.
	 */
	public final synchronized void setRespawnState(boolean respawn)
	{
		_respawnState = respawn;
	}

	/**
	 * @return The respawn state of this {@link NpcMaker}.
	 */
	public final boolean getRespawnState()
	{
		return _respawnState;
	}

	/**
	 * @return the amount of currently spawned {@link Npc}s by the {@link NpcMaker}.
	 */
	public final int getNpcsAlive()
	{
		return _npcs;
	}

	/**
	 * @return the amount of currently decayed {@link Npc}s by the {@link NpcMaker}.
	 */
	public final long getNpcsDead()
	{
		return _spawns.stream().mapToLong(sd -> sd.getDecayed()).sum();
	}

	/**
	 * @return the list of registered {@link Quest}s.
	 */
	public final List<Quest> getQuestEvents()
	{
		return _questEvents;
	}

	/**
	 * Add a {@link Quest} on _questEvents {@link List}. Generate {@link List} if not existing (lazy initialization).<br>
	 * If already existing, we remove and add it back.
	 * @param quest : The {@link Quest} to add.
	 */
	public final void addQuestEvent(Quest quest)
	{
		if (_questEvents.isEmpty())
		{
			_questEvents = new ArrayList<>(3);
		}

		_questEvents.remove(quest);
		_questEvents.add(quest);
	}

	/**
	 * Spawns all {@link Npc} of this {@link NpcMaker}.
	 * @param respawn : Enables or disable respawn state of this {@link NpcMaker}.
	 * @return the amount of spawned {@link Npc}s.
	 */
	public final synchronized int spawnAll(boolean respawn)
	{
		return spawnAll(respawn, _maximumNpc);
	}

	/**
	 * Spawns {@link Npc}s of this {@link NpcMaker} up maximum defined count.
	 * @param respawn : Enables or disable respawn state of this {@link NpcMaker}.
	 * @param max : Maximum amount of {@link Npc}s to spawn, overrides native {@link NpcMaker} capacity.
	 * @return the amount of spawned {@link Npc}s.
	 */
	public final synchronized int spawnAll(boolean respawn, int max)
	{
		// Set respawn state.
		_respawnState = respawn;

		// Can't spawn more than capacity.
		max = Math.min(max, _maximumNpc);

		switch (_spawnType)
		{
			case ALL:
				// Standard spawn.
				for (MultiSpawn spawn : _spawns)
				{
					// Spawn all NPCs.
					while (_npcs < max && spawn.getNpcsAmount() < spawn.getTotal())
					{
						spawn.doSpawn(false);
					}
				}
				break;

			case RANDOM:
				// Randomly generated spawn.
				while (_npcs < max)
				{
					// Get random spawn.
					final MultiSpawn spawn = Rnd.get(_spawns);

					// Spawn NPC.
					spawn.doSpawn(false);
				}
				break;

			case RANDOM_GROUP:
				// Get random spawn.
				final MultiSpawn spawn = Rnd.get(_spawns);

				// Spawn all NPCs.
				while (_npcs < max && spawn.getNpcsAmount() < spawn.getTotal())
				{
					spawn.doSpawn(false);
				}
				break;
		}

		return _npcs;
	}

	/**
	 * Handles {@link Npc} spawn event in the {@link NpcMaker}.
	 * @param npc : The spawned {@link Npc}.
	 */
	public final void onSpawn(Npc npc)
	{
		_npcs++;
	}

	/**
	 * Handles {@link Npc} decay event in the {@link NpcMaker}.
	 * @param npc : The despawned {@link Npc}.
	 */
	public final void onDecay(Npc npc)
	{
		if (--_npcs == 0)
		{
			for (Quest quest : _questEvents)
			{
				quest.onMakerNpcsKilled(this, npc);
			}
		}
	}

	/**
	 * Immediately respawns all {@link Npc}s of this {@link NpcMaker}.
	 * @return the amount of respawned {@link Npc}s.
	 */
	public final synchronized int respawnAll()
	{
		int npcs = _npcs;
		boolean respawnState = _respawnState;

		// Respawn all NPCs.
		// Note: must set respawn state to true (enables respawn).
		_respawnState = true;
		_spawns.stream().forEach(MultiSpawn::doRespawn);

		_respawnState = respawnState;
		return _npcs - npcs;
	}

	/**
	 * Handles respawn behaviour of this {@link NpcMaker} depending on {@link SpawnType}.<br>
	 * Either start {@link MultiSpawn} respawn or start another spawn, in case of random-based {@link MultiSpawn}.
	 * @param spawn : {@link MultiSpawn} to be checked.
	 * @return the {@link MultiSpawn} from which the next spawn of {@link Npc} should be handled
	 */
	public final MultiSpawn onRespawn(MultiSpawn spawn)
	{
		switch (_spawnType)
		{
			case ALL:
			case RANDOM_GROUP:
				// Return self -> perform respawn.
				return spawn;

			case RANDOM:
				// Return random spawn -> start its spawn.
				return Rnd.get(_spawns);

			default:
				// Should not happen.
				return null;
		}
	}

	/**
	 * Deletes all {@link Npc}s of this {@link NpcMaker}.
	 * @return the amount of despawned {@link Npc}s.
	 */
	public final synchronized int deleteAll()
	{
		int npcs = _npcs;
		_respawnState = false;

		// Delete spawned NPCs.
		// Note: must set npcs to 0 (prevents onMakerNpcsKilled event to be called)
		_npcs = 0;
		_spawns.stream().forEach(MultiSpawn::doDelete);

		_npcs = 0;
		return npcs;
	}
}