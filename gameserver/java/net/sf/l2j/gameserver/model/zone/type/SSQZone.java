package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;

public class SSQZone extends ZoneType
{
	private final int[] _oustLoc = new int[3];

	public SSQZone(int id)
	{
		super(id);
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("oustX"))
		{
			_oustLoc[0] = Integer.parseInt(value);
		}
		else if (name.equals("oustY"))
		{
			_oustLoc[1] = Integer.parseInt(value);
		}
		else if (name.equals("oustZ"))
		{
			_oustLoc[2] = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
		character.setInsideZone(ZoneId.SSQ, true);

		if (character instanceof Pet)
		{
			final Player player = ((Pet) character).getOwner();
			if (player != null)
			{
				// Remove summon.
				((Pet) character).unSummon(player);
			}
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		character.setInsideZone(ZoneId.SSQ, false);
	}
}