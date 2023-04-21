package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;

public class Victim extends AttackableAIScript
{
	private static final int[] VICTIMS = new int[]
	{
		18150, // r31_mission_roomboss1
		18151, // r31_mission_roomboss2

		18152, // r32_mission_roomboss1
		18153, // r32_mission_roomboss2

		18154, // r33_mission_roomboss1
		18155, // r33_mission_roomboss2

		18156, // r34_mission_roomboss1
		18157, // r34_mission_roomboss2
	};

	public Victim()
	{
		super("ai/individual");
	}

	@Override
	protected void registerNpcs()
	{
		addNoDesire(VICTIMS);
	}

	@Override
	public void onNoDesire(Npc npc)
	{
	}
}