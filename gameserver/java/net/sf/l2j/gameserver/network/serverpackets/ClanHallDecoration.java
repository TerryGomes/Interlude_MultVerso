package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.clanhall.ClanHallFunction;

public class ClanHallDecoration extends L2GameServerPacket
{
	private final ClanHall _ch;
	
	public ClanHallDecoration(ClanHall ch)
	{
		_ch = ch;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xf7);
		writeD(_ch.getId());
		
		// FUNC_RESTORE_HP
		ClanHallFunction chf = _ch.getFunction(ClanHall.FUNC_RESTORE_HP);
		if (chf == null || chf.getLvl() == 0)
			writeC(0);
		else if ((_ch.getGrade() == 0 && chf.getLvl() < 220) || (_ch.getGrade() == 1 && chf.getLvl() < 160) || (_ch.getGrade() == 2 && chf.getLvl() < 260) || (_ch.getGrade() == 3 && chf.getLvl() < 300))
			writeC(1);
		else
			writeC(2);
		
		// FUNC_RESTORE_MP
		chf = _ch.getFunction(ClanHall.FUNC_RESTORE_MP);
		if (chf == null || chf.getLvl() == 0)
		{
			writeC(0);
			writeC(0);
		}
		else if (((_ch.getGrade() == 0 || _ch.getGrade() == 1) && chf.getLvl() < 25) || (_ch.getGrade() == 2 && chf.getLvl() < 30) || (_ch.getGrade() == 3 && chf.getLvl() < 40))
		{
			writeC(1);
			writeC(1);
		}
		else
		{
			writeC(2);
			writeC(2);
		}
		
		// FUNC_RESTORE_EXP
		chf = _ch.getFunction(ClanHall.FUNC_RESTORE_EXP);
		if (chf == null || chf.getLvl() == 0)
			writeC(0);
		else if ((_ch.getGrade() == 0 && chf.getLvl() < 25) || (_ch.getGrade() == 1 && chf.getLvl() < 30) || (_ch.getGrade() == 2 && chf.getLvl() < 40) || (_ch.getGrade() == 3 && chf.getLvl() < 50))
			writeC(1);
		else
			writeC(2);
		
		// FUNC_TELEPORT
		chf = _ch.getFunction(ClanHall.FUNC_TELEPORT);
		if (chf == null || chf.getLvl() == 0)
			writeC(0);
		else if (chf.getLvl() < 2)
			writeC(1);
		else
			writeC(2);
		
		writeC(0);
		
		// CURTAINS
		chf = _ch.getFunction(ClanHall.FUNC_DECO_CURTAINS);
		if (chf == null || chf.getLvl() == 0)
			writeC(0);
		else if (chf.getLvl() <= 1)
			writeC(1);
		else
			writeC(2);
		
		// FUNC_ITEM_CREATE
		chf = _ch.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if (chf == null || chf.getLvl() == 0)
			writeC(0);
		else if ((_ch.getGrade() == 0 && chf.getLvl() < 2) || chf.getLvl() < 3)
			writeC(1);
		else
			writeC(2);
		
		// FUNC_SUPPORT
		chf = _ch.getFunction(ClanHall.FUNC_SUPPORT);
		if (chf == null || chf.getLvl() == 0)
		{
			writeC(0);
			writeC(0);
		}
		else if ((_ch.getGrade() == 0 && chf.getLvl() < 2) || (_ch.getGrade() == 1 && chf.getLvl() < 4) || (_ch.getGrade() == 2 && chf.getLvl() < 5) || (_ch.getGrade() == 3 && chf.getLvl() < 8))
		{
			writeC(1);
			writeC(1);
		}
		else
		{
			writeC(2);
			writeC(2);
		}
		
		// Front Plateform
		chf = _ch.getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM);
		if (chf == null || chf.getLvl() == 0)
			writeC(0);
		else if (chf.getLvl() <= 1)
			writeC(1);
		else
			writeC(2);
		
		// FUNC_ITEM_CREATE
		chf = _ch.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if (chf == null || chf.getLvl() == 0)
			writeC(0);
		else if ((_ch.getGrade() == 0 && chf.getLvl() < 2) || chf.getLvl() < 3)
			writeC(1);
		else
			writeC(2);
		
		writeD(0);
		writeD(0);
	}
}