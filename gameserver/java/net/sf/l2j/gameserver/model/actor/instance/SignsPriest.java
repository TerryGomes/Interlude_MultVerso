package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Map.Entry;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class SignsPriest extends Folk
{
	public SignsPriest(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player.getCurrentFolk() == null || player.getCurrentFolk().getObjectId() != getObjectId())
			return;
		
		if (command.startsWith("SevenSignsDesc"))
		{
			showChatWindow(player, Integer.parseInt(command.substring(15)), null, true);
		}
		else if (command.startsWith("SevenSigns"))
		{
			// Create string tokenizer and get actual command.
			final StringTokenizer st = new StringTokenizer(command.trim());
			st.nextToken();
			
			// Get command value.
			final int value = Integer.parseInt(st.nextToken());
			switch (value)
			{
				case 2: // "I want to buy the Record of Seven Signs.", [SevenSigns 2]
					if (!player.getInventory().validateCapacity(1))
					{
						player.sendPacket(SystemMessageId.SLOTS_FULL);
						return;
					}
					
					if (!player.reduceAdena("SevenSigns", SevenSignsManager.RECORD_SEVEN_SIGNS_COST, this, true))
					{
						showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn_no" : "dusk_no", false);
						return;
					}
					
					player.addItem("SevenSigns", SevenSignsManager.RECORD_SEVEN_SIGNS_ID, 1, player, true);
					
					showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
					break;
				
				case 33: // "I wish to participate.", "Back", [SevenSigns 33 cabal]
					CabalType cabal = CabalType.VALUES[Integer.parseInt(st.nextToken())];
					
					// Check player's current cabal.
					if (SevenSignsManager.getInstance().getPlayerCabal(player.getObjectId()) != CabalType.NORMAL)
					{
						showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn_member" : "dusk_member", false);
						return;
					}
					
					if (!Config.SEVEN_SIGNS_BYPASS_PREREQUISITES)
					{
						// First check player's class.
						final int classLevel = player.getClassId().getLevel();
						if (classLevel == 0)
						{
							showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn_firstclass" : "dusk_firstclass", false);
							return;
						}
						
						// Characters without a Second Class Transfer may freely join either the Lords of Dawn or the Revolutionary Army of Dusk.
						if (classLevel > 1)
						{
							final boolean hasCastle = player.getClan() != null && player.getClan().hasCastle();
							
							// Only characters who are not members of the castle-holding clans and guilds may join Dusk.
							if (cabal == CabalType.DUSK && hasCastle)
							{
								showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "signs_33_dusk_no.htm");
								return;
							}
							
							// Characters who are not members of castle-holding clans need to get an Approval Certificate from their castle's Lord to join Dawn, or pay 50,000 adena.
							if (cabal == CabalType.DAWN && !hasCastle)
							{
								showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_fee.htm");
								return;
							}
						}
					}
					
					showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
					break;
				
				case 34: // "Pay the participation fee.", [SevenSigns 34 cabal] (cabal is not used, it is only for Dawn Priest)
					final ItemInstance adena = player.getInventory().getItemByItemId(PcInventory.ADENA_ID);
					final ItemInstance certif = player.getInventory().getItemByItemId(6388);
					final boolean canPayFee = (adena != null && adena.getCount() >= SevenSignsManager.ADENA_JOIN_DAWN_COST) || (certif != null && certif.getCount() >= 1);
					
					showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + ((canPayFee) ? "signs_33_dawn.htm" : "signs_33_dawn_no.htm"));
					break;
				
				case 3: // "I want to participate in the Seven Signs.", "Back", [SevenSigns 3 cabal]
				case 8: // "I want to move to the Oracle of Dawn/Dusk.", "I wish to participate in the Festival of Darkness.", [SevenSigns 8 cabal]
					cabal = CabalType.VALUES[Integer.parseInt(st.nextToken())];
					showChatWindow(player, value, cabal.getShortName(), false);
					break;
				
				case 4: // "I wish to fight for the Seal of Avarice/Gnosis/Strife.", [SevenSigns 4 cabal seal]
					cabal = CabalType.VALUES[Integer.parseInt(st.nextToken())];
					SealType seal = SealType.VALUES[Integer.parseInt(st.nextToken())];
					
					if (!Config.SEVEN_SIGNS_BYPASS_PREREQUISITES)
					{
						final boolean hasCastle = player.getClan() != null && player.getClan().hasCastle();
						
						if (cabal == CabalType.DUSK && hasCastle)
						{
							showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "signs_33_dusk_no.htm");
							return;
						}
						
						// Test, in the order - Castle ownership, Certificate of Lord, Adena.
						if (cabal == CabalType.DAWN && (!hasCastle && !player.destroyItemByItemId("SevenSigns", SevenSignsManager.CERTIFICATE_OF_APPROVAL_ID, 1, this, false) && !player.reduceAdena("SevenSigns", SevenSignsManager.ADENA_JOIN_DAWN_COST, this, false)))
						{
							showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_no.htm");
							return;
						}
					}
					
					SevenSignsManager.getInstance().setPlayerInfo(player.getObjectId(), cabal, seal);
					
					if (cabal == CabalType.DAWN)
						player.sendPacket(SystemMessageId.SEVENSIGNS_PARTECIPATION_DAWN); // Joined Dawn
					else
						player.sendPacket(SystemMessageId.SEVENSIGNS_PARTECIPATION_DUSK); // Joined Dusk
						
					// Show a confirmation message to the user, indicating which seal they chose.
					switch (seal)
					{
						case AVARICE:
							player.sendPacket(SystemMessageId.FIGHT_FOR_AVARICE);
							break;
						
						case GNOSIS:
							player.sendPacket(SystemMessageId.FIGHT_FOR_GNOSIS);
							break;
						
						case STRIFE:
							player.sendPacket(SystemMessageId.FIGHT_FOR_STRIFE);
							break;
					}
					
					showChatWindow(player, 4, cabal.getShortName(), false);
					break;
				
				case 5: // "I want to contribute Seal Stones.", "Back", [SevenSigns 5 cabal]
					cabal = CabalType.VALUES[Integer.parseInt(st.nextToken())];
					
					if (SevenSignsManager.getInstance().getPlayerCabal(player.getObjectId()) == CabalType.NORMAL)
						showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn_no" : "dusk_no", false);
					else
						showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
					break;
				
				case 21: // Contribution of Seal Stones. [SevenSigns 21 id amount]
					int itemId = Integer.parseInt(st.nextToken());
					int amount = 0;
					try
					{
						// Note: We must use substring, as the amount parameter (from edit HTML component) may return number with spaces inside.
						amount = Integer.parseInt(command.substring(19).trim());
					}
					catch (Exception NumberFormatException)
					{
						showChatWindow(player, 6, (this instanceof DawnPriest) ? "dawn_failure" : "dusk_failure", false);
						return;
					}
					
					ItemInstance contribBlueStones = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_BLUE_ID);
					ItemInstance contribGreenStones = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_GREEN_ID);
					ItemInstance contribRedStones = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_RED_ID);
					
					int contribBlueStoneCount = contribBlueStones == null ? 0 : contribBlueStones.getCount();
					int contribGreenStoneCount = contribGreenStones == null ? 0 : contribGreenStones.getCount();
					int contribRedStoneCount = contribRedStones == null ? 0 : contribRedStones.getCount();
					
					int score = SevenSignsManager.getInstance().getPlayerContribScore(player.getObjectId());
					
					int redContrib = 0;
					int greenContrib = 0;
					int blueContrib = 0;
					switch (itemId)
					{
						case SevenSignsManager.SEAL_STONE_BLUE_ID:
							blueContrib = (Config.MAXIMUM_PLAYER_CONTRIB - score) / SevenSignsManager.SEAL_STONE_BLUE_VALUE;
							if (blueContrib > contribBlueStoneCount)
								blueContrib = amount;
							break;
						
						case SevenSignsManager.SEAL_STONE_GREEN_ID:
							greenContrib = (Config.MAXIMUM_PLAYER_CONTRIB - score) / SevenSignsManager.SEAL_STONE_GREEN_VALUE;
							if (greenContrib > contribGreenStoneCount)
								greenContrib = amount;
							break;
						
						case SevenSignsManager.SEAL_STONE_RED_ID:
							redContrib = (Config.MAXIMUM_PLAYER_CONTRIB - score) / SevenSignsManager.SEAL_STONE_RED_VALUE;
							if (redContrib > contribRedStoneCount)
								redContrib = amount;
							break;
					}
					
					boolean contribStonesFound = false;
					if (redContrib > 0)
						contribStonesFound |= player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_RED_ID, redContrib, this, true);
					
					if (greenContrib > 0)
						contribStonesFound |= player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_GREEN_ID, greenContrib, this, true);
					
					if (blueContrib > 0)
						contribStonesFound |= player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_BLUE_ID, blueContrib, this, true);
					
					if (!contribStonesFound)
					{
						showChatWindow(player, 6, (this instanceof DawnPriest) ? "dawn_low_stones" : "dusk_low_stones", false);
						return;
					}
					
					score = SevenSignsManager.getInstance().addPlayerStoneContrib(player.getObjectId(), blueContrib, greenContrib, redContrib);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CONTRIB_SCORE_INCREASED_S1).addItemNumber(score));
					
					showChatWindow(player, 6, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
					break;
				
				case 6: // "Contribute Blue/Green/Red/all of my Seal Stones.", "Receive bonuses for gathering seal stones." [SevenSigns 6 stone]
					int stoneType = Integer.parseInt(st.nextToken());
					
					ItemInstance blueStones = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_BLUE_ID);
					ItemInstance greenStones = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_GREEN_ID);
					ItemInstance redStones = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_RED_ID);
					
					int blueStoneCount = blueStones == null ? 0 : blueStones.getCount();
					int greenStoneCount = greenStones == null ? 0 : greenStones.getCount();
					int redStoneCount = redStones == null ? 0 : redStones.getCount();
					
					int contribScore = SevenSignsManager.getInstance().getPlayerContribScore(player.getObjectId());
					boolean stonesFound = false;
					
					if (contribScore == Config.MAXIMUM_PLAYER_CONTRIB)
						player.sendPacket(SystemMessageId.CONTRIB_SCORE_EXCEEDED);
					else
					{
						String stoneColor = "";
						int stoneCount = 0;
						int stoneId = 0;
						
						switch (stoneType)
						{
							case 1:
								stoneColor = "Blue";
								stoneId = SevenSignsManager.SEAL_STONE_BLUE_ID;
								stoneCount = blueStoneCount;
								break;
							
							case 2:
								stoneColor = "Green";
								stoneId = SevenSignsManager.SEAL_STONE_GREEN_ID;
								stoneCount = greenStoneCount;
								break;
							
							case 3:
								stoneColor = "Red";
								stoneId = SevenSignsManager.SEAL_STONE_RED_ID;
								stoneCount = redStoneCount;
								break;
							
							case 4:
								int tempContribScore = contribScore;
								int redContribCount = (Config.MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSignsManager.SEAL_STONE_RED_VALUE;
								if (redContribCount > redStoneCount)
									redContribCount = redStoneCount;
								
								tempContribScore += redContribCount * SevenSignsManager.SEAL_STONE_RED_VALUE;
								int greenContribCount = (Config.MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSignsManager.SEAL_STONE_GREEN_VALUE;
								if (greenContribCount > greenStoneCount)
									greenContribCount = greenStoneCount;
								
								tempContribScore += greenContribCount * SevenSignsManager.SEAL_STONE_GREEN_VALUE;
								int blueContribCount = (Config.MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSignsManager.SEAL_STONE_BLUE_VALUE;
								if (blueContribCount > blueStoneCount)
									blueContribCount = blueStoneCount;
								
								if (redContribCount > 0)
									stonesFound |= player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_RED_ID, redContribCount, this, true);
								
								if (greenContribCount > 0)
									stonesFound |= player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_GREEN_ID, greenContribCount, this, true);
								
								if (blueContribCount > 0)
									stonesFound |= player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_BLUE_ID, blueContribCount, this, true);
								
								if (!stonesFound)
									showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn_no_stones" : "dusk_no_stones", false);
								else
								{
									contribScore = SevenSignsManager.getInstance().addPlayerStoneContrib(player.getObjectId(), blueContribCount, greenContribCount, redContribCount);
									player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CONTRIB_SCORE_INCREASED_S1).addItemNumber(contribScore));
									
									showChatWindow(player, 6, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
								}
								return;
						}
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(SevenSignsManager.SEVEN_SIGNS_HTML_PATH + ((this instanceof DawnPriest) ? "signs_6_dawn_contribute.htm" : "signs_6_dusk_contribute.htm"));
						html.replace("%stoneColor%", stoneColor);
						html.replace("%stoneCount%", stoneCount);
						html.replace("%stoneItemId%", stoneId);
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					break;
				
				case 7: // Exchange Ancient Adena for Adena at Black Marketeer of Mammon, [SevenSigns 7 adena]
					amount = 0;
					try
					{
						// Note: We must use substring, as the amount parameter (from edit HTML component) may return number with spaces inside.
						amount = Integer.parseInt(command.substring(13).trim());
					}
					catch (Exception e)
					{
						showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						return;
					}
					
					if (amount < 1)
					{
						showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						return;
					}
					
					if (player.getAncientAdena() < amount)
					{
						showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "blkmrkt_4.htm");
						return;
					}
					
					if (player.reduceAncientAdena("SevenSigns", amount, this, true))
						player.addAdena("SevenSigns", amount, this, true);
					
					showChatWindow(player, SevenSignsManager.SEVEN_SIGNS_HTML_PATH + "blkmrkt_5.htm");
					break;
				
				case 9: // "I want my reward for collecting Seal Stones.", [SevenSigns 9]
					if (SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getPlayerCabal(player.getObjectId()) == SevenSignsManager.getInstance().getWinningCabal())
					{
						final int reward = SevenSignsManager.getInstance().getAncientAdenaReward(player.getObjectId());
						if (reward <= 0)
						{
							showChatWindow(player, 9, (this instanceof DawnPriest) ? "dawn_b" : "dusk_b", false);
							return;
						}
						
						player.addAncientAdena("SevenSigns", reward, this, true);
						
						showChatWindow(player, 9, (this instanceof DawnPriest) ? "dawn_a" : "dusk_a", false);
					}
					break;
				
				case 11: // Teleport to Hunting Grounds. [SevenSigns 11 X Y Z cost]
					try
					{
						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());
						
						final int ancientAdenaCost = Integer.parseInt(st.nextToken());
						if (ancientAdenaCost > 0 && !player.reduceAncientAdena("SevenSigns", ancientAdenaCost, this, true))
							return;
						
						player.teleportTo(x, y, z, 0);
					}
					catch (Exception e)
					{
						LOGGER.error("An error occurred while teleporting a player.", e);
					}
					break;
				
				case 16: // "I want to exchange Seal Stones for Ancient Adena.", [SevenSigns 16]
					showChatWindow(player, value, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
					break;
				
				case 17: // "Exchange Blue/Green/Red/all Seal Stones.", [SevenSigns 17 stone]
					stoneType = Integer.parseInt(command.substring(14));
					
					int stoneId = 0;
					int stoneCount = 0;
					int stoneValue = 0;
					
					String stoneColor = "";
					
					switch (stoneType)
					{
						case 1:
							stoneColor = "blue";
							stoneId = SevenSignsManager.SEAL_STONE_BLUE_ID;
							stoneValue = SevenSignsManager.SEAL_STONE_BLUE_VALUE;
							break;
						
						case 2:
							stoneColor = "green";
							stoneId = SevenSignsManager.SEAL_STONE_GREEN_ID;
							stoneValue = SevenSignsManager.SEAL_STONE_GREEN_VALUE;
							break;
						
						case 3:
							stoneColor = "red";
							stoneId = SevenSignsManager.SEAL_STONE_RED_ID;
							stoneValue = SevenSignsManager.SEAL_STONE_RED_VALUE;
							break;
						
						case 4:
							ItemInstance blueStonesAll = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_BLUE_ID);
							ItemInstance greenStonesAll = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_GREEN_ID);
							ItemInstance redStonesAll = player.getInventory().getItemByItemId(SevenSignsManager.SEAL_STONE_RED_ID);
							
							int blueStoneCountAll = blueStonesAll == null ? 0 : blueStonesAll.getCount();
							int greenStoneCountAll = greenStonesAll == null ? 0 : greenStonesAll.getCount();
							int redStoneCountAll = redStonesAll == null ? 0 : redStonesAll.getCount();
							int ancientAdenaRewardAll = 0;
							
							ancientAdenaRewardAll = SevenSignsManager.calcScore(blueStoneCountAll, greenStoneCountAll, redStoneCountAll);
							
							if (ancientAdenaRewardAll == 0)
							{
								showChatWindow(player, 18, (this instanceof DawnPriest) ? "dawn_no_stones" : "dusk_no_stones", false);
								return;
							}
							
							if (blueStoneCountAll > 0)
								player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_BLUE_ID, blueStoneCountAll, this, true);
							
							if (greenStoneCountAll > 0)
								player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_GREEN_ID, greenStoneCountAll, this, true);
							
							if (redStoneCountAll > 0)
								player.destroyItemByItemId("SevenSigns", SevenSignsManager.SEAL_STONE_RED_ID, redStoneCountAll, this, true);
							
							player.addAncientAdena("SevenSigns", ancientAdenaRewardAll, this, true);
							
							showChatWindow(player, 18, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
							return;
					}
					
					ItemInstance stoneInstance = player.getInventory().getItemByItemId(stoneId);
					if (stoneInstance != null)
						stoneCount = stoneInstance.getCount();
					
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(SevenSignsManager.SEVEN_SIGNS_HTML_PATH + ((this instanceof DawnPriest) ? "signs_17_dawn.htm" : "signs_17_dusk.htm"));
					html.replace("%stoneColor%", stoneColor);
					html.replace("%stoneValue%", stoneValue);
					html.replace("%stoneCount%", stoneCount);
					html.replace("%stoneItemId%", stoneId);
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				
				case 18: // Exchange Seal Stones for Ancient Adena, [SevenSigns 18 id amount]
					itemId = Integer.parseInt(st.nextToken());
					amount = 0;
					try
					{
						// Note: We must use substring, as the amount parameter (from edit HTML component) may return number with spaces inside.
						amount = Integer.parseInt(command.substring(19).trim());
					}
					catch (Exception e)
					{
						showChatWindow(player, 18, (this instanceof DawnPriest) ? "dawn_failed" : "dusk_failed", false);
						return;
					}
					
					// Get stone item instance and check it.
					ItemInstance convertItem = player.getInventory().getItemByItemId(itemId);
					if (convertItem == null)
					{
						showChatWindow(player, 18, (this instanceof DawnPriest) ? "dawn_no_stones" : "dusk_no_stones", false);
						return;
					}
					
					// Check amount.
					if (amount <= 0 || amount > convertItem.getCount())
					{
						showChatWindow(player, 18, (this instanceof DawnPriest) ? "dawn_low_stones" : "dusk_low_stones", false);
						return;
					}
					
					// Calculate reward.
					int reward = 0;
					switch (itemId)
					{
						case SevenSignsManager.SEAL_STONE_BLUE_ID:
							reward = SevenSignsManager.calcScore(amount, 0, 0);
							break;
						
						case SevenSignsManager.SEAL_STONE_GREEN_ID:
							reward = SevenSignsManager.calcScore(0, amount, 0);
							break;
						
						case SevenSignsManager.SEAL_STONE_RED_ID:
							reward = SevenSignsManager.calcScore(0, 0, amount);
							break;
					}
					
					// Destroy seal stone and add reward.
					if (!player.destroyItemByItemId("SevenSigns", itemId, amount, this, true))
						return;
					
					player.addAncientAdena("SevenSigns", reward, this, true);
					
					showChatWindow(player, 18, (this instanceof DawnPriest) ? "dawn" : "dusk", false);
					break;
				
				case 19: // "Seal of Avarice/Gnosis/Strife.", [SevenSigns 19 cabal seal]
					cabal = CabalType.VALUES[Integer.parseInt(st.nextToken())];
					seal = SealType.VALUES[Integer.parseInt(st.nextToken())];
					
					showChatWindow(player, value, seal.getShortName() + "_" + cabal.getShortName(), false);
					break;
				
				case 20: // "What is the status of the seals?", "Go back.", [SevenSigns 20 cabal] (cabal is not used)
					final StringBuilder sb = new StringBuilder();
					
					if (this instanceof DawnPriest)
						sb.append("<html><body>Priest of Dawn:<br><font color=\"LEVEL\">[ Seal Status ]</font><br>");
					else
						sb.append("<html><body>Dusk Priestess:<br><font color=\"LEVEL\">[ Status of the Seals ]</font><br>");
					
					for (Entry<SealType, CabalType> entry : SevenSignsManager.getInstance().getSealOwners().entrySet())
					{
						final SealType s = entry.getKey();
						final CabalType so = entry.getValue();
						
						if (so != CabalType.NORMAL)
							sb.append("[" + s.getFullName() + ": " + so.getFullName() + "]<br>");
						else
							sb.append("[" + s.getFullName() + ": Nothingness]<br>");
					}
					
					sb.append("<a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">Go back.</a></body></html>");
					
					html = new NpcHtmlMessage(getObjectId());
					html.setHtml(sb.toString());
					player.sendPacket(html);
					break;
				
				default:
					showChatWindow(player, value, null, false);
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		final int npcId = getTemplate().getNpcId();
		String filename = SevenSignsManager.SEVEN_SIGNS_HTML_PATH;
		
		final CabalType playerCabal = SevenSignsManager.getInstance().getPlayerCabal(player.getObjectId());
		final CabalType winningCabal = SevenSignsManager.getInstance().getWinningCabal();
		
		switch (npcId)
		{
			case 31092: // Black Marketeer of Mammon
				filename += "blkmrkt_1.htm";
				break;
			
			case 31113: // Merchant of Mammon
				final CabalType sealAvariceOwner = SevenSignsManager.getInstance().getSealOwner(SealType.AVARICE);
				switch (winningCabal)
				{
					case DAWN:
						if (playerCabal != winningCabal || playerCabal != sealAvariceOwner)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
					
					case DUSK:
						if (playerCabal != winningCabal || playerCabal != sealAvariceOwner)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
					
					default:
						player.sendPacket(SystemMessageId.QUEST_EVENT_PERIOD);
						return;
				}
				filename += "mammmerch_1.htm";
				break;
			
			case 31126: // Blacksmith of Mammon
				final CabalType sealGnosisOwner = SevenSignsManager.getInstance().getSealOwner(SealType.GNOSIS);
				switch (winningCabal)
				{
					case DAWN:
						if (playerCabal != winningCabal || playerCabal != sealGnosisOwner)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
					
					case DUSK:
						if (playerCabal != winningCabal || playerCabal != sealGnosisOwner)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
				}
				filename += "mammblack_1.htm";
				break;
			
			default:
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = (getHtmlPath(npcId, val));
				break;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showChatWindow(Player player, int val, String suffix, boolean isDescription)
	{
		String filename = SevenSignsManager.SEVEN_SIGNS_HTML_PATH;
		
		filename += (isDescription) ? "desc_" + val : "signs_" + val;
		filename += (suffix != null) ? "_" + suffix + ".htm" : ".htm";
		
		showChatWindow(player, filename);
	}
}