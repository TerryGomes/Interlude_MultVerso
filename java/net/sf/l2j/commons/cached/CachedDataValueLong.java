package net.sf.l2j.commons.cached;

public class CachedDataValueLong extends CachedDataValue
{
	private volatile long value;

	CachedDataValueLong(String name, long defaultValue, int charId)
	{
		super(name, Long.toString(defaultValue), charId);
		value = defaultValue;
	}

	@Override
	void load()
	{
		super.load();
		value = Long.parseLong(getValue());
	}

	public long get()
	{
		return value;
	}

	public void set(long value)
	{
		this.value = value;
		setValue(Long.toString(value));
	}
}