package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.type.SiegeGuardAI;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * This class represents all Castle guards.
 */
public final class SiegeGuard extends Attackable
{
	public SiegeGuard(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public SiegeGuardAI getAI()
	{
		return (SiegeGuardAI) _ai;
	}

	@Override
	public void setAI()
	{
		_ai = new SiegeGuardAI(this);
	}

	@Override
	public boolean isAttackableBy(Creature attacker)
	{
		if (!super.isAttackableBy(attacker))
		{
			return false;
		}

		final Player player = attacker.getActingPlayer();
		if (player == null)
		{
			return false;
		}

		if (getCastle() != null && getCastle().getSiege().isInProgress())
		{
			return getCastle().getSiege().checkSides(player.getClan(), SiegeSide.ATTACKER);
		}

		if (getSiegableHall() != null && getSiegableHall().isInSiege())
		{
			return getSiegableHall().getSiege().checkSides(player.getClan(), SiegeSide.ATTACKER);
		}

		return false;
	}

	@Override
	public boolean isAttackableWithoutForceBy(Playable attacker)
	{
		return isAttackableBy(attacker);
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	@Override
	public boolean returnHome()
	{
		// We check if a SpawnLocation exists, and if we're far from it (using drift range).
		if (getSpawnLocation() != null && !isIn2DRadius(getSpawnLocation(), getDriftRange()))
		{
			getAggroList().cleanAllHate();

			setIsReturningToSpawnPoint(true);
			forceRunStance();
			getAI().tryToMoveTo(getSpawnLocation(), null);
			return true;
		}
		return false;
	}

	@Override
	public boolean isGuard()
	{
		return true;
	}

	@Override
	public int getDriftRange()
	{
		return 20;
	}

	@Override
	public boolean canAutoAttack(Creature target)
	{
		final Player player = target.getActingPlayer();
		// Check if the target isn't GM on hide mode.
		if (player == null || player.isAlikeDead() || (player.isGM() && !player.getAppearance().isVisible()))
		{
			return false;
		}

		// Check if the target isn't in silent move mode AND too far
		if (player.isSilentMoving() && !isIn3DRadius(player, 250))
		{
			return false;
		}

		return target.isAttackableBy(this) && GeoEngine.getInstance().canSeeTarget(this, target);
	}
}