package net.sf.l2j.gameserver.model.trade;

public class SellProcessItem
{
	private final int _objectId;
	private final int _count;
	private final int _price;
	
	public SellProcessItem(int objectId, int count, int price)
	{
		_objectId = objectId;
		_count = count;
		_price = price;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public long getPrice()
	{
		return _count * _price;
	}
	
	public boolean addToTradeList(TradeList list)
	{
		return list.addItem(_objectId, _count, _price) != null;
	}
}