package net.sf.l2j.gameserver.scripting.script.ai.area;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Primeval Isle AIs. This script controls following behaviors :
 * <ul>
 * <li>Sprigant : cast a spell if you enter in aggro range, finish task if die or none around.</li>
 * <li>Ancient Egg : call all NPCs in a 2k range if attacked.</li>
 * <li>Pterosaurs and Tyrannosaurus : can see through Silent Move.</li>
 * </ul>
 */
public class PrimevalIsle extends AttackableAIScript
{
	private static final int[] SPRIGANTS =
	{
		18345,
		18346
	};

	private static final int[] MOBIDS =
	{
		22199,
		22215,
		22216,
		22217
	};

	private static final int ANCIENT_EGG = 18344;

	private static final L2Skill ANESTHESIA = SkillTable.getInstance().getInfo(5085, 1);
	private static final L2Skill POISON = SkillTable.getInstance().getInfo(5086, 1);

	public PrimevalIsle()
	{
		super("ai/area");
	}

	@Override
	protected void registerNpcs()
	{
		addEventIds(SPRIGANTS, EventHandler.MY_DYING, EventHandler.SEE_CREATURE);
		addAttacked(ANCIENT_EGG);
		addCreated(MOBIDS);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("sprigant_skill_cast"))
		{
			int playableCounter = 0;
			for (Playable playable : npc.getKnownTypeInRadius(Playable.class, npc.getTemplate().getAggroRange()))
			{
				if (!playable.isDead())
				{
					playableCounter++;
				}
			}

			// If no one is inside aggro range, drop the task.
			if (playableCounter == 0)
			{
				cancelQuestTimers("sprigant_skill_cast", npc);
				return null;
			}

			npc.getAI().tryToCast(npc, (npc.getNpcId() == 18345) ? ANESTHESIA : POISON);
		}
		return null;
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (Rnd.get(100) < 80)
		{
			for (Attackable called : attacker.getKnownTypeInRadius(Attackable.class, 300))
			{
				// Called is dead, or got already target registered.
				if (called.isDead() || called.getAggroList().containsKey(attacker))
				{
					continue;
				}

				// TODO Must be a swap of aggro with highest aggro (eg. ScriptEvent 10016).
				called.forceAttack(attacker, 1);
			}
		}
	}

	@Override
	public void onCreated(Npc npc)
	{
		if (npc instanceof Attackable)
		{
			((Attackable) npc).seeThroughSilentMove(true);
		}

		super.onCreated(npc);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		cancelQuestTimers("sprigant_skill_cast", npc);

		super.onMyDying(npc, killer);
	}

	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (creature instanceof Playable)
		{
			npc.getAI().tryToCast(npc, (npc.getNpcId() == 18345) ? ANESTHESIA : POISON);

			// Launch a task every 15sec.
			startQuestTimerAtFixedRate("sprigant_skill_cast", npc, null, 15000);
		}
		super.onSeeCreature(npc, creature);
	}
}