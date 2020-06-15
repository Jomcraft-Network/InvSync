package net.jomcraft.invsync;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import net.jomcraft.jclib.JCLib;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(value = InvSync.MODID)
public class InvSync {

	public static final String MODID = "invsync";
	public static final Logger log = LogManager.getLogger(InvSync.MODID);
	public static Timer aliveTimer = new Timer();
	public static final String VERSION = getModVersion();
	public static InvSync instance;
	
	public InvSync() {
		instance = this;
		MinecraftForge.EVENT_BUS.register(new EventHandler());
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postInit);
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> "OHNOES\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31", (test2, test) -> true));
	}
	
	public void postInit(FMLLoadCompleteEvent event) {
		if(!JCLib.databaseInitialized)
			JCLib.connectMySQL();
		
		startKeepAlive();
	}

	private static void startKeepAlive() {
		aliveTimer.scheduleAtFixedRate(new KeepAlive(), 10 * 60 * 1000, 10 * 60 * 1000);
	}
	
	public static class KeepAlive extends TimerTask {
		public void run() {
			SQLHandler.getVersion();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static String getModVersion() {
		//Stupid FG 3 workaround
		TomlParser parser = new TomlParser();
		InputStream stream = JCLib.class.getClassLoader().getResourceAsStream("META-INF/mods.toml");
		CommentedConfig file = parser.parse(stream);

		return ((ArrayList<CommentedConfig>) file.get("mods")).get(0).get("version");
	}
	
	public static InvSync getInstance() {
		return instance;
	}
}