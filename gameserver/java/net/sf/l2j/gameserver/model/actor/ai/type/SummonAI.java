package net.sf.l2j.gameserver.model.actor.ai.type;

import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.LootRule;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStart;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStop;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.ItemsOnGroundTaskManager;

public class SummonAI extends PlayableAI
{
	private volatile boolean _followOwner = true;
	
	public SummonAI(Summon summon)
	{
		super(summon);
	}
	
	@Override
	protected void thinkIdle()
	{
		super.thinkIdle();
		
		_followOwner = false;
	}
	
	@Override
	protected void thinkActive()
	{
		if (_nextIntention.isBlank())
		{
			if (_followOwner)
				doFollowIntention(getOwner(), false);
			else
				doIdleIntention();
		}
		else
			super.thinkActive();
	}
	
	@Override
	protected void onEvtTeleported()
	{
		_followOwner = true;
		doFollowIntention(getOwner(), false);
	}
	
	@Override
	protected void onEvtFinishedCasting()
	{
		if (_nextIntention.isBlank())
		{
			if (_previousIntention.getType() == IntentionType.ATTACK)
				doIntention(_previousIntention);
			else
				doActiveIntention();
		}
		else
			doIntention(_nextIntention);
	}
	
	@Override
	public void onEvtAttacked(Creature attacker)
	{
		super.onEvtAttacked(attacker);
		
		getActor().getMove().avoidAttack(attacker);
	}
	
	@Override
	protected void onEvtEvaded(Creature attacker)
	{
		super.onEvtEvaded(attacker);
		
		getActor().getMove().avoidAttack(attacker);
	}
	
	@Override
	protected void thinkAttack()
	{
		if (getActor().denyAiAction())
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
		
		final boolean isShiftPressed = _currentIntention.isShiftPressed();
		if (getActor().getMove().maybeStartOffensiveFollow(target, getActor().getStatus().getPhysicalAttackRange()))
		{
			if (isShiftPressed)
				doActiveIntention();
			
			return;
		}
		
		getActor().getMove().stop();
		
		if (!getActor().getAttack().canDoAttack(target))
		{
			doActiveIntention();
			return;
		}
		
		getActor().getAttack().doAttack(target);
	}
	
	@Override
	protected void thinkFollow()
	{
		if (getActor().denyAiAction() || getActor().isMovementDisabled())
			return;
		
		final Creature target = _currentIntention.getFinalTarget();
		if (getActor() == target)
			return;
		
		final boolean isShiftPressed = _currentIntention.isShiftPressed();
		if (isShiftPressed)
			return;
		
		getActor().getMove().maybeStartFriendlyFollow(target, 70);
	}
	
	@Override
	protected void thinkInteract()
	{
		final WorldObject target = _currentIntention.getTarget();
		if (isTargetLost(target))
		{
			doActiveIntention();
			return;
		}
		
		final boolean isShiftPressed = _currentIntention.isShiftPressed();
		if (getActor().getMove().maybeMoveToLocation(target.getPosition(), getActor().getStatus().getPhysicalAttackRange(), true, isShiftPressed))
		{
			if (isShiftPressed)
				doActiveIntention();
			
			return;
		}
	}
	
	@Override
	protected ItemInstance thinkPickUp()
	{
		final ItemInstance item = super.thinkPickUp();
		
		if (item == null)
			return null;
		
		if (CursedWeaponManager.getInstance().isCursed(item.getItemId()))
		{
			getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(item.getItemId()));
			return null;
		}
		
		if (item.getItem().getItemType() == EtcItemType.ARROW || item.getItem().getItemType() == EtcItemType.SHOT)
		{
			getOwner().sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return null;
		}
		
		synchronized (item)
		{
			if (!item.isVisible())
				return null;
			
			if (!getActor().getInventory().validateCapacity(item))
			{
				getOwner().sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
				return null;
			}
			
			if (item.getOwnerId() != 0 && !getActor().getOwner().isLooterOrInLooterParty(item.getOwnerId()))
			{
				if (item.getItemId() == 57)
					getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(item.getCount()));
				else if (item.getCount() > 1)
					getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(item.getItemId()).addNumber(item.getCount()));
				else
					getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(item.getItemId()));
				
				return null;
			}
			
			if (item.hasDropProtection())
				item.removeDropProtection();
			
			final Party party = getActor().getOwner().getParty();
			if (party != null && party.getLootRule() != LootRule.ITEM_LOOTER)
				party.distributeItem(getActor().getOwner(), item);
			else
				item.pickupMe(_actor);
			
			ItemsOnGroundTaskManager.getInstance().remove(item);
		}
		
		if (item.getItemType() == EtcItemType.HERB)
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
			if (handler != null)
				handler.useItem(getActor(), item, false);
			
			item.destroyMe("Consume", getActor().getOwner(), null);
			getActor().getStatus().broadcastStatusUpdate();
		}
		else
		{
			SystemMessage sm;
			
			if (item.getItemType() instanceof ArmorType || item.getItemType() instanceof WeaponType)
			{
				if (item.getEnchantLevel() > 0)
					sm = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PET_PICKED_UP_S2_S3).addCharName(getActor().getOwner()).addNumber(item.getEnchantLevel()).addItemName(item.getItemId());
				else
					sm = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PET_PICKED_UP_S2).addCharName(getActor().getOwner()).addItemName(item.getItemId());
				
				getOwner().broadcastPacketInRadius(sm, 1400);
			}
			
			if (item.getItemId() == 57)
				sm = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1_ADENA).addItemNumber(item.getCount());
			else if (item.getEnchantLevel() > 0)
				sm = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1_S2).addNumber(item.getEnchantLevel()).addItemName(item.getItemId());
			else if (item.getCount() > 1)
				sm = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S2_S1_S).addItemName(item.getItemId()).addItemNumber(item.getCount());
			else
				sm = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1).addItemName(item.getItemId());
			
			getOwner().sendPacket(sm);
			getActor().getInventory().addItem("Pickup", item, getOwner(), getActor());
			getOwner().sendPacket(new PetItemList(getActor()));
		}
		
		return item;
	}
	
	@Override
	public Summon getActor()
	{
		return (Summon) _actor;
	}
	
	private Player getOwner()
	{
		return getActor().getOwner();
	}
	
	@Override
	public void startAttackStance()
	{
		if (!AttackStanceTaskManager.getInstance().isInAttackStance(getOwner()))
		{
			getActor().broadcastPacket(new AutoAttackStart(getActor().getObjectId()));
			getOwner().broadcastPacket(new AutoAttackStart(getOwner().getObjectId()));
		}
		
		AttackStanceTaskManager.getInstance().add(getOwner());
	}
	
	@Override
	public void stopAttackStance()
	{
		getActor().broadcastPacket(new AutoAttackStop(getActor().getObjectId()));
	}
	
	public void switchFollowStatus()
	{
		setFollowStatus(!_followOwner);
	}
	
	@Override
	public void setFollowStatus(boolean state)
	{
		_followOwner = state;
		
		if (_followOwner)
			tryToFollow(getOwner(), false);
		else
			tryToIdle();
	}
	
	@Override
	public boolean getFollowStatus()
	{
		return _followOwner;
	}
	
	@Override
	public boolean isTargetLost(WorldObject object)
	{
		final boolean isTargetLost = super.isTargetLost(object);
		if (isTargetLost)
			setFollowStatus(true);
		
		return isTargetLost;
	}
	
	@Override
	public boolean isTargetLost(WorldObject object, L2Skill skill)
	{
		final boolean isTargetLost = super.isTargetLost(object, skill);
		if (isTargetLost)
			setFollowStatus(true);
		
		return isTargetLost;
	}
}