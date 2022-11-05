package net.sf.l2j.gameserver.scripting.quest;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.clanhall.SiegableHall;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q504_CompetitionForTheBanditStronghold extends Quest
{
	private static final String QUEST_NAME = "Q504_CompetitionForTheBanditStronghold";
	
	// Misc
	private static final SiegableHall BANDIT_STRONGHOLD = ClanHallManager.getInstance().getSiegableHall(35);
	
	// NPC
	private static final int MESSENGER = 35437;
	
	// Monsters
	private static final Map<Integer, Integer> MONSTERS = new HashMap<>();
	{
		MONSTERS.put(20570, 600000); // Tarlk Bugbear
		MONSTERS.put(20571, 700000); // Tarlk Bugbear Warrior
		MONSTERS.put(20572, 800000); // Tarlk Bugbear High Warrior
		MONSTERS.put(20573, 900000); // Tarlk Basilisk
		MONSTERS.put(20574, 700000); // Elder Tarlk Basilisk
	}
	
	// Items
	private static final int TARLK_AMULET = 4332;
	private static final int CONTEST_CERTIFICATE = 4333;
	private static final int TROPHY_OF_ALLIANCE = 5009;
	
	public Q504_CompetitionForTheBanditStronghold()
	{
		super(504, "Competition for the Bandit Stronghold");
		
		addStartNpc(MESSENGER);
		addTalkId(MESSENGER);
		
		for (int mob : MONSTERS.keySet())
			addKillId(mob);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("35437-02.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
			giveItems(player, CONTEST_CERTIFICATE, 1);
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
		
		final Clan clan = player.getClan();
		
		if (!BANDIT_STRONGHOLD.isWaitingBattle())
			htmltext = getHtmlText("35437-09.htm").replaceAll("%nextSiege%", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(BANDIT_STRONGHOLD.getSiegeDate().getTime()));
		else if (clan == null || clan.getLevel() < 4)
			htmltext = "35437-04.htm";
		else if (!player.isClanLeader())
			htmltext = "35437-05.htm";
		else if (clan.getClanHallId() > 0 || clan.getCastleId() > 0)
			htmltext = "35437-10.htm";
		else
		{
			switch (st.getState())
			{
				case CREATED:
					if (!BANDIT_STRONGHOLD.isWaitingBattle())
						htmltext = getHtmlText("35437-03.htm").replaceAll("%nextSiege%", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(BANDIT_STRONGHOLD.getSiegeDate().getTime()));
					else
						htmltext = "35437-01.htm";
					break;
				
				case STARTED:
					if (player.getInventory().getItemCount(TARLK_AMULET) < 30)
						htmltext = "35437-07.htm";
					else
					{
						htmltext = "35437-08.htm";
						takeItems(player, TARLK_AMULET, 30);
						rewardItems(player, TROPHY_OF_ALLIANCE, 1);
						st.exitQuest(true);
					}
					break;
				
				case COMPLETED:
					htmltext = "35437-07a.htm";
					break;
			}
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
		
		dropItems(player, TARLK_AMULET, 1, 30, MONSTERS.get(npc.getNpcId()));
		return null;
	}
}