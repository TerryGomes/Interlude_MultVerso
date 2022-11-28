package net.sf.l2j.gameserver.model.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.DropCalc;
import net.sf.l2j.gameserver.enums.DropType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

public class DropCategory
{
	private final DropType _dropType;
	private final double _chance;
	private final List<DropData> _drops;

	private double _cumulativeChance;

	public DropCategory(DropType dropType, double chance)
	{
		_dropType = dropType;
		_chance = chance;
		_drops = new ArrayList<>(0);

		_cumulativeChance = 0;
	}

	/**
	 * Adds {@link DropData} to this {@link DropCategory}.
	 * @param drop
	 */
	public void addDropData(DropData drop)
	{
		_drops.add(drop);

		_cumulativeChance += drop.getChance();
	}

	/**
	 * @return The {@link DropType} of this {@link DropCategory}.
	 */
	public DropType getDropType()
	{
		return _dropType;
	}

	/**
	 * @return The {@link DropCategory} chance.
	 */
	public double getChance()
	{
		return _chance;
	}

	/**
	 * @return The list of all {@link DropData}, which belongs to this {@link DropCategory}.
	 */
	public List<DropData> getAllDrops()
	{
		return _drops;
	}

	public double getCategoryCumulativeChance()
	{
		return (_dropType == DropType.SPOIL) ? 100. : _cumulativeChance;
	}

	static final ThreadLocal<DropData[]> threadLocalDropArray = ThreadLocal.withInitial(() -> new DropData[100]);

	public DropData[] shuffleDropList()
	{
		DropData[] arr = threadLocalDropArray.get();
		Arrays.fill(arr, null);
		for (int i = 0; i < _drops.size(); i++)
		{
			arr[i] = _drops.get(i);
		}

		Collections.shuffle(Arrays.asList(arr));
		trim(arr);
		return arr;
	}

	public static void trim(Object[] arr)
	{
		for (int i = 0; i < arr.length; i++)
		{
			if (arr[i] == null)
			{
				for (int n = i + 1; n < arr.length; n++)
				{
					if (arr[n] != null)
					{
						arr[i] = arr[n];
						arr[n] = null;
						break;
					}
				}
			}
		}
	}

	public List<IntIntHolder> calcDropList(Player player, Monster monster, List<IntIntHolder> out, boolean raid)
	{
		final var list = shuffleDropList();
		if (DropCalc.getInstance().dice(player, monster, DropCalc.getInstance().calcDropChance(player, monster, this, getDropType(), raid, monster instanceof GrandBoss)))
		{
			if (Config.ALTERNATE_DROP_LIST)
			{
				for (var i = 0; i < _drops.size(); i++)
				{
					final DropData item = list[i];
					if (item != null)
					{
						if (calcDropItem(item, player, monster, out, raid))
						{
							break;
						}
					}
				}
			}
			else
			{
				for (final DropData item : _drops)
				{
					if (calcDropItem(item, player, monster, out, raid))
					{
						break;
					}
				}
			}
		}

		return out;
	}

	public boolean calcDropItem(DropData item, Player player, Monster monster, List<IntIntHolder> out, boolean raid)
	{
		double itemChance = DropCalc.getInstance().calcDropChance(player, monster, item, getDropType(), raid, monster instanceof GrandBoss);
		int[] itemCount =
		{
			0
		};

		if (DropCalc.getInstance().dice(itemChance))
		{
			itemCount[0] = DropCalc.getInstance().calcItemDropCount(player, monster, getChance(), item, getDropType(), raid, monster instanceof GrandBoss);
		}

		if (itemCount[0] > 0)
		{
			Optional<IntIntHolder> holder = out.stream().filter(h -> h.getId() == item.getItemId()).findAny();
			if (holder.isEmpty())
			{
				out.add(new IntIntHolder(item.getItemId(), itemCount[0]));
			}
			else
			{
				holder.ifPresent(h -> h.setValue(h.getValue() + itemCount[0]));
			}
			return true;
		}

		return false;
	}

	/**
	 * Calculates drops of this {@link DropCategory}.
	 * @param player
	 * @param monster
	 * @param levelMultiplier : The input level modifier of the last attacker.
	 * @param raid : The NPC is raid boss.
	 * @return The list of {@link IntIntHolder} holding item ID and item count.
	 */
	public List<IntIntHolder> calculateDrop(Player player, Monster monster, double levelMultiplier, boolean raid)
	{
		// Get base category chance and apply level multiplier and drop rate config based on type.
		double chance = getChance() * levelMultiplier * getDropType().getDropRate(player, monster, raid, monster instanceof GrandBoss) * DropData.MAX_CHANCE / 100;

		// Check chance exceeding 100% limit and calculate drop chance multiplier.
		double multiplier;

		// Chance is not over 100% (inclusive).
		if (chance <= DropData.MAX_CHANCE)
		{
			// Check category success, set drop chance multiplier.
			if (Rnd.get(DropData.MAX_CHANCE) < chance)
			{
				multiplier = 1;
			}
			else
			{
				return Collections.emptyList();
			}
		}
		// Chance is over 100%. Category automatically succeed, calculate drop chance multiplier.
		else
		{
			multiplier = chance / DropData.MAX_CHANCE;
		}

		// Category success, calculate chance for individual drop and go through drops.
		final List<IntIntHolder> result = new ArrayList<>(1);

		// Evaluate all drops if the drop type is SPOIL - each drop has a chance to be dropped.
		if (_dropType == DropType.SPOIL)
		{
			for (DropData dd : getAllDrops())
			{
				// Calculate drop chance and apply drop chance multiplier.
				chance = dd.getChance() * multiplier * DropData.MAX_CHANCE / 100;

				// Chance is not over 100% (inclusive).
				if (chance <= DropData.MAX_CHANCE)
				{
					// Calculate drop success, calculate drop using fixed amount multiplier.
					if (Rnd.get(DropData.MAX_CHANCE) < chance)
					{
						result.add(dd.calculateDrop(1));
					}
				}
				// Chance is over 100%. Drop automatically succeed, calculate drop using calculated amount multiplier.
				else
				{
					result.add(dd.calculateDrop(chance / DropData.MAX_CHANCE));
				}
			}
		}
		// Evaluate all drops, pick one drop to be dropped.
		else
		{
			// Calculate category cumulative chance and apply drop chance multiplier.
			chance = getCategoryCumulativeChance() * multiplier * DropData.MAX_CHANCE / 100;

			// Chance is not over 100% (inclusive).
			if (chance <= DropData.MAX_CHANCE)
			{
				// Get drop chance and loop for drop.
				chance = Rnd.get(DropData.MAX_CHANCE);
				for (DropData dd : getAllDrops())
				{
					// Check drop chance and evaluate.
					chance -= dd.getChance() * multiplier * DropData.MAX_CHANCE / 100;
					if (chance < 0)
					{
						result.add(dd.calculateDrop(1));
						break;
					}
				}
			}
			// Chance is over 100%. Calculate drop chance multiplier and drop amount multiplier.
			else
			{
				double amount = multiplier;
				multiplier = 100 / getCategoryCumulativeChance();
				amount /= multiplier;

				// Get drop chance and loop for drop.
				chance = Rnd.get(DropData.MAX_CHANCE);
				for (DropData dd : getAllDrops())
				{
					// Check drop chance and evaluate.
					chance -= dd.getChance() * multiplier * DropData.MAX_CHANCE / 100;
					if (chance < 0)
					{
						result.add(dd.calculateDrop(amount));
						break;
					}
				}
			}
		}

		return result;
	}
}