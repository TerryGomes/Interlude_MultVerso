package net.sf.l2j.gameserver.scripting.script.ai.boss;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Orfen extends AttackableAIScript
{
	private static final SpawnLocation[] ORFEN_LOCATION =
	{
		new SpawnLocation(43728, 17220, -4342, 0),
		new SpawnLocation(55024, 17368, -5412, 0),
		new SpawnLocation(53504, 21248, -5486, 0),
		new SpawnLocation(53248, 24576, -5262, 0)
	};

	private static final NpcStringId[] ORFEN_CHAT =
	{
		NpcStringId.ID_1000028,
		NpcStringId.ID_1000029,
		NpcStringId.ID_1000030,
		NpcStringId.ID_1000031
	};

	// Grand boss
	private static final int ORFEN = 29014;

	// Monsters
	private static final int RAIKEL_LEOS = 29016;
	private static final int RIBA_IREN = 29018;

	public Orfen()
	{
		super("ai/boss");
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(ORFEN, RIBA_IREN);
		addClanAttacked(RAIKEL_LEOS, RIBA_IREN);
		addCreated(ORFEN);
		addMyDying(ORFEN);
		addSeeSpell(ORFEN);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3001"))
		{
			if (npc.isScriptValue(1))
			{
				// HPs raised over 95%, instantly random teleport elsewhere. Teleport flag is set back to false.
				if (npc.getStatus().getHpRatio() > 0.95)
				{
					teleportOrfen(npc, Rnd.get(1, 3));

					npc.setScriptValue(0);
				}
				// Orfen already ported once and is lured out of her lair ; teleport her back.
				else if (!npc.isInsideZone(ZoneId.SWAMP))
				{
					teleportOrfen(npc, 0);
				}
			}
		}

		return super.onTimer(name, npc, player);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			// Curses
			if (attacker.testCursesOnAttack(npc))
			{
				return;
			}

			if (npc.getNpcId() == ORFEN)
			{
				// Orfen didn't yet teleport, and reached 50% HP.
				if (npc.isScriptValue(0) && npc.getStatus().getHpRatio() < 0.5)
				{
					// Set teleport flag to true.
					npc.setScriptValue(1);

					// Teleport Orfen to her lair.
					teleportOrfen(npc, 0);
				}
				else if (attacker instanceof Player)
				{
					final double dist = npc.distance3D(attacker);
					if (dist > 300 && dist < 1000 && Rnd.get(100) < 10)
					{
						// Random chat.
						npc.broadcastNpcSay(Rnd.get(ORFEN_CHAT), attacker.getName());

						// Teleport caster near Orfen.
						attacker.teleportTo(npc.getPosition(), 0);

						npc.getAI().tryToCast(attacker, 4063, 1);
					}
					else if (Rnd.get(100) < 20)
					{
						npc.getAI().tryToCast(attacker, 4064, 1);
					}
				}
			}
			// RIBA_IREN case, as it's the only other registered.
			else if (npc.getStatus().getHpRatio() < 0.5)
			{
				npc.getAI().tryToCast(attacker, 4516, 1);
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onClanAttacked(Attackable caller, Attackable called, Creature attacker, int damage)
	{
		if (called.getNpcId() == RAIKEL_LEOS && Rnd.get(100) < 5)
		{
			called.getAI().tryToCast(attacker, 4067, 4);
		}
		else if (called.getNpcId() == RIBA_IREN && caller.getNpcId() != RIBA_IREN && (caller.getStatus().getHpRatio() < 0.5) && Rnd.get(100) < ((caller.getNpcId() == ORFEN) ? 90 : 10))
		{
			called.getAI().tryToCast(caller, 4516, 1);
		}

		super.onClanAttacked(caller, called, attacker, damage);
	}

	@Override
	public void onCreated(Npc npc)
	{
		// Broadcast spawn sound.
		npc.broadcastPacket(new PlaySound(1, "BS01_A", npc));

		// Fire a 10s task to check Orfen status.
		startQuestTimerAtFixedRate("3001", npc, null, 10000);

		super.onCreated(npc);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		cancelQuestTimers("3001", npc);

		npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));

		super.onMyDying(npc, killer);
	}

	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		Creature originalCaster = isPet ? caster.getSummon() : caster;
		if (skill.getAggroPoints() > 0 && Rnd.get(100) < 20 && npc.isIn3DRadius(originalCaster, 1000))
		{
			// Random chat.
			npc.broadcastNpcSay(Rnd.get(ORFEN_CHAT), caster.getName());

			// Teleport caster near Orfen.
			originalCaster.teleportTo(npc.getPosition(), 0);

			// Cast a skill.
			npc.getAI().tryToCast(originalCaster, 4064, 1);
		}
		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}

	/**
	 * This method is used by Orfen to teleport from one location to another. In all cases, she loses aggro.
	 * @param npc : Orfen in any case.
	 * @param index : The SpawnLocation array index, which is 0 for her lair or 1-3 for desert.
	 */
	private static void teleportOrfen(Npc npc, int index)
	{
		// Clear all aggro.
		((Attackable) npc).getAggroList().clear();

		// Retrieve the SpawnLocation.
		final SpawnLocation loc = ORFEN_LOCATION[index];

		// Edit the spawn location and teleport the Npc.
		npc.setSpawnLocation(loc);
		npc.teleportTo(loc, 0);
	}
}