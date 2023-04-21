package net.sf.l2j.commons.cached;

public class CachedDataValueShort extends CachedDataValue
{
	private volatile short value;

	CachedDataValueShort(String name, short defaultValue, int charId)
	{
		super(name, Short.toString(defaultValue), charId);
		value = defaultValue;
	}

	@Override
	void load()
	{
		super.load();
		value = Short.parseShort(getValue());
	}

	public short get()
	{
		return value;
	}

	public void set(short value)
	{
		this.value = value;
		setValue(Short.toString(value));
	}
}