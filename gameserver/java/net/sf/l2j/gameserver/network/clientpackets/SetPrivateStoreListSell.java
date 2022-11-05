package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.OperateType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.trade.SellProcessItem;
import net.sf.l2j.gameserver.model.trade.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgSell;

public final class SetPrivateStoreListSell extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 12;
	
	private boolean _packageSale;
	private SellProcessItem[] _items = null;
	
	@Override
	protected void readImpl()
	{
		_packageSale = (readD() == 1);
		
		final int count = readD();
		if (count < 1 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new SellProcessItem[count];
		
		for (int i = 0; i < count; i++)
		{
			final int objectId = readD();
			final int cnt = readD();
			final int price = readD();
			
			_items[i] = new SellProcessItem(objectId, cnt, price);
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		// Retrieve and clear the selllist.
		final TradeList tradeList = player.getSellList();
		tradeList.clear();
		
		// Integrity check.
		if (ArraysUtil.isEmpty(_items))
		{
			player.setOperateType(OperateType.NONE);
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
			return;
		}
		
		// Integrity check.
		if (!player.getInventory().canPassSellProcess(_items))
		{
			player.setOperateType(OperateType.NONE);
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.setOperateType(OperateType.NONE);
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		// Check multiple conditions. Message and OperateType reset are sent directly from the method.
		if (!player.canOpenPrivateStore(false))
			return;
		
		// Check maximum number of allowed slots.
		if (_items.length > player.getStatus().getPrivateSellStoreLimit())
		{
			player.setOperateType(OperateType.NONE);
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
			return;
		}
		
		tradeList.setPackaged(_packageSale);
		
		long totalCost = player.getAdena();
		
		for (SellProcessItem i : _items)
		{
			if (!i.addToTradeList(tradeList))
			{
				player.sendPacket(SystemMessageId.EXCEEDED_THE_MAXIMUM);
				player.sendPacket(new PrivateStoreManageListSell(player, _packageSale));
				return;
			}
			
			totalCost += i.getPrice();
			if (totalCost > Integer.MAX_VALUE)
			{
				player.sendPacket(SystemMessageId.EXCEEDED_THE_MAXIMUM);
				player.sendPacket(new PrivateStoreManageListSell(player, _packageSale));
				return;
			}
		}
		
		player.getMove().stop();
		player.sitDown();
		player.setOperateType((_packageSale) ? OperateType.PACKAGE_SELL : OperateType.SELL);
		player.broadcastUserInfo();
		player.broadcastPacket(new PrivateStoreMsgSell(player));
	}
}