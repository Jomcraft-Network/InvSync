package net.jomcraft.invsync;

import com.google.common.eventbus.Subscribe;
import net.jomcraft.jclib.events.DBConnectEvent;
import net.minecraftforge.eventbus.api.Event.Result;

public class EventListener {

	@Subscribe
    public void dbSetupEvent(DBConnectEvent event) {
       if(event.getDBName().equals("InvSync") && event.getResult() == Result.ALLOW) {
    	   InvSync.log.info("InvSync database connection successfully established");
       }
    }
	
}