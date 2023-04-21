package net.sf.l2j.gameserver.scripting.script.event;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;

public class HeavyMedal extends ScheduledQuest
{
	private final int EVENT_MEDAL_COUNT = Config.EVENT_MEDAL_COUNT;
	private final int EVENT_MEDAL_CHANCE = Config.EVENT_MEDAL_CHANCE;

	private final int GLITTERING_MEDAL_COUNT = Config.GLITTERING_MEDAL_COUNT;
	private final int GLITTERING_MEDAL_CHANCE = Config.GLITTERING_MEDAL_CHANCE;

	private final static int CAT_ROY = 31228;
	private final static int CAT_WINNIE = 31229;

	private final static int MEDAL = 6392;
	private final static int GLITTERING_MEDAL = 6393;

	private final static int WIN_CHANCE = 50;

	private final static int[] MEDALS =
	{
		5,
		10,
		20,
		40
	};
	private final static int[] BADGES =
	{
		6399,
		6400,
		6401,
		6402
	};

	public HeavyMedal()
	{
		super(-1, "events");

		addQuestStart(CAT_ROY, CAT_WINNIE);
		addTalkId(CAT_ROY, CAT_WINNIE);
		addFirstTalkId(CAT_ROY, CAT_WINNIE);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		int level = checkLevel(player);

		if (event.equalsIgnoreCase("game"))
		{
			if (player.getInventory().getItemCount(GLITTERING_MEDAL) < MEDALS[level])
			{
				return "31229-no.htm";
			}

			return "31229-game.htm";
		}
		else if (event.equalsIgnoreCase("heads") || event.equalsIgnoreCase("tails"))
		{
			if (player.getInventory().getItemCount(GLITTERING_MEDAL) < MEDALS[level])
			{
				return "31229-" + event.toLowerCase() + "-10.htm";
			}

			if (level < 4)
			{
				takeItems(player, GLITTERING_MEDAL, MEDALS[level]);

				if (Rnd.get(100) >= WIN_CHANCE)
				{
					level = 0;
				}
				else
				{
					if (level > 0)
					{
						takeItems(player, BADGES[level - 1], -1);
					}
					giveItems(player, BADGES[level], 1);
					playSound(player, SOUND_ITEMGET);
					level++;
				}
				return "31229-" + event.toLowerCase() + "-" + String.valueOf(level) + ".htm";
			}
		}
		else if (event.equalsIgnoreCase("talk"))
		{
			return String.valueOf(npc.getNpcId()) + "-lvl-" + String.valueOf(level) + ".htm";
		}

		return htmltext;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestList().getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		return npc.getNpcId() + ".htm";
	}

	public int checkLevel(Player player)
	{
		int level = 0;

		if (player.getInventory().getItemCount(6402) > 0)
		{
			level = 4;
		}
		else if (player.getInventory().getItemCount(6401) > 0)
		{
			level = 3;
		}
		else if (player.getInventory().getItemCount(6400) > 0)
		{
			level = 2;
		}
		else if (player.getInventory().getItemCount(6399) > 0)
		{
			level = 1;
		}

		return level;
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();

		if (npc instanceof Monster)
		{
			final Monster mob = (Monster) npc;
			if (mob.isRaidBoss() || (player.getStatus().getLevel() - mob.getStatus().getLevel() > 8))
			{
				return;
			}

			if (Rnd.get(100) < EVENT_MEDAL_CHANCE)
			{
				mob.dropOrAutoLootItem(player, new IntIntHolder(MEDAL, EVENT_MEDAL_COUNT));
			}

			if (Rnd.get(100) < GLITTERING_MEDAL_CHANCE)
			{
				mob.dropOrAutoLootItem(player, new IntIntHolder(GLITTERING_MEDAL, GLITTERING_MEDAL_COUNT));
			}
		}
		return;
	}

	@Override
	protected void onStart()
	{
		for (final NpcTemplate template : NpcData.getInstance().getAllNpcs())
		{
			if (!template.isType("Monster"))
			{
				continue;
			}

			try
			{
				if (Attackable.class.isAssignableFrom(Class.forName("net.sf.l2j.gameserver.model.actor.instance." + template.getType())))
				{
					addEventId(template.getNpcId(), EventHandler.MY_DYING);
				}
			}
			catch (ClassNotFoundException e)
			{
				LOGGER.error("An unknown template type {} has been found on {}.", e, template.getType(), toString());
			}
		}
	}

	@Override
	protected void onEnd()
	{
	}
}