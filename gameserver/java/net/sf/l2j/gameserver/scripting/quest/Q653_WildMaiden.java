package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q653_WildMaiden extends Quest
{
	private static final String QUEST_NAME = "Q653_WildMaiden";
	
	// NPCs
	private static final int SUKI = 32013;
	private static final int GALIBREDO = 30181;
	
	// Item
	private static final int SCROLL_OF_ESCAPE = 736;
	
	// Table of possible spawns
	private static final SpawnLocation[] SPAWNS =
	{
		new SpawnLocation(66578, 72351, -3731, 0),
		new SpawnLocation(77189, 73610, -3708, 2555),
		new SpawnLocation(71809, 67377, -3675, 29130),
		new SpawnLocation(69166, 88825, -3447, 43886)
	};
	
	// Current position
	private int _currentPosition = 0;
	
	public Q653_WildMaiden()
	{
		super(653, "Wild Maiden");
		
		addStartNpc(SUKI);
		addTalkId(SUKI, GALIBREDO);
		
		addSpawn(SUKI, 66578, 72351, -3731, 0, false, 0, false);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("32013-03.htm"))
		{
			if (player.getInventory().hasItems(SCROLL_OF_ESCAPE))
			{
				st.setState(QuestStatus.STARTED);
				st.setCond(1);
				playSound(player, SOUND_ACCEPT);
				takeItems(player, SCROLL_OF_ESCAPE, 1);
				
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 2013, 1, 3500, 0));
				startQuestTimer("apparition_npc", npc, null, 4000);
			}
			else
			{
				htmltext = "32013-03a.htm";
				st.exitQuest(true);
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("apparition_npc"))
		{
			int chance = Rnd.get(4);
			
			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
				chance = Rnd.get(4);
			
			// Register new position.
			_currentPosition = chance;
			
			npc.deleteMe();
			addSpawn(SUKI, SPAWNS[chance], false, 0, false);
		}
		
		return null;
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
				htmltext = (player.getStatus().getLevel() < 36) ? "32013-01.htm" : "32013-02.htm";
				break;
			
			case STARTED:
				switch (npc.getNpcId())
				{
					case GALIBREDO:
						htmltext = "30181-01.htm";
						rewardItems(player, 57, 2883);
						playSound(player, SOUND_FINISH);
						st.exitQuest(true);
						break;
					
					case SUKI:
						htmltext = "32013-04a.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
}