package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class BalanceLife implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.BALANCE_LIFE
	};
	
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		final ISkillHandler handler = SkillHandler.getInstance().getHandler(SkillType.BUFF);
		if (handler != null)
			handler.useSkill(activeChar, skill, targets);
		
		final Player player = activeChar.getActingPlayer();
		final List<Creature> finalList = new ArrayList<>();
		
		double fullHP = 0;
		double currentHPs = 0;
		
		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature))
				continue;
			
			final Creature target = ((Creature) obj);
			if (target.isDead())
				continue;
			
			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar)
			{
				if (target instanceof Player && ((Player) target).isCursedWeaponEquipped())
					continue;
				else if (player != null && player.isCursedWeaponEquipped())
					continue;
			}
			
			fullHP += target.getStatus().getMaxHp();
			currentHPs += target.getStatus().getHp();
			
			// Add the character to the final list.
			finalList.add(target);
		}
		
		if (!finalList.isEmpty())
		{
			double percentHP = currentHPs / fullHP;
			
			for (Creature target : finalList)
				target.getStatus().setHp(target.getStatus().getMaxHp() * percentHP);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}