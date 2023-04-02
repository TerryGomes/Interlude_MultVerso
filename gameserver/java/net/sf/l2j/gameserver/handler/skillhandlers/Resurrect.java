package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

public class Resurrect implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.RESURRECT
	};

	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets, ItemInstance itemInstance)
	{
		for (WorldObject cha : targets)
		{
			final Creature target = (Creature) cha;
			if (activeChar instanceof Player)
			{
				if (cha instanceof Player)
				{
					((Player) cha).reviveRequest((Player) activeChar, skill, false);
				}
				else if (!(cha instanceof Pet) || (((Pet) cha).getOwner() == activeChar))
				{
					target.doRevive(Formulas.calcRevivePower(activeChar, skill.getPower()));
				}
				else
				{
					((Pet) cha).getOwner().reviveRequest((Player) activeChar, skill, true);
				}
			}
			else
			{
				DecayTaskManager.getInstance().cancel(target);
				target.doRevive(Formulas.calcRevivePower(activeChar, skill.getPower()));
			}
		}
		activeChar.setChargedShot(activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT) ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, skill.isStaticReuse());
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}