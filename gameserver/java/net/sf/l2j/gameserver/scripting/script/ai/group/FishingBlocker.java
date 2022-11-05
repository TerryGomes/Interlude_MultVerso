package net.sf.l2j.gameserver.scripting.script.ai.group;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Fishing monster behavior, occuring at 5% of a successful fishing action.
 */
public class FishingBlocker extends AttackableAIScript
{
	private static final int[] FISHING_BLOCKERS =
	{
		18319,
		18320,
		18321,
		18322,
		18323,
		18324,
		18325,
		18326
	};
	
	public FishingBlocker()
	{
		super("ai/group");
	}
	
	@Override
	protected void registerNpcs()
	{
		addEventIds(FISHING_BLOCKERS, ScriptEventType.ON_ATTACK, ScriptEventType.ON_KILL, ScriptEventType.ON_SPAWN);
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (Rnd.get(100) < 33)
			npc.broadcastNpcSay(retrieveNpcStringId(npc, 3), attacker.getName());
		
		return super.onAttack(npc, attacker, damage, skill);
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		npc.broadcastNpcSay(retrieveNpcStringId(npc, 6), killer.getName());
		
		cancelQuestTimers("3000", npc);
		
		return super.onKill(npc, killer);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		// Workaround, since it's impossible to find "summoner" in a regular way. L2OFF passes the Player using GetCreatureFromIndex, on CREATED.
		final Player player = Rnd.get(npc.getKnownTypeInRadius(Player.class, 200, p -> p.getAttackType() == WeaponType.FISHINGROD));
		if (player == null)
			npc.deleteMe();
		else
		{
			npc.forceAttack(player, 2000);
			npc.broadcastNpcSay(retrieveNpcStringId(npc, 0), player.getName());
			
			startQuestTimerAtFixedRate("3000", npc, null, 50000, 50000);
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3000"))
		{
			final IntentionType type = npc.getAI().getCurrentIntention().getType();
			if (type != IntentionType.ATTACK && type != IntentionType.CAST)
			{
				npc.deleteMe();
				
				cancelQuestTimers("3000", npc);
			}
		}
		return null;
	}
	
	private static final NpcStringId retrieveNpcStringId(Npc npc, int index)
	{
		return NpcStringId.get(1010400 + index + ((npc.getNpcId() - 18319) * 9) + Rnd.get(3));
	}
}