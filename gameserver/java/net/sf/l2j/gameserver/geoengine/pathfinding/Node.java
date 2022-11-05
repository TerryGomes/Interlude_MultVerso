package net.sf.l2j.gameserver.geoengine.pathfinding;

import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;
import net.sf.l2j.gameserver.model.location.Location;

public class Node extends Location implements Comparable<Node>
{
	// Node geodata values.
	private int _geoX;
	private int _geoY;
	private byte _nswe;
	private byte _nsweExpand;
	
	// The cost G (movement cost done) and cost H (estimated cost to target).
	private int _costG;
	private int _costH;
	private int _costF;
	
	// Node parent (reverse path construction).
	private Node _parent;
	
	public Node()
	{
		super(0, 0, 0);
	}
	
	@Override
	public void clean()
	{
		super.clean();
		
		_geoX = 0;
		_geoY = 0;
		_nswe = GeoStructure.CELL_FLAG_NONE;
		_nsweExpand = GeoStructure.CELL_FLAG_NONE;
		
		_costG = 0;
		_costH = 0;
		_costF = 0;
		
		_parent = null;
	}
	
	public final void setGeo(int gx, int gy, int gz, byte nswe, byte nsweExpand)
	{
		super.set(GeoEngine.getWorldX(gx), GeoEngine.getWorldY(gy), gz);
		
		_geoX = gx;
		_geoY = gy;
		_nswe = nswe;
		_nsweExpand = nsweExpand;
	}
	
	public final void setCost(Node parent, int weight, int costH)
	{
		_costG = weight;
		if (parent != null)
			_costG += parent._costG;
		_costH = costH;
		_costF = _costG + _costH;
		
		_parent = parent;
	}
	
	public int getGeoX()
	{
		return _geoX;
	}
	
	public int getGeoY()
	{
		return _geoY;
	}
	
	public byte getNswe()
	{
		return _nswe;
	}
	
	public byte getNsweExpand()
	{
		return _nsweExpand;
	}
	
	public int getCostF()
	{
		return _costF;
	}
	
	public Node getParent()
	{
		return _parent;
	}
	
	@Override
	public int compareTo(Node o)
	{
		return _costF - o._costF;
	}
}