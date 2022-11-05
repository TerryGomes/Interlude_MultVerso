package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectSeed;

public class ConditionElementSeed extends Condition
{
	private static final int[] SEED_SKILLS =
	{
		1285,
		1286,
		1287
	};
	
	private final int[] _requiredSeeds;
	
	public ConditionElementSeed(int[] seeds)
	{
		_requiredSeeds = seeds;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, L2Skill skill, Item item)
	{
		int[] seeds = new int[3];
		for (int i = 0; i < seeds.length; i++)
		{
			seeds[i] = (effector.getFirstEffect(SEED_SKILLS[i]) instanceof EffectSeed ? ((EffectSeed) effector.getFirstEffect(SEED_SKILLS[i])).getPower() : 0);
			if (seeds[i] >= _requiredSeeds[i])
				seeds[i] -= _requiredSeeds[i];
			else
				return false;
		}
		
		if (_requiredSeeds[3] > 0)
		{
			int count = 0;
			for (int i = 0; i < seeds.length && count < _requiredSeeds[3]; i++)
			{
				if (seeds[i] > 0)
				{
					seeds[i]--;
					count++;
				}
			}
			if (count < _requiredSeeds[3])
				return false;
		}
		
		if (_requiredSeeds[4] > 0)
		{
			int count = 0;
			for (int i = 0; i < seeds.length && count < _requiredSeeds[4]; i++)
			{
				count += seeds[i];
			}
			if (count < _requiredSeeds[4])
				return false;
		}
		
		return true;
	}
}