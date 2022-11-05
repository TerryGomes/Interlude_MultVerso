package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.enums.DoorType;
import net.sf.l2j.gameserver.enums.OpenType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.geoengine.geodata.IGeoObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.ai.type.DoorAI;
import net.sf.l2j.gameserver.model.actor.status.DoorStatus;
import net.sf.l2j.gameserver.model.actor.template.DoorTemplate;
import net.sf.l2j.gameserver.model.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.clanhall.SiegableHall;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.DoorInfo;
import net.sf.l2j.gameserver.network.serverpackets.DoorStatusUpdate;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Door extends Creature implements IGeoObject
{
	private final Castle _castle;
	private final ClanHall _clanHall;
	
	private boolean _open;
	
	public Door(int objectId, DoorTemplate template)
	{
		super(objectId, template);
		
		// Assign the Door to a Castle, if the Castle owns the door id.
		_castle = CastleManager.getInstance().getCastleById(template.getCastleId());
		if (_castle != null)
			_castle.getDoors().add(this);
		
		// Assign the Door to a ClanHall, if the ClanHall owns the door id.
		_clanHall = ClanHallManager.getInstance().getClanHall(template.getClanHallId());
		if (_clanHall != null)
			_clanHall.getDoors().add(this);
		
		// Temporarily set opposite state to initial state (will be set correctly by onSpawn).
		_open = !getTemplate().isOpened();
		
		// Set the name.
		setName(template.getName());
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
					_ai = ai = new DoorAI(this);
			}
		}
		return ai;
	}
	
	@Override
	public final DoorStatus getStatus()
	{
		return (DoorStatus) _status;
	}
	
	@Override
	public void setStatus()
	{
		_status = new DoorStatus(this);
	}
	
	@Override
	public final DoorTemplate getTemplate()
	{
		return (DoorTemplate) super.getTemplate();
	}
	
	@Override
	public void addFuncsToNewCharacter()
	{
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public boolean isAttackableBy(Creature attacker)
	{
		if (!super.isAttackableBy(attacker))
			return false;
		
		if (!(attacker instanceof Playable))
			return false;
		
		if (_castle != null && _castle.getSiege().isInProgress())
		{
			if (!_castle.getSiege().checkSides(attacker.getActingPlayer().getClan(), SiegeSide.ATTACKER))
				return false;
			
			if (isWall())
				return attacker instanceof SiegeSummon && ((Summon) attacker).getNpcId() != SiegeSummon.SWOOP_CANNON_ID;
			
			return true;
		}
		
		if (_clanHall instanceof SiegableHall)
		{
			final SiegableHall hall = (SiegableHall) _clanHall;
			return hall.isInSiege() && hall.getSiege().doorIsAutoAttackable() && hall.getSiege().checkSides(attacker.getActingPlayer().getClan(), SiegeSide.ATTACKER);
		}
		
		return false;
	}
	
	@Override
	public boolean isAttackableWithoutForceBy(Playable attacker)
	{
		return isAttackableBy(attacker);
	}
	
	@Override
	public void onInteract(Player player)
	{
		// Clan members (with privs) of door associated with a clan hall get a pop-up window to open/close the said door
		if (player.getClan() != null && _clanHall != null && player.getClanId() == _clanHall.getOwnerId() && player.hasClanPrivileges(Clan.CP_CH_OPEN_DOOR))
		{
			player.setRequestedGate(this);
			player.sendPacket(new ConfirmDlg((!isOpened()) ? 1140 : 1141));
		}
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		// HPs can only be reduced during sieges.
		if (_castle != null && _castle.getSiege().isInProgress())
		{
			// SiegeSummon can attack both Walls and Doors (excepted Swoop Cannon - anti-infantery summon).
			if (attacker instanceof SiegeSummon && ((SiegeSummon) attacker).getNpcId() == SiegeSummon.SWOOP_CANNON_ID)
				return;
			
			super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
		}
		else if (_clanHall instanceof SiegableHall && ((SiegableHall) _clanHall).getSiegeZone().isActive())
			super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	@Override
	public void reduceCurrentHpByDOT(double i, Creature attacker, L2Skill skill)
	{
		// Doors can't be damaged by DOTs.
	}
	
	@Override
	public void onSpawn()
	{
		changeState(getTemplate().isOpened(), false);
		
		super.onSpawn();
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (!_open)
			GeoEngine.getInstance().removeGeoObject(this);
		
		if (_castle != null && _castle.getSiege().isInProgress())
			_castle.getSiege().announce((isWall()) ? SystemMessageId.CASTLE_WALL_DAMAGED : SystemMessageId.CASTLE_GATE_BROKEN_DOWN, SiegeSide.DEFENDER);
		
		return true;
	}
	
	@Override
	public void doRevive()
	{
		_open = getTemplate().isOpened();
		
		if (!_open)
			GeoEngine.getInstance().addGeoObject(this);
		
		super.doRevive();
	}
	
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new DoorInfo(player, this));
		player.sendPacket(new DoorStatusUpdate(this));
	}
	
	@Override
	public int getGeoX()
	{
		return getTemplate().getGeoX();
	}
	
	@Override
	public int getGeoY()
	{
		return getTemplate().getGeoY();
	}
	
	@Override
	public int getGeoZ()
	{
		return getTemplate().getGeoZ();
	}
	
	@Override
	public int getHeight()
	{
		return (int) getTemplate().getCollisionHeight();
	}
	
	@Override
	public byte[][] getObjectGeoData()
	{
		return getTemplate().getGeoData();
	}
	
	@Override
	public double getCollisionHeight()
	{
		return getTemplate().getCollisionHeight() / 2;
	}
	
	@Override
	public boolean canBeHealed()
	{
		return false;
	}
	
	/**
	 * @return The {@link Door} id.
	 */
	public final int getDoorId()
	{
		return getTemplate().getId();
	}
	
	/**
	 * @return True if this {@link Door} is opened, false otherwise.
	 */
	public final boolean isOpened()
	{
		return _open;
	}
	
	/**
	 * @return True if this {@link Door} can be unlocked.
	 */
	public final boolean isUnlockable()
	{
		return getTemplate().getOpenType() == OpenType.SKILL;
	}
	
	/**
	 * @return True if this {@link Door} is a wall.
	 */
	public final boolean isWall()
	{
		return getTemplate().getType() == DoorType.WALL;
	}
	
	/**
	 * @return The actual damage of this {@link Door}.
	 */
	public final int getDamage()
	{
		return Math.max(0, Math.min(6, 6 - (int) Math.ceil(getStatus().getHpRatio() * 6)));
	}
	
	/**
	 * Open the {@link Door}.
	 */
	public final void openMe()
	{
		// open door using external action
		changeState(true, false);
	}
	
	/**
	 * Close the {@link Door}.
	 */
	public final void closeMe()
	{
		// close door using external action
		changeState(false, false);
	}
	
	/**
	 * Open/close the {@link Door}, triggers other {@link Door}s and schedule automatic open/close task.
	 * @param open : Requested status change.
	 * @param triggered : If true, it means the status change was triggered by another {@link Door}.
	 */
	public final void changeState(boolean open, boolean triggered)
	{
		// door is dead or already in requested state, return
		if (isDead() || _open == open)
			return;
		
		// change door state and broadcast change
		_open = open;
		if (open)
			GeoEngine.getInstance().removeGeoObject(this);
		else
			GeoEngine.getInstance().addGeoObject(this);
		
		getStatus().broadcastStatusUpdate();
		
		// door controls another door
		int triggerId = getTemplate().getTriggerId();
		if (triggerId > 0)
		{
			// get door and trigger state change
			Door door = DoorData.getInstance().getDoor(triggerId);
			if (door != null)
				door.changeState(open, true);
		}
		
		// request is not triggered
		if (!triggered)
		{
			// calculate time for automatic state change
			int time = open ? getTemplate().getCloseTime() : getTemplate().getOpenTime();
			if (getTemplate().getRandomTime() > 0)
				time += Rnd.get(getTemplate().getRandomTime());
			
			// try to schedule automatic state change
			if (time > 0)
				ThreadPool.schedule(() -> changeState(!open, false), time * 1000);
		}
	}
	
	public final Castle getCastle()
	{
		return _castle;
	}
	
	public final ClanHall getClanHall()
	{
		return _clanHall;
	}
}