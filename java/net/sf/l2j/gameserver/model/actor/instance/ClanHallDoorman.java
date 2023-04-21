package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * An instance type extending {@link Doorman}, used by clan hall doorman.<br>
 * <br>
 * isOwnerClan() checks if the user is part of clan owning the clan hall.
 */
public class ClanHallDoorman extends Doorman
{
	public ClanHallDoorman(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player.getCurrentFolk() == null || player.getCurrentFolk().getObjectId() != getObjectId())
		{
			return;
		}

		if (command.startsWith("RideWyvern"))
		{
			if (!player.isClanLeader())
			{
				sendHtm(player, "2");
				return;
			}

			// Verify if Dusk owns the Seal of Strife (if true, CLs can't mount Wyvern).
			if (SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) == CabalType.DUSK)
			{
				sendHtm(player, "3");
				return;
			}

			// Check if the Player is mounted on a Strider.
			if (!player.isMounted() || (player.getMountNpcId() != 12526 && player.getMountNpcId() != 12527 && player.getMountNpcId() != 12528))
			{
				player.sendPacket(SystemMessageId.YOU_MAY_ONLY_RIDE_WYVERN_WHILE_RIDING_STRIDER);
				sendHtm(player, "1");
				return;
			}

			// Check for strider level.
			if (player.getMountLevel() < Config.WYVERN_REQUIRED_LEVEL)
			{
				sendHtm(player, "6");
				return;
			}

			// Check for items consumption.
			if (!player.destroyItemByItemId("Wyvern", 1460, Config.WYVERN_REQUIRED_CRYSTALS, player, true))
			{
				sendHtm(player, "5");
				return;
			}

			// Dismount the Strider.
			player.dismount();

			// Mount a Wyvern. If successful, call an HTM.
			if (player.mount(12621, 0))
			{
				sendHtm(player, "4");
			}
		}
		else if (command.startsWith("Chat"))
		{
			String val = "1"; // Default send you to error HTM.
			try
			{
				val = command.substring(5);
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}

			sendHtm(player, val);
		}
		else if (command.startsWith("open_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
				{
					cannotManageDoors(player);
					player.sendPacket(SystemMessageId.GATES_NOT_OPENED_CLOSED_DURING_SIEGE);
				}
				else
				{
					openDoors(player, command);
				}
			}
		}
		else if (command.startsWith("close_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
				{
					cannotManageDoors(player);
					player.sendPacket(SystemMessageId.GATES_NOT_OPENED_CLOSED_DURING_SIEGE);
				}
				else
				{
					closeDoors(player, command);
				}
			}
		}
	}

	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		if (getClanHall() == null)
		{
			return;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		final Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
		if (isOwnerClan(player))
		{
			if (player.isClanLeader() && getClanHall().getId() == 36 || getClanHall().getId() == 37 || getClanHall().getId() == 38 || getClanHall().getId() == 39 || getClanHall().getId() == 40 || getClanHall().getId() == 41)
			{ // TODO: rewrite
				html.setFile(player.isLang() + "clanHallDoormen/doormen-owner.htm");
			}
			else
			{ // TODO: rewrite
				html.setFile(player.isLang() + "clanHallDoormen/doormen.htm");
			}

			html.replace("%clanname%", owner.getName());
		}
		else if (owner != null && owner.getLeader() != null)
		{
			html.setFile(player.isLang() + "clanHallDoormen/doormen-no.htm");
			html.replace("%leadername%", owner.getLeaderName());
			html.replace("%clanname%", owner.getName());
		}
		else
		{
			html.setFile(player.isLang() + "clanHallDoormen/emptyowner.htm");
			html.replace("%hallname%", getClanHall().getName());
		}
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		showChatWindow(player);
	}

	@Override
	protected final void openDoors(Player player, String command)
	{
		getClanHall().openCloseDoors(true);

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.isLang() + "clanHallDoormen/doormen-opened.htm");
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	@Override
	protected final void closeDoors(Player player, String command)
	{
		getClanHall().openCloseDoors(false);

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.isLang() + "clanHallDoormen/doormen-closed.htm");
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	@Override
	protected final boolean isOwnerClan(Player player)
	{
		return getClanHall() != null && player.getClan() != null && player.getClanId() == getClanHall().getOwnerId();
	}

	private void sendHtm(Player player, String val)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.isLang() + "wyvernmanager/wyvernmanager-" + val + ".htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%npcname%", getName());
		html.replace("%wyvern_level%", Config.WYVERN_REQUIRED_LEVEL);
		html.replace("%needed_crystals%", Config.WYVERN_REQUIRED_CRYSTALS);
		player.sendPacket(html);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}