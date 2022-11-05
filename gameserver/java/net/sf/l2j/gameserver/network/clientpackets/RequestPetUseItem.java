package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestPetUseItem extends L2GameClientPacket
{
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null || !player.hasPet())
			return;
		
		final Pet pet = (Pet) player.getSummon();
		
		final ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;
		
		if (player.isAlikeDead() || pet.isDead())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(item));
			return;
		}
		
		if (!item.isEquipped() && !item.getItem().checkCondition(pet, pet, true))
			return;
		
		// Check if item is pet armor or pet weapon
		if (item.isPetItem())
		{
			// Verify if the pet can wear that item
			if (!pet.canWear(item.getItem()))
			{
				player.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
				return;
			}
			
			if (item.isEquipped())
			{
				pet.getInventory().unequipItemInSlot(item.getLocationSlot());
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_OFF_S1).addItemName(item));
			}
			else
			{
				pet.getInventory().equipPetItem(item);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_PUT_ON_S1).addItemName(item));
			}
			
			player.sendPacket(new PetItemList(pet));
			pet.updateAndBroadcastStatus(1);
			return;
		}
		
		final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
		if (handler == null || (!pet.getTemplate().canEatFood(item.getItemId()) && !item.isPotion()))
		{
			player.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
			return;
		}
		
		handler.useItem(pet, item, false);
		pet.updateAndBroadcastStatus(1);
	}
}