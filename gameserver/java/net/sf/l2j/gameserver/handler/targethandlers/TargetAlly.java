package net.sf.l2j.gameserver.handler.targethandlers;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.handler.ITargetHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class TargetAlly implements ITargetHandler
{
	@Override
	public SkillTargetType getTargetType()
	{
		return SkillTargetType.ALLY;
	}
	
	@Override
	public Creature[] getTargetList(Creature caster, Creature target, L2Skill skill)
	{
		final Player player = caster.getActingPlayer();
		
		if (player.isInOlympiadMode())
			return new Creature[]
			{
				caster
			};
		
		final List<Creature> list = new ArrayList<>();
		list.add(player);
		
		if (skill.addSummon(caster, player, false))
			list.add(player.getSummon());
		
		if (player.getClan() != null)
		{
			for (Playable playable : caster.getKnownTypeInRadius(Playable.class, skill.getSkillRadius()))
			{
				if (playable.isDead())
					continue;
				
				final Player targetPlayer = playable.getActingPlayer();
				if (targetPlayer == null || targetPlayer.getClan() == null)
					continue;
				
				// Only buff allies
				if (player.getClanId() != targetPlayer.getClanId() || (player.getAllyId() > 0 && player.getAllyId() != targetPlayer.getAllyId()))
					continue;
				
				// Do not buff opposing duel side
				if (player.isInDuel() && (player.getDuelId() != targetPlayer.getDuelId() || player.getTeam() != targetPlayer.getTeam()))
					continue;
				
				list.add(playable);
			}
		}
		
		return list.toArray(new Creature[list.size()]);
	}
	
	@Override
	public Creature getFinalTarget(Creature caster, Creature target, L2Skill skill)
	{
		return caster;
	}
	
	@Override
	public boolean meetCastConditions(Playable caster, Creature target, L2Skill skill, boolean isCtrlPressed)
	{
		return true;
	}
}