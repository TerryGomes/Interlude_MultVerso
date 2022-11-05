package net.sf.l2j.gameserver.scripting.script.ai.area;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * This AI handles following behaviors :
 * <ul>
 * <li>Cannibalistic Stakato Leader : try to eat a Follower, if any around, at low HPs.</li>
 * <li>Female Spiked Stakato : when Male dies, summons 3 Spiked Stakato Guards.</li>
 * <li>Male Spiked Stakato : when Female dies, transforms in stronger form.</li>
 * <li>Spiked Stakato Baby : when Spiked Stakato Nurse dies, her baby summons 3 Spiked Stakato Captains.</li>
 * <li>Spiked Stakato Nurse : when Spiked Stakato Baby dies, transforms in stronger form.</li>
 * </ul>
 * As NCSoft implemented it on postIL, but skills exist since IL, I decided to implemented that script to "honor" the idea (which is kinda funny).
 */
public class StakatoNest extends AttackableAIScript
{
	private static final int SPIKED_STAKATO_GUARD = 22107;
	private static final int FEMALE_SPIKED_STAKATO = 22108;
	private static final int MALE_SPIKED_STAKATO_1 = 22109;
	private static final int MALE_SPIKED_STAKATO_2 = 22110;
	
	private static final int STAKATO_FOLLOWER = 22112;
	private static final int CANNIBALISTIC_STAKATO_LEADER_1 = 22113;
	private static final int CANNIBALISTIC_STAKATO_LEADER_2 = 22114;
	
	private static final int SPIKED_STAKATO_CAPTAIN = 22117;
	private static final int SPIKED_STAKATO_NURSE_1 = 22118;
	private static final int SPIKED_STAKATO_NURSE_2 = 22119;
	private static final int SPIKED_STAKATO_BABY = 22120;
	
	public StakatoNest()
	{
		super("ai/area");
	}
	
	@Override
	protected void registerNpcs()
	{
		addAttackId(CANNIBALISTIC_STAKATO_LEADER_1, CANNIBALISTIC_STAKATO_LEADER_2);
		addKillId(MALE_SPIKED_STAKATO_1, FEMALE_SPIKED_STAKATO, SPIKED_STAKATO_NURSE_1, SPIKED_STAKATO_BABY);
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.getStatus().getHpRatio() < 0.3 && Rnd.get(100) < 5)
		{
			for (Monster follower : npc.getKnownTypeInRadius(Monster.class, 400))
			{
				if (follower.getNpcId() == STAKATO_FOLLOWER && !follower.isDead())
				{
					npc.getAI().tryToCast(follower, (npc.getNpcId() == CANNIBALISTIC_STAKATO_LEADER_2) ? 4072 : 4073, 1);
					
					ThreadPool.schedule(() ->
					{
						if (npc.isDead() || follower.isDead())
							return;
						
						npc.getStatus().addHp(follower.getStatus().getHp() / 2);
						follower.doDie(follower);
					}, 3000L);
					
					break;
				}
			}
		}
		return super.onAttack(npc, attacker, damage, skill);
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		switch (npc.getNpcId())
		{
			case MALE_SPIKED_STAKATO_1:
				for (Monster female : npc.getKnownTypeInRadius(Monster.class, 400))
				{
					if (female.getNpcId() == FEMALE_SPIKED_STAKATO && !female.isDead())
					{
						for (int i = 0; i < 3; i++)
						{
							final Npc guard = addSpawn(SPIKED_STAKATO_GUARD, female, true, 0, false);
							guard.forceAttack(killer, 200);
						}
					}
				}
				break;
			
			case FEMALE_SPIKED_STAKATO:
				for (Monster morphingMale : npc.getKnownTypeInRadius(Monster.class, 400))
				{
					if (morphingMale.getNpcId() == MALE_SPIKED_STAKATO_1 && !morphingMale.isDead())
					{
						final Npc newForm = addSpawn(MALE_SPIKED_STAKATO_2, morphingMale, true, 0, false);
						newForm.forceAttack(killer, 200);
						
						morphingMale.deleteMe();
					}
				}
				break;
			
			case SPIKED_STAKATO_NURSE_1:
				for (Monster baby : npc.getKnownTypeInRadius(Monster.class, 400))
				{
					if (baby.getNpcId() == SPIKED_STAKATO_BABY && !baby.isDead())
					{
						for (int i = 0; i < 3; i++)
						{
							final Npc captain = addSpawn(SPIKED_STAKATO_CAPTAIN, baby, true, 0, false);
							captain.forceAttack(killer, 200);
						}
					}
				}
				break;
			
			case SPIKED_STAKATO_BABY:
				for (Monster morphingNurse : npc.getKnownTypeInRadius(Monster.class, 400))
				{
					if (morphingNurse.getNpcId() == SPIKED_STAKATO_NURSE_1 && !morphingNurse.isDead())
					{
						final Npc newForm = addSpawn(SPIKED_STAKATO_NURSE_2, morphingNurse, true, 0, false);
						newForm.forceAttack(killer, 200);
						
						morphingNurse.deleteMe();
					}
				}
				break;
		}
		return super.onKill(npc, killer);
	}
}