package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Collection;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;

public class ExHeroList extends L2GameServerPacket
{
	private final Collection<StatSet> _sets;
	
	public ExHeroList()
	{
		_sets = HeroManager.getInstance().getHeroes().values();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x23);
		writeD(_sets.size());
		
		for (StatSet set : _sets)
		{
			writeS(set.getString(Olympiad.CHAR_NAME));
			writeD(set.getInteger(Olympiad.CLASS_ID));
			writeS(set.getString(HeroManager.CLAN_NAME, ""));
			writeD(set.getInteger(HeroManager.CLAN_CREST, 0));
			writeS(set.getString(HeroManager.ALLY_NAME, ""));
			writeD(set.getInteger(HeroManager.ALLY_CREST, 0));
			writeD(set.getInteger(HeroManager.COUNT));
		}
	}
}