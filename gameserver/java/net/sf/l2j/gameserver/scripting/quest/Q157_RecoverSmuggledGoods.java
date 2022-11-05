package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q157_RecoverSmuggledGoods extends Quest
{
	private static final String QUEST_NAME = "Q157_RecoverSmuggledGoods";
	
	// Item
	private static final int ADAMANTITE_ORE = 1024;
	
	// Reward
	private static final int BUCKLER = 20;
	
	public Q157_RecoverSmuggledGoods()
	{
		super(157, "Recover Smuggled Goods");
		
		setItemsIds(ADAMANTITE_ORE);
		
		addStartNpc(30005); // Wilford
		addTalkId(30005);
		
		addKillId(20121); // Toad
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30005-05.htm"))
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
				htmltext = (player.getStatus().getLevel() < 5) ? "30005-02.htm" : "30005-03.htm";
				break;
			
			case STARTED:
				int cond = st.getCond();
				if (cond == 1)
					htmltext = "30005-06.htm";
				else if (cond == 2)
				{
					htmltext = "30005-07.htm";
					takeItems(player, ADAMANTITE_ORE, -1);
					giveItems(player, BUCKLER, 1);
					playSound(player, SOUND_FINISH);
					st.exitQuest(false);
				}
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
		
		if (dropItems(player, ADAMANTITE_ORE, 1, 20, 400000))
			st.setCond(2);
		
		return null;
	}
}