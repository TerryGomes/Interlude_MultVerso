package net.sf.l2j.gameserver.model.actor.container.npc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.MinionData;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.SpawnLocation;

public class MinionList extends ConcurrentHashMap<Monster, Boolean>
{
	private static final long serialVersionUID = 1L;
	
	private final Monster _master;
	
	public MinionList(Monster master)
	{
		_master = master;
	}
	
	/**
	 * @return a {@link List} of spawned {@link Monster}s minions.
	 */
	public List<Monster> getSpawnedMinions()
	{
		return entrySet().stream().filter(m -> m.getValue() == true).map(Map.Entry::getKey).collect(Collectors.toList());
	}
	
	/**
	 * Manage the spawn of {@link Monster}s minions, using the associated {@link MinionData}'s {@link Monster} master.
	 */
	public final void spawnMinions()
	{
		// We generate new instances. We can't reuse existing instances, since previous monsters can still exist.
		for (MinionData data : _master.getTemplate().getMinionData())
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(data.getMinionId());
			if (template == null)
				continue;
			
			for (int i = 0; i < data.getAmount(); i++)
			{
				final Monster minion = new Monster(IdFactory.getInstance().getNextId(), template);
				minion.setMaster(_master);
				minion.setMinion(_master.isRaidBoss());
				
				initializeMinion(_master, minion);
			}
		}
	}
	
	/**
	 * Called on the {@link Monster} master death.
	 * <ul>
	 * <li>In case of regular {@link Monster} master : minions references are deleted, but {@link Monster}s instances are kept alive (until they gonna be deleted if their interaction move to IDLE, or killed).</li>
	 * <li>In case of raid bosses : all {@link Monster} minions are instantly deleted.</li>
	 * </ul>
	 */
	public void onMasterDie()
	{
		if (_master.isRaidBoss())
		{
			// For all spawned minions, delete them.
			for (Monster minion : getSpawnedMinions())
				minion.deleteMe();
		}
		else
		{
			// For all minions, remove leader reference.
			for (Monster minion : keySet())
				minion.setMaster(null);
		}
		
		// Cleanup the entire MinionList.
		clear();
	}
	
	/**
	 * Called on {@link Monster} master deletion.
	 */
	public void onMasterDeletion()
	{
		// For all minions, delete them and remove leader reference.
		for (Monster minion : keySet())
		{
			minion.setMaster(null);
			minion.deleteMe();
		}
		
		// Cleanup the entire MinionList.
		clear();
	}
	
	/**
	 * Called on {@link Monster} minion deletion, or on {@link Monster} master death.
	 * @param minion : The {@link Monster} minion to make checks on.
	 */
	public void onMinionDeletion(Monster minion)
	{
		// Keep it to avoid OOME.
		minion.setMaster(null);
		
		// Cleanup the map form this reference.
		remove(minion);
	}
	
	/**
	 * Called on {@link Monster} minion death. Flag the {@link Monster} from the list of the spawned minions as unspawned.
	 * @param minion : The {@link Monster} minion to make checks on.
	 * @param respawnTime : Respawn the {@link Monster} using this timer, but only if {@link Monster} master is alive.
	 */
	public void onMinionDie(Monster minion, int respawnTime)
	{
		put(minion, false);
		
		if (minion.isRaidRelated() && respawnTime > 0 && !_master.isAlikeDead())
		{
			ThreadPool.schedule(() ->
			{
				// Master is visible, but minion isn't spawned back (via teleport, for example).
				if (!_master.isAlikeDead() && _master.isVisible())
				{
					final Boolean state = get(minion);
					if (state != null && !state)
					{
						minion.refreshID();
						
						initializeMinion(_master, minion);
					}
				}
			}, respawnTime);
		}
	}
	
	/**
	 * Called when {@link Monster} master/minion is attacked. Master and its minions aggro the {@link Creature} attacker.
	 * @param caller : The {@link Creature} calling for help.
	 * @param attacker : The {@link Creature} who will be aggroed.
	 */
	public void onAssist(Creature caller, Creature attacker)
	{
		if (attacker == null)
			return;
		
		// The master is aggroed.
		if (!_master.isAlikeDead() && !_master.isInCombat())
			_master.getAggroList().addDamageHate(attacker, 0, 1);
		
		final boolean callerIsMaster = (caller == _master);
		
		// Define the aggro value of minions.
		int aggro = (callerIsMaster ? 10 : 1);
		if (_master.isRaidBoss())
			aggro *= 10;
		
		for (Monster minion : getSpawnedMinions())
		{
			if (!minion.isDead() && (callerIsMaster || !minion.isInCombat()))
				minion.getAggroList().addDamageHate(attacker, 0, aggro);
		}
	}
	
	/**
	 * Teleport all {@link Monster} minions back to {@link Monster} master position.
	 */
	public void onMasterTeleported()
	{
		for (Monster minion : getSpawnedMinions())
		{
			if (minion.isDead() || minion.isMovementDisabled())
				continue;
			
			minion.teleportToMaster();
		}
	}
	
	/**
	 * Prepare a {@link Monster} minion and spawn it.
	 * @param master : The {@link Monster} master.
	 * @param minion : The {@link Monster} minion.
	 */
	private void initializeMinion(Monster master, Monster minion)
	{
		put(minion, true);
		
		minion.setNoRndWalk(true);
		minion.stopAllEffects();
		minion.setIsDead(false);
		minion.setDecayed(false);
		minion.getStatus().setMaxHpMp();
		
		final int minOffset = (int) (master.getCollisionRadius() + 30);
		final int maxOffset = (int) (100 + minion.getCollisionRadius() + master.getCollisionRadius());
		
		final SpawnLocation spawnLoc = master.getPosition().clone();
		spawnLoc.addRandomOffsetBetweenTwoValues(minOffset, maxOffset);
		spawnLoc.set(GeoEngine.getInstance().getValidLocation(master, spawnLoc));
		
		minion.spawnMe(spawnLoc);
	}
}