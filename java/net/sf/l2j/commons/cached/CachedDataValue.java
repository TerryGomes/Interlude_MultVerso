package net.sf.l2j.commons.cached;

import java.sql.SQLException;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;

public class CachedDataValue
{
	public static final CLogger LOGGER = new CLogger(CachedDataValue.class.getName());

	private static final String LOAD_QUERY = "SELECT `valueData` FROM `character_data` WHERE `valueName`='%s' AND `charId`=%d LIMIT 1";
	private static final String UPDATE_QUERY = "INSERT INTO `character_data` (`charId`, `valueName`, `valueData`) VALUES (%d, '%s', ?) ON DUPLICATE KEY UPDATE `valueData`=?";

	private final String valueName;

	private final String compiledLoadQuery;
	private final String compiledUpdateQuery;

	private volatile String valueData;

	private volatile boolean dirty = false;

	CachedDataValue(String valueName, String defaultValue, int charId)
	{
		this.valueName = valueName;
		valueData = defaultValue;
		compiledLoadQuery = String.format(LOAD_QUERY, valueName, charId);
		compiledUpdateQuery = String.format(UPDATE_QUERY, charId, valueName);
	}

	public String getKey()
	{
		return valueName;
	}

	synchronized void update()
	{
		if (dirty)
		{
			save();
			dirty = false;
		}
	}

	void save()
	{
		try (var conn = ConnectionPool.getConnection();
			var stmt = conn.prepareStatement(compiledUpdateQuery))
		{
			stmt.setString(1, valueData);
			stmt.setString(2, valueData);
			stmt.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.error("Failed save({}) character_data", e, valueName);
		}
	}

	void load()
	{
		try (var conn = ConnectionPool.getConnection();
			var stmt = conn.prepareStatement(compiledLoadQuery))
		{
			try (var rs = stmt.executeQuery())
			{
				while (rs.next())
				{
					valueData = rs.getString(1);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.error("Failed load({}) character_data", e, valueName);
		}
	}

	public synchronized void setValue(String value)
	{
		valueData = value;
		dirty = true;
	}

	public String getValue()
	{
		return valueData;
	}
}