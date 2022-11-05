package net.sf.l2j.gameserver.model.actor.cast;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceled;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * This class groups all cast data related to a {@link Creature}.
 * @param <T> : The {@link Creature} used as actor.
 */
public class CreatureCast<T extends Creature>
{
	public static final CLogger LOGGER = new CLogger(CreatureCast.class.getName());
	
	protected final T _actor;
	
	protected long _castInterruptTime;
	
	protected Creature[] _targets;
	protected Creature _target;
	protected L2Skill _skill;
	protected int _hitTime;
	protected int _coolTime;
	
	protected ScheduledFuture<?> _castTask;
	
	private boolean _isCastingNow;
	
	public CreatureCast(T actor)
	{
		_actor = actor;
	}
	
	public final boolean canAbortCast()
	{
		return _castInterruptTime > System.currentTimeMillis();
	}
	
	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public final L2Skill getCurrentSkill()
	{
		return _skill;
	}
	
	public void doFusionCast(L2Skill skill, Creature target)
	{
		// Non-Player Creatures cannot use FUSION or SIGNETS
	}
	
	public void doInstantCast(L2Skill itemSkill, ItemInstance item)
	{
		// Non-Playable Creatures cannot use potions or energy stones
	}
	
	public void doToggleCast(L2Skill skill, Creature target)
	{
		// Non-Player Creatures cannot use TOGGLES
	}
	
	/**
	 * Manage the casting task and display the casting bar and animation on client.
	 * @param skill : The {@link L2Skill} to cast.
	 * @param target : The {@link Creature} effected target.
	 * @param itemInstance : The potential {@link ItemInstance} used to cast.
	 */
	public void doCast(L2Skill skill, Creature target, ItemInstance itemInstance)
	{
		int hitTime = skill.getHitTime();
		int coolTime = skill.getCoolTime();
		if (!skill.isStaticHitTime())
		{
			hitTime = Formulas.calcAtkSpd(_actor, skill, hitTime);
			if (coolTime > 0)
				coolTime = Formulas.calcAtkSpd(_actor, skill, coolTime);
			
			if (skill.isMagic() && (_actor.isChargedShot(ShotType.SPIRITSHOT) || _actor.isChargedShot(ShotType.BLESSED_SPIRITSHOT)))
			{
				hitTime = (int) (0.70 * hitTime);
				coolTime = (int) (0.70 * coolTime);
			}
			
			if (skill.getHitTime() >= 500 && hitTime < 500)
				hitTime = 500;
		}
		
		int reuseDelay = skill.getReuseDelay();
		if (!skill.isStaticReuse())
		{
			reuseDelay *= _actor.getStatus().calcStat(skill.isMagic() ? Stats.MAGIC_REUSE_RATE : Stats.P_REUSE, 1, null, null);
			reuseDelay *= 333.0 / (skill.isMagic() ? _actor.getStatus().getMAtkSpd() : _actor.getStatus().getPAtkSpd());
		}
		
		final boolean skillMastery = Formulas.calcSkillMastery(_actor, skill);
		if (skillMastery)
		{
			if (_actor.getActingPlayer() != null)
				_actor.getActingPlayer().sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
		}
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
		
		_actor.broadcastPacket(new MagicSkillUse(_actor, target, skill.getId(), skill.getLevel(), hitTime, reuseDelay, false));
		
		if (itemInstance == null)
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_S1).addSkillName(skill));
		
		final long castInterruptTime = System.currentTimeMillis() + hitTime - 200;
		
		setCastTask(skill, target, hitTime, coolTime, castInterruptTime);
		
		if (_hitTime > 410)
		{
			if (_actor instanceof Player)
				_actor.sendPacket(new SetupGauge(GaugeColor.BLUE, _hitTime));
		}
		else
			_hitTime = 0;
		
		_castTask = ThreadPool.schedule(this::onMagicLaunch, hitTime > 410 ? hitTime - 400 : 0);
	}
	
	/**
	 * Manage the launching task and display the animation on client.
	 */
	private final void onMagicLaunch()
	{
		// Content was cleaned meantime, simply return doing nothing.
		if (!isCastingNow())
			return;
		
		// No checks for range, LoS, PEACE if the target is the caster.
		if (_target != _actor)
		{
			int escapeRange = 0;
			if (_skill.getEffectRange() > 0)
				escapeRange = _skill.getEffectRange();
			else if (_skill.getCastRange() <= 0 && _skill.getSkillRadius() > 80)
				escapeRange = _skill.getSkillRadius();
			
			// If the target disappears, stop the cast.
			if (_actor.getAI().isTargetLost(_target, _skill))
			{
				stop();
				return;
			}
			
			// If the target is out of range, stop the cast.
			if (escapeRange > 0 && !MathUtil.checkIfInRange(escapeRange, _actor, _target, true))
			{
				_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
				
				stop();
				return;
			}
			
			// If the target is out of view, stop the cast.
			if (_skill.getSkillRadius() > 0 && !GeoEngine.getInstance().canSeeTarget(_actor, _target))
			{
				_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
				
				stop();
				return;
			}
			
			// If the target reached a PEACE zone, stop the cast.
			if (_skill.isOffensive() && _actor instanceof Playable && _target instanceof Playable)
			{
				if (_actor.isInsideZone(ZoneId.PEACE))
				{
					_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_ATK_PEACEZONE));
					
					stop();
					return;
				}
				
				if (_target.isInsideZone(ZoneId.PEACE))
				{
					_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
					
					stop();
					return;
				}
			}
		}
		
		_targets = _skill.getTargetList(_actor, _target);
		
		_actor.broadcastPacket(new MagicSkillLaunched(_actor, _skill, _targets));
		
		_castTask = ThreadPool.schedule(this::onMagicHitTimer, _hitTime == 0 ? 0 : 400);
	}
	
	/**
	 * Manage effects application, after cast animation occured. Verify if conditions are still met.
	 */
	private final void onMagicHitTimer()
	{
		// Content was cleaned meantime, simply return doing nothing.
		if (!isCastingNow())
			return;
		
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
		
		if (_actor instanceof Player && _skill.getNumCharges() > 0)
		{
			if (_skill.getMaxCharges() > 0)
				((Player) _actor).increaseCharges(_skill.getNumCharges(), _skill.getMaxCharges());
			else
				((Player) _actor).decreaseCharges(_skill.getNumCharges());
		}
		
		for (final Creature target : _targets)
		{
			if (target instanceof Summon && _actor instanceof Player)
				((Summon) target).updateAndBroadcastStatus(1);
		}
		
		callSkill(_skill, _targets);
		
		_castTask = ThreadPool.schedule(this::onMagicFinalizer, (_hitTime == 0 || _coolTime == 0) ? 0 : _coolTime);
	}
	
	/**
	 * Manage the end of a cast launch.
	 */
	protected final void onMagicFinalizer()
	{
		// Content was cleaned meantime, simply return doing nothing.
		if (!isCastingNow())
			return;
		
		_actor.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot());
		
		if (_skill.isOffensive() && _targets.length != 0)
			_actor.getAI().startAttackStance();
		
		final Creature target = _targets.length > 0 ? _targets[0] : _target;
		_actor.notifyQuestEventSkillFinished(_skill, target);
		
		clearCastTask();
		_actor.getAI().notifyEvent(AiEventType.FINISHED_CASTING, null, null);
	}
	
	/**
	 * Check cast conditions BEFORE MOVEMENT.
	 * @param target : The {@link Creature} used as parameter.
	 * @param skill : The {@link L2Skill} used as parameter.
	 * @return True if casting is possible, false otherwise.
	 */
	public boolean canAttemptCast(Creature target, L2Skill skill)
	{
		if (_actor.isSkillDisabled(skill))
		{
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check cast conditions AFTER MOVEMENT.
	 * @param target : The {@link Creature} used as parameter.
	 * @param skill : The {@link L2Skill} used as parameter.
	 * @param isCtrlPressed : If True, we use specific CTRL rules.
	 * @param itemObjectId : If different than 0, an object has been used.
	 * @return True if casting is possible, false otherwise.
	 */
	public boolean canDoCast(Creature target, L2Skill skill, boolean isCtrlPressed, int itemObjectId)
	{
		final int initialMpConsume = _actor.getStatus().getMpInitialConsume(skill);
		final int mpConsume = _actor.getStatus().getMpConsume(skill);
		
		if ((initialMpConsume > 0 || mpConsume > 0) && (int) _actor.getStatus().getMp() < initialMpConsume + mpConsume)
		{
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			return false;
		}
		
		if (skill.getHpConsume() > 0 && (int) _actor.getStatus().getHp() <= skill.getHpConsume())
		{
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			return false;
		}
		
		if ((skill.isMagic() && _actor.isMuted()) || (!skill.isMagic() && _actor.isPhysicalMuted()))
			return false;
		
		if (skill.getCastRange() > 0 && !GeoEngine.getInstance().canSeeTarget(_actor, target))
		{
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
			return false;
		}
		
		if (!skill.getWeaponDependancy(_actor))
			return false;
		
		return true;
	}
	
	/**
	 * Abort the current cast, no matter actual cast step.
	 */
	public final void stop()
	{
		if (_actor.getFusionSkill() != null)
			_actor.getFusionSkill().onCastAbort();
		
		final AbstractEffect effect = _actor.getFirstEffect(EffectType.SIGNET_GROUND);
		if (effect != null)
			effect.exit();
		
		if (_actor.isAllSkillsDisabled())
			_actor.enableAllSkills();
		
		if (isCastingNow())
			_actor.broadcastPacket(new MagicSkillCanceled(_actor.getObjectId()));
		
		if (_castTask != null)
		{
			_castTask.cancel(false);
			_castTask = null;
		}
		
		clearCastTask();
		
		_actor.getAI().tryToActive();
		_actor.getAI().clientActionFailed();
	}
	
	/**
	 * Interrupt the current cast, if it is still breakable.
	 */
	public void interrupt()
	{
		if (canAbortCast())
		{
			stop();
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTING_INTERRUPTED));
		}
	}
	
	/**
	 * Launch the magic skill and calculate its effects on each target contained in the targets array.
	 * @param skill : The {@link L2Skill} to use.
	 * @param targets : The array of {@link Creature} targets.
	 */
	public void callSkill(L2Skill skill, Creature[] targets)
	{
		// Raid Curses system.
		if (_actor instanceof Playable && _actor.testCursesOnSkillSee(skill, targets))
			return;
		
		for (final Creature target : targets)
		{
			if (_actor instanceof Playable && target instanceof Monster && skill.isOverhit())
				((Monster) target).getOverhitState().set(true);
			
			switch (skill.getSkillType())
			{
				case COMMON_CRAFT:
				case DWARVEN_CRAFT:
					break;
				
				default:
					final Weapon activeWeaponItem = _actor.getActiveWeaponItem();
					if (activeWeaponItem != null && !target.isDead())
						activeWeaponItem.castSkillOnMagic(_actor, target, skill);
					
					if (_actor.getChanceSkills() != null)
						_actor.getChanceSkills().onSkillHit(target, false, skill.isMagic(), skill.isOffensive());
					
					if (target.getChanceSkills() != null)
						target.getChanceSkills().onSkillHit(_actor, true, skill.isMagic(), skill.isOffensive());
			}
		}
		
		final ISkillHandler handler = SkillHandler.getInstance().getHandler(skill.getSkillType());
		if (handler != null)
			handler.useSkill(_actor, skill, targets);
		else
			skill.useSkill(_actor, targets);
		
		final Player player = _actor.getActingPlayer();
		if (player != null)
		{
			for (final Creature target : targets)
			{
				if (skill.isOffensive())
				{
					if (player.getSummon() != target)
						player.updatePvPStatus(target);
				}
				else
				{
					if (target instanceof Playable)
					{
						final Player targetPlayer = target.getActingPlayer();
						if (!(targetPlayer.equals(_actor) || targetPlayer.equals(player)) && (targetPlayer.getPvpFlag() > 0 || targetPlayer.getKarma() > 0))
							player.updatePvPStatus();
					}
					else if (target instanceof Attackable && !((Attackable) target).isGuard())
					{
						switch (skill.getSkillType())
						{
							case SUMMON:
							case BEAST_FEED:
							case UNLOCK:
							case UNLOCK_SPECIAL:
							case DELUXE_KEY_UNLOCK:
								break;
							
							default:
								player.updatePvPStatus();
						}
					}
				}
				
				switch (skill.getTargetType())
				{
					case CORPSE_MOB:
					case AREA_CORPSE_MOB:
						if (target instanceof Npc && target.isDead())
							((Npc) target).endDecayTask();
						break;
					default:
						break;
				}
			}
			
			// Notify NPCs in a 1000 range of a skill use.
			for (Npc npc : _actor.getKnownTypeInRadius(Npc.class, 1000))
			{
				for (Quest quest : npc.getTemplate().getEventQuests(ScriptEventType.ON_SKILL_SEE))
					quest.notifySkillSee(npc, player, skill, targets, _actor instanceof Summon);
			}
		}
		
		if (skill.isOffensive())
		{
			switch (skill.getSkillType())
			{
				case AGGREDUCE:
				case AGGREMOVE:
				case AGGREDUCE_CHAR:
					break;
				
				default:
					for (final Creature target : targets)
					{
						if (target != null && target.hasAI())
							target.getAI().notifyEvent(AiEventType.ATTACKED, _actor, null);
					}
					break;
			}
		}
	}
	
	protected void clearCastTask()
	{
		_isCastingNow = false;
	}
	
	protected void setCastTask(L2Skill skill, Creature target, int hitTime, int coolTime, long castInterruptTime)
	{
		_skill = skill;
		_target = target;
		_hitTime = hitTime;
		_coolTime = coolTime;
		_castInterruptTime = castInterruptTime;
		_isCastingNow = true;
	}
}