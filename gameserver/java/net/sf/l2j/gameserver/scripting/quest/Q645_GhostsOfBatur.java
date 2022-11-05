package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q645_GhostsOfBatur extends Quest
{
	private static final String QUEST_NAME = "Q645_GhostsOfBatur";
	
	// NPC
	private static final int KARUDA = 32017;
	
	// Item
	private static final int CURSED_GRAVE_GOODS = 8089;
	
	// Rewards
	private static final int[][] REWARDS =
	{
		{
			1878,
			18
		},
		{
			1879,
			7
		},
		{
			1880,
			4
		},
		{
			1881,
			6
		},
		{
			1882,
			10
		},
		{
			1883,
			2
		}
	};
	
	public Q645_GhostsOfBatur()
	{
		super(645, "Ghosts Of Batur");
		
		addStartNpc(KARUDA);
		addTalkId(KARUDA);
		
		addKillId(22007, 22009, 22010, 22011, 22012, 22013, 22014, 22015, 22016);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("32017-03.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
		}
		else if (StringUtil.isDigit(event))
		{
			htmltext = "32017-07.htm";
			takeItems(player, CURSED_GRAVE_GOODS, -1);
			
			final int[] reward = REWARDS[Integer.parseInt(event)];
			giveItems(player, reward[0], reward[1]);
			
			playSound(player, SOUND_FINISH);
			st.exitQuest(true);
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
				htmltext = (player.getStatus().getLevel() < 23) ? "32017-02.htm" : "32017-01.htm";
				break;
			
			case STARTED:
				final int cond = st.getCond();
				if (cond == 1)
					htmltext = "32017-04.htm";
				else if (cond == 2)
					htmltext = "32017-05.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();
		
		final QuestState st = getRandomPartyMember(player, npc, 1);
		if (st == null)
			return null;
		
		if (dropItems(st.getPlayer(), CURSED_GRAVE_GOODS, 1, 180, 750000))
			st.setCond(2);
		
		return null;
	}
}