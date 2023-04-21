package net.sf.l2j.gameserver.scripting.script.ai.area;

import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * This script holds MoS monsters behavior. If they see you with an equipped weapon, they will speak and attack you.
 */
public class MonasteryOfSilence extends AttackableAIScript
{
	private static final int[] BROTHERS_SEEKERS_MONKS =
	{
		22124,
		22126,
		22129
	};

	public MonasteryOfSilence()
	{
		super("ai/area");
	}

	@Override
	protected void registerNpcs()
	{
		addEventIds(BROTHERS_SEEKERS_MONKS, EventHandler.SEE_CREATURE, EventHandler.USE_SKILL_FINISHED);
	}

	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (creature instanceof Player)
		{
			final Player player = creature.getActingPlayer();
			if (!npc.isInCombat() && player.getActiveWeaponInstance() != null)
			{
				npc.getAI().tryToCast(player, 4589, 8);
			}
		}
	}

	@Override
	public void onUseSkillFinished(Npc npc, Player player, L2Skill skill)
	{
		if (skill.getId() == 4589 && npc.getScriptValue() == 0)
		{
			npc.broadcastNpcSay(NpcStringId.ID_1022122, player.getName());
			npc.forceAttack(player, 10000);
			npc.setScriptValue(1);
		}
		super.onUseSkillFinished(npc, player, skill);
	}
}