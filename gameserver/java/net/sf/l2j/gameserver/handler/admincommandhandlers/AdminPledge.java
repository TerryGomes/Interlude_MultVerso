package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.GMViewPledgeInfo;

public class AdminPledge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pledge"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final Player targetPlayer = getTargetPlayer(player, true);
		
		if (command.startsWith("admin_pledge"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			try
			{
				final String action = st.nextToken();
				
				if (action.equals("create"))
				{
					try
					{
						final String parameter = st.nextToken();
						final long cet = targetPlayer.getClanCreateExpiryTime();
						
						targetPlayer.setClanCreateExpiryTime(0);
						
						final Clan clan = ClanTable.getInstance().createClan(targetPlayer, parameter);
						if (clan != null)
							player.sendMessage("Clan " + parameter + " have been created. Clan leader is " + targetPlayer.getName() + ".");
						else
						{
							targetPlayer.setClanCreateExpiryTime(cet);
							player.sendMessage("There was a problem while creating the clan.");
						}
					}
					catch (Exception e)
					{
						player.sendMessage("Invalid string parameter for //pledge create.");
					}
				}
				else
				{
					final Clan targetClan = targetPlayer.getClan();
					if (targetClan == null)
					{
						player.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
						sendFile(player, "game_menu.htm");
						return;
					}
					
					if (action.equals("dismiss"))
					{
						ClanTable.getInstance().destroyClan(targetClan);
						player.sendMessage("The clan is now disbanded.");
					}
					else if (action.equals("info"))
						player.sendPacket(new GMViewPledgeInfo(targetClan, targetPlayer));
					else if (action.equals("level"))
					{
						try
						{
							final int level = Integer.parseInt(st.nextToken());
							if (level >= 0 && level < 9)
							{
								targetClan.changeLevel(level);
								player.sendMessage("You have set clan " + targetClan.getName() + " to level " + level);
							}
							else
								player.sendMessage("This clan level is incorrect. Put a number between 0 and 8.");
						}
						catch (Exception e)
						{
							player.sendMessage("Invalid number parameter for //pledge setlevel.");
						}
					}
					else if (action.startsWith("rep"))
					{
						try
						{
							final int points = Integer.parseInt(st.nextToken());
							
							if (targetClan.getLevel() < 5)
							{
								player.sendMessage("Only clans of level 5 or above may receive reputation points.");
								sendFile(player, "game_menu.htm");
								return;
							}
							
							targetClan.addReputationScore(points);
							player.sendMessage("You " + (points > 0 ? "added " : "removed ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + targetClan.getName() + "'s reputation. Their current score is: " + targetClan.getReputationScore());
						}
						catch (Exception e)
						{
							player.sendMessage("Invalid number parameter for //pledge rep.");
						}
					}
				}
			}
			catch (Exception e)
			{
				player.sendMessage("Usage: //pledge create|dismiss|info|level|rep");
			}
		}
		sendFile(player, "game_menu.htm");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}