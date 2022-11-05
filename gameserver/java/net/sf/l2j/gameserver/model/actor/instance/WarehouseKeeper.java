package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.itemcontainer.PcFreight;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.PackageToList;
import net.sf.l2j.gameserver.network.serverpackets.WarehouseDepositList;
import net.sf.l2j.gameserver.network.serverpackets.WarehouseWithdrawList;

/**
 * An instance type extending {@link Folk}, used by warehouse keepers.<br>
 * <br>
 * A warehouse keeper stores {@link Player} items in a personal container.
 */
public class WarehouseKeeper extends Folk
{
	public WarehouseKeeper(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean isWarehouse()
	{
		return true;
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/warehouse/" + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		// Generic PK check. Send back the HTM if found and cancel current action.
		if (!Config.KARMA_PLAYER_CAN_USE_WH && player.getKarma() > 0 && showPkDenyChatWindow(player, "warehouse"))
			return;
		
		if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}
		
		if (player.getActiveEnchantItem() != null)
		{
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}
		
		if (command.startsWith("WithdrawP"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(player.getWarehouse());
			
			if (player.getActiveWarehouse().getSize() == 0)
			{
				player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
				return;
			}
			
			player.sendPacket(new WarehouseWithdrawList(player, WarehouseWithdrawList.PRIVATE));
		}
		else if (command.equals("DepositP"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(player.getWarehouse());
			player.tempInventoryDisable();
			player.sendPacket(new WarehouseDepositList(player, WarehouseDepositList.PRIVATE));
		}
		else if (command.equals("WithdrawC"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			if (!player.hasClanPrivileges(Clan.CP_CL_VIEW_WAREHOUSE))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
				return;
			}
			
			final Clan clan = player.getClan();
			if (clan == null)
				return;
			
			if (clan.getLevel() == 0)
			{
				player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
				return;
			}
			
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.sendPacket(new WarehouseWithdrawList(player, WarehouseWithdrawList.CLAN));
		}
		else if (command.equals("DepositC"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			final Clan clan = player.getClan();
			if (clan == null)
				return;
			
			if (clan.getLevel() == 0)
			{
				player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
				return;
			}
			
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.tempInventoryDisable();
			player.sendPacket(new WarehouseDepositList(player, WarehouseDepositList.CLAN));
		}
		else if (command.startsWith("WithdrawF"))
		{
			if (!Config.ALLOW_FREIGHT)
				return;
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			final PcFreight freight = player.getFreight();
			if (freight == null || freight.getSize() <= 0)
			{
				player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
				return;
			}
			
			freight.setActiveLocation((Config.REGION_BASED_FREIGHT) ? getRegion().hashCode() : 0);
			
			player.setActiveWarehouse(freight);
			player.sendPacket(new WarehouseWithdrawList(player, WarehouseWithdrawList.FREIGHT));
		}
		else if (command.startsWith("DepositF"))
		{
			if (!Config.ALLOW_FREIGHT)
				return;
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			// No other chars in the account of this Player.
			final Map<Integer, String> chars = player.getAccountChars();
			if (chars.isEmpty())
			{
				player.sendPacket(SystemMessageId.CHARACTER_DOES_NOT_EXIST);
				return;
			}
			
			// One or more chars other than this Player for this account.
			if (chars.size() < 1)
				return;
			
			player.sendPacket(new PackageToList(chars));
		}
		else if (command.startsWith("FreightChar"))
		{
			if (!Config.ALLOW_FREIGHT)
				return;
			
			final String id = command.substring(command.lastIndexOf("_") + 1);
			final PcFreight freight = player.getDepositedFreight(Integer.parseInt(id));
			freight.setActiveLocation((Config.REGION_BASED_FREIGHT) ? getRegion().hashCode() : 0);
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(freight);
			player.tempInventoryDisable();
			player.sendPacket(new WarehouseDepositList(player, WarehouseDepositList.FREIGHT));
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		// Generic PK check. Send back the HTM if found and cancel current action.
		if (!Config.KARMA_PLAYER_CAN_USE_WH && player.getKarma() > 0 && showPkDenyChatWindow(player, "warehouse"))
			return;
		
		showChatWindow(player, getHtmlPath(getNpcId(), val));
	}
}