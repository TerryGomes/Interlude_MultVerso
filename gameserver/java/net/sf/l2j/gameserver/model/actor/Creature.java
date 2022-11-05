package net.sf.l2j.gameserver.model.actor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.MoveType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.ai.type.AttackableAI;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.attack.CreatureAttack;
import net.sf.l2j.gameserver.model.actor.cast.CreatureCast;
import net.sf.l2j.gameserver.model.actor.container.creature.ChanceSkillList;
import net.sf.l2j.gameserver.model.actor.container.creature.EffectList;
import net.sf.l2j.gameserver.model.actor.container.creature.FusionSkill;
import net.sf.l2j.gameserver.model.actor.move.CreatureMove;
import net.sf.l2j.gameserver.model.actor.status.CreatureStatus;
import net.sf.l2j.gameserver.model.actor.template.CreatureTemplate;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.WaterZone;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ChangeMoveType;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.Revive;
import net.sf.l2j.gameserver.network.serverpackets.ServerObjectInfo;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.TeleportToLocation;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Calculator;
import net.sf.l2j.gameserver.skills.IChanceSkillTrigger;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkAccuracy;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkCritical;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkEvasion;
import net.sf.l2j.gameserver.skills.funcs.FuncMAtkCritical;
import net.sf.l2j.gameserver.skills.funcs.FuncMAtkMod;
import net.sf.l2j.gameserver.skills.funcs.FuncMAtkSpeed;
import net.sf.l2j.gameserver.skills.funcs.FuncMDefMod;
import net.sf.l2j.gameserver.skills.funcs.FuncMaxHpMul;
import net.sf.l2j.gameserver.skills.funcs.FuncMaxMpMul;
import net.sf.l2j.gameserver.skills.funcs.FuncMoveSpeed;
import net.sf.l2j.gameserver.skills.funcs.FuncPAtkMod;
import net.sf.l2j.gameserver.skills.funcs.FuncPAtkSpeed;
import net.sf.l2j.gameserver.skills.funcs.FuncPDefMod;
import net.sf.l2j.gameserver.skills.funcs.FuncRegenHpMul;
import net.sf.l2j.gameserver.skills.funcs.FuncRegenMpMul;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * An instance type extending {@link WorldObject} which represents the mother class of all character objects of the world such as players, NPCs and monsters.
 */
public abstract class Creature extends WorldObject
{
	protected String _title;
	
	protected volatile CreatureAI _ai;
	
	private CreatureTemplate _template;
	private NpcTemplate _polymorphTemplate;
	
	protected CreatureStatus<? extends Creature> _status;
	protected CreatureMove<? extends Creature> _move;
	protected CreatureAttack<? extends Creature> _attack;
	protected CreatureCast<? extends Creature> _cast;
	
	private WorldObject _target;
	
	private boolean _isImmobilized;
	private boolean _isParalyzed;
	private boolean _isDead;
	private boolean _isRunning;
	private boolean _isTeleporting;
	private boolean _showSummonAnimation;
	
	private boolean _isInvul;
	private boolean _isMortal = true;
	
	private final Calculator[] _calculators;
	
	private ChanceSkillList _chanceSkills;
	private FusionSkill _fusionSkill;
	
	private final byte[] _zones = new byte[ZoneId.VALUES.length];
	protected byte _zoneValidateCounter = 4;
	
	protected final EffectList _effects = new EffectList(this);
	private int _abnormalEffects;
	
	private final Map<Integer, Long> _disabledSkills = new ConcurrentHashMap<>();
	private boolean _allSkillsDisabled;
	
	public Creature(int objectId, CreatureTemplate template)
	{
		super(objectId);
		
		_template = template;
		_calculators = new Calculator[Stats.NUM_STATS];
		
		addFuncsToNewCharacter();
		
		setStatus();
		setMove();
		setAttack();
		setCast();
	}
	
	/**
	 * Broadcast packet related to state of abnormal effect of this {@link Creature}.
	 */
	public abstract void updateAbnormalEffect();
	
	/**
	 * @return The {@link ItemInstance} equipped in the right hand of this {@link Creature}.
	 */
	public abstract ItemInstance getActiveWeaponInstance();
	
	/**
	 * @return The {@link Weapon} equipped in the right hand of this {@link Creature}.
	 */
	public abstract Weapon getActiveWeaponItem();
	
	/**
	 * @return The {@link ItemInstance} equipped in the left hand of this {@link Creature}.
	 */
	public abstract ItemInstance getSecondaryWeaponInstance();
	
	/**
	 * @return The {@link Item} equiped in the left hand of this {@link Creature}.
	 */
	public abstract Item getSecondaryWeaponItem();
	
	@Override
	public String toString()
	{
		return "[Creature objId=" + getObjectId() + "]";
	}
	
	/**
	 * Set all related {@link Func}s of this {@link Creature}.
	 */
	public void addFuncsToNewCharacter()
	{
		addStatFunc(FuncPAtkMod.getInstance());
		addStatFunc(FuncMAtkMod.getInstance());
		addStatFunc(FuncPDefMod.getInstance());
		addStatFunc(FuncMDefMod.getInstance());
		
		addStatFunc(FuncMaxHpMul.getInstance());
		addStatFunc(FuncMaxMpMul.getInstance());
		addStatFunc(FuncRegenHpMul.getInstance());
		addStatFunc(FuncRegenMpMul.getInstance());
		
		addStatFunc(FuncAtkAccuracy.getInstance());
		addStatFunc(FuncAtkEvasion.getInstance());
		
		addStatFunc(FuncPAtkSpeed.getInstance());
		addStatFunc(FuncMAtkSpeed.getInstance());
		
		addStatFunc(FuncMoveSpeed.getInstance());
		
		addStatFunc(FuncAtkCritical.getInstance());
		addStatFunc(FuncMAtkCritical.getInstance());
	}
	
	/**
	 * Remove the Creature from the world when the decay task is launched.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _objects of World.</B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players.</B></FONT>
	 */
	public void onDecay()
	{
		decayMe();
	}
	
	public void onTeleported()
	{
		if (!isTeleporting())
			return;
		
		setTeleporting(false);
		
		setRegion(World.getInstance().getRegion(getPosition()));
	}
	
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(String process, int itemId, int count, WorldObject reference, boolean sendMessage)
	{
		return true;
	}
	
	public boolean destroyItem(String process, int objectId, int count, WorldObject reference, boolean sendMessage)
	{
		return true;
	}
	
	@Override
	public boolean isInsideZone(ZoneId zone)
	{
		return zone == ZoneId.PVP ? _zones[ZoneId.PVP.getId()] > 0 && _zones[ZoneId.PEACE.getId()] == 0 : _zones[zone.getId()] > 0;
	}
	
	public void setInsideZone(ZoneId zone, boolean state)
	{
		if (state)
			_zones[zone.getId()]++;
		else
		{
			_zones[zone.getId()]--;
			if (_zones[zone.getId()] < 0)
				_zones[zone.getId()] = 0;
		}
	}
	
	/**
	 * @return true if the player is GM.
	 */
	public boolean isGM()
	{
		return false;
	}
	
	/**
	 * Send a {@link L2GameServerPacket} to all known {@link Player}s.
	 * @param packet : The packet to send.
	 */
	public void broadcastPacket(L2GameServerPacket packet)
	{
		broadcastPacket(packet, true);
	}
	
	/**
	 * Send a {@link L2GameServerPacket} to all known {@link Player}s. Overidden on Player, which uses selfToo boolean flag to send the packet to self.
	 * @param packet : The packet to send.
	 * @param selfToo : If true, we also send it to self.
	 */
	public void broadcastPacket(L2GameServerPacket packet, boolean selfToo)
	{
		for (final Player player : getKnownType(Player.class))
			player.sendPacket(packet);
	}
	
	/**
	 * Send a {@link L2GameServerPacket} to self and to all known {@link Player}s in a given radius. Overidden on Player, which also send the packet to self.
	 * @param packet : The packet to send.
	 * @param radius : The radius to check.
	 */
	public void broadcastPacketInRadius(L2GameServerPacket packet, int radius)
	{
		if (radius < 0)
			radius = 600;
		
		for (final Player player : getKnownTypeInRadius(Player.class, radius))
			player.sendPacket(packet);
	}
	
	/**
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>Player</li><BR>
	 * <BR>
	 * @param mov The packet to send.
	 */
	public void sendPacket(L2GameServerPacket mov)
	{
		// default implementation
	}
	
	/**
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>Player</li><BR>
	 * <BR>
	 * @param text The string to send.
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}
	
	/**
	 * Instantly teleport this {@link Creature} to defined coordinates X/Y/Z.<br>
	 * <br>
	 * <b>BEWARE : has to be used on really short distances (mostly only skills), since there isn't any region edit.</b>.
	 * @param x : The X coord to set.
	 * @param y : The Y coord to set.
	 * @param z : The Z coord to set.
	 * @param randomOffset : If > 0, we randomize the teleport location.
	 */
	public void instantTeleportTo(int x, int y, int z, int randomOffset)
	{
		if (randomOffset > 0)
		{
			// Get new coordinates.
			final int nx = x + Rnd.get(-randomOffset, randomOffset);
			final int ny = y + Rnd.get(-randomOffset, randomOffset);
			
			// Validate new coordinates.
			final Location loc = GeoEngine.getInstance().getValidLocation(x, y, z, nx, ny, z, null);
			x = loc.getX();
			y = loc.getY();
		}
		
		// Validate Z coordinate, if not flying or target coordinates are not inside water (creature would be swimming).
		if (!isFlying() && ZoneManager.getInstance().getZone(x, y, z, WaterZone.class) == null)
			z = GeoEngine.getInstance().getHeight(x, y, z);
		
		// Broadcast TeleportToLocation packet.
		broadcastPacket(new TeleportToLocation(this, x, y, z, true));
		
		// Set the position.
		getPosition().set(x, y, z);
		
		getAI().notifyEvent(AiEventType.TELEPORTED, null, null);
		
		// Refresh knownlist.
		refreshKnownlist();
	}
	
	/**
	 * Instantly teleport this {@link Creature} to a defined {@link Location}.<br>
	 * <br>
	 * <b>BEWARE : has to be used on really short distances (mostly only skills), since there isn't any region edit.</b>.
	 * @param loc : The Location to teleport to.
	 * @param randomOffset : If > 0, we randomize the teleport location.
	 */
	public void instantTeleportTo(Location loc, int randomOffset)
	{
		instantTeleportTo(loc.getX(), loc.getY(), loc.getZ(), randomOffset);
	}
	
	/**
	 * Teleport this {@link Creature} to defined coordinates X/Y/Z.
	 * @param x : The X coord to set.
	 * @param y : The Y coord to set.
	 * @param z : The Z coord to set.
	 * @param randomOffset : If > 0, we randomize the teleport location.
	 */
	public void teleportTo(int x, int y, int z, int randomOffset)
	{
		// Abort attack, cast and move.
		abortAll(true);
		
		setTeleporting(true);
		
		if (randomOffset > 0)
		{
			// Get new coordinates.
			final int nx = x + Rnd.get(-randomOffset, randomOffset);
			final int ny = y + Rnd.get(-randomOffset, randomOffset);
			
			// Validate new coordinates.
			final Location loc = GeoEngine.getInstance().getValidLocation(x, y, z, nx, ny, z, null);
			x = loc.getX();
			y = loc.getY();
		}
		
		// Validate Z coordinate, if not flying or target coordinates are not inside water (creature would be swimming).
		if (!isFlying() && ZoneManager.getInstance().getZone(x, y, z, WaterZone.class) == null)
			z = GeoEngine.getInstance().getHeight(x, y, z);
		
		// Broadcast TeleportToLocation packet.
		broadcastPacket(new TeleportToLocation(this, x, y, z, false));
		
		// Remove the object from its old location.
		setRegion(null);
		
		// Set the position.
		getPosition().set(x, y, z);
		
		// Handle onTeleported behavior, but only if it's not a Player. Players are handled from Appearing packet.
		if (!(this instanceof Player) || (((Player) this).getClient() != null && ((Player) this).getClient().isDetached()))
			onTeleported();
		
		getAI().notifyEvent(AiEventType.TELEPORTED, null, null);
	}
	
	/**
	 * Teleport this {@link Creature} to a defined {@link Location}.
	 * @param loc : The Location to teleport to.
	 * @param randomOffset : If > 0, we randomize the teleport location.
	 */
	public void teleportTo(Location loc, int randomOffset)
	{
		teleportTo(loc.getX(), loc.getY(), loc.getZ(), randomOffset);
	}
	
	/**
	 * Teleport this {@link Creature} to a defined {@link TeleportType} (CASTLE, CLAN_HALL, SIEGE_FLAG, TOWN).
	 * @param type : The TeleportType to teleport to.
	 */
	public void teleportTo(TeleportType type)
	{
		teleportTo(MapRegionData.getInstance().getLocationToTeleport(this, type), 20);
	}
	
	/**
	 * Index according to skill id the current timestamp of use, overridden in Player.
	 * @param skill id
	 * @param reuse delay
	 */
	public void addTimeStamp(L2Skill skill, long reuse)
	{
	}
	
	public void startFusionSkill(Creature target, L2Skill skill)
	{
		if (_fusionSkill == null)
			_fusionSkill = new FusionSkill(this, target, skill);
	}
	
	/**
	 * Kill this {@link Creature}.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the Creature</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform</li>
	 * <li>Notify Creature AI</li>
	 * </ul>
	 * <B><U> Overridden in </U> :</B>
	 * <ul>
	 * <li>Npc : Create a DecayTask to remove the corpse of the Npc after 7 seconds</li>
	 * <li>Attackable : Distribute rewards (EXP, SP, Drops...) and notify Quest Engine</li>
	 * <li>Player : Apply Death Penalty, Manage gain/loss Karma and Item Drop</li>
	 * </ul>
	 * @param killer : The Creature who killed it
	 * @return true if successful.
	 */
	public boolean doDie(Creature killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;
			
			// now reset currentHp to zero
			getStatus().setHp(0);
			
			setIsDead(true);
		}
		
		// Abort attack, cast and move.
		abortAll(true);
		
		// Stop Regeneration task, and removes all current effects
		getStatus().stopHpMpRegeneration();
		stopAllEffectsExceptThoseThatLastThroughDeath();
		
		calculateRewards(killer);
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform
		getStatus().broadcastStatusUpdate();
		
		// Notify Creature AI
		if (hasAI())
			getAI().notifyEvent(AiEventType.DEAD, null, null);
		
		return true;
	}
	
	public void deleteMe()
	{
		getStatus().stopHpMpRegeneration();
		
		if (hasAI())
			getAI().stopAITask();
	}
	
	public void detachAI()
	{
		_ai = null;
	}
	
	/**
	 * Distribute rewards to any {@link Playable}s who participated to the kill of this instance.
	 * @param creature : The {@link Creature} who killed this instance.
	 */
	protected void calculateRewards(Creature creature)
	{
	}
	
	/** Sets HP, MP and CP and revives the Creature. */
	public void doRevive()
	{
		if (!isDead() || isTeleporting())
			return;
		
		setIsDead(false);
		
		_status.setHp(_status.getMaxHp() * Config.RESPAWN_RESTORE_HP);
		
		// Start broadcast status
		broadcastPacket(new Revive(this));
	}
	
	/**
	 * Revives the Creature using skill.
	 * @param revivePower
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	/**
	 * @return the CreatureAI of the Creature and if its null create a new one.
	 */
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				ai = _ai;
				if (ai == null)
					_ai = ai = new CreatureAI(this);
			}
		}
		return ai;
	}
	
	public void setAI(CreatureAI newAI)
	{
		final CreatureAI oldAI = getAI();
		if (oldAI != null && oldAI != newAI && oldAI instanceof AttackableAI)
			((AttackableAI) oldAI).stopAITask();
		
		_ai = newAI;
	}
	
	/**
	 * @return true if this object has a running AI.
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/**
	 * @return true if this object is a raid boss.
	 */
	public boolean isRaidBoss()
	{
		return false;
	}
	
	/**
	 * @return true if this object is either a raid minion or a raid boss.
	 */
	public boolean isRaidRelated()
	{
		return false;
	}
	
	/**
	 * @return true if this object is a minion.
	 */
	public boolean isMinion()
	{
		return false;
	}
	
	public final boolean isAfraid()
	{
		return isAffected(EffectFlag.FEAR);
	}
	
	public final boolean isConfused()
	{
		return isAffected(EffectFlag.CONFUSED);
	}
	
	public final boolean isMuted()
	{
		return isAffected(EffectFlag.MUTED);
	}
	
	public final boolean isPhysicalMuted()
	{
		return isAffected(EffectFlag.PHYSICAL_MUTED);
	}
	
	public final boolean isRooted()
	{
		return isAffected(EffectFlag.ROOTED);
	}
	
	public final boolean isSleeping()
	{
		return isAffected(EffectFlag.SLEEP);
	}
	
	public final boolean isStunned()
	{
		return isAffected(EffectFlag.STUNNED);
	}
	
	public final boolean isBetrayed()
	{
		return isAffected(EffectFlag.BETRAYED);
	}
	
	public final boolean isImmobileUntilAttacked()
	{
		return isAffected(EffectFlag.MEDITATING);
	}
	
	/**
	 * @return True if this {@link Creature} can't use its skills.
	 */
	public final boolean isAllSkillsDisabled()
	{
		return getAllSkillsDisabled() || isStunned() || isImmobileUntilAttacked() || isSleeping() || isParalyzed();
	}
	
	/**
	 * @return True if this {@link Creature} can't attack.
	 */
	public boolean isAttackingDisabled()
	{
		return isFlying() || isStunned() || isImmobileUntilAttacked() || isSleeping() || isParalyzed() || isAlikeDead();
	}
	
	/**
	 * @return True if this {@link Creature} can't perform an action NOW.
	 */
	public boolean denyAiAction()
	{
		return isStunned() || isImmobileUntilAttacked() || isSleeping() || isParalyzed() || isTeleporting() || isDead();
	}
	
	/**
	 * @return True if this {@link Creature} is in a state where it can't move.
	 */
	public boolean isMovementDisabled()
	{
		return isStunned() || isImmobileUntilAttacked() || isRooted() || isSleeping() || isParalyzed() || isImmobilized() || isAlikeDead() || isTeleporting() || isSitting() || isSittingNow() || isStandingNow();
	}
	
	/**
	 * @return True if this {@link Creature} is in a state where he can't be controlled.
	 */
	public boolean isOutOfControl()
	{
		return isStunned() || isImmobileUntilAttacked() || isSleeping() || isParalyzed() || isAfraid() || isConfused() || isTeleporting() || isDead();
	}
	
	public final Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	/**
	 * @return True if this {@link Creature} is dead or use fake death.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	public boolean isFakeDeath()
	{
		return false;
	}
	
	/**
	 * @return True if this {@link Creature} is dead.
	 */
	public final boolean isDead()
	{
		return _isDead;
	}
	
	@Override
	public boolean isAttackableBy(Creature attacker)
	{
		return !isDead() && attacker != this;
	}
	
	@Override
	public boolean isAttackableWithoutForceBy(Playable attacker)
	{
		return false;
	}
	
	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}
	
	public final boolean isParalyzed()
	{
		return _isParalyzed || isAffected(EffectFlag.PARALYZED);
	}
	
	public final void setIsParalyzed(boolean value)
	{
		_isParalyzed = value;
	}
	
	/**
	 * Overriden in {@link Player}.
	 * @return the {@link Summon} of this {@link Creature}.
	 */
	public Summon getSummon()
	{
		return null;
	}
	
	public boolean isOperating()
	{
		return false;
	}
	
	public boolean isSeated()
	{
		return false;
	}
	
	public boolean isStandingNow()
	{
		return false;
	}
	
	public boolean isStanding()
	{
		return false;
	}
	
	public boolean isSittingNow()
	{
		return false;
	}
	
	public boolean isSitting()
	{
		return false;
	}
	
	public boolean isRiding()
	{
		return false;
	}
	
	public boolean isFlying()
	{
		return false;
	}
	
	public final boolean isRunning()
	{
		return _isRunning;
	}
	
	/**
	 * Make this {@link Creature} walk/run and send related packets to all {@link Player}s.
	 * @param value : If false, the {@link Creature} will walk. If true, it will run.
	 */
	public void setWalkOrRun(boolean value)
	{
		_isRunning = value;
		
		if (_status.getMoveSpeed() != 0)
			broadcastPacket(new ChangeMoveType(this));
	}
	
	/**
	 * Force this {@link Creature} to walk. Do nothing if already walking.
	 */
	public final void forceWalkStance()
	{
		if (isRunning())
			setWalkOrRun(false);
	}
	
	/**
	 * Force this {@link Creature} to run. Do nothing if already running.
	 */
	public final void forceRunStance()
	{
		if (!isRunning())
			setWalkOrRun(true);
	}
	
	/**
	 * @return True if this {@link Creature} is currently teleporting, false otherwise.
	 */
	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public final void setTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	/**
	 * @return True if this {@link Creature} is invulnerable, false otherwise. If invulnerable, HPs can't decrease and effects, bad or good, can't be applied.
	 */
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}
	
	public void setInvul(boolean value)
	{
		_isInvul = value;
	}
	
	/**
	 * @return True if this {@link Creature} is mortal, false otherwise. If immortal, HPs can't drop lower than 1.
	 */
	public boolean isMortal()
	{
		return _isMortal;
	}
	
	public void setMortal(boolean value)
	{
		_isMortal = value;
	}
	
	/**
	 * @return True if this {@link Creature} is undead. Overidden in {@link Npc}.
	 */
	public boolean isUndead()
	{
		return false;
	}
	
	public CreatureStatus<? extends Creature> getStatus()
	{
		return _status;
	}
	
	public void setStatus()
	{
		_status = new CreatureStatus<>(this);
	}
	
	public CreatureMove<? extends Creature> getMove()
	{
		return _move;
	}
	
	public void setMove()
	{
		_move = new CreatureMove<>(this);
	}
	
	public CreatureAttack<? extends Creature> getAttack()
	{
		return _attack;
	}
	
	public void setAttack()
	{
		_attack = new CreatureAttack<>(this);
	}
	
	public CreatureCast<? extends Creature> getCast()
	{
		return _cast;
	}
	
	public void setCast()
	{
		_cast = new CreatureCast<>(this);
	}
	
	public CreatureTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Set the {@link CreatureTemplate} of this {@link Creature}.
	 * @param template : The {@link CreatureTemplate} to set.
	 */
	protected final void setTemplate(CreatureTemplate template)
	{
		_template = template;
	}
	
	/**
	 * @return The title of this {@link Creature}.
	 */
	public final String getTitle()
	{
		return _title;
	}
	
	/**
	 * Set the title of this {@link Creature}. Concatenate it if the length is > 16.
	 * @param value : The {@link String} to set.
	 */
	public void setTitle(String value)
	{
		_title = StringUtil.trim(value, 16, "");
	}
	
	/**
	 * In Server->Client packet, each effect is represented by 1 bit of the map (ex : BLEEDING = 0x0001 (bit 1), SLEEP = 0x0080 (bit 8)...). The map is calculated by applying a BINARY OR operation on each effect.
	 * @return a map of 16 bits (0x0000) containing all abnormal effect in progress for this Creature.
	 */
	public int getAbnormalEffect()
	{
		int ae = _abnormalEffects;
		if (isStunned())
			ae |= AbnormalEffect.STUN.getMask();
		if (isRooted())
			ae |= AbnormalEffect.ROOT.getMask();
		if (isSleeping())
			ae |= AbnormalEffect.SLEEP.getMask();
		if (isConfused())
			ae |= AbnormalEffect.FEAR.getMask();
		if (isAfraid())
			ae |= AbnormalEffect.FEAR.getMask();
		if (isMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isPhysicalMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isImmobileUntilAttacked())
			ae |= AbnormalEffect.FLOATING_ROOT.getMask();
		
		return ae;
	}
	
	public final void startAbnormalEffect(AbnormalEffect mask)
	{
		_abnormalEffects |= mask.getMask();
		updateAbnormalEffect();
	}
	
	public final void startAbnormalEffect(int mask)
	{
		_abnormalEffects |= mask;
		updateAbnormalEffect();
	}
	
	public final void stopAbnormalEffect(AbnormalEffect mask)
	{
		_abnormalEffects &= ~mask.getMask();
		updateAbnormalEffect();
	}
	
	public final void stopAbnormalEffect(int mask)
	{
		_abnormalEffects &= ~mask;
		updateAbnormalEffect();
	}
	
	/**
	 * Queue an {@link AbstractEffect} to this {@link Creature}.
	 * @param effect : The {@link AbstractEffect} to add.
	 */
	public void addEffect(AbstractEffect effect)
	{
		_effects.queueEffect(effect, false);
	}
	
	/**
	 * Remove an {@link AbstractEffect} from this {@link Creature}.
	 * @param effect : The {@link AbstractEffect} to remove.
	 */
	public final void removeEffect(AbstractEffect effect)
	{
		_effects.queueEffect(effect, true);
	}
	
	/**
	 * Stop all {@link AbstractEffect}s in progress from this {@link Creature}.
	 */
	public void stopAllEffects()
	{
		_effects.stopAllEffects();
	}
	
	/**
	 * Stop all {@link AbstractEffect}s in progress, except those lasting through death, from this {@link Creature}.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effects.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Stop all {@link AbstractEffect}s corresponding to the {@link L2Skill} id set as parameter.
	 * @param skillId : The {@link L2Skill} id to test.
	 */
	public final void stopSkillEffects(int skillId)
	{
		_effects.stopSkillEffects(skillId);
	}
	
	/**
	 * Stop all {@link AbstractEffect}s corresponding to the {@link L2Skill} id and negateId set as parameters.
	 * @param skillType : The {@link SkillType} to test.
	 * @param negateLvl : The negate id to test.
	 */
	public final void stopSkillEffects(SkillType skillType, int negateLvl)
	{
		_effects.stopSkillEffects(skillType, negateLvl);
	}
	
	/**
	 * Stop all {@link AbstractEffect}s corresponding to the {@link L2Skill} id set as parameter.
	 * @param skillType : The {@link SkillType} to test.
	 * @see #stopSkillEffects(SkillType, int)
	 */
	public final void stopSkillEffects(SkillType skillType)
	{
		_effects.stopSkillEffects(skillType, -1);
	}
	
	/**
	 * Stop all {@link AbstractEffect}s corresponding to the {@link EffectType} set as parameter.
	 * @param type : The {@link EffectType} to test.
	 */
	public final void stopEffects(EffectType type)
	{
		_effects.stopEffects(type);
	}
	
	/**
	 * @return An array of all {@link AbstractEffect}s in progress on this {@link Creature}.
	 */
	public final AbstractEffect[] getAllEffects()
	{
		return _effects.getAllEffects();
	}
	
	/**
	 * @param skillId : The {@link L2Skill} id to test.
	 * @return The first {@link AbstractEffect} corresponding to the {@link L2Skill} id set as parameter.
	 */
	public final AbstractEffect getFirstEffect(int skillId)
	{
		return _effects.getFirstEffect(skillId);
	}
	
	/**
	 * @param skill : The {@link L2Skill} to test.
	 * @return The first {@link AbstractEffect} corresponding to the {@link L2Skill} set as parameter.
	 */
	public final AbstractEffect getFirstEffect(L2Skill skill)
	{
		return _effects.getFirstEffect(skill);
	}
	
	/**
	 * @param type : The {@link EffectType} to test.
	 * @return The first {@link AbstractEffect} corresponding to the {@link EffectType} set as parameter.
	 */
	public final AbstractEffect getFirstEffect(EffectType type)
	{
		return _effects.getFirstEffect(type);
	}
	
	/**
	 * Update active skills in progress (In Use and Not In Use because stacked) icones on client.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icone on the client.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the client of the player and not clients of all players in the party.</B></FONT><BR>
	 * <BR>
	 */
	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Updates Effect Icons for this character(palyer/summon) and his party if any<BR>
	 * Overridden in:
	 * <ul>
	 * <li>Player</li>
	 * <li>L2Summon</li>
	 * </ul>
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}
	
	/**
	 * Add a {@link Func} to the {@link Calculator} set on this {@link Creature}.
	 * @param function : The {@link Func} corresponding to the affected stat.
	 */
	public final void addStatFunc(Func function)
	{
		if (function == null)
			return;
		
		// Select the Calculator of the affected state in the Calculator set
		final int stat = function.getStat().ordinal();
		
		synchronized (_calculators)
		{
			if (_calculators[stat] == null)
				_calculators[stat] = new Calculator();
			
			// Add the Func to the calculator corresponding to the state
			_calculators[stat].addFunc(function);
		}
	}
	
	/**
	 * Add a {@link List} of {@link Func}s to the {@link Calculator} set on this {@link Creature}.
	 * @param funcs : The {@link List} of {@link Func}s corresponding to the affected stat.
	 */
	public final void addStatFuncs(List<Func> funcs)
	{
		final List<Stats> modifiedStats = new ArrayList<>();
		
		for (Func f : funcs)
		{
			modifiedStats.add(f.getStat());
			addStatFunc(f);
		}
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove all {@link Func} associated to an {@link Object} owner from the {@link Calculator} set on this {@link Creature}.
	 * @param owner : The {@link Object} owner.
	 */
	public final void removeStatsByOwner(Object owner)
	{
		List<Stats> modifiedStats = null;
		
		int i = 0;
		// Go through the Calculator set
		synchronized (_calculators)
		{
			for (final Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Delete all Func objects of the selected owner
					if (modifiedStats != null)
						modifiedStats.addAll(calc.removeOwner(owner));
					else
						modifiedStats = calc.removeOwner(owner);
					
					if (calc.size() == 0)
						_calculators[i] = null;
				}
				i++;
			}
			
			if (owner instanceof AbstractEffect)
			{
				if (!((AbstractEffect) owner).cantUpdateAnymore())
					broadcastModifiedStats(modifiedStats);
			}
			else
				broadcastModifiedStats(modifiedStats);
		}
	}
	
	private void broadcastModifiedStats(List<Stats> stats)
	{
		if (stats == null || stats.isEmpty())
			return;
		
		boolean broadcastFull = false;
		StatusUpdate su = null;
		
		if (this instanceof Summon && ((Summon) this).getOwner() != null)
			((Summon) this).updateAndBroadcastStatusAndInfos(1);
		else
		{
			for (final Stats stat : stats)
			{
				if (stat == Stats.POWER_ATTACK_SPEED)
				{
					if (su == null)
						su = new StatusUpdate(this);
					
					su.addAttribute(StatusType.ATK_SPD, _status.getPAtkSpd());
				}
				else if (stat == Stats.MAGIC_ATTACK_SPEED)
				{
					if (su == null)
						su = new StatusUpdate(this);
					
					su.addAttribute(StatusType.CAST_SPD, _status.getMAtkSpd());
				}
				else if (stat == Stats.MAX_HP && this instanceof Attackable)
				{
					if (su == null)
						su = new StatusUpdate(this);
					
					su.addAttribute(StatusType.MAX_HP, _status.getMaxHp());
				}
				else if (stat == Stats.RUN_SPEED)
					broadcastFull = true;
			}
		}
		
		if (this instanceof Player)
		{
			if (broadcastFull)
				((Player) this).updateAndBroadcastStatus(2);
			else
			{
				((Player) this).updateAndBroadcastStatus(1);
				if (su != null)
					broadcastPacket(su);
			}
		}
		else if (this instanceof Npc)
		{
			if (broadcastFull)
			{
				for (final Player player : getKnownType(Player.class))
				{
					if (_status.getMoveSpeed() == 0)
						player.sendPacket(new ServerObjectInfo((Npc) this, player));
					else
						player.sendPacket(new NpcInfo((Npc) this, player));
				}
			}
			else if (su != null)
				broadcastPacket(su);
		}
		else if (su != null)
			broadcastPacket(su);
	}
	
	/**
	 * @return True if the Creature is in combat.
	 */
	public boolean isInCombat()
	{
		return hasAI() && AttackStanceTaskManager.getInstance().isInAttackStance(this);
	}
	
	/**
	 * @return True if the Creature is moving.
	 */
	public final boolean isMoving()
	{
		return getMove().getTask() != null;
	}
	
	/**
	 * Abort potential attack, move and cast launched actions of this {@link Creature}.
	 * @param resetTarget : If true, we also clean up current target.
	 */
	public void abortAll(boolean resetTarget)
	{
		_move.stop();
		_attack.stop();
		_cast.stop();
		
		if (resetTarget)
			setTarget(null);
	}
	
	public boolean isInWater()
	{
		return _move.getMoveType() == MoveType.SWIM;
	}
	
	public void revalidateZone(boolean force)
	{
		if (getRegion() == null)
			return;
		
		// This function is called too often from movement code
		if (force)
			_zoneValidateCounter = 4;
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
				_zoneValidateCounter = 4;
			else
				return;
		}
		getRegion().revalidateZones(this);
	}
	
	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Target an object. If the object is invisible, we set it to null.<br>
	 * <B><U>Overridden in Player</U></B> : Remove the Player from the old target _statusListener and add it to the new target if it was a Creature
	 * @param object WorldObject to target
	 */
	public void setTarget(WorldObject object)
	{
		if (object != null && !object.isVisible())
			object = null;
		
		_target = object;
	}
	
	/**
	 * @return the identifier of the WorldObject targeted or -1.
	 */
	public final int getTargetId()
	{
		return (_target != null) ? _target.getObjectId() : -1;
	}
	
	/**
	 * @return the WorldObject targeted or null.
	 */
	public final WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * @return True if arrows are available.
	 */
	public boolean checkAndEquipArrows()
	{
		return true;
	}
	
	/**
	 * Add Exp and Sp to the Creature.
	 * @param addToExp An int value.
	 * @param addToSp An int value.
	 */
	public void addExpAndSp(long addToExp, int addToSp)
	{
		// Dummy method (overridden by players and pets)
	}
	
	/**
	 * @return the type of attack, depending of the worn weapon.
	 */
	public WeaponType getAttackType()
	{
		final Weapon weapon = getActiveWeaponItem();
		return (weapon == null) ? WeaponType.NONE : weapon.getItemType();
	}
	
	/**
	 * Reduce the arrow number of the Creature.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>Player</li><BR>
	 * <BR>
	 */
	public void reduceArrowCount()
	{
		// default is to do nothing
	}
	
	@Override
	public void onAction(Player player, boolean isCtrlPressed, boolean isShiftPressed)
	{
		// Set the target of the player
		if (player.getTarget() != this)
			player.setTarget(this);
		else
		{
			if (isAttackableWithoutForceBy(player) || (isCtrlPressed && isAttackableBy(player)))
				player.getAI().tryToAttack(this, isCtrlPressed, isShiftPressed);
			else
				player.getAI().tryToInteract(this, isCtrlPressed, isShiftPressed);
		}
	}
	
	/**
	 * @return true if this {@link Creature} is inside an active {@link WorldRegion}.
	 */
	public boolean isInActiveRegion()
	{
		final WorldRegion region = getRegion();
		return region != null && region.isActive();
	}
	
	/**
	 * @return true if this {@link Creature} has a {@link Party} in progress.
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * @return the {@link Party} of this {@link Creature}.
	 */
	public Party getParty()
	{
		return null;
	}
	
	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}
	
	public void removeChanceSkill(int id)
	{
		if (_chanceSkills == null)
			return;
		
		for (final IChanceSkillTrigger trigger : _chanceSkills.keySet())
		{
			if (!(trigger instanceof L2Skill))
				continue;
			
			if (((L2Skill) trigger).getId() == id)
				_chanceSkills.remove(trigger);
		}
	}
	
	public void addChanceTrigger(IChanceSkillTrigger trigger)
	{
		if (_chanceSkills == null)
			_chanceSkills = new ChanceSkillList(this);
		
		_chanceSkills.put(trigger, trigger.getTriggeredChanceCondition());
	}
	
	public void removeChanceEffect(EffectChanceSkillTrigger effect)
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.remove(effect);
	}
	
	public void onStartChanceEffect()
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.onStart();
	}
	
	public void onActionTimeChanceEffect()
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.onActionTime();
	}
	
	public void onExitChanceEffect()
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.onExit();
	}
	
	/**
	 * By default, return an empty immutable map. This method is overidden on {@link Player}, {@link Summon} and {@link Npc}.
	 * @return the skills list of this {@link Creature}.
	 */
	public Map<Integer, L2Skill> getSkills()
	{
		return Collections.emptyMap();
	}
	
	/**
	 * Returns the level of a skill owned by this {@link Creature}.
	 * @param skillId : The skill identifier whose level must be returned.
	 * @return the level of the skill identified by skillId.
	 */
	public int getSkillLevel(int skillId)
	{
		final L2Skill skill = getSkills().get(skillId);
		return (skill == null) ? 0 : skill.getLevel();
	}
	
	/**
	 * @param skillId : The skill identifier to check.
	 * @return the {@link L2Skill} reference if known by this {@link Creature}, or null.
	 */
	public L2Skill getSkill(int skillId)
	{
		return getSkills().get(skillId);
	}
	
	/**
	 * @param skillId : The skill identifier to check.
	 * @return true if the {@link L2Skill} is known by this {@link Creature}, false otherwise.
	 */
	public boolean hasSkill(int skillId)
	{
		return getSkills().containsKey(skillId);
	}
	
	/**
	 * Return the number of skills of type(Buff, Debuff, HEAL_PERCENT, MANAHEAL_PERCENT) affecting this Creature.
	 * @return The number of Buffs affecting this Creature
	 */
	public int getBuffCount()
	{
		return _effects.getBuffCount();
	}
	
	public int getDanceCount()
	{
		return _effects.getDanceCount();
	}
	
	// Quest event ON_SPELL_FINISHED
	public void notifyQuestEventSkillFinished(L2Skill skill, WorldObject target)
	{
	}
	
	public Map<Integer, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	/**
	 * Enable a skill (remove it from _disabledSkills of the Creature).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their skillId in <B>_disabledSkills</B> of the Creature
	 * @param skill The L2Skill to enable
	 */
	public void enableSkill(L2Skill skill)
	{
		if (skill == null)
			return;
		
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Disable this skill id for the duration of the delay in milliseconds.
	 * @param skill
	 * @param delay (seconds * 1000)
	 */
	public void disableSkill(L2Skill skill, long delay)
	{
		if (skill == null)
			return;
		
		_disabledSkills.put(skill.getReuseHashCode(), (delay > 10) ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	/**
	 * Check if a skill is disabled. All skills disabled are identified by their reuse hashcodes in <B>_disabledSkills</B>.
	 * @param skill The L2Skill to check
	 * @return true if the skill is currently disabled.
	 */
	public boolean isSkillDisabled(L2Skill skill)
	{
		if (_disabledSkills.isEmpty())
			return false;
		
		if (skill == null || isAllSkillsDisabled())
			return true;
		
		final int hashCode = skill.getReuseHashCode();
		
		final Long timeStamp = _disabledSkills.get(hashCode);
		if (timeStamp == null)
			return false;
		
		if (timeStamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Disable all skills (set _allSkillsDisabled to True).
	 */
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enable all skills (set _allSkillsDisabled to False).
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	public boolean getAllSkillsDisabled()
	{
		return _allSkillsDisabled;
	}
	
	// =========================================================
	// Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
	// Method - Public
	
	public void reduceCurrentHp(double i, Creature attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, true, false, skill);
	}
	
	public void reduceCurrentHpByDOT(double i, Creature attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, !skill.isToggle(), true, skill);
	}
	
	public void reduceCurrentHp(double i, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		getStatus().reduceHp(i, attacker, awake, isDOT, false);
	}
	
	/**
	 * Send system message about damage.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B>
	 * <ul>
	 * <li>Player</li>
	 * <li>Servitor</li>
	 * <li>Pet</li>
	 * </ul>
	 * @param target
	 * @param damage
	 * @param mcrit
	 * @param pcrit
	 * @param miss
	 */
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
	}
	
	public FusionSkill getFusionSkill()
	{
		return _fusionSkill;
	}
	
	public void setFusionSkill(FusionSkill fb)
	{
		_fusionSkill = fb;
	}
	
	/**
	 * Check if target is affected with special buff
	 * @see EffectList#isAffected(EffectFlag)
	 * @param flag int
	 * @return boolean
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return _effects.isAffected(flag);
	}
	
	/**
	 * Check player max buff count
	 * @return max buff count
	 */
	public int getMaxBuffCount()
	{
		return Config.MAX_BUFFS_AMOUNT + getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION);
	}
	
	/**
	 * @return a multiplier based on weapon random damage.
	 */
	public final double getRandomDamageMultiplier()
	{
		final Weapon activeWeapon = getActiveWeaponItem();
		int random;
		
		if (activeWeapon != null)
			random = activeWeapon.getRandomDamage();
		else
			random = 5 + (int) Math.sqrt(getStatus().getLevel());
		
		return (1 + ((double) Rnd.get(0 - random, random) / 100));
	}
	
	/**
	 * @return true if the character is located in an arena (aka a PvP zone which isn't a siege).
	 */
	public boolean isInArena()
	{
		return false;
	}
	
	public double getCollisionRadius()
	{
		return getTemplate().getCollisionRadius();
	}
	
	public double getCollisionHeight()
	{
		return getTemplate().getCollisionHeight();
	}
	
	/**
	 * Calculate a new {@link Location} to go in opposite side of the {@link Creature} reference.<br>
	 * <br>
	 * This method is perfect to calculate fleeing characters position.
	 * @param attacker : The {@link Creature} used as reference.
	 * @param distance : The distance to flee.
	 */
	public void fleeFrom(Creature attacker, int distance)
	{
		// No attacker or distance isn't noticeable ; return instantly.
		if (attacker == null || distance < 10)
			return;
		
		// Enforce running state.
		forceRunStance();
		
		// Generate a Location and calculate the destination.
		final Location loc = getPosition().clone();
		loc.setFleeing(attacker.getPosition(), distance);
		
		// Try to move to the position.
		getAI().tryToMoveTo(loc, null);
	}
	
	/**
	 * Move this {@link Creature} from its current {@link Location} using a defined random offset. The {@link Creature} will circle around the initial location.
	 * @param offset : The random offset used.
	 */
	public void moveUsingRandomOffset(int offset)
	{
		// Offset isn't noticeable ; return instantly.
		if (offset < 10)
			return;
		
		// Generate a new Location and calculate the destination.
		final Location loc = getPosition().clone();
		loc.addRandomOffset(offset);
		
		// Try to move to the position.
		getAI().tryToMoveTo(loc, null);
	}
	
	@Override
	public final void setRegion(WorldRegion newRegion)
	{
		// If old region exists.
		if (getRegion() != null)
		{
			// No new region is set, we delete directly from current region zones.
			if (newRegion == null)
				getRegion().removeFromZones(this);
			// If a different region is set, we test old region zones to see if we're still on it or no.
			else if (newRegion != getRegion())
				getRegion().revalidateZones(this);
		}
		
		// Update the zone, send the knownlist.
		super.setRegion(newRegion);
		
		// Revalidate current zone (used instead of "getRegion().revalidateZones(this)" because it's overidden on Player).
		revalidateZone(true);
	}
	
	@Override
	public void removeKnownObject(WorldObject object)
	{
		// If object is targeted by the Creature, cancel Attack or Cast
		if (object == getTarget())
			setTarget(null);
	}
	
	/**
	 * @return The {@link List} of GMs {@link Player}s in surrounding regions.
	 */
	public List<Player> getSurroundingGMs()
	{
		return getKnownType(Player.class, Player::isGM);
	}
	
	/**
	 * Test and cast curses once a {@link Creature} attacks this {@link Creature}.<br>
	 * <br>
	 * <font color=red>BEWARE :
	 * <ul>
	 * <li>no Playable checks are made</li>
	 * <li>no raid related checks are made (due to some scripts/cases), so any {@link Creature} will trigger it.</li>
	 * </ul>
	 * </font>
	 * @param npc : The {@link Npc} to test.
	 * @param npcId : The npcId who calls Anti Strider debuff (only bosses, normally).
	 * @return True if the curse must counter the leftover behavior.
	 */
	public boolean testCursesOnAttack(Npc npc, int npcId)
	{
		return false;
	}
	
	/**
	 * Similar to its mother class, but the Anti Strider Slow debuff is known to be casted by this {@link Creature}.
	 * @see #testCursesOnAttack(Npc, int)
	 * @param npc : The {@link Npc} to test.
	 * @return True if the curse must counter the leftover behavior.
	 */
	public boolean testCursesOnAttack(Npc npc)
	{
		return false;
	}
	
	/**
	 * Enforced testCursesOnAttack with third parameter set to -1.<br>
	 * <br>
	 * We only test RAID_CURSE2, not RAID_ANTI_STRIDER_SLOW.
	 * @see #testCursesOnAttack(Npc, int)
	 * @param npc : The {@link Npc} to test.
	 * @return True if the curse must counter the leftover behavior.
	 */
	public boolean testCursesOnAggro(Npc npc)
	{
		return false;
	}
	
	/**
	 * Test and cast curses if :
	 * <ul>
	 * <li>the {@link Creature} caster is 8 levels higher than the tested instance</li>
	 * <li>the helped {@link Creature} is registered into the AggroList, and got positive hate</li>
	 * <li>the tested {@link L2Skill} must be beneficial</li>
	 * </ul>
	 * <font color=red>BEWARE :
	 * <ul>
	 * <li>no Playable checks are made</li>
	 * <li>no raid related checks are made (due to some scripts/cases), so any {@link Creature} will trigger it</li>
	 * </ul>
	 * </font>
	 * @param skill : The {@link L2Skill} to test.
	 * @param targets : The {@link Creature} targets to check.
	 * @return True if the curse must counter the leftover behavior.
	 */
	public boolean testCursesOnSkillSee(L2Skill skill, Creature[] targets)
	{
		return false;
	}
	
	public final NpcTemplate getPolymorphTemplate()
	{
		return _polymorphTemplate;
	}
	
	public boolean polymorph(int id)
	{
		if (!(this instanceof Npc) && !(this instanceof Player))
			return false;
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(id);
		if (template == null)
			return false;
		
		_polymorphTemplate = template;
		
		decayMe();
		spawnMe();
		
		return true;
	}
	
	public void unpolymorph()
	{
		_polymorphTemplate = null;
		
		decayMe();
		spawnMe();
	}
	
	/**
	 * @return True if this {@link Creature} can be healed, false otherwise.
	 */
	public boolean canBeHealed()
	{
		return !isDead() && !isInvul();
	}
}