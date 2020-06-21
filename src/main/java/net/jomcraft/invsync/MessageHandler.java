package net.jomcraft.invsync;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class MessageHandler {
	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(InvSync.MODID, "channel"), () -> "1.0", // version that will be offered to the server
			(String s) -> s.equals("1.0"), // client accepted versions
			(String s) -> s.equals("1.0"));// server accepted versions

	public static void init() {
		int index = 0;
		INSTANCE.registerMessage(index++, MessageRequestConnectedPlayers.class, MessageRequestConnectedPlayers::encode, MessageRequestConnectedPlayers::decode, MessageRequestConnectedPlayers::handle);
		INSTANCE.registerMessage(index++, MessageSendConnectedPlayers.class, MessageSendConnectedPlayers::encode, MessageSendConnectedPlayers::decode, MessageSendConnectedPlayers::handle);
	}
}
