package net.jomcraft.invsync;

import java.util.function.Supplier;
import net.jomcraft.jclib.MySQL;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public class MessageRequestConnectedPlayers {

	public static void encode(final MessageRequestConnectedPlayers msg, final PacketBuffer packetBuffer) {
		
	}
	
	public static MessageRequestConnectedPlayers decode(final PacketBuffer packetBuffer) {
		return new MessageRequestConnectedPlayers();
	}
	
	public static void handle(final MessageRequestConnectedPlayers msg, final Supplier<NetworkEvent.Context> contextSupplier) {
		final NetworkEvent.Context context = contextSupplier.get();
		contextSupplier.get().enqueueWork(() -> {
			SQLHandler.executor.execute(new Runnable() {

				@Override
				public void run() {
					synchronized (MySQL.con) {
						final MessageSendConnectedPlayers mscp = new MessageSendConnectedPlayers(SQLHandler.getAllPlayersInDatabase());
						MessageHandler.INSTANCE.sendTo(mscp, ((ServerPlayerEntity) (context.getSender())).connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
					}
				}
			});
		});
		contextSupplier.get().setPacketHandled(true);
	}

}
