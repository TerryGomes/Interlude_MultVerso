package net.sf.l2j.gameserver.skills.effects;

import java.util.ArrayList;

import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.EffectPoint;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignetCasttime;

public class EffectSignetMDam extends AbstractEffect
{
	private boolean _srcInArena;
	private int _state = 0;
	private EffectPoint _actor;

	public EffectSignetMDam(EffectTemplate template, L2Skill skill, Creature effected, Creature effector)
	{
		super(template, skill, effected, effector);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.SIGNET_GROUND;
	}

	@Override
	public boolean onStart()
	{
		if (!(_skill instanceof L2SkillSignetCasttime))
		{
			return false;
		}

		final NpcTemplate template = NpcData.getInstance().getTemplate(((L2SkillSignetCasttime) getSkill()).effectNpcId);
		if (template == null)
		{
			return false;
		}

		final EffectPoint effectPoint = new EffectPoint(IdFactory.getInstance().getNextId(), template, getEffector());
		effectPoint.getStatus().setMaxHpMp();

		Location worldPosition = null;
		if (getEffector() instanceof Player && getSkill().getTargetType() == SkillTargetType.GROUND)
		{
			worldPosition = ((Player) getEffector()).getCast().getSignetLocation();
		}

		effectPoint.setInvul(true);
		effectPoint.spawnMe((worldPosition != null) ? worldPosition : getEffector().getPosition());

		_actor = effectPoint;
		return true;

	}

	@Override
	public boolean onActionTime()
	{
		// on offi the zone get created and the first wave starts later
		// there is also an first hit animation to the caster
		switch (_state)
		{
			case 0:
			case 2:
				_state++;
				return true;
			case 1:
				getEffected().broadcastPacket(new MagicSkillLaunched(_actor, getSkill(), new Creature[]
				{
					getEffected()
				}));
				_state++;
				return true;
		}

		int mpConsume = getSkill().getMpConsume();

		Player caster = (Player) getEffected();

		boolean ss = false;
		boolean bss = false;

		if (!bss && !ss)
		{
			caster.rechargeShots(false, true);
		}

		ArrayList<Creature> targets = new ArrayList<>();

		for (Creature creature : _actor.getKnownTypeInRadius(Creature.class, _skill.getSkillRadius(), creature -> !creature.isDead() && !(creature instanceof Door) && !creature.isInsideZone(ZoneId.PEACE)))
		{
			if ((creature == null) || creature == getEffected())
			{
				continue;
			}

			if (creature instanceof Attackable || creature instanceof Playable)
			{
				// isSignetOffensiveSkill only really checks for Day of Doom, the other signets ahve different Effects
				if (creature.isAlikeDead() || (_skill.isOffensive() && !_skill.checkForAreaOffensiveSkill(_actor, creature, true, _srcInArena)))
				{
					continue;
				}

				if (mpConsume > caster.getStatus().getMp())
				{
					caster.sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
					return false;
				}

				caster.getStatus().reduceMp(mpConsume);

				targets.add(creature);
			}
		}

		if (targets.size() > 0)
		{
			caster.broadcastPacket(new MagicSkillLaunched(caster, getSkill(), targets.toArray(new Creature[targets.size()])));
			for (Creature target : targets)
			{
				final boolean isCrit = Formulas.calcMCrit(caster, target, getSkill());
				final ShieldDefense sDef = Formulas.calcShldUse(caster, target, getSkill(), false);
				final boolean sps = caster.isChargedShot(ShotType.SPIRITSHOT);
				final boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
				final int damage = (int) Formulas.calcMagicDam(caster, target, getSkill(), sDef, sps, bsps, isCrit);

				if (target instanceof Summon)
				{
					target.getStatus().broadcastStatusUpdate();
				}

				if (damage > 0)
				{
					// Manage cast break of the target (calculating rate, sending message...)
					Formulas.calcCastBreak(target, damage);

					caster.sendDamageMessage(target, damage, isCrit, false, false);
					target.reduceCurrentHp(damage, caster, getSkill());
				}
				target.getAI().notifyEvent(AiEventType.ATTACKED, caster, target);
			}
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (_actor != null)
		{
			_actor.deleteMe();
		}
	}
}