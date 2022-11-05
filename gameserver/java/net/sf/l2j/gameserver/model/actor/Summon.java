package net.sf.l2j.gameserver.model.actor;

import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.enums.items.ActionType;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.ai.type.SummonAI;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.FriendlyMonster;
import net.sf.l2j.gameserver.model.actor.instance.Guard;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.move.SummonMove;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.SummonInfo;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PetDelete;
import net.sf.l2j.gameserver.network.serverpackets.PetInfo;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.PetStatusShow;
import net.sf.l2j.gameserver.network.serverpackets.PetStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public abstract class Summon extends Playable
{
	private Player _owner;
	private boolean _previousFollowStatus = true;
	private int _shotsMask = 0;
	
	public static final int CONTRACT_PAYMENT = 4140;
	
	public Summon(int objectId, NpcTemplate template, Player owner)
	{
		super(objectId, template);
		
		// Calculate passive skills stats.
		for (L2Skill skill : template.getSkills(NpcSkillType.PASSIVE))
			addStatFuncs(skill.getStatFuncs(this));
		
		// Set the magical circle animation.
		setShowSummonAnimation(true);
		
		// Set the Player owner.
		_owner = owner;
	}
	
	public abstract int getSummonType();
	
	@Override
	public SummonStatus<? extends Summon> getStatus()
	{
		return (SummonStatus<?>) _status;
	}
	
	@Override
	public void setStatus()
	{
		_status = new SummonStatus<>(this);
	}
	
	@Override
	public SummonMove getMove()
	{
		return (SummonMove) _move;
	}
	
	@Override
	public void setMove()
	{
		_move = new SummonMove(this);
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
					_ai = ai = new SummonAI(this);
			}
		}
		return ai;
	}
	
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	@Override
	public void setWalkOrRun(boolean value)
	{
		super.setWalkOrRun(value);
		
		getStatus().broadcastStatusUpdate();
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		for (Player player : getKnownType(Player.class))
			player.sendPacket(new SummonInfo(this, player, 1));
	}
	
	/**
	 * @return Returns the mountable.
	 */
	public boolean isMountable()
	{
		return false;
	}
	
	@Override
	public void onInteract(Player player)
	{
		player.sendPacket(new PetStatusShow(this));
	}
	
	@Override
	public void onAction(Player player, boolean isCtrlPressed, boolean isShiftPressed)
	{
		// Set the target of the player
		if (player.getTarget() != this)
			player.setTarget(this);
		else
		{
			if (player == _owner)
			{
				if (isCtrlPressed)
					player.getAI().tryToAttack(this, isCtrlPressed, isShiftPressed);
				else
					player.getAI().tryToInteract(this, isCtrlPressed, isShiftPressed);
			}
			else
			{
				if (isAttackableWithoutForceBy(player) || (isCtrlPressed && isAttackableBy(player)))
					player.getAI().tryToAttack(this, isCtrlPressed, isShiftPressed);
				else
					player.getAI().tryToFollow(this, isShiftPressed);
			}
		}
	}
	
	@Override
	public final int getKarma()
	{
		return (getOwner() != null) ? getOwner().getKarma() : 0;
	}
	
	@Override
	public final byte getPvpFlag()
	{
		return (getOwner() != null) ? getOwner().getPvpFlag() : 0;
	}
	
	public final TeamType getTeam()
	{
		return (getOwner() != null) ? getOwner().getTeam() : TeamType.NONE;
	}
	
	public final Player getOwner()
	{
		return _owner;
	}
	
	public final int getNpcId()
	{
		return getTemplate().getNpcId();
	}
	
	@Override
	public int getWeightLimit()
	{
		return 0;
	}
	
	public int getSoulShotsPerHit()
	{
		return getTemplate().getSsCount();
	}
	
	public int getSpiritShotsPerHit()
	{
		return getTemplate().getSpsCount();
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		// Refresh aggro list of all Attackables which were hit by that Summon.
		for (Attackable attackable : getKnownType(Attackable.class))
		{
			if (attackable.isDead())
				continue;
			
			final boolean isGuard = attackable instanceof Guard || attackable instanceof FriendlyMonster;
			if (this instanceof Pet && isGuard)
				continue;
			
			final AggroInfo info = attackable.getAggroList().get(this);
			if (info != null && (!isGuard || info.getDamage() > 0))
				attackable.getAggroList().addDamageHate(getOwner(), 0, 1);
		}
		
		// Disable beastshots
		for (int itemId : getOwner().getAutoSoulShot())
		{
			switch (ItemData.getInstance().getTemplate(itemId).getDefaultAction())
			{
				case summon_soulshot:
				case summon_spiritshot:
					getOwner().disableAutoShot(itemId);
					break;
			}
		}
		return true;
	}
	
	@Override
	public void onDecay()
	{
		if (_owner.getSummon() != this)
			return;
		
		// Remove Contract Payment effect from owner.
		_owner.stopSkillEffects(CONTRACT_PAYMENT);
		
		deleteMe(_owner);
	}
	
	public void deleteMe(Player owner)
	{
		owner.setSummon(null);
		owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));
		
		decayMe();
		deleteMe();
	}
	
	@Override
	public void deleteMe()
	{
		super.deleteMe();
		
		// We stop effects here and not higher in hierarchy, because Players need to keep their effects.
		stopAllEffects();
	}
	
	public void unSummon(Player owner)
	{
		if (isVisible() && !isDead())
		{
			// Abort attack, cast and move.
			abortAll(true);
			
			getStatus().stopHpMpRegeneration();
			stopAllEffects();
			store();
			
			owner.setSummon(null);
			owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));
			
			decayMe();
			
			// Remove Contract Payment effect from owner.
			_owner.stopSkillEffects(CONTRACT_PAYMENT);
			
			// Disable beastshots
			for (int itemId : owner.getAutoSoulShot())
			{
				switch (ItemData.getInstance().getTemplate(itemId).getDefaultAction())
				{
					case summon_soulshot:
					case summon_spiritshot:
						owner.disableAutoShot(itemId);
						break;
				}
			}
			
			super.deleteMe();
		}
	}
	
	public int getAttackRange()
	{
		return 36;
	}
	
	public int getControlItemId()
	{
		return 0;
	}
	
	public Weapon getActiveWeapon()
	{
		return null;
	}
	
	@Override
	public PetInventory getInventory()
	{
		return null;
	}
	
	public void store()
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
	
	/**
	 * Return True if the L2Summon is invulnerable or if the summoner is in spawn protection.<BR>
	 * <BR>
	 */
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || getOwner().isSpawnProtected();
	}
	
	/**
	 * Return the Party of its owner, or null.
	 */
	@Override
	public Party getParty()
	{
		return (_owner == null) ? null : _owner.getParty();
	}
	
	/**
	 * Return True if the Summon owner has a Party in progress.
	 */
	@Override
	public boolean isInParty()
	{
		return _owner != null && _owner.getParty() != null;
	}
	
	@Override
	public void setIsImmobilized(boolean value)
	{
		super.setIsImmobilized(value);
		
		if (value)
		{
			_previousFollowStatus = getAI().getFollowStatus();
			// if immobilized, disable follow mode
			if (_previousFollowStatus)
				getAI().setFollowStatus(false);
		}
		else
		{
			// if not more immobilized, restore follow mode
			getAI().setFollowStatus(_previousFollowStatus);
		}
	}
	
	public void setOwner(Player newOwner)
	{
		_owner = newOwner;
	}
	
	@Override
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss || getOwner() == null)
			return;
		
		// Prevents the double spam of system messages, if the target is the owning player.
		if (target.getObjectId() != getOwner().getObjectId())
		{
			if (pcrit || mcrit)
				sendPacket(SystemMessageId.CRITICAL_HIT_BY_PET);
			
			if (target.isInvul())
			{
				if (target.isParalyzed())
					sendPacket(SystemMessageId.OPPONENT_PETRIFIED);
				else
					sendPacket(SystemMessageId.ATTACK_WAS_BLOCKED);
			}
			else
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE).addNumber(damage));
			
			if (getOwner().isInOlympiadMode() && target instanceof Player && ((Player) target).isInOlympiadMode() && ((Player) target).getOlympiadGameId() == getOwner().getOlympiadGameId())
				OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
		}
	}
	
	@Override
	public boolean isOutOfControl()
	{
		return super.isOutOfControl() || isBetrayed();
	}
	
	@Override
	public boolean isInCombat()
	{
		return getOwner() != null && getOwner().isInCombat();
	}
	
	@Override
	public Player getActingPlayer()
	{
		return getOwner();
	}
	
	@Override
	public String toString()
	{
		return super.toString() + "(" + getNpcId() + ") Owner: " + getOwner();
	}
	
	@Override
	public void sendPacket(L2GameServerPacket mov)
	{
		if (getOwner() != null)
			getOwner().sendPacket(mov);
	}
	
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (getOwner() != null)
			getOwner().sendPacket(id);
	}
	
	public int getWeapon()
	{
		return 0;
	}
	
	public int getArmor()
	{
		return 0;
	}
	
	public void updateAndBroadcastStatusAndInfos(int val)
	{
		sendPacket(new PetInfo(this, val));
		
		// The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
		updateEffectIcons(true);
		
		updateAndBroadcastStatus(val);
	}
	
	public void sendPetInfosToOwner()
	{
		sendPacket(new PetInfo(this, 2));
		
		// The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
		updateEffectIcons(true);
	}
	
	public void updateAndBroadcastStatus(int val)
	{
		sendPacket(new PetStatusUpdate(this));
		
		if (isVisible())
		{
			for (Player player : getKnownType(Player.class))
			{
				if (player == getOwner())
					continue;
				
				player.sendPacket(new SummonInfo(this, player, val));
			}
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Need it only for "crests on summons" custom.
		if (Config.SHOW_SUMMON_CREST)
			sendPacket(new SummonInfo(this, getOwner(), 0));
		
		sendPacket(new RelationChanged(this, getOwner().getRelation(getOwner()), false));
		broadcastRelationsChanges();
	}
	
	@Override
	public void broadcastRelationsChanges()
	{
		for (Player player : getOwner().getKnownType(Player.class))
			player.sendPacket(new RelationChanged(this, getOwner().getRelation(player), isAttackableWithoutForceBy(player)));
	}
	
	@Override
	public void sendInfo(Player player)
	{
		// Check if the Player is the owner of the Pet
		if (player == getOwner())
		{
			player.sendPacket(new PetInfo(this, 0));
			
			// The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
			updateEffectIcons(true);
			
			if (this instanceof Pet)
				player.sendPacket(new PetItemList(this));
		}
		else
			player.sendPacket(new SummonInfo(this, player, 0));
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		return (_shotsMask & type.getMask()) == type.getMask();
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		if (charged)
			_shotsMask |= type.getMask();
		else
			_shotsMask &= ~type.getMask();
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		if (getOwner().getAutoSoulShot() == null || getOwner().getAutoSoulShot().isEmpty())
			return;
		
		for (int itemId : getOwner().getAutoSoulShot())
		{
			ItemInstance item = getOwner().getInventory().getItemByItemId(itemId);
			if (item != null)
			{
				if (magic && item.getItem().getDefaultAction() == ActionType.summon_spiritshot)
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
						handler.useItem(getOwner(), item, false);
				}
				
				if (physical && item.getItem().getDefaultAction() == ActionType.summon_soulshot)
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
						handler.useItem(getOwner(), item, false);
				}
			}
			else
				getOwner().removeAutoSoulShot(itemId);
		}
	}
	
	@Override
	public int getSkillLevel(int skillId)
	{
		for (List<L2Skill> list : getTemplate().getSkills().values())
		{
			for (L2Skill skill : list)
				if (skill.getId() == skillId)
					return skill.getLevel();
		}
		return 0;
	}
	
	@Override
	public L2Skill getSkill(int skillId)
	{
		for (List<L2Skill> list : getTemplate().getSkills().values())
		{
			for (L2Skill skill : list)
				if (skill.getId() == skillId)
					return skill;
		}
		return null;
	}
	
	@Override
	public void onTeleported()
	{
		super.onTeleported();
		
		// Need it only for "crests on summons" custom.
		if (Config.SHOW_SUMMON_CREST)
			sendPacket(new SummonInfo(this, getOwner(), 0));
	}
}