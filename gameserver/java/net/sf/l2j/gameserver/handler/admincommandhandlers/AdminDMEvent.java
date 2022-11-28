package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEventTeleporter;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMManager;

public class AdminDMEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_dm_add",
		"admin_dm_remove",
		"admin_dm_advance"
	};

	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_dm_add"))
		{
			WorldObject target = player.getTarget();

			if (!(target instanceof Player))
			{
				player.sendMessage("You should select a player!");
				return;
			}

			add(player, (Player) target);
		}
		else if (command.startsWith("admin_dm_remove"))
		{
			WorldObject target = player.getTarget();

			if (!(target instanceof Player))
			{
				player.sendMessage("You should select a player!");
				return;
			}

			remove(player, (Player) target);
		}
		else if (command.startsWith("admin_dm_advance"))
		{
			DMManager.getInstance().skipDelay();
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
		if (DMEvent.isPlayerParticipant(player.getObjectId()))
		{
			activeChar.sendMessage("Player already participated in the event!");
			return;
		}

		if (!DMEvent.addParticipant(player))
		{
			activeChar.sendMessage("Player instance could not be added, it seems to be null!");
			return;
		}

		if (DMEvent.isStarted())
		{
			new DMEventTeleporter(player, true, false);
		}
	}

	private static void remove(Player activeChar, Player player)
	{
		if (!DMEvent.removeParticipant(player))
		{
			activeChar.sendMessage("Player is not part of the event!");
			return;
		}

		new DMEventTeleporter(player, Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
}