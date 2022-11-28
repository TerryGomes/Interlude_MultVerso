package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEventTeleporter;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFManager;

public class AdminCTFEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ctf_add",
		"admin_ctf_remove",
		"admin_ctf_advance"
	};

	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_ctf_add"))
		{
			WorldObject target = player.getTarget();

			if (!(target instanceof Player))
			{
				player.sendMessage("You should select a player!");
				return;
			}

			add(player, (Player) target);
		}
		else if (command.startsWith("admin_ctf_remove"))
		{
			WorldObject target = player.getTarget();

			if (!(target instanceof Player))
			{
				player.sendMessage("You should select a player!");
				return;
			}

			remove(player, (Player) target);
		}
		else if (command.startsWith("admin_ctf_advance"))
		{
			CTFManager.getInstance().skipDelay();
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
		if (CTFEvent.isPlayerParticipant(player.getObjectId()))
		{
			activeChar.sendMessage("Player already participated in the event!");
			return;
		}

		if (!CTFEvent.addParticipant(player))
		{
			activeChar.sendMessage("Player instance could not be added, it seems to be null!");
			return;
		}

		if (CTFEvent.isStarted())
		{
			new CTFEventTeleporter(player, CTFEvent.getParticipantTeamCoordinates(player.getObjectId()), true, false);
		}
	}

	private static void remove(Player activeChar, Player player)
	{
		if (!CTFEvent.removeParticipant(player.getObjectId()))
		{
			activeChar.sendMessage("Player is not part of the event!");
			return;
		}

		new CTFEventTeleporter(player, Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
}