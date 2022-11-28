package net.sf.l2j.gameserver.data;

import java.util.concurrent.ThreadLocalRandom;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.DropType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;

public final class DropCalc
{
	public static final int SEED = 0x7FFF;

	public boolean dice(Player player, Monster monster, double chance)
	{
		return calcLevelPenalty(player, monster) >= (prob() % 100) && dice(chance);
	}

	public boolean dice(double chance)
	{
		return rand(0.0f, 100.0f) <= chance;
	}

	public double calcDropChance(Player player, Npc npc, double chance, DropType dropType, boolean isRaid, boolean isGrand)
	{
		return chance * dropType.getDropRate(player, npc, isRaid, isGrand);
	}

	public double calcDropChance(Player player, Npc npc, DropData drop, DropType dropType, boolean isRaid, boolean isGrand)
	{
		return calcDropChance(player, npc, drop.getChance(), dropType, isRaid, isGrand);
	}

	public double calcDropChance(Player player, Npc npc, DropCategory category, DropType dropType, boolean isRaid, boolean isGrand)
	{
		return calcDropChance(player, npc, category.getChance(), dropType, isRaid, isGrand);
	}

	public int calcItemDropMin(Player player, Npc npc, DropData drop, DropType dropType, boolean isRaid, boolean isGrand)
	{
		return (int) (drop.getMinDrop() + Math.max((calcDropChance(player, npc, drop.getChance(), dropType, isRaid, isGrand) - 100) / 100, 0.0));
	}

	public int calcItemDropMax(Player player, Npc npc, DropData drop, DropType dropType, boolean isRaid, boolean isGrand)
	{
		return (int) (drop.getMaxDrop() + Math.max((calcDropChance(player, npc, drop.getChance(), dropType, isRaid, isGrand) - 100) / 100, 0.0));
	}

	public int calcItemDropCount(Player player, Monster monster, double categoryChance, DropData drop, DropType dropType, boolean isRaid, boolean isGrand)
	{
		int itemCount = (int) rand(drop.getMinDrop(), drop.getMaxDrop(), 0.5);
		if (Config.ALTERNATE_DROP_LIST)
		{
			final double overflowFactor = (calcDropChance(player, monster, drop, dropType, isRaid, isGrand) - 100) / 100;
			final double inverseCategoryChance = (100 - categoryChance) / 100;
			final double reduceFactor = Math.pow(inverseCategoryChance, 10);
			final double additionalItemCount = itemCount * overflowFactor;
			final double levelFactor = (80.0 - monster.getStatus().getLevel()) / 90;
			itemCount = (int) (itemCount + additionalItemCount - additionalItemCount * reduceFactor);

			if (dropType != DropType.CURRENCY)
			{
				itemCount = (int) Math.max(drop.getMinDrop(), (itemCount - itemCount * levelFactor));
			}
		}

		return itemCount;
	}

	static float calcLevelPenalty(Player player, Monster monster)
	{
		final int diff = monster.getStatus().getLevel() - player.getStatus().getLevel();
		if (diff < -5)
		{
			if (diff < -10)
			{
				return 10.0f;
			}
			else
			{
				return diff * 18.0f + 190.0f;
			}
		}
		else
		{
			return 100.0f;
		}
	}

	static double rand(double min, double max)
	{
		return prob() / (double) SEED * (max - min) + min;
	}

	static double rand(double min, double max, double fact)
	{
		return prob() / (double) SEED * (max - min) + min + fact;
	}

	static int prob()
	{
		return ThreadLocalRandom.current().nextInt() & SEED;
	}

	public static final DropCalc getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final DropCalc INSTANCE = new DropCalc();
	}
}