package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author TerryMaster Pro Jr
 *
 */
public class CloseGame implements Runnable{
	private final Player _player;
	private int _time;

	public CloseGame(Player player, int time) {
		_time = time;
		_player = player;
	}

	@Override
	public void run() {
		if (_player.isOnline()) {
			switch (_time) {
			case 60:
			case 120:
			case 180:
			case 240:
			case 300:
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time / 60 + " minute(s) ..");
				break;
			case 20:
			case 30:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 3000));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				break;
			case 15:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 3000));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				break;
			case 10:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 3000));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				break;
			case 5:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 1500));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				break;
			case 4:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 1500));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				break;
			case 3:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 1500));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				break;
			case 2:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 1500));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				break;
			case 1:
				_player.sendPacket(new ExShowScreenMessage("" + _time + " ..", 1500));
				_player.sendChatMessage(0, SayType.CRITICAL_ANNOUNCE, "SYS", "Desconecting in " + _time + " second(s) ..");
				_player.logout(true);
			}
			if (_time > 1) {
				ThreadPool.schedule(new CloseGame(_player, _time - 1), 1000L);
			}
		}
	}
}
