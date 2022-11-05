package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;

public class PetStatusUpdate extends L2GameServerPacket
{
	private final Summon _summon;
	
	private int _maxFed;
	private int _curFed;
	
	public PetStatusUpdate(Summon summon)
	{
		_summon = summon;
		if (_summon instanceof Pet)
		{
			Pet pet = (Pet) _summon;
			_curFed = pet.getCurrentFed();
			_maxFed = pet.getPetData().getMaxMeal();
		}
		else if (_summon instanceof Servitor)
		{
			Servitor sum = (Servitor) _summon;
			_curFed = sum.getTimeRemaining();
			_maxFed = sum.getTotalLifeTime();
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xb5);
		writeD(_summon.getSummonType());
		writeD(_summon.getObjectId());
		writeD(_summon.getX());
		writeD(_summon.getY());
		writeD(_summon.getZ());
		writeS(_summon.getTitle());
		writeD(_curFed);
		writeD(_maxFed);
		writeD((int) _summon.getStatus().getHp());
		writeD(_summon.getStatus().getMaxHp());
		writeD((int) _summon.getStatus().getMp());
		writeD(_summon.getStatus().getMaxMp());
		writeD(_summon.getStatus().getLevel());
		writeQ(_summon.getStatus().getExp());
		writeQ(_summon.getStatus().getExpForThisLevel());// 0% absolute value
		writeQ(_summon.getStatus().getExpForNextLevel());// 100% absolute value
	}
}