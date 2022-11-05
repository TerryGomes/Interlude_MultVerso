package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.Henna;

public class HennaItemUnequipInfo extends L2GameServerPacket
{
	private final Henna _henna;
	private final int _adena;
	private final int _int;
	private final int _str;
	private final int _con;
	private final int _men;
	private final int _dex;
	private final int _wit;
	
	public HennaItemUnequipInfo(Henna henna, Player player)
	{
		_henna = henna;
		_adena = player.getAdena();
		_int = player.getStatus().getINT();
		_str = player.getStatus().getSTR();
		_con = player.getStatus().getCON();
		_men = player.getStatus().getMEN();
		_dex = player.getStatus().getDEX();
		_wit = player.getStatus().getWIT();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xe6);
		writeD(_henna.getSymbolId());
		writeD(_henna.getDyeId());
		writeD(Henna.REMOVE_AMOUNT);
		writeD(_henna.getRemovePrice());
		writeD(1);
		writeD(_adena);
		writeD(_int);
		writeC(_int - _henna.getINT());
		writeD(_str);
		writeC(_str - _henna.getSTR());
		writeD(_con);
		writeC(_con - _henna.getCON());
		writeD(_men);
		writeC(_men - _henna.getMEN());
		writeD(_dex);
		writeC(_dex - _henna.getDEX());
		writeD(_wit);
		writeC(_wit - _henna.getWIT());
	}
}