package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;

public class DrainSoul implements ISkillHandler
{
	private static final String qn = "Q350_EnhanceYourWeapon";
	
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.DRAIN_SOUL
	};
	
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		// Check player.
		if (activeChar == null || activeChar.isDead() || !(activeChar instanceof Player))
			return;
		
		// Check quest condition.
		final Player player = (Player) activeChar;
		QuestState st = player.getQuestList().getQuestState(qn);
		if (st == null || !st.isStarted())
			return;
		
		// Get target.
		WorldObject target = targets[0];
		if (!(target instanceof Monster))
			return;
		
		// Check monster.
		final Monster mob = (Monster) target;
		if (mob.isDead())
			return;
		
		// Range condition, cannot be higher than skill's effectRange.
		if (!player.isIn3DRadius(mob, skill.getEffectRange()))
			return;
		
		// Register.
		mob.registerAbsorber(player);
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}