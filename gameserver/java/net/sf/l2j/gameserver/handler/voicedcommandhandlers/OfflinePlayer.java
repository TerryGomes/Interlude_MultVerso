package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.sql.OfflineTradersTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.trade.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.util.CustomMessage;

public class OfflinePlayer implements IVoicedCommandHandler // TODO: Rewrite this.
{
	private static final String[] _voicedCommands =
	{
		"offline"
	};

	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (player == null)
		{
			return false;
		}

		if ((!player.isInStoreMode() && (!player.isCrafting())) || !player.isSitting())
		{
			player.sendMessage(new CustomMessage("OFFLINE_NOT_RUNNING"));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		final TradeList storeListBuy = player.getBuyList();
		if (storeListBuy == null)
		{
			player.sendMessage(new CustomMessage("OFFLINE_BUY_LIST_EMPTY"));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		final TradeList storeListSell = player.getSellList();
		if (storeListSell == null)
		{
			player.sendMessage(new CustomMessage("OFFLINE_SELL_LIST_EMPTY"));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		player.getInventory().updateDatabase();

		if (AttackStanceTaskManager.getInstance().isInAttackStance(player))
		{
			player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Dont allow leaving if player is in combat
		if (player.isInCombat() && !player.isGM())
		{
			player.sendMessage(new CustomMessage("OFFLINE_COMBAT_MODE"));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Dont allow leaving if player is teleporting
		if (player.isTeleporting() && !player.isGM())
		{
			player.sendMessage(new CustomMessage("OFFLINE_TELEPORT"));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage(new CustomMessage("OFFLINE_OLYMPIAD_MODE"));
			return false;
		}

		// Prevent player from logging out if they are a festival participant nd it is in progress, otherwise notify party members that the player is not longer a participant.
		if (player.isFestivalParticipant())
		{
			if (FestivalOfDarknessManager.getInstance().isFestivalInitialized())
			{
				player.sendMessage(new CustomMessage("OFFLINE_FESTIVAL"));
				return false;
			}

			Party playerParty = player.getParty();
			if (playerParty != null)
			{
				player.getParty().broadcastToPartyMembers(player, SystemMessage.sendString(player.getName() + new CustomMessage("OFFLINE_REMOVED_FESTIVAL")));
			}
		}

		if (!OfflineTradersTable.offlineMode(player))
		{
			player.sendMessage(new CustomMessage("OFFLINE_LOGOUT"));
			return false;
		}

		if (player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE || player.isCrafting() && Config.OFFLINE_CRAFT_ENABLE)
		{
			player.logout(false);
			return true;
		}

		OfflineTradersTable.saveOfflineTraders(player);
		return false;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}