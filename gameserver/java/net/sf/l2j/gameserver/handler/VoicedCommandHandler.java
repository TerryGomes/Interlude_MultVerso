package net.sf.l2j.gameserver.handler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.handler.voicedcommandhandlers.EventCommand;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Menu;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.OfflinePlayer;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Online;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.PremiumStatus;

public class VoicedCommandHandler
{
	private final Map<Integer, IVoicedCommandHandler> _entries = new HashMap<>();
	
	protected VoicedCommandHandler()
	{
		registerHandler(new Online());
		registerHandler(new Menu());
		registerHandler(new OfflinePlayer());
		registerHandler(new PremiumStatus());
		registerHandler(new EventCommand());
	}
	
	public void registerHandler(IVoicedCommandHandler handler)
	{
		String[] ids = handler.getVoicedCommandList();
		
		for (int i = 0; i < ids.length; i++)
		{
			_entries.put(ids[i].hashCode(), handler);
		}
	}
	
	public IVoicedCommandHandler getHandler(String voicedCommand)
	{
		String command = voicedCommand;
		
		if (voicedCommand.indexOf(" ") != -1)
		{
			command = voicedCommand.substring(0, voicedCommand.indexOf(" "));
		}
		
		return _entries.get(command.hashCode());
	}
	
	public void registerVoicedCommand(IVoicedCommandHandler voicedCommand)
	{
		Arrays.stream(voicedCommand.getVoicedCommandList()).forEach(v -> _entries.put(v.intern().hashCode(), voicedCommand));
	}
	
	public IVoicedCommandHandler getVoicedCommand(String voicedCommand)
	{
		return _entries.get(voicedCommand.hashCode());
	}
	
	public int size()
	{
		return _entries.size();
	}
	
	public static VoicedCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VoicedCommandHandler INSTANCE = new VoicedCommandHandler();
	}
}