package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;

import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.TeleportMode;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;

public class MoveBackwardToLocation extends L2GameClientPacket
{
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private int _originX;
	private int _originY;
	@SuppressWarnings("unused")
	private int _originZ;
	
	@SuppressWarnings("unused")
	private int _moveMovement;
	
	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		
		try
		{
			_moveMovement = readD(); // is 0 if cursor keys are used 1 if mouse is used
		}
		catch (BufferUnderflowException e)
		{
			if (Config.L2WALKER_PROTECTION)
			{
				final Player player = getClient().getPlayer();
				if (player != null)
					player.logout(false);
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getStatus().getMoveSpeed() == 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.CANT_MOVE_TOO_ENCUMBERED);
			return;
		}
		
		if (player.getActiveEnchantItem() != null)
		{
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}
		
		// Correcting targetZ from floor level to head level
		_targetZ += player.getCollisionHeight();
		
		switch (player.getTeleportMode())
		{
			case ONE_TIME:
				player.setTeleportMode(TeleportMode.NONE);
			case FULL_TIME:
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.teleportTo(_targetX, _targetY, _targetZ, 0);
				return;
		}
		
		double dx = _targetX - _originX;
		double dy = _targetY - _originY;
		
		if ((dx * dx + dy * dy) > 98010000) // 9900*9900
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!player.isInBoat())
			player.getAI().tryToMoveTo(new Location(_targetX, _targetY, _targetZ), null);
		// Player is on the boat, we don't want to schedule a real movement until he gets out of it otherwise GeoEngine will be confused.
		else
		{
			// We want to set the real player heading though so it can be used during actual departure.
			player.getPosition().setHeading(MathUtil.calculateHeadingFrom(_originX, _originY, _targetX, _targetY));
			
			// Just sending a client move packet so player will try to move towards exit.
			player.broadcastPacket(new MoveToLocation(player, new Location(_targetX, _targetY, _targetZ)));
		}
	}
}