package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEventTeleporter;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTManager;

public class AdminTvTEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_tvt_add",
		"admin_tvt_remove",
		"admin_tvt_advance"
	};

	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_tvt_add"))
		{
			WorldObject target = player.getTarget();

			if (!(target instanceof Player))
			{
				player.sendMessage("You should select a player!");
				return;
			}

			add(player, (Player) target);
		}
		else if (command.startsWith("admin_tvt_remove"))
		{
			WorldObject target = player.getTarget();

			if (!(target instanceof Player))
			{
				player.sendMessage("You should select a player!");
				return;
			}

			remove(player, (Player) target);
		}
		else if (command.startsWith("admin_tvt_advance"))
		{
			TvTManager.getInstance().skipDelay();
		}

		return;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private static void add(Player activeChar, Player player)
	{
		if (TvTEvent.isPlayerParticipant(player.getObjectId()))
		{
			activeChar.sendMessage("Player already participated in the event!");
			return;
		}

		if (!TvTEvent.addParticipant(player))
		{
			activeChar.sendMessage("Player instance could not be added, it seems to be null!");
			return;
		}

		if (TvTEvent.isStarted())
		{
			new TvTEventTeleporter(player, TvTEvent.getParticipantTeamCoordinates(player.getObjectId()), true, false);
		}
	}

	private static void remove(Player activeChar, Player player)
	{
		if (!TvTEvent.removeParticipant(player.getObjectId()))
		{
			activeChar.sendMessage("Player is not part of the event!");
			return;
		}

		new TvTEventTeleporter(player, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
}