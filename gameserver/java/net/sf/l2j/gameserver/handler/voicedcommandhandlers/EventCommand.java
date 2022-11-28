package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class EventCommand implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"ctfinfo",
		"ctfjoin",
		"ctfleave",
		"dminfo",
		"dmjoin",
		"dmleave",
		"lminfo",
		"lmjoin",
		"lmleave",
		"tvtinfo",
		"tvtjoin",
		"tvtleave"
	};

	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.equals("ctfinfo"))
		{
			if (CTFEvent.isStarting() || CTFEvent.isStarted())
			{
				showCTFStatusPage(player);
				return true;
			}
			player.sendMessage(player.isLangString().equals("en") ? "Capture the Flag event is not in progress." : "Ивент захват флага не запущен.");
			return false;
		}
		else if (command.equals("ctfjoin"))
		{
			if (!CTFEvent.isPlayerParticipant(player.getObjectId()))
			{
				CTFEvent.onBypass("ctf_event_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are already registered." : "Вы уже зарегистрированы.");
				return false;
			}
		}
		else if (command.equals("ctfleave"))
		{
			if (CTFEvent.isPlayerParticipant(player.getObjectId()))
			{
				CTFEvent.onBypass("ctf_event_remove_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are not registered." : "Вы не регистрировались.");
				return false;
			}
		}
		else if (command.equals("dminfo"))
		{
			if (DMEvent.isStarting() || DMEvent.isStarted())
			{
				showDMStatusPage(player);
				return true;
			}
			player.sendMessage(player.isLangString().equals("en") ? "Deathmatch fight is not in progress." : "Ивент дезматч не запущен.");
			return false;
		}
		else if (command.equalsIgnoreCase("dmjoin"))
		{
			if (!DMEvent.isPlayerParticipant(player))
			{
				DMEvent.onBypass("dm_event_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are already registered." : "Вы уже зарегистрированы.");
				return false;
			}
		}
		else if (command.equalsIgnoreCase("dmleave"))
		{
			if (DMEvent.isPlayerParticipant(player))
			{
				DMEvent.onBypass("dm_event_remove_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are not registered." : "Вы не регистрировались.");
				return false;
			}
		}
		else if (command.equals("lminfo"))
		{
			if (LMEvent.isStarting() || LMEvent.isStarted())
			{
				showLMStatusPage(player);
				return true;
			}
			player.sendMessage(player.isLangString().equals("en") ? "Last Man fight is not in progress." : "Ивент последний человек не запущен.");
			return false;
		}
		else if (command.equalsIgnoreCase("lmjoin"))
		{
			if (!LMEvent.isPlayerParticipant(player))
			{
				LMEvent.onBypass("lm_event_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are already registered." : "Вы уже зарегистрированы.");
				return false;
			}
		}
		else if (command.equalsIgnoreCase("lmleave"))
		{
			if (LMEvent.isPlayerParticipant(player))
			{
				LMEvent.onBypass("lm_event_remove_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are not registered." : "Вы не регистрировались.");
				return false;
			}
		}
		else if (command.equals("tvtinfo"))
		{
			if (TvTEvent.isStarting() || TvTEvent.isStarted())
			{
				showTvTStatusPage(player);
				return true;
			}
			player.sendMessage(player.isLangString().equals("en") ? "Team vs Team fight is not in progress." : "Ивент твт не запущен.");
			return false;
		}
		else if (command.equals("tvtjoin"))
		{
			if (!TvTEvent.isPlayerParticipant(player.getObjectId()))
			{
				TvTEvent.onBypass("tvt_event_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are already registered." : "Вы уже зарегистрированы.");
				return false;
			}
		}
		else if (command.equals("tvtleave"))
		{
			if (TvTEvent.isPlayerParticipant(player.getObjectId()))
			{
				TvTEvent.onBypass("tvt_event_remove_participation", player);
			}
			else
			{
				player.sendMessage(player.isLangString().equals("en") ? "You are not registered." : "Вы не регистрировались.");
				return false;
			}
		}
		return true;
	}

	private static void showCTFStatusPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.isLang() + "mods/events/ctf/Status.htm");
		html.replace("%team1name%", Config.CTF_EVENT_TEAM_1_NAME);
		html.replace("%team1playercount%", String.valueOf(CTFEvent.getTeamsPlayerCounts()[0]));
		html.replace("%team1points%", String.valueOf(CTFEvent.getTeamsPoints()[0]));
		html.replace("%team2name%", Config.CTF_EVENT_TEAM_2_NAME);
		html.replace("%team2playercount%", String.valueOf(CTFEvent.getTeamsPlayerCounts()[1]));
		html.replace("%team2points%", String.valueOf(CTFEvent.getTeamsPoints()[1]));
		player.sendPacket(html);
	}

	private static void showDMStatusPage(Player player)
	{
		String[] firstPositions = DMEvent.getFirstPosition(Config.DM_REWARD_FIRST_PLAYERS);
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.isLang() + "mods/events/dm/Status.htm");

		String htmltext = "";
		if (firstPositions != null)
		{
			for (int i = 0; i < firstPositions.length; i++)
			{
				String[] row = firstPositions[i].split("\\,");
				htmltext += "<tr><td>" + row[0] + "</td><td width=\"100\" align=\"center\">" + row[1] + "</td></tr>";
			}
		}

		html.replace("%positions%", htmltext);
		player.sendPacket(html);
	}

	private static void showLMStatusPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.isLang() + "mods/events/lm/Status.htm");
		String htmltext = String.valueOf(LMEvent.getPlayerCounts());
		html.replace("%countplayer%", htmltext);
		player.sendPacket(html);
	}

	private static void showTvTStatusPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.isLang() + "mods/events/tvt/Status.htm");
		html.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
		html.replace("%team1playercount%", String.valueOf(TvTEvent.getTeamsPlayerCounts()[0]));
		html.replace("%team1points%", String.valueOf(TvTEvent.getTeamsPoints()[0]));
		html.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
		html.replace("%team2playercount%", String.valueOf(TvTEvent.getTeamsPlayerCounts()[1]));
		html.replace("%team2points%", String.valueOf(TvTEvent.getTeamsPoints()[1]));
		player.sendPacket(html);
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}