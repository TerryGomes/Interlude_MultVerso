package net.sf.l2j.gameserver.model.actor.ai.type;

import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.Die;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;
import net.sf.l2j.gameserver.skills.L2Skill;

public class CreatureAI extends AbstractAI
{
	public CreatureAI(Creature actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtFinishedAttack()
	{
		if (_nextIntention.isBlank())
			notifyEvent(AiEventType.THINK, null, null);
		else
			doIntention(_nextIntention);
	}
	
	@Override
	protected void onEvtFinishedAttackBow()
	{
		if (!_nextIntention.isBlank())
			doIntention(_nextIntention);
	}
	
	@Override
	protected void onEvtBowAttackReuse()
	{
		if (_nextIntention.isBlank())
			notifyEvent(AiEventType.THINK, null, null);
	}
	
	@Override
	protected void onEvtFinishedCasting()
	{
		if (_nextIntention.isBlank())
			doActiveIntention();
		else
			doIntention(_nextIntention);
	}
	
	@Override
	protected void onEvtArrived()
	{
		if (_currentIntention.getType() == IntentionType.FOLLOW)
			return;
		
		if (_nextIntention.isBlank())
		{
			if (_currentIntention.getType() == IntentionType.MOVE_TO)
				doActiveIntention();
			else
				notifyEvent(AiEventType.THINK, null, null);
		}
		else
			doIntention(_nextIntention);
	}
	
	@Override
	protected void onEvtArrivedBlocked()
	{
		getActor().broadcastPacket(new MoveToLocation(getActor(), getActor().getPosition()));
	}
	
	@Override
	protected void onEvtDead()
	{
		stopAITask();
		
		getActor().broadcastPacket(new Die(getActor()));
		
		stopAttackStance();
		
		doIdleIntention();
	}
	
	@Override
	protected void onEvtTeleported()
	{
		doIdleIntention();
	}
	
	@Override
	protected void thinkActive()
	{
	}
	
	@Override
	protected void thinkAttack()
	{
		if (getActor().denyAiAction() || getActor().isSitting())
		{
			doActiveIntention();
			return;
		}
		
		final Creature target = _currentIntention.getFinalTarget();
		if (isTargetLost(target))
		{
			doActiveIntention();
			return;
		}
		
		if (getActor().getMove().maybeStartOffensiveFollow(target, getActor().getStatus().getPhysicalAttackRange()))
			return;
		
		getActor().getMove().stop();
		
		if ((getActor().getAttackType() == WeaponType.BOW && getActor().getAttack().isBowCoolingDown()) || getActor().getAttack().isAttackingNow())
		{
			setNextIntention(_currentIntention);
			return;
		}
		
		if (!getActor().getAttack().canDoAttack(target))
		{
			doActiveIntention();
			return;
		}
		
		getActor().getAttack().doAttack(target);
	}
	
	@Override
	protected void thinkCast()
	{
		if (getActor().denyAiAction() || getActor().getAllSkillsDisabled() || getActor().getCast().isCastingNow())
		{
			doActiveIntention();
			return;
		}
		
		final Creature target = _currentIntention.getFinalTarget();
		final L2Skill skill = _currentIntention.getSkill();
		
		if (isTargetLost(target, skill))
		{
			doActiveIntention();
			return;
		}
		
		if (!getActor().getCast().canAttemptCast(target, skill))
			return;
		
		final boolean isShiftPressed = _currentIntention.isShiftPressed();
		if (_actor.getMove().maybeStartOffensiveFollow(target, skill.getCastRange()))
		{
			if (isShiftPressed)
				doActiveIntention();
			
			return;
		}
		
		if (!getActor().getCast().canDoCast(target, skill, _currentIntention.isCtrlPressed(), _currentIntention.getItemObjectId()))
		{
			doActiveIntention();
			return;
		}
		
		if (skill.getHitTime() > 50)
			getActor().getMove().stop();
		
		getActor().getCast().doCast(skill, target, null);
	}
	
	@Override
	protected void thinkFakeDeath()
	{
	}
	
	@Override
	protected void thinkFollow()
	{
		clientActionFailed();
		
		if (getActor().denyAiAction() || getActor().isMovementDisabled())
		{
			doActiveIntention();
			return;
		}
		
		final Creature target = _currentIntention.getFinalTarget();
		if (getActor() == target)
		{
			doActiveIntention();
			return;
		}
		
		final boolean isShiftPressed = _currentIntention.isShiftPressed();
		if (isShiftPressed)
		{
			doActiveIntention();
			return;
		}
		
		getActor().getMove().maybeStartFriendlyFollow(target, 70);
	}
	
	@Override
	protected void thinkIdle()
	{
		getActor().getMove().stop();
	}
	
	@Override
	protected void thinkInteract()
	{
	}
	
	@Override
	protected void thinkMoveTo()
	{
		if (getActor().denyAiAction() || getActor().isMovementDisabled())
		{
			doActiveIntention();
			clientActionFailed();
			return;
		}
		
		getActor().getMove().maybeMoveToLocation(_currentIntention.getLoc(), 0, true, false);
	}
	
	@Override
	protected ItemInstance thinkPickUp()
	{
		return null;
	}
	
	@Override
	protected void thinkSit()
	{
	}
	
	@Override
	protected void thinkStand()
	{
	}
	
	@Override
	protected void thinkUseItem()
	{
	}
	
	@Override
	protected void onEvtSatDown(WorldObject target)
	{
		// Not all Creatures can SIT
	}
	
	@Override
	protected void onEvtStoodUp()
	{
		// Not all Creatures can STAND
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		startAttackStance();
	}
	
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		// Not all Creatures can ATTACK
	}
	
	@Override
	protected void onEvtEvaded(Creature attacker)
	{
		// Not all Creatures have a behaviour after having evaded a shot
	}
	
	@Override
	protected void onEvtOwnerAttacked(Creature attacker)
	{
		// Not all Creatures have a behaviour after their owner has been attacked
	}
	
	@Override
	protected void onEvtCancel()
	{
		// Not all Creatures can CANCEL
	}
	
	public boolean getFollowStatus()
	{
		return false;
	}
	
	public void setFollowStatus(boolean followStatus)
	{
		// Not all Creatures can FOLLOW
	}
	
	public boolean canDoInteract(WorldObject target)
	{
		return false;
	}
	
	public boolean canAttemptInteract()
	{
		return false;
	}
}