package net.sf.l2j.gameserver.model.actor.ai.type;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcAiType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.FestivalMonster;
import net.sf.l2j.gameserver.model.entity.CursedWeapon;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AttackableAI<T extends Attackable> extends NpcAI<T> implements Runnable
{
	private final Set<Creature> _seenCreatures = ConcurrentHashMap.newKeySet();

	protected Future<?> _aiTask;

	protected int _attackTimeout;

	protected int _globalAggro;

	protected boolean _isInCombatMode;

	public AttackableAI(T attackable)
	{
		super(attackable);

		_attackTimeout = 60;
		_globalAggro = -10;
		_seenCreatures.clear();
		_isInCombatMode = false;
	}

	@Override
	public void run()
	{
		if (!_isInCombatMode)
		{
			peaceMode();
		}
		else
		{
			combatMode();
		}
	}

	@Override
	protected void thinkIdle()
	{
		// If the region is active and actor isn't dead, set the intention as ACTIVE.
		if (!_actor.isAlikeDead() && _actor.isInActiveRegion())
		{
			doActiveIntention();
			return;
		}

		// The intention is still IDLE ; we detach the AI and stop both AI and follow tasks.
		stopAITask();

		super.thinkIdle();

		_isInCombatMode = false;
	}

	@Override
	protected void thinkActive()
	{
		super.thinkActive();

		// Create an AI task (schedule onEvtThink every second).
		if (_aiTask == null)
		{
			_aiTask = ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
		}

		_actor.startRandomAnimationTimer();
	}

	@Override
	protected void thinkAttack()
	{
		_isInCombatMode = true;

		canSelfBuff();

		super.thinkAttack();
	}

	@Override
	protected ItemInstance thinkPickUp()
	{
		if (_actor.denyAiAction())
		{
			doActiveIntention();
			return null;
		}

		final WorldObject target = World.getInstance().getObject(_currentIntention.getItemObjectId());
		if (!(target instanceof ItemInstance) || isTargetLost(target))
		{
			doActiveIntention();
			return null;
		}

		final ItemInstance item = (ItemInstance) target;
		if (item.getLocation() != ItemLocation.VOID)
		{
			doActiveIntention();
			return null;
		}

		if (_actor.getMove().maybeMoveToLocation(target.getPosition(), 36, false, false))
		{
			return null;
		}

		final CursedWeapon cw = CursedWeaponManager.getInstance().getCursedWeapon(item.getItemId());
		if (cw != null)
		{
			cw.endOfLife();
		}
		else
		{
			item.decayMe();
		}

		return item;
	}

	@Override
	protected void onEvtFinishedAttackBow()
	{
		// Attackables that use a bow do not do anything until the attack is fully reused (equivalent of the Player red gauge bar).
	}

	@Override
	protected void onEvtBowAttackReuse()
	{
		if (_nextIntention.isBlank())
		{
			notifyEvent(AiEventType.THINK, null, null);
		}
		else
		{
			doIntention(_nextIntention);
		}
	}

	@Override
	protected void onEvtArrived()
	{
		if (_currentIntention.getType() == IntentionType.FOLLOW)
		{
			return;
		}

		if (_nextIntention.isBlank())
		{
			if (_currentIntention.getType() == IntentionType.MOVE_TO)
			{
				if (_actor.isReturningToSpawnPoint())
				{
					_actor.setIsReturningToSpawnPoint(false);
				}

				doActiveIntention();
			}
			else
			{
				notifyEvent(AiEventType.THINK, null, null);
			}
		}
		else
		{
			doIntention(_nextIntention);
		}
	}

	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		_actor.addAttacker(attacker);

		onEvtAggression(attacker, 10);

		super.onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		// Reset the attack timeout.
		_attackTimeout = 60;

		// Add the target to the AggroList or update hate if already present.
		_actor.getAggroList().addDamageHate(target, 0, aggro);

		// Set the Intention to ATTACK and make the character running, but only if the AI isn't disabled.
		if (!_actor.isCoreAiDisabled() && !_isInCombatMode)
		{
			_actor.forceRunStance();
			tryToAttack(target);
		}

		// Party aggro (minion/master).
		if (_actor.isMaster() || _actor.hasMaster())
		{
			// If we have a master, we call the event.
			final Npc master = _actor.getMaster();
			if (master != null && !master.isDead())
			{
				// Retrieve scripts associated to called Attackable and notify the party call.
				for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.PARTY_ATTACKED))
				{
					quest.onPartyAttacked(_actor, master, target, aggro);
				}
			}

			// For all minions except me, we call the event.
			for (Npc minion : _actor.getMinions())
			{
				if (minion == _actor || minion.isDead())
				{
					continue;
				}

				// Retrieve scripts associated to called Attackable and notify the party call.
				for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.PARTY_ATTACKED))
				{
					quest.onPartyAttacked(_actor, minion, target, aggro);
				}
			}
		}

		// Social aggro.
		final String[] actorClans = _actor.getTemplate().getClans();
		if (actorClans != null && _actor.getTemplate().getClanRange() > 0)
		{
			for (final Attackable called : _actor.getKnownTypeInRadius(Attackable.class, _actor.getTemplate().getClanRange()))
			{
				// Called is dead.
				

				// Caller clan doesn't correspond to the called clan.
				

				// Called ignores that type of caller id.
				// Check if the Attackable is in the LoS of the caller.
				if (called.isDead() || !ArraysUtil.contains(actorClans, called.getTemplate().getClans()) || ArraysUtil.contains(called.getTemplate().getIgnoredIds(), _actor.getNpcId()) || !GeoEngine.getInstance().canSeeTarget(_actor, called))
				{
					continue;
				}

				// Retrieve scripts associated to called Attackable and notify the clan call.
				for (Quest quest : called.getTemplate().getEventQuests(EventHandler.CLAN_ATTACKED))
				{
					quest.onClanAttacked(_actor, called, target, aggro);
				}
			}
		}
	}

	@Override
	public void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		super.stopAITask();
	}

	/**
	 * This method holds behavioral information on which Intentions are scheduled and which are cast immediately.
	 * <ul>
	 * <li>All possible intentions are scheduled for AttackableAI.</li>
	 * </ul>
	 * @param oldIntention : The {@link IntentionType} to test against.
	 * @param newIntention : The {@link IntentionType} to test.
	 * @return True if the {@link IntentionType} set as parameter can be sheduled after this {@link IntentionType}, otherwise cast it immediately.
	 */
	@Override
	public boolean canScheduleAfter(IntentionType oldIntention, IntentionType newIntention)
	{
		return false;
	}

	public void decreaseAttackTimeout()
	{
		if (_attackTimeout > 0)
		{
			_attackTimeout--;
		}
	}

	/**
	 * Manage AI when not engaged in combat.
	 */
	protected void peaceMode()
	{
		// Reset the attack timeout.
		_attackTimeout = 60;

		// An Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10.
		if (updateGlobalAggro() >= 0 && !_actor.isReturningToSpawnPoint())
		{
			final List<Quest> scripts = _actor.getTemplate().getEventQuests(EventHandler.SEE_CREATURE);

			// Get all visible objects inside its Aggro Range
			for (final Creature obj : _actor.getKnownType(Creature.class))
			{
				// Check to see if this is a festival mob spawn. If it is, then check to see if the aggro trigger is a festival participant...if so, move to attack it.
				if (_actor instanceof FestivalMonster && obj instanceof Player && !((Player) obj).isFestivalParticipant())
				{
					continue;
				}

				// ON_CREATURE_SEE implementation.
				if (!scripts.isEmpty())
				{
					final boolean isInRange = _actor.isIn3DRadius(obj, Math.max(500, _actor.getTemplate().getAggroRange()));
					if (_seenCreatures.contains(obj))
					{
						if (!isInRange)
						{
							_seenCreatures.remove(obj);
						}
					}
					else if (isInRange)
					{
						_seenCreatures.add(obj);

						for (final Quest quest : scripts)
						{
							quest.onSeeCreature(_actor, obj);
						}
					}
				}
			}

			if (!_actor.isCoreAiDisabled())
			{
				// Choose a target from its aggroList.
				final Creature target = (_actor.isConfused()) ? getCurrentIntention().getFinalTarget() : _actor.getAggroList().getMostHatedCreature();
				if (target != null)
				{
					// Get the hate level of the Attackable against this Creature obj contained in _aggroList
					if (_actor.getAggroList().getHate(target) + _globalAggro > 0)
					{
						// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
						_actor.forceRunStance();

						// Set the AI Intention to ATTACK
						tryToAttack(target);
					}
					return;
				}
			}
		}

		// If this is a festival monster, then it remains in the same location.
		if (_actor instanceof FestivalMonster)
		{
			return;
		}

		// Retrieve scripts associated to NO_DESIRE.
		for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.NO_DESIRE))
		{
			quest.onNoDesire(_actor);
		}
	}

	public void setBackToPeace(int globalAggro)
	{
		_actor.getAggroList().clear();

		_isInCombatMode = false;
		_globalAggro = globalAggro;

		tryToActive();

		_actor.forceWalkStance();
	}

	/**
	 * Manage AI when engaged in combat.
	 */
	protected void combatMode()
	{
		// Corpse AIs, as AI scripts, are stopped here.
		if (_actor.isCoreAiDisabled() || _actor.isAfraid())
		{
			return;
		}

		// Retrieve scripts associated to OUT_OF_TERRITORY events, and fire them.
		if (!_actor.isInMyTerritory())
		{
			for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.OUT_OF_TERRITORY))
			{
				quest.onOutOfTerritory(_actor);
			}
		}
		// We're back or didn't leave territory, reset the attack timeout.
		else
		{
			_attackTimeout = 60;
		}

		// If timeout is expired or AggroList is empty, set back to peace immediately.
		if (_attackTimeout <= 0 || _actor.getAggroList().isEmpty())
		{
			setBackToPeace(-10);
			return;
		}

		// Cleanup aggro list of bad entries.
		_actor.getAggroList().refresh();

		// Pickup most hated target.
		final AggroInfo ai = _actor.getAggroList().getMostHated();
		if (ai == null)
		{
			setBackToPeace(-10);
			return;
		}

		Creature target = ai.getAttacker();

		// If target is too far, stop hating the current target. Do nothing for this round.
		if (!_actor.isIn3DRadius(target, 2000))
		{
			_actor.getAggroList().stopHate(target);
			return;
		}

		/**
		 * COMMON INFORMATIONS<br>
		 * Used for range and distance check.
		 */

		final int actorCollision = (int) _actor.getCollisionRadius();
		final int combinedCollision = (int) (actorCollision + target.getCollisionRadius());
		final double dist = _actor.distance2D(target);

		int range = combinedCollision;

		// Needed for all the useMagic calls
		_actor.setTarget(target);

		/**
		 * CAST CHECK<br>
		 * The mob succeeds a skill check ; make all possible checks to define the skill to launch. If nothing is found, go in MELEE CHECK.<br>
		 * It will check skills arrays in that order :
		 * <ul>
		 * <li>suicide skill at 15% max HPs</li>
		 * <li>buff skill if such effect isn't existing</li>
		 * <li>heal skill if self or ally is under 75% HPs (priority to others healers and mages)</li>
		 * <li>debuff skill if such effect isn't existing</li>
		 * <li>damage skill, in that order : short range and long range</li>
		 * </ul>
		 */

		if (willCastASpell())
		{
			// This list is used in order to avoid multiple calls on skills lists. Tests are made one after the other, and content is replaced when needed.
			List<L2Skill> defaultList;

			// -------------------------------------------------------------------------------
			// Suicide possibility if HPs are < 15%.
			defaultList = _actor.getTemplate().getSkills(NpcSkillType.SUICIDE);
			if (!defaultList.isEmpty() && _actor.getStatus().getHpRatio() < 0.15)
			{
				final L2Skill skill = Rnd.get(defaultList);

				if (useMagic(skill, target, dist, range + skill.getSkillRadius()))
				{
					return;
				}
			}

			// -------------------------------------------------------------------------------
			// Heal
			defaultList = _actor.getTemplate().getSkills(NpcSkillType.HEAL);
			if (!defaultList.isEmpty())
			{
				// First priority is to heal the master.
				final Npc master = _actor.getMaster();
				if (master != null && !master.isDead() && master.getStatus().getHpRatio() < 0.75)
				{
					for (final L2Skill sk : defaultList)
					{
						if (sk.getTargetType() == SkillTargetType.SELF)
						{
							continue;
						}

						useMagic(sk, master, dist, range + sk.getSkillRadius());
						return;
					}
				}

				// Second priority is to heal self.
				if (_actor.getStatus().getHpRatio() < 0.75)
				{
					for (final L2Skill sk : defaultList)
					{
						useMagic(sk, _actor, dist, range + sk.getSkillRadius());
						return;
					}
				}

				// Third priority is to heal clan
				for (final L2Skill sk : defaultList)
				{
					if (sk.getTargetType() == SkillTargetType.ONE)
					{
						final String[] actorClans = _actor.getTemplate().getClans();
						for (final Attackable obj : _actor.getKnownTypeInRadius(Attackable.class, sk.getCastRange() + actorCollision))
						{
							if (obj.isDead() || !ArraysUtil.contains(actorClans, obj.getTemplate().getClans()))
							{
								continue;
							}

							if (obj.getStatus().getHpRatio() < 0.75)
							{
								useMagic(sk, obj, dist, range + sk.getSkillRadius());
								return;
							}
						}
					}
				}
			}

			// -------------------------------------------------------------------------------
			// Buff
			defaultList = _actor.getTemplate().getSkills(NpcSkillType.BUFF);
			if (!defaultList.isEmpty())
			{
				for (final L2Skill sk : defaultList)
				{
					if (_actor.getFirstEffect(sk) == null)
					{
						useMagic(sk, _actor, dist, range + sk.getSkillRadius());
						_actor.setTarget(target);
						return;
					}
				}
			}

			// -------------------------------------------------------------------------------
			// Debuff - 10% luck to get debuffed.
			defaultList = _actor.getTemplate().getSkills(NpcSkillType.DEBUFF);
			if (Rnd.get(100) < 10 && !defaultList.isEmpty())
			{
				for (final L2Skill sk : defaultList)
				{
					if (target.getFirstEffect(sk) == null)
					{
						useMagic(sk, target, dist, range + sk.getSkillRadius());
						return;
					}
				}
			}

			// -------------------------------------------------------------------------------
			// General attack skill - short range is checked, then long range.
			defaultList = _actor.getTemplate().getSkills(NpcSkillType.SHORT_RANGE);
			if (!defaultList.isEmpty() && dist <= 150)
			{
				final L2Skill skill = Rnd.get(defaultList);

				if (useMagic(skill, target, dist, skill.getCastRange()))
				{
					return;
				}
			}
			else
			{
				defaultList = _actor.getTemplate().getSkills(NpcSkillType.LONG_RANGE);
				if (!defaultList.isEmpty() && dist > 150)
				{
					final L2Skill skill = Rnd.get(defaultList);

					if (useMagic(skill, target, dist, skill.getCastRange()))
					{
						return;
					}
				}
			}
		}

		/**
		 * MELEE CHECK<br>
		 * The mob failed a skill check ; make him flee if AI authorizes it, else melee attack.
		 */

		// The range takes now in consideration physical attack range.
		range += _actor.getStatus().getPhysicalAttackRange();

		if (_actor.isMovementDisabled())
		{
			// If distance is too big, choose another target.
			if (dist > range)
			{
				target = _actor.getAggroList().reconsiderTarget(range);
			}

			// Any AI type, even healer or mage, will try to melee attack if it can't do anything else (desperate situation).
			if (target != null)
			{
				tryToAttack(target);
			}

			return;
		}

		/**
		 * MOVE AROUND CHECK<br>
		 * In case many mobs are trying to hit from same place, move a bit, circling around the target
		 */

		if (Rnd.get(100) <= 3)
		{
			for (final Attackable nearby : _actor.getKnownTypeInRadius(Attackable.class, actorCollision))
			{
				if (nearby == target)
				{
					continue;
				}

				int newX = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newX = target.getX() + newX;
				}
				else
				{
					newX = target.getX() - newX;
				}

				int newY = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newY = target.getY() + newY;
				}
				else
				{
					newY = target.getY() - newY;
				}

				if (!_actor.isIn2DRadius(newX, newY, actorCollision))
				{
					tryToMoveTo(new Location(newX, newY, _actor.getZ() + 30), null);
				}

				return;
			}
		}

		/**
		 * FLEE CHECK<br>
		 * Test the flee possibility. Archers got 25% chance to flee.
		 */

		if (_actor.getTemplate().getAiType() == NpcAiType.ARCHER && dist <= (60 + combinedCollision) && Rnd.get(4) < 1)
		{
			_actor.fleeFrom(target, Config.MAX_DRIFT_RANGE);
			return;
		}

		/**
		 * BASIC MELEE ATTACK
		 */

		tryToAttack(target);
	}

	protected boolean useMagic(L2Skill sk, Creature originalTarget, double distance, int range)
	{
		if (sk == null || originalTarget == null)
		{
			return false;
		}

		switch (sk.getSkillType())
		{
			case BUFF:
				if (_actor.getFirstEffect(sk) == null)
				{
					tryToCast(originalTarget, sk);
					return true;
				}

				// ----------------------------------------
				// If actor already have buff, start looking at others same faction mob to cast
				if (sk.getTargetType() == SkillTargetType.SELF)
				{
					return false;
				}

				if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = _actor.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(target, sk);
						return true;
					}
				}

				if (canParty(sk))
				{
					tryToCast(originalTarget, sk);
					return true;
				}
				break;

			case HEAL:
			case HOT:
			case HEAL_PERCENT:
				// case HEAL_STATIC:
			case BALANCE_LIFE:
				// Minion case.
				if (sk.getTargetType() != SkillTargetType.SELF)
				{
					final Npc master = _actor.getMaster();
					if (master != null && !master.isDead() && Rnd.get(100) > (master.getStatus().getHpRatio() * 100))
					{
						tryToCast(master, sk);
						return true;
					}
				}

				// Personal case.
				double percentage = _actor.getStatus().getHpRatio() * 100;
				if (Rnd.get(100) < (100 - percentage) / 3)
				{
					tryToCast(_actor, sk);
					return true;
				}

				if (sk.getTargetType() == SkillTargetType.ONE)
				{
					for (final Attackable obj : _actor.getKnownTypeInRadius(Attackable.class, (int) (sk.getCastRange() + _actor.getCollisionRadius())))
					{
						if (obj.isDead() || !ArraysUtil.contains(_actor.getTemplate().getClans(), obj.getTemplate().getClans()))
						{
							continue;
						}

						percentage = obj.getStatus().getHpRatio() * 100;
						if (Rnd.get(100) < (100 - percentage) / 10)
						{
							if (GeoEngine.getInstance().canSeeTarget(_actor, obj))
							{
								tryToCast(obj, sk);
								return true;
							}
						}
					}
				}

				if (sk.getTargetType() == SkillTargetType.PARTY)
				{
					for (final Attackable obj : _actor.getKnownTypeInRadius(Attackable.class, (int) (sk.getSkillRadius() + _actor.getCollisionRadius())))
					{
						if (!ArraysUtil.contains(_actor.getTemplate().getClans(), obj.getTemplate().getClans()))
						{
							continue;
						}

						if (obj.getStatus().getHpRatio() < 1.0 && Rnd.get(100) < 20)
						{
							tryToCast(_actor, sk);
							return true;
						}
					}
				}
				break;

			case DEBUFF:
			case POISON:
			case DOT:
			case MDOT:
			case BLEED:
				if (GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !canAOE(sk, originalTarget) && !originalTarget.isDead() && distance <= range)
				{
					if (originalTarget.getFirstEffect(sk) == null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (canAOE(sk, originalTarget))
				{
					if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
					{
						tryToCast(originalTarget, sk);
						return true;
					}

					if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = _actor.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;

			case SLEEP:
				if (sk.getTargetType() == SkillTargetType.ONE)
				{
					if (!originalTarget.isDead() && distance <= range)
					{
						if (distance > range || originalTarget.isMoving())
						{
							if (originalTarget.getFirstEffect(sk) == null)
							{
								tryToCast(originalTarget, sk);
								return true;
							}
						}
					}

					final Creature target = _actor.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (canAOE(sk, originalTarget))
				{
					if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
					{
						tryToCast(originalTarget, sk);
						return true;
					}

					if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;

			case ROOT:
			case STUN:
			case PARALYZE:
				if (GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !canAOE(sk, originalTarget) && distance <= range)
				{
					if (originalTarget.getFirstEffect(sk) == null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (canAOE(sk, originalTarget))
				{
					if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
					else if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = _actor.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;

			case MUTE:
			case FEAR:
				if (GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !canAOE(sk, originalTarget) && distance <= range)
				{
					if (originalTarget.getFirstEffect(sk) == null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (canAOE(sk, originalTarget))
				{
					if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
					{
						tryToCast(originalTarget, sk);
						return true;
					}

					if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = _actor.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;

			case CANCEL:
			case NEGATE:
				// decrease cancel probability
				if (Rnd.get(50) != 0)
				{
					return true;
				}

				if (sk.getTargetType() == SkillTargetType.ONE)
				{
					if (originalTarget.getFirstEffect(EffectType.BUFF) != null && GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}

					final Creature target = _actor.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(target, sk);
						_actor.setTarget(originalTarget);
						return true;
					}
				}
				else if (canAOE(sk, originalTarget))
				{
					if ((sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA) && GeoEngine.getInstance().canSeeTarget(_actor, originalTarget))
					{
						tryToCast(originalTarget, sk);
						return true;
					}
					else if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;

			default:
				if (!canAura(sk, originalTarget))
				{
					if (GeoEngine.getInstance().canSeeTarget(_actor, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}

					final Creature target = _actor.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(target, sk);
						_actor.setTarget(originalTarget);
						return true;
					}
				}
				else
				{
					tryToCast(originalTarget, sk);
					return true;
				}
				break;
		}

		return false;
	}

	/**
	 * This method checks if the actor will cast a skill or not.
	 * @return true if the actor will cast a spell, false otherwise.
	 */
	protected boolean willCastASpell()
	{
		switch (_actor.getTemplate().getAiType())
		{
			case HEALER:
			case MAGE:
				return !_actor.isMuted();

			default:
				if (_actor.isPhysicalMuted())
				{
					return false;
				}
		}
		return Rnd.get(100) < 10;
	}

	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}

	protected int updateGlobalAggro()
	{
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		return _globalAggro;
	}

	public boolean canSelfBuff()
	{
		if (Config.RANDOM_WALK_RATE > 0 && Rnd.get(Config.RANDOM_WALK_RATE) != 0)
		{
			return false;
		}

		for (final L2Skill sk : _actor.getTemplate().getSkills(NpcSkillType.BUFF))
		{
			if (_actor.getFirstEffect(sk) != null)
			{
				continue;
			}

			tryToCast(_actor, sk);
			return true;
		}

		return false;
	}

	private boolean canParty(L2Skill sk)
	{
		// Only TARGET_PARTY skills are allowed to be tested.
		if (sk.getTargetType() != SkillTargetType.PARTY)
		{
			return false;
		}

		// Retrieve actor factions.
		final String[] actorClans = _actor.getTemplate().getClans();

		// Test all Attackable around skill radius.
		for (final Attackable target : _actor.getKnownTypeInRadius(Attackable.class, sk.getSkillRadius()))
		{
			// Can't see the target, continue.
			// Faction doesn't match, continue.
			if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || !ArraysUtil.contains(actorClans, target.getTemplate().getClans()))
			{
				continue;
			}

			// Return true if at least one target is missing the buff.
			if (target.getFirstEffect(sk) == null)
			{
				return true;
			}
		}
		return false;
	}

	protected boolean canAura(L2Skill sk, Creature originalTarget)
	{
		if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
		{
			for (final WorldObject target : _actor.getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
			{
				if (target == originalTarget)
				{
					return true;
				}
			}
		}
		return false;
	}

	private boolean canAOE(L2Skill sk, Creature originalTarget)
	{
		if (sk.getSkillType() != SkillType.NEGATE || sk.getSkillType() != SkillType.CANCEL)
		{
			if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
			{
				boolean cancast = true;
				for (final Creature target : _actor.getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target instanceof Attackable && !_actor.isConfused()))
					{
						continue;
					}

					if (target.getFirstEffect(sk) != null)
					{
						cancast = false;
					}
				}

				if (cancast)
				{
					return true;
				}
			}
			else if (sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA)
			{
				boolean cancast = true;
				for (final Creature target : originalTarget.getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target instanceof Attackable && !_actor.isConfused()))
					{
						continue;
					}

					final AbstractEffect[] effects = target.getAllEffects();
					if (effects.length > 0)
					{
						cancast = true;
					}
				}
				if (cancast)
				{
					return true;
				}
			}
		}
		else if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
		{
			boolean cancast = false;
			for (final Creature target : _actor.getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
			{
				if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target instanceof Attackable && !_actor.isConfused()))
				{
					continue;
				}

				final AbstractEffect[] effects = target.getAllEffects();
				if (effects.length > 0)
				{
					cancast = true;
				}
			}
			if (cancast)
			{
				return true;
			}
		}
		else if (sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA)
		{
			boolean cancast = true;
			for (final Creature target : originalTarget.getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
			{
				if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target instanceof Attackable && !_actor.isConfused()))
				{
					continue;
				}

				if (target.getFirstEffect(sk) != null)
				{
					cancast = false;
				}
			}

			if (cancast)
			{
				return true;
			}
		}
		return false;
	}
}