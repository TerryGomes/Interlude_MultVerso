package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.data.manager.FourSepulchersManager;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q620_FourGoblets extends Quest
{
	private static final String QUEST_NAME = "Q620_FourGoblets";
	
	// NPCs
	private static final int GHOST_OF_WIGOTH_1 = 31452;
	private static final int NAMELESS_SPIRIT = 31453;
	private static final int GHOST_OF_WIGOTH_2 = 31454;
	
	private static final int GHOST_CHAMBERLAIN = 31919;
	
	private static final int CONQUERORS_SEPULCHER_MANAGER = 31921;
	private static final int EMPERORS_SEPULCHER_MANAGER = 31922;
	private static final int GREAT_SAGES_SEPULCHER_MANAGER = 31923;
	private static final int JUDGES_SEPULCHER_MANAGER = 31924;
	
	// Items
	private static final int BROKEN_RELIC_PART = 7254;
	private static final int SEALED_BOX = 7255;
	
	private static final int GOBLET_OF_ALECTIA = 7256;
	private static final int GOBLET_OF_TISHAS = 7257;
	private static final int GOBLET_OF_MEKARA = 7258;
	private static final int GOBLET_OF_MORIGUL = 7259;
	
	private static final int USED_GRAVE_PASS = 7261;
	
	// Rewards
	private static final int ANTIQUE_BROOCH = 7262;
	private static final int[] RCP_REWARDS = new int[]
	{
		6881,
		6883,
		6885,
		6887,
		6891,
		6893,
		6895,
		6897,
		6899,
		7580
	};
	
	public Q620_FourGoblets()
	{
		super(620, "Four Goblets");
		
		setItemsIds(SEALED_BOX, USED_GRAVE_PASS, GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL);
		
		addStartNpc(NAMELESS_SPIRIT);
		addTalkId(NAMELESS_SPIRIT, CONQUERORS_SEPULCHER_MANAGER, EMPERORS_SEPULCHER_MANAGER, GREAT_SAGES_SEPULCHER_MANAGER, JUDGES_SEPULCHER_MANAGER, GHOST_CHAMBERLAIN, GHOST_OF_WIGOTH_1, GHOST_OF_WIGOTH_2);
		
		for (int id = 18120; id <= 18256; id++)
			addKillId(id);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		// Ghost of Wigoth 1
		if (event.equalsIgnoreCase("31452-05.htm"))
		{
			if (Rnd.nextBoolean())
				htmltext = (Rnd.nextBoolean()) ? "31452-03.htm" : "31452-04.htm";
		}
		else if (event.equalsIgnoreCase("31452-06.htm"))
		{
			player.teleportTo(169590, -90218, -2914, 0);
		}
		// Nameless Spirit
		else if (event.equalsIgnoreCase("31453-13.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31453-16.htm"))
		{
			if (player.getInventory().hasItems(GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL))
			{
				st.setCond(2);
				playSound(player, SOUND_MIDDLE);
				takeItems(player, GOBLET_OF_ALECTIA, -1);
				takeItems(player, GOBLET_OF_TISHAS, -1);
				takeItems(player, GOBLET_OF_MEKARA, -1);
				takeItems(player, GOBLET_OF_MORIGUL, -1);
				giveItems(player, ANTIQUE_BROOCH, 1);
			}
			else
				htmltext = "31453-14.htm";
		}
		else if (event.equalsIgnoreCase("31453-13.htm"))
		{
			if (st.getCond() == 2)
				htmltext = "31453-19.htm";
		}
		else if (event.equalsIgnoreCase("31453-18.htm"))
		{
			playSound(player, SOUND_GIVEUP);
			st.exitQuest(true);
		}
		// Ghost of Wigoth 2
		else if (event.equalsIgnoreCase("boxes"))
		{
			if (player.getInventory().hasItems(SEALED_BOX))
			{
				takeItems(player, SEALED_BOX, 1);
				
				if (!calculateBoxReward(player))
					htmltext = (Rnd.nextBoolean()) ? "31454-09.htm" : "31454-10.htm";
				else
					htmltext = "31454-08.htm";
			}
		}
		else if (StringUtil.isDigit(event))
		{
			// If event is a simple digit, parse it to get an integer form, then test the reward list.
			final int id = Integer.parseInt(event);
			if (ArraysUtil.contains(RCP_REWARDS, id) && player.getInventory().getItemCount(BROKEN_RELIC_PART) >= 1000)
			{
				takeItems(player, BROKEN_RELIC_PART, 1000);
				giveItems(player, id, 1);
			}
			htmltext = "31454-12.htm";
		}
		// Ghost Chamberlain of Elmoreden
		else if (event.equalsIgnoreCase("31919-06.htm"))
		{
			if (player.getInventory().hasItems(SEALED_BOX))
			{
				takeItems(player, SEALED_BOX, 1);
				
				if (!calculateBoxReward(player))
					htmltext = (Rnd.nextBoolean()) ? "31919-04.htm" : "31919-05.htm";
				else
					htmltext = "31919-03.htm";
			}
		}
		// Sepulcher managers
		else if (event.equalsIgnoreCase("Enter"))
		{
			FourSepulchersManager.getInstance().tryEntry(npc, player);
			return null;
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
				htmltext = (player.getStatus().getLevel() >= 74) ? "31453-01.htm" : "31453-12.htm";
				break;
			
			case STARTED:
				final int cond = st.getCond();
				switch (npc.getNpcId())
				{
					case GHOST_OF_WIGOTH_1:
						if (cond == 1)
							htmltext = "31452-01.htm";
						else if (cond == 2)
							htmltext = "31452-02.htm";
						break;
					
					case NAMELESS_SPIRIT:
						if (cond == 1)
							htmltext = (player.getInventory().hasItems(GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL)) ? "31453-15.htm" : "31453-14.htm";
						else if (cond == 2)
							htmltext = "31453-17.htm";
						break;
					
					case GHOST_OF_WIGOTH_2:
						// Possibilities : 0 = nothing, 1 = seal boxes only, 2 = relics only, 3 = both, 4/5/6/7 = "4 goblets" versions of 0/1/2/3.
						int index = 0;
						
						if (player.getInventory().hasItems(GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL))
							index = 4;
						
						final boolean gotSealBoxes = player.getInventory().hasItems(SEALED_BOX);
						final boolean gotEnoughRelics = player.getInventory().getItemCount(BROKEN_RELIC_PART) >= 1000;
						
						if (gotSealBoxes && gotEnoughRelics)
							index += 3;
						else if (!gotSealBoxes && gotEnoughRelics)
							index += 2;
						else if (gotSealBoxes)
							index += 1;
						
						htmltext = "31454-0" + index + ".htm";
						break;
					
					case GHOST_CHAMBERLAIN:
					case CONQUERORS_SEPULCHER_MANAGER:
					case EMPERORS_SEPULCHER_MANAGER:
					case GREAT_SAGES_SEPULCHER_MANAGER:
					case JUDGES_SEPULCHER_MANAGER:
						htmltext = npc.getNpcId() + "-01.htm";
						break;
				}
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
		
		dropItems(st.getPlayer(), SEALED_BOX, 1, 0, 300000);
		return null;
	}
	
	/**
	 * Calculate boxes rewards, then return if there was a reward.
	 * @param player : The Player to calculate rewards for.
	 * @return true if there was a reward, false if not (used to call a "no-reward" html)
	 */
	private static boolean calculateBoxReward(Player player)
	{
		boolean reward = false;
		int rnd = Rnd.get(5);
		
		if (rnd == 0)
		{
			giveItems(player, 57, 10000);
			reward = true;
		}
		else if (rnd == 1)
		{
			if (Rnd.get(1000) < 848)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 43)
					giveItems(player, 1884, 42);
				else if (i < 66)
					giveItems(player, 1895, 36);
				else if (i < 184)
					giveItems(player, 1876, 4);
				else if (i < 250)
					giveItems(player, 1881, 6);
				else if (i < 287)
					giveItems(player, 5549, 8);
				else if (i < 484)
					giveItems(player, 1874, 1);
				else if (i < 681)
					giveItems(player, 1889, 1);
				else if (i < 799)
					giveItems(player, 1877, 1);
				else if (i < 902)
					giveItems(player, 1894, 1);
				else
					giveItems(player, 4043, 1);
			}
			
			if (Rnd.get(1000) < 323)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 335)
					giveItems(player, 1888, 1);
				else if (i < 556)
					giveItems(player, 4040, 1);
				else if (i < 725)
					giveItems(player, 1890, 1);
				else if (i < 872)
					giveItems(player, 5550, 1);
				else if (i < 962)
					giveItems(player, 1893, 1);
				else if (i < 986)
					giveItems(player, 4046, 1);
				else
					giveItems(player, 4048, 1);
			}
		}
		else if (rnd == 2)
		{
			if (Rnd.get(1000) < 847)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 148)
					giveItems(player, 1878, 8);
				else if (i < 175)
					giveItems(player, 1882, 24);
				else if (i < 273)
					giveItems(player, 1879, 4);
				else if (i < 322)
					giveItems(player, 1880, 6);
				else if (i < 357)
					giveItems(player, 1885, 6);
				else if (i < 554)
					giveItems(player, 1875, 1);
				else if (i < 685)
					giveItems(player, 1883, 1);
				else if (i < 803)
					giveItems(player, 5220, 1);
				else if (i < 901)
					giveItems(player, 4039, 1);
				else
					giveItems(player, 4044, 1);
			}
			
			if (Rnd.get(1000) < 251)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 350)
					giveItems(player, 1887, 1);
				else if (i < 587)
					giveItems(player, 4042, 1);
				else if (i < 798)
					giveItems(player, 1886, 1);
				else if (i < 922)
					giveItems(player, 4041, 1);
				else if (i < 966)
					giveItems(player, 1892, 1);
				else if (i < 996)
					giveItems(player, 1891, 1);
				else
					giveItems(player, 4047, 1);
			}
		}
		else if (rnd == 3)
		{
			if (Rnd.get(1000) < 31)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 223)
					giveItems(player, 730, 1);
				else if (i < 893)
					giveItems(player, 948, 1);
				else
					giveItems(player, 960, 1);
			}
			
			if (Rnd.get(1000) < 5)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 202)
					giveItems(player, 729, 1);
				else if (i < 928)
					giveItems(player, 947, 1);
				else
					giveItems(player, 959, 1);
			}
		}
		else if (rnd == 4)
		{
			if (Rnd.get(1000) < 329)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 88)
					giveItems(player, 6698, 1);
				else if (i < 185)
					giveItems(player, 6699, 1);
				else if (i < 238)
					giveItems(player, 6700, 1);
				else if (i < 262)
					giveItems(player, 6701, 1);
				else if (i < 292)
					giveItems(player, 6702, 1);
				else if (i < 356)
					giveItems(player, 6703, 1);
				else if (i < 420)
					giveItems(player, 6704, 1);
				else if (i < 482)
					giveItems(player, 6705, 1);
				else if (i < 554)
					giveItems(player, 6706, 1);
				else if (i < 576)
					giveItems(player, 6707, 1);
				else if (i < 640)
					giveItems(player, 6708, 1);
				else if (i < 704)
					giveItems(player, 6709, 1);
				else if (i < 777)
					giveItems(player, 6710, 1);
				else if (i < 799)
					giveItems(player, 6711, 1);
				else if (i < 863)
					giveItems(player, 6712, 1);
				else if (i < 927)
					giveItems(player, 6713, 1);
				else
					giveItems(player, 6714, 1);
			}
			
			if (Rnd.get(1000) < 54)
			{
				reward = true;
				int i = Rnd.get(1000);
				
				if (i < 100)
					giveItems(player, 6688, 1);
				else if (i < 198)
					giveItems(player, 6689, 1);
				else if (i < 298)
					giveItems(player, 6690, 1);
				else if (i < 398)
					giveItems(player, 6691, 1);
				else if (i < 499)
					giveItems(player, 7579, 1);
				else if (i < 601)
					giveItems(player, 6693, 1);
				else if (i < 703)
					giveItems(player, 6694, 1);
				else if (i < 801)
					giveItems(player, 6695, 1);
				else if (i < 902)
					giveItems(player, 6696, 1);
				else
					giveItems(player, 6697, 1);
			}
		}
		
		return reward;
	}
}