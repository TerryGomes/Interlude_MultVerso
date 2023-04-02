package net.sf.l2j.gameserver.model.actor.ai.type;

import net.sf.l2j.gameserver.model.actor.Boat;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.VehicleDeparture;

public class BoatAI extends CreatureAI<Boat>
{
	public BoatAI(Boat boat)
	{
		super(boat);
	}

	@Override
	public void describeStateToPlayer(Player player)
	{
		if (_actor.isMoving())
		{
			player.sendPacket(new VehicleDeparture(_actor));
		}
	}

	@Override
	public void onEvtAttacked(Creature attacker)
	{
	}

	@Override
	protected void onEvtArrived()
	{
		_actor.getMove().onArrival();
	}

	@Override
	protected void onEvtDead()
	{
	}

	@Override
	protected void onEvtFinishedCasting()
	{
	}
}