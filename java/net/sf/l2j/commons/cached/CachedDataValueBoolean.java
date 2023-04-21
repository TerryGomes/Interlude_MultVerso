package net.sf.l2j.commons.cached;

public class CachedDataValueBoolean extends CachedDataValue
{
	private volatile boolean value;

	CachedDataValueBoolean(String name, boolean defaultValue, int charId)
	{
		super(name, Boolean.toString(defaultValue), charId);
		value = defaultValue;
	}

	@Override
	void load()
	{
		super.load();
		value = Boolean.parseBoolean(getValue());
	}

	public boolean get()
	{
		return value;
	}

	public void set(boolean value)
	{
		this.value = value;
		setValue(Boolean.toString(value));
	}
}