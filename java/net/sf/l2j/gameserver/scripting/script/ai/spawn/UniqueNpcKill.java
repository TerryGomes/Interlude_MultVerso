package net.sf.l2j.gameserver.scripting.script.ai.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * Spawns additional single wave of monsters when unique {@link Npc} dies.
 */
public class UniqueNpcKill extends Quest
{
	private static final List<UniqueData> DATA = new ArrayList<>();

	public UniqueNpcKill()
	{
		super(-1, "ai/spawn");

		DATA.add(new UniqueData("godard14_05_01", 21399, "unk_godard14_05_02"));
		DATA.add(new UniqueData("godard14_07_01", 21399, "unk_godard14_07_02"));
		DATA.add(new UniqueData("godard14_06_01", 21399, "unk_godard14_06_02"));
		DATA.add(new UniqueData("godard14_08_01", 21399, "unk_godard14_08_02"));

		List<Integer> npcIds = DATA.stream().map(UniqueData::getNpcId).collect(Collectors.toList());
		addEventIds(npcIds, EventHandler.MY_DYING);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		final ASpawn spawn = npc.getSpawn();
		if (spawn != null && spawn instanceof MultiSpawn)
		{
			// get NpcMaker name, it is used as event name
			final String name = ((MultiSpawn) spawn).getNpcMaker().getName();
			final int npcId = npc.getNpcId();

			// get data and spawn monsters, once
			UniqueData ud = DATA.stream().filter(d -> d.valid(name, npcId)).findFirst().orElse(null);
			if (ud != null)
			{
				SpawnManager.getInstance().spawnEventNpcs(ud.getEvent(), false, false);
			}
		}
	}

	/**
	 * Contains unique spawn data.<br>
	 * Links particular {@link NpcMaker} and {@link Npc} with event to be spawned.
	 */
	private class UniqueData
	{
		private final String _maker;
		private final int _npcId;
		private final String _event;

		protected UniqueData(String maker, int npcId, String event)
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