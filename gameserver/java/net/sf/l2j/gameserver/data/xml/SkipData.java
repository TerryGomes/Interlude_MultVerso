package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;

public class SkipData implements IXmlReader
{
	private static final Logger _log = Logger.getLogger(SkipData.class.getName());

	private static final List<Integer> _skip = new ArrayList<>();

	public SkipData()
	{
		load();
	}

	@Override
	public void load()
	{
		parseFile("./data/xml/skippingItems.xml");
	}

	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "item", itemNode ->
		{
			final StatSet set = parseAttributes(itemNode);
			int itemId = set.getInteger("id");
			_skip.add(itemId);
		}));

		_log.info("Loaded " + _skip.size() + " skipping item(s).");
	}

	public static boolean isSkipped(int itemId)
	{
		return _skip.contains(itemId);
	}

	public static SkipData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final SkipData INSTANCE = new SkipData();
	}
}