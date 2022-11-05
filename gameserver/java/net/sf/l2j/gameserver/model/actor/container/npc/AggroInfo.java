package net.sf.l2j.gameserver.model.actor.container.npc;

import net.sf.l2j.gameserver.model.actor.Creature;

/**
 * This class contains all aggro informations (damage and hate) against a {@link Creature}.<br>
 * <br>
 * Values are limited to 999999999.
 */
public final class AggroInfo
{
	private final Creature _attacker;
	
	private int _damage;
	private int _hate;
	
	public AggroInfo(Creature attacker)
	{
		_attacker = attacker;
	}
	
	@Override
	public final boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (obj instanceof AggroInfo)
			return (((AggroInfo) obj).getAttacker() == _attacker);
		
		return false;
	}
	
	@Override
	public final int hashCode()
	{
		return _attacker.getObjectId();
	}
	
	@Override
	public String toString()
	{
		return "AggroInfo [attacker=" + _attacker + ", damage=" + _damage + ", hate=" + _hate + "]";
	}
	
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	public int getDamage()
	{
		return _damage;
	}
	
	public void addDamage(int value)
	{
		_damage = (int) Math.min(_damage + (long) value, 999999999);
	}
	
	public int getHate()
	{
		return _hate;
	}
	
	public void addHate(int value)
	{
		_hate = (int) Math.min(_hate + (long) value, 999999999);
	}
	
	public void stopHate()
	{
		_hate = 0;
	}
}