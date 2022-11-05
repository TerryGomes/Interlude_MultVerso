package net.sf.l2j.gameserver.network.clientpackets;

import java.awt.Color;

import net.sf.l2j.gameserver.enums.TeleportMode;
import net.sf.l2j.gameserver.enums.actors.MoveType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.GetOnVehicle;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;

public class ValidatePosition extends L2GameClientPacket
{
	private int _x;
	private int _y;
	private int _z;
	@SuppressWarnings("unused")
	private int _heading;
	private int _boatId;
	
	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
		_boatId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null || player.isTeleporting() || player.isInObserverMode())
			return;
		
		// Disable validation for CameraMode.
		if (player.getTeleportMode() == TeleportMode.CAMERA_MODE)
		{
			// Retrieve the current WorldRegion passed by the client location, and set it. It allows knownlist to be properly refreshed.
			final WorldRegion region = World.getInstance().getRegion(_x, _y);
			if (region != null)
				player.setRegion(region);
			
			player.setXYZ(_x, _y, _z);
			return;
		}
		
		// Disable validation during fall to avoid "jumping".
		if (player.isFalling(_z))
			return;
		
		final float actualSpeed;
		final double dist;
		
		// Send back position if client<>server desync is too big. For boats, send back if the desync is bigger than 500.
		if (player.isInBoat())
		{
			actualSpeed = 500;
			dist = player.getBoatPosition().distance2D(_x, _y);
			
			if (dist > actualSpeed)
				sendPacket(new GetOnVehicle(player.getObjectId(), _boatId, player.getBoatPosition()));
		}
		// For regular movement, send back if the desync is bigger than actual speed.
		else
		{
			actualSpeed = player.getStatus().getMoveSpeed();
			dist = (player.getMove().getMoveType() == MoveType.GROUND) ? player.getPosition().distance2D(_x, _y) : player.getPosition().distance3D(_x, _y, _z);
			
			if (dist > actualSpeed)
				sendPacket(new ValidateLocation(player));
		}
		
		// Draw a debug of this packet if activated.
		if (player.getMove().isDebugMove())
		{
			final String desc = "speed=" + actualSpeed + " desync=" + dist;
			
			// Draw debug packet to all players.
			for (Player p : player.getSurroundingGMs())
			{
				// Get debug packet.
				final ExServerPrimitive debug = p.getDebugPacket("MOVE" + player.getObjectId());
				debug.addPoint(desc, Color.GREEN, true, _x, _y, _z);
				debug.sendTo(p);
			}
		}
	}
}