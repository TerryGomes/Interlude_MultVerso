package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
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
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				ai = _ai;
				if (ai == null)
					_ai = ai = new SiegeGuardAI(this);
			}
		}
		return ai;
	}
	
	@Override
	public boolean isAttackableBy(Creature attacker)
	{
		if (!super.isAttackableBy(attacker))
			return false;
		
		final Player player = attacker.getActingPlayer();
		if (player == null)
			return false;
		
		if (getCastle() != null && getCastle().getSiege().isInProgress())
			return getCastle().getSiege().checkSides(player.getClan(), SiegeSide.ATTACKER);
		
		if (getSiegableHall() != null && getSiegableHall().isInSiege())
			return getSiegableHall().getSiege().checkSides(player.getClan(), SiegeSide.ATTACKER);
		
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
		// TODO Is this necessary?
		if (isDead())
			return false;
		
		// TODO is getSpawn() necessary?
		if (getSpawn() != null && !isIn2DRadius(getSpawn().getLoc(), getDriftRange()))
		{
			getAggroList().cleanAllHate();
			
			setIsReturningToSpawnPoint(true);
			forceRunStance();
			getAI().tryToMoveTo(getSpawn().getLoc(), null);
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
		if (player == null || player.isAlikeDead())
			return false;
		
		// Check if the target isn't GM on hide mode.
		if (player.isGM() && !player.getAppearance().isVisible())
			return false;
		
		// Check if the target isn't in silent move mode AND too far
		if (player.isSilentMoving() && !isIn3DRadius(player, 250))
			return false;
		
		return target.isAttackableBy(this) && GeoEngine.getInstance().canSeeTarget(this, target);
	}
}