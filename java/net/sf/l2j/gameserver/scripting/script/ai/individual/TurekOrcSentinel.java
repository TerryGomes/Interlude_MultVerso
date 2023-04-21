package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class TurekOrcSentinel extends AttackableAIScript
{
	private static final int TUREK_ORC_SENTINEL = 20500;

	private Location _warlordPosition;

	public TurekOrcSentinel()
	{
		super("ai/individual");
	}

	@Override
	protected void registerNpcs()
	{
		addAttacked(TUREK_ORC_SENTINEL);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.getStatus().getHp() < npc.getStatus().getMaxHp() * 0.5 && Rnd.get(100) < 10)
		{
			for (Monster mob : npc.getKnownTypeInRadius(Monster.class, 2000))
			{
				if (mob.getNpcId() == 20495 && npc.getTarget() != mob)
				{
					_warlordPosition = new Location(mob.getX() + Rnd.get(-150, 150), mob.getY() + Rnd.get(-150, 150), mob.getZ());
					npc.setTarget(mob);
				}

				if (_warlordPosition != null && npc.getTarget() != null && npc.isIn2DRadius(_warlordPosition, 2000))
				{
					npc.disableCoreAi(true);
					npc.forceRunStance();
					npc.getAI().tryToMoveTo(_warlordPosition, null);
				}
			}

			npc.broadcastNpcSay(NpcStringId.getNpcMessage(Rnd.get(1000007, 1000027)));
		}

		super.onAttacked(npc, attacker, damage, skill);
	}
}
