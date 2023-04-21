package net.sf.l2j.gameserver.model.location;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.network.NpcStringId;

/**
 * A datatype extending {@link Location}, used as a unique node of a pre-programmed route for walking NPCs.<br>
 * <br>
 * Added to the x/y/z informations, you can also find delay (the time the walking NPC will stand on the point without moving), the String to broadcast (null if none) and the running behavior.
 */
public class WalkerLocation extends Location
{
	private final boolean _mustRun;
	private final int _delay;
	private final NpcStringId _fstring;
	private final int _socialId;

	public WalkerLocation(StatSet set, boolean mustRun)
	{
		super(set);

		_mustRun = mustRun;
		_delay = set.getInteger("delay", 0) * 1000;
		_fstring = set.getNpcStringId("fstring", null);
		_socialId = set.getInteger("socialId", 0);
	}

	public boolean mustRun()
	{
		return _mustRun;
	}

	public int getDelay()
	{
		return _delay;
	}

	public NpcStringId getNpcStringId()
	{
		return _fstring;
	}

	public int getSocialId()
	{
		return _socialId;
	}
}