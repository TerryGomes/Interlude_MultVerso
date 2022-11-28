package net.sf.l2j.gameserver.model.entity.events.teamvsteam;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.Player;

public class TvTEventTeam
{
	/** The name of the team */
	private final String _name;

	/** The team spot coordinated */
	private int[] _coordinates = new int[3];

	/** The points of the team */
	private short _points;

	/** Name and instance of all participated players in FastMap */
	private Map<Integer, Player> _participatedPlayers = new HashMap<>();

	/** Points of the event participants. */
	private Map<Integer, Integer> _pointPlayers = new HashMap<>();

	/**
	 * C'tor initialize the team
	 * @param name as String
	 * @param coordinates as int[]
	 */
	public TvTEventTeam(String name, int[] coordinates)
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
	 * Removes a player from the team<br>
	 * <br>
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
	 * Increases the points of the player<br>
	 * @param charId
	 */
	public void increasePoints(int charId)
	{
		synchronized (_pointPlayers)
		{
			if (_pointPlayers.containsKey(charId))
			{
				_pointPlayers.put(charId, _pointPlayers.get(charId) + 1);
			}
			else
			{
				_pointPlayers.put(charId, 1);
			}
		}
	}

	/**
	 * Cleanup the team and make it ready for adding players again
	 */
	public void cleanMe()
	{
		_participatedPlayers.clear();
		_participatedPlayers = new HashMap<>();
		_pointPlayers.clear();
		_pointPlayers = new HashMap<>();
		_points = 0;
	}

	/**
	 * Is given player in this team?<br>
	 * <br>
	 * @param objectId
	 * @return boolean: true if player is in this team, otherwise false<br>
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

	/**
	 * Returns name and instance of all participated players who scored in FastMap<br>
	 * <br>
	 * @return Map<String, Integer>: map of players who scored.<br>
	 */
	public Map<Integer, Integer> getScoredPlayers()
	{
		Map<Integer, Integer> scoredPlayers = null;

		synchronized (_pointPlayers)
		{
			scoredPlayers = _pointPlayers;
		}

		return scoredPlayers;
	}

	/**
	 * Returns player count of this team who scored.<br>
	 * <br>
	 * @return int: number of players in team who scored.<br>
	 */
	public int getScoredPlayerCount()
	{
		int scoredPlayerCount;

		synchronized (_pointPlayers)
		{
			scoredPlayerCount = _pointPlayers.size();
		}

		return scoredPlayerCount;
	}

	public boolean onScoredPlayer(int charId)
	{
		synchronized (_pointPlayers)
		{
			if (_pointPlayers.containsKey(charId))
			{
				return (_pointPlayers.get(charId) > 0);
			}
			else
			{
				return false;
			}
		}
	}
}