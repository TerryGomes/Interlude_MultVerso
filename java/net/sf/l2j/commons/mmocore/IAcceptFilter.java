package net.sf.l2j.commons.mmocore;

import java.net.Socket;

public interface IAcceptFilter
{
	public boolean accept(Socket socket);
}