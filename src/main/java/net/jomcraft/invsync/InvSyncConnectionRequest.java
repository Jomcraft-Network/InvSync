package net.jomcraft.invsync;

import java.sql.SQLException;
import net.jomcraft.jclib.DBRequestHandler;
import net.jomcraft.jclib.JCLib;
import net.jomcraft.jclib.MySQL;

public class InvSyncConnectionRequest implements DBRequestHandler {

	@Override
	public DBRequestHandler establishCon(MySQL connection) {
		InvSync.mysql = connection;
		return this;
	}

	@Override
	public void sendVoidQuery(String query) {
		try {
			InvSync.mysql.update(query);
		} catch (ClassNotFoundException | SQLException e) {
			JCLib.getLog().error("Exception while keeping alive: ", e);
		}
	}

	@Override
	public void shutdown() {
		InvSync.mysql.close();
	}
}