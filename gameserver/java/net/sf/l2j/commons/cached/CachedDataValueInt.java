package net.sf.l2j.commons.cached;

public class CachedDataValueInt extends CachedDataValue
{
	private volatile int value;

	CachedDataValueInt(String name, int defaultValue, int charId)
	{
		super(name, Integer.toString(defaultValue), charId);
		value = defaultValue;
	}

	@Override
	void load()
	{
		super.load();
		value = Integer.parseInt(getValue());
	}

	public int get()
	{
		return value;
	}

	public void set(int value)
	{
		this.value = value;
		setValue(Integer.toString(value));
	}
}