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

public class Q604_DaimonTheWhiteEyed_Part2 extends Quest
{
	private static final String QUEST_NAME = "Q604_DaimonTheWhiteEyed_Part2";
	
	// Monster
	private static final int DAIMON_THE_WHITE_EYED = 25290;
	
	// NPCs
	private static final int EYE_OF_ARGOS = 31683;
	private static final int DAIMON_ALTAR = 31541;
	
	// Items
	private static final int UNFINISHED_SUMMON_CRYSTAL = 7192;
	private static final int SUMMON_CRYSTAL = 7193;
	private static final int ESSENCE_OF_DAIMON = 7194;
	private static final int[] REWARDS =
	{
		4595,
		4596,
		4597,
		4598,
		4599,
		4600
	};
	
	// Other
	private static final int CHECK_INTERVAL = 600000; // 10 minutes
	private static final int IDLE_INTERVAL = 3; // (X * CHECK_INTERVAL) = 30 minutes
	
	private Npc _npc;
	private int _status = -1;
	
	public Q604_DaimonTheWhiteEyed_Part2()
	{
		super(604, "Daimon The White-Eyed - Part 2");
		
		setItemsIds(SUMMON_CRYSTAL, ESSENCE_OF_DAIMON);
		
		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS, DAIMON_ALTAR);
		
		addAttackId(DAIMON_THE_WHITE_EYED);
		addKillId(DAIMON_THE_WHITE_EYED);
		
		switch (RaidBossManager.getInstance().getStatus(DAIMON_THE_WHITE_EYED))
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
		
		// Eye of Argos
		if (event.equalsIgnoreCase("31683-03.htm"))
		{
			if (player.getInventory().hasItems(UNFINISHED_SUMMON_CRYSTAL))
			{
				st.setState(QuestStatus.STARTED);
				st.setCond(1);
				playSound(player, SOUND_ACCEPT);
				takeItems(player, UNFINISHED_SUMMON_CRYSTAL, 1);
				giveItems(player, SUMMON_CRYSTAL, 1);
			}
			else
				htmltext = "31683-04.htm";
		}
		else if (event.equalsIgnoreCase("31683-08.htm"))
		{
			if (player.getInventory().hasItems(ESSENCE_OF_DAIMON))
			{
				takeItems(player, ESSENCE_OF_DAIMON, 1);
				rewardItems(player, Rnd.get(REWARDS), 5);
				playSound(player, SOUND_FINISH);
				st.exitQuest(true);
			}
			else
				htmltext = "31683-09.htm";
		}
		// Diamon's Altar
		else if (event.equalsIgnoreCase("31541-02.htm"))
		{
			if (player.getInventory().hasItems(SUMMON_CRYSTAL))
			{
				if (_status < 0)
				{
					if (spawnRaid())
					{
						st.setCond(2);
						playSound(player, SOUND_MIDDLE);
						takeItems(player, SUMMON_CRYSTAL, 1);
					}
				}
				else
					htmltext = "31541-04.htm";
			}
			else
				htmltext = "31541-03.htm";
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
				if (player.getStatus().getLevel() < 73)
				{
					htmltext = "31683-02.htm";
					st.exitQuest(true);
				}
				else
					htmltext = "31683-01.htm";
				break;
			
			case STARTED:
				int cond = st.getCond();
				switch (npc.getNpcId())
				{
					case EYE_OF_ARGOS:
						if (cond == 1)
							htmltext = "31683-05.htm";
						else if (cond == 2)
							htmltext = "31683-06.htm";
						else
							htmltext = "31683-07.htm";
						break;
					
					case DAIMON_ALTAR:
						if (cond == 1)
							htmltext = "31541-01.htm";
						else if (cond == 2)
							htmltext = "31541-05.htm";
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
			final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(DAIMON_THE_WHITE_EYED);
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
				giveItems(pm, ESSENCE_OF_DAIMON, 1);
			}
		}
		
		npc.broadcastNpcSay(NpcStringId.ID_60404);
		
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
			_npc = addSpawn(DAIMON_ALTAR, 186304, -43744, -3193, 57000, false, 0, false);
	}
	
	private boolean spawnRaid()
	{
		final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(DAIMON_THE_WHITE_EYED);
		if (bs != null && bs.getStatus() == BossStatus.ALIVE)
		{
			final Npc raid = bs.getBoss();
			
			// set temporarily spawn location (to provide correct behavior of checkAndReturnToSpawn())
			raid.getSpawn().setLoc(185900, -44000, -3160, Rnd.get(65536));
			
			// teleport raid from secret place
			raid.teleportTo(185900, -44000, -3160, 100);
			raid.broadcastNpcSay(NpcStringId.ID_60403);
			
			// set raid status
			_status = IDLE_INTERVAL;
			
			return true;
		}
		
		return false;
	}
	
	private void despawnRaid(Npc raid)
	{
		// reset spawn location
		raid.getSpawn().setLoc(-106500, -252700, -15542, 0);
		
		// teleport raid back to secret place
		if (!raid.isDead())
			raid.teleportTo(-106500, -252700, -15542, 0);
		
		// reset raid status
		_status = -1;
	}
}