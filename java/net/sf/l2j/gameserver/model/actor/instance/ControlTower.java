package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class ControlTower extends Npc
{
	public ControlTower(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isAttackableBy(Creature attacker)
	{
		if (!super.isAttackableBy(attacker) || !(attacker instanceof Playable))
		{
			return false;
		}

		if (getCastle() != null && getCastle().getSiege().isInProgress())
		{
			return getCastle().getSiege().checkSides(attacker.getActingPlayer().getClan(), SiegeSide.ATTACKER);
		}

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
		if (getCastle() != null)
		{
			final Siege siege = getCastle().getSiege();
			if (siege.isInProgress())
			{
				setScriptValue(1);

				// If Life Control Tower amount reach 0, broadcast a message to defenders.
				if (siege.getControlTowerCount() == 0)
				{
					siege.announce(SystemMessageId.TOWER_DESTROYED_NO_RESURRECTION, SiegeSide.DEFENDER);
				}

				// Spawn a little version of it. This version is a simple NPC, cleaned on siege end.
				try
				{
					final Spawn spawn = new Spawn(13003);
					spawn.setLoc(getPosition());

					final Npc tower = spawn.doSpawn(false);
					tower.setCastle(getCastle());

					siege.getDestroyedTowers().add(tower);
				}
				catch (Exception e)
				{
					LOGGER.error("Couldn't spawn the control tower.", e);
				}
			}
		}
		return super.doDie(killer);
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}