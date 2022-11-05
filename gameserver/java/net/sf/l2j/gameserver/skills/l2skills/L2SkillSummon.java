package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.actor.instance.SiegeSummon;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.skills.L2Skill;

public class L2SkillSummon extends L2Skill
{
	private final int _npcId;
	private final float _expPenalty;
	private final boolean _isCubic;
	
	private final int _activationTime;
	private final int _activationChance;
	
	private final int _summonTotalLifeTime;
	private final int _summonTimeLostIdle;
	private final int _summonTimeLostActive;
	
	private final int _itemConsumeTime;
	private final int _itemConsumeOT;
	private final int _itemConsumeIdOT;
	private final int _itemConsumeSteps;
	
	private static final int SUMMON_SOULLESS = 1278;
	
	public L2SkillSummon(StatSet set)
	{
		super(set);
		
		_npcId = set.getInteger("npcId", 0); // default for undescribed skills
		_expPenalty = set.getFloat("expPenalty", 0.f);
		_isCubic = set.getBool("isCubic", false);
		
		_activationTime = set.getInteger("activationtime", 8);
		_activationChance = set.getInteger("activationchance", 30);
		
		_summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000); // 20 minutes default
		_summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);
		
		_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
		_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
		_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
	}
	
	public boolean checkCondition(Creature activeChar)
	{
		if (activeChar instanceof Player)
		{
			Player player = (Player) activeChar;
			
			if (isCubic())
			{
				// Player is always able to cast mass cubic skill
				if (getTargetType() != SkillTargetType.SELF)
					return true;
				
				if (player.getCubicList().isFull())
				{
					player.sendPacket(SystemMessageId.CUBIC_SUMMONING_FAILED);
					return false;
				}
			}
			else
			{
				if (player.isInObserverMode())
					return false;
				
				if (player.getSummon() != null)
				{
					player.sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
					return false;
				}
			}
		}
		return super.checkCondition(activeChar, null, false);
	}
	
	@Override
	public void useSkill(Creature caster, WorldObject[] targets)
	{
		if (caster.isAlikeDead() || !(caster instanceof Player))
			return;
		
		Player activeChar = (Player) caster;
		
		if (_npcId == 0)
		{
			activeChar.sendMessage("Summon skill " + getId() + " not described yet");
			return;
		}
		
		if (_isCubic)
		{
			int skillLevel = getLevel();
			if (skillLevel > 100)
				skillLevel = Math.round(((getLevel() - 100) / 7) + 8);
			
			// Mass cubic skill.
			if (targets.length > 1)
			{
				for (WorldObject obj : targets)
				{
					if (!(obj instanceof Player))
						continue;
					
					((Player) obj).getCubicList().addOrRefreshCubic(_npcId, skillLevel, getPower(), _activationTime, _activationChance, _summonTotalLifeTime, (obj != activeChar));
				}
			}
			else
				activeChar.getCubicList().addOrRefreshCubic(_npcId, skillLevel, getPower(), _activationTime, _activationChance, _summonTotalLifeTime, false);
		}
		else
		{
			if (activeChar.getSummon() != null || activeChar.isMounted())
				return;
			
			Servitor summon;
			NpcTemplate summonTemplate = NpcData.getInstance().getTemplate(_npcId);
			if (summonTemplate == null)
			{
				LOGGER.warn("Couldn't properly spawn with id {} ; the template is missing.", _npcId);
				return;
			}
			
			if (summonTemplate.isType("SiegeSummon"))
				summon = new SiegeSummon(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
			else
				summon = new Servitor(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
			
			activeChar.setSummon(summon);
			
			summon.setName(summonTemplate.getName());
			summon.setTitle(activeChar.getName());
			summon.setExpPenalty(_expPenalty);
			summon.getStatus().setMaxHpMp();
			summon.forceRunStance();
			
			final SpawnLocation spawnLoc = activeChar.getPosition().clone();
			spawnLoc.addStrictOffset(40);
			spawnLoc.setHeadingTo(activeChar.getPosition());
			spawnLoc.set(GeoEngine.getInstance().getValidLocation(activeChar, spawnLoc));
			
			summon.spawnMe(spawnLoc);
			summon.getAI().setFollowStatus(true);
			
			if (getId() == SUMMON_SOULLESS)
				SkillTable.getInstance().getInfo(Summon.CONTRACT_PAYMENT, MathUtil.limit(getLevel() - 2, 1, 12)).getEffects(activeChar, activeChar);
		}
		
		activeChar.setChargedShot(activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT) ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
	}
	
	public final boolean isCubic()
	{
		return _isCubic;
	}
	
	public final int getTotalLifeTime()
	{
		return _summonTotalLifeTime;
	}
	
	public final int getTimeLostIdle()
	{
		return _summonTimeLostIdle;
	}
	
	public final int getTimeLostActive()
	{
		return _summonTimeLostActive;
	}
	
	/**
	 * @return Returns the itemConsume count over time.
	 */
	public final int getItemConsumeOT()
	{
		return _itemConsumeOT;
	}
	
	/**
	 * @return Returns the itemConsumeId over time.
	 */
	public final int getItemConsumeIdOT()
	{
		return _itemConsumeIdOT;
	}
	
	public final int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}
	
	/**
	 * @return Returns the itemConsume time in milliseconds.
	 */
	public final int getItemConsumeTime()
	{
		return _itemConsumeTime;
	}
}