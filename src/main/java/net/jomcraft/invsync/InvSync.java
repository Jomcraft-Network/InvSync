package net.jomcraft.invsync;

import java.io.InputStream;
import java.util.ArrayList;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import net.jomcraft.jclib.JCLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod(value = InvSync.MODID)
public class InvSync {

	public static final String MODID = "invsync";
	public static final Logger log = LogManager.getLogger(InvSync.MODID);
	public static final String VERSION = getModVersion();
	public static InvSync instance;
	
	public InvSync() {
		instance = this;
		
		DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
			MinecraftForge.EVENT_BUS.register(new EventHandler());
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postInit);
		});
		
		final String any = FMLNetworkConstants.IGNORESERVERONLY;
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> any, (test2, test) -> true));
	}
	
	public void postInit(FMLLoadCompleteEvent event) {
		
		JCLib.communicateLogin(MODID);
		
		if(!JCLib.databaseInitialized())
			JCLib.connectMySQL();
		
		JCLib.startKeepAlive(10);
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