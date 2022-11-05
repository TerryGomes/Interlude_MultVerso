package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Q170_DangerousSeduction extends Quest
{
	private static final String QUEST_NAME = "Q170_DangerousSeduction";
	
	// Item
	private static final int NIGHTMARE_CRYSTAL = 1046;
	
	public Q170_DangerousSeduction()
	{
		super(170, "Dangerous Seduction");
		
		setItemsIds(NIGHTMARE_CRYSTAL);
		
		addStartNpc(30305); // Vellior
		addTalkId(30305);
		
		addEventIds(27022, ScriptEventType.ON_ATTACK, ScriptEventType.ON_KILL); // Merkenis
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30305-04.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
		}
		
		return htmltext;
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.isScriptValue(1))
			return null;
		
		npc.broadcastNpcSay(NpcStringId.ID_17004);
		npc.setScriptValue(1);
		return null;
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
				if (player.getRace() != ClassRace.DARK_ELF)
					htmltext = "30305-00.htm";
				else if (player.getStatus().getLevel() < 21)
					htmltext = "30305-02.htm";
				else
					htmltext = "30305-03.htm";
				break;
			
			case STARTED:
				if (player.getInventory().hasItems(NIGHTMARE_CRYSTAL))
				{
					htmltext = "30305-06.htm";
					takeItems(player, NIGHTMARE_CRYSTAL, -1);
					rewardItems(player, 57, 102680);
					playSound(player, SOUND_FINISH);
					st.exitQuest(false);
				}
				else
					htmltext = "30305-05.htm";
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
		
		st.setCond(2);
		playSound(player, SOUND_MIDDLE);
		giveItems(player, NIGHTMARE_CRYSTAL, 1);
		npc.broadcastNpcSay(NpcStringId.ID_17005);
		
		return null;
	}
}