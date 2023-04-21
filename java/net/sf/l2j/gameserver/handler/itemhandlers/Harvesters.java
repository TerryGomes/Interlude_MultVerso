package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class Harvesters implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!Config.ALLOW_MANOR || !(playable instanceof Player))
		{
			return;
		}

		final WorldObject target = playable.getTarget();
		if (!(target instanceof Creature))
		{
			playable.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}

		final Creature creature = (Creature) target;
		if (!creature.isDead())
		{
			playable.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}

		if (!playable.getInventory().validateCapacity(1))
		{
			playable.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		playable.getAI().tryToCast(creature, FrequentSkill.HARVEST.getSkill());
	}
}