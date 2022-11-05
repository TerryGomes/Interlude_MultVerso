package net.sf.l2j.gameserver.model.actor.attack;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.creature.ChanceSkillList;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Attack;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * This class groups all attack data related to a {@link Creature}.
 * @param <T> : The {@link Creature} used as actor.
 */
public class CreatureAttack<T extends Creature>
{
	public static final CLogger LOGGER = new CLogger(CreatureAttack.class.getName());
	
	protected final T _actor;
	
	private boolean _isAttackingNow;
	private boolean _isBowCoolingDown;
	private HitHolder[] _hitHolders;
	private WeaponType _weaponType;
	private int _afterAttackDelay;
	
	private ScheduledFuture<?> _attackTask;
	
	public CreatureAttack(T actor)
	{
		_actor = actor;
	}
	
	public boolean isAttackingNow()
	{
		return _isAttackingNow;
	}
	
	public boolean isBowCoolingDown()
	{
		return _isBowCoolingDown;
	}
	
	/**
	 * @param target The target to check
	 * @return True if the attacker doesn't have isAttackingDisabled
	 */
	public boolean canDoAttack(Creature target)
	{
		if (_actor.isAttackingDisabled())
			return false;
		
		if (!target.isAttackableBy(_actor) || !_actor.knows(target))
			return false;
		
		if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
		{
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Manage hit process (called by Hit Task).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send ActionFailed (if attacker is a Player)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are Player</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
	 * </ul>
	 */
	private final void onHitTimer()
	{
		// Content was cleaned meantime, simply return doing nothing.
		if (!isAttackingNow())
			return;
			
		// Something happens to the target between the attacker attacking and the actual damage being dealt.
		// There is no PEACE zone check here. If the attack starts outside and in the meantime the mainTarget walks into a PEACE zone, it gets hit.
		final Creature mainTarget = _hitHolders[0]._target;
		if (mainTarget.isDead() || !_actor.knows(mainTarget))
		{
			stop();
			return;
		}
		
		final Player player = _actor.getActingPlayer();
		
		// Player can't flag if attacking his Summon, and vice-versa.
		if (player != null && player.getSummon() != mainTarget && !(player.getSummon() == _actor && mainTarget == player))
			player.updatePvPStatus(mainTarget);
		
		_actor.rechargeShots(true, false);
		
		// Test curses. Prevents messing up drop calculation.
		if (_actor instanceof Playable && mainTarget.isRaidRelated() && _actor.testCursesOnAttack((Npc) mainTarget))
		{
			stop();
			return;
		}
		
		switch (_weaponType)
		{
			case DUAL:
				doHit(_hitHolders[0]);
				
				_attackTask = ThreadPool.schedule(() ->
				{
					// Content was cleaned meantime, simply return doing nothing.
					if (!isAttackingNow())
						return;
					
					doHit(_hitHolders[1]);
					
					_attackTask = ThreadPool.schedule(this::onFinishedAttack, _afterAttackDelay);
				}, _afterAttackDelay);
				break;
			
			case POLE:
				for (HitHolder hitHolder : _hitHolders)
					doHit(hitHolder);
				
				_attackTask = ThreadPool.schedule(this::onFinishedAttack, _afterAttackDelay);
				break;
			
			case BOW:
				doHit(_hitHolders[0]);
				
				_isBowCoolingDown = true;
				
				_attackTask = ThreadPool.schedule(() ->
				{
					_isBowCoolingDown = false;
					_actor.getAI().notifyEvent(AiEventType.BOW_ATTACK_REUSED, null, null);
					
				}, _afterAttackDelay);
				
				onFinishedAttackBow();
				break;
			
			default:
				doHit(_hitHolders[0]);
				
				_attackTask = ThreadPool.schedule(this::onFinishedAttack, _afterAttackDelay);
				break;
		}
	}
	
	private void onFinishedAttackBow()
	{
		clearAttackTask(false);
		
		_actor.getAI().notifyEvent(AiEventType.FINISHED_ATTACK_BOW, null, null);
	}
	
	private void onFinishedAttack()
	{
		clearAttackTask(false);
		
		_actor.getAI().notifyEvent(AiEventType.FINISHED_ATTACK, null, null);
	}
	
	private void doHit(HitHolder hitHolder)
	{
		final Creature target = hitHolder._target;
		if (hitHolder._miss)
		{
			if (target.hasAI())
				target.getAI().notifyEvent(AiEventType.EVADED, _actor, null);
			
			if (target.getChanceSkills() != null)
				target.getChanceSkills().onEvadedHit(_actor);
			
			if (target instanceof Player)
				target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(_actor));
		}
		
		_actor.sendDamageMessage(target, hitHolder._damage, false, hitHolder._crit, hitHolder._miss);
		
		if (!hitHolder._miss && hitHolder._damage > 0)
		{
			_actor.getAI().startAttackStance();
			
			if (target.hasAI())
				target.getAI().notifyEvent(AiEventType.ATTACKED, _actor, null);
			
			int reflectedDamage = 0;
			
			// Reflect damage system - do not reflect if weapon is a bow or target is invulnerable
			if (_weaponType != WeaponType.BOW && !target.isInvul())
			{
				// quick fix for no drop from raid if boss attack high-level char with damage reflection
				if (!target.isRaidRelated() || _actor.getActingPlayer() == null || _actor.getActingPlayer().getStatus().getLevel() <= target.getStatus().getLevel() + 8)
				{
					// Calculate reflection damage to reduce HP of attacker if necessary
					final double reflectPercent = target.getStatus().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
					if (reflectPercent > 0)
					{
						reflectedDamage = (int) (reflectPercent / 100. * hitHolder._damage);
						
						if (reflectedDamage > target.getStatus().getMaxHp())
							reflectedDamage = target.getStatus().getMaxHp();
					}
				}
			}
			
			// Reduce target HPs
			target.reduceCurrentHp(hitHolder._damage, _actor, null);
			
			// Reduce attacker HPs in case of a reflect.
			if (reflectedDamage > 0)
				_actor.reduceCurrentHp(reflectedDamage, target, true, false, null);
			
			// Calculate the absorbed HP percentage. Do not absorb if weapon is a bow.
			if (_weaponType != WeaponType.BOW)
			{
				final double absorbPercent = _actor.getStatus().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
					_actor.getStatus().addHp(absorbPercent / 100. * hitHolder._damage);
			}
			
			// Manage cast break of the target (calculating rate, sending message...)
			Formulas.calcCastBreak(target, hitHolder._damage);
			
			// Maybe launch chance skills on us
			final ChanceSkillList chanceSkills = _actor.getChanceSkills();
			if (chanceSkills != null)
			{
				chanceSkills.onHit(target, false, hitHolder._crit);
				
				// Reflect triggers onHit
				if (reflectedDamage > 0)
					chanceSkills.onHit(target, true, false);
			}
			
			// Maybe launch chance skills on target
			if (target.getChanceSkills() != null)
				target.getChanceSkills().onHit(_actor, true, hitHolder._crit);
			
			// Launch weapon Special ability effect if available
			if (hitHolder._crit)
			{
				final Weapon activeWeapon = _actor.getActiveWeaponItem();
				if (activeWeapon != null)
					activeWeapon.castSkillOnCrit(_actor, target);
			}
		}
	}
	
	/**
	 * Launch a physical attack against a {@link Creature}.
	 * @param target : The {@link Creature} used as target.
	 * @return True if the hit was actually successful, false otherwise.
	 */
	public boolean doAttack(Creature target)
	{
		final int timeAtk = Formulas.calculateTimeBetweenAttacks(_actor);
		final Weapon weaponItem = _actor.getActiveWeaponItem();
		final Attack attack = new Attack(_actor, _actor.isChargedShot(ShotType.SOULSHOT), (weaponItem != null) ? weaponItem.getCrystalType().getId() : 0);
		
		_actor.getPosition().setHeadingTo(target);
		
		HitHolder[] hits;
		
		switch (_actor.getAttackType())
		{
			case BOW:
				hits = doAttackHitByBow(attack, target, timeAtk, weaponItem);
				break;
			
			case POLE:
				hits = doAttackHitByPole(attack, target, timeAtk / 2);
				break;
			
			case DUAL:
			case DUALFIST:
				hits = doAttackHitByDual(attack, target, timeAtk / 2);
				break;
			
			case FIST:
				hits = (_actor.getSecondaryWeaponItem() instanceof Armor) ? doAttackHitSimple(attack, target, timeAtk / 2) : doAttackHitByDual(attack, target, timeAtk / 2);
				break;
			
			default:
				hits = doAttackHitSimple(attack, target, timeAtk / 2);
				break;
		}
		
		// Process attack, store result.
		final boolean isHit = attack.processHits(hits);
		
		// Check if hit isn't missed ; if we didn't miss the hit, discharge the shoulshots, if any.
		if (isHit)
			_actor.setChargedShot(ShotType.SOULSHOT, false);
		
		if (attack.hasHits())
			_actor.broadcastPacket(attack);
		
		return isHit;
	}
	
	/**
	 * Launch a Bow attack.
	 * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
	 * @param target : The targeted {@link Creature}.
	 * @param sAtk : The Attack Speed of the attacker.
	 * @param weapon : The {@link Weapon} used to retrieve the reuse delay.
	 * @return An array of generated {@link HitHolder}s.
	 */
	private HitHolder[] doAttackHitByBow(Attack attack, Creature target, int sAtk, Weapon weapon)
	{
		_actor.reduceArrowCount();
		_actor.getStatus().reduceMp(_actor.getActiveWeaponItem().getMpConsume());
		
		final HitHolder[] hits = new HitHolder[]
		{
			getHitHolder(attack, target, false)
		};
		
		int reuse = weapon.getReuseDelay();
		if (reuse != 0)
			reuse = (reuse * 345) / _actor.getStatus().getPAtkSpd();
		
		setAttackTask(hits, WeaponType.BOW, reuse);
		
		_attackTask = ThreadPool.schedule(this::onHitTimer, sAtk);
		
		if (_actor instanceof Player)
		{
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.GETTING_READY_TO_SHOOT_AN_ARROW));
			_actor.sendPacket(new SetupGauge(GaugeColor.RED, sAtk + reuse));
		}
		return hits;
	}
	
	/**
	 * Launch a Dual wield attack.
	 * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
	 * @param target : The targeted {@link Creature}.
	 * @param sAtk : The Attack Speed of the attacker.
	 * @return An array of generated {@link HitHolder}s.
	 */
	private HitHolder[] doAttackHitByDual(Attack attack, Creature target, int sAtk)
	{
		final HitHolder[] hits = new HitHolder[]
		{
			getHitHolder(attack, target, true),
			getHitHolder(attack, target, true)
		};
		
		setAttackTask(hits, WeaponType.DUAL, sAtk / 2);
		
		_attackTask = ThreadPool.schedule(this::onHitTimer, sAtk / 2);
		
		return hits;
	}
	
	/**
	 * Launch a Pole attack.
	 * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
	 * @param target : The targeted {@link Creature}.
	 * @param sAtk : The Attack Speed of the attacker.
	 * @return An array of generated {@link HitHolder}s.
	 */
	private HitHolder[] doAttackHitByPole(Attack attack, Creature target, int sAtk)
	{
		final ArrayList<HitHolder> hitHolders = new ArrayList<>();
		hitHolders.add(getHitHolder(attack, target, false));
		
		final int maxAttackedCount;
		if (_actor.getFirstEffect(EffectType.POLEARM_TARGET_SINGLE) != null)
			maxAttackedCount = 1;
		else
			maxAttackedCount = (int) _actor.getStatus().calcStat(Stats.ATTACK_COUNT_MAX, 0, null, null);
		
		if (maxAttackedCount > 1)
		{
			final int maxAngleDiff = (int) _actor.getStatus().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
			final boolean isMainTargetPlayable = target instanceof Playable;
			
			int attackedCount = 1;
			
			for (Creature knownCreature : _actor.getKnownTypeInRadius(Creature.class, _actor.getStatus().getPhysicalAttackRange()))
			{
				if (knownCreature == target)
					continue;
				
				if (!_actor.isFacing(knownCreature, maxAngleDiff))
					continue;
				
				if (!knownCreature.isAttackableBy(_actor))
					continue;
				
				if (_actor instanceof Playable && knownCreature instanceof Playable && (knownCreature.isInsideZone(ZoneId.PEACE) || !isMainTargetPlayable || !knownCreature.isAttackableWithoutForceBy((Playable) _actor)))
					continue;
				
				attackedCount++;
				if (attackedCount > maxAttackedCount)
					break;
				
				hitHolders.add(getHitHolder(attack, knownCreature, false));
			}
		}
		
		final HitHolder[] hits = hitHolders.toArray(new HitHolder[] {});
		
		setAttackTask(hits, WeaponType.POLE, sAtk);
		
		_attackTask = ThreadPool.schedule(this::onHitTimer, sAtk);
		
		return hits;
	}
	
	/**
	 * Launch a simple attack.
	 * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
	 * @param target : The targeted {@link Creature}.
	 * @param sAtk : The Attack Speed of the attacker.
	 * @return An array of generated {@link HitHolder}s.
	 */
	private HitHolder[] doAttackHitSimple(Attack attack, Creature target, int sAtk)
	{
		final HitHolder[] hits = new HitHolder[]
		{
			getHitHolder(attack, target, false)
		};
		
		setAttackTask(hits, WeaponType.ETC, sAtk);
		
		_attackTask = ThreadPool.schedule(this::onHitTimer, sAtk);
		
		return hits;
	}
	
	/**
	 * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
	 * @param target : The targeted {@link Creature}.
	 * @param isSplit : If true, damages will be split in 2. Used for dual wield attacks.
	 * @return a new {@link HitHolder} with generated damage, shield resistance, critical and miss informations.
	 */
	private HitHolder getHitHolder(Attack attack, Creature target, boolean isSplit)
	{
		boolean crit = false;
		ShieldDefense shld = ShieldDefense.FAILED;
		int damage = 0;
		
		final boolean miss = Formulas.calcHitMiss(_actor, target);
		if (!miss)
		{
			crit = Formulas.calcCrit(_actor, target, null);
			shld = Formulas.calcShldUse(_actor, target, null, crit);
			damage = (int) Formulas.calcPhysicalAttackDamage(_actor, target, shld, crit, attack.soulshot);
			
			if (isSplit)
				damage /= 2;
		}
		
		return new HitHolder(target, damage, crit, miss, shld);
	}
	
	/**
	 * Abort the current attack of the {@link Creature} and send {@link ActionFailed} packet.
	 */
	public final void stop()
	{
		if (_attackTask != null)
		{
			_attackTask.cancel(false);
			_attackTask = null;
		}
		
		clearAttackTask(true);
		
		_actor.getAI().tryToActive();
		_actor.getAI().clientActionFailed();
	}
	
	/**
	 * Abort the current attack and send {@link SystemMessageId#ATTACK_FAILED} to the {@link Creature}.
	 */
	public void interrupt()
	{
		if (isAttackingNow())
		{
			stop();
			_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
		}
	}
	
	private void setAttackTask(HitHolder[] hitHolders, WeaponType weaponType, int afterAttackDelay)
	{
		_isAttackingNow = true;
		_isBowCoolingDown = (weaponType == WeaponType.BOW);
		_hitHolders = hitHolders;
		_weaponType = weaponType;
		_afterAttackDelay = afterAttackDelay;
	}
	
	private void clearAttackTask(boolean clearBowCooldown)
	{
		_isAttackingNow = false;
		
		if (clearBowCooldown)
			_isBowCoolingDown = false;
	}
	
	public static class HitHolder
	{
		public Creature _target;
		public int _targetId;
		public int _damage;
		public boolean _crit;
		public boolean _miss;
		public ShieldDefense _sDef;
		public int _flags;
		
		public HitHolder(Creature target, int damage, boolean crit, boolean miss, ShieldDefense sDef)
		{
			_target = target;
			_targetId = target.getObjectId();
			_damage = damage;
			_crit = crit;
			_sDef = sDef;
			_miss = miss;
		}
	}
}