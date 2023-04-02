package net.sf.l2j.gameserver.model.entity.events.capturetheflag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.model.actor.Player;

public class CTFEventTeam
{
	/**
	 * The name of the team<br>
	 */
	private final String _name;

	/**
	 * The team spot coordinated<br>
	 */
	private int[] _coordinates = new int[3];

	/**
	 * The points of the team<br>
	 */
	private short _points;

	/** Name and instance of all participated players in map. */
	private final Map<Integer, Player> _participatedPlayers = new ConcurrentHashMap<>();

	/**
	 * C'tor initialize the team<br>
	 * <br>
	 * @param name as String<br>
	 * @param coordinates as int[]<br>
	 */
	public CTFEventTeam(String name, int[] coordinates)
	{
		_name = name;
		_coordinates = coordinates;
		_points = 0;
	}

	/**
	 * Adds a player to the team<br>
	 * <br>
	 * @param player as Player<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public boolean addPlayer(Player player)
	{
		if (player == null)
		{
			return false;
		}

		synchronized (_participatedPlayers)
		{
			_participatedPlayers.put(player.getObjectId(), player);
		}

		return true;
	}

	/**
	 * Removes a player from the team
	 * @param objectId
	 */
	public void removePlayer(int objectId)
	{
		synchronized (_participatedPlayers)
		{
			_participatedPlayers.remove(objectId);
		}
	}

	/**
	 * Increases the points of the team<br>
	 */
	public void increasePoints()
	{
		++_points;
	}

	/**
	 * Cleanup the team and make it ready for adding players again<br>
	 */
	public void cleanMe()
	{
		_participatedPlayers.clear();
		_points = 0;
	}

	/**
	 * Is given player in this team?
	 * @param objectId
	 * @return boolean: true if player is in this team, otherwise false
	 */
	public boolean containsPlayer(int objectId)
	{
		boolean containsPlayer;

		synchronized (_participatedPlayers)
		{
			containsPlayer = _participatedPlayers.containsKey(objectId);
		}

		return containsPlayer;
	}

	/**
	 * Returns the name of the team<br>
	 * <br>
	 * @return String: name of the team<br>
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * Returns the coordinates of the team spot<br>
	 * <br>
	 * @return int[]: team coordinates<br>
	 */
	public int[] getCoordinates()
	{
		return _coordinates;
	}

	/**
	 * Returns the points of the team<br>
	 * <br>
	 * @return short: team points<br>
	 */
	public short getPoints()
	{
		return _points;
	}

	/**
	 * Returns name and instance of all participated players in FastMap<br>
	 * <br>
	 * @return Map<String, Player>: map of players in this team<br>
	 */
	public Map<Integer, Player> getParticipatedPlayers()
	{
		Map<Integer, Player> participatedPlayers = null;

		synchronized (_participatedPlayers)
		{
			participatedPlayers = _participatedPlayers;
		}

		return participatedPlayers;
	}

	/**
	 * Returns player count of this team<br>
	 * <br>
	 * @return int: number of players in team<br>
	 */
	public int getParticipatedPlayerCount()
	{
		int participatedPlayerCount;

		synchronized (_participatedPlayers)
		{
			participatedPlayerCount = _participatedPlayers.size();
		}

		return participatedPlayerCount;
	}
}