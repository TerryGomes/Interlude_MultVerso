package net.sf.l2j.gameserver.model.actor.cast;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.skillhandlers.StriderSiegeAssault;
import net.sf.l2j.gameserver.handler.skillhandlers.SummonFriend;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeCastle;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSiegeFlag;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;

/**
 * This class groups all cast data related to a {@link Player}.
 */
public class PlayerCast extends PlayableCast<Player>
{
	private final Location _signetLocation = new Location(Location.DUMMY_LOC);
	
	public PlayerCast(Player actor)
	{
		super(actor);
	}
	
	@Override
	public void doFusionCast(L2Skill skill, Creature target)
	{
		final int reuseDelay = skill.getReuseDelay();
		
		final boolean skillMastery = Formulas.calcSkillMastery(_actor, skill);
		if (skillMastery)
			_actor.sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
		else
		{
			if (reuseDelay > 30000)
				_actor.addTimeStamp(skill, reuseDelay);
			
			if (reuseDelay > 10)
				_actor.disableSkill(skill, reuseDelay);
		}
		
		final int initMpConsume = _actor.getStatus().getMpInitialConsume(skill);
		if (initMpConsume > 0)
			_actor.getStatus().reduceMp(initMpConsume);
		
		if (target != _actor)
			_actor.getPosition().setHeadingTo(target);
		
		_targets = new Creature[]
		{
			target
		};
		
		final int hitTime = skill.getHitTime();
		final int coolTime = skill.getCoolTime();
		final long castInterruptTime = System.currentTimeMillis() + hitTime - 200;
		
		setCastTask(skill, target, hitTime, coolTime, castInterruptTime);
		
		if (skill.getSkillType() == SkillType.FUSION)
			_actor.startFusionSkill(target, skill);
		else
			callSkill(skill, _targets);
		
		_actor.broadcastPacket(new MagicSkillUse(_actor, target, skill.getId(), skill.getLevel(), hitTime, reuseDelay, false));
		_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_S1).addSkillName(skill));
		_actor.sendPacket(new SetupGauge(GaugeColor.BLUE, _hitTime));
		
		_castTask = ThreadPool.schedule(this::onMagicEffectHitTimer, hitTime > 410 ? hitTime - 400 : 0);
	}
	
	@Override
	public void doInstantCast(L2Skill skill, ItemInstance item)
	{
		if (!item.isHerb() && !_actor.destroyItem("Consume", item.getObjectId(), (skill.getItemConsumeId() == 0 && skill.getItemConsume() > 0) ? skill.getItemConsume() : 1, null, false))
		{
			_actor.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return;
		}
		
		int reuseDelay = skill.getReuseDelay();
		if (reuseDelay > 10)
			_actor.disableSkill(skill, reuseDelay);
		
		_actor.broadcastPacket(new MagicSkillUse(_actor, _actor, skill.getId(), skill.getLevel(), 0, 0));
		
		_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_S1).addSkillName(skill));
		
		if (skill.getNumCharges() > 0)
		{
			if (skill.getMaxCharges() > 0)
				_actor.increaseCharges(skill.getNumCharges(), skill.getMaxCharges());
			else
				_actor.decreaseCharges(skill.getNumCharges());
		}
		
		callSkill(skill, new Creature[]
		{
			_actor
		});
	}
	
	@Override
	public void doToggleCast(L2Skill skill, Creature target)
	{
		setCastTask(skill, target, 0, 0, 0);
		
		_actor.broadcastPacket(new MagicSkillUse(_actor, _actor, _skill.getId(), _skill.getLevel(), 0, 0));
		
		_targets = new Creature[]
		{
			_target
		};
		
		// If the toggle is already active, we don't need to do anything else besides stopping it.
		final AbstractEffect effect = _actor.getFirstEffect(_skill.getId());
		if (effect != null)
			effect.exit();
		else
		{
			final double mpConsume = _actor.getStatus().getMpConsume(_skill);
			if (mpConsume > 0)
			{
				if (mpConsume > _actor.getStatus().getMp())
				{
					_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
					stop();
					return;
				}
				
				_actor.getStatus().reduceMp(mpConsume);
			}
			
			final double hpConsume = _skill.getHpConsume();
			if (hpConsume > 0)
			{
				if (hpConsume > _actor.getStatus().getHp())
				{
					_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));
					stop();
					return;
				}
				
				_actor.getStatus().reduceHp(hpConsume, _actor, true);
			}
			
			final ISkillHandler handler = SkillHandler.getInstance().getHandler(_skill.getSkillType());
			if (handler != null)
				handler.useSkill(_actor, _skill, _targets);
			else
				_skill.useSkill(_actor, _targets);
		}
		
		_castTask = ThreadPool.schedule(this::onMagicFinalizer, 0);
	}
	
	@Override
	public void doCast(L2Skill skill, Creature target, ItemInstance itemInstance)
	{
		super.doCast(skill, target, itemInstance);
		
		if (skill.getItemConsumeId() > 0)
			_actor.destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, true);
		
		_actor.clearRecentFakeDeath();
	}
	
	@Override
	public boolean canAttemptCast(Creature target, L2Skill skill)
	{
		if (!super.canAttemptCast(target, skill))
			return false;
		
		if (_actor.isWearingFormalWear())
		{
			_actor.sendPacket(SystemMessageId.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR);
			return false;
		}
		
		final SkillType skillType = skill.getSkillType();
		if (_actor.isFishing() && (skillType != SkillType.PUMPING && skillType != SkillType.REELING && skillType != SkillType.FISHING))
		{
			_actor.sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
			return false;
		}
		
		if (_actor.isInObserverMode())
		{
			_actor.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			return false;
		}
		
		if (_actor.isSitting() && !_actor.isFakeDeath())
		{
			_actor.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return false;
		}
		
		if (_actor.isFakeDeath() && skill.getId() != 60)
		{
			_actor.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return false;
		}
		
		if (skill.getTargetType() == SkillTargetType.GROUND && _signetLocation.equals(Location.DUMMY_LOC))
			return false;
		
		if (_actor.isInDuel())
		{
			final Player targetPlayer = target.getActingPlayer();
			if (targetPlayer != null && targetPlayer.getDuelId() != _actor.getDuelId())
			{
				_actor.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
		}
		
		if (skill.isSiegeSummonSkill())
		{
			final Siege siege = CastleManager.getInstance().getActiveSiege(_actor);
			if (siege == null || !siege.checkSide(_actor.getClan(), SiegeSide.ATTACKER))
			{
				_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
				return false;
			}
			
			if (_actor.isInsideZone(ZoneId.CASTLE))
			{
				_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_CALL_PET_FROM_THIS_LOCATION));
				return false;
			}
			
			if (SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) == CabalType.DAWN && SevenSignsManager.getInstance().getPlayerCabal(_actor.getObjectId()) == CabalType.DUSK)
			{
				_actor.sendPacket(SystemMessageId.SEAL_OF_STRIFE_FORBIDS_SUMMONING);
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean canDoCast(Creature target, L2Skill skill, boolean isCtrlPressed, int itemObjectId)
	{
		if (!super.canDoCast(target, skill, isCtrlPressed, itemObjectId))
			return false;
		
		switch (skill.getSkillType())
		{
			case SUMMON:
				if (!((L2SkillSummon) skill).isCubic() && (_actor.getSummon() != null || _actor.isMounted()))
				{
					_actor.sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
					return false;
				}
				break;
			
			case RESURRECT:
				final Siege siege = CastleManager.getInstance().getActiveSiege(_actor);
				if (siege != null)
				{
					if (_actor.getClan() == null)
					{
						_actor.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
						return false;
					}
					
					final SiegeSide side = siege.getSide(_actor.getClan());
					if (side == SiegeSide.DEFENDER || side == SiegeSide.OWNER)
					{
						if (siege.getControlTowerCount() == 0)
						{
							_actor.sendPacket(SystemMessageId.TOWER_DESTROYED_NO_RESURRECTION);
							return false;
						}
					}
					else if (side == SiegeSide.ATTACKER)
					{
						if (_actor.getClan().getFlag() == null)
						{
							_actor.sendPacket(SystemMessageId.NO_RESURRECTION_WITHOUT_BASE_CAMP);
							return false;
						}
					}
					else
					{
						_actor.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
						return false;
					}
				}
				break;
			
			case SPOIL:
			case DRAIN_SOUL:
				if (!(target instanceof Monster))
				{
					_actor.sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
				break;
			
			case SWEEP:
				if (skill.getTargetType() != SkillTargetType.AREA_CORPSE_MOB)
				{
					final int spoilerId = ((Monster) target).getSpoilState().getSpoilerId();
					if (spoilerId == 0)
					{
						_actor.sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);
						return false;
					}
					
					if (!_actor.isLooterOrInLooterParty(spoilerId))
					{
						_actor.sendPacket(SystemMessageId.SWEEP_NOT_ALLOWED);
						return false;
					}
				}
				break;
			
			case TAKE_CASTLE:
				if (TakeCastle.check(_actor, target, skill, true) == null)
					return false;
				
				break;
			
			case SIEGE_FLAG:
				if (!L2SkillSiegeFlag.check(_actor, false))
					return false;
				
				break;
			
			case STRIDER_SIEGE_ASSAULT:
				if (!StriderSiegeAssault.check(_actor, target, skill))
					return false;
				
				break;
			
			case SUMMON_FRIEND:
				if (!(SummonFriend.checkSummoner(_actor) && SummonFriend.checkSummoned(_actor, target)))
					return false;
				
				break;
		}
		return true;
	}
	
	/**
	 * Used by {@link #doFusionCast(L2Skill, Creature)}
	 */
	private final void onMagicEffectHitTimer()
	{
		// Content was cleaned meantime, simply return doing nothing.
		if (!isCastingNow())
			return;
		
		_targets = _skill.getTargetList(_actor, _target);
		
		if (_actor.getFusionSkill() != null)
		{
			_actor.getFusionSkill().onCastAbort();
			
			clearCastTask();
			return;
		}
		
		_actor.broadcastPacket(new MagicSkillLaunched(_actor, _skill, _targets));
		
		_actor.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot());
		
		final double mpConsume = _actor.getStatus().getMpConsume(_skill);
		if (mpConsume > 0)
		{
			if (mpConsume > _actor.getStatus().getMp())
			{
				_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
				stop();
				return;
			}
			
			_actor.getStatus().reduceMp(mpConsume);
		}
		
		_castTask = ThreadPool.schedule(this::onMagicEffectFinalizer, 400);
	}
	
	/**
	 * Used by {@link #doFusionCast(L2Skill, Creature)}
	 */
	public void onMagicEffectFinalizer()
	{
		_actor.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot());
		
		if (_skill.isOffensive() && _targets.length != 0)
			_actor.getAI().startAttackStance();
		
		clearCastTask();
		
		_actor.getAI().notifyEvent(AiEventType.FINISHED_CASTING, null, null);
	}
	
	public Location getSignetLocation()
	{
		return _signetLocation;
	}
}