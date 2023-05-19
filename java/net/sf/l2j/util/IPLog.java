package net.sf.l2j.util;

/**
 * @author TerryMaster Pro Jr
 *
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.math.MathUtil;

public class IPLog{
	static {
		new File("log/Player Log/IPLog").mkdirs();
	}

	private static final Logger _log = Logger.getLogger(IPLog.class.getName());

	public static void auditGMAction(String gmName, String action, String Hwid, String params) {
		final File file = new File("log/Player Log/IPLog/" + gmName + ".txt");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
			}
		}

		try (FileWriter save = new FileWriter(file, true)) {
			save.write(MathUtil.formatDate(new Date(), "dd/MM/yyyy H:mm:ss") + " >> IP: [" + action + "] >> HWID: [" + Hwid + "]\r\n");
		} catch (IOException e) {
			_log.log(Level.SEVERE, "IPLog for Player " + gmName + " could not be saved: ", e);
		}
	}

	public static void auditGMAction(String gmName, String action, String Hwid) {
		auditGMAction(gmName, action, Hwid, "");
	}
}