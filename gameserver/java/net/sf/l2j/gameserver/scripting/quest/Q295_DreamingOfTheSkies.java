package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q295_DreamingOfTheSkies extends Quest
{
	private static final String QUEST_NAME = "Q295_DreamingOfTheSkies";
	
	// Item
	private static final int FLOATING_STONE = 1492;
	
	// Reward
	private static final int RING_OF_FIREFLY = 1509;
	
	public Q295_DreamingOfTheSkies()
	{
		super(295, "Dreaming of the Skies");
		
		setItemsIds(FLOATING_STONE);
		
		addStartNpc(30536); // Arin
		addTalkId(30536);
		
		addKillId(20153); // Magical Weaver
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30536-03.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
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
				htmltext = (player.getStatus().getLevel() < 11) ? "30536-01.htm" : "30536-02.htm";
				break;
			
			case STARTED:
				if (st.getCond() == 1)
					htmltext = "30536-04.htm";
				else
				{
					takeItems(player, FLOATING_STONE, -1);
					
					if (!player.getInventory().hasItems(RING_OF_FIREFLY))
					{
						htmltext = "30536-05.htm";
						giveItems(player, RING_OF_FIREFLY, 1);
					}
					else
					{
						htmltext = "30536-06.htm";
						rewardItems(player, 57, 2400);
					}
					
					rewardExpAndSp(player, 0, 500);
					playSound(player, SOUND_FINISH);
					st.exitQuest(true);
				}
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
		
		if (dropItemsAlways(player, FLOATING_STONE, (Rnd.get(100) > 25) ? 1 : 2, 50))
			st.setCond(2);
		
		return null;
	}
}