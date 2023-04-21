package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.text.SimpleDateFormat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class PremiumStatus implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"premium"
	};

	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.startsWith(VOICED_COMMANDS[0]))
		{
			SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			if (player.getPremiumService() == 0)
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setFile(player.isLang() + "mods/premium/normal.htm");

				htm.replace("%rate_xp%", Config.RATE_XP);
				htm.replace("%rate_sp%", Config.RATE_SP);
				htm.replace("%rate_drop%", Config.RATE_DROP_ITEMS);
				htm.replace("%rate_spoil%", Config.RATE_DROP_SPOIL);
				htm.replace("%rate_currency%", Config.RATE_DROP_CURRENCY);
				htm.replace("%current%", String.valueOf(format.format(System.currentTimeMillis())));
				htm.replace("%prem_rate_xp%", Config.PREMIUM_RATE_XP);
				htm.replace("%prem_rate_sp%", Config.PREMIUM_RATE_SP);
				htm.replace("%prem_rate_drop%", Config.PREMIUM_RATE_DROP_ITEMS);
				htm.replace("%prem_rate_spoil%", Config.PREMIUM_RATE_DROP_SPOIL);
				htm.replace("%prem_currency%", Config.PREMIUM_RATE_DROP_CURRENCY);
				player.sendPacket(htm);
			}
			else
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setFile(player.isLang() + "mods/premium/premium.htm");

				htm.replace("%prem_rate_xp%", Config.PREMIUM_RATE_XP);
				htm.replace("%prem_rate_sp%", Config.PREMIUM_RATE_SP);
				htm.replace("%prem_rate_drop%", Config.PREMIUM_RATE_DROP_ITEMS);
				htm.replace("%prem_rate_spoil%", Config.PREMIUM_RATE_DROP_SPOIL);
				htm.replace("%prem_currency%", Config.PREMIUM_RATE_DROP_CURRENCY);
				htm.replace("%expires%", String.valueOf(format.format(player.getPremServiceData())));
				htm.replace("%current%", String.valueOf(format.format(System.currentTimeMillis())));
				player.sendPacket(htm);
			}
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}