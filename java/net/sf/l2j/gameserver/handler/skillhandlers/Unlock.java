package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Unlock implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.UNLOCK,
		SkillType.UNLOCK_SPECIAL,
		SkillType.DELUXE_KEY_UNLOCK, // Skill ids: 2065, 2229
	};

	private static final int SKILL_BOX_KEY = 2065;

	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets, ItemInstance itemInstance)
	{
		final WorldObject object = targets[0];

		if (object instanceof Door)
		{
			final Door door = (Door) object;
			if (!door.isUnlockable() && skill.getSkillType() != SkillType.UNLOCK_SPECIAL)
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UNABLE_TO_UNLOCK_DOOR));
				return;
			}

			if (doorUnlock(skill) && (!door.isOpened()))
			{
				door.openMe();
			}
			else
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
			}
		}
		else if (object instanceof Chest)
		{
			final Chest chest = (Chest) object;
			if (chest.isDead() || chest.isInteracted())
			{
				return;
			}

			if (!chest.isBox())
			{
				chest.getAggroList().addDamageHate(activeChar, 0, 200);
				chest.getAI().tryToAttack(activeChar);
				return;
			}

			chest.setInteracted();
			if (chestUnlock(skill, chest.getStatus().getLevel()))
			{
				// Add some hate, so Monster#calculateRewards is evaluated properly.
				chest.getAggroList().addDamageHate(activeChar, 0, 200);
				chest.doDie(activeChar);
			}
			else
			{
				chest.deleteMe();
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INVALID_TARGET));
		}
	}

	private static final boolean doorUnlock(L2Skill skill)
	{
		if (skill.getSkillType() == SkillType.UNLOCK_SPECIAL)
		{
			return Rnd.get(100) < skill.getPower();
		}

		switch (skill.getLevel())
		{
			case 0:
				return false;
			case 1:
				return Rnd.get(120) < 30;
			case 2:
				return Rnd.get(120) < 50;
			case 3:
				return Rnd.get(120) < 75;
			default:
				return Rnd.get(120) < 100;
		}
	}

	private static final boolean chestUnlock(L2Skill skill, int level)
	{
		int chance = 0;

		if (skill.getSkillType() == SkillType.DELUXE_KEY_UNLOCK)
		{
			// check the chance to open the box.
			int keyLevelNeeded = (level / 10) - skill.getLevel();
			if (keyLevelNeeded < 0)
			{
				keyLevelNeeded *= -1;
			}

			// Regular keys got 60% to succeed.
			chance = ((skill.getId() == SKILL_BOX_KEY) ? 60 : 100) - keyLevelNeeded * 40;
		}
		else
		{
			if (level > 60)
			{
				if (skill.getLevel() < 10)
				{
					return false;
				}

				chance = (skill.getLevel() - 10) * 5 + 30;
			}
			else if (level > 40)
			{
				if (skill.getLevel() < 6)
				{
					return false;
				}

				chance = (skill.getLevel() - 6) * 5 + 10;
			}
			else if (level > 30)
			{
				if (skill.getLevel() < 3)
				{
					return false;
				}

				if (skill.getLevel() > 12)
				{
					return true;
				}

				chance = (skill.getLevel() - 3) * 5 + 30;
			}
			else
			{
				if (skill.getLevel() > 10)
				{
					return true;
				}

				chance = skill.getLevel() * 5 + 35;
			}

			chance = Math.min(chance, 50);
		}

		return Rnd.get(100) < chance;
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}