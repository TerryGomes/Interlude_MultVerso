package net.sf.l2j.loginserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.LogManager;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.mmocore.SelectorThread;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.Config;
import net.sf.l2j.loginserver.data.manager.GameServerManager;
import net.sf.l2j.loginserver.data.manager.IpBanManager;
import net.sf.l2j.loginserver.data.sql.AccountTable;
import net.sf.l2j.loginserver.network.LoginClient;
import net.sf.l2j.loginserver.network.LoginPacketHandler;

public class LoginServer
{
	private static final CLogger LOGGER = new CLogger(LoginServer.class.getName());

	public static final int PROTOCOL_REV = 0x0102;

	private static LoginServer _loginServer;

	private GameServerListener _gameServerListener;
	private SelectorThread<LoginClient> _selectorThread;

	public static void main(String[] args) throws Exception
	{
		_loginServer = new LoginServer();
	}

	public LoginServer() throws Exception
	{
		informarLogin();

		// Create log folder
		new File("./log").mkdir();
		new File("./log/console").mkdir();
		new File("./log/error").mkdir();

		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(new File("config/logging.properties")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}

		StringUtil.printSection("Config");
		Config.loadLoginServer();

		StringUtil.printSection("Poolers");
		ConnectionPool.init();

		AccountTable.getInstance();

		StringUtil.printSection("LoginController");
		LoginController.getInstance();

		StringUtil.printSection("GameServerManager");
		GameServerManager.getInstance();

		StringUtil.printSection("Ban List");
		IpBanManager.getInstance();

		StringUtil.printSection("IP, Ports & Socket infos");
		InetAddress bindAddress = null;
		if (!Config.LOGINSERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.LOGINSERVER_HOSTNAME);
			}
			catch (UnknownHostException uhe)
			{
				LOGGER.error("The LoginServer bind address is invalid, using all available IPs.", uhe);
			}
		}

		final LoginPacketHandler lph = new LoginPacketHandler();
		final SelectorHelper sh = new SelectorHelper();
		try
		{
			_selectorThread = new SelectorThread<>(sh, lph, sh, sh);
		}
		catch (IOException ioe)
		{
			LOGGER.error("Failed to open selector.", ioe);

			System.exit(1);
		}

		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();

			LOGGER.info("Listening for gameservers on {}:{}.", Config.GAMESERVER_LOGIN_HOSTNAME, Config.GAMESERVER_LOGIN_PORT);
		}
		catch (IOException ioe)
		{
			LOGGER.error("Failed to start the gameserver listener.", ioe);

			System.exit(1);
		}

		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.LOGINSERVER_PORT);
		}
		catch (IOException ioe)
		{
			LOGGER.error("Failed to open server socket.", ioe);

			System.exit(1);
		}
		_selectorThread.start();
		LOGGER.info("Loginserver ready on {}:{}.", (bindAddress == null) ? "*" : bindAddress.getHostAddress(), Config.LOGINSERVER_PORT);

		StringUtil.printSection("Waiting for gameserver answer");
	}

	public static LoginServer getInstance()
	{
		return _loginServer;
	}

	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}

	public void shutdown(boolean restart)
	{
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}

	public void informarLogin()
	{

		LOGGER.info("============================================================================");
		LOGGER.info("Start: ................................................................... " + "LOGIM SERVER");
		LOGGER.info("Nome: .................................................................... " + "Dev TerryMaster");
		LOGGER.info("Nome: .................................................................... " + "www.l2multverso.com.br");
		LOGGER.info("============================================================================");
	}
}