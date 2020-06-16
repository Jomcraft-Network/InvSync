package net.jomcraft.invsync;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import net.jomcraft.invsync.EventHandler.SQLItem;
import net.jomcraft.jclib.MySQL;

public class SQLHandler {
	
	public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
	
	public static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
	
	public static boolean createIfNonExistant(final String uuid) {
		
		try {
			final ResultSet exists = MySQL.con.getMetaData().getTables(null, null, uuid, null);

			if(!exists.next()) {
				MySQL.update("CREATE TABLE " + uuid + " (ID INT NOT NULL, SLOT INT NOT NULL, REGISTRY VARCHAR(128) NOT NULL, COUNT INT NOT NULL, TAGSTRING TEXT NOT NULL);");
				return false;
			}
			return true;
		} catch (NullPointerException e) {
			return createIfNonExistant(uuid);
		} catch (Exception e) {
			InvSync.log.error("An error occured while creating a player's custom MySQL table: ", e);
		}
		return false;
	}
	
	public static void uploadInventories(final String uuid, final ArrayList<SQLItem> mainInventory, final ArrayList<SQLItem> armorInventory, final ArrayList<SQLItem> offHandInventory) {
		try {
			MySQL.update("TRUNCATE TABLE " + uuid + ";");
		} catch (ClassNotFoundException | SQLException e1) {
			InvSync.log.error("Was not able to clean the user table: ", e1);
		}
		try (final PreparedStatement stmt = MySQL.con.prepareStatement("INSERT INTO " + uuid + "(ID, SLOT, REGISTRY, COUNT, TAGSTRING) VALUES (?, ?, ?, ?, ?);")) {

			MySQL.con.setAutoCommit(false);

			for (SQLItem item : mainInventory) {
				stmt.setInt(1, 0);
				stmt.setInt(2, item.slot);
				stmt.setString(3, item.registry);
				stmt.setInt(4, item.count);
				stmt.setString(5, item.tagString);
				stmt.addBatch();
			}

			for (SQLItem item : armorInventory) {
				stmt.setInt(1, 1);
				stmt.setInt(2, item.slot);
				stmt.setString(3, item.registry);
				stmt.setInt(4, item.count);
				stmt.setString(5, item.tagString);
				stmt.addBatch();
			}

			for (SQLItem item : offHandInventory) {
				stmt.setInt(1, 2);
				stmt.setInt(2, item.slot);
				stmt.setString(3, item.registry);
				stmt.setInt(4, item.count);
				stmt.setString(5, item.tagString);
				stmt.addBatch();
			}

			stmt.executeBatch();
			MySQL.con.commit();

		} catch (NullPointerException e) {
			uploadInventories(uuid, offHandInventory, offHandInventory, offHandInventory);
		} catch (Exception e) {
			InvSync.log.error("An error occured while saving a player's inventory: ", e);
		} finally {
			try {
				MySQL.con.setAutoCommit(true);
			} catch (SQLException e) {
				InvSync.log.error("An error occured while saving a player's inventory: ", e);
			}
		}
	}
	
	public static HashMap<Integer, ArrayList<SQLItem>> getInventories(final String uuid) {
		HashMap<Integer, ArrayList<SQLItem>> items = new HashMap<Integer, ArrayList<SQLItem>>();
		ArrayList<SQLItem> main = new ArrayList<SQLItem>();
		ArrayList<SQLItem> armor = new ArrayList<SQLItem>();
		ArrayList<SQLItem> off = new ArrayList<SQLItem>();
		try (final ResultSet rs = MySQL.query("SELECT * FROM " + uuid + ";")) {

			while (rs.next()) {
				if(rs.getInt("ID") == 0)
						main.add(new SQLItem(rs.getInt("SLOT"), rs.getString("REGISTRY"), rs.getInt("COUNT"), rs.getString("TAGSTRING")));
				else if(rs.getInt("ID") == 1)
						armor.add(new SQLItem(rs.getInt("SLOT"), rs.getString("REGISTRY"), rs.getInt("COUNT"), rs.getString("TAGSTRING")));
				else if(rs.getInt("ID") == 2)
						off.add(new SQLItem(rs.getInt("SLOT"), rs.getString("REGISTRY"), rs.getInt("COUNT"), rs.getString("TAGSTRING")));
			}
			
			items.put(0, main);
			items.put(1, armor);
			items.put(2, off);

			return items;
		} catch (NullPointerException e) {
			return getInventories(uuid);
		} catch (Exception e) {
			InvSync.log.error("An error occured while downloading a player's inventory: ", e);
		}
		return items;
	}
}