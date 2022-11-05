package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q246_PossessorOfAPreciousSoul extends Quest
{
	private static final String QUEST_NAME = "Q246_PossessorOfAPreciousSoul";
	
	// NPCs
	private static final int CARADINE = 31740;
	private static final int OSSIAN = 31741;
	private static final int LADD = 30721;
	
	// Items
	private static final int WATERBINDER = 7591;
	private static final int EVERGREEN = 7592;
	private static final int RAIN_SONG = 7593;
	private static final int RELIC_BOX = 7594;
	private static final int CARADINE_LETTER_1 = 7678;
	private static final int CARADINE_LETTER_2 = 7679;
	
	// Mobs
	private static final int PILGRIM_OF_SPLENDOR = 21541;
	private static final int JUDGE_OF_SPLENDOR = 21544;
	private static final int BARAKIEL = 25325;
	
	public Q246_PossessorOfAPreciousSoul()
	{
		super(246, "Possessor of a Precious Soul - 3");
		
		setItemsIds(WATERBINDER, EVERGREEN, RAIN_SONG, RELIC_BOX);
		
		addStartNpc(CARADINE);
		addTalkId(CARADINE, OSSIAN, LADD);
		
		addKillId(PILGRIM_OF_SPLENDOR, JUDGE_OF_SPLENDOR, BARAKIEL);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		// Caradine
		if (event.equalsIgnoreCase("31740-04.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
			takeItems(player, CARADINE_LETTER_1, 1);
		}
		// Ossian
		else if (event.equalsIgnoreCase("31741-02.htm"))
		{
			st.setCond(2);
			playSound(player, SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31741-05.htm"))
		{
			if (player.getInventory().hasItems(WATERBINDER, EVERGREEN))
			{
				st.setCond(4);
				playSound(player, SOUND_MIDDLE);
				takeItems(player, WATERBINDER, 1);
				takeItems(player, EVERGREEN, 1);
			}
			else
				htmltext = null;
		}
		else if (event.equalsIgnoreCase("31741-08.htm"))
		{
			if (player.getInventory().hasItems(RAIN_SONG))
			{
				st.setCond(6);
				playSound(player, SOUND_MIDDLE);
				takeItems(player, RAIN_SONG, 1);
				giveItems(player, RELIC_BOX, 1);
			}
			else
				htmltext = null;
		}
		// Ladd
		else if (event.equalsIgnoreCase("30721-02.htm"))
		{
			if (player.getInventory().hasItems(RELIC_BOX))
			{
				takeItems(player, RELIC_BOX, 1);
				giveItems(player, CARADINE_LETTER_2, 1);
				rewardExpAndSp(player, 719843, 0);
				player.broadcastPacket(new SocialAction(player, 3));
				playSound(player, SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = null;
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
				if (player.getInventory().hasItems(CARADINE_LETTER_1))
					htmltext = (!player.isSubClassActive() || player.getStatus().getLevel() < 65) ? "31740-02.htm" : "31740-01.htm";
				break;
			
			case STARTED:
				if (!player.isSubClassActive())
					break;
				
				int cond = st.getCond();
				switch (npc.getNpcId())
				{
					case CARADINE:
						if (cond == 1)
							htmltext = "31740-05.htm";
						break;
					
					case OSSIAN:
						if (cond == 1)
							htmltext = "31741-01.htm";
						else if (cond == 2)
							htmltext = "31741-03.htm";
						else if (cond == 3)
						{
							if (player.getInventory().hasItems(WATERBINDER, EVERGREEN))
								htmltext = "31741-04.htm";
						}
						else if (cond == 4)
							htmltext = "31741-06.htm";
						else if (cond == 5)
						{
							if (player.getInventory().hasItems(RAIN_SONG))
								htmltext = "31741-07.htm";
						}
						else if (cond == 6)
							htmltext = "31741-09.htm";
						break;
					
					case LADD:
						if (cond == 6 && player.getInventory().hasItems(RELIC_BOX))
							htmltext = "30721-01.htm";
						break;
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
		final int npcId = npc.getNpcId();
		
		if (npcId == BARAKIEL)
		{
			for (QuestState st : getPartyMembers(player, npc, 4))
			{
				Player pm = st.getPlayer();
				if (!pm.isSubClassActive())
					continue;
				
				if (!pm.getInventory().hasItems(RAIN_SONG))
				{
					st.setCond(5);
					playSound(pm, SOUND_MIDDLE);
					giveItems(pm, RAIN_SONG, 1);
				}
			}
		}
		else
		{
			if (!player.isSubClassActive())
				return null;
			
			QuestState st = checkPlayerCondition(player, npc, 2);
			if (st == null)
				return null;
			
			if (Rnd.get(10) < 2)
			{
				final int neklaceOrRing = (npcId == PILGRIM_OF_SPLENDOR) ? WATERBINDER : EVERGREEN;
				
				if (!player.getInventory().hasItems(neklaceOrRing))
				{
					giveItems(player, neklaceOrRing, 1);
					
					if (!player.getInventory().hasItems((npcId == PILGRIM_OF_SPLENDOR) ? EVERGREEN : WATERBINDER))
						playSound(player, SOUND_ITEMGET);
					else
					{
						st.setCond(3);
						playSound(player, SOUND_MIDDLE);
					}
				}
			}
		}
		return null;
	}
}