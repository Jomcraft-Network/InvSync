package net.jomcraft.invsync;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class MessageSendConnectedPlayers {

	private static final Charset defaultCharset = Charset.defaultCharset();

	public ArrayList<String[]> playerData = new ArrayList<String[]>();
	
	public MessageSendConnectedPlayers(ArrayList<String[]> playerData) {
		this.playerData = playerData;
	}

	public static void encode(final MessageSendConnectedPlayers msg, final PacketBuffer packetBuffer) {
		packetBuffer.writeInt(msg.playerData.size());
		for (int i = 0; i < msg.playerData.size(); i++) {
			packetBuffer.writeInt(msg.playerData.get(i)[0].getBytes().length);
			packetBuffer.writeBytes(msg.playerData.get(i)[0].getBytes());
			packetBuffer.writeInt(msg.playerData.get(i)[1].getBytes().length);
			packetBuffer.writeBytes(msg.playerData.get(i)[1].getBytes());
			
			packetBuffer.writeInt(msg.playerData.get(i)[2].getBytes().length);
			packetBuffer.writeBytes(msg.playerData.get(i)[2].getBytes());
			packetBuffer.writeInt(msg.playerData.get(i)[3].getBytes().length);
			packetBuffer.writeBytes(msg.playerData.get(i)[3].getBytes());
		}
	}
	
	public static MessageSendConnectedPlayers decode(final PacketBuffer packetBuffer) {
		ArrayList<String[]> playerData = new ArrayList<String[]>();
		final int len = packetBuffer.readInt();
		for (int i = 0; i < len; i++) {
			String[] tmp = new String[4];
			tmp[0] = packetBuffer.readBytes(packetBuffer.readInt()).toString(defaultCharset);
			tmp[1] = packetBuffer.readBytes(packetBuffer.readInt()).toString(defaultCharset);
			tmp[2] = packetBuffer.readBytes(packetBuffer.readInt()).toString(defaultCharset);
			tmp[3] = packetBuffer.readBytes(packetBuffer.readInt()).toString(defaultCharset);
			playerData.add(tmp);
		}
		return new MessageSendConnectedPlayers(playerData);
	}
	
	public static void handle(final MessageSendConnectedPlayers msg, final Supplier<NetworkEvent.Context> contextSupplier) {
		contextSupplier.get().enqueueWork(() -> {
			InvSync.tabOverlayGui.remotePlayerInfo.clear();
				for (int i = 0; i < msg.playerData.size(); i++) {
					InvSync.tabOverlayGui.remotePlayerInfo.add(InvSync.tabOverlayGui.makeFakePlayerNetworkInfo(msg.playerData.get(i)[0], msg.playerData.get(i)[1], Integer.parseInt(msg.playerData.get(i)[2]), Integer.parseInt(msg.playerData.get(i)[3])));
				}
			
		});
		contextSupplier.get().setPacketHandled(true);
	}

}
