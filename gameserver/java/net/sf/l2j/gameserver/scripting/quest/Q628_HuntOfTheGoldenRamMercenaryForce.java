package net.sf.l2j.gameserver.scripting.quest;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q628_HuntOfTheGoldenRamMercenaryForce extends Quest
{
	private static final String QUEST_NAME = "Q628_HuntOfTheGoldenRamMercenaryForce";
	
	private static final Map<Integer, IntIntHolder> BUFFS = new HashMap<>();
	{
		BUFFS.put(4404, new IntIntHolder(2, 2));
		BUFFS.put(4405, new IntIntHolder(2, 2));
		BUFFS.put(4393, new IntIntHolder(3, 3));
		BUFFS.put(4400, new IntIntHolder(2, 3));
		BUFFS.put(4397, new IntIntHolder(1, 3));
		BUFFS.put(4399, new IntIntHolder(2, 3));
		BUFFS.put(4401, new IntIntHolder(1, 6));
		BUFFS.put(4402, new IntIntHolder(2, 6));
	}
	
	// NPCs
	private static final int PIERCE = 31553;
	private static final int KAHMAN = 31554;
	private static final int ABERCROMBIE = 31555;
	private static final int SELINA = 31556;
	
	// Items
	private static final int GOLDEN_RAM_BADGE_RECRUIT = 7246;
	private static final int GOLDEN_RAM_BADGE_SOLDIER = 7247;
	private static final int SPLINTER_STAKATO_CHITIN = 7248;
	private static final int NEEDLE_STAKATO_CHITIN = 7249;
	private static final int GOLDEN_RAM_COIN = 7251;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	{
		CHANCES.put(21508, 500000);
		CHANCES.put(21509, 430000);
		CHANCES.put(21510, 521000);
		CHANCES.put(21511, 575000);
		CHANCES.put(21512, 746000);
		CHANCES.put(21513, 500000);
		CHANCES.put(21514, 430000);
		CHANCES.put(21515, 520000);
		CHANCES.put(21516, 531000);
		CHANCES.put(21517, 744000);
	}
	
	public Q628_HuntOfTheGoldenRamMercenaryForce()
	{
		super(628, "Hunt of the Golden Ram Mercenary Force");
		
		setItemsIds(SPLINTER_STAKATO_CHITIN, NEEDLE_STAKATO_CHITIN, GOLDEN_RAM_BADGE_RECRUIT, GOLDEN_RAM_BADGE_SOLDIER);
		
		addStartNpc(KAHMAN);
		addFirstTalkId(PIERCE, KAHMAN, ABERCROMBIE, SELINA);
		addTalkId(KAHMAN, ABERCROMBIE, SELINA);
		
		for (int npcId : CHANCES.keySet())
			addKillId(npcId);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return null;
		
		if (StringUtil.isDigit(event))
		{
			if (st.getCond() < 3)
				return null;
			
			final int buffId = Integer.parseInt(event);
			
			final IntIntHolder holder = BUFFS.get(buffId);
			if (holder != null)
			{
				if (player.getInventory().getItemCount(GOLDEN_RAM_COIN) < holder.getValue())
					htmltext = "31556-11.htm";
				else
				{
					htmltext = "31556-12.htm";
					
					takeItems(player, GOLDEN_RAM_COIN, holder.getValue());
					npc.getAI().tryToCast(player, buffId, holder.getId());
				}
			}
		}
		else if (event.startsWith("gmultisell"))
		{
			if (st.getCond() == 3)
				MultisellData.getInstance().separateAndSend(event.substring(10).trim(), player, npc, false);
			
			return null;
		}
		else if (event.equalsIgnoreCase("31554-02.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31554-03a.htm"))
		{
			if (player.getInventory().getItemCount(SPLINTER_STAKATO_CHITIN) >= 100 && st.getCond() == 1) // Giving GOLDEN_RAM_BADGE_RECRUIT Medals
			{
				htmltext = "31554-04.htm";
				st.setCond(2);
				playSound(player, SOUND_MIDDLE);
				takeItems(player, SPLINTER_STAKATO_CHITIN, -1);
				giveItems(player, GOLDEN_RAM_BADGE_RECRUIT, 1);
			}
		}
		else if (event.equalsIgnoreCase("31554-07.htm")) // Cancel Quest
		{
			playSound(player, SOUND_GIVEUP);
			st.exitQuest(true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st != null)
		{
			final int cond = st.getCond();
			
			switch (npc.getNpcId())
			{
				case PIERCE:
				case KAHMAN:
					if (cond > 1)
						return npc.getNpcId() + "-09.htm";
					break;
				
				case ABERCROMBIE:
				case SELINA:
					if (cond == 2)
						return npc.getNpcId() + "-09.htm";
					
					if (cond == 3)
						return npc.getNpcId() + "-10.htm";
					break;
			}
		}
		
		npc.showChatWindow(player);
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
				htmltext = (player.getStatus().getLevel() < 66) ? "31554-01a.htm" : "31554-01.htm";
				break;
			
			case STARTED:
				final int cond = st.getCond();
				if (cond == 1)
				{
					if (player.getInventory().getItemCount(SPLINTER_STAKATO_CHITIN) >= 100)
						htmltext = "31554-03.htm";
					else
						htmltext = "31554-03a.htm";
				}
				else if (cond == 2)
				{
					if (player.getInventory().getItemCount(SPLINTER_STAKATO_CHITIN) >= 100 && player.getInventory().getItemCount(NEEDLE_STAKATO_CHITIN) >= 100)
					{
						htmltext = "31554-05.htm";
						st.setCond(3);
						playSound(player, SOUND_FINISH);
						takeItems(player, SPLINTER_STAKATO_CHITIN, -1);
						takeItems(player, NEEDLE_STAKATO_CHITIN, -1);
						takeItems(player, GOLDEN_RAM_BADGE_RECRUIT, 1);
						giveItems(player, GOLDEN_RAM_BADGE_SOLDIER, 1);
					}
					else if (!player.getInventory().hasItems(SPLINTER_STAKATO_CHITIN) && !player.getInventory().hasItems(NEEDLE_STAKATO_CHITIN))
						htmltext = "31554-04b.htm";
					else
						htmltext = "31554-04a.htm";
				}
				else if (cond == 3)
					htmltext = "31554-05a.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();
		
		final QuestState st = getRandomPartyMemberState(player, npc, QuestStatus.STARTED);
		if (st == null)
			return null;
		
		final int cond = st.getCond();
		final int npcId = npc.getNpcId();
		
		switch (npcId)
		{
			case 21508:
			case 21509:
			case 21510:
			case 21511:
			case 21512:
				if (cond == 1 || cond == 2)
					dropItems(st.getPlayer(), SPLINTER_STAKATO_CHITIN, 1, 100, CHANCES.get(npcId));
				break;
			
			case 21513:
			case 21514:
			case 21515:
			case 21516:
			case 21517:
				if (cond == 2)
					dropItems(st.getPlayer(), NEEDLE_STAKATO_CHITIN, 1, 100, CHANCES.get(npcId));
				break;
		}
		
		return null;
	}
}