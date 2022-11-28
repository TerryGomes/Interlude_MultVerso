package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.List;

import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * Gordon behavior. This boss attacks cursed weapons holders at sight.<br>
 * When he isn't attacking, he follows a pre-established path around Goddard castle.
 */
public class Gordon extends AttackableAIScript
{
	private static final int GORDON = 29095;

	public Gordon()
	{
		super("ai/boss");
	}

	@Override
	protected void registerNpcs()
	{
		addEventIds(GORDON, EventHandler.CREATED, EventHandler.MY_DYING, EventHandler.NO_DESIRE, EventHandler.SEE_ITEM);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("ai_loop"))
		{
			// Doesn't bother about task AI if the NPC is dead or already fighting.
			if (npc.isDead() || AttackStanceTaskManager.getInstance().isInAttackStance(npc))
			{
				return null;
			}

			// Check if player have Cursed Weapon and is in radius.
			for (Player pc : npc.getKnownTypeInRadius(Player.class, 450))
			{
				if (pc.isCursedWeaponEquipped())
				{
					((Attackable) npc).forceAttack(pc, 200);
					return null;
				}
			}
		}
		else if (name.equalsIgnoreCase("2003"))
		{
			npc.lookItem(500, 1, 8190, 8689);
		}
		return super.onTimer(name, npc, player);
	}

	@Override
	public void onCreated(Npc npc)
	{
		// Launch the AI loop.
		startQuestTimerAtFixedRate("ai_loop", npc, null, 1000);
		startQuestTimerAtFixedRate("2003", npc, null, 3000);

		super.onCreated(npc);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		cancelQuestTimers("ai_loop", npc);

		super.onMyDying(npc, killer);
	}

	@Override
	public void onNoDesire(Npc npc)
	{
		// Do nothing, otherwise he tries to returnHome.
	}

	@Override
	public void onSeeItem(Npc npc, int quantity, List<ItemInstance> items)
	{
		for (ItemInstance item : items)
		{
			npc.getAI().tryToPickUp(item.getObjectId(), false);
		}
	}
}