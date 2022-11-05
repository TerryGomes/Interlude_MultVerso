package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q510_AClansReputation extends Quest
{
	private static final String QUEST_NAME = "Q510_AClansReputation";
	
	// NPC
	private static final int VALDIS = 31331;
	
	// Quest Item
	private static final int TYRANNOSAURUS_CLAW = 8767;
	
	public Q510_AClansReputation()
	{
		super(510, "A Clan's Reputation");
		
		setItemsIds(TYRANNOSAURUS_CLAW);
		
		addStartNpc(VALDIS);
		addTalkId(VALDIS);
		
		addKillId(22215, 22216, 22217);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31331-3.htm"))
		{
			st.setState(QuestStatus.STARTED);
			st.setCond(1);
			playSound(player, SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31331-6.htm"))
		{
			playSound(player, SOUND_FINISH);
			st.exitQuest(true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case CREATED:
				htmltext = (!player.isClanLeader() || player.getClan().getLevel() < 5) ? "31331-0.htm" : "31331-1.htm";
				break;
			
			case STARTED:
				final int count = 50 * player.getInventory().getItemCount(TYRANNOSAURUS_CLAW);
				if (count > 0)
				{
					final Clan clan = player.getClan();
					
					htmltext = "31331-7.htm";
					takeItems(player, TYRANNOSAURUS_CLAW, -1);
					
					clan.addReputationScore(count);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_QUEST_COMPLETED_AND_S1_POINTS_GAINED).addNumber(count));
					clan.broadcastToMembers(new PledgeShowInfoUpdate(clan));
				}
				else
					htmltext = "31331-4.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();
		
		// Retrieve the qs of the clan leader.
		final QuestState st = getClanLeaderQuestState(player, npc);
		if (st == null || !st.isStarted())
			return null;
		
		dropItemsAlways(st.getPlayer(), TYRANNOSAURUS_CLAW, 1, 0);
		
		return null;
	}
}