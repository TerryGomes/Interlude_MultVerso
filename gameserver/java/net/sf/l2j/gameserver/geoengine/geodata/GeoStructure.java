package net.sf.l2j.gameserver.geoengine.geodata;

import net.sf.l2j.gameserver.model.World;

public final class GeoStructure
{
	// Geo cell direction (nswe) flags.
	public static final byte CELL_FLAG_NONE = 0x00;
	public static final byte CELL_FLAG_E = 0x01;
	public static final byte CELL_FLAG_W = 0x02;
	public static final byte CELL_FLAG_S = 0x04;
	public static final byte CELL_FLAG_N = 0x08;
	public static final byte CELL_FLAG_SE = 0x10;
	public static final byte CELL_FLAG_SW = 0x20;
	public static final byte CELL_FLAG_NE = 0x40;
	public static final byte CELL_FLAG_NW = (byte) 0x80;
	public static final byte CELL_FLAG_ALL = 0x0F;
	
	// Geo cell expansion flags
	public static final byte CELL_EXPANSION_E = CELL_FLAG_E | CELL_FLAG_NE | CELL_FLAG_SE;
	public static final byte CELL_EXPANSION_W = CELL_FLAG_W | CELL_FLAG_NW | CELL_FLAG_SW;
	public static final byte CELL_EXPANSION_S = CELL_FLAG_S | CELL_FLAG_SW | CELL_FLAG_SE;
	public static final byte CELL_EXPANSION_N = CELL_FLAG_N | CELL_FLAG_NW | CELL_FLAG_NE;
	public static final byte CELL_EXPANSION_SE = CELL_FLAG_SE | CELL_FLAG_S | CELL_FLAG_E;
	public static final byte CELL_EXPANSION_SW = CELL_FLAG_SW | CELL_FLAG_S | CELL_FLAG_W;
	public static final byte CELL_EXPANSION_NE = CELL_FLAG_NE | CELL_FLAG_N | CELL_FLAG_E;
	public static final byte CELL_EXPANSION_NW = CELL_FLAG_NW | CELL_FLAG_N | CELL_FLAG_W;
	// public static final byte CELL_EXPANSION_MASK = CELL_FLAG_SE | CELL_FLAG_SW | CELL_FLAG_NE | CELL_FLAG_NW;
	public static final byte CELL_EXPANSION_ALL = (byte) 0xFF;
	
	// Geo cell height constants.
	public static final int CELL_SIZE = 16;
	public static final int CELL_HEIGHT = 8;
	public static final int CELL_IGNORE_HEIGHT = CELL_HEIGHT * 6;
	
	// Geo block type identification.
	public static final byte TYPE_FLAT_L2J_L2OFF = 0;
	public static final byte TYPE_COMPLEX_L2J = 1;
	public static final byte TYPE_COMPLEX_L2OFF = 0x40;
	public static final byte TYPE_MULTILAYER_L2J = 2;
	// public static final byte TYPE_MULTILAYER_L2OFF = 0x41; // officially not does exist, is anything above complex block (0x41 - 0xFFFF)
	
	// Geo block dimensions.
	public static final int BLOCK_CELLS_X = 8;
	public static final int BLOCK_CELLS_Y = 8;
	public static final int BLOCK_CELLS = BLOCK_CELLS_X * BLOCK_CELLS_Y;
	
	// Geo region dimensions.
	public static final int REGION_BLOCKS_X = 256;
	public static final int REGION_BLOCKS_Y = 256;
	public static final int REGION_BLOCKS = REGION_BLOCKS_X * REGION_BLOCKS_Y;
	
	public static final int REGION_CELLS_X = REGION_BLOCKS_X * BLOCK_CELLS_X;
	public static final int REGION_CELLS_Y = REGION_BLOCKS_Y * BLOCK_CELLS_Y;
	
	// Geo world dimensions.
	public static final int GEO_REGIONS_X = (World.TILE_X_MAX - World.TILE_X_MIN + 1);
	public static final int GEO_REGIONS_Y = (World.TILE_Y_MAX - World.TILE_Y_MIN + 1);
	
	public static final int GEO_BLOCKS_X = GEO_REGIONS_X * REGION_BLOCKS_X;
	public static final int GEO_BLOCKS_Y = GEO_REGIONS_Y * REGION_BLOCKS_Y;
	
	public static final int GEO_CELLS_X = GEO_BLOCKS_X * BLOCK_CELLS_X;
	public static final int GEO_CELLS_Y = GEO_BLOCKS_Y * BLOCK_CELLS_Y;
}