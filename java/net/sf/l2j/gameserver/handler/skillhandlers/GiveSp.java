package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.skills.L2Skill;

public class GiveSp implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.GIVE_SP
	};

	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets, ItemInstance itemInstance)
	{
		final int spToAdd = (int) skill.getPower();

		for (WorldObject obj : targets)
		{
			final Creature target = (Creature) obj;
			if (target != null)
			{
				target.addExpAndSp(0, spToAdd);
			}
		}
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}