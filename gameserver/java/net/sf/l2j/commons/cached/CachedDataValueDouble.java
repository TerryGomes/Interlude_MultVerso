package net.sf.l2j.commons.cached;

public class CachedDataValueDouble extends CachedDataValue
{
	private volatile double value;

	CachedDataValueDouble(String name, double defaultValue, int charId)
	{
		super(name, Double.toString(defaultValue), charId);
		value = defaultValue;
	}

	@Override
	void load()
	{
		super.load();
		value = Double.parseDouble(getValue());
	}

	public double get()
	{
		return value;
	}

	public void set(double value)
	{
		this.value = value;
		setValue(Double.toString(value));
	}
}