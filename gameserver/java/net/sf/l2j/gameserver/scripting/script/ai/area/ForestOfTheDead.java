package net.sf.l2j.gameserver.scripting.script.ai.area;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.data.manager.RaidBossManager;
import net.sf.l2j.gameserver.enums.BossStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.spawn.BossSpawn;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class ForestOfTheDead extends AttackableAIScript
{
	public static final SpawnLocation HELLMANN_DAY_LOC = new SpawnLocation(-104100, -252700, -15542, 0);
	public static final SpawnLocation HELLMANN_NIGHT_LOC = new SpawnLocation(59050, -42200, -3003, 100);
	
	private static final String QUEST_NAME = "Q024_InhabitantsOfTheForestOfTheDead";
	
	private static final int HELLMANN = 25328;
	private static final int NIGHT_DORIAN = 25332;
	private static final int LIDIA_MAID = 31532;
	
	private static final int DAY_VIOLET = 31386;
	private static final int DAY_KURSTIN = 31387;
	private static final int DAY_MINA = 31388;
	private static final int DAY_DORIAN = 31389;
	
	private static final int SILVER_CROSS = 7153;
	private static final int BROKEN_SILVER_CROSS = 7154;
	
	private final Set<Npc> _npcs = ConcurrentHashMap.newKeySet(4);
	
	private Npc _lidiaMaid;
	
	public ForestOfTheDead()
	{
		super("ai/area");
		
		if (!GameTimeTaskManager.getInstance().isNight())
			handleDay();
		else
			handleNight();
	}
	
	@Override
	protected void registerNpcs()
	{
		addCreatureSeeId(NIGHT_DORIAN);
		
		addGameTimeNotify();
	}
	
	@Override
	public String onCreatureSee(Npc npc, Creature creature)
	{
		if (creature instanceof Player)
		{
			final Player player = creature.getActingPlayer();
			
			final QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
			if (st == null)
				return super.onCreatureSee(npc, creature);
			
			if (st.getCond() == 3)
			{
				st.setCond(4);
				takeItems(player, SILVER_CROSS, -1);
				giveItems(player, BROKEN_SILVER_CROSS, 1);
				playSound(player, SOUND_MIDDLE);
				npc.broadcastNpcSay(NpcStringId.ID_2450);
			}
		}
		return super.onCreatureSee(npc, creature);
	}
	
	@Override
	public void onGameTime(int gameTime)
	{
		// Hellmann despawns at day.
		if (gameTime == 360)
			handleDay();
		// And spawns at night.
		else if (gameTime == 0)
			handleNight();
	}
	
	private void handleDay()
	{
		// Spawn Hellmann boss in hidden room.
		final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(HELLMANN);
		if (bs != null && bs.getStatus() == BossStatus.ALIVE)
		{
			final Npc raid = bs.getBoss();
			
			raid.getSpawn().setLoc(HELLMANN_DAY_LOC);
			raid.teleportTo(HELLMANN_DAY_LOC, 0);
		}
		
		// Despawn Lidia.
		if (_lidiaMaid != null)
		{
			_lidiaMaid.deleteMe();
			_lidiaMaid = null;
		}
		
		// Spawn Day NPCs in Cursed Village.
		_npcs.add(addSpawn(DAY_VIOLET, 59618, -42774, -3000, 5636, false, 0, false));
		_npcs.add(addSpawn(DAY_KURSTIN, 58790, -42646, -3000, 240, false, 0, false));
		_npcs.add(addSpawn(DAY_MINA, 59626, -41684, -3000, 48457, false, 0, false));
		_npcs.add(addSpawn(DAY_DORIAN, 60161, -42086, -3000, 30212, false, 0, false));
	}
	
	private void handleNight()
	{
		// Spawn Hellmann boss in Cursed Village.
		final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(HELLMANN);
		if (bs != null && bs.getStatus() == BossStatus.ALIVE)
		{
			final Npc raid = bs.getBoss();
			
			raid.getSpawn().setLoc(HELLMANN_NIGHT_LOC);
			raid.teleportTo(HELLMANN_NIGHT_LOC, 0);
		}
		
		// Spawn Lidia.
		_lidiaMaid = addSpawn(LIDIA_MAID, 47108, -36189, -1624, -22192, false, 0, false);
		
		// Despawn Day NPCs in Cursed Village.
		_npcs.forEach(Npc::deleteMe);
		_npcs.clear();
	}
}