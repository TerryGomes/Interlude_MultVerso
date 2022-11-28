package net.sf.l2j.gameserver.scripting.script.ai.area;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Those monsters don't attack at sight players owning itemId 8064, 8065 or 8067.
 */
public class PaganTemple extends AttackableAIScript
{
	public PaganTemple()
	{
		super("ai/area");
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(22136);
		addSeeCreature(22136);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		final Player player = attacker.getActingPlayer();
		if (npc.isVisible() && !npc.isDead())
		{
			if (!player.getInventory().hasAtLeastOneItem(8064, 8065, 8067))
			{
				player.teleportTo(43797, -48141, -792, 0);
			}
		}

		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		final Player player = creature.getActingPlayer();
		if (player != null)
		{
			if (!player.getInventory().hasAtLeastOneItem(8064, 8065, 8067))
			{
				player.teleportTo(43797, -48141, -792, 0);
			}
		}
	}
}