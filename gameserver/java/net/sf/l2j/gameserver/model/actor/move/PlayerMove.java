package net.sf.l2j.gameserver.model.actor.move;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.actors.MoveType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.WaterZone;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.MoveToPawn;

/**
 * This class groups all movement data related to a {@link Player}.
 */
public class PlayerMove extends CreatureMove<Player>
{
	private volatile Instant _instant;
	
	private int _moveTimeStamp;
	
	private double _zAccurate;
	
	public PlayerMove(Player actor)
	{
		super(actor);
	}
	
	@Override
	public void cancelMoveTask()
	{
		super.cancelMoveTask();
		
		_moveTimeStamp = 0;
	}
	
	private void moveToPawn(WorldObject pawn, int offset)
	{
		if (!_actor.getClient().performAction(FloodProtector.MOVE))
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Get the current position of the pawn.
		final int tx = pawn.getX();
		final int ty = pawn.getY();
		final int tz = pawn.getZ();
		
		// Set the pawn and offset.
		_pawn = pawn;
		_offset = offset;
		
		if (_task != null)
			updatePosition(true);
		
		_instant = Instant.now();
		
		// Get the current position of the actor.
		final int ox = _actor.getX();
		final int oy = _actor.getY();
		final int oz = _actor.getZ();
		
		// Set the current x/y/z.
		_xAccurate = ox;
		_yAccurate = oy;
		_zAccurate = oz;
		
		// Initialize variables.
		_geoPath.clear();
		
		// Draw a debug of this movement if activated.
		if (_isDebugMove)
		{
			// Draw debug packet to surrounding GMs.
			for (Player p : _actor.getSurroundingGMs())
			{
				// Get debug packet.
				final ExServerPrimitive debug = p.getDebugPacket("MOVE" + _actor.getObjectId());
				
				// Reset the packet lines and points.
				debug.reset();
				
				// Add a RED point corresponding to initial start location.
				debug.addPoint(Color.RED, ox, oy, oz);
				
				// Add a WHITE line corresponding to the initial click release.
				debug.addLine("MoveToPawn (" + _offset + "): " + tx + " " + ty + " " + tz, Color.WHITE, true, ox, oy, oz, tx, ty, tz);
				
				p.sendMessage("Moving from " + ox + " " + oy + " " + oz + " to " + tx + " " + ty + " " + tz);
			}
		}
		
		// Set the destination.
		_destination.set(tx, ty, tz);
		
		_actor.getPosition().setHeadingTo(tx, ty);
		
		registerMoveTask();
		
		_actor.broadcastPacket(new MoveToPawn(_actor, pawn, offset));
	}
	
	@Override
	protected void moveToLocation(Location destination, boolean pathfinding)
	{
		if (!_actor.getClient().performAction(FloodProtector.MOVE))
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (_task != null)
			updatePosition(true);
		
		_instant = Instant.now();
		
		// Get the current position of the Creature.
		final Location position = _actor.getPosition().clone();
		
		// Set the current x/y/z.
		_xAccurate = position.getX();
		_yAccurate = position.getY();
		_zAccurate = position.getZ();
		
		// Initialize variables.
		_geoPath.clear();
		
		if (pathfinding)
		{
			// Calculate the path.
			final Location loc = calculatePath(position.getX(), position.getY(), position.getZ(), destination.getX(), destination.getY(), destination.getZ());
			if (loc != null)
				destination.set(loc);
		}
		
		// Draw a debug of this movement if activated.
		if (_isDebugMove)
		{
			// Draw debug packet to surrounding GMs.
			for (Player p : _actor.getSurroundingGMs())
			{
				// Get debug packet.
				final ExServerPrimitive debug = p.getDebugPacket("MOVE" + _actor.getObjectId());
				
				// Reset the packet lines and points.
				debug.reset();
				
				// Add a WHITE line corresponding to the initial click release.
				debug.addLine("MoveToLocation: " + destination.toString(), Color.WHITE, true, position, destination);
				
				// Add a RED point corresponding to initial start location.
				debug.addPoint(Color.RED, position);
				
				// Add YELLOW lines corresponding to the geo path, if any. Add a single YELLOW line if no geoPath encountered.
				if (!_geoPath.isEmpty())
				{
					// Add manually a segment, since poll() was executed.
					debug.addLine("Segment #1", Color.YELLOW, true, position, destination);
					
					// Initialize a Location based on target location.
					final Location curPos = new Location(destination);
					int i = 2;
					
					// Iterate geo path.
					for (final Location geoPos : _geoPath)
					{
						// Draw a blue line going from initial to geo path.
						debug.addLine("Segment #" + i, Color.YELLOW, true, curPos, geoPos);
						
						// Set current path as geo path ; the draw will start from here.
						curPos.set(geoPos);
						i++;
					}
				}
				else
					debug.addLine("No geopath", Color.YELLOW, true, position, destination);
				
				p.sendMessage("Moving from " + position.toString() + " to " + destination.toString());
			}
		}
		
		// Set the destination.
		_destination.set(destination);
		
		// Calculate the heading.
		_actor.getPosition().setHeadingTo(destination);
		
		registerMoveTask();
		
		_actor.broadcastPacket(new MoveToLocation(_actor, destination));
	}
	
	@Override
	public boolean updatePosition(boolean firstRun)
	{
		if (_task == null || !_actor.isVisible())
			return true;
		
		// We got a pawn target, but it is not known anymore - stop the movement.
		if (_pawn != null && !_actor.knows(_pawn))
			return true;
		
		// Save current Instant.
		final Instant instant = Instant.now();
		
		// Compare tested and saved Instants.
		long timePassed = Duration.between(_instant, instant).toMillis();
		if (timePassed == 0)
			timePassed = 1;
		
		_instant = instant;
		
		final MoveType type = getMoveType();
		final boolean canBypassZCheck = _actor.getBoat() != null || type == MoveType.FLY;
		
		// Increment the timestamp.
		_moveTimeStamp++;
		
		final int curX = _actor.getX();
		final int curY = _actor.getY();
		final int curZ = _actor.getZ();
		
		if (_pawn != null && !firstRun)
			_destination.set(_pawn.getPosition());
		
		if (type == MoveType.GROUND)
			_destination.setZ(GeoEngine.getInstance().getHeight(_destination));
		
		final double dx = _destination.getX() - curX;
		final double dy = _destination.getY() - curY;
		final double dz = _destination.getZ() - curZ;
		
		// We use Z for delta calculation only if different of GROUND MoveType.
		final double leftDistance = (type == MoveType.GROUND) ? Math.sqrt(dx * dx + dy * dy) : Math.sqrt(dx * dx + dy * dy + dz * dz);
		final double passedDistance = _actor.getStatus().getRealMoveSpeed(type != MoveType.FLY && _moveTimeStamp <= 5) / (1000d / timePassed);
		
		// Calculate the maximum Z. Only FLY is allowed to bypass Z check.
		int maxZ = World.WORLD_Z_MAX;
		if (canBypassZCheck)
		{
			final WaterZone waterZone = ZoneManager.getInstance().getZone(curX, curY, curZ, WaterZone.class);
			if (waterZone != null && GeoEngine.getInstance().getHeight(curX, curY, curZ) - waterZone.getWaterZ() < -20)
				maxZ = waterZone.getWaterZ();
		}
		
		final int nextX;
		final int nextY;
		final int nextZ;
		
		// Set the position only
		if (passedDistance < leftDistance)
		{
			// Calculate the current distance fraction based on the delta.
			final double fraction = passedDistance / leftDistance;
			
			_xAccurate += dx * fraction;
			_yAccurate += dy * fraction;
			_zAccurate += dz * fraction;
			
			// Note: Z coord shifted up to avoid dual-layer issues.
			nextX = (int) Math.round(_xAccurate);
			nextY = (int) Math.round(_yAccurate);
			nextZ = Math.min((type == MoveType.GROUND) ? GeoEngine.getInstance().getHeight(nextX, nextY, curZ + 2 * GeoStructure.CELL_HEIGHT) : (int) Math.round(_zAccurate), maxZ);
		}
		// Already there : set the position to the destination.
		else
		{
			nextX = _destination.getX();
			nextY = _destination.getY();
			nextZ = Math.min(_destination.getZ(), maxZ);
		}
		
		// Check if location can be reached (case of dynamic objects, such as opening doors/fences).
		if (type == MoveType.GROUND && !GeoEngine.getInstance().canMoveToTarget(curX, curY, curZ, nextX, nextY, nextZ))
		{
			_blocked = true;
			return true;
		}
		
		// Calculate the heading. Must be computed BEFORE setting setXYZ, otherwise ends to 0.
		if (_pawn != null)
			_actor.getPosition().setHeadingTo(nextX, nextY);
		
		// Set the position of the Creature.
		_actor.setXYZ(nextX, nextY, nextZ);
		
		// Draw a debug of this movement if activated.
		if (_isDebugMove)
		{
			final String heading = "" + _actor.getHeading();
			
			// Draw debug packet to surrounding GMs.
			for (Player p : _actor.getSurroundingGMs())
			{
				// Get debug packet.
				final ExServerPrimitive debug = p.getDebugPacket("MOVE" + _actor.getObjectId());
				
				// Draw a RED point for current position.
				debug.addPoint(heading, Color.RED, true, _actor.getPosition());
				
				// Send the packet to the Player.
				debug.sendTo(p);
				
				// We are supposed to run, but the difference of Z is way too high.
				if (type == MoveType.GROUND && Math.abs(curZ - _actor.getPosition().getZ()) > 100)
					p.sendMessage("Falling/Climb bug found when moving from " + curX + ", " + curY + ", " + curZ + " to " + _actor.getPosition().toString());
			}
		}
		
		_actor.revalidateZone(false);
		
		if (isOnLastPawnMoveGeoPath() && ((type == MoveType.GROUND) ? _actor.isIn2DRadius(_pawn, _offset) : _actor.isIn3DRadius(_pawn, _offset)))
			return true;
		
		return (passedDistance >= leftDistance);
	}
	
	/**
	 * @param target : The WorldObject we try to reach.
	 * @param offset : The interact area radius.
	 * @param isShiftPressed : If movement is necessary, it disallows it.
	 * @return true if a movement must be done to reach the {@link WorldObject}, based on an offset.
	 */
	public boolean maybeMoveToPawn(WorldObject target, int offset, boolean isShiftPressed)
	{
		if (offset < 0 || _actor == target)
			return false;
		
		if (_actor.isIn3DRadius(target, (int) (offset + _actor.getCollisionRadius() + ((target instanceof Creature) ? ((Creature) target).getCollisionRadius() : 0))))
			return false;
		
		if (!_actor.isMovementDisabled() && !isShiftPressed)
		{
			_pawn = target;
			_offset = offset;
			
			moveToPawn(target, offset);
		}
		
		return true;
	}
	
	@Override
	protected void offensiveFollowTask(Creature target, int offset)
	{
		// No follow task, return.
		if (_followTask == null)
			return;
		
		// Pawn isn't registered on knownlist.
		if (!_actor.knows(target))
		{
			_actor.getAI().tryToActive();
			return;
		}
		
		final int realOffset = (int) (offset + _actor.getCollisionRadius() + target.getCollisionRadius());
		if ((getMoveType() == MoveType.GROUND) ? _actor.isIn2DRadius(target, realOffset) : _actor.isIn3DRadius(target, realOffset))
			return;
		
		// If an obstacle is/appears while the _followTask is running (ex: door closing) between the Player and the pawn, move to latest good location.
		final Location moveOk = GeoEngine.getInstance().getValidLocation(_actor, target);
		final boolean isPathClear = MathUtil.checkIfInRange(offset, target, moveOk, true);
		if (isPathClear)
		{
			_pawn = target;
			_offset = offset;
			
			moveToPawn(target, offset);
		}
		else
		{
			_pawn = null;
			_offset = 0;
			
			moveToLocation(moveOk, false);
		}
	}
	
	@Override
	protected void friendlyFollowTask(Creature target, int offset)
	{
		// No follow task, return.
		if (_followTask == null)
			return;
		
		// Invalid pawn to follow, or the pawn isn't registered on knownlist.
		if (!_actor.knows(target))
		{
			_actor.getAI().tryToActive();
			return;
		}
		
		// Don't bother moving if already in radius.
		if ((getMoveType() == MoveType.GROUND) ? _actor.isIn2DRadius(target, offset) : _actor.isIn3DRadius(target, offset))
			return;
		
		if (_task == null)
		{
			_pawn = target;
			_offset = offset;
			
			moveToPawn(target, offset);
		}
	}
	
	@Override
	protected Location calculatePath(int ox, int oy, int oz, int tx, int ty, int tz)
	{
		// Retain some informations fur future use.
		final MoveType moveType = getMoveType();
		
		// We can process to next point without extra help ; return directly.
		if (moveType == MoveType.FLY)
		{
			if (GeoEngine.getInstance().canFlyToTarget(ox, oy, oz, 32, tx, ty, tz))
				return null;
		}
		else if (GeoEngine.getInstance().canMoveToTarget(ox, oy, oz, tx, ty, tz))
			return null;
		
		// Create dummy packet.
		final ExServerPrimitive dummy = _isDebugPath ? new ExServerPrimitive() : null;
		
		if (moveType != MoveType.GROUND)
			return GeoEngine.getInstance().getValidFlyLocation(ox, oy, oz, 32, tx, ty, tz, dummy);
		
		// Calculate the path. If no path or too short, calculate the first valid location.
		final List<Location> path = GeoEngine.getInstance().findPath(ox, oy, oz, tx, ty, tz, true, dummy);
		if (path.size() < 2)
			return GeoEngine.getInstance().getValidLocation(ox, oy, oz, tx, ty, tz, null);
		
		// Draw a debug of this movement if activated.
		if (_isDebugPath)
		{
			// Draw debug packet to all players.
			for (Player p : _actor.getSurroundingGMs())
			{
				// Get debug packet.
				final ExServerPrimitive debug = p.getDebugPacket("PATH" + _actor.getObjectId());
				
				// Reset the packet and add all lines and points.
				debug.reset();
				debug.addAll(dummy);
				
				// Send.
				debug.sendTo(p);
			}
		}
		
		// Feed the geopath with whole path.
		_geoPath.addAll(path);
		
		// Retrieve first Location.
		return _geoPath.poll();
	}
}