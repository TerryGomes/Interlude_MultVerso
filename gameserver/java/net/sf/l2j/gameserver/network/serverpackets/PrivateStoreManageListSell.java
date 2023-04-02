package net.sf.l2j.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.trade.TradeItem;

public class PrivateStoreManageListSell extends L2GameServerPacket
{
	private final int _objId;
	private final int _playerAdena;
	private final boolean _packageSale;
	private List<TradeItem> _sellList;
	private List<TradeItem> _sellList0;

	public PrivateStoreManageListSell(Player player, boolean isPackageSale)
	{
		_objId = player.getObjectId();
		_playerAdena = player.getAdena();

		player.getSellList().updateItems();

		_packageSale = (player.getSellList().isPackaged()) ? true : isPackageSale;
		_sellList0 = player.getSellList();
		_sellList = new ArrayList<>();

		for (TradeItem si : _sellList0)
		{
			if (si.getCount() <= 0)
			{
				_sellList0.remove(si);
				continue;
			}

			ItemInstance item = player.getInventory().getItemByObjectId(si.getObjectId());

			if (item == null || !item.isTradable() || item.getItemId() == 57)
			{
				_sellList0.remove(si);
				continue;
			}

			si.setCount(Math.min(item.getCount(), si.getCount()));
		}

		Set<ItemInstance> items = player.getInventory().getItems();
		loop:
		for (ItemInstance item : items)
		{
			if (item.isTradable() && item.getItemId() != 57)
			{
				for (TradeItem si : _sellList0)
				{
					if (si.getObjectId() == item.getObjectId())
					{
						if (si.getCount() == item.getCount())
						{
							continue loop;
						}

						TradeItem ti = new TradeItem(item, si.getCount(), si.getPrice());
						ti.setCount(item.getCount() - si.getCount());
						_sellList.add(ti);
						continue loop;
					}
				}
				_sellList.add(new TradeItem(item, item.getCount(), 0));
			}
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9a);
		writeD(_objId);
		writeD(_packageSale ? 1 : 0);
		writeD(_playerAdena);

		writeD(_sellList.size());
		for (TradeItem item : _sellList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0x00);
			writeH(item.getEnchant());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice());
		}

		writeD(_sellList0.size());
		for (TradeItem item : _sellList0)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0x00);
			writeH(item.getEnchant());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice());
			writeD(item.getItem().getReferencePrice());
		}
	}
}