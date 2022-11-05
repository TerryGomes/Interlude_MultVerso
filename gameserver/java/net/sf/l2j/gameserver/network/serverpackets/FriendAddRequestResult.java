package net.sf.l2j.gameserver.network.serverpackets;

public class FriendAddRequestResult extends L2GameServerPacket
{
	public static final FriendAddRequestResult STATIC_ACCEPT = new FriendAddRequestResult(true);
	public static final FriendAddRequestResult STATIC_FAIL = new FriendAddRequestResult(false);
	
	private final int _accepted;
	
	private FriendAddRequestResult(boolean accepted)
	{
		_accepted = accepted ? 1 : 0;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x77);
		writeD(_accepted);
	}
}