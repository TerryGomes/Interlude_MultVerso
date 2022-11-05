package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * This class extends {@link Attackable} and manages all {@link Guard}s.<br>
 * <br>
 * A {@link Guard} is used to protect Players from Player Killers (PKs).
 */
public final class Guard extends Attackable
{
	public Guard(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onSpawn()
	{
		setNoRndWalk(true);
		super.onSpawn();
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/guard/" + filename + ".htm";
	}
	
	@Override
	public void onInteract(Player player)
	{
		switch (getNpcId())
		{
			case 30733: // Guards in start villages
			case 31032:
			case 31033:
			case 31034:
			case 31035:
			case 31036:
			case 31671: // Patrols
			case 31672:
			case 31673:
			case 31674:
				return;
		}
		
		if (hasRandomAnimation())
			onRandomAnimation(Rnd.get(8));
		
		player.getQuestList().setLastQuestNpcObjectId(getObjectId());
		
		List<Quest> scripts = getTemplate().getEventQuests(ScriptEventType.ON_FIRST_TALK);
		if (scripts.size() == 1)
			scripts.get(0).notifyFirstTalk(this, player);
		else
			showChatWindow(player);
	}
	
	@Override
	public boolean isGuard()
	{
		return true;
	}
	
	@Override
	public int getDriftRange()
	{
		return 20;
	}
}