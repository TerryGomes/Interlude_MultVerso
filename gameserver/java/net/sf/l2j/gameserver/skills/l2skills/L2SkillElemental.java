package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class L2SkillElemental extends L2Skill
{
	private final int[] _seeds;
	private final boolean _seedAny;
	
	public L2SkillElemental(StatSet set)
	{
		super(set);
		
		_seeds = new int[3];
		_seeds[0] = set.getInteger("seed1", 0);
		_seeds[1] = set.getInteger("seed2", 0);
		_seeds[2] = set.getInteger("seed3", 0);
		
		if (set.getInteger("seed_any", 0) == 1)
			_seedAny = true;
		else
			_seedAny = false;
	}
	
	@Override
	public void useSkill(Creature activeChar, WorldObject[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		final boolean sps = activeChar.isChargedShot(ShotType.SPIRITSHOT);
		final boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
		
		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature))
				continue;
			
			final Creature target = ((Creature) obj);
			if (target.isAlikeDead())
				continue;
			
			boolean charged = true;
			if (!_seedAny)
			{
				for (int _seed : _seeds)
				{
					if (_seed != 0)
					{
						final AbstractEffect effect = target.getFirstEffect(_seed);
						if (effect == null || !effect.getInUse())
						{
							charged = false;
							break;
						}
					}
				}
			}
			else
			{
				charged = false;
				for (int _seed : _seeds)
				{
					if (_seed != 0)
					{
						final AbstractEffect effect = target.getFirstEffect(_seed);
						if (effect != null && effect.getInUse())
						{
							charged = true;
							break;
						}
					}
				}
			}
			
			if (!charged)
			{
				activeChar.sendMessage("Target is not charged by elements.");
				continue;
			}
			
			final boolean isCrit = Formulas.calcMCrit(activeChar, target, this);
			final ShieldDefense sDef = Formulas.calcShldUse(activeChar, target, this, false);
			
			int damage = (int) Formulas.calcMagicDam(activeChar, target, this, sDef, sps, bsps, isCrit);
			if (damage > 0)
			{
				target.reduceCurrentHp(damage, activeChar, this);
				
				// Manage cast break of the target (calculating rate, sending message...)
				Formulas.calcCastBreak(target, damage);
				
				activeChar.sendDamageMessage(target, damage, false, false, false);
			}
			
			// activate attacked effects, if any
			target.stopSkillEffects(getId());
			getEffects(activeChar, target, sDef, bsps);
		}
		
		activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
	}
}