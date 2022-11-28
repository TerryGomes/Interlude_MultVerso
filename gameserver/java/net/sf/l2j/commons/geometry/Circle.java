package net.sf.l2j.commons.geometry;

import java.awt.Color;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

public class Circle extends AShape
{
	protected static final int STEP = 50;

	// circle center coordinates
	protected final int _x;
	protected final int _y;

	// circle radius
	protected final int _r;

	/**
	 * Circle constructor
	 * @param x : Center X coordinate.
	 * @param y : Center Y coordinate.
	 * @param r : Circle radius.
	 */
	public Circle(int x, int y, int r)
	{
		_x = x;
		_y = y;

		_r = r;

		_center = new Point2D(_x, _y);
	}

	@Override
	public final long getSize()
	{
		return (long) Math.PI * _r * _r;
	}

	@Override
	public double getArea()
	{
		return (int) Math.PI * _r * _r;
	}

	@Override
	public double getVolume()
	{
		return 0;
	}

	@Override
	public final boolean isInside(int x, int y)
	{
		final int dx = x - _x;
		final int dy = y - _y;

		return (dx * dx + dy * dy) <= _r * _r;
	}

	@Override
	public boolean isInside(int x, int y, int z)
	{
		final int dx = x - _x;
		final int dy = y - _y;

		return (dx * dx + dy * dy) <= _r * _r;
	}

	@Override
	public Location getRandomLocation()
	{
		// get uniform distance and angle
		final double distance = Math.sqrt(Rnd.nextDouble()) * _r;
		final double angle = Rnd.nextDouble() * Math.PI * 2;

		// calculate coordinates and return
		return new Location((int) (distance * Math.cos(angle)), (int) (distance * Math.sin(angle)), 0);
	}

	@Override
	public void visualize(String info, ExServerPrimitive debug, int z)
	{
		final int count = (int) (2 * Math.PI * _r / STEP);
		final double angle = 2 * Math.PI / count;
		z -= 32;

		int dX = _r;
		int dY = 0;

		for (int i = 1; i <= count; i++)
		{
			int nextX = (int) (Math.cos(angle * i) * _r);
			int nextY = (int) (Math.sin(angle * i) * _r);

			debug.addLine(info, Color.YELLOW, true, _x + dX, _y + dY, z, _x + nextX, _y + nextY, z);

			dX = nextX;
			dY = nextY;
		}
	}
}