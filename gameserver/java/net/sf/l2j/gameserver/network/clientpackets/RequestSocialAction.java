package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

public class RequestSocialAction extends L2GameClientPacket
{
	private int _actionId;
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().performAction(FloodProtector.SOCIAL))
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			return;
		}
		
		if (_actionId < 2 || _actionId > 13)
			return;
		
		if (player.isOperating() || player.getActiveRequester() != null || player.isAlikeDead() || player.getAI().getCurrentIntention().getType() != IntentionType.IDLE)
			return;
		
		player.broadcastPacket(new SocialAction(player, _actionId));
	}
}