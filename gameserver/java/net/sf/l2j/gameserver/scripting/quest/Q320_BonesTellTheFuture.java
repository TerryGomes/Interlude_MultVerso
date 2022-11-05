package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q320_BonesTellTheFuture extends Quest
{
	private static final String QUEST_NAME = "Q320_BonesTellTheFuture";
	
	// Quest item
	private static final int BONE_FRAGMENT = 809;
	
	public Q320_BonesTellTheFuture()
	{
		super(320, "Bones Tell the Future");
		
		setItemsIds(BONE_FRAGMENT);
		
		addStartNpc(30359); // Kaitar
		addTalkId(30359);
		
		addKillId(20517, 20518);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30359-04.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
		}
		
		return event;
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
				if (player.getRace() != ClassRace.DARK_ELF)
					htmltext = "30359-00.htm";
				else if (player.getStatus().getLevel() < 10)
					htmltext = "30359-02.htm";
				else
					htmltext = "30359-03.htm";
				break;
			
			case STARTED:
				if (st.getCond() == 1)
					htmltext = "30359-05.htm";
				else
				{
					htmltext = "30359-06.htm";
					takeItems(player, BONE_FRAGMENT, -1);
					rewardItems(player, 57, 8470);
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
		
		if (dropItems(player, BONE_FRAGMENT, 1, 10, (npc.getNpcId() == 20517) ? 180000 : 200000))
			st.setCond(2);
		
		return null;
	}
}