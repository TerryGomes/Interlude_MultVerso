package net.sf.l2j.gameserver.scripting.script.ai.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * When unique {@link Npc} spawns, the linked {@link NpcMaker} is despawned.<br>
 * When unique {@link Npc} despawns, the linked {@link NpcMaker} is spawned.
 */
public class ExclusiveSpawn extends Quest
{
	private static final List<ExclusiveData> SPAWN = new ArrayList<>();

	private static final List<ExclusiveData> DESPAWN = new ArrayList<>();

	public ExclusiveSpawn()
	{
		super(-1, "ai/spawn");

		DESPAWN.add(new ExclusiveData("godard14_18_02", 21434, "es_godard14_18_01"));
		DESPAWN.add(new ExclusiveData("godard14_19_02", 21434, "es_godard14_19_01"));
		DESPAWN.add(new ExclusiveData("godard14_20_02", 21434, "es_godard14_20_01"));

		List<Integer> npcIds = SPAWN.stream().map(ExclusiveData::getNpcId).collect(Collectors.toList());
		addEventIds(npcIds, EventHandler.CREATED, EventHandler.DECAYED);

		npcIds = DESPAWN.stream().map(ExclusiveData::getNpcId).collect(Collectors.toList());
		addEventIds(npcIds, EventHandler.CREATED, EventHandler.DECAYED);
	}

	@Override
	public void onCreated(Npc npc)
	{
		final ASpawn spawn = npc.getSpawn();
		if (spawn != null && spawn instanceof MultiSpawn)
		{
			// get NpcMaker name, it is used as event name
			final String name = ((MultiSpawn) spawn).getNpcMaker().getName();
			final int npcId = npc.getNpcId();

			// get data and spawn monsters
			ExclusiveData ed = SPAWN.stream().filter(d -> d.valid(name, npcId)).findFirst().orElse(null);
			if (ed != null)
			{
				SpawnManager.getInstance().spawnEventNpcs(ed.getEvent(), true, false);
			}

			// get data and despawn monsters
			ed = DESPAWN.stream().filter(d -> d.valid(name, npcId)).findFirst().orElse(null);
			if (ed != null)
			{
				SpawnManager.getInstance().despawnEventNpcs(ed.getEvent(), false);
			}
		}
	}

	@Override
	public void onDecayed(Npc npc)
	{
		final ASpawn spawn = npc.getSpawn();
		if (spawn != null && spawn instanceof MultiSpawn)
		{
			// get NpcMaker name, it is used as event name
			final String name = ((MultiSpawn) spawn).getNpcMaker().getName();
			final int npcId = npc.getNpcId();

			// get data and spawn monsters
			ExclusiveData ed = SPAWN.stream().filter(d -> d.valid(name, npcId)).findFirst().orElse(null);
			if (ed != null)
			{
				SpawnManager.getInstance().despawnEventNpcs(ed.getEvent(), false);
			}

			// get data and spawn monsters
			ed = DESPAWN.stream().filter(d -> d.valid(name, npcId)).findFirst().orElse(null);
			if (ed != null)
			{
				SpawnManager.getInstance().spawnEventNpcs(ed.getEvent(), true, false);
			}
		}
		super.onDecayed(npc);
	}

	/**
	 * Contains exclusive spawn data.<br>
	 * Links particular {@link NpcMaker} and {@link Npc} with event to be spawned/despawned.
	 */
	private class ExclusiveData
	{
		private final String _maker;
		private final int _npcId;
		private final String _event;

		protected ExclusiveData(String maker, int npcId, String event)
		{
			_maker = maker;
			_npcId = npcId;
			_event = event;
		}

		protected int getNpcId()
		{
			return _npcId;
		}

		protected boolean valid(String maker, int npcId)
		{
			return _maker.equals(maker) && _npcId == npcId;
		}

		protected String getEvent()
		{
			return _event;
		}
	}
}