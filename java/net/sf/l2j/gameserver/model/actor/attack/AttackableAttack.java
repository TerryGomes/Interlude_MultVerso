package net.sf.l2j.gameserver.model.actor.attack;

import net.sf.l2j.gameserver.enums.EventHandler;
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
		if (!super.canDoAttack(target) || target.isFakeDeath())
		{
			return false;
		}

		return true;
	}

	@Override
	protected void onFinishedAttackBow(Creature mainTarget)
	{
		// Bypass behavior if the victim isn't a player.
		final Player player = mainTarget.getActingPlayer();
		if (player != null)
		{
			for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.ATTACK_FINISHED))
			{
				quest.onAttackFinished(_actor, player);
			}
		}
		super.onFinishedAttackBow(mainTarget);
	}

	@Override
	protected void onFinishedAttack(Creature mainTarget)
	{
		// Bypass behavior if the victim isn't a player.
		final Player player = mainTarget.getActingPlayer();
		if (player != null)
		{
			for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.ATTACK_FINISHED))
			{
				quest.onAttackFinished(_actor, player);
			}
		}
		super.onFinishedAttack(mainTarget);
	}
}