package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.skills.L2Skill;

public class DewdropOfDestruction implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		final int itemId = item.getItemId();
		if (!(playable instanceof Player))
		{
			return;
		}

		final Player player = (Player) playable;
		final WorldObject target = player.getTarget();
		if (!(target instanceof Monster))
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final Monster evilSpirit = (Monster) target;
		if (!player.isIn3DRadius(evilSpirit, 500))
		{
			player.sendMessage("Target inaccessible, perhaps too far away");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if ((itemId == 8556) && (evilSpirit.getNpcId() == 29048 || evilSpirit.getNpcId() == 29049))
		{
			final IntIntHolder[] skills = item.getEtcItem().getSkills();
			if (skills == null)
			{
				LOGGER.warn("{} doesn't have any registered skill for handler.", item.getName());
				return;
			}

			for (final IntIntHolder skillInfo : skills)
			{
				if (skillInfo == null)
				{
					continue;
				}

				final L2Skill itemSkill = skillInfo.getSkill();
				if (itemSkill == null)
				{
					continue;
				}

				// No message on retail, the use is just forgotten.
				if (!itemSkill.checkCondition(playable, ((Creature) target), false) || playable.isSkillDisabled(itemSkill))
				{
					return;
				}

				playable.getAI().tryToCast(((Creature) target), itemSkill, forceUse, false, (item.isEtcItem() ? item.getObjectId() : 0));
			}
		}
	}
}