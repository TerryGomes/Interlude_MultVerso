package net.sf.l2j.gameserver.model.actor.attack;

import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * This class groups all attack data related to a {@link Creature}.
 */
public class AttackableAttack extends CreatureAttack<Attackable>
{
	public AttackableAttack(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	public boolean canDoAttack(Creature target)
	{
		if (!super.canDoAttack(target))
			return false;
		
		if (target.isFakeDeath())
			return false;
		
		return true;
	}
	
	@Override
	public boolean doAttack(Creature target)
	{
		final boolean isHit = super.doAttack(target);
		if (isHit)
		{
			// Bypass behavior if the victim isn't a player
			final Player victim = target.getActingPlayer();
			if (victim != null)
			{
				for (Quest quest : _actor.getTemplate().getEventQuests(ScriptEventType.ON_ATTACK_ACT))
					quest.notifyAttackAct(_actor, victim);
			}
		}
		return isHit;
	}
}