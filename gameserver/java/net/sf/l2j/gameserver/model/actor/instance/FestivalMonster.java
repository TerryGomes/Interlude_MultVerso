package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * This class manages all attackable festival NPCs, spawned during the Festival of Darkness.
 */
public class FestivalMonster extends Monster
{
	protected int _bonusMultiplier = 1;
	
	public FestivalMonster(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean isAggressive()
	{
		return true;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	@Override
	public void doItemDrop(NpcTemplate template, Creature attacker)
	{
		final Player player = attacker.getActingPlayer();
		if (player == null || !player.isInParty())
			return;
		
		player.getParty().getLeader().addItem("Sign", FestivalOfDarknessManager.FESTIVAL_OFFERING_ID, _bonusMultiplier, attacker, true);
		
		super.doItemDrop(template, attacker);
	}
	
	public void setOfferingBonus(int bonusMultiplier)
	{
		_bonusMultiplier = bonusMultiplier;
	}
}