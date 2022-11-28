package net.sf.l2j.gameserver.enums;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;

public enum DropType
{
	SPOIL,
	CURRENCY,
	DROP,
	HERB;

	public double getDropRate(Player player, Npc npc, boolean isRaid, boolean isGrand)
	{
		switch (this)
		{
			case SPOIL:
				if (player.getPremiumService() == 1)
				{
					return Config.PREMIUM_RATE_DROP_SPOIL;
				}

				return Config.RATE_DROP_SPOIL;

			case CURRENCY:
				if (player.getPremiumService() == 1 && npc.isChampion())
				{
					return Config.PREMIUM_CHAMPION_ADENAS_REWARDS;
				}
				else if (npc.isChampion())
				{
					return Config.CHAMPION_ADENAS_REWARDS;
				}

				if (player.getPremiumService() == 1)
				{
					return Config.PREMIUM_RATE_DROP_CURRENCY;
				}

				return Config.RATE_DROP_CURRENCY;

			case DROP:
				if (player.getPremiumService() == 1 && npc.isChampion())
				{
					return Config.PREMIUM_CHAMPION_REWARDS;
				}
				else if (npc.isChampion())
				{
					return Config.CHAMPION_REWARDS;
				}

				if (player.getPremiumService() == 1)
				{
					if (isGrand)
					{
						return Config.PREMIUM_RATE_DROP_ITEMS_BY_GRAND;
					}

					if (isRaid)
					{
						return Config.PREMIUM_RATE_DROP_ITEMS_BY_RAID;
					}

					return Config.PREMIUM_RATE_DROP_ITEMS;
				}
				else
				{
					if (isGrand)
					{
						return Config.RATE_DROP_ITEMS_BY_GRAND;
					}

					if (isRaid)
					{
						return Config.RATE_DROP_ITEMS_BY_RAID;
					}

					return Config.RATE_DROP_ITEMS;
				}

			case HERB:
				return Config.RATE_DROP_HERBS;

			default:
				return 0;
		}
	}
}