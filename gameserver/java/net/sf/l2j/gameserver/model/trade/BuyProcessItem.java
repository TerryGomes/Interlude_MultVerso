package net.sf.l2j.gameserver.model.trade;

public class BuyProcessItem
{
	private final int _itemId;
	private final int _count;
	private final int _price;
	private final int _enchant;
	
	public BuyProcessItem(int itemId, int count, int price, int enchant)
	{
		_itemId = itemId;
		_count = count;
		_price = price;
		_enchant = enchant;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public long getCost()
	{
		return _count * _price;
	}
	
	public boolean addToTradeList(TradeList list)
	{
		return list.addItemByItemId(_itemId, _count, _price, _enchant) != null;
	}
}