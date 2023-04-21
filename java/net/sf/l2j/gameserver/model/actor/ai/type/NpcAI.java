package net.sf.l2j.gameserver.model.actor.ai.type;

import java.util.List;

import net.sf.l2j.gameserver.data.xml.WalkerRouteData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.WalkerLocation;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.taskmanager.WalkerTaskManager;

public class NpcAI<T extends Npc> extends CreatureAI<T>
{
	private int _index = 0;

	public NpcAI(T npc)
	{
		super(npc);
	}

	@Override
	protected void onEvtArrived()
	{
		// Retrieve walking route, if any.
		final List<WalkerLocation> route = WalkerRouteData.getInstance().getWalkerRoute(_actor.getNpcId());
		if (route == null || route.isEmpty())
		{
			return;
		}

		// Retrieve current node.
		final WalkerLocation node = route.get(_index);

		// If node got a NpcStringId, broadcast it.
		if (node.getNpcStringId() != null)
		{
			_actor.broadcastNpcSay(node.getNpcStringId());
		}

		// We freeze the NPC and store it on WalkerTaskManager, which will release it in the future.
		if (node.getDelay() > 0)
		{
			// If node got a SocialAction id, broadcast it.
			if (node.getSocialId() > 0)
			{
				_actor.broadcastPacket(new SocialAction(_actor, node.getSocialId()));
			}

			// Delay the movement.
			WalkerTaskManager.getInstance().add(_actor, node.getDelay());
		}
		else
		{
			moveToNextPoint();
		}
	}

	public int getIndex()
	{
		return _index;
	}

	/**
	 * Move the {@link Npc} to the next {@link WalkerLocation} of his route.
	 */
	public void moveToNextPoint()
	{
		// Retrieve walking route, if any.
		final List<WalkerLocation> route = WalkerRouteData.getInstance().getWalkerRoute(_actor.getNpcId());
		if (route == null || route.isEmpty())
		{
			return;
		}

		// Actor is on reverse path. Decrease the index.
		if (_actor.isReversePath() && _index > 0)
		{
			_index--;

			if (_index == 0)
			{
				_actor.setReversePath(false);
			}
		}
		// Set the next node value.
		else if (_index < route.size() - 1)
		{
			_index++;
			// Reset the index, and return the behavior to normal state.
		}
		else
		{
			_index = 0;
		}

		// Retrieve next node.
		WalkerLocation node = route.get(_index);

		// Test the path. If no path is found, we set the reverse path.
		if (!GeoEngine.getInstance().canMoveToTarget(_actor.getPosition(), node))
		{
			final List<Location> path = GeoEngine.getInstance().findPath(_actor.getX(), _actor.getY(), _actor.getZ(), node.getX(), node.getY(), node.getZ(), true, null);
			if (path.isEmpty())
			{
				if (_index == 0)
				{
					_index = route.size() - 2;
					_actor.setReversePath(true);
				}
				else
				{
					_index--;
				}

				node = route.get(_index);
			}
		}

		// Running state.
		if (node.mustRun())
		{
			_actor.forceRunStance();
		}
		else
		{
			_actor.forceWalkStance();
		}

		tryToMoveTo(node, null);
	}
}