package net.sf.l2j.gameserver.model.actor.ai;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Boat;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * A datatype used as a simple "wish" of an actor, consisting of an {@link IntentionType} and all needed parameters.
 */
public class Intention
{
	private IntentionType _type;
	
	private WorldObject _target;
	private Creature _finalTarget;
	
	private L2Skill _skill;
	private Location _loc;
	private Boat _boat;
	
	private boolean _isCtrlPressed;
	private boolean _isShiftPressed;
	
	private int _itemObjectId;
	
	public Intention()
	{
		_type = IntentionType.IDLE;
	}
	
	@Override
	public String toString()
	{
		return "[Intention type=" + _type.toString() + " target=" + _target + " finalTarget=" + _finalTarget + " skill=" + _skill + " loc=" + _loc + " boat=" + _boat + " isCtrlPressed=" + _isCtrlPressed + " isShiftPressed=" + _isShiftPressed + " itemObjectId=" + _itemObjectId + "]";
	}
	
	public IntentionType getType()
	{
		return _type;
	}
	
	public WorldObject getTarget()
	{
		return _target;
	}
	
	public Creature getFinalTarget()
	{
		return _finalTarget;
	}
	
	public L2Skill getSkill()
	{
		return _skill;
	}
	
	public Location getLoc()
	{
		return _loc;
	}
	
	public Boat getBoat()
	{
		return _boat;
	}
	
	public boolean isCtrlPressed()
	{
		return _isCtrlPressed;
	}
	
	public boolean isShiftPressed()
	{
		return _isShiftPressed;
	}
	
	public int getItemObjectId()
	{
		return _itemObjectId;
	}
	
	/**
	 * Set internally values, used as a shortcut for all "updateAs" methods.
	 * @param type : The new {@link IntentionType} to set.
	 * @param target : A {@link WorldObject} used as target.
	 * @param finalTarget : A {@link Creature} used as target.
	 * @param skill : A {@link L2Skill} used as reference.
	 * @param loc : A {@link Location} used as reference.
	 * @param boat : A {@link Boat} used as reference.
	 * @param isCtrlPressed : A boolean used as reference.
	 * @param isShiftPressed : A boolean used as reference.
	 * @param itemObjectId : An integer used as reference.
	 */
	private synchronized void set(IntentionType type, WorldObject target, Creature finalTarget, L2Skill skill, Location loc, Boat boat, boolean isCtrlPressed, boolean isShiftPressed, int itemObjectId)
	{
		_type = type;
		
		_target = target;
		_finalTarget = finalTarget;
		
		_skill = skill;
		_loc = (loc == null) ? null : loc.clone();
		_boat = boat;
		
		_isCtrlPressed = isCtrlPressed;
		_isShiftPressed = isShiftPressed;
		
		_itemObjectId = itemObjectId;
	}
	
	public synchronized void updateAsActive()
	{
		set(IntentionType.ACTIVE, null, null, null, null, null, false, false, 0);
	}
	
	public synchronized void updateAsAttack(Creature target, boolean isCtrlPressed, boolean isShiftPressed)
	{
		set(IntentionType.ATTACK, null, target, null, null, null, isCtrlPressed, isShiftPressed, 0);
	}
	
	public synchronized void updateAsCast(Creature caster, Creature target, L2Skill skill, boolean isCtrlPressed, boolean isShiftPressed, int itemObjectId)
	{
		set(IntentionType.CAST, null, skill.getFinalTarget(caster, target), skill, null, null, isCtrlPressed, isShiftPressed, itemObjectId);
	}
	
	public synchronized void updateAsFakeDeath(boolean startFakeDeath)
	{
		set(IntentionType.FAKE_DEATH, null, null, null, null, null, startFakeDeath, false, 0);
	}
	
	public synchronized void updateAsFollow(Creature target, boolean isShiftPressed)
	{
		set(IntentionType.FOLLOW, null, target, null, null, null, false, isShiftPressed, 0);
	}
	
	public synchronized void updateAsIdle()
	{
		set(IntentionType.IDLE, null, null, null, null, null, false, false, 0);
	}
	
	public synchronized void updateAsInteract(WorldObject target, boolean isCtrlPressed, boolean isShiftPressed)
	{
		set(IntentionType.INTERACT, target, null, null, null, null, isCtrlPressed, isShiftPressed, 0);
	}
	
	public synchronized void updateAsMoveTo(Location loc, Boat boat)
	{
		set(IntentionType.MOVE_TO, null, null, null, loc, boat, false, false, 0);
	}
	
	public synchronized void updateAsPickUp(int itemObjectId, boolean isShiftPressed)
	{
		set(IntentionType.PICK_UP, null, null, null, null, null, false, isShiftPressed, itemObjectId);
	}
	
	public synchronized void updateAsSit(WorldObject target)
	{
		set(IntentionType.SIT, target, null, null, null, null, false, false, 0);
	}
	
	public synchronized void updateAsStand()
	{
		set(IntentionType.STAND, null, null, null, null, null, false, false, 0);
	}
	
	public synchronized void updateAsUseItem(int itemObjectId)
	{
		set(IntentionType.USE_ITEM, null, null, null, null, null, false, false, itemObjectId);
	}
	
	/**
	 * Update the current {@link Intention} with parameters taken from another {@link Intention}.
	 * @param intention : The {@link Intention} to use as parameters.
	 */
	public synchronized void updateUsing(Intention intention)
	{
		set(intention.getType(), intention.getTarget(), intention.getFinalTarget(), intention.getSkill(), intention.getLoc(), intention.getBoat(), intention.isCtrlPressed(), intention.isShiftPressed(), intention.getItemObjectId());
	}
	
	/**
	 * @return True if the current {@link Intention} got blank parameters.
	 */
	public synchronized boolean isBlank()
	{
		return _type == IntentionType.IDLE && _target == null && _finalTarget == null && _skill == null && _loc == null && _boat == null && _itemObjectId == 0;
	}
}