package net.jomcraft.invsync;

import java.util.ArrayList;
import java.util.HashMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.jomcraft.jclib.MySQL;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class EventHandler {

	@SubscribeEvent
	public void stopServer(FMLServerStoppedEvent event) {

		MySQL.close();
		SQLHandler.executor.shutdown();

	}

	@SubscribeEvent
	public void logoutEvent(PlayerLoggedOutEvent event) {

		ArrayList<SQLItem> mainInventory = new ArrayList<SQLItem>();
		ArrayList<SQLItem> armorInventory = new ArrayList<SQLItem>();
		ArrayList<SQLItem> offHandInventory = new ArrayList<SQLItem>();

		PlayerEntity player = event.getPlayer();
		PlayerInventory inv = player.inventory;
		NonNullList<ItemStack> armor = inv.armorInventory;
		NonNullList<ItemStack> offHand = inv.offHandInventory;
		NonNullList<ItemStack> main = inv.mainInventory;

		for (int slot = 0; slot < main.size(); slot++) {

			ItemStack itemStack = main.get(slot);

			String tagString = "";

			if (itemStack.hasTag())
				tagString = itemStack.getTag().toString();

			String registry = itemStack.getItem().getRegistryName().toString();

			int count = itemStack.getCount();

			if (!registry.equals("minecraft:air"))
				mainInventory.add(new SQLItem(slot, registry, count, tagString));
		}

		for (int slot = 0; slot < armor.size(); slot++) {

			ItemStack itemStack = armor.get(slot);

			String tagString = "";

			if (itemStack.hasTag())
				tagString = itemStack.getTag().toString();

			String registry = itemStack.getItem().getRegistryName().toString();

			int count = itemStack.getCount();

			if (!registry.equals("minecraft:air"))
				armorInventory.add(new SQLItem(slot, registry, count, tagString));
		}

		for (int slot = 0; slot < offHand.size(); slot++) {

			ItemStack itemStack = offHand.get(slot);

			String tagString = "";

			if (itemStack.hasTag())
				tagString = itemStack.getTag().toString();

			String registry = itemStack.getItem().getRegistryName().toString();

			int count = itemStack.getCount();

			if (!registry.equals("minecraft:air"))
				offHandInventory.add(new SQLItem(slot, registry, count, tagString));
		}

		String uuid = player.getGameProfile().getId().toString();
		uuid = uuid.replace("-", "");
		SQLHandler.createIfNonExistant(uuid);
		SQLHandler.uploadInventories(uuid, mainInventory, armorInventory, offHandInventory);
	}

	@SubscribeEvent
	public void loginEvent(PlayerEvent.LoadFromFile event) {
		PlayerEntity player = event.getPlayer();

		String uuid = player.getGameProfile().getId().toString();
		uuid = uuid.replace("-", "");

		boolean exists = SQLHandler.createIfNonExistant(uuid);

		if (!exists)
			return;

		HashMap<Integer, ArrayList<SQLItem>> list = SQLHandler.getInventories(uuid);

		for (int j = 0; j < player.inventory.getSizeInventory(); ++j) {
			ItemStack itemstack = player.inventory.getStackInSlot(j);
			if (!itemstack.isEmpty()) 
				player.inventory.setInventorySlotContents(j, ItemStack.EMPTY);
			
		}

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
					e.printStackTrace();

				}
			}
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