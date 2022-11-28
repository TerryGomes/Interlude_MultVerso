package net.sf.l2j.gameserver.model.entity.events.lastman;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;

public class LMPlayer
{
	private Player _player;
	private short _points;
	private short _credits;
	private String _hexCode;

	/**
	 * @param player
	 * @param hexCode
	 */
	public LMPlayer(Player player, String hexCode)
	{
		_player = player;
		_points = 0;
		_credits = Config.LM_EVENT_PLAYER_CREDITS;
		_hexCode = hexCode;
	}

	/**
	 * @return the _player
	 */
	public Player getPlayer()
	{
		return _player;
	}

	/**
	 * @param player the _player to set
	 */
	public void setPlayer(Player player)
	{
		_player = player;
	}

	/**
	 * @return the _credits
	 */
	public short getCredits()
	{
		return _credits;
	}

	/**
	 * @param credits the _credits to set
	 */
	public void setCredits(short credits)
	{
		_credits = credits;
	}

	/**
	 * Decreases the credits of the player<br>
	 */
	public void decreaseCredits()
	{
		--_credits;
	}

	/**
	 * @return the _points
	 */
	public short getPoints()
	{
		return _points;
	}

	/**
	 * @param points the _points to set
	 */
	public void setPoints(short points)
	{
		_points = points;
	}

	/**
	 * Decreases the credits of the player<br>
	 */
	public void increasePoints()
	{
		++_points;
	}

	/**
	 * @return the _hexCode
	 */
	public String getHexCode()
	{
		return _hexCode;
	}
}