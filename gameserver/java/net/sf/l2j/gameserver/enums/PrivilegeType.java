package net.sf.l2j.gameserver.enums;

import java.util.EnumSet;
import java.util.Set;

public enum PrivilegeType
{
	CL_JOIN_CLAN,
	CL_GIVE_TITLE,
	CL_VIEW_WAREHOUSE,
	CL_MANAGE_RANKS,
	CL_PLEDGE_WAR,
	CL_DISMISS,
	CL_REGISTER_CREST,
	CL_MASTER_RIGHTS,
	CL_MANAGE_LEVELS,
	
	CH_OPEN_DOOR,
	CH_USE_FUNCTIONS,
	CH_AUCTION,
	CH_DISMISS,
	CH_SET_FUNCTIONS,
	
	CS_OPEN_DOOR,
	CS_MANOR_ADMIN,
	CS_MANAGE_SIEGE,
	CS_USE_FUNCTIONS,
	CS_DISMISS,
	CS_TAXES,
	CS_MERCENARIES,
	CS_SET_FUNCTIONS;
	
	private int _mask;
	
	private PrivilegeType()
	{
		_mask = 1 << ordinal();
	}
	
	public static final PrivilegeType[] VALUES = values();
	
	public int getMask()
	{
		return _mask;
	}
	
	public static int encode(Set<PrivilegeType> set)
	{
		int result = 0;
		for (PrivilegeType pt : set)
			result |= pt.getMask();
		
		return result;
	}
	
	public static Set<PrivilegeType> decode(int code)
	{
		final EnumSet<PrivilegeType> result = EnumSet.noneOf(PrivilegeType.class);
		for (PrivilegeType pt : VALUES)
		{
			if ((pt.getMask() & code) != 0)
				result.add(pt);
		}
		
		return result;
	}
}