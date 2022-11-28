package net.sf.l2j.gameserver.scripting.script.ai.boss;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.enums.skills.ElementType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class QueenAnt extends AttackableAIScript
{
	// Grand boss
	private static final int QUEEN_ANT = 29001;

	// Monsters
	private static final int QUEEN_ANT_LARVA = 29002;
	private static final int NURSE_ANT = 29003;
	private static final int GUARD_ANT = 29004;
	private static final int ROYAL_GUARD_ANT = 29005;

	private static final Location[] PLAYER_TELE_OUT =
	{
		new Location(-19480, 187344, -5600),
		new Location(-17928, 180912, -5520),
		new Location(-23808, 182368, -5600)
	};

	private Monster _larva = null;

	public QueenAnt()
	{
		super("ai/boss");
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(QUEEN_ANT, QUEEN_ANT_LARVA, NURSE_ANT, GUARD_ANT, ROYAL_GUARD_ANT);
		addClanAttacked(QUEEN_ANT, QUEEN_ANT_LARVA, NURSE_ANT, GUARD_ANT, ROYAL_GUARD_ANT);
		addCreated(QUEEN_ANT, QUEEN_ANT_LARVA, NURSE_ANT, GUARD_ANT);
		addSeeCreature(QUEEN_ANT_LARVA, NURSE_ANT, GUARD_ANT, ROYAL_GUARD_ANT);
		addMyDying(QUEEN_ANT, NURSE_ANT, ROYAL_GUARD_ANT);
		addOutOfTerritory(QUEEN_ANT, ROYAL_GUARD_ANT);
		addPartyAttacked(QUEEN_ANT, QUEEN_ANT_LARVA, NURSE_ANT, GUARD_ANT, ROYAL_GUARD_ANT);
		addSeeSpell(QUEEN_ANT, QUEEN_ANT_LARVA, NURSE_ANT, GUARD_ANT, ROYAL_GUARD_ANT);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		// Queen Ant animation timer.
		if (name.equalsIgnoreCase("1001"))
		{
			if (!npc.isInCombat() && Rnd.get(10) < 3)
			{
				npc.broadcastPacket(new SocialAction(npc, (Rnd.nextBoolean()) ? 3 : 4));
			}
		}
		// Guard Ant frenzy.
		else if (name.equalsIgnoreCase("3001"))
		{
			if (npc.isInCombat() && Rnd.get(100) < 66)
			{
				((Attackable) npc).getAggroList().randomizeAttack();
			}

			startQuestTimer("3001", npc, null, 90000L + Rnd.get(240000));
		}
		// Guard Ant teleport back if out of territory.
		else if (name.equalsIgnoreCase("3002"))
		{
			if (!npc.isInMyTerritory())
			{
				((Attackable) npc).getAggroList().clear();
				npc.teleportTo(npc.getSpawnLocation(), 0);
			}
		}
		// Delete the larva and the reference.
		else if (name.equalsIgnoreCase("clean"))
		{
			_larva.deleteMe();
			_larva = null;
		}

		return super.onTimer(name, npc, player);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			// Curses
			if (attacker.testCursesOnAttack(npc, QUEEN_ANT))
			{
				return;
			}

			// Pick current attacker, and make actions based on it and the actual distance range seperating them.
			if (npc.getNpcId() == QUEEN_ANT)
			{
				if (skill != null && skill.getElement() == ElementType.FIRE && Rnd.get(100) < 70)
				{
					npc.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
				}
				else
				{
					final double dist = npc.distance3D(attacker);
					if (dist > 500 && Rnd.get(100) < 10)
					{
						npc.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
					}
					else if (dist > 150 && Rnd.get(100) < 10)
					{
						npc.getAI().tryToCast(attacker, (Rnd.get(10) < 8) ? FrequentSkill.QUEEN_ANT_STRIKE.getSkill() : FrequentSkill.QUEEN_ANT_SPRINKLE.getSkill());
					}
					else if (dist < 250 && Rnd.get(100) < 5)
					{
						npc.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_BRANDISH.getSkill());
					}
				}
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onClanAttacked(Attackable caller, Attackable called, Creature attacker, int damage)
	{
		final double dist = called.distance3D(attacker);
		if (dist > 500 && Rnd.get(100) < 3)
		{
			called.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
		}
		else if (dist > 150 && Rnd.get(100) < 3)
		{
			called.getAI().tryToCast(attacker, (Rnd.get(100) < 80) ? FrequentSkill.QUEEN_ANT_SPRINKLE.getSkill() : FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
		}
		else if (dist < 250 && Rnd.get(100) < 2)
		{
			called.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_BRANDISH.getSkill());
		}

		switch (called.getNpcId())
		{
			case NURSE_ANT:
				// If the faction caller is the larva, assist it directly, no matter what.
				if (caller.getNpcId() == QUEEN_ANT_LARVA)
				{
					called.getAI().tryToCast(caller, Rnd.nextBoolean() ? FrequentSkill.NURSE_HEAL_1.getSkill() : FrequentSkill.NURSE_HEAL_2.getSkill());
				}
				else if (caller.getNpcId() == QUEEN_ANT)
				{
					if (_larva != null && _larva.getStatus().getHpRatio() < 1.0)
					{
						called.getAI().tryToCast(_larva, Rnd.nextBoolean() ? FrequentSkill.NURSE_HEAL_1.getSkill() : FrequentSkill.NURSE_HEAL_2.getSkill());
					}
					else
					{
						called.getAI().tryToCast(caller, FrequentSkill.NURSE_HEAL_1.getSkill());
					}
				}
				break;

			case ROYAL_GUARD_ANT:
				called.forceAttack(attacker, 200);
				break;
		}

		called.getAggroList().addDamageHate(attacker, 0, (int) (damage / (called.getStatus().getMaxHp() / 0.05) * 500));
	}

	@Override
	public void onCreated(Npc npc)
	{
		switch (npc.getNpcId())
		{
			case QUEEN_ANT:
				startQuestTimerAtFixedRate("1001", npc, null, 10000);

				npc.broadcastPacket(new PlaySound(1, "BS01_A", npc));

				_larva = (Monster) addSpawn(QUEEN_ANT_LARVA, -21600, 179482, -5846, Rnd.get(360), false, 0, false);

				// Choose a teleport location, and teleport players out of Queen Ant zone.
				if (Rnd.get(100) < 33)
				{
					npc.getSpawn().instantTeleportInMyTerritory(PLAYER_TELE_OUT[0]);
				}
				else if (Rnd.nextBoolean())
				{
					npc.getSpawn().instantTeleportInMyTerritory(PLAYER_TELE_OUT[1]);
				}
				else
				{
					npc.getSpawn().instantTeleportInMyTerritory(PLAYER_TELE_OUT[2]);
				}
				break;

			case QUEEN_ANT_LARVA:
				npc.setMortal(false);
				npc.setIsImmobilized(true);
				npc.disableCoreAi(true);
				break;

			case NURSE_ANT:
				npc.disableCoreAi(true);
				break;

			case GUARD_ANT:
				startQuestTimer("3001", npc, null, 90000L + Rnd.get(240000));
				startQuestTimerAtFixedRate("3002", npc, null, 10000);
				break;
		}

		super.onCreated(npc);
	}

	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (creature instanceof Playable && creature.testCursesOnAggro(npc))
		{
			return;
		}

		super.onSeeCreature(npc, creature);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (npc.getNpcId() == QUEEN_ANT)
		{
			// Broadcast death sound.
			npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));

			// Cancel tasks.
			cancelQuestTimers("1001");

			// Start respawn timer, and clean the monster references.
			startQuestTimer("clean", null, null, 5000);
		}
		else
		{/*
			 * // Set the respawn time of Royal Guards and Nurses. Pick the npc master. final Monster minion = ((Monster) npc); final Monster master = minion.getMaster(); if (master != null && master.hasMinions()) master.getMinionList().onMinionDie(minion, (npc.getNpcId() == NURSE_ANT) ? 10000 :
			 * (280000 + (Rnd.get(40) * 1000))); return null;
			 */
		}
		super.onMyDying(npc, killer);
	}

	@Override
	public void onOutOfTerritory(Npc npc)
	{
		((Attackable) npc).getAggroList().clear();
		npc.teleportTo(npc.getSpawnLocation(), 0);
	}

	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		switch (called.getNpcId())
		{
			case QUEEN_ANT:
				final double dist = called.distance3D(target);
				if (dist > 500 && Rnd.get(100) < 5)
				{
					called.getAI().tryToCast(target, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
				}
				else if (dist > 150 && Rnd.get(100) < 5)
				{
					called.getAI().tryToCast(target, (Rnd.get(100) < 80) ? FrequentSkill.QUEEN_ANT_SPRINKLE.getSkill() : FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
				}
				else if (dist < 250 && Rnd.get(100) < 2)
				{
					called.getAI().tryToCast(target, FrequentSkill.QUEEN_ANT_BRANDISH.getSkill());
				}
				break;

			case NURSE_ANT:
				// If the faction caller is the larva, assist it directly, no matter what.
				if (caller.getNpcId() == QUEEN_ANT_LARVA)
				{
					called.getAI().tryToCast(caller, Rnd.nextBoolean() ? FrequentSkill.NURSE_HEAL_1.getSkill() : FrequentSkill.NURSE_HEAL_2.getSkill());
				}
				else if (caller.getNpcId() == QUEEN_ANT)
				{
					if (_larva != null && _larva.getStatus().getHpRatio() < 1.0)
					{
						called.getAI().tryToCast(_larva, Rnd.nextBoolean() ? FrequentSkill.NURSE_HEAL_1.getSkill() : FrequentSkill.NURSE_HEAL_2.getSkill());
					}
					else
					{
						called.getAI().tryToCast(caller, FrequentSkill.NURSE_HEAL_1.getSkill());
					}
				}
				break;
		}
	}

	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		final Playable realAttacker = (isPet && caster.getSummon() != null) ? caster.getSummon() : caster;
		if (!Config.RAID_DISABLE_CURSE && realAttacker.getStatus().getLevel() - npc.getStatus().getLevel() > 8)
		{
			final L2Skill curse = FrequentSkill.RAID_CURSE.getSkill();

			npc.broadcastPacket(new MagicSkillUse(npc, realAttacker, curse.getId(), curse.getLevel(), 300, 0));
			curse.getEffects(npc, realAttacker);

			((Attackable) npc).getAggroList().stopHate(realAttacker);
			return;
		}

		// If Queen Ant see an aggroable skill, try to launch Queen Ant Strike.
		if (npc.getNpcId() == QUEEN_ANT && skill.getAggroPoints() > 0 && Rnd.get(100) < 15)
		{
			npc.getAI().tryToCast(realAttacker, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
		}

		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}
}