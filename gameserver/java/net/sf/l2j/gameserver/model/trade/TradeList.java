package net.sf.l2j.gameserver.model.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class TradeList extends CopyOnWriteArrayList<TradeItem>
{
	private static final long serialVersionUID = 1L;
	
	private final Player _owner;
	
	private Player _partner;
	private String _title;
	
	private boolean _packaged;
	private boolean _confirmed;
	private boolean _locked;
	
	public TradeList(Player owner)
	{
		_owner = owner;
	}
	
	@Override
	public String toString()
	{
		return "TradeList [_owner=" + _owner + ", _partner=" + _partner + ", _title=" + _title + ", _packaged=" + _packaged + ", _confirmed=" + _confirmed + ", _locked=" + _locked + "]";
	}
	
	public Player getOwner()
	{
		return _owner;
	}
	
	public Player getPartner()
	{
		return _partner;
	}
	
	public void setPartner(Player partner)
	{
		_partner = partner;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public void setTitle(String title)
	{
		_title = title;
	}
	
	public boolean isPackaged()
	{
		return _packaged;
	}
	
	public void setPackaged(boolean value)
	{
		_packaged = value;
	}
	
	public boolean isConfirmed()
	{
		return _confirmed;
	}
	
	public boolean isLocked()
	{
		return _locked;
	}
	
	/**
	 * @param inventory : The {@link PcInventory} to test.
	 * @return A cloned {@link List} of this {@link TradeList} adjusted to {@link PcInventory} available items.
	 */
	public List<TradeItem> getAvailableItems(PcInventory inventory)
	{
		final List<TradeItem> list = new ArrayList<>(this);
		list.forEach(ti -> inventory.adjustAvailableItem(ti));
		return list;
	}
	
	/**
	 * Create a {@link TradeItem} based on an existing {@link ItemInstance}.
	 * @param item : The {@link ItemInstance} to test.
	 * @return A {@link TradeItem} based on {@link ItemInstance}.
	 */
	public TradeItem adjustAvailableItem(ItemInstance item)
	{
		if (item.isStackable())
		{
			for (TradeItem tradeItem : this)
			{
				if (tradeItem.getItem().getItemId() == item.getItemId())
				{
					if (item.getCount() <= tradeItem.getCount())
						return null;
					
					return new TradeItem(item, item.getCount() - tradeItem.getCount(), item.getReferencePrice());
				}
			}
		}
		return new TradeItem(item, item.getCount(), item.getReferencePrice());
	}
	
	/**
	 * Create a {@link TradeItem} based on an existing {@link ItemInstance}, and add it to this {@link TradeList}.
	 * @param objectId : The {@link WorldObject} objectId to test.
	 * @param count : The amount of newly formed {@link TradeItem}.
	 * @param price : The price of newly formed {@link TradeItem}.
	 * @return A {@link TradeItem} based on {@link ItemInstance}, which is itself retrieved from its objectId from {@link World#getObject(int)}.
	 */
	public synchronized TradeItem addItem(int objectId, int count, int price)
	{
		if (isLocked())
			return null;
		
		final WorldObject object = World.getInstance().getObject(objectId);
		if (!(object instanceof ItemInstance))
			return null;
		
		final ItemInstance item = (ItemInstance) object;
		
		if (!item.isTradable() || item.isQuestItem())
			return null;
		
		if (count <= 0 || count > item.getCount())
			return null;
		
		if (!item.isStackable() && count > 1)
			return null;
		
		if ((Integer.MAX_VALUE / count) < price)
			return null;
		
		for (TradeItem checkitem : this)
		{
			if (checkitem.getObjectId() == objectId)
				return null;
		}
		
		final TradeItem tradeItem = new TradeItem(item, count, price);
		add(tradeItem);
		
		// If Player has already confirmed this trade, invalidate the confirmation.
		invalidateConfirmation();
		
		return tradeItem;
	}
	
	/**
	 * Create a {@link TradeItem} based on itemId, and add it to this {@link TradeList}.
	 * @param itemId : The itemId of newly formed {@link TradeItem}.
	 * @param count : The amount of newly formed {@link TradeItem}.
	 * @param price : The price of newly formed {@link TradeItem}.
	 * @param enchant : The enchant value of newly formed {@link TradeItem}.
	 * @return A {@link TradeItem} based on itemId.
	 */
	public synchronized TradeItem addItemByItemId(int itemId, int count, int price, int enchant)
	{
		if (isLocked())
			return null;
		
		final Item item = ItemData.getInstance().getTemplate(itemId);
		if (item == null)
			return null;
		
		if (!item.isTradable() || item.isQuestItem())
			return null;
		
		if (!item.isStackable() && count > 1)
			return null;
		
		if ((Integer.MAX_VALUE / count) < price)
			return null;
		
		final TradeItem tradeItem = new TradeItem(item, count, price, enchant);
		add(tradeItem);
		
		// If Player has already confirmed this trade, invalidate the confirmation.
		invalidateConfirmation();
		
		return tradeItem;
	}
	
	/**
	 * Remove or decrease amount of a {@link TradeItem} from this {@link TradeList}, by either its objectId or itemId.
	 * @param objectId : The objectId to test.
	 * @param itemId : The itemId ot test.
	 * @param count : The amount to remove.
	 */
	public synchronized void removeItem(int objectId, int itemId, int count)
	{
		if (isLocked())
			return;
		
		for (TradeItem tradeItem : this)
		{
			if (tradeItem.getObjectId() == objectId || tradeItem.getItem().getItemId() == itemId)
			{
				// If Partner has already confirmed this trade, invalidate the confirmation.
				if (_partner != null)
				{
					TradeList partnerList = _partner.getActiveTradeList();
					if (partnerList == null)
						break;
					
					partnerList.invalidateConfirmation();
				}
				
				// Reduce item count or complete item.
				tradeItem.setCount(tradeItem.getCount() - count);
				tradeItem.setQuantity(tradeItem.getQuantity() - count);
				
				if (tradeItem.getQuantity() <= 0)
					remove(tradeItem);
				
				break;
			}
		}
	}
	
	/**
	 * Update {@link TradeItem}s from this {@link TradeList} according to their quantity in owner inventory.
	 */
	public synchronized void updateItems()
	{
		for (TradeItem tradeItem : this)
		{
			final ItemInstance item = _owner.getInventory().getItemByObjectId(tradeItem.getObjectId());
			if (item == null || tradeItem.getCount() < 1)
				removeItem(tradeItem.getObjectId(), -1, -1);
			else if (item.getCount() < tradeItem.getCount())
				tradeItem.setCount(item.getCount());
		}
	}
	
	/**
	 * Lock this {@link TradeList}, meaning than no further changes are allowed.
	 */
	public void lock()
	{
		_locked = true;
	}
	
	@Override
	public synchronized void clear()
	{
		super.clear();
		
		_locked = false;
	}
	
	/**
	 * Confirm this {@link TradeList}, cancelling the trade if checks aren't properly passed (distance, items manipulation, etc).<br>
	 * <br>
	 * In case partner already confirmed its {@link TradeList}, then proceed to the exchange. Otherwise confirm this {@link TradeList}.
	 */
	public void confirm()
	{
		// The trade is already confirmed, don't process further.
		if (_confirmed)
			return;
		
		if (_partner == null)
		{
			_confirmed = true;
			return;
		}
		
		final TradeList partnerList = _partner.getActiveTradeList();
		if (partnerList == null)
		{
			_owner.cancelActiveTrade();
			return;
		}
		
		// Synchronization order to avoid deadlock
		TradeList sync1;
		TradeList sync2;
		
		if (getOwner().getObjectId() > partnerList.getOwner().getObjectId())
		{
			sync1 = partnerList;
			sync2 = this;
		}
		else
		{
			sync1 = this;
			sync2 = partnerList;
		}
		
		synchronized (sync1)
		{
			synchronized (sync2)
			{
				_confirmed = true;
				
				// If partner has already confirmed this trade, proceed to the exchange.
				if (partnerList.isConfirmed())
				{
					// Lock both TradeLists.
					partnerList.lock();
					lock();
					
					// Test the validity of the trade.
					if (!validate(_partner, true) || !partnerList.validate(_owner, true))
					{
						_owner.cancelActiveTrade();
						return;
					}
					
					// We passed all tests ; finally exchange.
					doExchange(partnerList);
				}
				// Otherwise, we are the first to try to confirm the trade.
				else
				{
					// Test the validity of the trade.
					if (!validate(_partner, false) || !partnerList.validate(_owner, false))
					{
						_owner.cancelActiveTrade();
						return;
					}
					
					// Test is passed ; confirm our TradeList.
					_partner.onTradeConfirm(_owner);
				}
			}
		}
	}
	
	/**
	 * Cancel {@link TradeList} confirmation.
	 */
	public void invalidateConfirmation()
	{
		_confirmed = false;
	}
	
	/**
	 * Test the validity of this {@link TradeList}.
	 * @param partner : The {@link Player} partner to test.
	 * @param isCheckingItems : If True, we also check item manipulation.
	 * @return True if all tests passed, false otherwise.
	 */
	private boolean validate(Player partner, boolean isCheckingItems)
	{
		// Check owner validity.
		if (_owner == null || World.getInstance().getPlayer(_owner.getObjectId()) == null)
			return false;
		
		// Check partner validity.
		if (partner == null || !_owner.isIn3DRadius(partner, Npc.INTERACTION_DISTANCE))
			return false;
		
		// Check item validity.
		if (isCheckingItems)
		{
			for (TradeItem tradeItem : this)
			{
				final ItemInstance item = _owner.checkItemManipulation(tradeItem.getObjectId(), tradeItem.getCount());
				if (item == null)
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Transfer all {@link TradeItem}s of this {@link TradeList} from {@link Player} owner inventory to {@link Player} partner.
	 * @param partner : The {@link Player} used as partner.
	 * @param ownerIU : The owner {@link InventoryUpdate} packet, used to refresh owner client-side inventory.
	 * @param partnerIU : The partner {@link InventoryUpdate} packet, used to refresh partner client-side inventory.
	 * @return True if all {@link TradeItem}s were successfully transfered, or false otherwise.
	 */
	private boolean transferItems(Player partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU)
	{
		for (TradeItem tradeItem : this)
		{
			final ItemInstance oldItem = _owner.getInventory().getItemByObjectId(tradeItem.getObjectId());
			if (oldItem == null)
				return false;
			
			final ItemInstance newItem = _owner.getInventory().transferItem("Trade", tradeItem.getObjectId(), tradeItem.getCount(), partner.getInventory(), _owner, _partner);
			if (newItem == null)
				return false;
			
			// Add changes to InventoryUpdate packets.
			if (ownerIU != null)
			{
				if (oldItem.getCount() > 0 && oldItem != newItem)
					ownerIU.addModifiedItem(oldItem);
				else
					ownerIU.addRemovedItem(oldItem);
			}
			
			if (partnerIU != null)
			{
				if (newItem.getCount() > tradeItem.getCount())
					partnerIU.addModifiedItem(newItem);
				else
					partnerIU.addNewItem(newItem);
			}
		}
		return true;
	}
	
	/**
	 * Proceed to the transfer of items, if all tests successfully passed.
	 * @param partnerTradeList : The {@link TradeList} of the {@link Player} partner.
	 */
	private void doExchange(TradeList partnerTradeList)
	{
		boolean isSuccessful = true;
		
		// Check weight integrity.
		if (!_owner.getInventory().validateTradeListWeight(partnerTradeList) || !partnerTradeList.getOwner().getInventory().validateTradeListWeight(this))
		{
			isSuccessful = false;
			
			_owner.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			partnerTradeList.getOwner().sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
		}
		// Check inventory slots integrity.
		else if (!_owner.getInventory().validateTradeListCapacity(partnerTradeList) || !partnerTradeList.getOwner().getInventory().validateTradeListCapacity(this))
		{
			isSuccessful = false;
			
			_owner.sendPacket(SystemMessageId.SLOTS_FULL);
			partnerTradeList.getOwner().sendPacket(SystemMessageId.SLOTS_FULL);
		}
		// Check if both TradeLists were empty.
		else if (isEmpty() && partnerTradeList.isEmpty())
		{
			isSuccessful = false;
		}
		// All tests passed, it's a success.
		else
		{
			// Prepare InventoryUpdate packet.
			InventoryUpdate ownerIU = new InventoryUpdate();
			InventoryUpdate partnerIU = new InventoryUpdate();
			
			// Transfer items.
			partnerTradeList.transferItems(_owner, partnerIU, ownerIU);
			transferItems(partnerTradeList.getOwner(), ownerIU, partnerIU);
			
			// Send InventoryUpdate packet.
			_owner.sendPacket(ownerIU);
			_partner.sendPacket(partnerIU);
			
			// Update current load aswell.
			StatusUpdate su = new StatusUpdate(_owner);
			su.addAttribute(StatusType.CUR_LOAD, _owner.getCurrentWeight());
			_owner.sendPacket(su);
			
			su = new StatusUpdate(_partner);
			su.addAttribute(StatusType.CUR_LOAD, _partner.getCurrentWeight());
			_partner.sendPacket(su);
		}
		
		// Finish the trade.
		_owner.onTradeFinish(isSuccessful);
		partnerTradeList.getOwner().onTradeFinish(isSuccessful);
	}
	
	/**
	 * Buy items from this {@link TradeList}.
	 * @param player : The {@link Player} who tries to buy an item.
	 * @param items : The {@link Set} of {@link ItemRequest} to test.
	 * @return True if all checks passed and the buy was successful, or false otherwise.
	 */
	public synchronized boolean privateStoreBuy(Player player, Set<ItemRequest> items)
	{
		if (_locked)
			return false;
		
		if (!validate(player, true))
		{
			lock();
			return false;
		}
		
		if (!_owner.isOnline() || !player.isOnline())
			return false;
		
		int slots = 0;
		int weight = 0;
		long totalPrice = 0;
		
		final PcInventory ownerInventory = _owner.getInventory();
		final PcInventory playerInventory = player.getInventory();
		
		for (ItemRequest item : items)
		{
			boolean found = false;
			
			for (TradeItem tradeItem : this)
			{
				if (tradeItem.getObjectId() == item.getObjectId())
				{
					if (tradeItem.getPrice() == item.getPrice())
					{
						if (tradeItem.getCount() < item.getCount())
							item.setCount(tradeItem.getCount());
						
						found = true;
					}
					break;
				}
			}
			
			// The item with this objectId and price wasn't found in this TradeList, set its count to 0 or if packaged, return false directly.
			if (!found)
			{
				if (isPackaged())
					return false;
				
				item.setCount(0);
				continue;
			}
			
			// Integer overflow check for the single item.
			if ((Integer.MAX_VALUE / item.getCount()) < item.getPrice())
			{
				lock();
				return false;
			}
			
			totalPrice += item.getCount() * item.getPrice();
			
			// Integer overflow check for the total price.
			if (Integer.MAX_VALUE < totalPrice || totalPrice < 0)
			{
				lock();
				return false;
			}
			
			// Check if requested item is available for manipulation.
			final ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount());
			if (oldItem == null || !oldItem.isTradable())
			{
				lock();
				return false;
			}
			
			final Item template = ItemData.getInstance().getTemplate(item.getItemId());
			if (template == null)
				continue;
			
			weight += item.getCount() * template.getWeight();
			
			if (!template.isStackable())
				slots += item.getCount();
			else if (playerInventory.getItemByItemId(item.getItemId()) == null)
				slots++;
		}
		
		if (totalPrice > playerInventory.getAdena())
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return false;
		}
		
		if (!playerInventory.validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return false;
		}
		
		if (!playerInventory.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return false;
		}
		
		// Prepare InventoryUpdate packets.
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		
		final ItemInstance adenaItem = playerInventory.getAdenaInstance();
		if (!playerInventory.reduceAdena("PrivateStore", (int) totalPrice, player, _owner))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return false;
		}
		
		playerIU.addItem(adenaItem);
		ownerInventory.addAdena("PrivateStore", (int) totalPrice, _owner, player);
		
		boolean ok = true;
		
		// Transfer items.
		for (ItemRequest item : items)
		{
			if (item.getCount() == 0)
				continue;
			
			// Check if requested item is available for manipulation.
			final ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount());
			if (oldItem == null)
			{
				lock();
				ok = false;
				break;
			}
			
			// Proceed with item transfer.
			final ItemInstance newItem = ownerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), playerInventory, _owner, player);
			if (newItem == null)
			{
				ok = false;
				break;
			}
			removeItem(item.getObjectId(), -1, item.getCount());
			
			// Add changes to InventoryUpdate packets.
			if (oldItem.getCount() > 0 && oldItem != newItem)
				ownerIU.addModifiedItem(oldItem);
			else
				ownerIU.addRemovedItem(oldItem);
			
			if (newItem.getCount() > item.getCount())
				playerIU.addModifiedItem(newItem);
			else
				playerIU.addNewItem(newItem);
			
			// Send messages about the transaction to both players.
			if (newItem.isStackable())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				sm.addString(player.getName());
				sm.addItemName(newItem.getItemId());
				sm.addNumber(item.getCount());
				_owner.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1);
				sm.addString(_owner.getName());
				sm.addItemName(newItem.getItemId());
				sm.addNumber(item.getCount());
				player.sendPacket(sm);
			}
			else if (newItem.getEnchantLevel() > 0)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S2_S3);
				sm.addString(player.getName());
				sm.addNumber(newItem.getEnchantLevel());
				sm.addItemName(newItem.getItemId());
				_owner.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_S3_FROM_S1);
				sm.addString(_owner.getName());
				sm.addNumber(newItem.getEnchantLevel());
				sm.addItemName(newItem.getItemId());
				player.sendPacket(sm);
			}
			else
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S2);
				sm.addString(player.getName());
				sm.addItemName(newItem.getItemId());
				_owner.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_FROM_S1);
				sm.addString(_owner.getName());
				sm.addItemName(newItem.getItemId());
				player.sendPacket(sm);
			}
		}
		
		// Send InventoryUpdate packets.
		_owner.sendPacket(ownerIU);
		player.sendPacket(playerIU);
		
		return ok;
	}
	
	/**
	 * Sell items to this PrivateStore list
	 * @param player
	 * @param itemRequests
	 * @return true if successful, false otherwise.
	 */
	public synchronized boolean privateStoreSell(Player player, ItemRequest[] itemRequests)
	{
		if (_locked)
			return false;
		
		if (!_owner.isOnline() || !player.isOnline())
			return false;
		
		boolean ok = false;
		
		final PcInventory ownerInventory = _owner.getInventory();
		final PcInventory playerInventory = player.getInventory();
		
		// Prepare InventoryUpdate packets.
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		
		long totalPrice = 0;
		
		for (ItemRequest itemRequest : itemRequests)
		{
			boolean found = false;
			
			for (TradeItem tradeItem : this)
			{
				if (tradeItem.getItem().getItemId() == itemRequest.getItemId())
				{
					if (tradeItem.getPrice() == itemRequest.getPrice())
					{
						if (tradeItem.getCount() < itemRequest.getCount())
							itemRequest.setCount(tradeItem.getCount());
						
						found = itemRequest.getCount() > 0;
					}
					break;
				}
			}
			
			// The item with this itemid and price wasn't found in this TradeList, continue.
			if (!found)
				continue;
			
			// Integer overflow check for the single item.
			if ((Integer.MAX_VALUE / itemRequest.getCount()) < itemRequest.getPrice())
			{
				lock();
				break;
			}
			
			long _totalPrice = totalPrice + itemRequest.getCount() * itemRequest.getPrice();
			
			// Integer overflow check for the total price.
			if (Integer.MAX_VALUE < _totalPrice || _totalPrice < 0)
			{
				lock();
				break;
			}
			
			if (ownerInventory.getAdena() < _totalPrice)
				continue;
			
			int objectId = itemRequest.getObjectId();
			
			// Check if requested item is available for manipulation.
			ItemInstance oldItem = player.checkItemManipulation(objectId, itemRequest.getCount());
			
			// Private store - buy use same objectId for buying several non-stackable items.
			if (oldItem == null)
			{
				// searching other items using same itemId
				oldItem = playerInventory.getItemByItemId(itemRequest.getItemId());
				if (oldItem == null)
					continue;
				
				objectId = oldItem.getObjectId();
				oldItem = player.checkItemManipulation(objectId, itemRequest.getCount());
				if (oldItem == null)
					continue;
			}
			
			if (oldItem.getItemId() != itemRequest.getItemId() || oldItem.getEnchantLevel() != itemRequest.getEnchantLevel())
				return false;
			
			if (!oldItem.isTradable())
				continue;
			
			// Proceed with item transfer.
			ItemInstance newItem = playerInventory.transferItem("PrivateStore", objectId, itemRequest.getCount(), ownerInventory, player, _owner);
			if (newItem == null)
				continue;
			
			removeItem(-1, itemRequest.getItemId(), itemRequest.getCount());
			ok = true;
			
			// Increase total price only after successful transaction.
			totalPrice = _totalPrice;
			
			// Add changes to InventoryUpdate packets.
			if (oldItem.getCount() > 0 && oldItem != newItem)
				playerIU.addModifiedItem(oldItem);
			else
				playerIU.addRemovedItem(oldItem);
			
			if (newItem.getCount() > itemRequest.getCount())
				ownerIU.addModifiedItem(newItem);
			else
				ownerIU.addNewItem(newItem);
			
			// Send messages about the transaction to both players.
			if (newItem.isStackable())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1);
				sm.addString(player.getName());
				sm.addItemName(newItem.getItemId());
				sm.addNumber(itemRequest.getCount());
				_owner.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				sm.addString(_owner.getName());
				sm.addItemName(newItem.getItemId());
				sm.addNumber(itemRequest.getCount());
				player.sendPacket(sm);
			}
			else if (newItem.getEnchantLevel() > 0)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_S3_FROM_S1);
				sm.addString(player.getName());
				sm.addNumber(newItem.getEnchantLevel());
				sm.addItemName(newItem.getItemId());
				_owner.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S2_S3);
				sm.addString(_owner.getName());
				sm.addNumber(newItem.getEnchantLevel());
				sm.addItemName(newItem.getItemId());
				player.sendPacket(sm);
			}
			else
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_FROM_S1);
				sm.addString(player.getName());
				sm.addItemName(newItem.getItemId());
				_owner.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S2);
				sm.addString(_owner.getName());
				sm.addItemName(newItem.getItemId());
				player.sendPacket(sm);
			}
		}
		
		// Transfer adena.
		if (totalPrice > 0)
		{
			if (totalPrice > ownerInventory.getAdena())
				return false;
			
			final ItemInstance adenaItem = ownerInventory.getAdenaInstance();
			ownerInventory.reduceAdena("PrivateStore", (int) totalPrice, _owner, player);
			ownerIU.addItem(adenaItem);
			
			playerInventory.addAdena("PrivateStore", (int) totalPrice, player, _owner);
			playerIU.addItem(playerInventory.getAdenaInstance());
		}
		
		// Send InventoryUpdate packets.
		if (ok)
		{
			_owner.sendPacket(ownerIU);
			player.sendPacket(playerIU);
		}
		return ok;
	}
}