package net.sf.l2j.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.items.ItemState;
import net.sf.l2j.gameserver.enums.items.ItemType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ChangeRecorderListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.OnEquipListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.StatsListener;

/**
 * This class manages a {@link Creature}'s inventory.<br>
 * <br>
 * It extends {@link ItemContainer}.
 */
public abstract class Inventory extends ItemContainer
{
	private static final String RESTORE_INVENTORY = "SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data";
	
	private final ItemInstance[] _paperdoll = new ItemInstance[Paperdoll.TOTAL_SLOTS];
	
	protected final List<OnEquipListener> _paperdollListeners = new ArrayList<>();
	
	protected int _totalWeight;
	private int _wornMask;
	
	protected Inventory()
	{
		addPaperdollListener(StatsListener.getInstance());
	}
	
	protected abstract ItemLocation getEquipLocation();
	
	@Override
	protected void refreshWeight()
	{
		int weight = 0;
		for (ItemInstance item : _items)
			weight += item.getItem().getWeight() * item.getCount();
		
		_totalWeight = weight;
	}
	
	@Override
	protected void addItem(ItemInstance item)
	{
		super.addItem(item);
		
		if (item.isEquipped())
			equipItem(item);
	}
	
	@Override
	protected boolean removeItem(ItemInstance item)
	{
		// Unequip item if equipped
		for (int i = 0; i < _paperdoll.length; i++)
		{
			if (_paperdoll[i] == item)
				unequipItemInSlot(i);
		}
		return super.removeItem(item);
	}
	
	@Override
	public void restore()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_INVENTORY))
		{
			ps.setInt(1, getOwnerId());
			ps.setString(2, getBaseLocation().name());
			ps.setString(3, getEquipLocation().name());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					// Restore the item.
					final ItemInstance item = ItemInstance.restoreFromDb(getOwnerId(), rs);
					if (item == null)
						continue;
					
					// If the item is an hero item and inventory's owner is a player who isn't an hero, then set it to inventory.
					if (getOwner() instanceof Player && item.isHeroItem() && !HeroManager.getInstance().isActiveHero(getOwnerId()))
						item.setLocation(ItemLocation.INVENTORY);
					
					// Add the item to world objects list.
					World.getInstance().addObject(item);
					
					// If stackable item is found in inventory just add to current quantity
					if (item.isStackable() && getItemByItemId(item.getItemId()) != null)
						addItem("Restore", item, getOwner().getActingPlayer(), null);
					else
						addItem(item);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't restore inventory for {}.", e, getOwnerId());
		}
		refreshWeight();
	}
	
	public int getTotalWeight()
	{
		return _totalWeight;
	}
	
	/**
	 * @param type : The {@link ItemType} to check.
	 * @return True if the given {@link ItemType} is worn, false otherwise.
	 */
	public boolean isWearingType(ItemType type)
	{
		return isWearingType(type.mask());
	}
	
	/**
	 * @param mask : The mask to check.
	 * @return True if the given {@link ItemType} mask is worn, false otherwise.
	 */
	public boolean isWearingType(int mask)
	{
		return (mask & _wornMask) != 0;
	}
	
	/**
	 * Drop an item from this {@link Inventory} and update database.
	 * @param process : The {@link String} process triggering this action.
	 * @param item : The {@link ItemInstance} to drop.
	 * @param actor : The {@link Player} requesting the item drop.
	 * @param reference : The {@link WorldObject} referencing current action.
	 * @return The {@link ItemInstance} corresponding to the destroyed item or the updated item in {@link Inventory}.
	 */
	public ItemInstance dropItem(String process, ItemInstance item, Player actor, WorldObject reference)
	{
		if (item == null)
			return null;
		
		synchronized (item)
		{
			if (!_items.contains(item))
				return null;
			
			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(ItemState.REMOVED);
			
			item.updateDatabase();
			refreshWeight();
		}
		return item;
	}
	
	/**
	 * Drop an item using its objectIdfrom this {@link Inventory} and update database.
	 * @param process : The {@link String} process triggering this action.
	 * @param objectId : The {@link ItemInstance} objectId to drop.
	 * @param count : The amount to drop.
	 * @param actor : The {@link Player} requesting the item drop.
	 * @param reference : The {@link WorldObject} referencing current action.
	 * @return The {@link ItemInstance} corresponding to the destroyed item or the updated item in {@link Inventory}.
	 */
	public ItemInstance dropItem(String process, int objectId, int count, Player actor, WorldObject reference)
	{
		ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
			return null;
		
		synchronized (item)
		{
			if (!_items.contains(item))
				return null;
			
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(ItemState.MODIFIED);
				item.updateDatabase();
				
				item = ItemInstance.create(item.getItemId(), count, actor, reference);
				item.updateDatabase();
				refreshWeight();
				return item;
			}
		}
		return dropItem(process, item, actor, reference);
	}
	
	/**
	 * @param slot : The {@link Paperdoll} slot to check.
	 * @return The {@link ItemInstance} associated to the {@link Paperdoll} slot.
	 */
	public ItemInstance getItemFrom(Paperdoll slot)
	{
		return _paperdoll[slot.getId()];
	}
	
	/**
	 * @param slot : The {@link Paperdoll} slot to check.
	 * @return True if an {@link ItemInstance} is associated to the {@link Paperdoll} slot, false otherwise.
	 */
	public boolean hasItemIn(Paperdoll slot)
	{
		return _paperdoll[slot.getId()] != null;
	}
	
	/**
	 * @param slot : The {@link Paperdoll} slot to test.
	 * @return The id of the {@link ItemInstance} in the {@link Paperdoll} slot, or 0 if not found.
	 */
	public int getItemIdFrom(Paperdoll slot)
	{
		final ItemInstance item = getItemFrom(slot);
		return (item == null) ? 0 : item.getItemId();
	}
	
	/**
	 * @param slot : The {@link Paperdoll} slot to test.
	 * @return The augment id of the {@link ItemInstance} in the {@link Paperdoll} slot, or 0 if not found.
	 */
	public int getAugmentationIdFrom(Paperdoll slot)
	{
		final ItemInstance item = getItemFrom(slot);
		return (item == null || item.getAugmentation() == null) ? 0 : item.getAugmentation().getId();
	}
	
	/**
	 * @param slot : The {@link Paperdoll} slot to test.
	 * @return The object id of the {@link ItemInstance} in the {@link Paperdoll} slot, or 0 if not found.
	 */
	public int getItemObjectIdFrom(Paperdoll slot)
	{
		final ItemInstance item = getItemFrom(slot);
		return (item == null) ? 0 : item.getObjectId();
	}
	
	/**
	 * @param itemSlot : The item slot to check.
	 * @return The {@link ItemInstance} associated to the item slot.
	 */
	public ItemInstance getItemFrom(int itemSlot)
	{
		return getItemFrom(getPaperdollIndex(itemSlot));
	}
	
	/**
	 * @return The {@link List} of equipped {@link ItemInstance}s.
	 */
	public List<ItemInstance> getPaperdollItems()
	{
		return Stream.of(_paperdoll).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	/**
	 * @param slot : The item slot to test.
	 * @return The {@link Paperdoll} associated to an item slot.
	 */
	public static Paperdoll getPaperdollIndex(int slot)
	{
		switch (slot)
		{
			case Item.SLOT_UNDERWEAR:
				return Paperdoll.UNDER;
			
			case Item.SLOT_R_EAR:
				return Paperdoll.REAR;
			
			case Item.SLOT_L_EAR:
				return Paperdoll.LEAR;
			
			case Item.SLOT_NECK:
				return Paperdoll.NECK;
			
			case Item.SLOT_R_FINGER:
				return Paperdoll.RFINGER;
			
			case Item.SLOT_L_FINGER:
				return Paperdoll.LFINGER;
			
			case Item.SLOT_HEAD:
				return Paperdoll.HEAD;
			
			case Item.SLOT_R_HAND:
			case Item.SLOT_LR_HAND:
				return Paperdoll.RHAND;
			
			case Item.SLOT_L_HAND:
				return Paperdoll.LHAND;
			
			case Item.SLOT_GLOVES:
				return Paperdoll.GLOVES;
			
			case Item.SLOT_CHEST:
			case Item.SLOT_FULL_ARMOR:
			case Item.SLOT_ALLDRESS:
				return Paperdoll.CHEST;
			
			case Item.SLOT_LEGS:
				return Paperdoll.LEGS;
			
			case Item.SLOT_FEET:
				return Paperdoll.FEET;
			
			case Item.SLOT_BACK:
				return Paperdoll.CLOAK;
			
			case Item.SLOT_FACE:
			case Item.SLOT_HAIRALL:
				return Paperdoll.FACE;
			
			case Item.SLOT_HAIR:
				return Paperdoll.HAIR;
		}
		return Paperdoll.NULL;
	}
	
	/**
	 * Register a new {@link OnEquipListener} on paperdoll listeners.
	 * @param listener : The {@link OnEquipListener} to add.
	 */
	public synchronized void addPaperdollListener(OnEquipListener listener)
	{
		_paperdollListeners.add(listener);
	}
	
	/**
	 * Unregister an existing {@link OnEquipListener} from paperdoll listeners.
	 * @param listener : The {@link OnEquipListener} to remove.
	 */
	public synchronized void removePaperdollListener(OnEquipListener listener)
	{
		_paperdollListeners.remove(listener);
	}
	
	/**
	 * Equip an {@link ItemInstance} in the given {@link Paperdoll} slot.
	 * @param slot : The {@link Paperdoll} slot to edit.
	 * @param item : The {@link ItemInstance} to add.
	 * @return The previous {@link ItemInstance} set in given {@link Paperdoll}, or null if unequipped.
	 */
	public synchronized ItemInstance setPaperdollItem(Paperdoll slot, ItemInstance item)
	{
		ItemInstance old = getItemFrom(slot);
		if (old != item)
		{
			if (old != null)
			{
				_paperdoll[slot.getId()] = null;
				
				// Put old item from paperdoll slot to base location.
				old.setLocation(getBaseLocation());
				old.setLastChange(ItemState.MODIFIED);
				
				// Delete armor mask flag (in case of two-piece armor it does not matter, we need to deactivate mask too).
				_wornMask &= ~old.getItem().getItemMask();
				
				// Notify all paperdoll listener in order to unequip old item in slot.
				for (OnEquipListener listener : _paperdollListeners)
					listener.onUnequip(slot, old, (Playable) getOwner());
				
				old.updateDatabase();
			}
			
			if (item != null)
			{
				_paperdoll[slot.getId()] = item;
				
				// Add new item in slot of paperdoll.
				item.setLocation(getEquipLocation(), slot.getId());
				item.setLastChange(ItemState.MODIFIED);
				
				// Activate mask (check 2nd armor part for two-piece armors).
				final Item itm = item.getItem();
				if (itm.getBodyPart() == Item.SLOT_CHEST)
				{
					final ItemInstance legs = getItemFrom(Paperdoll.LEGS);
					if (legs != null && legs.getItem().getItemMask() == itm.getItemMask())
						_wornMask |= itm.getItemMask();
				}
				else if (itm.getBodyPart() == Item.SLOT_LEGS)
				{
					final ItemInstance legs = getItemFrom(Paperdoll.CHEST);
					if (legs != null && legs.getItem().getItemMask() == itm.getItemMask())
						_wornMask |= itm.getItemMask();
				}
				else
					_wornMask |= itm.getItemMask();
				
				for (OnEquipListener listener : _paperdollListeners)
					listener.onEquip(slot, item, (Playable) getOwner());
				
				item.updateDatabase();
			}
		}
		return old;
	}
	
	/**
	 * @param item : The {@link ItemInstance} to test.
	 * @return The item slot associated to a given {@link Paperdoll}.
	 */
	public int getSlotFromItem(ItemInstance item)
	{
		switch (Paperdoll.getEnumById(item.getLocationSlot()))
		{
			case UNDER:
				return Item.SLOT_UNDERWEAR;
			
			case LEAR:
				return Item.SLOT_L_EAR;
			
			case REAR:
				return Item.SLOT_R_EAR;
			
			case NECK:
				return Item.SLOT_NECK;
			
			case RFINGER:
				return Item.SLOT_R_FINGER;
			
			case LFINGER:
				return Item.SLOT_L_FINGER;
			
			case HAIR:
				return Item.SLOT_HAIR;
			
			case FACE:
				return Item.SLOT_FACE;
			
			case HEAD:
				return Item.SLOT_HEAD;
			
			case RHAND:
				return Item.SLOT_R_HAND;
			
			case LHAND:
				return Item.SLOT_L_HAND;
			
			case GLOVES:
				return Item.SLOT_GLOVES;
			
			case CHEST:
				return item.getItem().getBodyPart();
			
			case LEGS:
				return Item.SLOT_LEGS;
			
			case CLOAK:
				return Item.SLOT_BACK;
			
			case FEET:
				return Item.SLOT_FEET;
			
			default:
				return -1;
		}
	}
	
	/**
	 * Equip an {@link ItemInstance} in {@link Paperdoll} slot.
	 * @param item : The {@link ItemInstance} to set.
	 */
	public void equipItem(ItemInstance item)
	{
		switch (item.getItem().getBodyPart())
		{
			case Item.SLOT_LR_HAND:
				setPaperdollItem(Paperdoll.LHAND, null);
				setPaperdollItem(Paperdoll.RHAND, item);
				break;
			
			case Item.SLOT_L_HAND:
				ItemInstance rh = getItemFrom(Paperdoll.RHAND);
				if (rh != null && rh.getItem().getBodyPart() == Item.SLOT_LR_HAND && !((rh.getItemType() == WeaponType.BOW && item.getItemType() == EtcItemType.ARROW) || (rh.getItemType() == WeaponType.FISHINGROD && item.getItemType() == EtcItemType.LURE)))
					setPaperdollItem(Paperdoll.RHAND, null);
				
				setPaperdollItem(Paperdoll.LHAND, item);
				break;
			
			case Item.SLOT_R_HAND:
				setPaperdollItem(Paperdoll.RHAND, item);
				break;
			
			case Item.SLOT_L_EAR:
			case Item.SLOT_R_EAR:
			case Item.SLOT_L_EAR | Item.SLOT_R_EAR:
				if (getItemFrom(Paperdoll.LEAR) == null)
					setPaperdollItem(Paperdoll.LEAR, item);
				else if (getItemFrom(Paperdoll.REAR) == null)
					setPaperdollItem(Paperdoll.REAR, item);
				else
				{
					if (getItemIdFrom(Paperdoll.REAR) == item.getItemId())
						setPaperdollItem(Paperdoll.LEAR, item);
					else if (getItemIdFrom(Paperdoll.LEAR) == item.getItemId())
						setPaperdollItem(Paperdoll.REAR, item);
					else
						setPaperdollItem(Paperdoll.LEAR, item);
				}
				break;
			
			case Item.SLOT_L_FINGER:
			case Item.SLOT_R_FINGER:
			case Item.SLOT_L_FINGER | Item.SLOT_R_FINGER:
				if (getItemFrom(Paperdoll.LFINGER) == null)
					setPaperdollItem(Paperdoll.LFINGER, item);
				else if (getItemFrom(Paperdoll.RFINGER) == null)
					setPaperdollItem(Paperdoll.RFINGER, item);
				else
				{
					if (getItemIdFrom(Paperdoll.RFINGER) == item.getItemId())
						setPaperdollItem(Paperdoll.LFINGER, item);
					else if (getItemIdFrom(Paperdoll.LFINGER) == item.getItemId())
						setPaperdollItem(Paperdoll.RFINGER, item);
					else
						setPaperdollItem(Paperdoll.LFINGER, item);
				}
				break;
			
			case Item.SLOT_NECK:
				setPaperdollItem(Paperdoll.NECK, item);
				break;
			
			case Item.SLOT_FULL_ARMOR:
				setPaperdollItem(Paperdoll.LEGS, null);
				setPaperdollItem(Paperdoll.CHEST, item);
				break;
			
			case Item.SLOT_CHEST:
				setPaperdollItem(Paperdoll.CHEST, item);
				break;
			
			case Item.SLOT_LEGS:
				// handle full armor
				final ItemInstance chest = getItemFrom(Paperdoll.CHEST);
				if (chest != null && chest.getItem().getBodyPart() == Item.SLOT_FULL_ARMOR)
					setPaperdollItem(Paperdoll.CHEST, null);
				
				setPaperdollItem(Paperdoll.LEGS, item);
				break;
			
			case Item.SLOT_FEET:
				setPaperdollItem(Paperdoll.FEET, item);
				break;
			
			case Item.SLOT_GLOVES:
				setPaperdollItem(Paperdoll.GLOVES, item);
				break;
			
			case Item.SLOT_HEAD:
				setPaperdollItem(Paperdoll.HEAD, item);
				break;
			
			case Item.SLOT_FACE:
				final ItemInstance hair = getItemFrom(Paperdoll.HAIR);
				if (hair != null && hair.getItem().getBodyPart() == Item.SLOT_HAIRALL)
					setPaperdollItem(Paperdoll.HAIR, null);
				
				setPaperdollItem(Paperdoll.FACE, item);
				break;
			
			case Item.SLOT_HAIR:
				final ItemInstance face = getItemFrom(Paperdoll.FACE);
				if (face != null && face.getItem().getBodyPart() == Item.SLOT_HAIRALL)
					setPaperdollItem(Paperdoll.FACE, null);
				
				setPaperdollItem(Paperdoll.HAIR, item);
				break;
			
			case Item.SLOT_HAIRALL:
				setPaperdollItem(Paperdoll.FACE, null);
				setPaperdollItem(Paperdoll.HAIR, item);
				break;
			
			case Item.SLOT_UNDERWEAR:
				setPaperdollItem(Paperdoll.UNDER, item);
				break;
			
			case Item.SLOT_BACK:
				setPaperdollItem(Paperdoll.CLOAK, item);
				break;
			
			case Item.SLOT_ALLDRESS:
				setPaperdollItem(Paperdoll.LEGS, null);
				setPaperdollItem(Paperdoll.LHAND, null);
				setPaperdollItem(Paperdoll.RHAND, null);
				setPaperdollItem(Paperdoll.HEAD, null);
				setPaperdollItem(Paperdoll.FEET, null);
				setPaperdollItem(Paperdoll.GLOVES, null);
				setPaperdollItem(Paperdoll.CHEST, item);
				break;
			
			default:
				LOGGER.warn("Unknown body slot {} for itemId {}.", item.getItem().getBodyPart(), item.getItemId());
		}
	}
	
	/**
	 * Equip an {@link ItemInstance} and return alterations.<br>
	 * <br>
	 * <b>If you dont need return value use {@link Inventory#equipItem(ItemInstance)} instead.</b>
	 * @param item : The {@link ItemInstance} to equip.
	 * @return The array of altered {@link ItemInstance}s.
	 */
	public ItemInstance[] equipItemAndRecord(ItemInstance item)
	{
		final ChangeRecorderListener recorder = new ChangeRecorderListener(this);
		
		try
		{
			equipItem(item);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}
	
	/**
	 * Equip an {@link ItemInstance}.<br>
	 * <br>
	 * Concerning pets, armors go to Paperdoll.CHEST and weapon to Paperdoll.RHAND.
	 * @param item : The {@link ItemInstance} to equip.
	 */
	public void equipPetItem(ItemInstance item)
	{
		// Verify first if item is a pet item.
		if (item.isPetItem())
		{
			// Check then about type of item : armor or weapon. Feed the correct slot.
			if (item.getItemType() == WeaponType.PET)
				setPaperdollItem(Paperdoll.RHAND, item);
			else if (item.getItemType() == ArmorType.PET)
				setPaperdollItem(Paperdoll.CHEST, item);
		}
	}
	
	/**
	 * Unequip an {@link ItemInstance} and return alterations.
	 * @param item : The {@link ItemInstance} used to find the slot back.
	 * @return The array of altered {@link ItemInstance}s.
	 */
	public ItemInstance[] unequipItemInBodySlotAndRecord(ItemInstance item)
	{
		final ChangeRecorderListener recorder = new ChangeRecorderListener(this);
		
		try
		{
			unequipItemInBodySlot(getSlotFromItem(item));
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}
	
	/**
	 * Unequip an {@link ItemInstance} and return alterations.
	 * @param itemSlot : The item slot to test.
	 * @return The array of altered {@link ItemInstance}s.
	 */
	public ItemInstance[] unequipItemInBodySlotAndRecord(int itemSlot)
	{
		final ChangeRecorderListener recorder = new ChangeRecorderListener(this);
		
		try
		{
			unequipItemInBodySlot(itemSlot);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}
	
	/**
	 * Unequip an {@link ItemInstance} by its {@link Paperdoll} id.
	 * @param slot : The {@link Paperdoll} id.
	 * @return The unequipped {@link ItemInstance}, or null if already unequipped.
	 */
	public ItemInstance unequipItemInSlot(int slot)
	{
		return setPaperdollItem(Paperdoll.getEnumById(slot), null);
	}
	
	/**
	 * Unequip an {@link ItemInstance} and return alterations.
	 * @param slot : The slot to test.
	 * @return The array of altered {@link ItemInstance}s.
	 */
	public ItemInstance[] unequipItemInSlotAndRecord(int slot)
	{
		final ChangeRecorderListener recorder = new ChangeRecorderListener(this);
		
		try
		{
			unequipItemInSlot(slot);
			if (getOwner() instanceof Player)
				((Player) getOwner()).refreshExpertisePenalty();
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}
	
	/**
	 * Unequip an {@link ItemInstance} using its item slot.
	 * @param itemSlot : The item slot used to find the {@link Paperdoll} slot.
	 * @return The unequipped {@link ItemInstance}, or null if already unequipped.
	 */
	public ItemInstance unequipItemInBodySlot(int itemSlot)
	{
		Paperdoll slot = Paperdoll.NULL;
		
		switch (itemSlot)
		{
			case Item.SLOT_L_EAR:
				slot = Paperdoll.LEAR;
				break;
			
			case Item.SLOT_R_EAR:
				slot = Paperdoll.REAR;
				break;
			
			case Item.SLOT_NECK:
				slot = Paperdoll.NECK;
				break;
			
			case Item.SLOT_R_FINGER:
				slot = Paperdoll.RFINGER;
				break;
			
			case Item.SLOT_L_FINGER:
				slot = Paperdoll.LFINGER;
				break;
			
			case Item.SLOT_HAIR:
				slot = Paperdoll.HAIR;
				break;
			
			case Item.SLOT_FACE:
				slot = Paperdoll.FACE;
				break;
			
			case Item.SLOT_HAIRALL:
				setPaperdollItem(Paperdoll.FACE, null);
				slot = Paperdoll.FACE;
				break;
			
			case Item.SLOT_HEAD:
				slot = Paperdoll.HEAD;
				break;
			
			case Item.SLOT_R_HAND:
			case Item.SLOT_LR_HAND:
				slot = Paperdoll.RHAND;
				break;
			
			case Item.SLOT_L_HAND:
				slot = Paperdoll.LHAND;
				break;
			
			case Item.SLOT_GLOVES:
				slot = Paperdoll.GLOVES;
				break;
			
			case Item.SLOT_CHEST:
			case Item.SLOT_FULL_ARMOR:
			case Item.SLOT_ALLDRESS:
				slot = Paperdoll.CHEST;
				break;
			
			case Item.SLOT_LEGS:
				slot = Paperdoll.LEGS;
				break;
			
			case Item.SLOT_BACK:
				slot = Paperdoll.CLOAK;
				break;
			
			case Item.SLOT_FEET:
				slot = Paperdoll.FEET;
				break;
			
			case Item.SLOT_UNDERWEAR:
				slot = Paperdoll.UNDER;
				break;
			
			default:
				LOGGER.warn("Slot type {} is unhandled.", slot);
		}
		
		return (slot == Paperdoll.NULL) ? null : setPaperdollItem(slot, null);
	}
	
	/**
	 * @param bow : The {@link Item} designating the bow.
	 * @return The {@link ItemInstance} pointing out arrows.
	 */
	public ItemInstance findArrowForBow(Item bow)
	{
		if (bow == null)
			return null;
		
		// Get the ItemInstance corresponding to the item identifier and return it.
		switch (bow.getCrystalType())
		{
			case NONE:
				return getItemByItemId(17); // Wooden arrow
				
			case D:
				return getItemByItemId(1341); // Bone arrow
				
			case C:
				return getItemByItemId(1342); // Fine steel arrow
				
			case B:
				return getItemByItemId(1343); // Silver arrow
				
			case A:
				return getItemByItemId(1344); // Mithril arrow
				
			case S:
				return getItemByItemId(1345); // Shining arrow
				
			default:
				return null;
		}
	}
}