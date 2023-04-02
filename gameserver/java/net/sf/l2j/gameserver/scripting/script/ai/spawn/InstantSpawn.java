package net.sf.l2j.gameserver.scripting.script.ai.spawn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * When all NPCs dies inside registered makers, immediately respawns:<br>
 * SELF = all dead NPCs the maker (itself)<br>
 * RANDOM = NPCs in randomly picked maker, the maker itself is blocked<br>
 * SERIAL = self, if counter is not cleared, another maker, if counter is cleared
 */
public class InstantSpawn extends Quest
{
	// Data container for self instant spawn.
	private static final List<String> SELF = new ArrayList<>();
	{
		SELF.add("gludio16_1823_08m1");
		SELF.add("gludio16_1823_11m1");
		SELF.add("gludio16_1823_17m2");
		SELF.add("gludio16_1823_18m1");
		SELF.add("gludio16_1823_22m2");
		SELF.add("gludio16_1823_31m2");
		SELF.add("gludio16_1823_32m2");
		SELF.add("gludio05_1923_03m1");
		SELF.add("gludio05_1923_07m1");
		SELF.add("gludio05_1923_10m1");
		SELF.add("gludio05_1923_13m1");
		SELF.add("gludio05_1923_15m2");
		SELF.add("gludio05_1923_17m1");
		SELF.add("gludio05_1923_19m1");
		SELF.add("gludio05_1923_21m1");
		SELF.add("gludio05_1923_24m1");
		SELF.add("gludio05_1923_24m2");
		SELF.add("gludio05_1923_27m1");
		SELF.add("gludio05_1923_30m1");
		SELF.add("gludio05_1923_36m1");
		SELF.add("gludio05_1923_42m2");
		SELF.add("gludio05_1923_44m1");
		SELF.add("gludio05_1923_46m1");
		SELF.add("gludio05_1923_49m1");
		SELF.add("gludio05_1923_51m1");
		SELF.add("gludio05_1923_56m1");
		SELF.add("gludio05_1923_58m1");
		SELF.add("gludio05_1923_58m2");
		SELF.add("gludio05_1923_61m1");
		SELF.add("gludio05_1923_61m3");
		SELF.add("gludio05_1923_65m1");
		SELF.add("gludio05_1923_65m2");
		SELF.add("gludio05_1923_70m1");
		SELF.add("gludio05_1923_70m2");
		SELF.add("gludio05_1923_74m1");
		SELF.add("gludio05_1923_79m1");
		SELF.add("gludio05_1923_79m2");
		SELF.add("gludio05_1923_80m1");
		SELF.add("gludio05_1923_80m2");
		SELF.add("gludio05_1923_101m1");
		SELF.add("gludio05_1923_101m2");
		SELF.add("gludio05_1923_102m2");
		SELF.add("gludio05_1923_104m1");
		SELF.add("gludio05_1923_104m2");
		SELF.add("gludio05_1923_110m1");
		SELF.add("gludio05_1923_110m3");
		SELF.add("gludio05_1923_111m1");
		SELF.add("gludio05_1923_116m1");
		SELF.add("gludio05_1923_117m1");
		SELF.add("gludio05_1923_117m3");
		SELF.add("gludio05_1923_128m1");
		SELF.add("gludio05_1923_128m3");
		SELF.add("gludio05_1923_133m2");
	}

	// Data container for serial instant spawn (7x one maker, 1x another maker, 7x one maker, etc, etc).
	private static final Map<String, SerialData> SERIAL = new HashMap<>();
	{
		SERIAL.put("godard14_12_01", new SerialData(7, "iss_godard14_12_02"));
		SERIAL.put("godard14_12_02", new SerialData(1, "iss_godard14_12_01"));
		SERIAL.put("godard14_13_01", new SerialData(7, "iss_godard14_13_02"));
		SERIAL.put("godard14_13_02", new SerialData(1, "iss_godard14_13_01"));
		SERIAL.put("godard14_14_01", new SerialData(7, "iss_godard14_14_02"));
		SERIAL.put("godard14_14_02", new SerialData(1, "iss_godard14_14_01"));
	}

	// Data container for random instant spawn (1st maker, 2nd maker, 1st maker, 3rd maker, etc, etc).
	private static final Map<String, RandomData> RANDOM = new HashMap<>();
	{
		RANDOM.put("t21_24_00101", new RandomData("isr_t21_24_00101", "isr_t21_24_00102"));
		RANDOM.put("t21_24_00102", new RandomData("isr_t21_24_00101", "isr_t21_24_00102"));
		RANDOM.put("t21_24_00103", new RandomData("isr_t21_24_00103", "isr_t21_24_00104"));
		RANDOM.put("t21_24_00104", new RandomData("isr_t21_24_00103", "isr_t21_24_00104"));
		RANDOM.put("t21_24_00105", new RandomData("isr_t21_24_00105", "isr_t21_24_00106"));
		RANDOM.put("t21_24_00106", new RandomData("isr_t21_24_00105", "isr_t21_24_00106"));
		RANDOM.put("t21_24_00107", new RandomData("isr_t21_24_00107", "isr_t21_24_00108"));
		RANDOM.put("t21_24_00108", new RandomData("isr_t21_24_00107", "isr_t21_24_00108"));
		RANDOM.put("t21_24_00201", new RandomData("isr_t21_24_00201", "isr_t21_24_00202"));
		RANDOM.put("t21_24_00202", new RandomData("isr_t21_24_00201", "isr_t21_24_00202"));
		RANDOM.put("t21_24_00301", new RandomData("isr_t21_24_00301", "isr_t21_24_00302"));
		RANDOM.put("t21_24_00302", new RandomData("isr_t21_24_00301", "isr_t21_24_00302"));
		RANDOM.put("t21_24_00303", new RandomData("isr_t21_24_00303", "isr_t21_24_00304"));
		RANDOM.put("t21_24_00304", new RandomData("isr_t21_24_00303", "isr_t21_24_00304"));
		RANDOM.put("t21_24_00401", new RandomData("isr_t21_24_00401", "isr_t21_24_00402"));
		RANDOM.put("t21_24_00402", new RandomData("isr_t21_24_00401", "isr_t21_24_00402"));
		RANDOM.put("t21_24_00601", new RandomData("isr_t21_24_00601", "isr_t21_24_00602"));
		RANDOM.put("t21_24_00602", new RandomData("isr_t21_24_00601", "isr_t21_24_00602"));
		RANDOM.put("t21_24_00603", new RandomData("isr_t21_24_00603", "isr_t21_24_00604"));
		RANDOM.put("t21_24_00604", new RandomData("isr_t21_24_00603", "isr_t21_24_00604"));
		RANDOM.put("t21_24_00701", new RandomData("isr_t21_24_00701", "isr_t21_24_00702"));
		RANDOM.put("t21_24_00702", new RandomData("isr_t21_24_00701", "isr_t21_24_00702"));
		RANDOM.put("t21_24_00801", new RandomData("isr_t21_24_00801", "isr_t21_24_00802"));
		RANDOM.put("t21_24_00802", new RandomData("isr_t21_24_00801", "isr_t21_24_00802"));
		RANDOM.put("t21_24_00803", new RandomData("isr_t21_24_00803", "isr_t21_24_00804"));
		RANDOM.put("t21_24_00804", new RandomData("isr_t21_24_00803", "isr_t21_24_00804"));
		RANDOM.put("t21_24_00901", new RandomData("isr_t21_24_00901", "isr_t21_24_00902"));
		RANDOM.put("t21_24_00902", new RandomData("isr_t21_24_00901", "isr_t21_24_00902"));
		RANDOM.put("t21_24_00903", new RandomData("isr_t21_24_00903", "isr_t21_24_00904"));
		RANDOM.put("t21_24_00904", new RandomData("isr_t21_24_00903", "isr_t21_24_00904"));
		RANDOM.put("t21_24_01001", new RandomData("isr_t21_24_01001", "isr_t21_24_01002"));
		RANDOM.put("t21_24_01002", new RandomData("isr_t21_24_01001", "isr_t21_24_01002"));
		RANDOM.put("t21_24_01101", new RandomData("isr_t21_24_01101", "isr_t21_24_01102"));
		RANDOM.put("t21_24_01102", new RandomData("isr_t21_24_01101", "isr_t21_24_01102"));
		RANDOM.put("t21_24_01103", new RandomData("isr_t21_24_01103", "isr_t21_24_01104"));
		RANDOM.put("t21_24_01104", new RandomData("isr_t21_24_01103", "isr_t21_24_01104"));
		RANDOM.put("t21_24_01201", new RandomData("isr_t21_24_01201", "isr_t21_24_01202"));
		RANDOM.put("t21_24_01202", new RandomData("isr_t21_24_01201", "isr_t21_24_01202"));
		RANDOM.put("t21_24_01301", new RandomData("isr_t21_24_01301", "isr_t21_24_01302"));
		RANDOM.put("t21_24_01302", new RandomData("isr_t21_24_01301", "isr_t21_24_01302"));
		RANDOM.put("t21_24_01303", new RandomData("isr_t21_24_01303", "isr_t21_24_01304"));
		RANDOM.put("t21_24_01304", new RandomData("isr_t21_24_01303", "isr_t21_24_01304"));
		RANDOM.put("t21_24_01305", new RandomData("isr_t21_24_01305", "isr_t21_24_01306"));
		RANDOM.put("t21_24_01306", new RandomData("isr_t21_24_01305", "isr_t21_24_01306"));
		RANDOM.put("t21_24_01307", new RandomData("isr_t21_24_01307", "isr_t21_24_01308"));
		RANDOM.put("t21_24_01308", new RandomData("isr_t21_24_01307", "isr_t21_24_01308"));
		RANDOM.put("t21_24_01401", new RandomData("isr_t21_24_01401", "isr_t21_24_01402"));
		RANDOM.put("t21_24_01402", new RandomData("isr_t21_24_01401", "isr_t21_24_01402"));
		RANDOM.put("t21_24_01403", new RandomData("isr_t21_24_01403", "isr_t21_24_01404"));
		RANDOM.put("t21_24_01404", new RandomData("isr_t21_24_01403", "isr_t21_24_01404"));
		RANDOM.put("t21_24_01501", new RandomData("isr_t21_24_01501", "isr_t21_24_01502"));
		RANDOM.put("t21_24_01502", new RandomData("isr_t21_24_01501", "isr_t21_24_01502"));
		RANDOM.put("t21_24_01503", new RandomData("isr_t21_24_01503", "isr_t21_24_01504"));
		RANDOM.put("t21_24_01504", new RandomData("isr_t21_24_01503", "isr_t21_24_01504"));
		RANDOM.put("t21_24_01505", new RandomData("isr_t21_24_01505", "isr_t21_24_01506"));
		RANDOM.put("t21_24_01506", new RandomData("isr_t21_24_01505", "isr_t21_24_01506"));
		RANDOM.put("t21_24_01507", new RandomData("isr_t21_24_01507", "isr_t21_24_01508"));
		RANDOM.put("t21_24_01508", new RandomData("isr_t21_24_01507", "isr_t21_24_01508"));
		RANDOM.put("t21_24_01601", new RandomData("isr_t21_24_01601", "isr_t21_24_01602"));
		RANDOM.put("t21_24_01602", new RandomData("isr_t21_24_01601", "isr_t21_24_01602"));
		RANDOM.put("t21_24_01701", new RandomData("isr_t21_24_01701", "isr_t21_24_01702"));
		RANDOM.put("t21_24_01702", new RandomData("isr_t21_24_01701", "isr_t21_24_01702"));
		RANDOM.put("t21_24_01703", new RandomData("isr_t21_24_01703", "isr_t21_24_01704"));
		RANDOM.put("t21_24_01704", new RandomData("isr_t21_24_01703", "isr_t21_24_01704"));
		RANDOM.put("t21_24_01705", new RandomData("isr_t21_24_01705", "isr_t21_24_01706"));
		RANDOM.put("t21_24_01706", new RandomData("isr_t21_24_01705", "isr_t21_24_01706"));
		RANDOM.put("t21_24_01707", new RandomData("isr_t21_24_01707", "isr_t21_24_01708"));
		RANDOM.put("t21_24_01708", new RandomData("isr_t21_24_01707", "isr_t21_24_01708"));
		RANDOM.put("t21_24_01709", new RandomData("isr_t21_24_01709", "isr_t21_24_01710"));
		RANDOM.put("t21_24_01710", new RandomData("isr_t21_24_01709", "isr_t21_24_01710"));
		RANDOM.put("t21_24_01801", new RandomData("isr_t21_24_01801", "isr_t21_24_01802"));
		RANDOM.put("t21_24_01802", new RandomData("isr_t21_24_01801", "isr_t21_24_01802"));
		RANDOM.put("t21_24_01803", new RandomData("isr_t21_24_01803", "isr_t21_24_01804"));
		RANDOM.put("t21_24_01804", new RandomData("isr_t21_24_01803", "isr_t21_24_01804"));
		RANDOM.put("t21_24_01805", new RandomData("isr_t21_24_01805", "isr_t21_24_01806"));
		RANDOM.put("t21_24_01806", new RandomData("isr_t21_24_01805", "isr_t21_24_01806"));
		RANDOM.put("t21_24_01807", new RandomData("isr_t21_24_01807", "isr_t21_24_01808"));
		RANDOM.put("t21_24_01808", new RandomData("isr_t21_24_01807", "isr_t21_24_01808"));
		RANDOM.put("t21_24_01809", new RandomData("isr_t21_24_01809", "isr_t21_24_01810"));
		RANDOM.put("t21_24_01810", new RandomData("isr_t21_24_01809", "isr_t21_24_01810"));
		RANDOM.put("t21_24_01901", new RandomData("isr_t21_24_01901", "isr_t21_24_01902"));
		RANDOM.put("t21_24_01902", new RandomData("isr_t21_24_01901", "isr_t21_24_01902"));
		RANDOM.put("t21_24_01903", new RandomData("isr_t21_24_01903", "isr_t21_24_01904"));
		RANDOM.put("t21_24_01904", new RandomData("isr_t21_24_01903", "isr_t21_24_01904"));
		RANDOM.put("t21_24_02001", new RandomData("isr_t21_24_02001", "isr_t21_24_02002"));
		RANDOM.put("t21_24_02002", new RandomData("isr_t21_24_02001", "isr_t21_24_02002"));
		RANDOM.put("t21_24_02101", new RandomData("isr_t21_24_02101", "isr_t21_24_02102"));
		RANDOM.put("t21_24_02102", new RandomData("isr_t21_24_02101", "isr_t21_24_02102"));
		RANDOM.put("t21_24_02201", new RandomData("isr_t21_24_02201", "isr_t21_24_02202"));
		RANDOM.put("t21_24_02202", new RandomData("isr_t21_24_02201", "isr_t21_24_02202"));
		RANDOM.put("t21_24_02203", new RandomData("isr_t21_24_02203", "isr_t21_24_02204"));
		RANDOM.put("t21_24_02204", new RandomData("isr_t21_24_02203", "isr_t21_24_02204"));
		RANDOM.put("t21_24_02301", new RandomData("isr_t21_24_02301", "isr_t21_24_02302"));
		RANDOM.put("t21_24_02302", new RandomData("isr_t21_24_02301", "isr_t21_24_02302"));
		RANDOM.put("t21_24_02303", new RandomData("isr_t21_24_02303", "isr_t21_24_02304"));
		RANDOM.put("t21_24_02304", new RandomData("isr_t21_24_02303", "isr_t21_24_02304"));
		RANDOM.put("t21_24_02401", new RandomData("isr_t21_24_02401", "isr_t21_24_02402"));
		RANDOM.put("t21_24_02402", new RandomData("isr_t21_24_02401", "isr_t21_24_02402"));
		RANDOM.put("t21_24_02501", new RandomData("isr_t21_24_02501", "isr_t21_24_02502"));
		RANDOM.put("t21_24_02502", new RandomData("isr_t21_24_02501", "isr_t21_24_02502"));
		RANDOM.put("t21_24_02503", new RandomData("isr_t21_24_02503", "isr_t21_24_02504"));
		RANDOM.put("t21_24_02504", new RandomData("isr_t21_24_02503", "isr_t21_24_02504"));
		RANDOM.put("t21_24_02601", new RandomData("isr_t21_24_02601", "isr_t21_24_02602"));
		RANDOM.put("t21_24_02602", new RandomData("isr_t21_24_02601", "isr_t21_24_02602"));
		RANDOM.put("t21_24_02603", new RandomData("isr_t21_24_02603", "isr_t21_24_02604"));
		RANDOM.put("t21_24_02604", new RandomData("isr_t21_24_02603", "isr_t21_24_02604"));
		RANDOM.put("t21_24_02605", new RandomData("isr_t21_24_02605", "isr_t21_24_02606"));
		RANDOM.put("t21_24_02606", new RandomData("isr_t21_24_02605", "isr_t21_24_02606"));
		RANDOM.put("t21_24_02607", new RandomData("isr_t21_24_02607", "isr_t21_24_02608"));
		RANDOM.put("t21_24_02608", new RandomData("isr_t21_24_02607", "isr_t21_24_02608"));
		RANDOM.put("t21_24_02701", new RandomData("isr_t21_24_02701", "isr_t21_24_02702"));
		RANDOM.put("t21_24_02702", new RandomData("isr_t21_24_02701", "isr_t21_24_02702"));
		RANDOM.put("t21_24_02801", new RandomData("isr_t21_24_02801", "isr_t21_24_02802"));
		RANDOM.put("t21_24_02802", new RandomData("isr_t21_24_02801", "isr_t21_24_02802"));
		RANDOM.put("t21_24_02901", new RandomData("isr_t21_24_02901", "isr_t21_24_02902"));
		RANDOM.put("t21_24_02902", new RandomData("isr_t21_24_02901", "isr_t21_24_02902"));
		RANDOM.put("t21_24_02903", new RandomData("isr_t21_24_02903", "isr_t21_24_02904"));
		RANDOM.put("t21_24_02904", new RandomData("isr_t21_24_02903", "isr_t21_24_02904"));
		RANDOM.put("t21_24_02905", new RandomData("isr_t21_24_02905", "isr_t21_24_02906"));
		RANDOM.put("t21_24_02906", new RandomData("isr_t21_24_02905", "isr_t21_24_02906"));
		RANDOM.put("t21_24_03001", new RandomData("isr_t21_24_03001", "isr_t21_24_03002"));
		RANDOM.put("t21_24_03002", new RandomData("isr_t21_24_03001", "isr_t21_24_03002"));
		RANDOM.put("t21_24_03003", new RandomData("isr_t21_24_03003", "isr_t21_24_03004"));
		RANDOM.put("t21_24_03004", new RandomData("isr_t21_24_03003", "isr_t21_24_03004"));
		RANDOM.put("t21_24_03101", new RandomData("isr_t21_24_03101", "isr_t21_24_03102"));
		RANDOM.put("t21_24_03102", new RandomData("isr_t21_24_03101", "isr_t21_24_03102"));
		RANDOM.put("t21_24_03201", new RandomData("isr_t21_24_03201", "isr_t21_24_03202"));
		RANDOM.put("t21_24_03202", new RandomData("isr_t21_24_03201", "isr_t21_24_03202"));
		RANDOM.put("t21_24_03203", new RandomData("isr_t21_24_03203", "isr_t21_24_03204"));
		RANDOM.put("t21_24_03204", new RandomData("isr_t21_24_03203", "isr_t21_24_03204"));
		RANDOM.put("t21_24_03301", new RandomData("isr_t21_24_03301", "isr_t21_24_03302"));
		RANDOM.put("t21_24_03302", new RandomData("isr_t21_24_03301", "isr_t21_24_03302"));
		RANDOM.put("t21_24_03401", new RandomData("isr_t21_24_03401", "isr_t21_24_03402"));
		RANDOM.put("t21_24_03402", new RandomData("isr_t21_24_03401", "isr_t21_24_03402"));
		RANDOM.put("t21_24_03501", new RandomData("isr_t21_24_03501", "isr_t21_24_03502"));
		RANDOM.put("t21_24_03502", new RandomData("isr_t21_24_03501", "isr_t21_24_03502"));
		RANDOM.put("t21_24_03601", new RandomData("isr_t21_24_03601", "isr_t21_24_03602"));
		RANDOM.put("t21_24_03602", new RandomData("isr_t21_24_03601", "isr_t21_24_03602"));
		RANDOM.put("t21_24_03603", new RandomData("isr_t21_24_03603", "isr_t21_24_03604"));
		RANDOM.put("t21_24_03604", new RandomData("isr_t21_24_03603", "isr_t21_24_03604"));
		RANDOM.put("t21_24_03701", new RandomData("isr_t21_24_03701", "isr_t21_24_03702"));
		RANDOM.put("t21_24_03702", new RandomData("isr_t21_24_03701", "isr_t21_24_03702"));
		RANDOM.put("t21_24_03703", new RandomData("isr_t21_24_03703", "isr_t21_24_03704"));
		RANDOM.put("t21_24_03704", new RandomData("isr_t21_24_03703", "isr_t21_24_03704"));
		RANDOM.put("t21_24_03801", new RandomData("isr_t21_24_03801", "isr_t21_24_03802"));
		RANDOM.put("t21_24_03802", new RandomData("isr_t21_24_03801", "isr_t21_24_03802"));
		RANDOM.put("t21_24_03901", new RandomData("isr_t21_24_03901", "isr_t21_24_03902"));
		RANDOM.put("t21_24_03902", new RandomData("isr_t21_24_03901", "isr_t21_24_03902"));
		RANDOM.put("t21_24_03903", new RandomData("isr_t21_24_03903", "isr_t21_24_03904"));
		RANDOM.put("t21_24_03904", new RandomData("isr_t21_24_03903", "isr_t21_24_03904"));
		RANDOM.put("t21_24_04001", new RandomData("isr_t21_24_04001", "isr_t21_24_04002"));
		RANDOM.put("t21_24_04002", new RandomData("isr_t21_24_04001", "isr_t21_24_04002"));
		RANDOM.put("t21_24_04003", new RandomData("isr_t21_24_04003", "isr_t21_24_04004"));
		RANDOM.put("t21_24_04004", new RandomData("isr_t21_24_04003", "isr_t21_24_04004"));
		RANDOM.put("t21_24_04101", new RandomData("isr_t21_24_04101", "isr_t21_24_04102"));
		RANDOM.put("t21_24_04102", new RandomData("isr_t21_24_04101", "isr_t21_24_04102"));
		RANDOM.put("t21_24_04103", new RandomData("isr_t21_24_04103", "isr_t21_24_04104"));
		RANDOM.put("t21_24_04104", new RandomData("isr_t21_24_04103", "isr_t21_24_04104"));
		RANDOM.put("t21_24_04201", new RandomData("isr_t21_24_04201", "isr_t21_24_04202"));
		RANDOM.put("t21_24_04202", new RandomData("isr_t21_24_04201", "isr_t21_24_04202"));
		RANDOM.put("t21_24_04203", new RandomData("isr_t21_24_04203", "isr_t21_24_04204"));
		RANDOM.put("t21_24_04204", new RandomData("isr_t21_24_04203", "isr_t21_24_04204"));
		RANDOM.put("t21_24_04301", new RandomData("isr_t21_24_04301", "isr_t21_24_04302"));
		RANDOM.put("t21_24_04302", new RandomData("isr_t21_24_04301", "isr_t21_24_04302"));
		RANDOM.put("t21_24_04303", new RandomData("isr_t21_24_04303", "isr_t21_24_04304"));
		RANDOM.put("t21_24_04304", new RandomData("isr_t21_24_04303", "isr_t21_24_04304"));
		RANDOM.put("t21_24_04401", new RandomData("isr_t21_24_04401", "isr_t21_24_04402"));
		RANDOM.put("t21_24_04402", new RandomData("isr_t21_24_04401", "isr_t21_24_04402"));
		RANDOM.put("t21_24_04403", new RandomData("isr_t21_24_04403", "isr_t21_24_04404"));
		RANDOM.put("t21_24_04404", new RandomData("isr_t21_24_04403", "isr_t21_24_04404"));
		RANDOM.put("t21_24_04405", new RandomData("isr_t21_24_04405", "isr_t21_24_04406"));
		RANDOM.put("t21_24_04406", new RandomData("isr_t21_24_04405", "isr_t21_24_04406"));
		RANDOM.put("t21_24_04501", new RandomData("isr_t21_24_04501", "isr_t21_24_04502"));
		RANDOM.put("t21_24_04502", new RandomData("isr_t21_24_04501", "isr_t21_24_04502"));
		RANDOM.put("t21_24_04503", new RandomData("isr_t21_24_04503", "isr_t21_24_04504"));
		RANDOM.put("t21_24_04504", new RandomData("isr_t21_24_04503", "isr_t21_24_04504"));
		RANDOM.put("t21_24_04505", new RandomData("isr_t21_24_04505", "isr_t21_24_04506"));
		RANDOM.put("t21_24_04506", new RandomData("isr_t21_24_04505", "isr_t21_24_04506"));
		RANDOM.put("t21_24_04601", new RandomData("isr_t21_24_04601", "isr_t21_24_04602"));
		RANDOM.put("t21_24_04602", new RandomData("isr_t21_24_04601", "isr_t21_24_04602"));
		RANDOM.put("t21_24_04603", new RandomData("isr_t21_24_04603", "isr_t21_24_04604"));
		RANDOM.put("t21_24_04604", new RandomData("isr_t21_24_04603", "isr_t21_24_04604"));
		RANDOM.put("t21_24_04701", new RandomData("isr_t21_24_04701", "isr_t21_24_04702"));
		RANDOM.put("t21_24_04702", new RandomData("isr_t21_24_04701", "isr_t21_24_04702"));
		RANDOM.put("t21_24_04703", new RandomData("isr_t21_24_04703", "isr_t21_24_04704"));
		RANDOM.put("t21_24_04704", new RandomData("isr_t21_24_04703", "isr_t21_24_04704"));
		RANDOM.put("t21_24_04801", new RandomData("isr_t21_24_04801", "isr_t21_24_04802"));
		RANDOM.put("t21_24_04802", new RandomData("isr_t21_24_04801", "isr_t21_24_04802"));
		RANDOM.put("t21_24_04803", new RandomData("isr_t21_24_04803", "isr_t21_24_04804"));
		RANDOM.put("t21_24_04804", new RandomData("isr_t21_24_04803", "isr_t21_24_04804"));
		RANDOM.put("t21_24_05101", new RandomData("isr_t21_24_05101", "isr_t21_24_05102"));
		RANDOM.put("t21_24_05102", new RandomData("isr_t21_24_05101", "isr_t21_24_05102"));
		RANDOM.put("t21_24_05103", new RandomData("isr_t21_24_05103", "isr_t21_24_05104"));
		RANDOM.put("t21_24_05104", new RandomData("isr_t21_24_05103", "isr_t21_24_05104"));
		RANDOM.put("t21_24_05201", new RandomData("isr_t21_24_05201", "isr_t21_24_05202"));
		RANDOM.put("t21_24_05202", new RandomData("isr_t21_24_05201", "isr_t21_24_05202"));
		RANDOM.put("t21_24_05203", new RandomData("isr_t21_24_05203", "isr_t21_24_05204"));
		RANDOM.put("t21_24_05204", new RandomData("isr_t21_24_05203", "isr_t21_24_05204"));
		RANDOM.put("t21_24_05301", new RandomData("isr_t21_24_05301", "isr_t21_24_05302"));
		RANDOM.put("t21_24_05302", new RandomData("isr_t21_24_05301", "isr_t21_24_05302"));
		RANDOM.put("t21_24_05303", new RandomData("isr_t21_24_05303", "isr_t21_24_05304"));
		RANDOM.put("t21_24_05304", new RandomData("isr_t21_24_05303", "isr_t21_24_05304"));
		RANDOM.put("t21_24_05401", new RandomData("isr_t21_24_05401", "isr_t21_24_05402"));
		RANDOM.put("t21_24_05402", new RandomData("isr_t21_24_05401", "isr_t21_24_05402"));
		RANDOM.put("t21_24_05403", new RandomData("isr_t21_24_05403", "isr_t21_24_05404"));
		RANDOM.put("t21_24_05404", new RandomData("isr_t21_24_05403", "isr_t21_24_05404"));
		RANDOM.put("t21_24_05501", new RandomData("isr_t21_24_05501", "isr_t21_24_05502"));
		RANDOM.put("t21_24_05502", new RandomData("isr_t21_24_05501", "isr_t21_24_05502"));
		RANDOM.put("t21_24_05601", new RandomData("isr_t21_24_05601", "isr_t21_24_05602"));
		RANDOM.put("t21_24_05602", new RandomData("isr_t21_24_05601", "isr_t21_24_05602"));
		RANDOM.put("t21_24_05603", new RandomData("isr_t21_24_05603", "isr_t21_24_05604"));
		RANDOM.put("t21_24_05604", new RandomData("isr_t21_24_05603", "isr_t21_24_05604"));
		RANDOM.put("t21_24_05605", new RandomData("isr_t21_24_05605", "isr_t21_24_05606"));
		RANDOM.put("t21_24_05606", new RandomData("isr_t21_24_05605", "isr_t21_24_05606"));
		RANDOM.put("t21_24_05607", new RandomData("isr_t21_24_05607", "isr_t21_24_05608"));
		RANDOM.put("t21_24_05608", new RandomData("isr_t21_24_05607", "isr_t21_24_05608"));
		RANDOM.put("t21_24_05701", new RandomData("isr_t21_24_05701", "isr_t21_24_05702"));
		RANDOM.put("t21_24_05702", new RandomData("isr_t21_24_05701", "isr_t21_24_05702"));
		RANDOM.put("t21_24_05703", new RandomData("isr_t21_24_05703", "isr_t21_24_05704"));
		RANDOM.put("t21_24_05704", new RandomData("isr_t21_24_05703", "isr_t21_24_05704"));
		RANDOM.put("t21_24_05705", new RandomData("isr_t21_24_05705", "isr_t21_24_05706"));
		RANDOM.put("t21_24_05706", new RandomData("isr_t21_24_05705", "isr_t21_24_05706"));
		RANDOM.put("t21_24_05801", new RandomData("isr_t21_24_05801", "isr_t21_24_05802"));
		RANDOM.put("t21_24_05802", new RandomData("isr_t21_24_05801", "isr_t21_24_05802"));
		RANDOM.put("t21_24_05803", new RandomData("isr_t21_24_05803", "isr_t21_24_05804"));
		RANDOM.put("t21_24_05804", new RandomData("isr_t21_24_05803", "isr_t21_24_05804"));
		RANDOM.put("t21_24_05901", new RandomData("isr_t21_24_05901", "isr_t21_24_05902"));
		RANDOM.put("t21_24_05902", new RandomData("isr_t21_24_05901", "isr_t21_24_05902"));
		RANDOM.put("t21_24_05903", new RandomData("isr_t21_24_05903", "isr_t21_24_05904"));
		RANDOM.put("t21_24_05904", new RandomData("isr_t21_24_05903", "isr_t21_24_05904"));
		RANDOM.put("t21_24_05905", new RandomData("isr_t21_24_05905", "isr_t21_24_05906"));
		RANDOM.put("t21_24_05906", new RandomData("isr_t21_24_05905", "isr_t21_24_05906"));
		RANDOM.put("t21_24_06001", new RandomData("isr_t21_24_06001", "isr_t21_24_06002"));
		RANDOM.put("t21_24_06002", new RandomData("isr_t21_24_06001", "isr_t21_24_06002"));
		RANDOM.put("t21_24_06003", new RandomData("isr_t21_24_06003", "isr_t21_24_06004"));
		RANDOM.put("t21_24_06004", new RandomData("isr_t21_24_06003", "isr_t21_24_06004"));
		RANDOM.put("t21_24_06101", new RandomData("isr_t21_24_06101", "isr_t21_24_06102"));
		RANDOM.put("t21_24_06102", new RandomData("isr_t21_24_06101", "isr_t21_24_06102"));
		RANDOM.put("t21_24_06103", new RandomData("isr_t21_24_06103", "isr_t21_24_06104"));
		RANDOM.put("t21_24_06104", new RandomData("isr_t21_24_06103", "isr_t21_24_06104"));
		RANDOM.put("t21_24_06105", new RandomData("isr_t21_24_06105", "isr_t21_24_06106"));
		RANDOM.put("t21_24_06106", new RandomData("isr_t21_24_06105", "isr_t21_24_06106"));
		RANDOM.put("t21_24_06107", new RandomData("isr_t21_24_06107", "isr_t21_24_06108"));
		RANDOM.put("t21_24_06108", new RandomData("isr_t21_24_06107", "isr_t21_24_06108"));
		RANDOM.put("t21_24_06201", new RandomData("isr_t21_24_06201", "isr_t21_24_06202"));
		RANDOM.put("t21_24_06202", new RandomData("isr_t21_24_06201", "isr_t21_24_06202"));
		RANDOM.put("t21_24_06301", new RandomData("isr_t21_24_06301", "isr_t21_24_06302"));
		RANDOM.put("t21_24_06302", new RandomData("isr_t21_24_06301", "isr_t21_24_06302"));
		RANDOM.put("t21_24_06401", new RandomData("isr_t21_24_06401", "isr_t21_24_06402"));
		RANDOM.put("t21_24_06402", new RandomData("isr_t21_24_06401", "isr_t21_24_06402"));
		RANDOM.put("t21_24_06403", new RandomData("isr_t21_24_06403", "isr_t21_24_06404"));
		RANDOM.put("t21_24_06404", new RandomData("isr_t21_24_06403", "isr_t21_24_06404"));
		RANDOM.put("t21_24_06501", new RandomData("isr_t21_24_06501", "isr_t21_24_06502"));
		RANDOM.put("t21_24_06502", new RandomData("isr_t21_24_06501", "isr_t21_24_06502"));
		RANDOM.put("t21_24_06503", new RandomData("isr_t21_24_06503", "isr_t21_24_06504"));
		RANDOM.put("t21_24_06504", new RandomData("isr_t21_24_06503", "isr_t21_24_06504"));
		RANDOM.put("t21_24_06505", new RandomData("isr_t21_24_06505", "isr_t21_24_06506"));
		RANDOM.put("t21_24_06506", new RandomData("isr_t21_24_06505", "isr_t21_24_06506"));
		RANDOM.put("t21_24_06601", new RandomData("isr_t21_24_06601", "isr_t21_24_06602"));
		RANDOM.put("t21_24_06602", new RandomData("isr_t21_24_06601", "isr_t21_24_06602"));
		RANDOM.put("t21_24_06603", new RandomData("isr_t21_24_06603", "isr_t21_24_06604"));
		RANDOM.put("t21_24_06604", new RandomData("isr_t21_24_06603", "isr_t21_24_06604"));
		RANDOM.put("t21_24_06605", new RandomData("isr_t21_24_06605", "isr_t21_24_06606"));
		RANDOM.put("t21_24_06606", new RandomData("isr_t21_24_06605", "isr_t21_24_06606"));
		RANDOM.put("t21_24_06701", new RandomData("isr_t21_24_06701", "isr_t21_24_06702"));
		RANDOM.put("t21_24_06702", new RandomData("isr_t21_24_06701", "isr_t21_24_06702"));
		RANDOM.put("t21_24_06801", new RandomData("isr_t21_24_06801", "isr_t21_24_06802"));
		RANDOM.put("t21_24_06802", new RandomData("isr_t21_24_06801", "isr_t21_24_06802"));
		RANDOM.put("t21_24_06803", new RandomData("isr_t21_24_06803", "isr_t21_24_06804"));
		RANDOM.put("t21_24_06804", new RandomData("isr_t21_24_06803", "isr_t21_24_06804"));
		RANDOM.put("t21_24_06805", new RandomData("isr_t21_24_06805", "isr_t21_24_06806"));
		RANDOM.put("t21_24_06806", new RandomData("isr_t21_24_06805", "isr_t21_24_06806"));
		RANDOM.put("t21_24_06901", new RandomData("isr_t21_24_06901", "isr_t21_24_06902"));
		RANDOM.put("t21_24_06902", new RandomData("isr_t21_24_06901", "isr_t21_24_06902"));
		RANDOM.put("t21_24_06903", new RandomData("isr_t21_24_06903", "isr_t21_24_06904"));
		RANDOM.put("t21_24_06904", new RandomData("isr_t21_24_06903", "isr_t21_24_06904"));
		RANDOM.put("t21_24_06905", new RandomData("isr_t21_24_06905", "isr_t21_24_06906"));
		RANDOM.put("t21_24_06906", new RandomData("isr_t21_24_06905", "isr_t21_24_06906"));
		RANDOM.put("t21_24_07001", new RandomData("isr_t21_24_07001", "isr_t21_24_07002"));
		RANDOM.put("t21_24_07002", new RandomData("isr_t21_24_07001", "isr_t21_24_07002"));
		RANDOM.put("t21_24_07003", new RandomData("isr_t21_24_07003", "isr_t21_24_07004"));
		RANDOM.put("t21_24_07004", new RandomData("isr_t21_24_07003", "isr_t21_24_07004"));
		RANDOM.put("t21_24_07101", new RandomData("isr_t21_24_07101", "isr_t21_24_07102"));
		RANDOM.put("t21_24_07102", new RandomData("isr_t21_24_07101", "isr_t21_24_07102"));
		RANDOM.put("t21_24_07201", new RandomData("isr_t21_24_07201", "isr_t21_24_07202"));
		RANDOM.put("t21_24_07202", new RandomData("isr_t21_24_07201", "isr_t21_24_07202"));
		RANDOM.put("t21_24_07301", new RandomData("isr_t21_24_07301", "isr_t21_24_07302"));
		RANDOM.put("t21_24_07302", new RandomData("isr_t21_24_07301", "isr_t21_24_07302"));
		RANDOM.put("t21_24_07401", new RandomData("isr_t21_24_07401", "isr_t21_24_07402"));
		RANDOM.put("t21_24_07402", new RandomData("isr_t21_24_07401", "isr_t21_24_07402"));
		RANDOM.put("t21_24_07403", new RandomData("isr_t21_24_07403", "isr_t21_24_07404"));
		RANDOM.put("t21_24_07404", new RandomData("isr_t21_24_07403", "isr_t21_24_07404"));
		RANDOM.put("t21_24_07405", new RandomData("isr_t21_24_07405", "isr_t21_24_07406"));
		RANDOM.put("t21_24_07406", new RandomData("isr_t21_24_07405", "isr_t21_24_07406"));
		RANDOM.put("t21_24_07501", new RandomData("isr_t21_24_07501", "isr_t21_24_07502"));
		RANDOM.put("t21_24_07502", new RandomData("isr_t21_24_07501", "isr_t21_24_07502"));
		RANDOM.put("t21_24_07601", new RandomData("isr_t21_24_07601", "isr_t21_24_07602"));
		RANDOM.put("t21_24_07602", new RandomData("isr_t21_24_07601", "isr_t21_24_07602"));
		RANDOM.put("t21_24_07801", new RandomData("isr_t21_24_07801", "isr_t21_24_07802"));
		RANDOM.put("t21_24_07802", new RandomData("isr_t21_24_07801", "isr_t21_24_07802"));
		RANDOM.put("t21_24_07803", new RandomData("isr_t21_24_07803", "isr_t21_24_07804"));
		RANDOM.put("t21_24_07804", new RandomData("isr_t21_24_07803", "isr_t21_24_07804"));
		RANDOM.put("t21_24_07805", new RandomData("isr_t21_24_07805", "isr_t21_24_07806"));
		RANDOM.put("t21_24_07806", new RandomData("isr_t21_24_07805", "isr_t21_24_07806"));
		RANDOM.put("t21_24_07807", new RandomData("isr_t21_24_07807", "isr_t21_24_07808"));
		RANDOM.put("t21_24_07808", new RandomData("isr_t21_24_07807", "isr_t21_24_07808"));
		RANDOM.put("t21_24_07809", new RandomData("isr_t21_24_07809", "isr_t21_24_07810"));
		RANDOM.put("t21_24_07810", new RandomData("isr_t21_24_07809", "isr_t21_24_07810"));
		RANDOM.put("t21_24_07901", new RandomData("isr_t21_24_07901", "isr_t21_24_07902"));
		RANDOM.put("t21_24_07902", new RandomData("isr_t21_24_07901", "isr_t21_24_07902"));
		RANDOM.put("t21_24_08001", new RandomData("isr_t21_24_08001", "isr_t21_24_08002"));
		RANDOM.put("t21_24_08002", new RandomData("isr_t21_24_08001", "isr_t21_24_08002"));
		RANDOM.put("t21_24_08101", new RandomData("isr_t21_24_08101", "isr_t21_24_08102"));
		RANDOM.put("t21_24_08102", new RandomData("isr_t21_24_08101", "isr_t21_24_08102"));
		RANDOM.put("t21_24_08103", new RandomData("isr_t21_24_08103", "isr_t21_24_08104"));
		RANDOM.put("t21_24_08104", new RandomData("isr_t21_24_08103", "isr_t21_24_08104"));
		RANDOM.put("t21_24_08105", new RandomData("isr_t21_24_08105", "isr_t21_24_08106"));
		RANDOM.put("t21_24_08106", new RandomData("isr_t21_24_08105", "isr_t21_24_08106"));
		RANDOM.put("t21_24_08201", new RandomData("isr_t21_24_08201", "isr_t21_24_08202"));
		RANDOM.put("t21_24_08202", new RandomData("isr_t21_24_08201", "isr_t21_24_08202"));
		RANDOM.put("t21_24_08203", new RandomData("isr_t21_24_08203", "isr_t21_24_08204"));
		RANDOM.put("t21_24_08204", new RandomData("isr_t21_24_08203", "isr_t21_24_08204"));
		RANDOM.put("t21_24_08205", new RandomData("isr_t21_24_08205", "isr_t21_24_08206"));
		RANDOM.put("t21_24_08206", new RandomData("isr_t21_24_08205", "isr_t21_24_08206"));
		RANDOM.put("t21_24_08207", new RandomData("isr_t21_24_08207", "isr_t21_24_08208"));
		RANDOM.put("t21_24_08208", new RandomData("isr_t21_24_08207", "isr_t21_24_08208"));
		RANDOM.put("t21_24_08301", new RandomData("isr_t21_24_08301", "isr_t21_24_08302"));
		RANDOM.put("t21_24_08302", new RandomData("isr_t21_24_08301", "isr_t21_24_08302"));
		RANDOM.put("t21_24_08303", new RandomData("isr_t21_24_08303", "isr_t21_24_08304"));
		RANDOM.put("t21_24_08304", new RandomData("isr_t21_24_08303", "isr_t21_24_08304"));
		RANDOM.put("t21_24_08305", new RandomData("isr_t21_24_08305", "isr_t21_24_08306"));
		RANDOM.put("t21_24_08306", new RandomData("isr_t21_24_08305", "isr_t21_24_08306"));
		RANDOM.put("t21_24_08401", new RandomData("isr_t21_24_08401", "isr_t21_24_08402"));
		RANDOM.put("t21_24_08402", new RandomData("isr_t21_24_08401", "isr_t21_24_08402"));
		RANDOM.put("t21_24_08403", new RandomData("isr_t21_24_08403", "isr_t21_24_08404"));
		RANDOM.put("t21_24_08404", new RandomData("isr_t21_24_08403", "isr_t21_24_08404"));
		RANDOM.put("t21_24_08501", new RandomData("isr_t21_24_08501", "isr_t21_24_08502"));
		RANDOM.put("t21_24_08502", new RandomData("isr_t21_24_08501", "isr_t21_24_08502"));
		RANDOM.put("t21_24_08503", new RandomData("isr_t21_24_08503", "isr_t21_24_08504"));
		RANDOM.put("t21_24_08504", new RandomData("isr_t21_24_08503", "isr_t21_24_08504"));
		RANDOM.put("t21_24_08601", new RandomData("isr_t21_24_08601", "isr_t21_24_08602"));
		RANDOM.put("t21_24_08602", new RandomData("isr_t21_24_08601", "isr_t21_24_08602"));
		RANDOM.put("t21_24_07701", new RandomData("isr_t21_24_07701", "isr_t21_24_07702"));
		RANDOM.put("t21_24_07702", new RandomData("isr_t21_24_07701", "isr_t21_24_07702"));
		RANDOM.put("t21_24_07703", new RandomData("isr_t21_24_07703", "isr_t21_24_07704"));
		RANDOM.put("t21_24_07704", new RandomData("isr_t21_24_07703", "isr_t21_24_07704"));
		RANDOM.put("t21_24_07705", new RandomData("isr_t21_24_07705", "isr_t21_24_07706"));
		RANDOM.put("t21_24_07706", new RandomData("isr_t21_24_07705", "isr_t21_24_07706"));
		RANDOM.put("t21_24_08701", new RandomData("isr_t21_24_08701", "isr_t21_24_08702"));
		RANDOM.put("t21_24_08702", new RandomData("isr_t21_24_08701", "isr_t21_24_08702"));
		RANDOM.put("t21_24_08801", new RandomData("isr_t21_24_08801", "isr_t21_24_08802"));
		RANDOM.put("t21_24_08802", new RandomData("isr_t21_24_08801", "isr_t21_24_08802"));
		RANDOM.put("t21_24_08803", new RandomData("isr_t21_24_08803", "isr_t21_24_08804"));
		RANDOM.put("t21_24_08804", new RandomData("isr_t21_24_08803", "isr_t21_24_08804"));
		RANDOM.put("t21_24_08805", new RandomData("isr_t21_24_08805", "isr_t21_24_08806"));
		RANDOM.put("t21_24_08806", new RandomData("isr_t21_24_08805", "isr_t21_24_08806"));
		RANDOM.put("t21_24_08901", new RandomData("isr_t21_24_08901", "isr_t21_24_08902"));
		RANDOM.put("t21_24_08902", new RandomData("isr_t21_24_08901", "isr_t21_24_08902"));
		RANDOM.put("t21_24_09001", new RandomData("isr_t21_24_09001", "isr_t21_24_09002"));
		RANDOM.put("t21_24_09002", new RandomData("isr_t21_24_09001", "isr_t21_24_09002"));
		RANDOM.put("t21_24_09101", new RandomData("isr_t21_24_09101", "isr_t21_24_09102"));
		RANDOM.put("t21_24_09102", new RandomData("isr_t21_24_09101", "isr_t21_24_09102"));
		RANDOM.put("t21_24_09201", new RandomData("isr_t21_24_09201", "isr_t21_24_09202"));
		RANDOM.put("t21_24_09202", new RandomData("isr_t21_24_09201", "isr_t21_24_09202"));
		RANDOM.put("t21_24_09203", new RandomData("isr_t21_24_09203", "isr_t21_24_09204"));
		RANDOM.put("t21_24_09204", new RandomData("isr_t21_24_09203", "isr_t21_24_09204"));
		RANDOM.put("t21_24_09205", new RandomData("isr_t21_24_09205", "isr_t21_24_09206"));
		RANDOM.put("t21_24_09206", new RandomData("isr_t21_24_09205", "isr_t21_24_09206"));
		RANDOM.put("t21_24_09207", new RandomData("isr_t21_24_09207", "isr_t21_24_09208"));
		RANDOM.put("t21_24_09208", new RandomData("isr_t21_24_09207", "isr_t21_24_09208"));
		RANDOM.put("t21_24_09301", new RandomData("isr_t21_24_09301", "isr_t21_24_09302"));
		RANDOM.put("t21_24_09302", new RandomData("isr_t21_24_09301", "isr_t21_24_09302"));
		RANDOM.put("t21_24_09303", new RandomData("isr_t21_24_09303", "isr_t21_24_09304"));
		RANDOM.put("t21_24_09304", new RandomData("isr_t21_24_09303", "isr_t21_24_09304"));
		RANDOM.put("t21_24_09305", new RandomData("isr_t21_24_09305", "isr_t21_24_09306"));
		RANDOM.put("t21_24_09306", new RandomData("isr_t21_24_09305", "isr_t21_24_09306"));
		RANDOM.put("t21_24_09401", new RandomData("isr_t21_24_09401", "isr_t21_24_09402"));
		RANDOM.put("t21_24_09402", new RandomData("isr_t21_24_09401", "isr_t21_24_09402"));
		RANDOM.put("t21_24_09403", new RandomData("isr_t21_24_09403", "isr_t21_24_09404"));
		RANDOM.put("t21_24_09404", new RandomData("isr_t21_24_09403", "isr_t21_24_09404"));
		RANDOM.put("t21_24_09405", new RandomData("isr_t21_24_09405", "isr_t21_24_09406"));
		RANDOM.put("t21_24_09406", new RandomData("isr_t21_24_09405", "isr_t21_24_09406"));
		RANDOM.put("t21_24_09501", new RandomData("isr_t21_24_09501", "isr_t21_24_09502"));
		RANDOM.put("t21_24_09502", new RandomData("isr_t21_24_09501", "isr_t21_24_09502"));
		RANDOM.put("t21_24_09503", new RandomData("isr_t21_24_09503", "isr_t21_24_09504"));
		RANDOM.put("t21_24_09504", new RandomData("isr_t21_24_09503", "isr_t21_24_09504"));
		RANDOM.put("t21_24_09601", new RandomData("isr_t21_24_09601", "isr_t21_24_09602"));
		RANDOM.put("t21_24_09602", new RandomData("isr_t21_24_09601", "isr_t21_24_09602"));
		RANDOM.put("t21_24_09603", new RandomData("isr_t21_24_09603", "isr_t21_24_09604"));
		RANDOM.put("t21_24_09604", new RandomData("isr_t21_24_09603", "isr_t21_24_09604"));
		RANDOM.put("t21_24_09701", new RandomData("isr_t21_24_09701", "isr_t21_24_09702"));
		RANDOM.put("t21_24_09702", new RandomData("isr_t21_24_09701", "isr_t21_24_09702"));
		RANDOM.put("t21_24_09801", new RandomData("isr_t21_24_09801", "isr_t21_24_09802"));
		RANDOM.put("t21_24_09802", new RandomData("isr_t21_24_09801", "isr_t21_24_09802"));
		RANDOM.put("t21_24_09803", new RandomData("isr_t21_24_09803", "isr_t21_24_09804"));
		RANDOM.put("t21_24_09804", new RandomData("isr_t21_24_09803", "isr_t21_24_09804"));
		RANDOM.put("t21_24_09805", new RandomData("isr_t21_24_09805", "isr_t21_24_09806"));
		RANDOM.put("t21_24_09806", new RandomData("isr_t21_24_09805", "isr_t21_24_09806"));
		RANDOM.put("t21_24_09807", new RandomData("isr_t21_24_09807", "isr_t21_24_09808"));
		RANDOM.put("t21_24_09808", new RandomData("isr_t21_24_09807", "isr_t21_24_09808"));
		RANDOM.put("godard26_2516_1402", new RandomData("isr_godard26_2516_1402", "isr_godard26_2516_1403", "isr_godard26_2516_1404"));
		RANDOM.put("godard26_2516_1403", new RandomData("isr_godard26_2516_1402", "isr_godard26_2516_1403", "isr_godard26_2516_1404"));
		RANDOM.put("godard26_2516_1404", new RandomData("isr_godard26_2516_1402", "isr_godard26_2516_1403", "isr_godard26_2516_1404"));
		RANDOM.put("godard26_2516_1405", new RandomData("isr_godard26_2516_1405", "isr_godard26_2516_1406", "isr_godard26_2516_1407"));
		RANDOM.put("godard26_2516_1406", new RandomData("isr_godard26_2516_1405", "isr_godard26_2516_1406", "isr_godard26_2516_1407"));
		RANDOM.put("godard26_2516_1407", new RandomData("isr_godard26_2516_1405", "isr_godard26_2516_1406", "isr_godard26_2516_1407"));
		RANDOM.put("godard26_2516_3902", new RandomData("isr_godard26_2516_3902", "isr_godard26_2516_3903", "isr_godard26_2516_3904"));
		RANDOM.put("godard26_2516_3903", new RandomData("isr_godard26_2516_3902", "isr_godard26_2516_3903", "isr_godard26_2516_3904"));
		RANDOM.put("godard26_2516_3904", new RandomData("isr_godard26_2516_3902", "isr_godard26_2516_3903", "isr_godard26_2516_3904"));
		RANDOM.put("godard26_2516_3905", new RandomData("isr_godard26_2516_3905", "isr_godard26_2516_3906", "isr_godard26_2516_3907"));
		RANDOM.put("godard26_2516_3906", new RandomData("isr_godard26_2516_3905", "isr_godard26_2516_3906", "isr_godard26_2516_3907"));
		RANDOM.put("godard26_2516_3907", new RandomData("isr_godard26_2516_3905", "isr_godard26_2516_3906", "isr_godard26_2516_3907"));
		RANDOM.put("godard26_2516_4301", new RandomData("isr_godard26_2516_4301", "isr_godard26_2516_4302", "isr_godard26_2516_4303"));
		RANDOM.put("godard26_2516_4302", new RandomData("isr_godard26_2516_4301", "isr_godard26_2516_4302", "isr_godard26_2516_4303"));
		RANDOM.put("godard26_2516_4303", new RandomData("isr_godard26_2516_4301", "isr_godard26_2516_4302", "isr_godard26_2516_4303"));
		RANDOM.put("godard26_2516_4304", new RandomData("isr_godard26_2516_4304", "isr_godard26_2516_4305", "isr_godard26_2516_4306"));
		RANDOM.put("godard26_2516_4305", new RandomData("isr_godard26_2516_4304", "isr_godard26_2516_4305", "isr_godard26_2516_4306"));
		RANDOM.put("godard26_2516_4306", new RandomData("isr_godard26_2516_4304", "isr_godard26_2516_4305", "isr_godard26_2516_4306"));
		RANDOM.put("godard26_2516_4401", new RandomData("isr_godard26_2516_4401", "isr_godard26_2516_4402", "isr_godard26_2516_4403"));
		RANDOM.put("godard26_2516_4402", new RandomData("isr_godard26_2516_4401", "isr_godard26_2516_4402", "isr_godard26_2516_4403"));
		RANDOM.put("godard26_2516_4403", new RandomData("isr_godard26_2516_4401", "isr_godard26_2516_4402", "isr_godard26_2516_4403"));
		RANDOM.put("godard26_2516_4404", new RandomData("isr_godard26_2516_4404", "isr_godard26_2516_4405", "isr_godard26_2516_4406"));
		RANDOM.put("godard26_2516_4405", new RandomData("isr_godard26_2516_4404", "isr_godard26_2516_4405", "isr_godard26_2516_4406"));
		RANDOM.put("godard26_2516_4406", new RandomData("isr_godard26_2516_4404", "isr_godard26_2516_4405", "isr_godard26_2516_4406"));
		RANDOM.put("godard26_2516_4501", new RandomData("isr_godard26_2516_4501", "isr_godard26_2516_4502", "isr_godard26_2516_4503"));
		RANDOM.put("godard26_2516_4502", new RandomData("isr_godard26_2516_4501", "isr_godard26_2516_4502", "isr_godard26_2516_4503"));
		RANDOM.put("godard26_2516_4503", new RandomData("isr_godard26_2516_4501", "isr_godard26_2516_4502", "isr_godard26_2516_4503"));
		RANDOM.put("godard26_2516_4504", new RandomData("isr_godard26_2516_4504", "isr_godard26_2516_4505", "isr_godard26_2516_4506"));
		RANDOM.put("godard26_2516_4505", new RandomData("isr_godard26_2516_4504", "isr_godard26_2516_4505", "isr_godard26_2516_4506"));
		RANDOM.put("godard26_2516_4506", new RandomData("isr_godard26_2516_4504", "isr_godard26_2516_4505", "isr_godard26_2516_4506"));
		RANDOM.put("godard14_09_01", new RandomData("isr_godard14_09_01", "isr_godard14_09_02", "isr_godard14_09_03"));
		RANDOM.put("godard14_09_02", new RandomData("isr_godard14_09_01", "isr_godard14_09_02", "isr_godard14_09_03"));
		RANDOM.put("godard14_09_03", new RandomData("isr_godard14_09_01", "isr_godard14_09_02", "isr_godard14_09_03"));
		RANDOM.put("godard14_10_01", new RandomData("isr_godard14_10_01", "isr_godard14_10_02", "isr_godard14_10_03"));
		RANDOM.put("godard14_10_02", new RandomData("isr_godard14_10_01", "isr_godard14_10_02", "isr_godard14_10_03"));
		RANDOM.put("godard14_10_03", new RandomData("isr_godard14_10_01", "isr_godard14_10_02", "isr_godard14_10_03"));
		RANDOM.put("godard14_11_01", new RandomData("isr_godard14_11_01", "isr_godard14_11_02", "isr_godard14_11_03"));
		RANDOM.put("godard14_11_02", new RandomData("isr_godard14_11_01", "isr_godard14_11_02", "isr_godard14_11_03"));
		RANDOM.put("godard14_11_03", new RandomData("isr_godard14_11_01", "isr_godard14_11_02", "isr_godard14_11_03"));
	}

	public InstantSpawn()
	{
		super(-1, "ai/spawn");

		addMakerNpcsKilledByName(SELF);
		addMakerNpcsKilledByName(SERIAL.keySet());
		addMakerNpcsKilledByName(RANDOM.keySet());
	}

	@Override
	public void onMakerNpcsKilled(NpcMaker maker, Npc npc)
	{
		String name = maker.getName();
		if (SELF.contains(name))
		{
			// respawn self
			name = maker.getEvent();
		}
		else if (SERIAL.containsKey(name))
		{
			// get serial spawn data
			final SerialData sd = SERIAL.get(name);

			// if loop count passed, spawn other, otherwise spawn self
			name = sd.isPassed() ? sd.getEvent() : maker.getEvent();
		}
		else if (RANDOM.containsKey(name))
		{
			// get random
			name = RANDOM.get(name).getRandomEvent();
		}

		// schedule next spawn
		startQuestTimer(name, null, null, npc.getSpawn().getRespawnDelay() * 1000);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		SpawnManager.getInstance().spawnEventNpcs(name, false, false);
		return null;
	}

	/**
	 * Contains serial triggered instant spawn data.<br>
	 * Contains total loop counts, when passed, the event is called. Also contains current loop counter.
	 */
	private class SerialData
	{
		private final int _loops;
		private final String _event;

		private int _loop;

		protected SerialData(int loops, String maker)
		{
			_loops = loops;
			_event = maker;

			_loop = loops;
		}

		protected synchronized boolean isPassed()
		{
			if (--_loop <= 0)
			{
				_loop = _loops;
				return true;
			}

			return false;
		}

		protected String getEvent()
		{
			return _event;
		}
	}

	/**
	 * Contains random triggered instant spawn data.<br>
	 * Contains also respawn delay for new spawns.
	 */
	private class RandomData
	{
		private final String _event[];

		protected RandomData(String... maker)
		{
			_event = maker;
		}

		protected String getRandomEvent()
		{
			return Rnd.get(_event);
		}
	}
}