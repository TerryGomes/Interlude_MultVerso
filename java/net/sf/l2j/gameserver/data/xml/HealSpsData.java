package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.HealSps;
import net.sf.l2j.gameserver.skills.L2Skill;

import org.w3c.dom.Document;

/**
 * This class loads and stores {@link HealSps}s infos. Those informations are used for Heal calculation.
 */
public class HealSpsData implements IXmlReader
{
	private final LinkedList<HealSps> _healSps = new LinkedList<>();

	protected HealSpsData()
	{
		load();
	}

	@Override
	public void load()
	{
		parseFile("./data/xml/healSps.xml");
		LOGGER.info("Loaded {} healSps entries.", _healSps.size());
	}

	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "healSps", healSpsNode -> _healSps.add(new HealSps(parseAttributes(healSpsNode)))));
	}

	/**
	 * @param skill : The {@link L2Skill} to check.
	 * @param mAtk : The mAtk to check.
	 * @return The heal SPS correction based on casted {@link L2Skill} and caster's mAtk.
	 */
	public double calculateHealSps(L2Skill skill, int mAtk)
	{
		// Enforce skill id/level content first.
		HealSps healSps = _healSps.stream().filter(h -> h.getId() == skill.getId() && h.getValue() == skill.getLevel()).findFirst().orElse(null);

		// healSps is still null, check magic level content.
		if (healSps == null && skill.getMagicLevel() > 0)
		{
			healSps = _healSps.stream().filter(h -> h.getMagicLevel() <= skill.getMagicLevel()).max(Comparator.comparing(HealSps::getMagicLevel)).orElse(null);
		}

		// healSps couldn't be found ; return 0.
		if (healSps == null)
		{
			return 0.;
		}

		// Pick default correction value.
		double amount = healSps.getCorrection();

		// Calculate correction based on Matk.
		final int mAtkDiff = healSps.getNeededMatk() - mAtk;
		if (mAtkDiff <= 0)
		{
			return amount;
		}

		amount -= (mAtkDiff / 2);

		return amount;
	}

	public static HealSpsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final HealSpsData INSTANCE = new HealSpsData();
	}
}