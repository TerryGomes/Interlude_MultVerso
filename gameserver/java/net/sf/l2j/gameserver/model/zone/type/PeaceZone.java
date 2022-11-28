package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;

/**
 * A zone extending {@link ZoneType}, notably used for peace behavior (pvp related).
 */
public class PeaceZone extends ZoneType
{
	public PeaceZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (Config.ZONE_TOWN == 1 && character instanceof Player && ((Player) character).getSiegeState() != 0)
		{
			return;
		}

		if (Config.ZONE_TOWN != 2)
		{
			character.setInsideZone(ZoneId.PEACE, true);
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		if (Config.ZONE_TOWN != 2)
		{
			character.setInsideZone(ZoneId.PEACE, false);
		}
	}
}