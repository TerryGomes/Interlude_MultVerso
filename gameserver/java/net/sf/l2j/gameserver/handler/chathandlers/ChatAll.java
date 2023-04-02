package net.sf.l2j.gameserver.handler.chathandlers;

import java.util.Optional;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatAll implements IChatHandler
{
	private static final SayType[] COMMAND_IDS =
	{
		SayType.ALL
	};

	@Override
	public void handleChat(SayType type, Player player, String target, String text)
	{
		if (!player.getClient().performAction(FloodProtector.GLOBAL_CHAT))
		{
			return;
		}

		boolean useHandler = false;
		if (text.startsWith("."))
		{
			final StringTokenizer st = new StringTokenizer(text);
			final IVoicedCommandHandler vch;
			String command = "";

			if (st.countTokens() > 1)
			{
				command = st.nextToken().substring(1);
				target = text.substring(command.length() + 2);
				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}
			else
			{
				command = text.substring(1);
				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}

			if (vch != null)
			{
				vch.useVoicedCommand(command, player, target);
				useHandler = true;
			}
		}

		if (!useHandler)
		{
			CreatureSay cs = new CreatureSay(player.getObjectId(), type, player.getName(), text);

			for (Player knownPlayer : player.getKnownTypeInRadius(Player.class, 1250))
			{
				if (!knownPlayer.getBlockList().isBlockingAll())
				{
					knownPlayer.sendPacket(cs);
				}
			}
			player.sendPacket(cs);
		}
		if (text.startsWith("."))
		{
			Optional.ofNullable(VoicedCommandHandler.getInstance().getVoicedCommand(text.substring(1).toLowerCase())).ifPresent(s -> s.useVoicedCommand(text, player, null));
			return;
		}
		
	}

	@Override
	public SayType[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}