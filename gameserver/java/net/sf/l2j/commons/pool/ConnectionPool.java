package net.sf.l2j.commons.pool;

import java.sql.Connection;
import java.sql.SQLException;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;

import org.mariadb.jdbc.MariaDbPoolDataSource;

public final class ConnectionPool
{
	private static final CLogger LOGGER = new CLogger(ConnectionPool.class.getName());
	
	private static MariaDbPoolDataSource _source;
	
	public static void init()
	{
		try
		{
			_source = new MariaDbPoolDataSource();
			_source.setMaxPoolSize(Config.DATABASE_MAX_CONNECTIONS);
			_source.setUrl(Config.DATABASE_URL);
			_source.setUser(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);
			_source.setStaticGlobal(true);
		}
		catch (SQLException e)
		{
			LOGGER.error("Couldn't initialize connection pooler.", e);
		}
		LOGGER.info("Initializing ConnectionPool.");
	}
	
	public static void shutdown()
	{
		if (_source != null)
		{
			_source.close();
			_source = null;
		}
	}
	
	public static Connection getConnection() throws SQLException
	{
		return _source.getConnection();
	}
}