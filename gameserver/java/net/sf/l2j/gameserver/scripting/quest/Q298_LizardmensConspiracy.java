package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q298_LizardmensConspiracy extends Quest
{
	private static final String QUEST_NAME = "Q298_LizardmensConspiracy";
	
	// NPCs
	private static final int PRAGA = 30333;
	private static final int ROHMER = 30344;
	
	// Items
	private static final int PATROL_REPORT = 7182;
	private static final int WHITE_GEM = 7183;
	private static final int RED_GEM = 7184;
	
	public Q298_LizardmensConspiracy()
	{
		super(298, "Lizardmen's Conspiracy");
		
		setItemsIds(PATROL_REPORT, WHITE_GEM, RED_GEM);
		
		addStartNpc(PRAGA);
		addTalkId(PRAGA, ROHMER);
		
		addKillId(20926, 20927, 20922, 20923, 20924);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30333-1.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
			giveItems(player, PATROL_REPORT, 1);
		}
		else if (event.equalsIgnoreCase("30344-1.htm"))
		{
			st.setCond(2);
			playSound(player, SOUND_MIDDLE);
			takeItems(player, PATROL_REPORT, 1);
		}
		else if (event.equalsIgnoreCase("30344-4.htm"))
		{
			if (st.getCond() == 3)
			{
				htmltext = "30344-3.htm";
				takeItems(player, WHITE_GEM, -1);
				takeItems(player, RED_GEM, -1);
				rewardExpAndSp(player, 0, 42000);
				playSound(player, SOUND_FINISH);
				st.exitQuest(true);
			}
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
				htmltext = (player.getStatus().getLevel() < 25) ? "30333-0b.htm" : "30333-0a.htm";
				break;
			
			case STARTED:
				switch (npc.getNpcId())
				{
					case PRAGA:
						htmltext = "30333-2.htm";
						break;
					
					case ROHMER:
						if (st.getCond() == 1)
							htmltext = (player.getInventory().hasItems(PATROL_REPORT)) ? "30344-0.htm" : "30344-0a.htm";
						else
							htmltext = "30344-2.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		Player player = killer.getActingPlayer();
		
		final QuestState st = getRandomPartyMember(player, npc, 2);
		if (st == null)
			return null;
		
		player = st.getPlayer();
		switch (npc.getNpcId())
		{
			case 20922:
				if (dropItems(player, WHITE_GEM, 1, 50, 400000) && player.getInventory().getItemCount(RED_GEM) >= 50)
					st.setCond(3);
				break;
			
			case 20923:
				if (dropItems(player, WHITE_GEM, 1, 50, 450000) && player.getInventory().getItemCount(RED_GEM) >= 50)
					st.setCond(3);
				break;
			
			case 20924:
				if (dropItems(player, WHITE_GEM, 1, 50, 350000) && player.getInventory().getItemCount(RED_GEM) >= 50)
					st.setCond(3);
				break;
			
			case 20926:
			case 20927:
				if (dropItems(player, RED_GEM, 1, 50, 400000) && player.getInventory().getItemCount(WHITE_GEM) >= 50)
					st.setCond(3);
				break;
		}
		
		return null;
	}
}