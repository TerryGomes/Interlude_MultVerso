package net.sf.l2j.gameserver.model.entity.events.capturetheflag;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;

public class CTFEventTeleporter implements Runnable
{
	/** The instance of the player to teleport */
	private Player _player = null;

	/** Coordinates of the spot to teleport to */
	private int[] _coordinates = new int[3];

	/** Admin removed this player from event */
	private boolean _adminRemove = false;

	/**
	 * Initialize the teleporter and start the delayed task.
	 * @param player
	 * @param coordinates
	 * @param fastSchedule
	 * @param adminRemove
	 */
	public CTFEventTeleporter(Player player, int[] coordinates, boolean fastSchedule, boolean adminRemove)
	{
		_player = player;
		_coordinates = coordinates;
		_adminRemove = adminRemove;

		long delay = (CTFEvent.isStarted() ? Config.CTF_EVENT_RESPAWN_TELEPORT_DELAY : Config.CTF_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;

		ThreadPool.schedule(this, fastSchedule ? 0 : delay);
	}

	/**
	 * The task method to teleport the player<br>
	 * 1. Unsummon pet if there is one<br>
	 * 2. Remove all effects<br>
	 * 3. Revive and full heal the player<br>
	 * 4. Teleport the player<br>
	 * 5. Broadcast status and user info
	 */
	@Override
	public void run()
	{
		if (_player == null)
		{
			return;
		}

		Summon summon = _player.getSummon();

		if (summon != null)
		{
			summon.unSummon(_player);
		}

		if ((Config.CTF_EVENT_EFFECTS_REMOVAL == 0) || ((Config.CTF_EVENT_EFFECTS_REMOVAL == 1) && ((_player.getTeam() == TeamType.NONE) || (_player.isInDuel() && (_player.getDuelState() != DuelState.INTERRUPTED)))))
		{
			_player.stopAllEffectsExceptThoseThatLastThroughDeath();
		}

		if (_player.isInDuel())
		{
			_player.setDuelState(DuelState.INTERRUPTED);
		}

		_player.doRevive();

		_player.teleportTo((_coordinates[0] + Rnd.get(101)) - 50, (_coordinates[1] + Rnd.get(101)) - 50, _coordinates[2], 0);

		// Reset flag carrier
		if (CTFEvent.playerIsCarrier(_player))
		{
			CTFEvent.removeFlagCarrier(_player);
			CTFEvent.sysMsgToAllParticipants("The " + CTFEvent.getParticipantEnemyTeam(_player.getObjectId()).getName() + " flag has been returned!");
		}

		if (CTFEvent.isStarted() && !_adminRemove)
		{
			int teamId = CTFEvent.getParticipantTeamId(_player.getObjectId()) + 1;
			switch (teamId)
			{
				case 0:
					_player.setTeam(TeamType.NONE);
					break;
				case 1:
					_player.setTeam(TeamType.BLUE);
					break;
				case 2:
					_player.setTeam(TeamType.RED);
					break;
			}
		}
		else
		{
			_player.setTeam(TeamType.NONE);
		}

		_player.getStatus().setCp(_player.getStatus().getMaxCp());
		_player.getStatus().setHp(_player.getStatus().getMaxHp());
		_player.getStatus().setMp(_player.getStatus().getMaxMp());

		_player.getStatus().broadcastStatusUpdate();
		_player.broadcastUserInfo();
	}
}