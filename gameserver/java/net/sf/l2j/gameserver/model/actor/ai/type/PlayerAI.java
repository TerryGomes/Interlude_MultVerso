package net.sf.l2j.gameserver.model.actor.ai.type;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.LootRule;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminInfo;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Boat;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.FestivalMonster;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.actor.instance.StaticObject;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.BoatEntrance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.RequestBypassToServer;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStart;
import net.sf.l2j.gameserver.network.serverpackets.ChairSit;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocationInVehicle;
import net.sf.l2j.gameserver.network.serverpackets.MoveToPawn;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.ItemsOnGroundTaskManager;

public class PlayerAI extends PlayableAI<Player>
{
	public PlayerAI(Player player)
	{
		super(player);
	}

	@Override
	protected void onEvtArrived()
	{
		if (_currentIntention.getType() == IntentionType.MOVE_TO)
		{
			final Boat boat = _currentIntention.getBoat();
			if (boat != null)
			{
				final BoatEntrance closestEntrance = boat.getClosestEntrance(_actor.getPosition());

				_actor.getBoatPosition().set(closestEntrance.getInnerLocation());

				// Since we're close enough to the boat we just send client onboarding packet without any movement on the server.
				_actor.broadcastPacket(new MoveToLocationInVehicle(_actor, boat, closestEntrance.getInnerLocation(), _actor.getPosition()));
			}
		}

		super.onEvtArrived();
	}

	@Override
	protected void onEvtArrivedBlocked()
	{
		if (_currentIntention.getType() == IntentionType.INTERACT)
		{
			clientActionFailed();

			final WorldObject target = _currentIntention.getTarget();
			if (_actor.getAI().canDoInteract(target))
			{
				_actor.broadcastPacket(new StopMove(_actor));

				target.onInteract(_actor);
			}
			else
			{
				super.onEvtArrivedBlocked();
			}

			doIdleIntention();
		}
		else if (_currentIntention.getType() == IntentionType.CAST)
		{
			_actor.sendPacket(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
			super.onEvtArrivedBlocked();
		}
		else
		{
			super.onEvtArrivedBlocked();
		}
	}

	@Override
	protected void onEvtSatDown(WorldObject target)
	{
		if (_nextIntention.isBlank())
		{
			doIdleIntention();
		}
		else
		{
			doIntention(_nextIntention);
		}
	}

	@Override
	protected void onEvtStoodUp()
	{
		if (_actor.getThroneId() != 0)
		{
			final WorldObject object = World.getInstance().getObject(_actor.getThroneId());
			if (object instanceof StaticObject)
			{
				((StaticObject) object).setBusy(false);
			}

			_actor.setThroneId(0);
		}

		if (_nextIntention.isBlank())
		{
			doIdleIntention();
		}
		else
		{
			doIntention(_nextIntention);
		}
	}

	@Override
	protected void onEvtBowAttackReuse()
	{
		if (_actor.getAttackType() == WeaponType.BOW)
		{
			// Attacks can be scheduled while isAttackingNow
			if (_nextIntention.getType() == IntentionType.ATTACK)
			{
				doIntention(_nextIntention);
				return;
			}

			if (_currentIntention.getType() == IntentionType.ATTACK)
			{
				if (_actor.canKeepAttacking(_currentIntention.getFinalTarget()))
				{
					notifyEvent(AiEventType.THINK, null, null);
				}
				else
				{
					doIdleIntention();
				}
			}
		}
	}

	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		if (_actor.getTamedBeast() != null)
		{
			_actor.getTamedBeast().getAI().notifyEvent(AiEventType.OWNER_ATTACKED, attacker, null);
		}

		if (_actor.isSitting())
		{
			doStandIntention();
		}

		super.onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtCancel()
	{
		_actor.getCast().stop();
		_actor.getMove().cancelFollowTask();

		doIdleIntention();
	}

	@Override
	public synchronized void doActiveIntention()
	{
		doIdleIntention();
	}

	@Override
	public synchronized void tryToActive()
	{
		tryToIdle();
	}

	@Override
	protected void thinkActive()
	{
		thinkIdle();
	}

	@Override
	protected void thinkAttack()
	{
		final Creature target = _currentIntention.getFinalTarget();
		final boolean isShiftPressed = _currentIntention.isShiftPressed();

		if (tryShiftClick(target, isShiftPressed))
		{
			return;
		}

		if (_actor.denyAiAction() || _actor.isSitting() || isTargetLost(target))
		{
			doIdleIntention();
			clientActionFailed();
			return;
		}

		if (_actor.getMove().maybeMoveToPawn(target, _actor.getStatus().getPhysicalAttackRange(), isShiftPressed))
		{
			if (isShiftPressed)
			{
				doIdleIntention();
				clientActionFailed();
			}

			return;
		}

		_actor.getMove().stop();

		if ((_actor.getAttackType() == WeaponType.BOW && _actor.getAttack().isBowCoolingDown()) || _actor.getAttack().isAttackingNow() || _actor.getCast().isCastingNow())
		{
			setNextIntention(_currentIntention);
			clientActionFailed();
			return;
		}

		if (!_actor.getAttack().canDoAttack(target))
		{
			doIdleIntention();
			clientActionFailed();
			return;
		}

		_actor.getAttack().doAttack(target);
		if (!Config.ATTACK_PTS)
		{
			setNextIntention(_currentIntention);
		}
	}

	@Override
	protected void thinkCast()
	{
		if (_actor.denyAiAction() || _actor.getAllSkillsDisabled() || _actor.getCast().isCastingNow())
		{
			doIdleIntention();
			clientActionFailed();
			return;
		}

		final Creature target = _currentIntention.getFinalTarget();
		if (target == null)
		{
			doIdleIntention();
			return;
		}

		final L2Skill skill = _currentIntention.getSkill();
		if (isTargetLost(target, skill))
		{
			doIdleIntention();
			return;
		}

		if (!_actor.getCast().canAttemptCast(target, skill))
		{
			return;
		}

		final boolean isShiftPressed = _currentIntention.isShiftPressed();
		if (skill.getTargetType() == SkillTargetType.GROUND)
		{
			if (_actor.getMove().maybeMoveToLocation(_actor.getCast().getSignetLocation(), skill.getCastRange(), false, isShiftPressed))
			{
				if (isShiftPressed)
				{
					_actor.sendPacket(SystemMessageId.TARGET_TOO_FAR);
					doIdleIntention();
				}

				return;
			}
		}
		else if (_actor.getMove().maybeMoveToPawn(target, skill.getCastRange(), isShiftPressed))
		{
			if (isShiftPressed)
			{
				_actor.sendPacket(SystemMessageId.TARGET_TOO_FAR);
				doIdleIntention();
			}

			return;
		}

		if (skill.isToggle())
		{
			_actor.getMove().stop();
			_actor.getCast().doToggleCast(skill, target);
		}
		else
		{
			final boolean isCtrlPressed = _currentIntention.isCtrlPressed();
			final int itemObjectId = _currentIntention.getItemObjectId();

			if (!_actor.getCast().canDoCast(target, skill, isCtrlPressed, itemObjectId))
			{
				if (skill.nextActionIsAttack() && target.isAttackableWithoutForceBy(_actor))
				{
					doAttackIntention(target, isCtrlPressed, isShiftPressed);
				}

				return;
			}

			if (skill.getHitTime() > 50)
			{
				_actor.getMove().stop();
			}

			if (skill.getSkillType() == SkillType.FUSION || skill.getSkillType() == SkillType.SIGNET_CASTTIME)
			{
				_actor.getCast().doFusionCast(skill, target);
			}
			else
			{
				_actor.getCast().doCast(skill, target, _actor.getInventory().getItemByObjectId(itemObjectId));
			}
		}
	}

	@Override
	protected void thinkFakeDeath()
	{
		if (_actor.denyAiAction() || _actor.isMounted())
		{
			clientActionFailed();
			return;
		}

		// Start fake death hidden in isCtrlPressed.
		if (_currentIntention.isCtrlPressed())
		{
			_actor.getMove().stop();
			_actor.startFakeDeath();
		}
		else
		{
			_actor.stopFakeDeath(false);
		}
	}

	@Override
	protected ItemInstance thinkPickUp()
	{
		final ItemInstance item = super.thinkPickUp();
		if (item == null)
		{
			return null;
		}

		synchronized (item)
		{
			if (!item.isVisible())
			{
				return null;
			}

			if (((_actor.isInParty() && _actor.getParty().getLootRule() == LootRule.ITEM_LOOTER) || !_actor.isInParty()) && !_actor.getInventory().validateCapacity(item))
			{
				_actor.sendPacket(SystemMessageId.SLOTS_FULL);
				return null;
			}

			if (_actor.getActiveTradeList() != null)
			{
				_actor.sendPacket(SystemMessageId.CANNOT_PICKUP_OR_USE_ITEM_WHILE_TRADING);
				return null;
			}

			if (item.getOwnerId() != 0 && !_actor.isLooterOrInLooterParty(item.getOwnerId()))
			{
				if (item.getItemId() == 57)
				{
					_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(item.getCount()));
				}
				else if (item.getCount() > 1)
				{
					_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(item).addNumber(item.getCount()));
				}
				else
				{
					_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(item));
				}

				return null;
			}

			if (item.hasDropProtection())
			{
				item.removeDropProtection();
			}

			item.pickupMe(_actor);

			ItemsOnGroundTaskManager.getInstance().remove(item);
		}

		if (item.getItemType() == EtcItemType.HERB)
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
			if (handler != null)
			{
				handler.useItem(_actor, item, false);
			}

			item.destroyMe("Consume", _actor, null);
		}
		else if (CursedWeaponManager.getInstance().isCursed(item.getItemId()))
		{
			_actor.addItem("Pickup", item, null, true);
		}
		else
		{
			if (item.getItemType() instanceof ArmorType || item.getItemType() instanceof WeaponType)
			{
				SystemMessage sm;
				if (item.getEnchantLevel() > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3).addString(_actor.getName()).addNumber(item.getEnchantLevel()).addItemName(item.getItemId());
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2).addString(_actor.getName()).addItemName(item.getItemId());
				}

				_actor.broadcastPacketInRadius(sm, 1400);
			}

			if (_actor.isInParty())
			{
				_actor.getParty().distributeItem(_actor, item);
			}
			else if (item.getItemId() == 57 && _actor.getInventory().getAdenaInstance() != null)
			{
				_actor.addAdena("Pickup", item.getCount(), null, true);
				item.destroyMe("Pickup", _actor, null);
			}
			else
			{
				_actor.addItem("Pickup", item, null, true);
			}
		}

		ThreadPool.schedule(() -> _actor.setIsParalyzed(false), 200);
		_actor.setIsParalyzed(true);

		return item;
	}

	@Override
	protected void thinkInteract()
	{
		final WorldObject target = _currentIntention.getTarget();
		final boolean isShiftPressed = _currentIntention.isShiftPressed();

		if (tryShiftClick(target, isShiftPressed))
		{
			return;
		}

		clientActionFailed();

		if (_actor.denyAiAction() || _actor.isSitting() || _actor.isFlying() || isTargetLost(target))
		{
			doIdleIntention();
			return;
		}

		if (!_actor.getAI().canAttemptInteract())
		{
			doIdleIntention();
			return;
		}

		if (_actor.getMove().maybeMoveToPawn(target, (target instanceof Attackable) ? Math.max(100, _actor.getStatus().getPhysicalAttackRange()) : 100, isShiftPressed))
		{
			if (isShiftPressed)
			{
				doIdleIntention();
			}

			return;
		}

		if (!_actor.getAI().canDoInteract(target))
		{
			doIdleIntention();
			return;
		}

		if (target instanceof Npc && ((Npc) target).isMoving())
		{
			_actor.broadcastPacket(new StopMove(_actor));
		}
		else
		{
			_actor.getPosition().setHeadingTo(target);
			_actor.broadcastPacket(new MoveToPawn(_actor, target, Npc.INTERACTION_DISTANCE));
		}

		target.onInteract(_actor);

		doIdleIntention();
	}

	private boolean tryShiftClick(WorldObject target, boolean isShiftPressed)
	{
		if (isShiftPressed)
		{
			if (_actor.isGM())
			{
				if (target instanceof Npc)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(0);
					AdminInfo.sendGeneralInfos(_actor, (Npc) target, html);
					_actor.sendPacket(html);
					clientActionFailed();
					return true;
				}
				else if (target instanceof Door)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(0);
					AdminInfo.showDoorInfo(_actor, (Door) target, html);
					_actor.sendPacket(html);
					clientActionFailed();
					return true;
				}
				else if (target instanceof Summon)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(0);
					AdminInfo.showPetInfo((Summon) target, _actor, html);
					_actor.sendPacket(html);
					clientActionFailed();
					return true;
				}
			}
			else if (Config.SHOW_NPC_INFO)
			{
				if (target instanceof Monster || target instanceof RaidBoss || target instanceof GrandBoss || target instanceof FestivalMonster || target instanceof Chest)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(0);
					RequestBypassToServer.showNpcStatsInfos(_actor, (Npc) target, html);
					_actor.sendPacket(html);
					clientActionFailed();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void thinkSit()
	{
		if (_actor.denyAiAction() || _actor.isSitting() || _actor.isOperating() || _actor.isMounted())
		{
			doIdleIntention();
			clientActionFailed();
			return;
		}

		_actor.getMove().stop();

		// sitDown sends the ChangeWaitType packet, which MUST precede the ChairSit packet (sent in this function) in order to properly sit on the throne.
		_actor.sitDown();

		final WorldObject target = _currentIntention.getTarget();
		final boolean isThrone = target instanceof StaticObject && ((StaticObject) target).getType() == 1;
		if (isThrone && !((StaticObject) target).isBusy() && _actor.isIn3DRadius(target, Npc.INTERACTION_DISTANCE))
		{
			_actor.setThroneId(target.getObjectId());

			((StaticObject) target).setBusy(true);
			_actor.broadcastPacket(new ChairSit(_actor.getObjectId(), ((StaticObject) target).getStaticObjectId()));
		}
	}

	@Override
	protected void thinkStand()
	{
		// no need to _actor.isOperating() here, because it is included in the Player overriden denyAiAction
		if (_actor.denyAiAction() || !_actor.isSitting() || _actor.isMounted())
		{
			doIdleIntention();
			clientActionFailed();
			return;
		}

		if (_actor.isFakeDeath())
		{
			_actor.stopFakeDeath(true);
		}
		else
		{
			_actor.standUp();
		}
	}

	@Override
	protected void thinkUseItem()
	{
		final ItemInstance itemToTest = _actor.getInventory().getItemByObjectId(_currentIntention.getItemObjectId());
		if (itemToTest == null)
		{
			return;
		}

		// Equip or unequip the related ItemInstance.
		_actor.useEquippableItem(itemToTest, false);

		// Resolve previous intention.
		if (_previousIntention.getType() != IntentionType.CAST && _previousIntention.getType() != IntentionType.USE_ITEM)
		{
			doIntention(_previousIntention);
		}
	}

	@Override
	public boolean canAttemptInteract()
	{
		if (_actor.isOperating() || _actor.isProcessingTransaction())
		{
			return false;
		}

		return true;
	}

	@Override
	public boolean canDoInteract(WorldObject target)
	{
		// Can't interact in shop mode, or during a transaction or a request.
		if (_actor.isOperating() || _actor.isProcessingTransaction())
		{
			return false;
		}

		// Can't interact if regular distance doesn't match.
		return target.isIn3DRadius(_actor, Npc.INTERACTION_DISTANCE);
	}

	@Override
	public void startAttackStance()
	{
		if (!AttackStanceTaskManager.getInstance().isInAttackStance(_actor))
		{
			final Summon summon = _actor.getSummon();
			if (summon != null)
			{
				summon.broadcastPacket(new AutoAttackStart(summon.getObjectId()));
			}

			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
		}

		AttackStanceTaskManager.getInstance().add(_actor);
	}

	@Override
	public void clientActionFailed()
	{
		_actor.sendPacket(ActionFailed.STATIC_PACKET);
	}
}