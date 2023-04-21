package net.sf.l2j.commons.cached;

public class CachedDataValueString extends CachedDataValue
{
	CachedDataValueString(String name, String defaultValue, int charId)
	{
		super(name, defaultValue, charId);
	}

	public String get()
	{
		return getValue();
	}

	public void set(String value)
	{
		setValue(value);
	}
}