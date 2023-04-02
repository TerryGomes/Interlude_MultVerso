package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.VersionCheck;

public final class SendProtocolVersion extends L2GameClientPacket
{
	private int _version;
	
	@Override
	protected void readImpl()
	{
		_version = readD();
	}
	
	@Override
	protected void runImpl()
	{
		
		if (_version == Config.CONFIG_PROTOCOL)
		{
			getClient().sendPacket(new VersionCheck(getClient().enableCrypt()));
		}
		else
		{
			getClient().close((L2GameServerPacket) null);
		}
	}
}