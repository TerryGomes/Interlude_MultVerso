package net.sf.l2j.gameserver.model.actor.ai.type;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Boat;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.Intention;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStart;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStop;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * A layer used to process {@link Intention}s of an {@link Creature}.<br>
 * <br>
 * Each {@link Creature} holds 3 {@link Intention}s : past, current and future.<br>
 * <br>
 * AI behavior is then affected by potential {@link AiEventType}s, or by own AI type.
 */
abstract class AbstractAI
{
	protected final Creature _actor;
	
	protected Intention _previousIntention = new Intention();
	protected Intention _currentIntention = new Intention();
	protected Intention _nextIntention = new Intention();
	
	protected AbstractAI(Creature actor)
	{
		_actor = actor;
	}
	
	protected abstract void onEvtAggression(Creature target, int aggro);
	
	protected abstract void onEvtArrived();
	
	protected abstract void onEvtArrivedBlocked();
	
	protected abstract void onEvtAttacked(Creature attacker);
	
	protected abstract void onEvtBowAttackReuse();
	
	protected abstract void onEvtCancel();
	
	protected abstract void onEvtDead();
	
	protected abstract void onEvtEvaded(Creature attacker);
	
	protected abstract void onEvtFinishedAttack();
	
	protected abstract void onEvtFinishedAttackBow();
	
	protected abstract void onEvtFinishedCasting();
	
	protected abstract void onEvtOwnerAttacked(Creature attacker);
	
	protected abstract void onEvtSatDown(WorldObject target);
	
	protected abstract void onEvtStoodUp();
	
	protected abstract void onEvtTeleported();
	
	protected abstract void thinkActive();
	
	protected abstract void thinkAttack();
	
	protected abstract void thinkCast();
	
	protected abstract void thinkFakeDeath();
	
	protected abstract void thinkFollow();
	
	protected abstract void thinkIdle();
	
	protected abstract void thinkInteract();
	
	protected abstract void thinkMoveTo();
	
	protected abstract ItemInstance thinkPickUp();
	
	protected abstract void thinkSit();
	
	protected abstract void thinkStand();
	
	protected abstract void thinkUseItem();
	
	@Override
	public String toString()
	{
		return "Actor: " + _actor;
	}
	
	public Creature getActor()
	{
		return _actor;
	}
	
	public final synchronized Intention getPreviousIntention()
	{
		return _previousIntention;
	}
	
	protected final synchronized void setPreviousIntention(Intention intention)
	{
		_previousIntention.updateUsing(intention);
	}
	
	public final synchronized Intention getCurrentIntention()
	{
		return _currentIntention;
	}
	
	public final synchronized Intention getNextIntention()
	{
		return _nextIntention;
	}
	
	protected final synchronized void setNextIntention(Intention intention)
	{
		_nextIntention.updateUsing(intention);
	}
	
	/**
	 * The core method used by sub-intentions, doing following actions :
	 * <ul>
	 * <li>Stop follow movement if necessary.</li>
	 * <li>Refresh previous intention with current {@link Intention}.</li>
	 * <li>Reset next {@link Intention}.</li>
	 * </ul>
	 */
	protected synchronized void prepareIntention()
	{
		_actor.getMove().cancelFollowTask();
		
		// Refresh previous intention with current intention.
		setPreviousIntention(_currentIntention);
		
		// Reset next intention.
		_nextIntention.updateAsIdle();
	}
	
	protected synchronized void doActiveIntention()
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsActive();
		
		thinkActive();
	}
	
	protected synchronized void doAttackIntention(Creature target, boolean isCtrlPressed, boolean isShiftPressed)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsAttack(target, isCtrlPressed, isShiftPressed);
		
		thinkAttack();
	}
	
	protected synchronized void doCastIntention(Creature target, L2Skill skill, boolean isCtrlPressed, boolean isShiftPressed, int itemObjectId)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsCast(getActor(), target, skill, isCtrlPressed, isShiftPressed, itemObjectId);
		
		thinkCast();
	}
	
	protected synchronized void doFakeDeathIntention(boolean startFakeDeath)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsFakeDeath(startFakeDeath);
		
		thinkFakeDeath();
	}
	
	protected synchronized void doFollowIntention(Creature target, boolean isShiftPressed)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsFollow(target, isShiftPressed);
		
		thinkFollow();
	}
	
	protected synchronized void doIdleIntention()
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsIdle();
		
		thinkIdle();
	}
	
	protected synchronized void doInteractIntention(WorldObject target, boolean isCtrlPressed, boolean isShiftPressed)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsInteract(target, isCtrlPressed, isShiftPressed);
		
		thinkInteract();
	}
	
	protected synchronized void doMoveToIntention(Location loc, Boat boat)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsMoveTo(loc, boat);
		
		thinkMoveTo();
	}
	
	protected synchronized void doPickUpIntention(int itemObjectId, boolean isShiftPressed)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsPickUp(itemObjectId, isShiftPressed);
		
		thinkPickUp();
	}
	
	protected synchronized void doSitIntention(WorldObject target)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsSit(target);
		
		thinkSit();
	}
	
	protected synchronized void doStandIntention()
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsStand();
		
		thinkStand();
	}
	
	protected synchronized void doUseItemIntention(int itemObjectId)
	{
		prepareIntention();
		
		// Feed related values.
		_currentIntention.updateAsUseItem(itemObjectId);
		
		thinkUseItem();
	}
	
	/**
	 * Launch the onIntention method corresponding to an existing {@link Intention}.
	 * @param intention : The new {@link Intention} to set.
	 */
	protected final synchronized void doIntention(Intention intention)
	{
		switch (intention.getType())
		{
			case ACTIVE:
				doActiveIntention();
				break;
			
			case ATTACK:
				doAttackIntention(intention.getFinalTarget(), intention.isCtrlPressed(), intention.isShiftPressed());
				break;
			
			case CAST:
				doCastIntention(intention.getFinalTarget(), intention.getSkill(), intention.isCtrlPressed(), intention.isShiftPressed(), intention.getItemObjectId());
				break;
			
			case FAKE_DEATH:
				doFakeDeathIntention(intention.isCtrlPressed());
				break;
			
			case FOLLOW:
				doFollowIntention(intention.getFinalTarget(), intention.isShiftPressed());
				break;
			
			case IDLE:
				doIdleIntention();
				break;
			
			case INTERACT:
				doInteractIntention(intention.getTarget(), intention.isCtrlPressed(), intention.isShiftPressed());
				break;
			
			case MOVE_TO:
				doMoveToIntention(intention.getLoc(), intention.getBoat());
				break;
			
			case PICK_UP:
				doPickUpIntention(intention.getItemObjectId(), intention.isShiftPressed());
				break;
			
			case SIT:
				doSitIntention(intention.getTarget());
				break;
			
			case STAND:
				doStandIntention();
				break;
			
			case USE_ITEM:
				doUseItemIntention(intention.getItemObjectId());
				break;
		}
	}
	
	public synchronized void tryToActive()
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.ACTIVE))
		{
			getNextIntention().updateAsActive();
			clientActionFailed();
			return;
		}
		
		doActiveIntention();
	}
	
	public synchronized void tryToAttack(Creature target, boolean isCtrlPressed, boolean isShiftPressed)
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.ATTACK))
		{
			getNextIntention().updateAsAttack(target, isCtrlPressed, isShiftPressed);
			clientActionFailed();
			return;
		}
		
		doAttackIntention(target, isCtrlPressed, isShiftPressed);
	}
	
	public synchronized void tryToAttack(Creature target)
	{
		tryToAttack(target, false, false);
	}
	
	public synchronized void tryToCast(Creature target, L2Skill skill, boolean isCtrlPressed, boolean isShiftPressed, int itemObjectId)
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		final Creature finalTarget = skill.getFinalTarget(_actor, target);
		if (finalTarget == null || !_actor.getCast().canAttemptCast(finalTarget, skill))
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.CAST))
		{
			getNextIntention().updateAsCast(_actor, target, skill, isCtrlPressed, isShiftPressed, itemObjectId);
			clientActionFailed();
			return;
		}
		
		doCastIntention(target, skill, isCtrlPressed, isShiftPressed, itemObjectId);
	}
	
	public synchronized void tryToCast(Creature target, L2Skill skill)
	{
		tryToCast(target, skill, false, false, 0);
	}
	
	public synchronized void tryToCast(Creature target, int id, int level)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
		if (skill != null)
			tryToCast(target, skill, false, false, 0);
	}
	
	public synchronized void tryToFollow(Creature target, boolean isShiftPressed)
	{
		if (_actor == target || _actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.FOLLOW))
		{
			getNextIntention().updateAsFollow(target, isShiftPressed);
			clientActionFailed();
			return;
		}
		
		doFollowIntention(target, isShiftPressed);
	}
	
	public synchronized void tryToIdle()
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.IDLE))
		{
			getNextIntention().updateAsIdle();
			clientActionFailed();
			return;
		}
		
		doIdleIntention();
	}
	
	public synchronized void tryToInteract(WorldObject target, boolean isCtrlPressed, boolean isShiftPressed)
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.INTERACT))
		{
			getNextIntention().updateAsInteract(target, isCtrlPressed, isShiftPressed);
			clientActionFailed();
			return;
		}
		
		doInteractIntention(target, isCtrlPressed, isShiftPressed);
	}
	
	public synchronized void tryToMoveTo(Location loc, Boat boat)
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.MOVE_TO))
		{
			getNextIntention().updateAsMoveTo(loc, boat);
			clientActionFailed();
			return;
		}
		
		doMoveToIntention(loc, boat);
	}
	
	public synchronized void tryToPickUp(int itemObjectId, boolean isShiftPressed)
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.PICK_UP))
		{
			getNextIntention().updateAsPickUp(itemObjectId, isShiftPressed);
			clientActionFailed();
			return;
		}
		
		doPickUpIntention(itemObjectId, isShiftPressed);
	}
	
	public synchronized void tryToSit(WorldObject target)
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.SIT))
		{
			getNextIntention().updateAsSit(target);
			return;
		}
		
		doSitIntention(target);
	}
	
	public synchronized void tryToStand()
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.STAND))
		{
			getNextIntention().updateAsStand();
			return;
		}
		
		doStandIntention();
	}
	
	public synchronized void tryToUseItem(int itemObjectId)
	{
		if (_actor.denyAiAction())
		{
			clientActionFailed();
			return;
		}
		
		// These situations are waited out regardless. Any Intention that is added is scheduled as nextIntention.
		if (_actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow() || _actor.isSittingNow() || _actor.isStandingNow() || canScheduleAfter(_currentIntention.getType(), IntentionType.USE_ITEM))
		{
			getNextIntention().updateAsUseItem(itemObjectId);
			return;
		}
		
		doUseItemIntention(itemObjectId);
	}
	
	/**
	 * Launch the onEvt method corresponding to the {@link AiEventType} set as parameter.<br>
	 * <br>
	 * <font color=#FF0000><b><u>Caution</u>: The current general intention won't be change (ex: if the character attack and is stunned, he will attack again after the stunned period).</b></font>
	 * @param evt : The {@link AiEventType} whose the AI must be notified.
	 * @param firstParameter : The first {@link Object} parameter of the event.
	 * @param secondParameter : The second {@link Object} parameter of the event.
	 */
	public final void notifyEvent(AiEventType evt, Object firstParameter, Object secondParameter)
	{
		if ((!_actor.isVisible() && !_actor.isTeleporting()) || !_actor.hasAI())
			return;
		
		switch (evt)
		{
			case THINK:
				onEvtThink();
				break;
			
			case ATTACKED:
				onEvtAttacked((Creature) firstParameter);
				break;
			
			case AGGRESSION:
				onEvtAggression((Creature) firstParameter, ((Number) secondParameter).intValue());
				break;
			
			case EVADED:
				onEvtEvaded((Creature) firstParameter);
				break;
			
			case FINISHED_ATTACK:
				onEvtFinishedAttack();
				break;
			
			case FINISHED_ATTACK_BOW:
				onEvtFinishedAttackBow();
				break;
			
			case BOW_ATTACK_REUSED:
				onEvtBowAttackReuse();
				break;
			
			case ARRIVED:
				onEvtArrived();
				break;
			
			case ARRIVED_BLOCKED:
				onEvtArrivedBlocked();
				break;
			
			case CANCEL:
				onEvtCancel();
				break;
			
			case DEAD:
				onEvtDead();
				break;
			
			case FINISHED_CASTING:
				onEvtFinishedCasting();
				break;
			
			case SAT_DOWN:
				onEvtSatDown((WorldObject) firstParameter);
				break;
			
			case STOOD_UP:
				onEvtStoodUp();
				break;
			
			case OWNER_ATTACKED:
				onEvtOwnerAttacked((Creature) firstParameter);
				break;
			
			case TELEPORTED:
				onEvtTeleported();
				break;
		}
	}
	
	protected synchronized void onEvtThink()
	{
		switch (_currentIntention.getType())
		{
			case ACTIVE:
				thinkActive();
				break;
			
			case ATTACK:
				thinkAttack();
				break;
			
			case CAST:
				thinkCast();
				break;
			
			case FAKE_DEATH:
				thinkFakeDeath();
				break;
			
			case FOLLOW:
				thinkFollow();
				break;
			
			case IDLE:
				thinkIdle();
				break;
			
			case INTERACT:
				thinkInteract();
				break;
			
			case MOVE_TO:
				thinkMoveTo();
				break;
			
			case PICK_UP:
				thinkPickUp();
				break;
			
			case SIT:
				thinkSit();
				break;
			
			case STAND:
				thinkStand();
				break;
			
			case USE_ITEM:
				thinkUseItem();
				break;
		}
	}
	
	/**
	 * This method holds behavioral information on which Intentions are scheduled and which are cast immediately.
	 * <ul>
	 * <li>Nothing is scheduled after FOLLOW, PICK_UP, INTERACT.</li>
	 * <li>Any action that occurs during isAttackingNow / isCastingNow is automatically scheduled, so checks for ATTACK and CAST are useless.</li>
	 * <li>Only STAND is scheduled after SIT and FAKE_DEATH. Anything else is illegal and is cast immediately so it can be rejected.</li>
	 * <li>Only SIT is scheduled after MOVE_TO. Anything else is cast immediately.</li>
	 * <li>All possible intentions are scheduled after STAND.</li>
	 * </ul>
	 * @param oldIntention : The {@link IntentionType} to test against.
	 * @param newIntention : The {@link IntentionType} to test.
	 * @return True if the {@link IntentionType} set as parameter can be sheduled after this {@link IntentionType}, otherwise cast it immediately.
	 */
	public boolean canScheduleAfter(IntentionType oldIntention, IntentionType newIntention)
	{
		switch (oldIntention)
		{
			case SIT:
			case FAKE_DEATH:
				return newIntention == IntentionType.STAND;
			
			case STAND:
				return true;
			
			case MOVE_TO:
				return newIntention == IntentionType.SIT;
		}
		return false;
	}
	
	/**
	 * Cancel action client side by sending ActionFailed packet to the Player actor.
	 */
	public void clientActionFailed()
	{
	}
	
	/**
	 * Activate the attack stance, broadcasting {@link AutoAttackStart} packet. Refresh the timer if already registered on {@link AttackStanceTaskManager}.
	 */
	public void startAttackStance()
	{
		// Initial check ; if the actor wasn't yet registered into AttackStanceTaskManager, broadcast AutoAttackStart packet.
		if (!AttackStanceTaskManager.getInstance().isInAttackStance(_actor))
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
		
		// Set out of the initial if check to be able to refresh the time.
		AttackStanceTaskManager.getInstance().add(_actor);
	}
	
	/**
	 * Deactivate the attack stance, broadcasting {@link AutoAttackStop} packet if the actor was registered on {@link AttackStanceTaskManager}.
	 */
	public void stopAttackStance()
	{
		// If we successfully remove the actor from AttackStanceTaskManager, we also broadcast AutoAttackStop packet.
		if (AttackStanceTaskManager.getInstance().remove(_actor))
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
	}
	
	/**
	 * Send the state of this actor to a {@link Player}.
	 * @param player : The {@link Player} to notify with the state of this actor.
	 */
	public void describeStateToPlayer(Player player)
	{
		if (_currentIntention.getType() == IntentionType.MOVE_TO)
			_actor.getMove().describeMovementTo(player);
		// else if (getIntention() == CtrlIntention.CAST) TODO
	}
	
	/**
	 * Stop all tasks related to AI.
	 */
	public void stopAITask()
	{
		_actor.getMove().cancelFollowTask();
	}
	
	/**
	 * @param target : The {@link WorldObject} used as parameter.
	 * @return True if the given {@link WorldObject} is missing out (null, not registered, not on same region).
	 */
	public boolean isTargetLost(WorldObject target)
	{
		if (target == null)
			return true;
		
		if (World.getInstance().getObject(target.getObjectId()) == null)
			return true;
		
		return !getActor().knows(target);
	}
	
	/**
	 * @param target : The {@link WorldObject} used as parameter.
	 * @param skill : The {@link L2Skill} used as parameter.
	 * @return True if the given {@link WorldObject} is missing out (null, not registered, not on same region), giving a given {@link L2Skill}.
	 */
	public boolean isTargetLost(WorldObject target, L2Skill skill)
	{
		if (target == null)
			return true;
		
		if (World.getInstance().getObject(target.getObjectId()) == null)
			return true;
		
		if (skill != null && skill.getSkillType() == SkillType.SUMMON_FRIEND)
			return false;
		
		return !getActor().knows(target);
	}
}