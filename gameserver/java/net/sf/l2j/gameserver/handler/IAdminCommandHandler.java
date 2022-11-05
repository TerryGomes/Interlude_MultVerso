package net.sf.l2j.gameserver.handler;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public interface IAdminCommandHandler
{
	final CLogger LOGGER = new CLogger(IAdminCommandHandler.class.getName());
	
	public static final int PAGE_LIMIT_7 = 7;
	public static final int PAGE_LIMIT_8 = 8;
	public static final int PAGE_LIMIT_10 = 10;
	public static final int PAGE_LIMIT_15 = 15;
	public static final int PAGE_LIMIT_18 = 18;
	public static final int PAGE_LIMIT_20 = 20;
	
	public void useAdminCommand(String command, Player player);
	
	public String[] getAdminCommandList();
	
	public default Player getTargetPlayer(Player player, String playerName, boolean defaultAdmin)
	{
		final Player toTest = World.getInstance().getPlayer(playerName);
		return (toTest == null) ? getTargetPlayer(player, defaultAdmin) : toTest;
	}
	
	public default Player getTargetPlayer(Player player, boolean defaultAdmin)
	{
		return getTarget(Player.class, player, defaultAdmin);
	}
	
	public default Creature getTargetCreature(Player player, boolean defaultAdmin)
	{
		return getTarget(Creature.class, player, defaultAdmin);
	}
	
	/**
	 * @param <A> : The {@link Class} to cast upon result.
	 * @param type : The {@link Class} type to check.
	 * @param player : The {@link Player} used to retrieve the target from.
	 * @param defaultAdmin : If true, we test the {@link Player} itself, in case target was invalid, otherwise we return null directly.
	 * @return The target of the {@link Player} set as parameter, under the given {@link Class} type. If the target isn't assignable to that {@link Class}, or if the defaultAdmin is set to true and the {@link Player} instance isn't assignable to that {@link Class} aswell, then return null.
	 */
	@SuppressWarnings("unchecked")
	public default <A> A getTarget(Class<A> type, Player player, boolean defaultAdmin)
	{
		final WorldObject target = player.getTarget();
		
		// Current player target is null or not assignable, return either himself (if type was assignable to Player) or null.
		if (target == null || !type.isAssignableFrom(target.getClass()))
			return (defaultAdmin && type.isAssignableFrom(player.getClass())) ? (A) player : null;
		
		return (A) target;
	}
	
	public default void sendFile(Player player, String filename)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/" + filename);
		player.sendPacket(html);
	}
}