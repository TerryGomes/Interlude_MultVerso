package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.SiegeSummon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Disablers implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.STUN,
		SkillType.ROOT,
		SkillType.SLEEP,
		SkillType.CONFUSION,
		SkillType.AGGDAMAGE,
		SkillType.AGGREDUCE,
		SkillType.AGGREDUCE_CHAR,
		SkillType.AGGREMOVE,
		SkillType.MUTE,
		SkillType.FAKE_DEATH,
		SkillType.NEGATE,
		SkillType.CANCEL_DEBUFF,
		SkillType.PARALYZE,
		SkillType.ERASE,
		SkillType.BETRAY
	};
	
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		final SkillType type = skill.getSkillType();
		
		final boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
		
		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature))
				continue;
			
			Creature target = (Creature) obj;
			if (target.isDead() || (target.isInvul() && !target.isParalyzed())) // bypass if target is dead or invul (excluding invul from Petrification)
				continue;
			
			if (skill.isOffensive() && target.getFirstEffect(EffectType.BLOCK_DEBUFF) != null)
				continue;
			
			final ShieldDefense sDef = Formulas.calcShldUse(activeChar, target, skill, false);
			
			switch (type)
			{
				case BETRAY:
					if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps))
						skill.getEffects(activeChar, target, sDef, bsps);
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					break;
				
				case FAKE_DEATH:
					// stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
					skill.getEffects(activeChar, target, sDef, bsps);
					break;
				
				case ROOT:
				case STUN:
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps))
						skill.getEffects(activeChar, target, sDef, bsps);
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;
				
				case SLEEP:
				case PARALYZE: // use same as root for now
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps))
						skill.getEffects(activeChar, target, sDef, bsps);
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;
				
				case MUTE:
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps))
					{
						// stop same type effect if available
						for (AbstractEffect effect : target.getAllEffects())
						{
							if (effect.getTemplate().getStackOrder() == 99)
								continue;
							
							if (effect.getSkill().getSkillType() == type)
								effect.exit();
						}
						skill.getEffects(activeChar, target, sDef, bsps);
					}
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;
				
				case CONFUSION:
					// do nothing if not on mob
					if (target instanceof Attackable)
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps))
						{
							for (AbstractEffect effect : target.getAllEffects())
							{
								if (effect.getTemplate().getStackOrder() == 99)
									continue;
								
								if (effect.getSkill().getSkillType() == type)
									effect.exit();
							}
							skill.getEffects(activeChar, target, sDef, bsps);
						}
						else
						{
							if (activeChar instanceof Player)
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
						}
					}
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INVALID_TARGET));
					break;
				
				case AGGDAMAGE:
					if (target instanceof Attackable)
						target.getAI().notifyEvent(AiEventType.AGGRESSION, activeChar, (int) (skill.getPower() / (target.getStatus().getLevel() + 7) * 150));
					
					skill.getEffects(activeChar, target, sDef, bsps);
					break;
				
				case AGGREDUCE:
					// TODO these skills needs to be rechecked
					if (target instanceof Attackable)
					{
						skill.getEffects(activeChar, target, sDef, bsps);
						
						if (skill.getPower() > 0)
							((Attackable) target).getAggroList().reduceAllHate((int) skill.getPower());
						else
						{
							final int hate = ((Attackable) target).getAggroList().getHate(activeChar);
							final double diff = hate - target.getStatus().calcStat(Stats.AGGRESSION, hate, target, skill);
							if (diff > 0)
								((Attackable) target).getAggroList().reduceAllHate((int) diff);
						}
					}
					break;
				
				case AGGREDUCE_CHAR:
					// TODO these skills need to be rechecked
					if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps))
					{
						if (target instanceof Attackable)
							((Attackable) target).getAggroList().stopHate(activeChar);
						
						skill.getEffects(activeChar, target, sDef, bsps);
					}
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					}
					break;
				
				case AGGREMOVE:
					// TODO these skills needs to be rechecked
					if (target instanceof Attackable && !target.isRaidRelated())
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps))
						{
							if (skill.getTargetType() == SkillTargetType.UNDEAD)
							{
								if (target.isUndead())
									((Attackable) target).getAggroList().stopHate(activeChar);
							}
							else
								((Attackable) target).getAggroList().stopHate(activeChar);
						}
						else
						{
							if (activeChar instanceof Player)
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
						}
					}
					break;
				
				case ERASE:
					// doesn't affect siege summons
					if (Formulas.calcSkillSuccess(activeChar, target, skill, sDef, bsps) && !(target instanceof SiegeSummon))
					{
						final Player summonOwner = ((Summon) target).getOwner();
						final Summon summonPet = summonOwner.getSummon();
						if (summonPet != null)
						{
							summonPet.unSummon(summonOwner);
							summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
						}
					}
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					}
					break;
				
				case CANCEL_DEBUFF:
					final AbstractEffect[] effects = target.getAllEffects();
					if (effects == null || effects.length == 0)
						break;
					
					int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
					for (AbstractEffect effect : effects)
					{
						if (!effect.getSkill().isDebuff() || !effect.getSkill().canBeDispeled() || effect.getTemplate().getStackOrder() == 99)
							continue;
						
						effect.exit();
						
						if (count > -1)
						{
							count++;
							if (count >= skill.getMaxNegatedEffects())
								break;
						}
					}
					break;
				
				case NEGATE:
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					// Skills with negateId (skillId)
					if (skill.getNegateId().length != 0)
					{
						for (int id : skill.getNegateId())
						{
							if (id != 0)
								target.stopSkillEffects(id);
						}
					}
					// All others negate type skills
					else
					{
						for (AbstractEffect effect : target.getAllEffects())
						{
							if (effect.getTemplate().getStackOrder() == 99)
								continue;
							
							final L2Skill effectSkill = effect.getSkill();
							for (SkillType skillType : skill.getNegateStats())
							{
								// If power is -1 the effect is always removed without lvl check
								if (skill.getNegateLvl() == -1)
								{
									if (effectSkill.getSkillType() == skillType || (effectSkill.getEffectType() != null && effectSkill.getEffectType() == skillType))
										effect.exit();
								}
								// Remove the effect according to its power.
								else
								{
									if (effectSkill.getEffectType() != null && effectSkill.getEffectAbnormalLvl() >= 0)
									{
										if (effectSkill.getEffectType() == skillType && effectSkill.getEffectAbnormalLvl() <= skill.getNegateLvl())
											effect.exit();
									}
									else if (effectSkill.getSkillType() == skillType && effectSkill.getAbnormalLvl() <= skill.getNegateLvl())
										effect.exit();
								}
							}
						}
					}
					skill.getEffects(activeChar, target, sDef, bsps);
					break;
			}
		}
		
		if (skill.hasSelfEffects())
		{
			final AbstractEffect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();
			
			skill.getEffectsSelf(activeChar);
		}
		activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, skill.isStaticReuse());
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}