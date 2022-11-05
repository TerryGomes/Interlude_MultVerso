package net.sf.l2j.gameserver.model.actor.ai.type;

import java.util.List;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.NpcAiType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.SiegeGuard;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SiegeGuardAI extends AttackableAI
{
	public SiegeGuardAI(SiegeGuard guard)
	{
		super(guard);
	}
	
	@Override
	protected void peaceMode()
	{
		// An Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10.
		if (updateGlobalAggro() >= 0)
		{
			final Attackable npc = (Attackable) _actor;
			for (Creature obj : npc.getKnownTypeInRadius(Creature.class, npc.getTemplate().getClanRange()))
			{
				// Check if the obj is autoattackable and if not already hating it, add it.
				if (npc.canAutoAttack(obj) && npc.getAggroList().getHate(obj) == 0)
					npc.getAggroList().addDamageHate(obj, 0, 1);
			}
			
			// Choose a target from its aggroList.
			final Creature target = (npc.isConfused()) ? getCurrentIntention().getFinalTarget() : npc.getAggroList().getMostHatedCreature();
			if (target != null)
			{
				// Get the hate level against this Creature target contained in _aggroList
				if (npc.getAggroList().getHate(target) + _globalAggro > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
					_actor.forceRunStance();
					
					// Set the AI Intention to ATTACK
					tryToAttack(target);
				}
				return;
			}
		}
		// Order to the SiegeGuard to return to its home location because there's no target to attack
		getActor().returnHome();
	}
	
	@Override
	protected void combatMode()
	{
		final SiegeGuard actor = getActor();
		
		/**
		 * RETURN HOME<br>
		 * Check if the siege guard isn't too far ; if yes, then move him back to home.
		 */
		if (!actor.isInsideZone(ZoneId.SIEGE))
		{
			setBackToPeace(-10);
			return;
		}
		
		// If timeout is expired or AggroList is empty, set back to peace immediately.
		if (_attackTimeout < System.currentTimeMillis() || actor.getAggroList().isEmpty())
		{
			setBackToPeace(-10);
			return;
		}
		
		// Cleanup aggro list of bad entries.
		actor.getAggroList().refresh();
		
		// Pickup most hated target.
		final AggroInfo ai = actor.getAggroList().getMostHated();
		if (ai == null)
		{
			setBackToPeace(-10);
			return;
		}
		
		Creature target = ai.getAttacker();
		
		// If target is too far, stop hating the current target. Do nothing for this round.
		if (!actor.isIn3DRadius(target, 2000))
		{
			actor.getAggroList().stopHate(target);
			return;
		}
		
		/**
		 * COMMON INFORMATIONS<br>
		 * Used for range and distance check.
		 */
		
		final int actorCollision = (int) actor.getCollisionRadius();
		final int combinedCollision = (int) (actorCollision + target.getCollisionRadius());
		final double dist = actor.distance2D(target);
		
		int range = combinedCollision;
		if (target.isMoving())
			range += 15;
		
		if (actor.isMoving())
			range += 15;
		
		/**
		 * Cast a spell.
		 */
		
		if (willCastASpell())
		{
			// This list is used in order to avoid multiple calls on skills lists. Tests are made one after the other, and content is replaced when needed.
			List<L2Skill> defaultList;
			
			// -------------------------------------------------------------------------------
			// Heal
			defaultList = actor.getTemplate().getSkills(NpcSkillType.HEAL);
			if (!defaultList.isEmpty())
			{
				final String[] clans = actor.getTemplate().getClans();
				
				// Go through all characters around the actor that belongs to its faction.
				for (Creature cha : actor.getKnownTypeInRadius(Creature.class, 1000))
				{
					// Don't bother about dead, not visible, or healthy characters.
					if (cha.isAlikeDead() || !GeoEngine.getInstance().canSeeTarget(actor, cha) || cha.getStatus().getHpRatio() > 0.75)
						continue;
					
					// Will affect only defenders or NPCs from same faction.
					if (!actor.isAttackingDisabled() && (cha instanceof Player && actor.getCastle().getSiege().checkSides(((Player) cha).getClan(), SiegeSide.DEFENDER, SiegeSide.OWNER)) || (cha instanceof Npc && ArraysUtil.contains(clans, ((Npc) cha).getTemplate().getClans())))
					{
						for (L2Skill sk : defaultList)
						{
							useMagic(sk, cha, dist, range + sk.getSkillRadius());
							return;
						}
					}
				}
			}
			
			// -------------------------------------------------------------------------------
			// Buff
			defaultList = actor.getTemplate().getSkills(NpcSkillType.BUFF);
			if (!defaultList.isEmpty())
			{
				for (L2Skill sk : defaultList)
				{
					if (actor.getFirstEffect(sk) == null)
					{
						useMagic(sk, actor, dist, range + sk.getSkillRadius());
						return;
					}
				}
			}
			
			// -------------------------------------------------------------------------------
			// Debuff - 10% luck to get debuffed.
			defaultList = actor.getTemplate().getSkills(NpcSkillType.DEBUFF);
			if (Rnd.get(100) < 10 && !defaultList.isEmpty())
			{
				for (L2Skill sk : defaultList)
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
			defaultList = actor.getTemplate().getSkills(NpcSkillType.SHORT_RANGE);
			if (!defaultList.isEmpty() && dist <= 150)
			{
				final L2Skill skill = Rnd.get(defaultList);
				if (useMagic(skill, target, dist, skill.getCastRange()))
					return;
			}
			else
			{
				defaultList = actor.getTemplate().getSkills(NpcSkillType.LONG_RANGE);
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
		range += actor.getStatus().getPhysicalAttackRange();
		
		if (actor.isMovementDisabled())
		{
			// If distance is too big, choose another target.
			if (dist > range)
				target = actor.getAggroList().reconsiderTarget(range);
			
			// Any AI type, even healer or mage, will try to melee attack if it can't do anything else (desesperate situation).
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
			for (Attackable nearby : actor.getKnownTypeInRadius(Attackable.class, actorCollision))
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
				
				if (!actor.isIn2DRadius(newX, newY, actorCollision))
					actor.getMove().maybeMoveToLocation(new Location(newX, newY, actor.getZ()), 0, true, false);
				
				return;
			}
		}
		
		/**
		 * FLEE CHECK<br>
		 * Test the flee possibility. Archers got 25% chance to flee.
		 */
		
		if (actor.getTemplate().getAiType() == NpcAiType.ARCHER && dist <= (60 + combinedCollision) && Rnd.get(4) < 1)
		{
			getActor().fleeFrom(target, Config.MAX_DRIFT_RANGE);
			return;
		}
		
		/**
		 * BASIC MELEE ATTACK
		 */
		
		tryToAttack(target);
	}
	
	@Override
	public SiegeGuard getActor()
	{
		return (SiegeGuard) _actor;
	}
}