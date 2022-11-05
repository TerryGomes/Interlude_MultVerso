package net.sf.l2j.gameserver.network.serverpackets;

public class ObservationMode extends L2GameServerPacket
{
	private final int _x;
	private final int _y;
	private final int _z;
	
	public ObservationMode(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xdf);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeC(0x00);
		writeC(0xc0);
		writeC(0x00);
	}
}