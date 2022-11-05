package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q271_ProofOfValor extends Quest
{
	private static final String QUEST_NAME = "Q271_ProofOfValor";
	
	// Item
	private static final int KASHA_WOLF_FANG = 1473;
	
	// Rewards
	private static final int NECKLACE_OF_VALOR = 1507;
	private static final int NECKLACE_OF_COURAGE = 1506;
	
	public Q271_ProofOfValor()
	{
		super(271, "Proof of Valor");
		
		setItemsIds(KASHA_WOLF_FANG);
		
		addStartNpc(30577); // Rukain
		addTalkId(30577);
		
		addKillId(20475); // Kasha Wolf
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30577-03.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
			
			if (player.getInventory().hasAtLeastOneItem(NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR))
				htmltext = "30577-07.htm";
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
				if (player.getRace() != ClassRace.ORC)
					htmltext = "30577-00.htm";
				else if (player.getStatus().getLevel() < 4)
					htmltext = "30577-01.htm";
				else
					htmltext = (player.getInventory().hasAtLeastOneItem(NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR)) ? "30577-06.htm" : "30577-02.htm";
				break;
			
			case STARTED:
				if (st.getCond() == 1)
					htmltext = (player.getInventory().hasAtLeastOneItem(NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR)) ? "30577-07.htm" : "30577-04.htm";
				else
				{
					htmltext = "30577-05.htm";
					takeItems(player, KASHA_WOLF_FANG, -1);
					giveItems(player, (Rnd.get(100) < 10) ? NECKLACE_OF_VALOR : NECKLACE_OF_COURAGE, 1);
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
		
		if (dropItemsAlways(player, KASHA_WOLF_FANG, (Rnd.get(4) == 0) ? 2 : 1, 50))
			st.setCond(2);
		
		return null;
	}
}