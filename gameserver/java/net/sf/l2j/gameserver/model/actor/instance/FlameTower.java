package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;

import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.model.zone.type.subtype.CastleZoneType;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class FlameTower extends Npc
{
	private int _upgradeLevel;
	private List<Integer> _zoneList;
	
	public FlameTower(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean isAttackableBy(Creature attacker)
	{
		if (!super.isAttackableBy(attacker))
			return false;
		
		if (!(attacker instanceof Playable))
			return false;
		
		if (getCastle() != null && getCastle().getSiege().isInProgress())
			return getCastle().getSiege().checkSides(attacker.getActingPlayer().getClan(), SiegeSide.ATTACKER);
		
		return false;
	}
	
	@Override
	public boolean isAttackableWithoutForceBy(Playable attacker)
	{
		return isAttackableBy(attacker);
	}
	
	@Override
	public void onInteract(Player player)
	{
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		enableZones(false);
		
		if (getCastle() != null)
		{
			// Message occurs only if the trap was triggered first.
			if (_zoneList != null && _upgradeLevel != 0)
				getCastle().getSiege().announce(SystemMessageId.A_TRAP_DEVICE_HAS_BEEN_STOPPED, SiegeSide.DEFENDER);
			
			// Spawn a little version of it. This version is a simple NPC, cleaned on siege end.
			try
			{
				final Spawn spawn = new Spawn(13005);
				spawn.setLoc(getPosition());
				
				final Npc tower = spawn.doSpawn(false);
				tower.setCastle(getCastle());
				
				getCastle().getSiege().getDestroyedTowers().add(tower);
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't spawn the flame tower.", e);
			}
		}
		
		return super.doDie(killer);
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	@Override
	public void deleteMe()
	{
		enableZones(false);
		super.deleteMe();
	}
	
	public final void enableZones(boolean state)
	{
		if (_zoneList != null && _upgradeLevel != 0)
		{
			final int maxIndex = _upgradeLevel * 2;
			for (int i = 0; i < maxIndex; i++)
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneById(_zoneList.get(i));
				if (zone instanceof CastleZoneType)
					((CastleZoneType) zone).setEnabled(state);
			}
		}
	}
	
	public final void setUpgradeLevel(int level)
	{
		_upgradeLevel = level;
	}
	
	public final void setZoneList(List<Integer> list)
	{
		_zoneList = list;
		enableZones(true);
	}
}