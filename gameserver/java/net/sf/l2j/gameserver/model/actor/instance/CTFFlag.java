
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class CTFFlag extends Folk
{
	private static final String flagsPath = "mods/events/ctf/flags/";

	public CTFFlag(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		if (player == null)
		{
			return;
		}

		if (CTFEvent.isStarting() || CTFEvent.isStarted())
		{
			final String flag = getTitle();
			final String team = CTFEvent.getParticipantTeam(player.getObjectId()).getName();
			final String enemyteam = CTFEvent.getParticipantEnemyTeam(player.getObjectId()).getName();

			// player talking to friendly flag
			if (flag == team)
			{
				// team flag is missing
				if (CTFEvent.getEnemyCarrier(player) != null)
				{
					final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + flagsPath + "flag_friendly_missing.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%enemyteam%", enemyteam);
					npcHtmlMessage.replace("%team%", team);
					npcHtmlMessage.replace("%player%", player.getName());
					player.sendPacket(npcHtmlMessage);
				}
				// player has returned with enemy flag
				else if (player == CTFEvent.getTeamCarrier(player))
				{
					if (Config.CTF_EVENT_CAPTURE_SKILL > 0)
					{
						player.broadcastPacket(new MagicSkillUse(player, Config.CTF_EVENT_CAPTURE_SKILL, 1, 1, 1));
					}

					CTFEvent.removeFlagCarrier(player);
					CTFEvent.getParticipantTeam(player.getObjectId()).increasePoints();
					CTFEvent.broadcastScreenMessage(player.getName() + " scored for the " + team + " team!", 7);
				}
				// go get the flag
				else
				{
					final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + flagsPath + "flag_friendly.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%enemyteam%", enemyteam);
					npcHtmlMessage.replace("%team%", team);
					npcHtmlMessage.replace("%player%", player.getName());
					player.sendPacket(npcHtmlMessage);
				}
			}
			else // player has flag
			if (CTFEvent.playerIsCarrier(player))
			{
				final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + flagsPath + "flag_enemy.htm");
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%enemyteam%", enemyteam);
				npcHtmlMessage.replace("%team%", team);
				npcHtmlMessage.replace("%player%", player.getName());
				player.sendPacket(npcHtmlMessage);
			}
			// enemy flag is missing
			else if (CTFEvent.getTeamCarrier(player) != null)
			{
				final String htmContent = HtmCache.getInstance().getHtm(player.isLang() + flagsPath + "flag_enemy_missing.htm");
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%enemyteam%", enemyteam);
				npcHtmlMessage.replace("%player%", CTFEvent.getTeamCarrier(player).getName());
				player.sendPacket(npcHtmlMessage);
			}
			// take flag
			else
			{
				if (Config.CTF_EVENT_CAPTURE_SKILL > 0)
				{
					player.broadcastPacket(new MagicSkillUse(player, Config.CTF_EVENT_CAPTURE_SKILL, 1, 1, 1));
				}

				CTFEvent.setCarrierUnequippedWeapons(player, player.getInventory().getItemFrom(Paperdoll.RHAND), player.getInventory().getItemFrom(Paperdoll.LHAND));
				player.getInventory().equipItem(ItemInstance.create(CTFEvent.getEnemyTeamFlagId(player), 1, player, null));
				player.getInventory().blockAllItems();
				player.broadcastUserInfo();
				CTFEvent.setTeamCarrier(player);
				CTFEvent.broadcastScreenMessage(player.getName() + " has taken the " + enemyteam + " flag team!", 5);
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