package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.RaidBossManager;
import net.sf.l2j.gameserver.enums.BossStatus;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.spawn.BossSpawn;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Q625_TheFinestIngredients_Part2 extends Quest
{
	private static final String QUEST_NAME = "Q625_TheFinestIngredients_Part2";
	
	// Monster
	private static final int ICICLE_EMPEROR_BUMBALUMP = 25296;
	
	// NPCs
	private static final int JEREMY = 31521;
	private static final int YETI_TABLE = 31542;
	
	// Items
	private static final int SOY_SAUCE_JAR = 7205;
	private static final int FOOD_FOR_BUMBALUMP = 7209;
	private static final int SPECIAL_YETI_MEAT = 7210;
	private static final int[] REWARDS =
	{
		4589,
		4590,
		4591,
		4592,
		4593,
		4594
	};
	
	// Other
	private static final int CHECK_INTERVAL = 600000; // 10 minutes
	private static final int IDLE_INTERVAL = 3; // (X * CHECK_INTERVAL) = 30 minutes
	
	private Npc _npc;
	private int _status = -1;
	
	public Q625_TheFinestIngredients_Part2()
	{
		super(625, "The Finest Ingredients - Part 2");
		
		setItemsIds(FOOD_FOR_BUMBALUMP, SPECIAL_YETI_MEAT);
		
		addStartNpc(JEREMY);
		addTalkId(JEREMY, YETI_TABLE);
		
		addAttackId(ICICLE_EMPEROR_BUMBALUMP);
		addKillId(ICICLE_EMPEROR_BUMBALUMP);
		
		switch (RaidBossManager.getInstance().getStatus(ICICLE_EMPEROR_BUMBALUMP))
		{
			case ALIVE:
				spawnNpc();
			case DEAD:
				startQuestTimerAtFixedRate("check", null, null, CHECK_INTERVAL);
				break;
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		// Jeremy
		if (event.equalsIgnoreCase("31521-03.htm"))
		{
			if (player.getInventory().hasItems(SOY_SAUCE_JAR))
			{
				st.setState(QuestStatus.STARTED);
				st.setCond(1);
				playSound(player, SOUND_ACCEPT);
				takeItems(player, SOY_SAUCE_JAR, 1);
				giveItems(player, FOOD_FOR_BUMBALUMP, 1);
			}
			else
				htmltext = "31521-04.htm";
		}
		else if (event.equalsIgnoreCase("31521-08.htm"))
		{
			if (player.getInventory().hasItems(SPECIAL_YETI_MEAT))
			{
				takeItems(player, SPECIAL_YETI_MEAT, 1);
				rewardItems(player, Rnd.get(REWARDS), 5);
				playSound(player, SOUND_FINISH);
				st.exitQuest(true);
			}
			else
				htmltext = "31521-09.htm";
		}
		// Yeti's Table
		else if (event.equalsIgnoreCase("31542-02.htm"))
		{
			if (player.getInventory().hasItems(FOOD_FOR_BUMBALUMP))
			{
				if (_status < 0)
				{
					if (spawnRaid())
					{
						st.setCond(2);
						playSound(player, SOUND_MIDDLE);
						takeItems(player, FOOD_FOR_BUMBALUMP, 1);
					}
				}
				else
					htmltext = "31542-04.htm";
			}
			else
				htmltext = "31542-03.htm";
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
				htmltext = (player.getStatus().getLevel() < 73) ? "31521-02.htm" : "31521-01.htm";
				break;
			
			case STARTED:
				final int cond = st.getCond();
				switch (npc.getNpcId())
				{
					case JEREMY:
						if (cond == 1)
							htmltext = "31521-05.htm";
						else if (cond == 2)
							htmltext = "31521-06.htm";
						else
							htmltext = "31521-07.htm";
						break;
					
					case YETI_TABLE:
						if (cond == 1)
							htmltext = "31542-01.htm";
						else if (cond == 2)
							htmltext = "31542-05.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equals("check"))
		{
			final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(ICICLE_EMPEROR_BUMBALUMP);
			if (bs != null && bs.getStatus() == BossStatus.ALIVE)
			{
				final Npc raid = bs.getBoss();
				
				if (_status >= 0 && _status-- == 0)
					despawnRaid(raid);
				
				spawnNpc();
			}
		}
		
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		final Player player = attacker.getActingPlayer();
		if (player != null)
			_status = IDLE_INTERVAL;
		
		return null;
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();
		if (player != null)
		{
			for (QuestState st : getPartyMembers(player, npc, 2))
			{
				Player pm = st.getPlayer();
				st.setCond(3);
				playSound(pm, SOUND_MIDDLE);
				giveItems(pm, SPECIAL_YETI_MEAT, 1);
			}
		}
		
		npc.broadcastNpcSay(NpcStringId.ID_62504);
		
		// despawn raid (reset info)
		despawnRaid(npc);
		
		// despawn npc
		if (_npc != null)
		{
			_npc.deleteMe();
			_npc = null;
		}
		
		return null;
	}
	
	private void spawnNpc()
	{
		// spawn npc, if not spawned
		if (_npc == null)
			_npc = addSpawn(YETI_TABLE, 157136, -121456, -2363, 40000, false, 0, false);
	}
	
	private boolean spawnRaid()
	{
		final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(ICICLE_EMPEROR_BUMBALUMP);
		if (bs != null && bs.getStatus() == BossStatus.ALIVE)
		{
			final Npc raid = bs.getBoss();
			
			// set temporarily spawn location (to provide correct behavior of checkAndReturnToSpawn())
			raid.getSpawn().setLoc(157117, -121939, -2397, Rnd.get(65536));
			
			// teleport raid from secret place
			raid.teleportTo(157117, -121939, -2397, 100);
			raid.broadcastNpcSay(NpcStringId.ID_62503);
			
			// set raid status
			_status = IDLE_INTERVAL;
			
			return true;
		}
		
		return false;
	}
	
	private void despawnRaid(Npc raid)
	{
		// reset spawn location
		raid.getSpawn().setLoc(-104700, -252700, -15542, 0);
		
		// teleport raid back to secret place
		if (!raid.isDead())
			raid.teleportTo(-104700, -252700, -15542, 0);
		
		// reset raid status
		_status = -1;
	}
}