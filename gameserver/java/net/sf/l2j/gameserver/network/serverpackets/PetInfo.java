package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;

public class PetInfo extends L2GameServerPacket
{
	private final Summon _summon;
	private final int _val;
	
	private int _maxFed;
	private int _curFed;
	
	public PetInfo(Summon summon, int val)
	{
		_summon = summon;
		_val = val;
		
		if (_summon instanceof Pet)
		{
			final Pet pet = (Pet) _summon;
			
			_curFed = pet.getCurrentFed();
			_maxFed = pet.getPetData().getMaxMeal();
		}
		else if (_summon instanceof Servitor)
		{
			final Servitor sum = (Servitor) _summon;
			
			_curFed = sum.getTimeRemaining();
			_maxFed = sum.getTotalLifeTime();
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xb1);
		writeD(_summon.getSummonType());
		writeD(_summon.getObjectId());
		writeD(_summon.getTemplate().getIdTemplate() + 1000000);
		writeD(0); // 1=attackable
		
		writeD(_summon.getX());
		writeD(_summon.getY());
		writeD(_summon.getZ());
		writeD(_summon.getHeading());
		writeD(0);
		writeD(_summon.getStatus().getMAtkSpd());
		writeD(_summon.getStatus().getPAtkSpd());
		
		final int runSpd = _summon.getStatus().getBaseRunSpeed();
		final int walkSpd = _summon.getStatus().getBaseWalkSpeed();
		
		writeD(runSpd);
		writeD(walkSpd);
		writeD(runSpd);
		writeD(walkSpd);
		writeD(runSpd);
		writeD(walkSpd);
		writeD(runSpd);
		writeD(walkSpd);
		
		writeF(_summon.getStatus().getMovementSpeedMultiplier());
		writeF(_summon.getStatus().getAttackSpeedMultiplier());
		writeF(_summon.getCollisionRadius());
		writeF(_summon.getCollisionHeight());
		writeD(_summon.getWeapon());
		writeD(_summon.getArmor());
		writeD(0);
		writeC((_summon.getOwner() != null) ? 1 : 0); // when pet is dead and player exit game, pet doesn't show master name
		writeC(1);
		writeC((_summon.isInCombat()) ? 1 : 0);
		writeC((_summon.isAlikeDead()) ? 1 : 0);
		writeC((_summon.isShowSummonAnimation()) ? 2 : _val);
		writeS(_summon.getName());
		writeS(_summon.getTitle());
		writeD(1);
		writeD(_summon.getPvpFlag());
		writeD(_summon.getKarma());
		writeD(_curFed);
		writeD(_maxFed);
		writeD((int) _summon.getStatus().getHp());
		writeD(_summon.getStatus().getMaxHp());
		writeD((int) _summon.getStatus().getMp());
		writeD(_summon.getStatus().getMaxMp());
		writeD(_summon.getStatus().getSp());
		writeD(_summon.getStatus().getLevel());
		writeQ(_summon.getStatus().getExp());
		writeQ(_summon.getStatus().getExpForThisLevel());
		writeQ(_summon.getStatus().getExpForNextLevel());
		writeD((_summon instanceof Pet) ? _summon.getInventory().getTotalWeight() : 0);
		writeD(_summon.getWeightLimit());
		writeD(_summon.getStatus().getPAtk(null));
		writeD(_summon.getStatus().getPDef(null));
		writeD(_summon.getStatus().getMAtk(null, null));
		writeD(_summon.getStatus().getMDef(null, null));
		writeD(_summon.getStatus().getAccuracy());
		writeD(_summon.getStatus().getEvasionRate(null));
		writeD(_summon.getStatus().getCriticalHit(null, null));
		writeD((int) _summon.getStatus().getMoveSpeed());
		writeD(_summon.getStatus().getPAtkSpd());
		writeD(_summon.getStatus().getMAtkSpd());
		
		writeD((_summon.getOwner() != null && !_summon.getOwner().getAppearance().isVisible()) ? _summon.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask() : _summon.getAbnormalEffect());
		writeH((_summon.isMountable()) ? 1 : 0);
		writeC(_summon.getMove().getMoveType().getId());
		
		writeH(0); // ??
		writeC(_summon.getTeam().getId());
		writeD(_summon.getSoulShotsPerHit());
		writeD(_summon.getSpiritShotsPerHit());
	}
}