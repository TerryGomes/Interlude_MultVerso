package net.sf.l2j.gameserver.model.actor.ai.type;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.enums.actors.NpcAiType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.FestivalMonster;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AttackableAI extends CreatureAI implements Runnable
{
	protected static final int MAX_ATTACK_TIMEOUT = 90000; // 1m30
	
	private final Set<Creature> _seenCreatures = ConcurrentHashMap.newKeySet();
	
	protected Future<?> _aiTask;
	
	protected long _attackTimeout;
	
	protected int _globalAggro;
	
	protected boolean _isInCombatMode;
	
	public AttackableAI(Attackable attackable)
	{
		super(attackable);
		
		_attackTimeout = Long.MAX_VALUE;
		_globalAggro = -10;
		_seenCreatures.clear();
		_isInCombatMode = false;
	}
	
	@Override
	public Attackable getActor()
	{
		return (Attackable) _actor;
	}
	
	@Override
	public void run()
	{
		if (!_isInCombatMode)
			peaceMode();
		else
			combatMode();
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
			_aiTask = ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
		
		getActor().startRandomAnimationTimer();
	}
	
	@Override
	protected void thinkAttack()
	{
		if (!_isInCombatMode)
		{
			_isInCombatMode = true;
			_attackTimeout = System.currentTimeMillis() + MAX_ATTACK_TIMEOUT;
		}
		
		canSelfBuff();
		
		super.thinkAttack();
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
			notifyEvent(AiEventType.THINK, null, null);
		else
			doIntention(_nextIntention);
	}
	
	@Override
	protected void onEvtArrived()
	{
		if (_currentIntention.getType() == IntentionType.FOLLOW)
			return;
		
		if (_nextIntention.isBlank())
		{
			if (_currentIntention.getType() == IntentionType.MOVE_TO)
			{
				if (getActor().isReturningToSpawnPoint())
					getActor().setIsReturningToSpawnPoint(false);
				
				doActiveIntention();
			}
			else
				notifyEvent(AiEventType.THINK, null, null);
		}
		else
			doIntention(_nextIntention);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		getActor().addAttacker(attacker);
		
		onEvtAggression(attacker, 1);
		
		super.onEvtAttacked(attacker);
	}
	
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		final Attackable me = getActor();
		
		// Calculate the attack timeout
		_attackTimeout = System.currentTimeMillis() + MAX_ATTACK_TIMEOUT;
		
		// Add the target to the AggroList or update hate if already present
		me.getAggroList().addDamageHate(target, 0, aggro);
		
		// Set the Intention to ATTACK and make the character running, but only if the AI isn't disabled.
		if (!me.isCoreAiDisabled() && !_isInCombatMode)
		{
			me.forceRunStance();
			tryToAttack(target);
		}
		
		if (me instanceof Monster)
		{
			Monster master = (Monster) me;
			
			if (master.hasMinions())
				master.getMinionList().onAssist(me, target);
			else
			{
				master = master.getMaster();
				if (master != null && master.hasMinions())
					master.getMinionList().onAssist(me, target);
			}
		}
		
		// Faction check.
		final String[] actorClans = me.getTemplate().getClans();
		if (actorClans != null)
		{
			for (final Attackable called : me.getKnownTypeInRadius(Attackable.class, me.getTemplate().getClanRange()))
			{
				// Called hasn't AI, is dead, or got already target registered.
				if (!called.hasAI() || called.isDead() || called.getAggroList().containsKey(target))
					continue;
				
				// Caller clan doesn't correspond to the called clan.
				if (!ArraysUtil.contains(actorClans, called.getTemplate().getClans()))
					continue;
				
				// Called ignores that type of caller id.
				if (ArraysUtil.contains(called.getTemplate().getIgnoredIds(), me.getNpcId()))
					continue;
				
				// Check if the Attackable is in the LoS of the caller.
				if (!GeoEngine.getInstance().canSeeTarget(me, called))
					continue;
				
				// Retrieve scripts associated to called Attackable and notify the faction call.
				for (Quest quest : called.getTemplate().getEventQuests(ScriptEventType.ON_FACTION_CALL))
					quest.notifyFactionCall(me, called, target);
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
		
		// Cancel the AI
		_actor.detachAI();
	}
	
	/**
	 * Manage AI when not engaged in combat.
	 */
	protected void peaceMode()
	{
		final Attackable npc = getActor();
		
		if (_attackTimeout != Long.MAX_VALUE)
			_attackTimeout = Long.MAX_VALUE;
		
		// An Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10.
		if (updateGlobalAggro() >= 0 && !npc.isReturningToSpawnPoint())
		{
			final List<Quest> scripts = npc.getTemplate().getEventQuests(ScriptEventType.ON_CREATURE_SEE);
			
			// Get all visible objects inside its Aggro Range
			for (final Creature obj : npc.getKnownType(Creature.class))
			{
				// Check to see if this is a festival mob spawn. If it is, then check to see if the aggro trigger is a festival participant...if so, move to attack it.
				if (npc instanceof FestivalMonster && obj instanceof Player && !((Player) obj).isFestivalParticipant())
					continue;
				
				// ON_CREATURE_SEE implementation.
				if (!scripts.isEmpty())
				{
					final boolean isInRange = npc.isIn3DRadius(obj, 400);
					if (_seenCreatures.contains(obj))
					{
						if (!isInRange)
							_seenCreatures.remove(obj);
					}
					else if (isInRange)
					{
						_seenCreatures.add(obj);
						
						for (final Quest quest : scripts)
							quest.notifyCreatureSee(npc, obj);
					}
				}
				
				// Check if the obj is autoattackable and if not already hating it, add it.
				if (npc.canAutoAttack(obj) && npc.getAggroList().getHate(obj) == 0)
					npc.getAggroList().addDamageHate(obj, 0, 0);
				
			}
			
			// TODO Review that section, maybe simply use _combatMode ?
			if (!npc.isCoreAiDisabled())
			{
				// Choose a target from its aggroList.
				final Creature target = (npc.isConfused()) ? getCurrentIntention().getFinalTarget() : npc.getAggroList().getMostHatedCreature();
				if (target != null)
				{
					// Get the hate level of the Attackable against this Creature obj contained in _aggroList
					if (npc.getAggroList().getHate(target) + _globalAggro > 0)
					{
						// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
						npc.forceRunStance();
						
						// Set the AI Intention to ATTACK
						tryToAttack(target);
					}
					return;
				}
			}
		}
		
		// If this is a festival monster, then it remains in the same location.
		if (npc instanceof FestivalMonster)
			return;
		
		// Check buffs.
		if (canSelfBuff())
			return;
		
		// Minions following leader.
		final Attackable master = npc.getMaster();
		if (master != null && !master.isAlikeDead())
		{
			if (master.isRunning())
				npc.forceRunStance();
			else
				npc.forceWalkStance();
			
			final int maxOffset = (int) (100 + npc.getCollisionRadius() + master.getCollisionRadius());
			if (npc.distance3D(master) > maxOffset)
			{
				final int minOffset = (int) (master.getCollisionRadius() + 30);
				
				final Location loc = master.getPosition().clone();
				loc.addRandomOffsetBetweenTwoValues(minOffset, maxOffset);
				loc.set(GeoEngine.getInstance().getValidLocation(master, loc));
				
				npc.getAI().tryToMoveTo(loc, null);
			}
		}
		else
		{
			// Return to home if too far.
			if (npc.returnHome())
				return;
			
			// Random walk otherwise.
			if (Config.RANDOM_WALK_RATE > 0 && !npc.isNoRndWalk() && Rnd.get(Config.RANDOM_WALK_RATE) == 0)
				npc.moveFromSpawnPointUsingRandomOffset(Config.MAX_DRIFT_RANGE);
		}
	}
	
	public void setBackToPeace(int globalAggro)
	{
		getActor().getAggroList().clear();
		
		_isInCombatMode = false;
		_globalAggro = globalAggro;
		
		tryToActive();
		
		getActor().forceWalkStance();
	}
	
	/**
	 * Manage AI when engaged in combat.
	 */
	protected void combatMode()
	{
		final Attackable npc = getActor();
		
		// Corpse AIs, as AI scripts, are stopped here.
		if (npc.isCoreAiDisabled() || npc.isAfraid())
			return;
		
		// If timeout is expired or AggroList is empty, set back to peace immediately.
		if (_attackTimeout < System.currentTimeMillis() || npc.getAggroList().isEmpty())
		{
			setBackToPeace(-10);
			return;
		}
		
		// Cleanup aggro list of bad entries.
		npc.getAggroList().refresh();
		
		// Pickup most hated target.
		final AggroInfo ai = npc.getAggroList().getMostHated();
		if (ai == null)
		{
			setBackToPeace(-10);
			return;
		}
		
		Creature target = ai.getAttacker();
		
		// If target is too far, stop hating the current target. Do nothing for this round.
		if (!npc.isIn3DRadius(target, 2000))
		{
			npc.getAggroList().stopHate(target);
			return;
		}
		
		/**
		 * COMMON INFORMATIONS<br>
		 * Used for range and distance check.
		 */
		
		final int actorCollision = (int) npc.getCollisionRadius();
		final int combinedCollision = (int) (actorCollision + target.getCollisionRadius());
		final double dist = npc.distance2D(target);
		
		int range = combinedCollision;
		
		// Needed for all the useMagic calls
		getActor().setTarget(target);
		
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
			defaultList = npc.getTemplate().getSkills(NpcSkillType.SUICIDE);
			if (!defaultList.isEmpty() && npc.getStatus().getHpRatio() < 0.15)
			{
				final L2Skill skill = Rnd.get(defaultList);
				
				if (useMagic(skill, target, dist, range + skill.getSkillRadius()))
					return;
			}
			
			// -------------------------------------------------------------------------------
			// Heal
			defaultList = npc.getTemplate().getSkills(NpcSkillType.HEAL);
			if (!defaultList.isEmpty())
			{
				// First priority is to heal the master.
				final Attackable master = npc.getMaster();
				if (master != null && !master.isDead() && master.getStatus().getHpRatio() < 0.75)
				{
					for (final L2Skill sk : defaultList)
					{
						if (sk.getTargetType() == SkillTargetType.SELF)
							continue;
						
						useMagic(sk, master, dist, range + sk.getSkillRadius());
						return;
					}
				}
				
				// Second priority is to heal self.
				if (npc.getStatus().getHpRatio() < 0.75)
				{
					for (final L2Skill sk : defaultList)
					{
						useMagic(sk, npc, dist, range + sk.getSkillRadius());
						return;
					}
				}
				
				// Third priority is to heal clan
				for (final L2Skill sk : defaultList)
				{
					if (sk.getTargetType() == SkillTargetType.ONE)
					{
						final String[] actorClans = npc.getTemplate().getClans();
						for (final Attackable obj : npc.getKnownTypeInRadius(Attackable.class, sk.getCastRange() + actorCollision))
						{
							if (obj.isDead())
								continue;
							
							if (!ArraysUtil.contains(actorClans, obj.getTemplate().getClans()))
								continue;
							
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
			defaultList = npc.getTemplate().getSkills(NpcSkillType.BUFF);
			if (!defaultList.isEmpty())
			{
				for (final L2Skill sk : defaultList)
				{
					if (npc.getFirstEffect(sk) == null)
					{
						useMagic(sk, npc, dist, range + sk.getSkillRadius());
						npc.setTarget(target);
						return;
					}
				}
			}
			
			// -------------------------------------------------------------------------------
			// Debuff - 10% luck to get debuffed.
			defaultList = npc.getTemplate().getSkills(NpcSkillType.DEBUFF);
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
			defaultList = npc.getTemplate().getSkills(NpcSkillType.SHORT_RANGE);
			if (!defaultList.isEmpty() && dist <= 150)
			{
				final L2Skill skill = Rnd.get(defaultList);
				
				if (useMagic(skill, target, dist, skill.getCastRange()))
					return;
			}
			else
			{
				defaultList = npc.getTemplate().getSkills(NpcSkillType.LONG_RANGE);
				if (!defaultList.isEmpty() && dist > 150)
				{
					final L2Skill skill = Rnd.get(defaultList);
					
					if (useMagic(skill, target, dist, skill.getCastRange()))
						return;
				}
			}
		}
		
		/**
		 * MELEE CHECK<br>
		 * The mob failed a skill check ; make him flee if AI authorizes it, else melee attack.
		 */
		
		// The range takes now in consideration physical attack range.
		range += npc.getStatus().getPhysicalAttackRange();
		
		if (npc.isMovementDisabled())
		{
			// If distance is too big, choose another target.
			if (dist > range)
				target = npc.getAggroList().reconsiderTarget(range);
			
			// Any AI type, even healer or mage, will try to melee attack if it can't do anything else (desperate situation).
			if (target != null)
				tryToAttack(target);
			
			return;
		}
		
		/**
		 * MOVE AROUND CHECK<br>
		 * In case many mobs are trying to hit from same place, move a bit, circling around the target
		 */
		
		if (Rnd.get(100) <= 3)
		{
			for (final Attackable nearby : npc.getKnownTypeInRadius(Attackable.class, actorCollision))
			{
				if (nearby == target)
					continue;
				
				int newX = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
					newX = target.getX() + newX;
				else
					newX = target.getX() - newX;
				
				int newY = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
					newY = target.getY() + newY;
				else
					newY = target.getY() - newY;
				
				if (!npc.isIn2DRadius(newX, newY, actorCollision))
					tryToMoveTo(new Location(newX, newY, npc.getZ() + 30), null);
				
				return;
			}
		}
		
		/**
		 * FLEE CHECK<br>
		 * Test the flee possibility. Archers got 25% chance to flee.
		 */
		
		if (npc.getTemplate().getAiType() == NpcAiType.ARCHER && dist <= (60 + combinedCollision) && Rnd.get(4) < 1)
		{
			getActor().fleeFrom(target, Config.MAX_DRIFT_RANGE);
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
			return false;
		
		final Attackable caster = getActor();
		
		switch (sk.getSkillType())
		{
			case BUFF:
				if (caster.getFirstEffect(sk) == null)
				{
					tryToCast(originalTarget, sk);
					return true;
				}
				
				// ----------------------------------------
				// If actor already have buff, start looking at others same faction mob to cast
				if (sk.getTargetType() == SkillTargetType.SELF)
					return false;
				
				if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = caster.getAggroList().reconsiderTarget(sk.getCastRange());
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
					final Attackable master = caster.getMaster();
					if (master != null && !master.isDead() && Rnd.get(100) > (master.getStatus().getHpRatio() * 100))
					{
						tryToCast(master, sk);
						return true;
					}
				}
				
				// Personal case.
				double percentage = caster.getStatus().getHpRatio() * 100;
				if (Rnd.get(100) < (100 - percentage) / 3)
				{
					tryToCast(caster, sk);
					return true;
				}
				
				if (sk.getTargetType() == SkillTargetType.ONE)
				{
					for (final Attackable obj : caster.getKnownTypeInRadius(Attackable.class, (int) (sk.getCastRange() + caster.getCollisionRadius())))
					{
						if (obj.isDead())
							continue;
						
						if (!ArraysUtil.contains(caster.getTemplate().getClans(), obj.getTemplate().getClans()))
							continue;
						
						percentage = obj.getStatus().getHpRatio() * 100;
						if (Rnd.get(100) < (100 - percentage) / 10)
						{
							if (GeoEngine.getInstance().canSeeTarget(caster, obj))
							{
								tryToCast(obj, sk);
								return true;
							}
						}
					}
				}
				
				if (sk.getTargetType() == SkillTargetType.PARTY)
				{
					for (final Attackable obj : caster.getKnownTypeInRadius(Attackable.class, (int) (sk.getSkillRadius() + caster.getCollisionRadius())))
					{
						if (!ArraysUtil.contains(caster.getTemplate().getClans(), obj.getTemplate().getClans()))
							continue;
						
						if (obj.getStatus().getHpRatio() < 1.0 && Rnd.get(100) < 20)
						{
							tryToCast(caster, sk);
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
				if (GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !canAOE(sk, originalTarget) && !originalTarget.isDead() && distance <= range)
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
					
					if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = caster.getAggroList().reconsiderTarget(sk.getCastRange());
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
					
					final Creature target = caster.getAggroList().reconsiderTarget(sk.getCastRange());
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
					
					if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;
			
			case ROOT:
			case STUN:
			case PARALYZE:
				if (GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !canAOE(sk, originalTarget) && distance <= range)
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
					else if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = caster.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;
			
			case MUTE:
			case FEAR:
				if (GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !canAOE(sk, originalTarget) && distance <= range)
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
					
					if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.ONE)
				{
					final Creature target = caster.getAggroList().reconsiderTarget(sk.getCastRange());
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
					return true;
				
				if (sk.getTargetType() == SkillTargetType.ONE)
				{
					if (originalTarget.getFirstEffect(EffectType.BUFF) != null && GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
					
					final Creature target = caster.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(target, sk);
						caster.setTarget(originalTarget);
						return true;
					}
				}
				else if (canAOE(sk, originalTarget))
				{
					if ((sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA) && GeoEngine.getInstance().canSeeTarget(caster, originalTarget))
					{
						tryToCast(originalTarget, sk);
						return true;
					}
					else if ((sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA) && GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
				}
				break;
			
			default:
				if (!canAura(sk, originalTarget))
				{
					if (GeoEngine.getInstance().canSeeTarget(caster, originalTarget) && !originalTarget.isDead() && distance <= range)
					{
						tryToCast(originalTarget, sk);
						return true;
					}
					
					final Creature target = caster.getAggroList().reconsiderTarget(sk.getCastRange());
					if (target != null)
					{
						tryToCast(target, sk);
						caster.setTarget(originalTarget);
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
		switch (getActor().getTemplate().getAiType())
		{
			case HEALER:
			case MAGE:
				return !getActor().isMuted();
			
			default:
				if (getActor().isPhysicalMuted())
					return false;
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
				_globalAggro++;
			else
				_globalAggro--;
		}
		return _globalAggro;
	}
	
	private boolean canSelfBuff()
	{
		if (Config.RANDOM_WALK_RATE > 0 && Rnd.get(Config.RANDOM_WALK_RATE) != 0)
			return false;
		
		for (final L2Skill sk : getActor().getTemplate().getSkills(NpcSkillType.BUFF))
		{
			if (getActor().getFirstEffect(sk) != null)
				continue;
			
			tryToCast(_actor, sk);
			return true;
		}
		
		return false;
	}
	
	private boolean canParty(L2Skill sk)
	{
		// Only TARGET_PARTY skills are allowed to be tested.
		if (sk.getTargetType() != SkillTargetType.PARTY)
			return false;
		
		// Retrieve actor factions.
		final String[] actorClans = getActor().getTemplate().getClans();
		
		// Test all Attackable around skill radius.
		for (final Attackable target : getActor().getKnownTypeInRadius(Attackable.class, sk.getSkillRadius()))
		{
			// Can't see the target, continue.
			if (!GeoEngine.getInstance().canSeeTarget(getActor(), target))
				continue;
			
			// Faction doesn't match, continue.
			if (!ArraysUtil.contains(actorClans, target.getTemplate().getClans()))
				continue;
			
			// Return true if at least one target is missing the buff.
			if (target.getFirstEffect(sk) == null)
				return true;
		}
		return false;
	}
	
	protected boolean canAura(L2Skill sk, Creature originalTarget)
	{
		if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
		{
			for (final WorldObject target : getActor().getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
			{
				if (target == originalTarget)
					return true;
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
				for (final Creature target : getActor().getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(getActor(), target))
						continue;
					
					if (target instanceof Attackable && !getActor().isConfused())
						continue;
					
					if (target.getFirstEffect(sk) != null)
						cancast = false;
				}
				
				if (cancast)
					return true;
			}
			else if (sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA)
			{
				boolean cancast = true;
				for (final Creature target : originalTarget.getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(getActor(), target))
						continue;
					
					if (target instanceof Attackable && !getActor().isConfused())
						continue;
					
					final AbstractEffect[] effects = target.getAllEffects();
					if (effects.length > 0)
						cancast = true;
				}
				if (cancast)
					return true;
			}
		}
		else
		{
			if (sk.getTargetType() == SkillTargetType.AURA || sk.getTargetType() == SkillTargetType.BEHIND_AURA || sk.getTargetType() == SkillTargetType.FRONT_AURA)
			{
				boolean cancast = false;
				for (final Creature target : getActor().getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(getActor(), target))
						continue;
					
					if (target instanceof Attackable && !getActor().isConfused())
						continue;
					
					final AbstractEffect[] effects = target.getAllEffects();
					if (effects.length > 0)
						cancast = true;
				}
				if (cancast)
					return true;
			}
			else if (sk.getTargetType() == SkillTargetType.AREA || sk.getTargetType() == SkillTargetType.FRONT_AREA)
			{
				boolean cancast = true;
				for (final Creature target : originalTarget.getKnownTypeInRadius(Creature.class, sk.getSkillRadius()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(getActor(), target))
						continue;
					
					if (target instanceof Attackable && !getActor().isConfused())
						continue;
					
					if (target.getFirstEffect(sk) != null)
						cancast = false;
				}
				
				if (cancast)
					return true;
			}
		}
		return false;
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
}