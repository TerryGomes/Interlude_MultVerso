package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Core extends AttackableAIScript
{
	// Grand boss
	private static final int CORE = 29006;

	// Monsters
	private static final int DEATH_KNIGHT = 29007;
	private static final int DOOM_WRAITH = 29008;
	private static final int SUSCEPTOR = 29011;

	// NPCs
	private static final int TELEPORTATION_CUBE = 31842;

	// Doors
	private static final int CORE_DOOR = 20210001;
	private static final String CORE_DOOR_GUARDS = "core_door_guards";

	// Core states.
	private static final byte ALIVE = 1;

	private final Set<Npc> _minions = ConcurrentHashMap.newKeySet();

	public Core()
	{
		super("ai/boss");
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(CORE);
		addCreated(CORE);
		addDoorChange(CORE_DOOR);
		addMyDying(CORE, DEATH_KNIGHT, DOOM_WRAITH, SUSCEPTOR);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("spawn_minion"))
		{
			final Monster monster = (Monster) addSpawn(npc.getNpcId(), npc, false, 0, false);
			monster.setRaidRelated();

			_minions.add(monster);
		}
		else if (name.equalsIgnoreCase("despawn_minions"))
		{
			_minions.forEach(Npc::deleteMe);
			_minions.clear();
		}

		return super.onTimer(name, npc, player);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			if (npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				npc.broadcastNpcSay(NpcStringId.ID_1000001);
				npc.broadcastNpcSay(NpcStringId.ID_1000002);
			}
			else if (Rnd.get(100) == 0)
			{
				npc.broadcastNpcSay(NpcStringId.ID_1000003);
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onCreated(Npc npc)
	{
		npc.broadcastPacket(new PlaySound(1, "BS01_A", npc));

		// Spawn minions
		Monster monster;
		for (int i = 0; i < 5; i++)
		{
			int x = 16800 + i * 360;
			monster = (Monster) addSpawn(DEATH_KNIGHT, x, 110000, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
			monster.setRaidRelated();
			_minions.add(monster);

			monster = (Monster) addSpawn(DEATH_KNIGHT, x, 109000, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
			monster.setRaidRelated();
			_minions.add(monster);
		}

		for (int i = 0; i < 3; i++)
		{
			int x2 = 16800 + i * 600;
			monster = (Monster) addSpawn(DOOM_WRAITH, x2, 109300, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
			monster.setRaidRelated();
			_minions.add(monster);
		}

		for (int i = 0; i < 4; i++)
		{
			int x = 16800 + i * 450;
			monster = (Monster) addSpawn(SUSCEPTOR, x, 110300, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
			monster.setRaidRelated();
			_minions.add(monster);
		}
		super.onCreated(npc);
	}

	@Override
	public void onDoorChange(Door door)
	{
		if (door.isOpened())
		{
			SpawnManager.getInstance().spawnEventNpcs(CORE_DOOR_GUARDS, true, false);
		}
		else
		{
			SpawnManager.getInstance().despawnEventNpcs(CORE_DOOR_GUARDS, false);
		}
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (npc.getNpcId() == CORE)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));
			npc.broadcastNpcSay(NpcStringId.ID_1000004);
			npc.broadcastNpcSay(NpcStringId.ID_1000005);
			npc.broadcastNpcSay(NpcStringId.ID_1000006);

			addSpawn(TELEPORTATION_CUBE, 16502, 110165, -6394, 0, false, 900000, false);
			addSpawn(TELEPORTATION_CUBE, 18948, 110166, -6397, 0, false, 900000, false);

			startQuestTimer("despawn_minions", null, null, 20000);
			cancelQuestTimers("spawn_minion");
		}
		else if (SpawnManager.getInstance().getSpawn(CORE).getSpawnData().getStatus() == ALIVE && _minions.contains(npc))
		{
			_minions.remove(npc);
			startQuestTimer("spawn_minion", npc, null, 60000);
		}
		super.onMyDying(npc, killer);
	}
}