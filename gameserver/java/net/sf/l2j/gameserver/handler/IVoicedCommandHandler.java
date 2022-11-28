package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.Player;

public interface IVoicedCommandHandler
{
	public boolean useVoicedCommand(String command, Player player, String params);

	public String[] getVoicedCommandList();
}
