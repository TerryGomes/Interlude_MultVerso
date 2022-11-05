package net.sf.l2j.gameserver.model.craft;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A datacontainer used by private workshop system. It retains a List of {@link ManufactureItem}s, the store name and the shop state.
 */
public class ManufactureList extends ArrayList<ManufactureItem>
{
	private static final long serialVersionUID = 1L;
	
	private boolean _confirmed;
	private boolean _isDwarven;
	
	private String _storeName;
	
	public ManufactureList()
	{
	}
	
	public void set(ManufactureItem[] items)
	{
		addAll(Arrays.asList(items));
	}
	
	public boolean hasConfirmed()
	{
		return _confirmed;
	}
	
	public void setConfirmedTrade(boolean confirmed)
	{
		_confirmed = confirmed;
	}
	
	public boolean isDwarven()
	{
		return _isDwarven;
	}
	
	public void setState(boolean isDwarven)
	{
		_isDwarven = isDwarven;
	}
	
	public String getStoreName()
	{
		return _storeName;
	}
	
	public void setStoreName(String storeName)
	{
		_storeName = storeName;
	}
}