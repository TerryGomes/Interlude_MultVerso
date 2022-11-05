package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q050_LanoscosSpecialBait extends Quest
{
	private static final String QUEST_NAME = "Q050_LanoscosSpecialBait";
	
	// Item
	private static final int ESSENCE_OF_WIND = 7621;
	
	// Reward
	private static final int WIND_FISHING_LURE = 7610;
	
	public Q050_LanoscosSpecialBait()
	{
		super(50, "Lanosco's Special Bait");
		
		setItemsIds(ESSENCE_OF_WIND);
		
		addStartNpc(31570); // Lanosco
		addTalkId(31570);
		
		addKillId(21026); // Singing wind
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31570-03.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31570-07.htm"))
		{
			htmltext = "31570-06.htm";
			takeItems(player, ESSENCE_OF_WIND, -1);
			rewardItems(player, WIND_FISHING_LURE, 4);
			playSound(player, SOUND_FINISH);
			st.exitQuest(false);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case CREATED:
				htmltext = (player.getStatus().getLevel() < 27) ? "31570-02.htm" : "31570-01.htm";
				break;
			
			case STARTED:
				htmltext = (player.getInventory().getItemCount(ESSENCE_OF_WIND) == 100) ? "31570-04.htm" : "31570-05.htm";
				break;
			
			case COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();
		
		final QuestState st = checkPlayerCondition(player, npc, 1);
		if (st == null)
			return null;
		
		if (dropItems(player, ESSENCE_OF_WIND, 1, 100, 500000))
			st.setCond(2);
		
		return null;
	}
}