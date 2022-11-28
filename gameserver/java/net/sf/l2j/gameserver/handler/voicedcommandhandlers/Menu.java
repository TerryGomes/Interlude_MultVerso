package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.CustomMessage;

public class Menu implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"menu",
		"mod_menu_"
	};

	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.equals("menu") && Config.ENABLE_MENU)
		{
			showHtm(player);
		}
		else if (command.startsWith("mod_menu_"))
		{
			String addcmd = command.substring(9).trim();
			if (addcmd.startsWith("exp"))
			{
				if (player.isStopExp())
				{
					player.setStopExp(false);
					player.sendMessage(new CustomMessage("EXP_OFF"));
				}
				else
				{
					player.setStopExp(true);
					player.sendMessage(new CustomMessage("EXP_ON"));
				}
				showHtm(player);
				return true;
			}
			else if (addcmd.startsWith("trade"))
			{
				if (player.isTradeRefusal())
				{
					player.setTradeRefusal(false);
					player.sendMessage(new CustomMessage("TRADE_OFF"));
				}
				else
				{
					player.setTradeRefusal(true);
					player.sendMessage(new CustomMessage("TRADE_ON"));
				}
				showHtm(player);
				return true;
			}
			else if (addcmd.startsWith("autoloot"))
			{
				if (player.isAutoLoot())
				{
					player.setAutoLoot(false);
					player.sendMessage(new CustomMessage("AUTO_LOOT_OFF"));
				}
				else
				{
					player.setAutoLoot(true);
					player.sendMessage(new CustomMessage("AUTO_LOOT_ON"));
				}

				showHtm(player);
				return true;
			}
			else if (addcmd.startsWith("lang_"))
			{
				player.setLang(addcmd.substring(5).trim());
				showHtm(player);
				return true;
			}
		}
		return true;
	}

	private static void showHtm(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.isLang() + "mods/menu/menu.htm");

		final String ACTIVED = player.isLangString().equals("en") ? "<font color=00FF00>ON</font>" : "<font color=00FF00>ВКЛ</font>";
		final String DESAСTIVED = player.isLangString().equals("en") ? "<font color=FF0000>OFF</font>" : "<font color=FF0000>ВЫКЛ</font>";

		htm.replace("%online%", World.getInstance().getPlayers().size() * Config.FAKE_ONLINE_AMOUNT);
		htm.replace("%gainexp%", player.isStopExp() ? ACTIVED : DESAСTIVED);
		htm.replace("%trade%", player.isTradeRefusal() ? ACTIVED : DESAСTIVED);
		htm.replace("%autoloot%", player.isAutoLoot() ? ACTIVED : DESAСTIVED);

		player.sendPacket(htm);
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}