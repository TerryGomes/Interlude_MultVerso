package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * A container used by monster drops.
 */
public class DropData
{
	public static final int MAX_CHANCE = 1000000;

	private final int _itemId;
	private final int _minDrop;
	private final int _maxDrop;
	private final double _chance;

	public DropData(int itemId, int minDrop, int maxDrop, double chance)
	{
		_itemId = itemId;
		_minDrop = minDrop;
		_maxDrop = maxDrop;
		_chance = chance;
	}

	@Override
	public String toString()
	{
		return "DropData =[ItemID: " + _itemId + " Min: " + _minDrop + " Max: " + _maxDrop + " Chance: " + _chance + "%]";
	}

	/**
	 * @return the id of the dropped item.
	 */
	public int getItemId()
	{
		return _itemId;
	}

	/**
	 * @return the minimum quantity of dropped items.
	 */
	public int getMinDrop()
	{
		return _minDrop;
	}

	/**
	 * @return the maximum quantity of dropped items.
	 */
	public int getMaxDrop()
	{
		return _maxDrop;
	}

	/**
	 * @return the chance to have a drop.
	 */
	public double getChance()
	{
		return _chance;
	}

	/**
	 * @param ratio : The given drop amount ratio (e.g. 2.3 means, there will be 130% increased amount of this particular drop).
	 * @return The {@link IntIntHolder} containing item ID and item count.
	 */
	public IntIntHolder calculateDrop(double ratio)
	{
		int count;
		if (ratio <= 1)
		{
			// Ratio is below 100% including.

			// Calculate count over given min-max.
			count = Rnd.get(_minDrop, _maxDrop);
		}
		else
		{
			// Ratio is above 100%.

			// Calculate amount multiplier and amount bonus using div-mod method.
			ratio *= 100;
			int multiplier = (int) (ratio / 100);
			int bonus = (int) (ratio % 100);

			// Calculate base count using multiplier.
			count = Rnd.get(_minDrop * multiplier, _maxDrop * multiplier);

			// Add bonus amount (when fraction of the ratio occurs).
			if (Rnd.get(100) < bonus)
			{
				count += Rnd.get(_minDrop, _maxDrop);
			}
		}

		return new IntIntHolder(_itemId, count);
	}
}