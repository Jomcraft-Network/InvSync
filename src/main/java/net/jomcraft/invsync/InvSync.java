package net.jomcraft.invsync;

import java.io.InputStream;
import java.util.ArrayList;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;

import net.jomcraft.jclib.ConnectionRequest;
import net.jomcraft.jclib.JCLib;
import net.jomcraft.jclib.MySQL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod(value = InvSync.MODID)
public class InvSync {

	public static final String MODID = "invsync";
	public static final Logger log = LogManager.getLogger(InvSync.MODID);
	public static final String VERSION = getModVersion();
	public static volatile MySQL mysql;
	public static InvSync instance;
	
	@OnlyIn(Dist.CLIENT)
	public static PlayerTabOverlayGui tabOverlayGui;
	
	@SuppressWarnings("deprecation")
	public InvSync() {
		instance = this;
		
		DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> {	
			MinecraftForge.EVENT_BUS.register(new EventHandler());
			JCLib.eventBus.register(new EventListener());
		});
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postInit);
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		});
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postRegistration);
		final String any = FMLNetworkConstants.IGNORESERVERONLY;
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> any, (test2, test) -> true));
	}
	
	@SuppressWarnings("resource")
	@OnlyIn(Dist.CLIENT)
	private void clientSetup(final FMLClientSetupEvent event) {
		IngameGui ig = Minecraft.getInstance().ingameGUI;
		tabOverlayGui = new PlayerTabOverlayGui(Minecraft.getInstance(), ig);
		Minecraft.getInstance().ingameGUI.overlayPlayerList = tabOverlayGui;
		// TODO: Server Names + gamemode updates
    }
	
	public void postInit(FMLLoadCompleteEvent event) {
		JCLib.startKeepAlive(10);
	}
	
	@SuppressWarnings("deprecation")
	public void postRegistration(FMLCommonSetupEvent event) {
		DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
			JCLib.communicateLogin(MODID);
			JCLib.putConnectionRequest(InvSync.MODID, new ConnectionRequest("InvSync", new InvSyncConnectionRequest()));
		});
        MessageHandler.init();
    }
	
	@SuppressWarnings("unchecked")
	public static String getModVersion() {
		//Stupid FG 3 workaround
		final TomlParser parser = new TomlParser();
		final InputStream stream = JCLib.class.getClassLoader().getResourceAsStream("META-INF/mods.toml");
		final CommentedConfig file = parser.parse(stream);

		return ((ArrayList<CommentedConfig>) file.get("mods")).get(0).get("version");
	}
	
	public static InvSync getInstance() {
		return instance;
	}
}