package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.TeleportType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Teleport;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * This class loads and stores {@link Teleport}s used as regular teleport positions.
 */
public class TeleportData implements IXmlReader
{
	private final Map<Integer, List<Teleport>> _teleports = new HashMap<>();
	
	protected TeleportData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/teleports.xml");
		LOGGER.info("Loaded {} teleport positions.", _teleports.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "telPosList", telPosListNode ->
		{
			final NamedNodeMap telPosListAttrs = telPosListNode.getAttributes();
			final int npcId = Integer.parseInt(telPosListAttrs.getNamedItem("npcId").getNodeValue());
			
			final List<Teleport> teleports = new ArrayList<>();
			forEach(telPosListNode, "loc", locNode -> teleports.add(new Teleport(parseAttributes(locNode))));
			
			_teleports.put(npcId, teleports);
		}));
	}
	
	public void reload()
	{
		_teleports.clear();
		
		load();
	}
	
	public List<Teleport> getTeleports(int npcId)
	{
		return _teleports.get(npcId);
	}
	
	/**
	 * Build and send an HTM to a {@link Player}, based on {@link Npc}'s {@link Teleport}s and {@link TeleportType}.
	 * @param player : The {@link Player} to test.
	 * @param npc : The {@link Npc} to test.
	 * @param type : The {@link TeleportType} to filter.
	 */
	public void showTeleportList(Player player, Npc npc, TeleportType type)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>&$556;<br><br>");
		
		final List<Teleport> teleports = _teleports.get(npc.getNpcId());
		if (teleports != null)
		{
			for (int index = 0; index < teleports.size(); index++)
			{
				final Teleport teleport = teleports.get(index);
				if (teleport == null || type != teleport.getType())
					continue;
				
				StringUtil.append(sb, "<a action=\"bypass -h npc_", npc.getObjectId(), "_teleport ", index, "\" msg=\"811;", teleport.getDesc(), "\">", teleport.getDesc());
				
				if (!Config.FREE_TELEPORT)
				{
					final int priceCount = teleport.getCalculatedPriceCount(player);
					if (priceCount > 0)
						StringUtil.append(sb, " - ", priceCount, " ", ItemData.getInstance().getTemplate(teleport.getPriceId()).getName());
				}
				
				sb.append("</a><br1>");
			}
		}
		sb.append("</body></html>");
		
		html.setHtml(sb.toString());
		
		player.sendPacket(html);
	}
	
	public static TeleportData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TeleportData INSTANCE = new TeleportData();
	}
}