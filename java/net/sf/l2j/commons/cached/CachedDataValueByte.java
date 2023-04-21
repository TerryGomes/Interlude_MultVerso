package net.sf.l2j.commons.cached;

public class CachedDataValueByte extends CachedDataValue
{
	private volatile byte value;

	CachedDataValueByte(String name, byte defaultValue, int charId)
	{
		super(name, Byte.toString(defaultValue), charId);
		value = defaultValue;
	}

	@Override
	void load()
	{
		super.load();
		value = Byte.parseByte(getValue());
	}

	public byte get()
	{
		return value;
	}

	public void set(byte value)
	{
		this.value = value;
		setValue(Byte.toString(value));
	}
}