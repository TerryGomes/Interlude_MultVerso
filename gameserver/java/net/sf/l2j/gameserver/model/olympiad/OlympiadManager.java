package net.sf.l2j.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.OlympiadType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class OlympiadManager
{
	private final List<Integer> _nonClassBasedParticipants = new CopyOnWriteArrayList<>();
	private final Map<Integer, List<Integer>> _classBasedParticipants = new ConcurrentHashMap<>();
	
	protected OlympiadManager()
	{
	}
	
	public final List<Integer> getNonClassBasedParticipants()
	{
		return _nonClassBasedParticipants;
	}
	
	public final Map<Integer, List<Integer>> getClassBasedParticipants()
	{
		return _classBasedParticipants;
	}
	
	protected final boolean hasEnoughNonClassBasedParticipants()
	{
		return _nonClassBasedParticipants.size() >= Config.OLY_NONCLASSED;
	}
	
	protected final List<List<Integer>> hasEnoughClassBasedParticipants()
	{
		List<List<Integer>> result = null;
		for (List<Integer> classList : _classBasedParticipants.values())
		{
			if (classList != null && classList.size() >= Config.OLY_CLASSED)
			{
				if (result == null)
					result = new ArrayList<>();
				
				result.add(classList);
			}
		}
		return result;
	}
	
	protected final void clearParticipants()
	{
		_nonClassBasedParticipants.clear();
		_classBasedParticipants.clear();
	}
	
	public final boolean isRegisteredInComp(Player noble)
	{
		return isRegistered(noble, false) || isInCompetition(noble, false);
	}
	
	public final boolean isRegistered(Player player)
	{
		return isRegistered(player, false);
	}
	
	private final boolean isRegistered(Player player, boolean showMessage)
	{
		final Integer objId = Integer.valueOf(player.getObjectId());
		
		if (_nonClassBasedParticipants.contains(objId))
		{
			if (showMessage)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME));
			
			return true;
		}
		
		final List<Integer> classed = _classBasedParticipants.get(player.getBaseClass());
		if (classed != null && classed.contains(objId))
		{
			if (showMessage)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_TO_PARTICIPATE_IN_THE_GAME_FOR_YOUR_CLASS));
			
			return true;
		}
		
		return false;
	}
	
	private static final boolean isInCompetition(Player player, boolean showMessage)
	{
		if (!Olympiad.getInstance().isInCompPeriod())
			return false;
		
		for (int i = OlympiadGameManager.getInstance().getNumberOfStadiums(); --i >= 0;)
		{
			AbstractOlympiadGame game = OlympiadGameManager.getInstance().getOlympiadTask(i).getGame();
			if (game == null)
				continue;
			
			if (game.containsParticipant(player.getObjectId()))
			{
				if (showMessage)
					player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT);
				
				return true;
			}
		}
		return false;
	}
	
	public final boolean registerNoble(Npc npc, Player player, OlympiadType type)
	{
		if (!Olympiad.getInstance().isInCompPeriod())
		{
			player.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		
		if (Olympiad.getInstance().getMillisToCompEnd() < 600000)
		{
			player.sendPacket(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			return false;
		}
		
		switch (type)
		{
			case CLASSED:
				if (!checkNoble(npc, player))
					return false;
				
				final List<Integer> classed = _classBasedParticipants.computeIfAbsent(player.getBaseClass(), c -> new CopyOnWriteArrayList<>());
				classed.add(player.getObjectId());
				
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
				break;
			
			case NON_CLASSED:
				if (!checkNoble(npc, player))
					return false;
				
				_nonClassBasedParticipants.add(player.getObjectId());
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
				break;
		}
		return true;
	}
	
	public final boolean unRegisterNoble(Player player)
	{
		if (!Olympiad.getInstance().isInCompPeriod())
		{
			player.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		
		if (!player.isNoble())
		{
			player.sendPacket(SystemMessageId.NOBLESSE_ONLY);
			return false;
		}
		
		if (!isRegistered(player, false))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			return false;
		}
		
		if (isInCompetition(player, false))
			return false;
		
		final Integer objectId = Integer.valueOf(player.getObjectId());
		if (_nonClassBasedParticipants.remove(objectId))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}
		
		final List<Integer> classed = _classBasedParticipants.get(player.getBaseClass());
		if (classed != null && classed.remove(objectId))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}
		
		return false;
	}
	
	public final void removeDisconnectedCompetitor(Player player)
	{
		final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
		if (task != null && task.isGameStarted())
			task.getGame().handleDisconnect(player);
		
		final Integer objId = Integer.valueOf(player.getObjectId());
		if (_nonClassBasedParticipants.remove(objId))
			return;
		
		final List<Integer> classed = _classBasedParticipants.get(player.getBaseClass());
		if (classed != null)
			classed.remove(objId);
	}
	
	/**
	 * @param npc : The {@link Npc} the {@link Player} is talking to.
	 * @param player : The {@link Player} being tested.
	 * @return True if all requirements are met, or false otherwise.
	 */
	private final boolean checkNoble(Npc npc, Player player)
	{
		if (!player.isNoble())
		{
			player.sendPacket(SystemMessageId.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			return false;
		}
		
		if (player.isSubClassActive())
		{
			player.sendPacket(SystemMessageId.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER);
			return false;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_JOIN_OLYMPIAD_POSSESSING_S1).addItemName(player.getCursedWeaponEquippedId()));
			return false;
		}
		
		if (player.getStatus().isOverburden())
		{
			player.sendPacket(SystemMessageId.SINCE_80_PERCENT_OR_MORE_OF_YOUR_INVENTORY_SLOTS_ARE_FULL_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			return false;
		}
		
		if (isRegistered(player, true))
			return false;
		
		if (isInCompetition(player, true))
			return false;
		
		StatSet set = Olympiad.getInstance().getNobleStats(player.getObjectId());
		if (set == null)
		{
			set = new StatSet();
			set.set(Olympiad.CLASS_ID, player.getBaseClass());
			set.set(Olympiad.CHAR_NAME, player.getName());
			set.set(Olympiad.POINTS, Config.OLY_START_POINTS);
			set.set(Olympiad.COMP_DONE, 0);
			set.set(Olympiad.COMP_WON, 0);
			set.set(Olympiad.COMP_LOST, 0);
			set.set(Olympiad.COMP_DRAWN, 0);
			
			Olympiad.getInstance().addNobleStats(player.getObjectId(), set);
		}
		
		final int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
		if (points <= 0)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/olympiad/noble_nopoints1.htm");
			html.replace("%objectId%", npc.getObjectId());
			player.sendPacket(html);
			return false;
		}
		
		return true;
	}
	
	public static final OlympiadManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OlympiadManager INSTANCE = new OlympiadManager();
	}
}