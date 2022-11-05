package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

public class AdminManage implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_cancel",
		"admin_heal",
		"admin_kill",
		"admin_res"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		String name = null;
		int radius = 0;
		
		final int paramCount = st.countTokens();
		if (paramCount == 2)
		{
			name = st.nextToken();
			radius = Integer.parseInt(st.nextToken());
		}
		else if (paramCount == 1)
		{
			final String paramToTest = st.nextToken();
			if (StringUtil.isDigit(paramToTest))
				radius = Integer.parseInt(paramToTest);
			else
				name = paramToTest;
		}
		
		// Retrieve the target if it's a Creature, or self otherwise.
		Creature targetCreature = getTargetCreature(player, true);
		
		// If name exists and the Player can be retrieved, he becomes the new target.
		if (!StringUtil.isEmpty(name))
		{
			final Player worldPlayer = World.getInstance().getPlayer(name);
			if (worldPlayer != null)
				targetCreature = worldPlayer;
		}
		
		// After all tests, if command target is null, abort.
		if (targetCreature == null)
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if (command.startsWith("admin_cancel"))
		{
			if (radius > 0)
			{
				targetCreature.stopAllEffects();
				
				for (Creature knownCreature : player.getKnownTypeInRadius(Creature.class, radius))
					knownCreature.stopAllEffects();
				
				player.sendMessage("All creatures around " + targetCreature.getName() + " within " + radius + " range got their buffs canceled.");
			}
			else
			{
				targetCreature.stopAllEffects();
				player.sendMessage(targetCreature.getName() + " got her/his buffs canceled.");
			}
		}
		else if (command.startsWith("admin_heal"))
		{
			if (radius > 0)
			{
				heal(targetCreature);
				
				for (Creature knownCreature : targetCreature.getKnownTypeInRadius(Creature.class, radius))
					heal(knownCreature);
				
				player.sendMessage("All creatures around " + targetCreature.getName() + " within " + radius + " range are healed.");
			}
			else if (heal(targetCreature))
				player.sendMessage(targetCreature.getName() + " is healed.");
		}
		else if (command.startsWith("admin_kill"))
		{
			if (radius > 0)
			{
				kill(targetCreature, player);
				
				for (Creature knownCreature : targetCreature.getKnownTypeInRadius(Creature.class, radius))
					kill(knownCreature, player);
				
				player.sendMessage("All creatures around " + targetCreature.getName() + " within " + radius + " range are killed.");
			}
			else if (kill(targetCreature, player))
				player.sendMessage(targetCreature.getName() + " is killed.");
		}
		else if (command.startsWith("admin_res"))
		{
			if (radius > 0)
			{
				resurrect(targetCreature);
				
				for (Creature knownCreature : targetCreature.getKnownTypeInRadius(Creature.class, radius))
					resurrect(knownCreature);
				
				player.sendMessage("All creatures around " + targetCreature.getName() + " within " + radius + " range are resurrected.");
			}
			else if (resurrect(targetCreature))
				player.sendMessage(targetCreature.getName() + " is resurrected.");
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static boolean heal(Creature creature)
	{
		if (creature.isDead())
			return false;
		
		if (creature instanceof Player)
			((Player) creature).getStatus().setMaxCpHpMp();
		else
			creature.getStatus().setMaxHpMp();
		
		return true;
	}
	
	private static boolean kill(Creature creature, Player player)
	{
		if (creature.isDead() || creature == player)
			return false;
		
		creature.stopAllEffects();
		creature.reduceCurrentHp(creature.getStatus().getMaxHp() + creature.getStatus().getMaxCp() + 1, player, null);
		return true;
	}
	
	private static boolean resurrect(Creature creature)
	{
		if (!creature.isDead())
			return false;
		
		// If the target is a player, then restore the XP lost on death.
		if (creature instanceof Player)
			((Player) creature).restoreExp(100.0);
		// If the target is an NPC, then abort it's auto decay and respawn.
		else
			DecayTaskManager.getInstance().cancel(creature);
		
		creature.doRevive();
		return true;
	}
}