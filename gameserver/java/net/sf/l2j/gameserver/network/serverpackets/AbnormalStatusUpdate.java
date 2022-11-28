package net.sf.l2j.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.l2j.gameserver.model.holder.EffectHolder;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AbnormalStatusUpdate extends L2GameServerPacket
{
	private final List<EffectHolder> _effects = new ArrayList<>();
	private final Set<EffectHolder> _toggles = new TreeSet<>(Comparator.comparing(EffectHolder::getId));

	public AbnormalStatusUpdate()
	{
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x7f);

		writeH(_effects.size() + _toggles.size());

		for (EffectHolder holder : _effects)
		{
			writeD(holder.getId());
			writeH(holder.getValue());
			writeD((holder.getDuration() == -1) ? -1 : holder.getDuration() / 1000);
		}

		for (EffectHolder holder : _toggles)
		{
			writeD(holder.getId());
			writeH(holder.getValue());
			writeD(-1);
		}
	}

	public void addEffect(L2Skill skill, int duration)
	{
		final EffectHolder eh = new EffectHolder(skill, duration);

		if (skill.isToggle())
		{
			_toggles.add(eh);
		}
		else
		{
			_effects.add(eh);
		}
	}
}