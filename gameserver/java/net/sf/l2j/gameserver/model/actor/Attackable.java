package net.sf.l2j.gameserver.model.actor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.ai.type.AttackableAI;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.attack.AttackableAttack;
import net.sf.l2j.gameserver.model.actor.container.attackable.AggroList;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.FriendlyMonster;
import net.sf.l2j.gameserver.model.actor.instance.Guard;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.RiftInvader;
import net.sf.l2j.gameserver.model.actor.status.AttackableStatus;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * This class manages all {@link Npc}s which can hold an {@link AggroList}.
 */
public class Attackable extends Npc
{
	private final AggroList _aggroList = new AggroList(this);
	
	private final Set<Creature> _attackedBy = ConcurrentHashMap.newKeySet();
	
	private boolean _isReturningToSpawnPoint;
	private boolean _seeThroughSilentMove;
	private boolean _isNoRndWalk;
	
	public Attackable(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) _status;
	}
	
	@Override
	public void setStatus()
	{
		_status = new AttackableStatus(this);
	}
	
	@Override
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				ai = _ai;
				if (ai == null)
					_ai = ai = new AttackableAI(this);
			}
		}
		return ai;
	}
	
	@Override
	public void setAttack()
	{
		_attack = new AttackableAttack(this);
	}
	
	@Override
	public void addKnownObject(WorldObject object)
	{
		// If the new object is a Player and our AI was IDLE, we set it to ACTIVE.
		if (object instanceof Player && getAI().getCurrentIntention().getType() == IntentionType.IDLE)
			getAI().tryToActive();
	}
	
	@Override
	public void removeKnownObject(WorldObject object)
	{
		super.removeKnownObject(object);
		
		// Delete the object from aggro list.
		if (object instanceof Creature)
			getAggroList().remove(object);
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, L2Skill skill)
	{
		reduceCurrentHp(damage, attacker, true, false, skill);
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		// Test the ON_KILL ScriptEventType.
		for (Quest quest : getTemplate().getEventQuests(ScriptEventType.ON_KILL))
			ThreadPool.schedule(() -> quest.notifyKill(this, killer), 3000);
		
		_attackedBy.clear();
		
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Clear the aggro list.
		_aggroList.clear();
		
		forceWalkStance();
		
		// Stop the AI if region is inactive.
		if (!isInActiveRegion() && hasAI())
			getAI().stopAITask();
	}
	
	@Override
	public int calculateRandomAnimationTimer()
	{
		return Rnd.get(Config.MIN_MONSTER_ANIMATION, Config.MAX_MONSTER_ANIMATION);
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return Config.MAX_MONSTER_ANIMATION > 0 && !isRaidRelated();
	}
	
	@Override
	public void onInteract(Player player)
	{
		// Attackables cannot be INTERACTed with
	}
	
	@Override
	public void onActiveRegion()
	{
		// Override Npc random timer animation.
	}
	
	@Override
	public void onInactiveRegion()
	{
		// Stop all active skills effects in progress.
		stopAllEffects();
		
		// Clear data.
		getAggroList().clear();
		getAttackByList().clear();
		
		// Stop all AI related tasks.
		if (hasAI())
			getAI().tryToIdle();
	}
	
	@Override
	public void forceAttack(Creature creature, int hate)
	{
		forceRunStance();
		getAggroList().addDamageHate(creature, 0, hate);
		getAI().tryToAttack(creature);
	}
	
	/**
	 * Add a {@link Creature} attacker on _attackedBy {@link List}.
	 * @param attacker : The {@link Creature} to add.
	 */
	public void addAttacker(Creature attacker)
	{
		if (attacker == null || attacker == this)
			return;
		
		_attackedBy.add(attacker);
	}
	
	/**
	 * @return True if the {@link Attackable} successfully returned to spawn point. In case of minions, they are simply deleted.
	 */
	public boolean returnHome()
	{
		// TODO Is this necessary?
		// Do nothing if the Attackable is already dead.
		if (isDead())
			return false;
		
		// TODO Gordon temporary bypass (the only attackable i can think of that doesn't return to its spawn)
		if (getNpcId() == 29095)
			return false;
		
		// Minions are simply squeezed if they lose activity.
		if (isMinion() && !isRaidRelated())
		{
			deleteMe();
			return true;
		}
		
		// For regular Attackable, we check if a spawn exists, and if we're far from it (using drift range).
		if (getSpawn() != null && !isIn2DRadius(getSpawn().getLoc(), getDriftRange()))
		{
			_aggroList.cleanAllHate();
			
			setIsReturningToSpawnPoint(true);
			forceWalkStance();
			getAI().tryToMoveTo(getSpawn().getLoc(), null);
			return true;
		}
		return false;
	}
	
	public int getDriftRange()
	{
		return Config.MAX_DRIFT_RANGE;
	}
	
	public final Set<Creature> getAttackByList()
	{
		return _attackedBy;
	}
	
	public final AggroList getAggroList()
	{
		return _aggroList;
	}
	
	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}
	
	public final void setIsReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}
	
	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}
	
	public void seeThroughSilentMove(boolean value)
	{
		_seeThroughSilentMove = value;
	}
	
	public final boolean isNoRndWalk()
	{
		return _isNoRndWalk;
	}
	
	public final void setNoRndWalk(boolean value)
	{
		_isNoRndWalk = value;
	}
	
	/**
	 * @return The {@link ItemInstance} used as weapon of this {@link Attackable} (null by default).
	 */
	public ItemInstance getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * @return The {@link Attackable} leader of this {@link Attackable}, or null if this {@link Attackable} isn't linked to any master.
	 */
	public Attackable getMaster()
	{
		return null;
	}
	
	public boolean isGuard()
	{
		return false;
	}
	
	/**
	 * The range used by default is getTemplate().getAggroRange().
	 * @param target : The targeted {@link Creature}.
	 * @return True if the {@link Creature} used as target is autoattackable, or false otherwise.
	 * @see #canAutoAttack(Creature)
	 */
	public boolean canAutoAttack(Creature target)
	{
		return canAutoAttack(target, getTemplate().getAggroRange(), false);
	}
	
	/**
	 * @param target : The targeted {@link Creature}.
	 * @param range : The range to check.
	 * @param allowPeaceful : If true, peaceful {@link Attackable}s are able to auto-attack.
	 * @return True if the {@link Creature} used as target is autoattackable, or false otherwise.
	 */
	public boolean canAutoAttack(Creature target, int range, boolean allowPeaceful)
	{
		// Check if the target isn't null, a Door or dead.
		if (target == null || target instanceof Door || target.isAlikeDead())
			return false;
		
		if (target instanceof Playable)
		{
			// Check if target is in the Aggro range
			if (!isIn3DRadius(target, range))
				return false;
			
			// Check if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode
			if (!(isRaidRelated()) && !(canSeeThroughSilentMove()) && ((Playable) target).isSilentMoving())
				return false;
			
			// Check if the target is a Player
			final Player targetPlayer = target.getActingPlayer();
			if (targetPlayer != null)
			{
				// GM checks ; check if the target is invisible or got access level
				if (targetPlayer.isGM() && !targetPlayer.getAppearance().isVisible())
					return false;
				
				// Check if player is an allied Varka.
				if (ArraysUtil.contains(getTemplate().getClans(), "varka_silenos_clan") && targetPlayer.isAlliedWithVarka())
					return false;
				
				// Check if player is an allied Ketra.
				if (ArraysUtil.contains(getTemplate().getClans(), "ketra_orc_clan") && targetPlayer.isAlliedWithKetra())
					return false;
				
				// check if the target is within the grace period for JUST getting up from fake death
				if (targetPlayer.isRecentFakeDeath())
					return false;
				
				if (this instanceof RiftInvader && targetPlayer.isInParty() && targetPlayer.getParty().isInDimensionalRift() && !targetPlayer.getParty().getDimensionalRift().isInCurrentRoomZone(this))
					return false;
			}
		}
		
		if (this instanceof Guard)
		{
			// Check if the Playable target has karma.
			if (target instanceof Playable && target.getActingPlayer().getKarma() > 0)
				return GeoEngine.getInstance().canSeeTarget(this, target);
			
			// Check if the Monster target is aggressive.
			if (target instanceof Monster && Config.GUARD_ATTACK_AGGRO_MOB)
				return (((Monster) target).isAggressive() && GeoEngine.getInstance().canSeeTarget(this, target));
			
			return false;
		}
		else if (this instanceof FriendlyMonster)
		{
			// Check if the Playable target has karma.
			if (target instanceof Playable && target.getActingPlayer().getKarma() > 0)
				return GeoEngine.getInstance().canSeeTarget(this, target);
			
			return false;
		}
		else
		{
			if (target instanceof Attackable && isConfused())
				return GeoEngine.getInstance().canSeeTarget(this, target);
			
			if (target instanceof Npc)
				return false;
			
			// Depending on Config, do not allow mobs to attack players in PEACE zones, unless they are already following those players outside.
			if (!Config.MOB_AGGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE))
				return false;
			
			// Check if the actor is Aggressive
			return ((allowPeaceful || isAggressive()) && GeoEngine.getInstance().canSeeTarget(this, target));
		}
	}
}