package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q159_ProtectTheWaterSource extends Quest
{
	private static final String QUEST_NAME = "Q159_ProtectTheWaterSource";
	
	// Items
	private static final int PLAGUE_DUST = 1035;
	private static final int HYACINTH_CHARM_1 = 1071;
	private static final int HYACINTH_CHARM_2 = 1072;
	
	public Q159_ProtectTheWaterSource()
	{
		super(159, "Protect the Water Source");
		
		setItemsIds(PLAGUE_DUST, HYACINTH_CHARM_1, HYACINTH_CHARM_2);
		
		addStartNpc(30154); // Asterios
		addTalkId(30154);
		
		addKillId(27017); // Plague Zombie
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30154-04.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
			giveItems(player, HYACINTH_CHARM_1, 1);
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
				if (player.getRace() != ClassRace.ELF)
					htmltext = "30154-00.htm";
				else if (player.getStatus().getLevel() < 12)
					htmltext = "30154-02.htm";
				else
					htmltext = "30154-03.htm";
				break;
			
			case STARTED:
				int cond = st.getCond();
				if (cond == 1)
					htmltext = "30154-05.htm";
				else if (cond == 2)
				{
					htmltext = "30154-06.htm";
					st.setCond(3);
					playSound(player, SOUND_MIDDLE);
					takeItems(player, PLAGUE_DUST, -1);
					takeItems(player, HYACINTH_CHARM_1, 1);
					giveItems(player, HYACINTH_CHARM_2, 1);
				}
				else if (cond == 3)
					htmltext = "30154-07.htm";
				else if (cond == 4)
				{
					htmltext = "30154-08.htm";
					takeItems(player, HYACINTH_CHARM_2, 1);
					takeItems(player, PLAGUE_DUST, -1);
					rewardItems(player, 57, 18250);
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
		
		final QuestState st = checkPlayerState(player, npc, QuestStatus.STARTED);
		if (st == null)
			return null;
		
		if (st.getCond() == 1 && dropItems(player, PLAGUE_DUST, 1, 1, 400000))
			st.setCond(2);
		else if (st.getCond() == 3 && dropItems(player, PLAGUE_DUST, 1, 5, 400000))
			st.setCond(4);
		
		return null;
	}
}