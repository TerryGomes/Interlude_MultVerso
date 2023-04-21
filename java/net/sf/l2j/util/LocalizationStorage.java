package net.sf.l2j.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.l2j.commons.logging.CLogger;

public class LocalizationStorage
{
	private static final CLogger _log = new CLogger(LocalizationStorage.class.getName());

	private final static Path basedir = Path.of("./data/localization");
	private final static Path languageFile = Path.of("messages.properties");

	private final Map<String, Map<String, String>> languages = new HashMap<>();

	protected LocalizationStorage()
	{
		reload();
	}

	public String getString(String lang, String name)
	{
		return languages.get(lang).get(name);
	}

	public void reload()
	{
		try
		{
			languages.clear();
			Files.list(basedir).filter(Files::isDirectory).map(Path::getFileName).map(Path::toString).collect(Collectors.toMap(String::toString, this::readFromFile, (a, b) -> a, () -> languages));
		}
		catch (IOException e)
		{
			_log.error("Fail load localization", e);
		}
	}

	protected Map<String, String> readFromFile(String lang)
	{
		final Map<String, String> map = new HashMap<>();
		try
		{
			final var file = basedir.resolve(lang).resolve(languageFile);
			if (!Files.exists(file))
			{
				_log.error("not found {} for language {}", languageFile, lang);
				return Map.of();
			}

			Files.lines(file).map(x -> x.split("=", 2)).filter(x -> x.length == 2).forEach(x -> map.put(x[0], x[1]));
		}
		catch (IOException e)
		{
			_log.error("Fail loading {} vocabulary", e, lang);
		}
		return Map.copyOf(map);
	}

	public static LocalizationStorage getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final LocalizationStorage _instance = new LocalizationStorage();
	}
}