package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class ImperialGravekeeper extends AttackableAIScript
{
	private static final int IMPERIAL_SLAVE = 27180;
	private static final int IMPERIAL_GRAVEKEEPER = 27181;
	
	private static final L2Skill SELF_HEAL = SkillTable.getInstance().getInfo(4080, 1);
	
	public ImperialGravekeeper()
	{
		super("ai/individual");
	}
	
	@Override
	protected void registerNpcs()
	{
		addEventIds(IMPERIAL_GRAVEKEEPER, ScriptEventType.ON_ATTACK);
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		// Get HP ratio.
		final int ratio = (int) (npc.getStatus().getHpRatio() * 100);
		
		// The script value contains 2 parameters encoded, xxyy:
		// xx - HP percentage to spawn minions, 80%, 40% and 20%
		// yy - HP percentage to teleport, 50% and 30%
		final int sv = npc.getScriptValue();
		int teleport = sv % 100;
		int minion = sv / 100;
		
		// Handle attacker teleport.
		if (ratio < teleport)
		{
			if (teleport == 50)
			{
				teleport = 30;
				// TODO It teleport players from its territory -> rework when territories are present.
				for (Player player : npc.getKnownTypeInRadius(Player.class, 900))
					player.teleportTo(171104, 6496, -2706, 200);
			}
			else
			{
				teleport = 0;
				// TODO It teleport players from its territory -> rework when territories are present.
				for (Player player : npc.getKnownTypeInRadius(Player.class, 900))
					player.teleportTo(179520, 6464, -2706, 200);
			}
		}
		
		if (ratio > 50)
			teleport = 50;
		else if (ratio > 30)
			teleport = 30;
		
		// Handle minion spawn.
		if (ratio < minion)
		{
			if (minion == 80)
				minion = 40;
			else if (minion == 40)
				minion = 20;
			else
				minion = 0;
			
			for (int i = 0; i < 4; i++)
				addSpawn(IMPERIAL_SLAVE, npc, true, 0, false);
		}
		
		if (ratio > 80)
			minion = 80;
		else if (ratio > 40)
			minion = 40;
		else if (ratio > 20)
			minion = 20;
		
		// Save script value.
		npc.setScriptValue(minion * 100 + teleport);
		
		// Cast self-heal whenever possible.
		npc.getAI().tryToCast(npc, SELF_HEAL);
		
		return super.onAttack(npc, attacker, damage, skill);
	}
}