package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AdminSkill implements IAdminCommandHandler
{
	private static final Comparator<L2Skill> COMPARE_SKILLS_BY_ID = Comparator.comparing(L2Skill::getId);
	private static final Comparator<L2Skill> COMPARE_SKILLS_BY_LVL = Comparator.comparing(L2Skill::getLevel);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_clan_skill",
		"admin_skill"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final Player targetPlayer = getTargetPlayer(player, true);
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		int page = 1;
		
		if (command.startsWith("admin_clan_skill"))
		{
			if (!targetPlayer.isClanLeader())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addCharName(targetPlayer));
				showMainPage(player, targetPlayer, page);
				return;
			}
			
			final Clan clan = targetPlayer.getClan();
			
			if (!st.hasMoreTokens())
			{
				showClanSkillList(player, clan, page);
				return;
			}
			
			final String param = st.nextToken();
			if (StringUtil.isDigit(param))
				page = Integer.parseInt(param);
			else
			{
				switch (param)
				{
					case "set":
						try
						{
							final String param2 = st.nextToken();
							if (param2.equals("all"))
							{
								if (clan.addAllClanSkills())
									player.sendMessage("You gave all available skills to " + clan.getName() + " clan.");
							}
							else
							{
								final int id = Integer.parseInt(param2);
								final int level = Integer.parseInt(st.nextToken());
								
								if (id < 370 || id > 391 || level < 1 || level > 3)
								{
									player.sendMessage("Usage: //clan_skill set id level [page]");
									showClanSkillList(player, clan, page);
									return;
								}
								
								final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
								if (skill == null)
								{
									player.sendMessage("Usage: //clan_skill set id level [page]");
									showClanSkillList(player, clan, page);
									return;
								}
								
								if (clan.addClanSkill(skill, false))
									player.sendMessage("You gave " + skill.getName() + " skill to " + clan.getName() + " clan.");
							}
						}
						catch (Exception e)
						{
							player.sendMessage("Usage: //clan_skill set id level [page]");
							
						}
						break;
					
					case "remove":
						try
						{
							final String param2 = st.nextToken();
							if (param2.equals("all"))
							{
								if (clan.removeAllClanSkills())
									player.sendMessage("You removed all skills from " + clan.getName() + " clan.");
							}
							else
							{
								final int skillId = Integer.parseInt(param2);
								
								if (clan.removeClanSkill(skillId))
									player.sendMessage("You removed " + skillId + " skillId from " + clan.getName() + " clan.");
							}
						}
						catch (Exception e)
						{
							player.sendMessage("Usage: //clan_skill remove id|all [page]");
						}
						break;
				}
				
				if (st.hasMoreTokens())
				{
					final String param3 = st.nextToken();
					if (StringUtil.isDigit(param3))
						page = Integer.parseInt(param3);
				}
			}
			
			showClanSkillList(player, clan, page);
		}
		else if (command.startsWith("admin_skill"))
		{
			if (!st.hasMoreTokens())
			{
				showMainPage(player, targetPlayer, page);
				return;
			}
			
			final String param = st.nextToken();
			if (StringUtil.isDigit(param))
				page = Integer.parseInt(param);
			else
			{
				switch (param)
				{
					case "list":
						if (st.hasMoreTokens())
						{
							final String param3 = st.nextToken();
							if (StringUtil.isDigit(param3))
								page = Integer.parseInt(param3);
						}
						showSkillList(player, targetPlayer, page);
						return;
					
					case "set":
						try
						{
							final String param2 = st.nextToken();
							if (param2.equals("all"))
							{
								targetPlayer.rewardSkills();
								player.sendMessage("You gave all available skills to " + targetPlayer.getName() + ".");
							}
							else
							{
								final L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(param2), Integer.parseInt(st.nextToken()));
								if (skill == null)
								{
									player.sendMessage("Usage: //skill set id level [page]");
									return;
								}
								
								targetPlayer.addSkill(skill, true, true);
								targetPlayer.sendSkillList();
								
								player.sendMessage("You gave " + skill.getName() + " skill to " + targetPlayer.getName() + ".");
							}
						}
						catch (Exception e)
						{
							player.sendMessage("Usage: //skill set id level [page]");
						}
						break;
					
					case "remove":
						try
						{
							final String param2 = st.nextToken();
							if (param2.equals("all"))
							{
								for (L2Skill skill : targetPlayer.getSkills().values())
									targetPlayer.removeSkill(skill.getId(), true);
								
								player.sendMessage("You removed all skills from " + targetPlayer.getName() + ".");
								
								targetPlayer.sendSkillList();
							}
							else
							{
								final int skillId = Integer.parseInt(param2);
								
								targetPlayer.removeSkill(skillId, true);
								
								player.sendMessage("You removed " + skillId + " skillId from " + targetPlayer.getName() + ".");
							}
						}
						catch (Exception e)
						{
							player.sendMessage("Usage: //skill remove id [page]");
						}
						break;
				}
				
				if (st.hasMoreTokens())
				{
					final String param3 = st.nextToken();
					if (StringUtil.isDigit(param3))
						page = Integer.parseInt(param3);
				}
			}
			
			showMainPage(player, targetPlayer, page);
		}
	}
	
	private static void showMainPage(Player player, Player targetPlayer, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/char_skills.htm");
		html.replace("%name%", targetPlayer.getName());
		
		final Pagination<L2Skill> list = new Pagination<>(targetPlayer.getSkills().values().stream(), page, PAGE_LIMIT_15);
		final StringBuilder sb = new StringBuilder(3000);
		
		sb.append("<table width=270><tr><td width=220>Name</td><td width=20>Lvl</td><td width=30>Id</td></tr>");
		
		for (L2Skill skill : list)
			StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_skill remove ", skill.getId(), "\">", skill.getName(), "</a></td><td>", skill.getLevel(), "</td><td>", skill.getId(), "</td></tr>");
		
		sb.append("</table><br>");
		
		list.generateSpace(sb);
		list.generatePages(sb, "bypass admin_skill %page%");
		
		html.replace("%content%", sb.toString());
		player.sendPacket(html);
	}
	
	private static void showSkillList(Player player, Player targetPlayer, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/char_skills_list.htm");
		html.replace("%name%", targetPlayer.getName());
		
		final Map<Object, Optional<L2Skill>> data = SkillTable.getInstance().getSkills().stream().filter(s -> s.getLevel() < 99).collect(Collectors.groupingBy(L2Skill::getId, Collectors.maxBy(COMPARE_SKILLS_BY_LVL)));
		final List<L2Skill> skills = new ArrayList<>(data.size());
		for (Optional<L2Skill> skill : data.values())
		{
			if (skill.isPresent())
				skills.add(skill.get());
		}
		
		final Pagination<L2Skill> list = new Pagination<>(skills.stream(), page, PAGE_LIMIT_15, COMPARE_SKILLS_BY_ID);
		final StringBuilder sb = new StringBuilder(3000);
		
		sb.append("<table width=270><tr><td width=220>Name</td><td width=20>Lvl</td><td width=30>Id</td></tr>");
		
		for (L2Skill skill : list)
			StringUtil.append(sb, "<tr><td>", skill.getName(), "</td><td>", skill.getLevel(), "</td><td>", skill.getId(), "</td></tr>");
		
		sb.append("</table><br>");
		
		list.generateSpace(sb);
		list.generatePages(sb, "bypass admin_skill list %page%");
		
		html.replace("%content%", sb.toString());
		player.sendPacket(html);
	}
	
	private static void showClanSkillList(Player player, Clan targetClan, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/clan_skills.htm");
		html.replace("%name%", targetClan.getName());
		
		final Pagination<L2Skill> list = new Pagination<>(Arrays.stream(SkillTable.getClanSkills()), page, PAGE_LIMIT_15, COMPARE_SKILLS_BY_ID);
		final StringBuilder sb = new StringBuilder(3000);
		
		sb.append("<table width=270><tr><td width=220>Name</td><td width=20>Lvl</td><td width=30>Id</td></tr>");
		
		for (L2Skill skill : list)
		{
			final L2Skill currentSkill = targetClan.getClanSkills().get(skill.getId());
			if (currentSkill == null)
				StringUtil.append(sb, "<tr><td>", skill.getName(), "</td><td>", skill.getLevel(), "</td><td>", skill.getId(), "</td></tr>");
			else
				StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_clan_skill remove ", skill.getId(), "\">", skill.getName(), "</a>", "</td><td>", currentSkill.getLevel(), "</td><td>", skill.getId(), "</td></tr>");
		}
		sb.append("</table><br>");
		
		list.generateSpace(sb);
		list.generatePages(sb, "bypass admin_clan_skill %page%");
		
		html.replace("%content%", sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}