package net.sf.l2j.gameserver.model;

import net.sf.l2j.commons.data.StatSet;

/**
 * A datatype used to retain admin command informations, such as name, authorized {@link AccessLevel} and description.
 */
public class AdminCommand
{
	private final String _name;
	private final int _accessLevel;
	private final String _params;
	private final String _desc;
	
	public AdminCommand(StatSet set)
	{
		_name = set.getString("name");
		_accessLevel = set.getInteger("accessLevel", 8);
		_params = set.getString("params", "");
		_desc = set.getString("desc", "The description is missing.");
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public final String getParams()
	{
		return _params;
	}
	
	public final String getDesc()
	{
		return _desc;
	}
}