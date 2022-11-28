package net.sf.l2j.commons.cached;

import java.util.ArrayList;
import java.util.List;

public class CachedData
{
	private final int charId;

	private final List<CachedDataValue> cachedDataValueList = new ArrayList<>();

	public CachedData(int charId)
	{
		this.charId = charId;
	}

	public void load()
	{
		for (var value : cachedDataValueList)
		{
			value.load();
		}
	}

	public void store()
	{
		for (var value : cachedDataValueList)
		{
			value.update();
		}
	}

	public static final boolean BOOLEAN_DEFAULT = true;
	public static final byte BYTE_DEFAULT = (byte) 0;
	public static final short SHORT_DEFAULT = (short) 0;
	public static final int INT_DEFAULT = 0;
	public static final long LONG_DEFAULT = 0;
	public static final float FLOAT_DEFAULT = 0.0f;
	public static final double DOUBLE_DEFAULT = 0.0d;
	public static final String STRING_DEFAULT = "";

	public CachedDataValueBoolean newBoolean(String name)
	{
		return newBoolean(name, BOOLEAN_DEFAULT);
	}

	public CachedDataValueBoolean newBoolean(String name, boolean defaultValue)
	{
		var value = new CachedDataValueBoolean(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}

	public CachedDataValueByte newByte(String name)
	{
		return newByte(name, BYTE_DEFAULT);
	}

	public CachedDataValueByte newByte(String name, byte defaultValue)
	{
		var value = new CachedDataValueByte(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}

	public CachedDataValueShort newShort(String name)
	{
		return newShort(name, SHORT_DEFAULT);
	}

	public CachedDataValueShort newShort(String name, short defaultValue)
	{
		var value = new CachedDataValueShort(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}

	public CachedDataValueInt newInt(String name)
	{
		return newInt(name, INT_DEFAULT);
	}

	public CachedDataValueInt newInt(String name, int defaultValue)
	{
		var value = new CachedDataValueInt(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}

	public CachedDataValueLong newLong(String name)
	{
		return newLong(name, LONG_DEFAULT);
	}

	public CachedDataValueLong newLong(String name, long defaultValue)
	{
		var value = new CachedDataValueLong(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}

	public CachedDataValueFloat newFloat(String name)
	{
		return newFloat(name, FLOAT_DEFAULT);
	}

	public CachedDataValueFloat newFloat(String name, float defaultValue)
	{
		var value = new CachedDataValueFloat(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}

	public CachedDataValueDouble newDouble(String name)
	{
		return newDouble(name, DOUBLE_DEFAULT);
	}

	public CachedDataValueDouble newDouble(String name, double defaultValue)
	{
		var value = new CachedDataValueDouble(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}

	public CachedDataValueString newString(String name)
	{
		return newString(name, STRING_DEFAULT);
	}

	public CachedDataValueString newString(String name, String defaultValue)
	{
		var value = new CachedDataValueString(name, defaultValue, charId);
		cachedDataValueList.add(value);
		return value;
	}
}