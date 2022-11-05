package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q651_RunawayYouth extends Quest
{
	private static final String QUEST_NAME = "Q651_RunawayYouth";
	
	// NPCs
	private static final int IVAN = 32014;
	private static final int BATIDAE = 31989;
	
	// Item
	private static final int SCROLL_OF_ESCAPE = 736;
	
	// Table of possible spawns
	private static final SpawnLocation[] SPAWNS =
	{
		new SpawnLocation(118600, -161235, -1119, 0),
		new SpawnLocation(108380, -150268, -2376, 0),
		new SpawnLocation(123254, -148126, -3425, 0)
	};
	
	// Current position
	private int _currentPosition = 0;
	
	public Q651_RunawayYouth()
	{
		super(651, "Runaway Youth");
		
		addStartNpc(IVAN);
		addTalkId(IVAN, BATIDAE);
		
		addSpawn(IVAN, 118600, -161235, -1119, 0, false, 0, false);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("32014-04.htm"))
		{
			if (player.getInventory().hasItems(SCROLL_OF_ESCAPE))
			{
				htmltext = "32014-03.htm";
				st.setState(QuestStatus.STARTED);
				st.setCond(1);
				playSound(player, SOUND_ACCEPT);
				takeItems(player, SCROLL_OF_ESCAPE, 1);
				
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 2013, 1, 3500, 0));
				startQuestTimer("apparition_npc", npc, null, 4000);
			}
			else
				st.exitQuest(true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("apparition_npc"))
		{
			int chance = Rnd.get(3);
			
			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
				chance = Rnd.get(3);
			
			// Register new position.
			_currentPosition = chance;
			
			npc.deleteMe();
			addSpawn(IVAN, SPAWNS[chance], false, 0, false);
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
				htmltext = (player.getStatus().getLevel() < 26) ? "32014-01.htm" : "32014-02.htm";
				break;
			
			case STARTED:
				switch (npc.getNpcId())
				{
					case BATIDAE:
						htmltext = "31989-01.htm";
						rewardItems(player, 57, 2883);
						playSound(player, SOUND_FINISH);
						st.exitQuest(true);
						break;
					
					case IVAN:
						htmltext = "32014-04a.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
}