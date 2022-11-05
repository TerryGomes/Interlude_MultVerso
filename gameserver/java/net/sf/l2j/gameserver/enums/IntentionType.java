package net.sf.l2j.gameserver.enums;

/**
 * Enumeration of generic intentions of an actor.
 */
public enum IntentionType
{
	/** Alerted state without goal : scan attackable targets, random walk, etc. */
	ACTIVE,
	/** Move to target if too far, then attack it - may be ignored (another target, invalid zoning, etc). */
	ATTACK,
	/** Move to target if too far, then cast a spell. */
	CAST,
	/** Fake death. */
	FAKE_DEATH,
	/** Check target's movement and follow it. */
	FOLLOW,
	/** Stop all actions and do nothing. In case of Npc, disconnect AI if no players around. */
	IDLE,
	/** Move to target if too far, then interact. */
	INTERACT,
	/** Move to another location. */
	MOVE_TO,
	/** Move to target if too far, then pick up the item. */
	PICK_UP,
	/** Rest (sit until attacked). */
	SIT,
	/** Stand Up. */
	STAND,
	/** Use an Item. */
	USE_ITEM;
}