package net.jomcraft.invsync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.jomcraft.jclib.JCLib;
import net.jomcraft.jclib.MySQL;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class EventHandler {
	
	public static HashMap<String, HashMap<Integer, ArrayList<SQLItem>>> respawnQueue = new HashMap<String, HashMap<Integer, ArrayList<SQLItem>>>();
	private static boolean shuttingDown = false;
	public static HashMap<String, ScheduledFuture<?>> updateTimer = new HashMap<String, ScheduledFuture<?>>();

	@SubscribeEvent
	public void stopServer(FMLServerStoppedEvent event) {
		shuttingDown = true;
		SQLHandler.executor.shutdown();
		SQLHandler.scheduledExecutor.shutdown();
		
		if(SQLHandler.executor.getActiveCount() < 2 && SQLHandler.executor.getQueue().size() == 0) {
			JCLib.getLog().info(InvSync.MODID + " is ready to shut down the MySQL-connection");
			JCLib.readyForShutdown(InvSync.MODID);
		}
		
	}

	@SubscribeEvent
	public void logoutEvent(PlayerLoggedOutEvent event) {
		final PlayerEntity player = event.getPlayer();
		final String standardUUID = player.getGameProfile().getId().toString();
		if(updateTimer.containsKey(standardUUID)) {
			updateTimer.get(standardUUID).cancel(false);
			updateTimer.remove(standardUUID);
		}
		if(respawnQueue.containsKey(standardUUID))
			return;
		
		ArrayList<SQLItem> mainInventory = new ArrayList<SQLItem>();
		ArrayList<SQLItem> armorInventory = new ArrayList<SQLItem>();
		ArrayList<SQLItem> offHandInventory = new ArrayList<SQLItem>();

		
		final PlayerInventory inv = player.inventory;
		final NonNullList<ItemStack> armor = inv.armorInventory;
		final NonNullList<ItemStack> offHand = inv.offHandInventory;
		final NonNullList<ItemStack> main = inv.mainInventory;

		for (int slot = 0; slot < main.size(); slot++) {

			final ItemStack itemStack = main.get(slot);

			String tagString = "";

			if (itemStack.hasTag())
				tagString = itemStack.getTag().toString();

			final String registry = itemStack.getItem().getRegistryName().toString();

			final int count = itemStack.getCount();

			if (!registry.equals("minecraft:air"))
				mainInventory.add(new SQLItem(slot, registry, count, tagString));
		}

		for (int slot = 0; slot < armor.size(); slot++) {

			final ItemStack itemStack = armor.get(slot);

			String tagString = "";

			if (itemStack.hasTag())
				tagString = itemStack.getTag().toString();

			final String registry = itemStack.getItem().getRegistryName().toString();

			final int count = itemStack.getCount();

			if (!registry.equals("minecraft:air"))
				armorInventory.add(new SQLItem(slot, registry, count, tagString));
		}

		for (int slot = 0; slot < offHand.size(); slot++) {

			final ItemStack itemStack = offHand.get(slot);

			String tagString = "";

			if (itemStack.hasTag())
				tagString = itemStack.getTag().toString();

			final String registry = itemStack.getItem().getRegistryName().toString();

			final int count = itemStack.getCount();

			if (!registry.equals("minecraft:air"))
				offHandInventory.add(new SQLItem(slot, registry, count, tagString));
		}

		String uuid = player.getGameProfile().getId().toString().replace("-", "");
		SQLHandler.createIfNonExistant(uuid);
		SQLHandler.executor.submit(() -> {
			synchronized (MySQL.con) {
				SQLHandler.uploadInventories(uuid, mainInventory, armorInventory, offHandInventory);
				if(EventHandler.shuttingDown && SQLHandler.executor.getActiveCount() < 2 && SQLHandler.executor.getQueue().size() == 0) {
					JCLib.getLog().info(InvSync.MODID + " is ready to shut down the MySQL-connection");
					JCLib.readyForShutdown(InvSync.MODID);
				}
			}
		});
	}
	
	@SubscribeEvent
	public void respawnEvent(PlayerRespawnEvent event) {
		PlayerEntity player = event.getPlayer();
		String uuid = player.getGameProfile().getId().toString();
		if(respawnQueue.containsKey(uuid)) {
			HashMap<Integer, ArrayList<SQLItem>> list = respawnQueue.get(uuid);
			for (int i = 0; i < 3; i++) {
				for (SQLItem item : list.get(i)) {

					NonNullList<ItemStack> editInv = null;
					if (i == 0)
						editInv = player.inventory.mainInventory;
					else if (i == 1)
						editInv = player.inventory.armorInventory;
					else if (i == 2)
						editInv = player.inventory.offHandInventory;

					try {
						ItemStack it = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item.registry)), item.count);
						if (!item.tagString.isEmpty())
							it.setTag(JsonToNBT.getTagFromJson(item.tagString));
						editInv.set(item.slot, it);
					} catch (CommandSyntaxException e) {
						InvSync.log.error("ItemStack NBT couldn't be serialized", e);
					}
				}
			}
			respawnQueue.remove(uuid);
		}
	}

	@SubscribeEvent
	public void loginEvent(PlayerEvent.LoadFromFile event) {
		final PlayerEntity player = event.getPlayer();

		final String uuid = player.getGameProfile().getId().toString().replace("-", "");

		final boolean exists = SQLHandler.createIfNonExistant(uuid);

		if (!exists)
			return;

		HashMap<Integer, ArrayList<SQLItem>> list = SQLHandler.getInventories(uuid);

		for (int j = 0; j < player.inventory.getSizeInventory(); ++j) {
			final ItemStack itemstack = player.inventory.getStackInSlot(j);
			if (!itemstack.isEmpty()) 
				player.inventory.setInventorySlotContents(j, ItemStack.EMPTY);
			
		}
		
		if(player.isAlive()) {
			updateTimer.put(player.getGameProfile().getId().toString(),
					SQLHandler.scheduledExecutor.scheduleAtFixedRate(new Runnable() {

						@Override
						public void run() {
							synchronized (respawnQueue) {
								final PlayerEntity player = event.getPlayer();
								if (respawnQueue.containsKey(player.getGameProfile().getId().toString()))
									return;

								ArrayList<SQLItem> mainInventory = new ArrayList<SQLItem>();
								ArrayList<SQLItem> armorInventory = new ArrayList<SQLItem>();
								ArrayList<SQLItem> offHandInventory = new ArrayList<SQLItem>();

								final PlayerInventory inv = player.inventory;
								final NonNullList<ItemStack> armor = inv.armorInventory;
								final NonNullList<ItemStack> offHand = inv.offHandInventory;
								final NonNullList<ItemStack> main = inv.mainInventory;

								for (int slot = 0; slot < main.size(); slot++) {

									final ItemStack itemStack = main.get(slot);

									String tagString = "";

									if (itemStack.hasTag())
										tagString = itemStack.getTag().toString();

									final String registry = itemStack.getItem().getRegistryName().toString();

									final int count = itemStack.getCount();

									if (!registry.equals("minecraft:air"))
										mainInventory.add(new SQLItem(slot, registry, count, tagString));
								}

								for (int slot = 0; slot < armor.size(); slot++) {

									final ItemStack itemStack = armor.get(slot);

									String tagString = "";

									if (itemStack.hasTag())
										tagString = itemStack.getTag().toString();

									final String registry = itemStack.getItem().getRegistryName().toString();

									final int count = itemStack.getCount();

									if (!registry.equals("minecraft:air"))
										armorInventory.add(new SQLItem(slot, registry, count, tagString));
								}

								for (int slot = 0; slot < offHand.size(); slot++) {

									final ItemStack itemStack = offHand.get(slot);

									String tagString = "";

									if (itemStack.hasTag())
										tagString = itemStack.getTag().toString();

									final String registry = itemStack.getItem().getRegistryName().toString();

									final int count = itemStack.getCount();

									if (!registry.equals("minecraft:air"))
										offHandInventory.add(new SQLItem(slot, registry, count, tagString));
								}

								String uuid = player.getGameProfile().getId().toString().replace("-", "");
								synchronized (MySQL.con) {
									SQLHandler.createIfNonExistant(uuid);
									SQLHandler.uploadInventories(uuid, mainInventory, armorInventory, offHandInventory);
								}
							}
						}
					}, 5, 5, TimeUnit.MINUTES));
			
			for (int i = 0; i < 3; i++) {
				for (SQLItem item : list.get(i)) {

					NonNullList<ItemStack> editInv = null;
					if (i == 0)
						editInv = player.inventory.mainInventory;
					else if (i == 1)
						editInv = player.inventory.armorInventory;
					else if (i == 2)
						editInv = player.inventory.offHandInventory;

					try {
						ItemStack it = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item.registry)), item.count);
						if (!item.tagString.isEmpty())
							it.setTag(JsonToNBT.getTagFromJson(item.tagString));
						editInv.set(item.slot, it);
					} catch (CommandSyntaxException e) {
						InvSync.log.error("ItemStack NBT couldn't be serialized", e);
					}
				}
			}
		} else {
			respawnQueue.put(player.getGameProfile().getId().toString(), list);
		}

	}

	public static class SQLItem {

		public final int slot;
		public final String registry;
		public final int count;
		public final String tagString;

		public SQLItem(int slot, String registry, int count, String tagString) {
			this.slot = slot;
			this.registry = registry;
			this.count = count;
			this.tagString = tagString;
		}

	}
}