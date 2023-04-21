package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.DropCalc;
import net.sf.l2j.gameserver.data.manager.BuyListManager;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.DropType;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.enums.skills.ElementType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.container.attackable.AggroList;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.MercenaryManagerNpc;
import net.sf.l2j.gameserver.model.actor.instance.Merchant;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.StaticObject;
import net.sf.l2j.gameserver.model.buylist.NpcBuyList;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AdminInfo implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_info"
	};

	private static final DecimalFormat PERCENT = new DecimalFormat("#.###");

	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_info"))
		{
			final WorldObject targetWorldObject = getTarget(WorldObject.class, player, true);

			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			if (targetWorldObject instanceof Door)
			{
				showDoorInfo(player, (Door) targetWorldObject, html);
			}
			else if (targetWorldObject instanceof Npc)
			{
				final Npc targetNpc = (Npc) targetWorldObject;

				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();

				if (!st.hasMoreTokens())
				{
					sendGeneralInfos(player, targetNpc, html);
				}
				else
				{
					final String subCommand = st.nextToken();
					switch (subCommand)
					{
						case "ai":
							sendAiInfos(player, targetNpc, html);
							break;

						case "aggro":
							sendAggroInfos(player, targetNpc, html);
							break;

						case "drop":
						case "spoil":
							try
							{
								final int page = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 1;

								sendDropInfos(player, targetNpc, html, page, subCommand.equalsIgnoreCase("drop"));
							}
							catch (Exception e)
							{
								sendDropInfos(player, targetNpc, html, 1, true);
							}
							break;

						case "shop":
							sendShopInfos(player, targetNpc, html);
							break;

						case "skill":
							sendSkillInfos(player, targetNpc, html);
							break;

						case "spawn":
							sendSpawnInfos(player, targetNpc, html);
							break;

						case "stat":
							sendStatsInfos(player, targetNpc, html);
							break;

						default:
							sendGeneralInfos(player, targetNpc, html);
					}
				}
			}
			else if (targetWorldObject instanceof Player)
			{
				AdminEditChar.gatherPlayerInfo(player, (Player) targetWorldObject, html);
			}
			else if (targetWorldObject instanceof Summon)
			{
				final Summon targetSummon = (Summon) targetWorldObject;
				final Player owner = targetWorldObject.getActingPlayer();

				html.setFile(player.isLang() + "admin/petinfo.htm");
				html.replace("%name%", (targetWorldObject.getName() == null) ? "N/A" : targetWorldObject.getName());
				html.replace("%level%", targetSummon.getStatus().getLevel());
				html.replace("%exp%", targetSummon.getStatus().getExp());
				html.replace("%owner%", (owner == null) ? "N/A" : " <a action=\"bypass -h admin_debug " + owner.getName() + "\">" + owner.getName() + "</a>");
				html.replace("%class%", targetSummon.getClass().getSimpleName());
				html.replace("%ai%", targetSummon.getAI().getCurrentIntention().getType().name());
				html.replace("%hp%", (int) targetSummon.getStatus().getHp() + "/" + targetSummon.getStatus().getMaxHp());
				html.replace("%mp%", (int) targetSummon.getStatus().getMp() + "/" + targetSummon.getStatus().getMaxMp());
				html.replace("%karma%", targetSummon.getKarma());
				html.replace("%undead%", (targetSummon.isUndead()) ? "yes" : "no");

				if (targetWorldObject instanceof Pet)
				{
					final Pet targetPet = ((Pet) targetWorldObject);

					html.replace("%inv%", (owner == null) ? "N/A" : " <a action=\"bypass admin_summon inventory\">view</a>");
					html.replace("%food%", targetPet.getCurrentFed() + "/" + targetPet.getPetData().getMaxMeal());
					html.replace("%load%", targetPet.getInventory().getTotalWeight() + "/" + targetPet.getWeightLimit());
				}
				else
				{
					html.replace("%inv%", "none");
					html.replace("%food%", "N/A");
					html.replace("%load%", "N/A");
				}
			}
			else if (targetWorldObject instanceof StaticObject)
			{
				final StaticObject targetStaticObject = (StaticObject) targetWorldObject;

				html.setFile(player.isLang() + "admin/staticinfo.htm");
				html.replace("%x%", targetStaticObject.getX());
				html.replace("%y%", targetStaticObject.getY());
				html.replace("%z%", targetStaticObject.getZ());
				html.replace("%objid%", targetStaticObject.getObjectId());
				html.replace("%staticid%", targetStaticObject.getStaticObjectId());
				html.replace("%class%", targetStaticObject.getClass().getSimpleName());
			}
			player.sendPacket(html);
		}
	}

	/**
	 * @param pet
	 * @param player
	 * @param html
	 */
	public static void showPetInfo(final Summon pet, Player player, final NpcHtmlMessage html)
	{
		if (pet != null)
		{
			final Summon targetSummon = pet;
			final Player owner = pet.getActingPlayer();

			html.setFile(player.isLang() + "admin/petinfo.htm");
			html.replace("%name%", (pet.getName() == null) ? "N/A" : pet.getName());
			html.replace("%level%", targetSummon.getStatus().getLevel());
			html.replace("%exp%", targetSummon.getStatus().getExp());
			html.replace("%owner%", (owner == null) ? "N/A" : " <a action=\"bypass -h admin_debug " + owner.getName() + "\">" + owner.getName() + "</a>");
			html.replace("%class%", targetSummon.getClass().getSimpleName());
			html.replace("%ai%", targetSummon.getAI().getCurrentIntention().getType().name());
			html.replace("%hp%", (int) targetSummon.getStatus().getHp() + "/" + targetSummon.getStatus().getMaxHp());
			html.replace("%mp%", (int) targetSummon.getStatus().getMp() + "/" + targetSummon.getStatus().getMaxMp());
			html.replace("%karma%", targetSummon.getKarma());
			html.replace("%undead%", (targetSummon.isUndead()) ? "yes" : "no");

			if (pet instanceof Pet)
			{
				final Pet targetPet = ((Pet) pet);

				html.replace("%inv%", (owner == null) ? "N/A" : " <a action=\"bypass admin_summon inventory\">view</a>");
				html.replace("%food%", targetPet.getCurrentFed() + "/" + targetPet.getPetData().getMaxMeal());
				html.replace("%load%", targetPet.getInventory().getTotalWeight() + "/" + targetPet.getWeightLimit());
			}
			else
			{
				html.replace("%inv%", "none");
				html.replace("%food%", "N/A");
				html.replace("%load%", "N/A");
			}
		}
	}

	/**
	 * @param player
	 * @param targetDoor
	 * @param html
	 */
	public static void showDoorInfo(Player player, Door targetDoor, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/doorinfo.htm");
		html.replace("%name%", targetDoor.getName());
		html.replace("%objid%", targetDoor.getObjectId());
		html.replace("%doorid%", targetDoor.getTemplate().getId());
		html.replace("%doortype%", targetDoor.getTemplate().getType().toString());
		html.replace("%doorlvl%", targetDoor.getTemplate().getLevel());
		html.replace("%castle%", (targetDoor.getCastle() != null) ? targetDoor.getCastle().getName() : "none");
		html.replace("%clanhall%", (targetDoor.getClanHall() != null) ? targetDoor.getClanHall().getName() : "none");
		html.replace("%opentype%", targetDoor.getTemplate().getOpenType().toString());
		html.replace("%initial%", targetDoor.getTemplate().isOpened() ? "Opened" : "Closed");
		html.replace("%ot%", targetDoor.getTemplate().getOpenTime());
		html.replace("%ct%", targetDoor.getTemplate().getCloseTime());
		html.replace("%rt%", targetDoor.getTemplate().getRandomTime());
		html.replace("%controlid%", targetDoor.getTemplate().getTriggerId());
		html.replace("%hp%", (int) targetDoor.getStatus().getHp());
		html.replace("%hpmax%", targetDoor.getStatus().getMaxHp());
		html.replace("%hpratio%", targetDoor.getStatus().getUpgradeHpRatio());
		html.replace("%pdef%", targetDoor.getStatus().getPDef(null));
		html.replace("%mdef%", targetDoor.getStatus().getMDef(null, null));
		html.replace("%spawn%", targetDoor.getPosition().toString());
		html.replace("%height%", targetDoor.getTemplate().getCollisionHeight());
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with informations regarding a {@link Npc}.
	 * @param player
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 */
	private static void sendAiInfos(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/npcinfo/ai.htm");

		final StringBuilder sb = new StringBuilder(500);

		// Check Intentions.
		StringUtil.append(sb, "<tr><td>", npc.getAI().getPreviousIntention().getType(), " <> ", npc.getAI().getCurrentIntention().getType(), " <> ", npc.getAI().getNextIntention().getType(), "</td></tr>");

		html.replace("%intention%", sb.toString());

		// Reset the StringBuilder.
		sb.setLength(0);

		// Check Clans.
		final String[] clans = npc.getTemplate().getClans();
		if (clans == null)
		{
			sb.append("<tr><td>This NPC got no clan informations.</td></tr>");
		}
		else
		{
			StringUtil.append(sb, "<tr><td><font color=\"LEVEL\">Clan:</font></td><td>", Arrays.toString(clans), " ", npc.getTemplate().getClanRange(), "</td></tr>");
			if (npc.getTemplate().getIgnoredIds() != null)
			{
				StringUtil.append(sb, "<tr><td><font color=\"LEVEL\">Ignored ids:</font></td><td>", Arrays.toString(npc.getTemplate().getIgnoredIds()), "</td></tr>");
			}
		}

		html.replace("%clan%", sb.toString());

		// Reset the StringBuilder.
		sb.setLength(0);

		// Check scripts.
		if (npc.getTemplate().getEventQuests().isEmpty())
		{
			sb.append("This NPC isn't affected by scripts.");
		}
		else
		{
			EventHandler type = null;

			for (Map.Entry<EventHandler, List<Quest>> entry : npc.getTemplate().getEventQuests().entrySet())
			{
				if (type != entry.getKey())
				{
					type = entry.getKey();
					StringUtil.append(sb, "<br><font color=\"LEVEL\">", type.name(), "</font><br1>");
				}

				for (Quest quest : entry.getValue())
				{
					StringUtil.append(sb, quest.getName(), "<br1>");
				}
			}
		}
		html.replace("%script%", sb.toString());
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with {@link AggroList} informations regarding a {@link Npc}.
	 * @param player
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 */
	private static void sendAggroInfos(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/npcinfo/default.htm");
		if (!(npc instanceof Attackable))
		{
			html.replace("%content%", "This NPC can't build aggro towards targets.<br><button value=\"Refresh\" action=\"bypass -h admin_info aggro\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
			return;
		}

		final AggroList aggroList = ((Attackable) npc).getAggroList();
		if (aggroList.isEmpty())
		{
			html.replace("%content%", "This NPC's AggroList is empty.<br><button value=\"Refresh\" action=\"bypass -h admin_info aggro\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
			return;
		}

		final StringBuilder sb = new StringBuilder(500);
		sb.append("<button value=\"Refresh\" action=\"bypass -h admin_info aggro\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"><br><table width=\"280\"><tr><td><font color=\"LEVEL\">Attacker</font></td><td><font color=\"LEVEL\">Damage</font></td><td><font color=\"LEVEL\">Hate</font></td></tr>");

		for (AggroInfo ai : aggroList.values().stream().sorted(Comparator.comparing(AggroInfo::getHate, Comparator.reverseOrder())).limit(15).collect(Collectors.toList()))
		{
			StringUtil.append(sb, "<tr><td>", ai.getAttacker().getName(), "</td><td>", ai.getDamage(), "</td><td>", ai.getHate(), "</td></tr>");
		}

		sb.append("</table><img src=\"L2UI.SquareGray\" width=280 height=1>");

		html.replace("%content%", sb.toString());
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with <b>DROPS</b> or <b>SPOILS</b> informations regarding a {@link Npc}.
	 * @param player
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 * @param page : The current page we are checking.
	 * @param isDrop : If true, we check drops only. If false, we check spoils.
	 */
	private static void sendDropInfos(Player player, Npc npc, NpcHtmlMessage html, int page, boolean isDrop)
	{
		// Load static htm.
		html.setFile(player.isLang() + "admin/npcinfo/default.htm");

		int row = 0;

		// Generate data.
		final Pagination<DropCategory> list = new Pagination<>(npc.getTemplate().getDropData().stream(), page, PAGE_LIMIT_1, dc -> (isDrop) ? dc.getDropType() != DropType.SPOIL : dc.getDropType() == DropType.SPOIL);
		for (DropCategory category : list)
		{
			double catChance = Math.min(DropCalc.getInstance().calcDropChance(player, npc, category, category.getDropType(), npc.isRaidBoss(), npc instanceof GrandBoss), 100.0);
			double baseCatChance = category.getChance() * category.getDropType().getDropRate(player, npc, npc.isRaidBoss(), npc instanceof GrandBoss);
			double chanceMultiplier = 1;
			double countMultiplier = 1;

			if (baseCatChance > 100)
			{
				countMultiplier = baseCatChance / category.getCategoryCumulativeChance();
				chanceMultiplier = baseCatChance / 100d / countMultiplier;
				baseCatChance = 100;
			}

			if (Config.ALTERNATE_DROP_LIST)
			{
				list.append("<br></center>Category: ", category.getDropType(), " - Rate: ", PERCENT.format(catChance), "%<center>");

				for (DropData drop : category.getAllDrops())
				{
					final double chance = DropCalc.getInstance().calcDropChance(player, npc, drop, category.getDropType(), npc.isRaidBoss(), npc instanceof GrandBoss);

					final double normChance = Math.min(99.99, chance);

					final double overflowFactor = Math.max(0.0, (chance - 100) / 100);
					final double inverseCategoryChance = (100 - category.getChance()) / 100;
					final double reduceFactor = Math.pow(inverseCategoryChance, 10);
					final double levelFactor = (80.0 - npc.getStatus().getLevel()) / 90;
					int min = drop.getMinDrop();
					int max = drop.getMaxDrop();

					min = (int) (min + min * overflowFactor - min * overflowFactor * reduceFactor);
					max = (int) (max + max * overflowFactor - max * overflowFactor * reduceFactor);
					if (category.getDropType() != DropType.CURRENCY)
					{
						min = (int) (min - min * levelFactor);
					}
					min = Math.max(min, drop.getMinDrop());
					if (category.getDropType() != DropType.CURRENCY)
					{
						max = (int) (max - max * levelFactor);
					}
					max = Math.max(max, min);

					final String color = (normChance > 80.) ? "90EE90" : (normChance > 5.) ? "BDB76B" : "F08080";
					final String percent = PERCENT.format(normChance);
					final String amount = (min == max) ? min + "" : min + "-" + max;
					final Item item = ItemData.getInstance().getTemplate(drop.getItemId());

					String name = item.getName();
					if (name.startsWith("Recipe: "))
					{
						name = "R: " + name.substring(8);
					}

					name = StringUtil.trimAndDress(name, 45);

					list.append(((row % 2) == 0 ? "<table width=280 bgcolor=000000><tr>" : "<table width=280><tr>"));
					list.append("<td width=44 height=41 align=center><table bgcolor=" + "FFFFFF" + " cellpadding=6 cellspacing=\"-5\"><tr><td><button width=32 height=32 back=" + item.getIcon() + " fore=" + item.getIcon() + "></td></tr></table></td>");
					list.append("<td width=246>&nbsp;", name, "<br1>");
					list.append("<table width=240><tr><td width=80><font color=B09878>Rate:</font> <font color=", color, ">", percent, "%</font></td><td width=160><font color=B09878>Amount: </font>", amount, "</td></tr></table>");
					list.append("</td></tr></table><img src=L2UI.SquareGray width=280 height=1>");

					row++;
				}
			}
			else
			{
				list.append("<br></center>Category: ", category.getDropType(), " - Rate: ", PERCENT.format(baseCatChance), "%<center>");

				for (DropData drop : category.getAllDrops())
				{
					final double chance = drop.getChance() * chanceMultiplier;
					final String color = (chance > 80.) ? "90EE90" : (chance > 5.) ? "BDB76B" : "F08080";
					final String percent = PERCENT.format(chance);
					final String amount = (drop.getMinDrop() == drop.getMaxDrop()) ? (int) (drop.getMinDrop() * countMultiplier) + "" : (int) (drop.getMinDrop() * countMultiplier) + " - " + (int) (drop.getMaxDrop() * countMultiplier);
					final Item item = ItemData.getInstance().getTemplate(drop.getItemId());

					String name = item.getName();
					if (name.startsWith("Recipe: "))
					{
						name = "R: " + name.substring(8);
					}

					name = StringUtil.trimAndDress(name, 45);

					list.append(((row % 2) == 0 ? "<table width=280 bgcolor=000000><tr>" : "<table width=280><tr>"));
					list.append("<td width=44 height=41 align=center><table bgcolor=" + "FFFFFF" + " cellpadding=6 cellspacing=\"-5\"><tr><td><button width=32 height=32 back=" + item.getIcon() + " fore=" + item.getIcon() + "></td></tr></table></td>");
					list.append("<td width=246>&nbsp;", name, "<br1>");
					list.append("<table width=240><tr><td width=80><font color=B09878>Rate:</font> <font color=", color, ">", percent, "%</font></td><td width=160><font color=B09878>Amount: </font>", amount, "</td></tr></table>");
					list.append("</td></tr></table><img src=L2UI.SquareGray width=280 height=1>");

					row++;
				}
			}
		}

		list.generateSpace(20);
		list.generatePages("bypass admin_info " + ((isDrop) ? "drop" : "spoil") + " %page%");

		html.replace("%content%", list.getContent());
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with <b>GENERAL</b> informations regarding a {@link Npc}.
	 * @param player
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 */
	public static void sendGeneralInfos(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/npcinfo/general.htm");

		html.replace("%objid%", npc.getObjectId());
		html.replace("%lvl%", npc.getTemplate().getLevel());
		html.replace("%id%", npc.getTemplate().getNpcId());
		html.replace("%tmplid%", npc.getTemplate().getIdTemplate());
		html.replace("%class%", npc.getClass().getSimpleName());
		html.replace("%race%", npc.getTemplate().getRace().toString());
		html.replace("%radius%", npc.getTemplate().getCollisionRadius());
		html.replace("%height%", npc.getTemplate().getCollisionHeight());

		html.replace("%ai_type%", npc.getTemplate().getAiType().name());
		html.replace("%ai_move%", String.valueOf(npc.getTemplate().canMove()));
		html.replace("%script%", npc.getScriptValue());
		html.replace("%ai_seed%", String.valueOf(npc.getTemplate().isSeedable()));
		html.replace("%ai_ssinfo%", npc.getCurrentSsCount() + "[" + npc.getTemplate().getSsCount() + "] - " + npc.getTemplate().getSsRate() + "%");
		html.replace("%ai_spsinfo%", npc.getCurrentSpsCount() + "[" + npc.getTemplate().getSpsCount() + "] - " + npc.getTemplate().getSpsRate() + "%");
		html.replace("%aggro%", npc.getTemplate().getAggroRange());
		html.replace("%enchant%", npc.getTemplate().getEnchantEffect());

		html.replace("%castle%", (npc.getCastle() != null) ? npc.getCastle().getName() : "none");
		html.replace("%clanhall%", (npc.getClanHall() != null) ? npc.getClanHall().getName() : "none");
		html.replace("%siegablehall%", (npc.getSiegableHall() != null) ? npc.getSiegableHall().getName() : "none");

		html.replace("%shop%", ((npc instanceof Merchant || npc instanceof MercenaryManagerNpc) ? "<button value=\"Shop\" action=\"bypass -h admin_info shop\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">" : ""));
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with <b>SPAWN</b> informations regarding a {@link Npc}.
	 * @param player : The {@link Player} used as reference.
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 */
	private static void sendSpawnInfos(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/npcinfo/spawn.htm");

		html.replace("%loc%", npc.getX() + " " + npc.getY() + " " + npc.getZ());
		html.replace("%dist%", (int) player.distance3D(npc));
		html.replace("%corpse%", StringUtil.getTimeStamp(npc.getTemplate().getCorpseTime()));

		final ASpawn spawn = npc.getSpawn();
		if (spawn != null)
		{
			html.replace("%spawn%", spawn.toString());
			if (spawn instanceof MultiSpawn)
			{
				final MultiSpawn ms = (MultiSpawn) spawn;
				html.replace("%spawndesc%", "<a action=\"bypass -h admin_maker " + ms.getNpcMaker().getName() + "\">" + ms.getDescription() + "</a>");

				final int[][] coords = ms.getCoords();
				if (coords == null)
				{
					html.replace("%spawninfo%", "loc: anywhere");
				}
				else if (coords.length == 1)
				{
					html.replace("%spawninfo%", "loc: fixed " + coords[0][0] + ", " + coords[0][1] + ", " + coords[0][2]);
				}
				else
				{
					html.replace("%spawninfo%", "loc: fixed random 1 of " + coords.length);
				}
			}
			else
			{
				html.replace("%spawndesc%", spawn.getDescription());
				html.replace("%spawninfo%", "loc: " + spawn.getSpawnLocation());
			}
			html.replace("%loc2d%", (int) npc.distance2D(npc.getSpawnLocation()));
			html.replace("%loc3d%", (int) npc.distance3D(npc.getSpawnLocation()));
			html.replace("%resp%", StringUtil.getTimeStamp(spawn.getRespawnDelay()));
			html.replace("%rand_resp%", StringUtil.getTimeStamp(spawn.getRespawnRandom()));
		}
		else
		{
			html.replace("%spawn%", "<font color=FF0000>--</font>");
			html.replace("%spawndesc%", "<font color=FF0000>--</font>");
			html.replace("%spawninfo%", "<font color=FF0000>--</font>");
			html.replace("%loc2d%", "<font color=FF0000>--</font>");
			html.replace("%loc3d%", "<font color=FF0000>--</font>");
			html.replace("%resp%", "<font color=FF0000>--</font>");
			html.replace("%rand_resp%", "<font color=FF0000>--</font>");
		}

		final StringBuilder sb = new StringBuilder(500);

		if (npc.isMaster() || npc.hasMaster())
		{
			final Npc master = npc.getMaster();
			if (master == null)
			{
				html.replace("%type%", player.isLangString().equals("en") ? "master" : "мастер");
				StringUtil.append(sb, "<tr><td><font color=LEVEL>", npc.toString(), "</font></td></tr>");
			}
			else
			{
				html.replace("%type%", player.isLangString().equals("en") ? "minion" : "миньон");
				StringUtil.append(sb, "<tr><td><font color=LEVEL>", master.toString(), "</font></td></tr>");
			}

			for (Npc minion : npc.getMinions())
			{
				StringUtil.append(sb, "<tr><td>", minion.toString(), "</td></tr>");
			}
		}
		else
		{
			html.replace("%type%", "regular NPC");
		}

		html.replace("%minion%", sb.toString());
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with <b>STATS</b> informations regarding a {@link Npc}.
	 * @param player
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 */
	private static void sendStatsInfos(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/npcinfo/stat.htm");

		html.replace("%hp%", (int) npc.getStatus().getHp());
		html.replace("%hpmax%", npc.getStatus().getMaxHp());
		html.replace("%mp%", (int) npc.getStatus().getMp());
		html.replace("%mpmax%", npc.getStatus().getMaxMp());
		html.replace("%patk%", npc.getStatus().getPAtk(null));
		html.replace("%matk%", npc.getStatus().getMAtk(null, null));
		html.replace("%pdef%", npc.getStatus().getPDef(null));
		html.replace("%mdef%", npc.getStatus().getMDef(null, null));
		html.replace("%accu%", npc.getStatus().getAccuracy());
		html.replace("%evas%", npc.getStatus().getEvasionRate(null));
		html.replace("%crit%", npc.getStatus().getCriticalHit(null, null));
		html.replace("%rspd%", (int) npc.getStatus().getMoveSpeed());
		html.replace("%aspd%", npc.getStatus().getPAtkSpd());
		html.replace("%cspd%", npc.getStatus().getMAtkSpd());
		html.replace("%str%", npc.getStatus().getSTR());
		html.replace("%dex%", npc.getStatus().getDEX());
		html.replace("%con%", npc.getStatus().getCON());
		html.replace("%int%", npc.getStatus().getINT());
		html.replace("%wit%", npc.getStatus().getWIT());
		html.replace("%men%", npc.getStatus().getMEN());
		html.replace("%ele_fire%", npc.getStatus().getDefenseElementValue(ElementType.FIRE));
		html.replace("%ele_water%", npc.getStatus().getDefenseElementValue(ElementType.WATER));
		html.replace("%ele_wind%", npc.getStatus().getDefenseElementValue(ElementType.WIND));
		html.replace("%ele_earth%", npc.getStatus().getDefenseElementValue(ElementType.EARTH));
		html.replace("%ele_holy%", npc.getStatus().getDefenseElementValue(ElementType.HOLY));
		html.replace("%ele_dark%", npc.getStatus().getDefenseElementValue(ElementType.DARK));
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with <b>SHOPS</b> informations regarding a {@link Npc}.
	 * @param player
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 */
	private static void sendShopInfos(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/npcinfo/default.htm");

		final List<NpcBuyList> buyLists = BuyListManager.getInstance().getBuyListsByNpcId(npc.getNpcId());
		if (buyLists.isEmpty())
		{
			html.replace("%content%", "This NPC doesn't hold any buyList.");
			return;
		}

		final StringBuilder sb = new StringBuilder(500);

		if (npc.getCastle() != null)
		{
			StringUtil.append(sb, "Tax rate: ", npc.getCastle().getTaxPercent(), "%<br>");
		}

		StringUtil.append(sb, "<table width=\"100%\">");

		for (NpcBuyList buyList : buyLists)
		{
			StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_buy ", buyList.getListId(), " 1\">Buylist id: ", buyList.getListId(), "</a></td></tr>");
		}

		StringUtil.append(sb, "</table>");

		html.replace("%content%", sb.toString());
	}

	/**
	 * Feed a {@link NpcHtmlMessage} with <b>SKILLS</b> informations regarding a {@link Npc}.
	 * @param player
	 * @param npc : The {@link Npc} used as reference.
	 * @param html : The {@link NpcHtmlMessage} used as reference.
	 */
	private static void sendSkillInfos(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.setFile(player.isLang() + "admin/npcinfo/default.htm");

		if (npc.getTemplate().getSkills().isEmpty())
		{
			html.replace("%content%", "This NPC doesn't hold any skill.");
			return;
		}

		final StringBuilder sb = new StringBuilder(500);

		NpcSkillType type = null; // Used to see if we moved of type.

		// For any type of SkillType
		for (Map.Entry<NpcSkillType, List<L2Skill>> entry : npc.getTemplate().getSkills().entrySet())
		{
			if (type != entry.getKey())
			{
				type = entry.getKey();
				StringUtil.append(sb, "<br><font color=\"LEVEL\">", type.name(), "</font><br1>");
			}

			for (L2Skill skill : entry.getValue())
			{
				StringUtil.append(sb, ((skill.getSkillType() == SkillType.NOTDONE) ? ("<font color=\"777777\">" + skill.getName() + "</font>") : skill.getName()), " [", skill.getId(), "-", skill.getLevel(), "]<br1>");
			}
		}

		html.replace("%content%", sb.toString());
	}
}