package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.skills.L2Skill;

public final class Service extends Merchant
{
	private static final String UPDATE_PREMIUMSERVICE = "REPLACE INTO account_premium (premium_service,enddate,account_name) values(?,?,?)";

	public Service(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(Player player, int npcId, int val)
	{
		String htmlName = val == 0 ? "" + npcId : "" + npcId + "-" + val;
		return String.format(player.isLang() + "mods/donate/%s.htm", htmlName);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		switch (actualCommand.toLowerCase())
		{
			case ("noble"):
				int nobleItemId = Integer.parseInt(st.nextToken());
				int nobleItemCount = Integer.parseInt(st.nextToken());

				if (player.getClassId().getLevel() < 3)
				{
					player.sendMessage("You have to get a 3rd profession.");
					return;
				}

				if (player.isNoble())
				{
					player.sendMessage("You already have the status of a Noblesse.");
					return;
				}

				if (!player.destroyItemByItemId("ServiceShop", nobleItemId, nobleItemCount, player, true))
				{
					return;
				}

				player.setNoble(true, true);
				player.broadcastUserInfo();
				break;

			case ("hero"):
				int heroDays = Integer.parseInt(st.nextToken());
				int heroItemId = Integer.parseInt(st.nextToken());
				int heroItemCount = Integer.parseInt(st.nextToken());

				if (player.isHero())
				{
					player.sendMessage("You are already a hero.");
					return;
				}

				if (player.getInventory().getItemByItemId(heroItemId) == null || player.getInventory().getItemByItemId(heroItemId).getCount() < heroItemCount)
				{
					player.sendMessage("Incorrect item count. You need " + heroItemCount);
					return;
				}

				player.destroyItemByItemId("donation shop", heroItemId, heroItemCount, this, true);
				player.sendPacket(new ItemList(player, false));

				player.setHero(true);
				player.setHeroUntil(System.currentTimeMillis() + (1000 * 60 * 60 * 24 * heroDays));
				player.store();
				player.sendMessage("You are now a Hero for the next " + heroDays + " days.");
				player.broadcastUserInfo();
				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						if (player.isOnline() && player.isHero())
						{
							player.setHero(false);
							player.setHeroUntil(0);
							player.store();
							player.broadcastUserInfo();
							player.sendMessage("Your hero status has expired.");
						}
					}
				}, player.getHeroUntil() - System.currentTimeMillis());
				break;

			case ("multisell"):
				if (st.countTokens() < 1)
				{
					return;
				}

				MultisellData.getInstance().separateAndSend(st.nextToken(), player, this, false);
				break;

			case ("setnamecolor"):
				int nameColorId = Integer.parseInt(st.nextToken());
				int nameColorCount = Integer.parseInt(st.nextToken());
				int colorName = Integer.decode("0x" + st.nextToken());

				if (!player.destroyItemByItemId("ServiceShop", nameColorId, nameColorCount, player, true))
				{
					return;
				}

				player.getAppearance().setNameColor(colorName);
				player.setNameColor(colorName);
				player.broadcastUserInfo();
				player.store();
				player.sendMessage("The color of the nickname has been successfully changed.");
				break;

			case ("settitlecolor"):
				int titleColorId = Integer.parseInt(st.nextToken());
				int titleColorCount = Integer.parseInt(st.nextToken());
				int colorTitle = Integer.decode("0x" + st.nextToken());

				if (!player.destroyItemByItemId("ServiceShop", titleColorId, titleColorCount, player, true))
				{
					return;
				}

				player.getAppearance().setTitleColor(colorTitle);
				player.setTitleColor(colorTitle);
				player.broadcastUserInfo();
				player.store();
				player.sendMessage("The color of the title has been successfully changed.");
				break;

			case ("setname"):
				if (st.countTokens() < 1)
				{
					return;
				}

				String nick = st.nextToken();
				if (nick.length() < 1 || nick.length() > 16 || !isValidNick(nick) || Config.LIST_RESTRICTED_CHAR_NAMES.contains(nick.toLowerCase()))
				{
					player.sendMessage("You entered an incorrect nickname.");
					return;
				}

				if (PlayerInfoTable.getInstance().getPlayerObjectId(nick) > 0)
				{
					player.sendMessage("This name is taken.");
					return;
				}

				int nameItemId = Integer.parseInt(st.nextToken());
				int nameItemCount = Integer.parseInt(st.nextToken());

				if (!player.destroyItemByItemId("NameChange", nameItemId, nameItemCount, player, true))
				{
					return;
				}

				player.setName(nick);
				PlayerInfoTable.getInstance().updatePlayerData(player, false);

				player.store();
				player.broadcastUserInfo();

				if (player.getClan() != null)
				{
					player.getClan().broadcastClanStatus();
				}

				player.sendMessage("Name successfully changed.");
				break;

			case ("premium"):
				int day = Integer.parseInt(st.nextToken());
				int itemPremiumId = Integer.parseInt(st.nextToken());
				int price = Integer.parseInt(st.nextToken());

				long premiumTime = 0L;

				if (!Config.USE_PREMIUM_SERVICE)
				{
					player.sendMessage("This feature is currently unavailable.");
					return;
				}

				if (player.getPremServiceData() > Calendar.getInstance().getTimeInMillis())
				{
					player.sendMessage("You already have a premium account.");
					return;
				}

				if (st.countTokens() >= 1)
				{
					try
					{
						day = Integer.parseInt(st.nextToken());
					}
					catch (NumberFormatException nfe)
					{
					}
				}

				try
				{
					Calendar now = Calendar.getInstance();
					now.add(Calendar.DATE, day);
					premiumTime = now.getTimeInMillis();
				}
				catch (NumberFormatException nfe)
				{
					return;
				}

				if (!player.destroyItemByItemId("ServicePremium" + day, itemPremiumId, price, player, true))
				{
					return;
				}

				player.setPremiumService(1);
				updateDatabasePremium(premiumTime, player.getAccountName());
				player.sendMessage(String.format("You have purchased a premium account.\n Number of days: %d.", day));
				player.broadcastUserInfo();
				break;

			case ("gender"):
				int itemGenderId = Integer.parseInt(st.nextToken());
				int itemGenderCount = Integer.parseInt(st.nextToken());

				if (!player.destroyItemByItemId("ServiceShop", itemGenderId, itemGenderCount, player, true))
				{
					return;
				}

				switch (player.getAppearance().getSex())
				{
					case MALE:
						player.getAppearance().setSex(Sex.FEMALE);
						break;

					case FEMALE:
						player.getAppearance().setSex(Sex.MALE);
						break;
				}

				player.store();
				player.broadcastUserInfo();
				player.sendMessage("Your gender has been successfully changed.");
				player.decayMe();
				player.spawnMe();
				player.logout(false);
				break;

			case ("nullpk"):
				int itemNullPkId = Integer.parseInt(st.nextToken());
				int itemNullPkCount = Integer.parseInt(st.nextToken());

				if (player.getPkKills() == 0)
				{
					player.sendMessage("You have nothing to clean up.");
					return;
				}

				if (player.getInventory().getItemByItemId(itemNullPkId) == null)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				if (player.getInventory().getItemByItemId(itemNullPkId).getCount() < itemNullPkCount)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				player.destroyItemByItemId("ServiceShop", itemNullPkId, itemNullPkCount, player, true);
				player.setPkKills(0);
				player.setKarma(0);
				player.sendMessage("Your PK and karma counters have been successfully reset.");
				break;

			case ("clanlvl"):
				int clanItemId = Integer.parseInt(st.nextToken());
				int lvl = Integer.parseInt(st.nextToken());
				int lvlPrice = Integer.parseInt(st.nextToken());

				if (st.countTokens() >= 1)
				{
					try
					{
						clanItemId = Integer.parseInt(st.nextToken());
					}
					catch (NumberFormatException nfe)
					{
					}
				}

				if (!player.isClanLeader())
				{
					player.sendMessage("This operation is only available to the clan leader.");
					return;
				}

				if (player.getInventory().getItemByItemId(clanItemId) == null)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				if (player.getInventory().getItemByItemId(clanItemId).getCount() < lvlPrice)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				if (!(lvl <= player.getClan().getLevel()))
				{
					player.destroyItemByItemId("ServiceShop", clanItemId, lvlPrice, player, true);
					player.getClan().changeLevel(lvl);
					player.sendMessage("Your clan level has been successfully upgraded to maximum.");
				}
				break;

			case ("clanskill"):
				int clanSkillItemId = Integer.parseInt(st.nextToken());
				int clanSkillItemCount = Integer.parseInt(st.nextToken());

				if (!player.isClanLeader())
				{
					player.sendMessage("This operation is only available to the clan leader.");
					return;
				}

				if (player.getClan() == null || player.getClan().getLevel() < 5)
				{
					player.sendMessage("The clan must be level 5 or higher.");
					return;
				}

				if (player.getInventory().getItemByItemId(clanSkillItemId) == null)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				if (player.getInventory().getItemByItemId(clanSkillItemId).getCount() < clanSkillItemCount)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				Collection<L2Skill> currentSkills = player.getClan().getClanSkills().values();
				boolean haveAll = true;
				for (int i = 370; i < 391; i++)
				{
					if (!currentSkills.contains(SkillTable.getInstance().getInfo(i, 3)))
					{
						haveAll = false;
					}
				}

				if (!currentSkills.contains(SkillTable.getInstance().getInfo(391, 1)))
				{
					haveAll = false;
				}

				if (haveAll)
				{
					player.sendMessage("All clan skills are already available to your clan.");
					return;
				}

				player.destroyItemByItemId("ServiceShop", clanSkillItemId, clanSkillItemCount, player, true);
				for (int i = 370; i < 391; i++)
				{
					player.getClan().addClanSkill(SkillTable.getInstance().getInfo(i, 3), true);
				}

				player.getClan().addClanSkill(SkillTable.getInstance().getInfo(391, 1), true);

				player.getClan().broadcastToMembers(new PledgeSkillList(player.getClan()));
				player.sendMessage("Your clan has been successfully issued all clan skills.");
				break;

			case ("clanrep"):
				int clanRepItemId = Integer.parseInt(st.nextToken());
				int clanRepItemCount = Integer.parseInt(st.nextToken());
				int clanRepCount = Integer.parseInt(st.nextToken());

				if (!player.isClanLeader())
				{
					player.sendMessage("This operation is only available to the clan leader.");
					return;
				}

				if (player.getClan() == null || player.getClan().getLevel() < 5)
				{
					player.sendMessage("The clan must be level 5.");
					return;
				}

				if (player.getInventory().getItemByItemId(clanRepItemId) == null)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				if (player.getInventory().getItemByItemId(clanRepItemId).getCount() < clanRepItemCount)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}

				player.destroyItemByItemId("ServiceShop", clanRepItemId, clanRepItemCount, player, true);
				player.getClan().addReputationScore(clanRepCount);
				player.sendMessage("Your clan's reputation " + player.getClan().getReputationScore() + "");
				break;
		}

		super.onBypassFeedback(player, command);
	}

	public static boolean isValidNick(String name)
	{
		boolean result = true;
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch (PatternSyntaxException e)
		{
			pattern = Pattern.compile(".*");
		}

		Matcher regexp = pattern.matcher(name);
		if (!regexp.matches())
		{
			result = false;
		}

		return result;
	}

	private static void updateDatabasePremium(long time, String AccName)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_PREMIUMSERVICE))
		{
			statement.setInt(1, 1);
			statement.setLong(2, time);
			statement.setString(3, AccName);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.warn("updateDatabasePremium: Could not update data:" + e);
		}
	}
}