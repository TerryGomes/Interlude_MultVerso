package net.sf.l2j.commons.geometry;

import java.awt.Color;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * Tri-sided polygon in 3D space, while having bottom and top area flat (in Z coordinate).<br>
 * It is <b>not</b> 3D oriented triangle.
 */
public class Triangle3D extends Triangle
{
	// min and max Z coorinates
	private final int _minZ;
	private final int _maxZ;

	// total length of all sides
	private final double _length;

	/**
	 * Triangle constructor.
	 * @param A : Point A of the triangle.
	 * @param B : Point B of the triangle.
	 * @param C : Point C of the triangle.
	 */
	public Triangle3D(Location A, Location B, Location C)
	{
		super(A, B, C);

		_minZ = Math.min(A.getZ(), Math.min(B.getZ(), C.getZ()));
		_maxZ = Math.max(A.getZ(), Math.max(B.getZ(), C.getZ()));

		final int CBx = _CAx - _BAx;
		final int CBy = _CAy - _BAy;
		_length = Math.sqrt(_BAx * _BAx + _BAy * _BAy) + Math.sqrt(_CAx * _CAx + _CAy * _CAy) + Math.sqrt(CBx * CBx + CBy * CBy);
	}

	@Override
	public double getArea()
	{
		return _size * 2 + _length * (_maxZ - _minZ);
	}

	@Override
	public double getVolume()
	{
		return _size * (_maxZ - _minZ);
	}

	@Override
	public final boolean isInside(int x, int y, int z)
	{
		if (z < _minZ || z > _maxZ)
		{
			return false;
		}

		return super.isInside(x, y, z);
	}

	@Override
	public final Location getRandomLocation()
	{
		// get relative length of AB and AC vectors
		double ba = Rnd.nextDouble();
		double ca = Rnd.nextDouble();

		// adjust length if too long
		if (ba + ca > 1)
		{
			ba = 1 - ba;
			ca = 1 - ca;
		}

		// calc coords (take A, add AB and AC)
		final int x = _Ax + (int) (ba * _BAx + ca * _CAx);
		final int y = _Ay + (int) (ba * _BAy + ca * _CAy);

		// return
		return new Location(x, y, Rnd.get(_minZ, _maxZ));
	}

	@Override
	public void visualize(String info, ExServerPrimitive debug, int z)
	{
		final int x2 = _Ax + _BAx;
		final int y2 = _Ay + _BAy;
		final int x3 = _Ax + _CAx;
		final int y3 = _Ay + _CAy;
		final int z1 = _minZ - 32;
		final int z2 = _maxZ - 32;

		debug.addLine(info + " MinZ", Color.GREEN, true, _Ax, _Ay, z1, x2, y2, z1);
		debug.addLine(info, Color.YELLOW, true, _Ax, _Ay, z, x2, y2, z);
		debug.addLine(info + " MaxZ", Color.RED, true, _Ax, _Ay, z2, x2, y2, z2);

		debug.addLine(info + " MinZ", Color.GREEN, true, _Ax, _Ay, z1, x3, y3, z1);
		debug.addLine(info, Color.YELLOW, true, _Ax, _Ay, z, x3, y3, z);
		debug.addLine(info + " MaxZ", Color.RED, true, _Ax, _Ay, z2, x3, y3, z2);

		debug.addLine(info + " MinZ", Color.GREEN, true, x2, y2, z1, x3, y3, z1);
		debug.addLine(info, Color.YELLOW, true, x2, y2, z, x3, y3, z);
		debug.addLine(info + " MaxZ", Color.RED, true, x2, y2, z2, x3, y3, z2);

		debug.addLine(info, Color.YELLOW, true, _Ax, _Ay, z1, _Ax, _Ay, z2);
		debug.addLine(info, Color.YELLOW, true, x2, y2, z1, x2, y2, z2);
		debug.addLine(info, Color.YELLOW, true, x3, y3, z1, x3, y3, z2);
	}
}