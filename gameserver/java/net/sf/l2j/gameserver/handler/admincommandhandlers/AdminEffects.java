package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.Arrays;
import java.util.StringTokenizer;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.ExRedSky;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SSQInfo;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SunRise;
import net.sf.l2j.gameserver.network.serverpackets.SunSet;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AdminEffects implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_abnormal",
		"admin_atmosphere",
		"admin_earthquake",
		"admin_effect",
		"admin_gmspeed",
		"admin_hide",
		"admin_invul",
		"admin_jukebox",
		"admin_para",
		"admin_play_sound",
		"admin_social",
		"admin_undying"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_hide"))
		{
			if (player.getAppearance().isVisible())
			{
				player.getAppearance().setVisible(false);
				player.decayMe();
				player.broadcastUserInfo();
				player.spawnMe();
			}
			else
			{
				player.getAppearance().setVisible(true);
				player.broadcastUserInfo();
			}
			
			final Summon summon = player.getSummon();
			if (summon != null)
			{
				summon.decayMe();
				summon.getStatus().broadcastStatusUpdate();
				summon.spawnMe();
			}
		}
		else if (command.startsWith("admin_invul"))
		{
			player.setInvul(!player.isInvul());
			player.sendMessage(((player.isInvul()) ? "You are now invulnerable." : "You are now vulnerable."));
		}
		else if (command.startsWith("admin_jukebox"))
		{
			sendFile(player, "songs/songs.htm");
		}
		else if (command.startsWith("admin_undying"))
		{
			player.setMortal(!player.isMortal());
			player.sendMessage(((player.isMortal()) ? "You are now mortal." : "You are now immortal."));
		}
		else
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			if (command.startsWith("admin_abnormal"))
			{
				final Creature targetCreature = getTargetCreature(player, true);
				
				try
				{
					final int abnormal = Integer.decode("0x" + st.nextToken());
					
					if ((targetCreature.getAbnormalEffect() & abnormal) == abnormal)
						targetCreature.stopAbnormalEffect(abnormal);
					else
						targetCreature.startAbnormalEffect(abnormal);
				}
				catch (Exception e)
				{
					player.sendMessage("Usage: //abnormal mask");
				}
			}
			else if (command.startsWith("admin_atmosphere"))
			{
				try
				{
					final String type = st.nextToken();
					final String state = st.nextToken();
					
					L2GameServerPacket packet = null;
					
					if (type.equals("ssqinfo"))
					{
						if (state.equals("dawn"))
							packet = SSQInfo.DAWN_SKY_PACKET;
						else if (state.equals("dusk"))
							packet = SSQInfo.DUSK_SKY_PACKET;
						else if (state.equals("red"))
							packet = SSQInfo.RED_SKY_PACKET;
						else if (state.equals("regular"))
							packet = SSQInfo.REGULAR_SKY_PACKET;
					}
					else if (type.equals("sky"))
					{
						if (state.equals("night"))
							packet = SunSet.STATIC_PACKET;
						else if (state.equals("day"))
							packet = SunRise.STATIC_PACKET;
						else if (state.equals("red"))
							packet = new ExRedSky(10);
					}
					
					if (packet == null)
					{
						player.sendMessage("Usage: //atmosphere <ssqinfo dawn|dusk|red|regular>");
						player.sendMessage("Usage: //atmosphere <sky day|night|red>");
						return;
					}
					
					World.toAllOnlinePlayers(packet);
				}
				catch (Exception e)
				{
					player.sendMessage("Usage: //atmosphere <ssqinfo dawn|dusk|red|regular>");
					player.sendMessage("Usage: //atmosphere <sky day|night|red>");
				}
			}
			else if (command.startsWith("admin_earthquake"))
			{
				try
				{
					World.toAllOnlinePlayers(new Earthquake(player, Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())));
				}
				catch (Exception e)
				{
					player.sendMessage("Use: //earthquake <intensity> <duration>");
				}
			}
			else if (command.startsWith("admin_effect"))
			{
				final Creature targetCreature = getTargetCreature(player, true);
				
				int page = 1;
				
				if (!st.hasMoreTokens())
				{
					showMainPage(player, targetCreature, page);
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
								final L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
								if (skill == null)
								{
									player.sendMessage("Usage: //effect set id level [page]");
									return;
								}
								
								skill.getEffects(player, targetCreature);
							}
							catch (Exception e)
							{
								player.sendMessage("Usage: //effect set id level [page]");
							}
							break;
						
						case "remove":
							try
							{
								final String param2 = st.nextToken();
								if (param2.equals("all"))
								{
									targetCreature.stopAllEffects();
									
									if (player != targetCreature)
										player.sendMessage("You removed all effects from " + targetCreature.getName() + ".");
								}
								else
								{
									final int skillId = Integer.parseInt(param2);
									if (skillId < 1)
										return;
									
									Arrays.stream(targetCreature.getAllEffects()).filter(e -> e != null && e.getSkill().getId() == skillId).forEach(AbstractEffect::exit);
									
									if (player != targetCreature)
										player.sendMessage("You removed " + skillId + " skillId effect from " + targetCreature.getName() + ".");
								}
							}
							catch (Exception e)
							{
								player.sendMessage("Usage: //effect remove id [page]");
							}
							break;
						
						case "visual":
							try
							{
								targetCreature.broadcastPacket(new MagicSkillUse(targetCreature, player, Integer.parseInt(st.nextToken()), 1, 1, 0));
							}
							catch (Exception e)
							{
								player.sendMessage("Usage: //effect visual id");
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
				
				showMainPage(player, targetCreature, page);
			}
			else if (command.startsWith("admin_gmspeed"))
			{
				try
				{
					player.stopSkillEffects(7029);
					
					final int skillLevel = Integer.parseInt(st.nextToken());
					if (skillLevel > 0 && skillLevel < 5)
						player.getCast().callSkill(SkillTable.getInstance().getInfo(7029, skillLevel), new Creature[]
						{
							player
						});
				}
				catch (Exception e)
				{
					player.sendMessage("Use: //gmspeed value (0-4).");
				}
				finally
				{
					player.updateEffectIcons();
				}
			}
			else if (command.startsWith("admin_para"))
			{
				final Creature targetCreature = getTargetCreature(player, false);
				if (targetCreature == null)
				{
					player.sendPacket(SystemMessageId.INVALID_TARGET);
					return;
				}
				
				if (!targetCreature.isParalyzed())
				{
					targetCreature.startAbnormalEffect(AbnormalEffect.HOLD_2);
					targetCreature.setIsParalyzed(true);
					targetCreature.abortAll(false);
				}
				else
				{
					targetCreature.stopAbnormalEffect(AbnormalEffect.HOLD_2);
					targetCreature.setIsParalyzed(false);
					
					if (!(targetCreature instanceof Player))
						targetCreature.getAI().notifyEvent(AiEventType.THINK, null, null);
				}
			}
			else if (command.startsWith("admin_play_sound"))
			{
				try
				{
					final String soundFile = st.nextToken();
					
					player.broadcastPacket((soundFile.contains(".")) ? new PlaySound(soundFile) : new PlaySound(1, soundFile));
				}
				catch (Exception e)
				{
					player.sendMessage("Usage: //play_sound soundFile");
				}
			}
			else if (command.startsWith("admin_social"))
			{
				final Creature targetCreature = getTargetCreature(player, true);
				
				try
				{
					final int actionId = Integer.parseInt(st.nextToken());
					
					if (targetCreature instanceof Summon || targetCreature instanceof Chest || (targetCreature instanceof Npc && (actionId < 1 || actionId > 3)) || (targetCreature instanceof Player && (actionId < 2 || actionId > 16)))
					{
						player.sendPacket(SystemMessageId.INVALID_TARGET);
						return;
					}
					
					targetCreature.broadcastPacket(new SocialAction(targetCreature, actionId));
				}
				catch (Exception e)
				{
					player.sendMessage("Usage: //social actionId");
				}
			}
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	public static void showMainPage(Player player, Creature creature, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/char_effects.htm");
		html.replace("%name%", creature.getName());
		
		final Pagination<AbstractEffect> list = new Pagination<>(Arrays.stream(creature.getAllEffects()), page, PAGE_LIMIT_15);
		final StringBuilder sb = new StringBuilder(3000);
		
		if (list.isEmpty())
			sb.append("<br>This Creature got no running effects.");
		else
		{
			sb.append("<table width=270><tr><td width=220>Name</td><td width=50>Time Left</td></tr>");
			
			for (AbstractEffect effect : list)
				StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_effect remove ", effect.getSkill().getId(), "\">", effect.getSkill().getName(), "</a></td><td>", (effect.getSkill().isToggle()) ? "toggle" : effect.getPeriod() - effect.getTime() + "s", "</td></tr>");
			
			sb.append("</table><br>");
			
			list.generateSpace(sb);
			list.generatePages(sb, "bypass admin_effect %page%");
		}
		html.replace("%content%", sb.toString());
		player.sendPacket(html);
	}
}