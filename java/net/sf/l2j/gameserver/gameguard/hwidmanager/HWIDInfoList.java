package net.sf.l2j.gameserver.gameguard.hwidmanager;

/**
 * @author TerryMaster Pro Jr
 *
 */
public class HWIDInfoList{
	private final int _id;
	private String HWID;
	private int count;
	private int playerID;
	private String login;
	private LockType lockType;

	public HWIDInfoList(int id) {
		_id = id;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getPlayerID() {
		return playerID;
	}

	public void setPlayerID(int playerID) {
		this.playerID = playerID;
	}

	public String getHWID() {
		return HWID;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setHWID(String HWID) {
		this.HWID = HWID;
	}

	public LockType getLockType() {
		return lockType;
	}

	public String getLogin() {
		return login;
	}

	public void setLockType(LockType lockType) {
		this.lockType = lockType;
	}

	public int get_id() {
		return _id;
	}

	public void setHwids(String hwid) {
		HWID = hwid;
		count = 1;
	}

	public static enum LockType {
		PLAYER_LOCK,
		ACCOUNT_LOCK,
		NONE;
	}
}