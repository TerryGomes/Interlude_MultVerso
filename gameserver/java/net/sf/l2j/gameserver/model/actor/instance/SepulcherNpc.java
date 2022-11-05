package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Calendar;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.manager.FourSepulchersManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class SepulcherNpc extends Folk
{
	private static final String HTML_FILE_PATH = "data/html/sepulchers/";
	private static final int HALLS_KEY = 7260;
	
	public SepulcherNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onInteract(Player player)
	{
		if (isDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		switch (getNpcId())
		{
			case 31468:
			case 31469:
			case 31470:
			case 31471:
			case 31472:
			case 31473:
			case 31474:
			case 31475:
			case 31476:
			case 31477:
			case 31478:
			case 31479:
			case 31480:
			case 31481:
			case 31482:
			case 31483:
			case 31484:
			case 31485:
			case 31486:
			case 31487:
				// Time limit is reached. You can't open anymore Mysterious boxes after the 49th minute.
				if (Calendar.getInstance().get(Calendar.MINUTE) >= 50)
				{
					broadcastNpcSay("You can start at the scheduled time.");
					return;
				}
				FourSepulchersManager.getInstance().spawnMonster(getNpcId());
				deleteMe();
				break;
			
			case 31455:
			case 31456:
			case 31457:
			case 31458:
			case 31459:
			case 31460:
			case 31461:
			case 31462:
			case 31463:
			case 31464:
			case 31465:
			case 31466:
			case 31467:
				if (player.isInParty() && !player.getParty().isLeader(player))
					player = player.getParty().getLeader();
				
				player.addItem("Quest", HALLS_KEY, 1, player, true);
				
				deleteMe();
				break;
			
			default:
				super.onInteract(player);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return HTML_FILE_PATH + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("open_gate"))
		{
			final ItemInstance hallsKey = player.getInventory().getItemByItemId(HALLS_KEY);
			if (hallsKey == null)
				showHtmlFile(player, "Gatekeeper-no.htm");
			else if (FourSepulchersManager.getInstance().isAttackTime())
			{
				switch (getNpcId())
				{
					case 31929:
					case 31934:
					case 31939:
					case 31944:
						FourSepulchersManager.getInstance().spawnShadow(getNpcId());
						
					default:
						openNextDoor(getNpcId());
						
						final Party party = player.getParty();
						if (party != null)
						{
							for (Player member : player.getParty().getMembers())
							{
								final ItemInstance key = member.getInventory().getItemByItemId(HALLS_KEY);
								if (key != null)
									member.destroyItemByItemId("Quest", HALLS_KEY, key.getCount(), member, true);
							}
						}
						else
							player.destroyItemByItemId("Quest", HALLS_KEY, hallsKey.getCount(), player, true);
				}
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	public void openNextDoor(int npcId)
	{
		final int doorId = FourSepulchersManager.getInstance().getHallGateKeepers().get(npcId);
		final Door door = DoorData.getInstance().getDoor(doorId);
		
		// Open the door.
		door.openMe();
		
		// Schedule the automatic door close.
		ThreadPool.schedule(() -> door.closeMe(), 10000);
		
		// Spawn the next mysterious box.
		FourSepulchersManager.getInstance().spawnMysteriousBox(npcId);
		
		sayInShout("The monsters have spawned!");
	}
	
	public void sayInShout(String msg)
	{
		if (msg == null || msg.isEmpty())
			return;
		
		broadcastNpcShout(msg);
	}
	
	public void showHtmlFile(Player player, String file)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/sepulchers/" + file);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}