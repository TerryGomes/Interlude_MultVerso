package net.sf.l2j.gameserver.data.manager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.BuffSkillHolder;
import net.sf.l2j.gameserver.skills.L2Skill;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * Loads and stores available {@link BuffSkillHolder}s for the integrated scheme buffer.<br>
 * Loads and stores Players' buff schemes into _schemesTable (under a {@link String} name and a {@link List} of {@link Integer} skill ids).
 */
public class BufferManager implements IXmlReader
{
	private static final String LOAD_SCHEMES = "SELECT * FROM buffer_schemes";
	private static final String DELETE_SCHEMES = "TRUNCATE TABLE buffer_schemes";
	private static final String INSERT_SCHEME = "INSERT INTO buffer_schemes (object_id, scheme_name, skills, levels) VALUES (?,?,?,?)";

	private final Map<Integer, Map<String, ArrayList<L2Skill>>> _schemesTable = new ConcurrentHashMap<>();
	private final Map<L2Skill, BuffSkillHolder> _availableBuffs = new LinkedHashMap<>();
	private final Map<BufferSchemeType, List<BuffSkillHolder>> _availableSchemes = new HashMap<>();

	public enum BufferSchemeType
	{
		FIGHTER,
		MAGE
	}

	protected BufferManager()
	{
		load();
	}

	@Override
	public void load()
	{
		parseFile("./data/xml/bufferSkills.xml");
		LOGGER.info("Loaded {} available buffs.", _availableBuffs.size());
		LOGGER.info("Loaded {} ready to use schemes.", _availableSchemes.size());

		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SCHEMES);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final ArrayList<L2Skill> schemeList = new ArrayList<>();

				final String[] skills = rs.getString("skills").split(",");
				String levelsString = rs.getString("levels");
				String[] levels = null;
				if (levelsString != null && levelsString.length() != 0)
				{
					levels = levelsString.split(",");
				}
				for (int i = 0; i < skills.length; i++)
				{
					if (skills[i].isEmpty())
					{
						break;
					}

					final int skillId = Integer.parseInt(skills[i]);
					int skillLevel = (levels == null || levels.length == 0) ? SkillTable.getInstance().getMaxLevel(skillId) : Integer.parseInt(levels[i]);

					if (skillLevel == -1)
					{
						skillLevel = SkillTable.getInstance().getMaxLevel(skillId);
					}
					final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);

					// Integrity check to see if the skillId is available as a buff.
					if (_availableBuffs.containsKey(skill))
					{
						schemeList.add(skill);
					}
				}

				setScheme(rs.getInt("object_id"), rs.getString("scheme_name"), schemeList);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load schemes data.", e);
		}
	}

	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "category", categoryNode ->
			{
				final String category = parseString(categoryNode.getAttributes(), "type");
				forEach(categoryNode, "buff", buffNode ->
				{
					final NamedNodeMap attrs = buffNode.getAttributes();
					final int skillId = parseInteger(attrs, "id");
					final int skillLevel = parseInteger(attrs, "level", SkillTable.getInstance().getMaxLevel(skillId));
					_availableBuffs.put(SkillTable.getInstance().getInfo(skillId, skillLevel), new BuffSkillHolder(skillId, skillLevel, parseInteger(attrs, "price", 0), category, parseString(attrs, "desc", "")));
				});
			});

			forEach(listNode, "scheme", schemeNode ->
			{
				final String scheme = parseString(schemeNode.getAttributes(), "type").toUpperCase();
				final List<BuffSkillHolder> skillHolder = new ArrayList<>();
				forEach(schemeNode, "buff", buffNode ->
				{
					final NamedNodeMap attrs = buffNode.getAttributes();
					final int skillId = parseInteger(attrs, "id");
					skillHolder.add(new BuffSkillHolder(skillId, parseInteger(attrs, "level", SkillTable.getInstance().getMaxLevel(skillId)), parseInteger(attrs, "price", 0), scheme, ""));
				});

				_availableSchemes.put(BufferSchemeType.valueOf(scheme), skillHolder);
			});
		});
	}

	public void saveSchemes()
	{
		final StringBuilder sb = new StringBuilder();
		final StringBuilder sb2 = new StringBuilder();

		try (Connection con = ConnectionPool.getConnection())
		{
			// Delete all entries from database.
			try (PreparedStatement ps = con.prepareStatement(DELETE_SCHEMES))
			{
				ps.execute();
			}

			try (PreparedStatement ps = con.prepareStatement(INSERT_SCHEME))
			{
				// Save _schemesTable content.
				for (Map.Entry<Integer, Map<String, ArrayList<L2Skill>>> player : _schemesTable.entrySet())
				{
					for (Map.Entry<String, ArrayList<L2Skill>> scheme : player.getValue().entrySet())
					{
						// Build a String composed of skill ids seperated by a ",".
						for (L2Skill skill : scheme.getValue())
						{
							StringUtil.append(sb, skill.getId(), ",");
							StringUtil.append(sb2, skill.getLevel(), ",");
						}

						// Delete the last "," : must be called only if there is something to delete !
						if (sb.length() > 0)
						{
							sb.setLength(sb.length() - 1);
						}

						if (sb2.length() > 0)
						{
							sb2.setLength(sb2.length() - 1);
						}

						ps.setInt(1, player.getKey());
						ps.setString(2, scheme.getKey());
						ps.setString(3, sb.toString());
						ps.setString(4, sb2.toString());
						ps.addBatch();

						// Reuse the StringBuilder for next iteration.
						sb.setLength(0);
						sb2.setLength(0);
					}
				}
				ps.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to save schemes data.", e);
		}
	}

	/**
	 * Add or retrieve the Player schemes {@link Map}, then add or update the given scheme based on the {@link String} name set as parameter.
	 * @param playerId : The Player objectId to check.
	 * @param schemeName : The {@link String} used as scheme name.
	 * @param list : The {@link ArrayList} of {@link Integer} used as skill ids.
	 */
	public void setScheme(int playerId, String schemeName, ArrayList<L2Skill> list)
	{
		final Map<String, ArrayList<L2Skill>> schemes = _schemesTable.computeIfAbsent(playerId, s -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
		if (schemes.size() >= Config.BUFFER_MAX_SCHEMES)
		{
			return;
		}

		schemes.put(schemeName, list);
	}

	/**
	 * @param playerId : The Player objectId to check.
	 * @return the {@link List} of schemes for a given Player.
	 */
	public Map<String, ArrayList<L2Skill>> getPlayerSchemes(int playerId)
	{
		return _schemesTable.get(playerId);
	}

	/**
	 * @param playerId : The Player objectId to check.
	 * @param schemeName : The scheme name to check.
	 * @return The {@link List} holding {@link L2Skill}s for the given scheme name and Player, or null (if scheme or Player isn't registered).
	 */
	public List<L2Skill> getScheme(int playerId, String schemeName)
	{
		final Player player = World.getInstance().getPlayer(playerId);
		final Map<String, ArrayList<L2Skill>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return Collections.emptyList();
		}

		final ArrayList<L2Skill> scheme = schemes.get(schemeName);
		if (scheme == null)
		{
			return Collections.emptyList();
		}

		if (player.getPremiumService() == 0)
		{
			int j = scheme.size();
			for (int i = 0; i < j; i++)
			{
				if (Config.PREMIUM_BUFFS_CATEGORY.contains(getAvailableBuff(scheme.get(i)).getType()))
				{
					scheme.remove(i);
					i--;
					j--;
				}
			}
		}

		return scheme;
	}

	/**
	 * Apply all effects of a scheme (retrieved by its Player objectId and {@link String} name) upon a {@link Creature} target.
	 * @param npc : The {@link Npc} which apply effects.
	 * @param target : The {@link Creature} benefactor.
	 * @param playerId : The Player objectId to check.
	 * @param schemeName : The scheme name to check.
	 */
	public void applySchemeEffects(Npc npc, Creature target, int playerId, String schemeName)
	{
		for (L2Skill skill : getScheme(playerId, schemeName))
		{
			final BuffSkillHolder holder = getAvailableBuff(skill);
			if (holder != null)
			{
				final L2Skill s = holder.getSkill();
				if (s != null)
				{
					s.getEffects(npc, target);
				}
			}
		}
	}

	/**
	 * @param playerId : The Player objectId to check.
	 * @param schemeName : The scheme name to check.
	 * @param skill : The {@link L2Skill} id to check.
	 * @return True if the {@link L2Skill} is already registered on the scheme, or false otherwise.
	 */
	public boolean getSchemeContainsSkill(int playerId, String schemeName, L2Skill skill)
	{
		return getScheme(playerId, schemeName).contains(skill);
	}

	/**
	 * @param groupType : The {@link String} group type of skill ids to return.
	 * @return a {@link List} of skill ids based on the given {@link String} groupType.
	 */
	public List<L2Skill> getSkillsIdsByType(String groupType)
	{
		final List<L2Skill> skills = new ArrayList<>();
		for (BuffSkillHolder holder : _availableBuffs.values())
		{
			if (holder.getType().equalsIgnoreCase(groupType))
			{
				skills.add(holder.getSkill());
			}
		}
		return skills;
	}

	public List<L2Skill> getSchemeSkills(BufferSchemeType schemeType)
	{
		final List<L2Skill> skills = new ArrayList<>();
		_availableSchemes.get(schemeType).forEach(skill -> skills.add(skill.getSkill()));
		return skills;
	}

	/**
	 * @return a {@link List} of all available {@link String} buff types.
	 */
	public List<String> getSkillTypes()
	{
		final List<String> skillTypes = new ArrayList<>();
		for (BuffSkillHolder holder : _availableBuffs.values())
		{
			if (!skillTypes.contains(holder.getType()))
			{
				skillTypes.add(holder.getType());
			}
		}
		return skillTypes;
	}

	public BuffSkillHolder getAvailableBuff(L2Skill skill)
	{
		return _availableBuffs.get(skill);
	}

	public Map<L2Skill, BuffSkillHolder> getAvailableBuffs()
	{
		return _availableBuffs;
	}

	public static BufferManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final BufferManager INSTANCE = new BufferManager();
	}
}