package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.StringTokenizer;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminPremium implements IAdminCommandHandler
{
	private static final CLogger LOGGER = new CLogger(AdminPremium.class.getName());

	private static final String UPDATE_PREMIUMSERVICE = "REPLACE INTO account_premium (premium_service,enddate,account_name) values(?,?,?)";

	private static final String[] ADMIN_COMMANDS =
	{
		"admin_premium_menu",
		"admin_premium_add"
	};

	@Override
	public void useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_premium_menu"))
		{
			sendFile(activeChar, "premium_menu.htm");
		}
		else if (command.startsWith("admin_premium_add"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			final String accname = st.nextToken();
			final int month = Integer.parseInt(st.nextToken());
			final int dayOfmonth = Integer.parseInt(st.nextToken());
			final int hourOfDay = Integer.parseInt(st.nextToken());

			if (accname.isEmpty())
			{
				activeChar.sendMessage("Invalid account!");
			}
			else
			{
				addPremiumServices(activeChar, month, dayOfmonth, hourOfDay, accname);
			}
		}
	}

	private static void addPremiumServices(Player player, int month, int dayOfMonth, int hourOfDay, String accName)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_PREMIUMSERVICE))
		{
			Calendar finishtime = Calendar.getInstance();
			finishtime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			finishtime.set(Calendar.HOUR_OF_DAY, hourOfDay);
			finishtime.set(Calendar.MINUTE, 0);
			finishtime.add(Calendar.MONTH, month);

			statement.setInt(1, 1);
			statement.setLong(2, finishtime.getTimeInMillis());
			statement.setString(3, accName);
			statement.execute();

			player.sendMessage("The premium has been set until: " + finishtime.getTime() + " for account: " + accName);
		}
		catch (SQLException e)
		{
			LOGGER.warn(AdminPremium.class.getName() + " Could not add premium services:" + e);
		}
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
