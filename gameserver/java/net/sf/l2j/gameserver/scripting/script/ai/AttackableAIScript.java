package net.sf.l2j.gameserver.scripting.script.ai;

import java.util.HashSet;
import java.util.Set;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.MinionData;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AttackableAIScript extends Quest
{
	private static final String ACTOR_INSTANCE_PACKAGE = "net.sf.l2j.gameserver.model.actor.instance.";

	/**
	 * Implicit constructor for generic AI script.<br>
	 * It is used by default for all {@link Attackable} instances.
	 */
	public AttackableAIScript()
	{
		super(-1, "ai");

		registerNpcs();
	}

	/**
	 * A superclass constructor for all inherited AI scripts.<br>
	 * Inherited AI provides special behavior for particular {@link Attackable} instances.
	 * @param descr : The path/package of the AI script.
	 */
	protected AttackableAIScript(String descr)
	{
		super(-1, descr);

		registerNpcs();
	}

	/**
	 * Registers this AI script to the {@link Attackable}'s {@link NpcTemplate} for various {@link EventHandler} events.<br>
	 * All inherited AI scripts must override this method and register only to related {@link NpcTemplate}s + {@link EventHandler}s.<br>
	 * Every overridden {@link EventHandler} replaces default {@link AttackableAIScript} with the new AI script.
	 */
	protected void registerNpcs()
	{
		final Set<Integer> masters = new HashSet<>();
		final Set<Integer> minions = new HashSet<>();

		for (final NpcTemplate template : NpcData.getInstance().getAllNpcs())
		{
			try
			{
				if (!Attackable.class.isAssignableFrom(Class.forName(ACTOR_INSTANCE_PACKAGE + template.getType())))
				{
					continue;
				}

				template.addQuestEvent(EventHandler.ATTACKED, this);
				template.addQuestEvent(EventHandler.CREATED, this);
				template.addQuestEvent(EventHandler.MY_DYING, this);
				template.addQuestEvent(EventHandler.NO_DESIRE, this);
				template.addQuestEvent(EventHandler.OUT_OF_TERRITORY, this);
				template.addQuestEvent(EventHandler.SEE_CREATURE, this);
				template.addQuestEvent(EventHandler.SEE_SPELL, this);

				// Feed CLAN EventHandlers.
				if (template.getClans() != null)
				{
					template.addQuestEvent(EventHandler.CLAN_ATTACKED, this);
					template.addQuestEvent(EventHandler.CLAN_DIED, this);
				}

				// Feed PARTY EventHandlers.
				if (!template.getMinionData().isEmpty())
				{
					masters.add(template.getNpcId());

					for (MinionData md : template.getMinionData())
					{
						minions.add(md.getId());
					}
				}
			}
			catch (final ClassNotFoundException e)
			{
				LOGGER.error("An unknown template type {} has been found on {}.", e, template.getType(), toString());
			}
		}

		addEventIds(masters, EventHandler.PARTY_ATTACKED, EventHandler.PARTY_DIED);
		addEventIds(minions, EventHandler.PARTY_ATTACKED, EventHandler.PARTY_DIED);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("1004"))
		{
			final Npc master = npc.getMaster();
			if (master == null || master.isDead())
			{
				final IntentionType type = npc.getAI().getCurrentIntention().getType();
				if (type != IntentionType.ATTACK && type != IntentionType.CAST)
				{
					npc.deleteMe();
				}
			}
		}
		else if (name.equalsIgnoreCase("1005"))
		{
			if (npc.isInMyTerritory())
			{
				return null;
			}

			final Npc master = npc.getMaster();
			if (master == null || master.isDead())
			{
				return null;
			}

			final IntentionType type = npc.getAI().getCurrentIntention().getType();
			if (type == IntentionType.ATTACK)
			{
				return null;
			}

			((Attackable) npc).getAggroList().clear();
			npc.teleportToMaster();
		}
		return null;
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		((Attackable) npc).getAggroList().addDamageHate(attacker, damage, damage * 100 / (npc.getStatus().getLevel() + 7));
	}

	@Override
	public void onClanAttacked(Attackable caller, Attackable called, Creature attacker, int damage)
	{
		final Npc master = called.getMaster();
		if (master == null || master.isDead())
		{
			called.getAggroList().addDamageHate(attacker, 0, damage * 30 / (called.getStatus().getLevel() + 7));
		}
	}

	@Override
	public void onCreated(Npc npc)
	{
		if (!npc.getTemplate().getMinionData().isEmpty())
		{
			npc.getMinions().clear();

			for (MinionData md : npc.getTemplate().getMinionData())
			{
				for (int i = 0; i < md.getAmount(); i++)
				{
					createOnePrivate(npc, md.getId(), 0, false);
				}
			}
		}
		else if (npc.hasMaster())
		{
			startQuestTimerAtFixedRate("1004", npc, null, 20000, 20000);
			startQuestTimerAtFixedRate("1005", npc, null, 120000, 120000);
		}
	}

	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		final Attackable attackable = ((Attackable) npc);

		// Check if the obj is autoattackable and if not already hating it, add it.
		if (attackable.canAutoAttack(creature) && attackable.getAggroList().getHate(creature) == 0)
		{
			attackable.getAggroList().addDamageHate(creature, 0, 1);
		}
	}

	@Override
	public void onNoDesire(Npc npc)
	{
		final Attackable attackable = (Attackable) npc;

		// Check buffs.
		if (attackable.getAI().canSelfBuff())
		{
			return;
		}

		if (!npc.hasMaster())
		{
			// Return to home if too far.
			// Try to random walk.
			if (attackable.returnHome() || Config.RANDOM_WALK_RATE <= 0 || attackable.isNoRndWalk() || Rnd.get(Config.RANDOM_WALK_RATE) != 0)
			{
				return;
			}
		}

		// Random walk otherwise.
		npc.moveFromSpawnPointUsingRandomOffset(Config.MAX_DRIFT_RANGE);
	}

	@Override
	public void onOutOfTerritory(Npc npc)
	{
		final Attackable attackable = (Attackable) npc;
		attackable.getAI().decreaseAttackTimeout();
	}

	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature attacker, int damage)
	{
		if (!(called instanceof Attackable))
		{
			return;
		}

		final Attackable attackable = (Attackable) called;
		attackable.getAggroList().addDamageHate(attacker, 0, damage * 100 / (called.getStatus().getLevel() + 7));
	}

	@Override
	public void onPartyDied(Npc caller, Npc called)
	{
		if (called.isMaster() && !called.isDead())
		{
			caller.scheduleRespawn((called.isRaidBoss()) ? Config.RAID_MINION_RESPAWN_TIMER : (called.getSpawn().getRespawnDelay() * 1000 / 2));
		}

		if ((caller == null || caller.isDead() && caller.isRaidBoss()))
		{
			ThreadPool.schedule(() -> called.deleteMe(), 5000);
		}
	}

	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if ((caster == null) || !(npc instanceof Attackable))
		{
			return;
		}

		final Npc master = npc.getMaster();
		if (master != null && !master.isDead())
		{
			return;
		}

		final Attackable attackable = (Attackable) npc;
		int skillAggroPoints = skill.getAggroPoints();

		// Do not hate if the skill is a solo target skill, and if the target is player summon OR if the target is the npc and the skill was a positive effect.
		if (targets.length == 1 && ((caster.getSummon() != null && ArraysUtil.contains(targets, caster.getSummon())) || (!skill.isOffensive() && !skill.isDebuff() && ArraysUtil.contains(targets, npc))))
		{
			skillAggroPoints = 0;
		}

		if (skillAggroPoints > 0 && attackable.getAI().getCurrentIntention().getType() == IntentionType.ATTACK)
		{
			final WorldObject npcTarget = attackable.getTarget();
			for (Creature target : targets)
			{
				if (npcTarget == target || npc == target)
				{
					final Creature originalCaster = isPet ? caster.getSummon() : caster;
					attackable.getAggroList().addDamageHate(originalCaster, 0, skillAggroPoints * 150 / (attackable.getStatus().getLevel() + 7));
				}
			}
		}
	}

	/**
	 * @param npc : The {@link Npc} to check.
	 * @return A random {@link Player} out of the {@link Npc} knownlist set as parameter. The {@link Player} can't be dead and can't be hidden aswell.
	 */
	public static Player getRandomPlayer(Npc npc)
	{
		return Rnd.get(npc.getKnownType(Player.class, p -> !p.isAlikeDead() && p.getAppearance().isVisible()));
	}

	/**
	 * @param range : The radius.
	 * @param npc : The {@link Npc} to check.
	 * @return The number of {@link Player}s in a defined radius. {@link Player}s can't be dead and can't be hidden aswell.
	 */
	public static int getPlayersCountInRadius(int range, Creature npc)
	{
		return npc.getKnownTypeInRadius(Player.class, range, p -> !p.isAlikeDead() && p.getAppearance().isVisible()).size();
	}

	/**
	 * @param range : The radius.
	 * @param npc : The {@link Npc} to check.
	 * @return An int array composed of front, back and side targets number. {@link Player}s can't be dead and can't be hidden aswell.
	 */
	public static int[] getPlayersCountInPositions(int range, Creature npc)
	{
		int frontCount = 0;
		int backCount = 0;
		int sideCount = 0;

		for (final Player player : npc.getKnownTypeInRadius(Player.class, range, p -> !p.isAlikeDead() && p.getAppearance().isVisible()))
		{
			if (player.isInFrontOf(npc))
			{
				frontCount++;
			}
			else if (player.isBehind(npc))
			{
				backCount++;
			}
			else
			{
				sideCount++;
			}
		}

		return new int[]
		{
			frontCount,
			backCount,
			sideCount
		};
	}
}