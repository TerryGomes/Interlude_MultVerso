package net.sf.l2j.gameserver.model;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;

public class HealSps extends IntIntHolder
{
	private final int _magicLevel;
	private final int _correction;
	private final int _neededMatk;

	public HealSps(StatSet set)
	{
		super(set.getInteger("skillId", 0), set.getInteger("skillLevel", 0));

		_magicLevel = set.getInteger("magicLevel", 0);
		_correction = set.getInteger("correction");
		_neededMatk = set.getInteger("neededMatk");
	}

	@Override
	public String toString()
	{
		return "HealSps [skillId=" + getId() + ", skillLevel=" + getValue() + ", magicLevel=" + _magicLevel + ", correction=" + _correction + ", neededMatk=" + _neededMatk + "]";
	}

	public int getMagicLevel()
	{
		return _magicLevel;
	}

	public int getCorrection()
	{
		return _correction;
	}

	public int getNeededMatk()
	{
		return _neededMatk;
	}
}