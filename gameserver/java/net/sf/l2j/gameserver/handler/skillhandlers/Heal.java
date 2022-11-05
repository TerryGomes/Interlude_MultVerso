package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Heal implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.HEAL,
		SkillType.HEAL_STATIC
	};
	
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		final ISkillHandler handler = SkillHandler.getInstance().getHandler(SkillType.BUFF);
		if (handler != null)
			handler.useSkill(activeChar, skill, targets);
		
		double power = skill.getPower() + activeChar.getStatus().calcStat(Stats.HEAL_PROFICIENCY, 0, null, null);
		
		if (skill.getSkillType() != SkillType.HEAL_STATIC)
		{
			final boolean sps = activeChar.isChargedShot(ShotType.SPIRITSHOT);
			final boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
			
			double staticShotBonus = 0;
			double mAtkMul = 1.;
			
			if ((sps || bsps) && (activeChar instanceof Player && activeChar.getActingPlayer().isMageClass()) || activeChar instanceof Summon)
			{
				staticShotBonus = skill.getMpConsume(); // static bonus for spiritshots
				
				if (bsps)
				{
					mAtkMul = 4.;
					staticShotBonus *= 2.4;
				}
				else
					mAtkMul = 2.;
			}
			else if ((sps || bsps) && activeChar instanceof Npc)
			{
				staticShotBonus = 2.4 * skill.getMpConsume(); // always blessed spiritshots
				mAtkMul = 4.;
			}
			else
			{
				// shot dynamic bonus
				if (bsps)
					mAtkMul *= 4.;
				else
					mAtkMul += 1.;
			}
			
			power += staticShotBonus + Math.sqrt(mAtkMul * activeChar.getStatus().getMAtk(activeChar, null));
			
			if (!skill.isPotion())
				activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, skill.isStaticReuse());
		}
		
		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature))
				continue;
			
			final Creature target = ((Creature) obj);
			if (!target.canBeHealed())
				continue;
			
			final double amount = target.getStatus().addHp(power * target.getStatus().calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100.);
			
			if (target instanceof Player)
			{
				if (skill.getId() == 4051)
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.REJUVENATING_HP));
				else
				{
					if (activeChar != target)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1).addCharName(activeChar).addNumber((int) amount));
					else
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED).addNumber((int) amount));
				}
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}