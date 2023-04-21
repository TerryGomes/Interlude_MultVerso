package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class EventManager extends Npc
{
	private static final String ctfhtmlPath = "mods/events/ctf/";
	private static final String TvthtmlPath = "mods/events/tvt/";
	private static final String dmhtmlPath = "mods/events/dm/";
	private static final String lmhtmlPath = "mods/events/lm/";

	public EventManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player playerInstance, String command)
	{
		CTFEvent.onBypass(command, playerInstance);
		TvTEvent.onBypass(command, playerInstance);
		DMEvent.onBypass(command, playerInstance);
		LMEvent.onBypass(command, playerInstance);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		if (player == null)
		{
			return;
		}

		if (TvTEvent.isParticipating())
		{
			final boolean isParticipant = TvTEvent.isPlayerParticipant(player.getObjectId());
			final String htmContent;

			if (!isParticipant)
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + TvthtmlPath + "Participation.htm");
			}
			else
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + TvthtmlPath + "RemoveParticipation.htm");
			}

			if (htmContent != null)
			{
				int[] teamsPlayerCounts = TvTEvent.getTeamsPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%playercount%", String.valueOf(teamsPlayerCounts[0] + teamsPlayerCounts[1]));
				if (!isParticipant)
				{
					npcHtmlMessage.replace("%fee%", TvTEvent.getParticipationFee());
				}

				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (TvTEvent.isStarting() || TvTEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + TvthtmlPath + "Status.htm");

			if (htmContent != null)
			{
				int[] teamsPlayerCounts = TvTEvent.getTeamsPlayerCounts();
				int[] teamsPointsCounts = TvTEvent.getTeamsPoints();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team1points%", String.valueOf(teamsPointsCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%team2points%", String.valueOf(teamsPointsCounts[1])); // <---- array index from 0 to 1 thx DaRkRaGe
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (DMEvent.isParticipating())
		{
			final boolean isParticipant = DMEvent.isPlayerParticipant(player.getObjectId());
			final String htmContent;

			if (!isParticipant)
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + dmhtmlPath + "Participation.htm");
			}
			else
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + dmhtmlPath + "RemoveParticipation.htm");
			}

			if (htmContent != null)
			{
				int PlayerCounts = DMEvent.getPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(PlayerCounts));
				if (!isParticipant)
				{
					npcHtmlMessage.replace("%fee%", DMEvent.getParticipationFee());
				}

				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (DMEvent.isStarting() || DMEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + dmhtmlPath + "Status.htm");

			if (htmContent != null)
			{
				String[] firstPositions = DMEvent.getFirstPosition(Config.DM_REWARD_FIRST_PLAYERS);
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				String htmltext = "";
				if (firstPositions != null)
				{
					for (int i = 0; i < firstPositions.length; i++)
					{
						String[] row = firstPositions[i].split("\\,");
						htmltext += "<tr><td></td><td>" + row[0] + "</td><td align=\"center\">" + row[1] + "</td></tr>";
					}
				}

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%positions%", htmltext);
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (LMEvent.isParticipating())
		{
			final boolean isParticipant = LMEvent.isPlayerParticipant(player.getObjectId());
			final String htmContent;

			if (!isParticipant)
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + lmhtmlPath + "Participation.htm");
			}
			else
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + lmhtmlPath + "RemoveParticipation.htm");
			}

			if (htmContent != null)
			{
				int PlayerCounts = LMEvent.getPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(PlayerCounts));
				if (!isParticipant)
				{
					npcHtmlMessage.replace("%fee%", LMEvent.getParticipationFee());
				}

				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (LMEvent.isStarting() || LMEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + lmhtmlPath + "Status.htm");

			if (htmContent != null)
			{
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				String htmltext = "";
				htmltext = String.valueOf(LMEvent.getPlayerCounts());
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%countplayer%", htmltext);
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (CTFEvent.isParticipating())
		{
			final boolean isParticipant = CTFEvent.isPlayerParticipant(player.getObjectId());
			final String htmContent;

			if (!isParticipant)
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + ctfhtmlPath + "Participation.htm");
			}
			else
			{
				htmContent = HtmCache.getInstance().getHtm(player.isLang() + ctfhtmlPath + "RemoveParticipation.htm");
			}

			if (htmContent != null)
			{
				int[] teamsPlayerCounts = CTFEvent.getTeamsPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.CTF_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.CTF_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%playercount%", String.valueOf(teamsPlayerCounts[0] + teamsPlayerCounts[1]));
				if (!isParticipant)
				{
					npcHtmlMessage.replace("%fee%", CTFEvent.getParticipationFee());
				}

				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (CTFEvent.isStarting() || CTFEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + ctfhtmlPath + "Status.htm");

			if (htmContent != null)
			{
				int[] teamsPlayerCounts = CTFEvent.getTeamsPlayerCounts();
				int[] teamsPointsCounts = CTFEvent.getTeamsPoints();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%team1name%", Config.CTF_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team1points%", String.valueOf(teamsPointsCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.CTF_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%team2points%", String.valueOf(teamsPointsCounts[1])); // <---- array index from 0 to 1 thx DaRkRaGe
				player.sendPacket(npcHtmlMessage);
			}
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public boolean isInvul()
	{
		return true;
	}
}