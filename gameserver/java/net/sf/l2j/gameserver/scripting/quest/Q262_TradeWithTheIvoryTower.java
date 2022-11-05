package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q262_TradeWithTheIvoryTower extends Quest
{
	private static final String QUEST_NAME = "Q262_TradeWithTheIvoryTower";
	
	// Item
	private static final int FUNGUS_SAC = 707;
	
	public Q262_TradeWithTheIvoryTower()
	{
		super(262, "Trade with the Ivory Tower");
		
		setItemsIds(FUNGUS_SAC);
		
		addStartNpc(30137); // Vollodos
		addTalkId(30137);
		
		addKillId(20400, 20007);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30137-03.htm"))
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
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case CREATED:
				htmltext = (player.getStatus().getLevel() < 8) ? "30137-01.htm" : "30137-02.htm";
				break;
			
			case STARTED:
				if (player.getInventory().getItemCount(FUNGUS_SAC) < 10)
					htmltext = "30137-04.htm";
				else
				{
					htmltext = "30137-05.htm";
					takeItems(player, FUNGUS_SAC, -1);
					rewardItems(player, 57, 3000);
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
		
		if (dropItems(player, FUNGUS_SAC, 1, 10, (npc.getNpcId() == 20400) ? 400000 : 300000))
			st.setCond(2);
		
		return null;
	}
}