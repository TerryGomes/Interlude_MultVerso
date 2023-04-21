package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.entity.ClanHallSiege;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.pledge.Clan;

public class Die extends L2GameServerPacket
{
	private final Creature _creature;
	private final boolean _canTeleport;
	private final int _objectId;
	private final boolean _fake;

	private boolean _sweepable;
	private boolean _allowFixedRes;
	private Clan _clan;

	public Die(Creature creature)
	{
		_creature = creature;
		_objectId = creature.getObjectId();
		_canTeleport = !(((creature instanceof Player)) && CTFEvent.isStarted() && CTFEvent.isPlayerParticipant(_objectId) || TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(_objectId) || LMEvent.isStarted() && LMEvent.isPlayerParticipant(_objectId) || DMEvent.isStarted() && DMEvent.isPlayerParticipant(_objectId));
		_fake = !creature.isDead();

		if (creature instanceof Player)
		{
			Player player = (Player) creature;
			_allowFixedRes = player.getAccessLevel().allowFixedRes();
			_clan = player.getClan();

		}
		else if (creature instanceof Monster)
		{
			_sweepable = ((Monster) creature).getSpoilState().isSweepable();
		}
	}

	@Override
	protected final void writeImpl()
	{
		if (_fake)
		{
			return;
		}

		writeC(0x06);
		writeD(_objectId);
		writeD(_canTeleport ? 0x01 : 0); // to nearest village

		if (_canTeleport && _clan != null)
		{
			final Siege siege = CastleManager.getInstance().getActiveSiege(_creature);
			final ClanHallSiege chs = ClanHallManager.getInstance().getActiveSiege(_creature);

			// Check first if an active Siege is under process.
			if (siege != null)
			{
				final SiegeSide side = siege.getSide(_clan);

				writeD((_clan.hasClanHall()) ? 0x01 : 0x00); // to clanhall
				writeD((_clan.hasCastle() || side == SiegeSide.OWNER || side == SiegeSide.DEFENDER) ? 0x01 : 0x00); // to castle
				writeD((side == SiegeSide.ATTACKER && _clan.getFlag() != null) ? 0x01 : 0x00); // to siege HQ
			}
			// If no Siege, check ClanHallSiege.
			else if (chs != null)
			{
				writeD((_clan.hasClanHall()) ? 0x01 : 0x00); // to clanhall
				writeD((_clan.hasCastle()) ? 0x01 : 0x00); // to castle
				writeD((chs.checkSide(_clan, SiegeSide.ATTACKER) && _clan.getFlag() != null) ? 0x01 : 0x00); // to siege HQ
			}
			// We're in peace mode, activate generic teleports.
			else
			{
				writeD((_clan.hasClanHall()) ? 0x01 : 0x00); // to clanhall
				writeD((_clan.hasCastle()) ? 0x01 : 0x00); // to castle
				writeD(0x00); // to siege HQ
			}
		}
		else
		{
			writeD(0x00); // to clanhall
			writeD(0x00); // to castle
			writeD(0x00); // to siege HQ
		}

		writeD((_sweepable) ? 0x01 : 0x00); // sweepable (blue glow)
		writeD((_allowFixedRes) ? 0x01 : 0x00); // FIXED
	}
}