package net.jomcraft.invsync;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jomcraft.invsync.EventHandler.SQLItem;

public class SQLHandler {
	
	public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
	
	public static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
	
	public static boolean createPlayerInventoryTableIfNonExistant(final String uuid) {
		
		try {
			final ResultSet exists = InvSync.mysql.con.getMetaData().getTables(null, null, uuid, null);

			if(!exists.next()) {
				InvSync.mysql.update("CREATE TABLE " + uuid + " (ID INT NOT NULL, SLOT INT NOT NULL, REGISTRY VARCHAR(128) NOT NULL, COUNT INT NOT NULL, TAGSTRING TEXT NOT NULL);");
				return false;
			}
			return true;
		} catch (NullPointerException e) {
			Callable<Boolean> task = new Callable<Boolean>() {
				public Boolean call() {
					return createPlayerInventoryTableIfNonExistant(uuid);
				}
			};
			
			Future<Boolean> result = scheduledExecutor.schedule(task, 5, TimeUnit.SECONDS);
			
			try {
				return result.get();
			} catch (InterruptedException | ExecutionException e1) {
				InvSync.log.error("An error occured while creating a player's custom MySQL table: ", e1);
			}
		} catch (Exception e) {
			InvSync.log.error("An error occured while creating a player's custom MySQL table: ", e);
		}
		return false;
	}
	
	public static void uploadInventories(final String uuid, final ArrayList<SQLItem> mainInventory, final ArrayList<SQLItem> armorInventory, final ArrayList<SQLItem> offHandInventory) {
		try {
			InvSync.mysql.update("TRUNCATE TABLE " + uuid + ";");
		} catch (ClassNotFoundException | SQLException e1) {
			InvSync.log.error("Was not able to clean the user table: ", e1);
		}
		try (final PreparedStatement stmt = InvSync.mysql.con.prepareStatement("INSERT INTO " + uuid + "(ID, SLOT, REGISTRY, COUNT, TAGSTRING) VALUES (?, ?, ?, ?, ?);")) {

			InvSync.mysql.con.setAutoCommit(false);

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
			InvSync.mysql.con.commit();

		} catch (NullPointerException e) {
			Runnable task = new Runnable() {
				public void run() {
					uploadInventories(uuid, offHandInventory, offHandInventory, offHandInventory);
				}
			};
			
			scheduledExecutor.schedule(task, 5, TimeUnit.SECONDS);
		} catch (Exception e) {
			InvSync.log.error("An error occured while saving a player's inventory: ", e);
		} finally {
			try {
				InvSync.mysql.con.setAutoCommit(true);
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
		try (final ResultSet rs = InvSync.mysql.query("SELECT * FROM " + uuid + ";")) {

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
			Callable<HashMap<Integer, ArrayList<SQLItem>>> task = new Callable<HashMap<Integer, ArrayList<SQLItem>>>() {
				public HashMap<Integer, ArrayList<SQLItem>> call() {
					return getInventories(uuid);
				}
			};
			
			Future<HashMap<Integer, ArrayList<SQLItem>>> result = scheduledExecutor.schedule(task, 5, TimeUnit.SECONDS);
			
			try {
				return result.get();
			} catch (InterruptedException | ExecutionException e1) {
				InvSync.log.error("An error occured while downloading a player's inventory: ", e1);
			}
			
		} catch (Exception e) {
			InvSync.log.error("An error occured while downloading a player's inventory: ", e);
		}
		return items;
	}
	
	public static boolean createCommonPlayersTableIfNonExistant() {

		try (final ResultSet exists = InvSync.mysql.con.getMetaData().getTables(null, null, "common_players", null)) {

			if (!exists.next()) {
				InvSync.mysql.update("CREATE TABLE common_players (UUID VARCHAR(40) NOT NULL, NAME VARCHAR(128) NOT NULL, GAMETYPE INT NOT NULL, LATENCY INT NOT NULL);");
				return false;
			}
			return true;
		} catch (Exception e) {
			InvSync.log.error("An error occured while creating the common_player MySQL table: ", e);
		}
		return false;
	}

	public static void addPlayerToDatabase(final UUID uuid, final String displayName, final int gametype, final int latency) {
		createCommonPlayersTableIfNonExistant();
		try (final PreparedStatement stmt = InvSync.mysql.con.prepareStatement("INSERT INTO common_players (UUID, NAME, GAMETYPE, LATENCY) VALUES (?, ?, ?, ?);")) {
			InvSync.mysql.con.setAutoCommit(false);

			stmt.setString(1, uuid.toString());
			stmt.setString(2, displayName);
			stmt.setInt(3, gametype);
			stmt.setInt(4, latency);
			stmt.addBatch();

			stmt.executeBatch();
			InvSync.mysql.con.commit();

		} catch (SQLException e) {
			InvSync.log.error("An error occured while adding player to common_player database: ", e);
		} finally {
			try {
				InvSync.mysql.con.setAutoCommit(true);
			} catch (SQLException e) {

			}
		}

	}

	public static void removePlayerFromDatabase(final UUID uuid) {
		createCommonPlayersTableIfNonExistant();
		try (final PreparedStatement stmt = InvSync.mysql.con.prepareStatement("DELETE FROM common_players WHERE UUID = ?;")) {
			InvSync.mysql.con.setAutoCommit(false);

			stmt.setString(1, uuid.toString());
			stmt.addBatch();

			stmt.executeBatch();
			InvSync.mysql.con.commit();

		} catch (SQLException e) {
			InvSync.log.error("An error occured while removing player from common_player databse: ", e);
		} finally {
			try {
				InvSync.mysql.con.setAutoCommit(true);
			} catch (SQLException e) {

			}
		}
	}

	public static ArrayList<String[]> getAllPlayersInDatabase() {
		createCommonPlayersTableIfNonExistant();
		try (ResultSet rs = InvSync.mysql.query("SELECT * FROM common_players ORDER BY NAME DESC;")) {
			ArrayList<String[]> playerData = new ArrayList<String[]>();
			while (rs.next()) {
				playerData.add(new String[] { rs.getString("UUID"), rs.getString("NAME"), "" + rs.getInt("GAMETYPE"), "" + rs.getInt("LATENCY") });
			}
			return playerData;
		} catch (SQLException | ClassNotFoundException e) {
			InvSync.log.error("An error occured while getting all common_players: ", e);
		}
		return new ArrayList<String[]>();
	}
}