package net.sf.l2j.commons.geometry;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;

/**
 * @author Hasha
 */
public class Polygon extends AShape
{
	private static final int TRIANGULATION_MAX_LOOPS = 100;
	
	protected final List<? extends AShape> _shapes;
	
	protected final int _size;
	
	/**
	 * Constructor of the {@link Polygon}.
	 * @param shapes : List of {@link AShape}.
	 */
	public Polygon(List<? extends AShape> shapes)
	{
		_shapes = shapes;
		
		int size = 0;
		for (AShape shape : shapes)
			size += shape.getSize();
		_size = size;
	}
	
	/**
	 * Constructor of the {@link Polygon}. Creates a polygon, which consists of triangles using Kong's algorithm.
	 * @param id : Virtual ID of the polygon, used to separate constructor types.
	 * @param points : List of {@code Point2D} points, forming a polygon.
	 */
	public Polygon(int id, List<Point2D> points)
	{
		List<Triangle> triangles = null;
		int size = 0;
		try
		{
			// not a polygon, throw exception
			if (points.size() < 3)
				throw new IndexOutOfBoundsException("Can not create Polygon (id=" + id + ") from less than 3 coordinates.");
			
			// get polygon orientation
			final boolean isCw = getPolygonOrientation(points);
			
			// calculate non convex points
			final List<Point2D> nonConvexPoints = calculateNonConvexPoints(points, isCw);
			
			// polygon triangulation of points based on orientation and non-convex points
			triangles = doTriangulationAlgorithm(points, isCw, nonConvexPoints);
			
			// calculate polygon size
			for (AShape shape : triangles)
				size += shape.getSize();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			triangles = new ArrayList<>();
		}
		
		_shapes = triangles;
		_size = size;
	}
	
	@Override
	public int getSize()
	{
		return _size;
	}
	
	@Override
	public double getArea()
	{
		// not supported yet
		return -1;
	}
	
	@Override
	public double getVolume()
	{
		// not supported yet
		return -1;
	}
	
	@Override
	public boolean isInside(int x, int y)
	{
		for (AShape shape : _shapes)
			if (shape.isInside(x, y))
				return true;
			
		return false;
	}
	
	@Override
	public boolean isInside(int x, int y, int z)
	{
		for (AShape shape : _shapes)
			if (shape.isInside(x, y, z))
				return true;
			
		return false;
	}
	
	@Override
	public Location getRandomLocation()
	{
		int size = Rnd.get(_size);
		
		for (AShape shape : _shapes)
		{
			size -= shape.getSize();
			if (size < 0)
				return shape.getRandomLocation();
		}
		
		// should never happen
		return null;
	}
	
	/**
	 * Returns clockwise (cw) or counter-clockwise (ccw) orientation of the polygon.
	 * @param points : List of all points.
	 * @return {@code boolean} : True, when the polygon is clockwise orientated.
	 */
	private static final boolean getPolygonOrientation(List<Point2D> points)
	{
		// first find point with minimum x-coord - if there are several ones take the one with maximal y-coord
		
		// get point
		final int size = points.size();
		
		int index = 0;
		Point2D point = points.get(0);
		
		for (int i = 1; i < size; i++)
		{
			Point2D pt = points.get(i);
			
			// x lower, or x same and y higher
			if ((pt.getX() < point.getX()) || pt.getX() == point.getX() && pt.getY() > point.getY())
			{
				point = pt;
				index = i;
			}
		}
		
		// get previous point
		final Point2D pointPrev = points.get(getPrevIndex(size, index));
		
		// get next point
		final Point2D pointNext = points.get(getNextIndex(size, index));
		
		// get orientation
		final int vx = point.getX() - pointPrev.getX();
		final int vy = point.getY() - pointPrev.getY();
		final int res = pointNext.getX() * vy - pointNext.getY() * vx + vx * pointPrev.getY() - vy * pointPrev.getX();
		
		// return
		return res <= 0;
	}
	
	/**
	 * Returns next index to given index of data container.
	 * @param size : Size of the data container.
	 * @param index : Index to be compared.
	 * @return {@code int} : Next index.
	 */
	private static final int getNextIndex(int size, int index)
	{
		// increase index and check for limit
		if (++index >= size)
			return 0;
		
		return index;
	}
	
	/**
	 * Returns previous index to given index of data container.
	 * @param size : Size of the data container.
	 * @param index : Index to be compared.
	 * @return {@code int} : Previous index.
	 */
	private static final int getPrevIndex(int size, int index)
	{
		// decrease index and check for limit
		if (--index < 0)
			return size - 1;
		
		return index;
	}
	
	/**
	 * This determines all concave vertices of the polygon and separate convex ones.
	 * @param points : List of all points.
	 * @param isCw : Polygon orientation (clockwise/counterclockwise).
	 * @return {@code List<Point2D>} : List of non-convex points.
	 */
	private static final List<Point2D> calculateNonConvexPoints(List<Point2D> points, boolean isCw)
	{
		// list of non convex points
		final List<Point2D> nonConvexPoints = new ArrayList<>();
		
		// result value of test function
		final int size = points.size();
		for (int i = 0; i < size - 1; i++)
		{
			// get 3 points
			final Point2D point = points.get(i);
			final Point2D pointNext = points.get(i + 1);
			final Point2D pointNextNext = points.get(getNextIndex(size, i + 2));
			
			final int vx = pointNext.getX() - point.getX();
			final int vy = pointNext.getY() - point.getY();
			
			// note: cw means res/newres is <= 0
			final boolean res = (pointNextNext.getX() * vy - pointNextNext.getY() * vx + vx * point.getY() - vy * point.getX()) > 0;
			if (res == isCw)
				nonConvexPoints.add(pointNext);
		}
		
		return nonConvexPoints;
	}
	
	/**
	 * Perform Kong's triangulation algorithm.
	 * @param points : List of all points.
	 * @param isCw : Polygon orientation (clockwise/counterclockwise).
	 * @param nonConvexPoints : List of all non-convex points.
	 * @return {@code List<Triangle>} : List of {@link Triangle}.
	 * @throws Exception : When coordinates are not aligned to form monotone polygon.
	 */
	private static final List<Triangle> doTriangulationAlgorithm(List<Point2D> points, boolean isCw, List<Point2D> nonConvexPoints) throws Exception
	{
		// create the list
		final List<Triangle> triangles = new ArrayList<>();
		
		int size = points.size();
		int loops = 0;
		int index = 1;
		while (size > 3)
		{
			// get next and previous indexes
			final int indexPrev = getPrevIndex(size, index);
			final int indexNext = getNextIndex(size, index);
			
			// get points
			final Point2D pointPrev = points.get(indexPrev);
			final Point2D point = points.get(index);
			final Point2D pointNext = points.get(indexNext);
			
			// check point to create polygon ear
			if (isEar(isCw, nonConvexPoints, pointPrev, point, pointNext))
			{
				// create triangle from polygon ear
				triangles.add(new Triangle(pointPrev, point, pointNext));
				
				// remove middle point from list, update size
				points.remove(index);
				size--;
				
				// move index
				index = getPrevIndex(size, index);
			}
			else
			{
				// move index
				index = indexNext;
			}
			
			if (++loops == TRIANGULATION_MAX_LOOPS)
				throw new Exception("Coordinates are not aligned to form monotone polygon.");
		}
		
		// add last triangle
		triangles.add(new Triangle(points.get(0), points.get(1), points.get(2)));
		
		// return triangles
		return triangles;
	}
	
	/**
	 * Returns true if the triangle formed by A, B, C points is an ear considering the polygon - thus if no other point is inside and it is convex.
	 * @param isCw : Polygon orientation (clockwise/counterclockwise).
	 * @param nonConvexPoints : List of all non-convex points.
	 * @param coordA : ABC triangle
	 * @param coordB : ABC triangle
	 * @param coordC : ABC triangle
	 * @return {@code boolean} : True, when ABC is ear of the polygon.
	 */
	private static final boolean isEar(boolean isCw, List<Point2D> nonConvexPoints, Point2D coordA, Point2D coordB, Point2D coordC)
	{
		// ABC triangle
		if (!(isConvex(isCw, coordA, coordB, coordC)))
			return false;
		
		// iterate over all concave points and check if one of them lies inside the given triangle
		for (int i = 0; i < nonConvexPoints.size(); i++)
		{
			if (isInside(coordA, coordB, coordC, nonConvexPoints.get(i)))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Returns true when the point B is convex considered the actual polygon. A, B and C are three consecutive points of the polygon.
	 * @param isCw : Polygon orientation (clockwise/counterclockwise).
	 * @param coordA : Point, previous to B.
	 * @param coordB : Point, which convex information is being checked.
	 * @param coordC : Point, next to B.
	 * @return {@code boolean} : True, when B is convex point.
	 */
	private static final boolean isConvex(boolean isCw, Point2D coordA, Point2D coordB, Point2D coordC)
	{
		// get vector coordinates
		final int BAx = coordB.getX() - coordA.getX();
		final int BAy = coordB.getY() - coordA.getY();
		
		// get virtual triangle orientation
		final boolean cw = (coordC.getX() * BAy - coordC.getY() * BAx + BAx * coordA.getY() - BAy * coordA.getX()) > 0;
		
		// compare with orientation of polygon
		return cw != isCw;
	}
	
	/**
	 * Returns true, when point P is inside triangle ABC.
	 * @param coordA : ABC triangle
	 * @param coordB : ABC triangle
	 * @param coordC : ABC triangle
	 * @param coordPoint : Point to be checked in ABC.
	 * @return {@code boolean} : True, when P is inside ABC.
	 */
	private static final boolean isInside(Point2D coordA, Point2D coordB, Point2D coordC, Point2D coordPoint)
	{
		// get vector coordinates
		final int BAx = coordB.getX() - coordA.getX();
		final int BAy = coordB.getY() - coordA.getY();
		final int CAx = coordC.getX() - coordA.getX();
		final int CAy = coordC.getY() - coordA.getY();
		final int PAx = coordPoint.getX() - coordA.getX();
		final int PAy = coordPoint.getY() - coordA.getY();
		
		// get determinant
		final double detXYZ = BAx * CAy - CAx * BAy;
		
		// calculate BA and CA coefficient to each P from A
		final double ba = (BAx * PAy - PAx * BAy) / detXYZ;
		final double ca = (PAx * CAy - CAx * PAy) / detXYZ;
		
		// check coefficients
		return (ba > 0 && ca > 0 && (ba + ca) < 1);
	}
}