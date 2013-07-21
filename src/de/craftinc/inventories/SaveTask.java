package de.craftinc.inventories;

import de.craftinc.inventories.utils.ConfigurationKeys;
import de.craftinc.inventories.utils.Logger;

import java.util.TimerTask;

public class SaveTask extends TimerTask
{
    @Override
    public void run()
    {
        WorldInventories plugin = WorldInventories.getSharedInstance();

        if (plugin.getConfig().getBoolean(ConfigurationKeys.logSaveTimerMessagesKey)) {
            int saveTimeInterval = plugin.getConfig().getInt(ConfigurationKeys.saveTimerIntervalKey);
            Logger.logStandard("Timer: Saving player information. New save due in " + saveTimeInterval + " seconds.");
	    }
        
        plugin.savePlayers(plugin.getConfig().getBoolean(ConfigurationKeys.logSaveTimerMessagesKey));
    }
}
