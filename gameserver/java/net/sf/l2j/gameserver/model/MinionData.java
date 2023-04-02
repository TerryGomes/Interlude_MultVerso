package net.sf.l2j.gameserver.model;

import net.sf.l2j.commons.random.Rnd;

/**
 * This class defines the spawn data of a Minion type.<BR>
 * In a NPC group, there are one master and several minions (named leader and privates on L2OFF).
 */
public class MinionData
{
	private final int _id;
	private final int _minAmount;
	private final int _maxAmount;

	public MinionData(int id, int minAmount, int maxAmount)
	{
		_id = id;
		_minAmount = minAmount;
		_maxAmount = maxAmount;
	}

	public int getId()
	{
		return _id;
	}

	public int getAmount()
	{
		return (_maxAmount > _minAmount) ? Rnd.get(_minAmount, _maxAmount) : _minAmount;
	}
}