package de.craftinc.inventories;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class Language
{
    public final static String diedMessageKey = "diedMessage";
    private final static String diedMessageDefault = "You died! Wiped inventory and stats for group: ";

    public final static String changedMessageKey = "changedMessage";
    private final static String changedMessageDefault = "Changed player information to match group: ";

    public final static String noChangeMessageKey = "noChangeMessage";
    private final static String noChangeMessageDefault = "No player information change needed to match group: ";

    public final static String loadedMessageKey = "loadedMessage";
    private final static String loadedMessageDefault = "Player information loaded for group: ";

    private final WorldInventories plugin;
    private HashMap<String, String> messages;
    
    public Language(WorldInventories plugin)
    {
        this.plugin = plugin;
        this.messages = new HashMap<String, String>();
    }
    
    public String get(String key)
    {
        return messages.get(key);
    }
    
    public boolean loadLanguages(String locale)
    {
        YamlConfiguration config = new YamlConfiguration();
        
        try {
            config.load(new File(plugin.getDataFolder().getPath(), "lang.yml"));            
        }
        catch(FileNotFoundException e) {
            plugin.saveResource("lang.yml", true);
        }
        catch(Exception e) {
            InventoriesLogger.logError("Failed to load languages, using defaults: " + e.getMessage());
        }

        messages.put(diedMessageKey, diedMessageDefault);
        messages.put(changedMessageKey, changedMessageDefault);
        messages.put(noChangeMessageKey, noChangeMessageDefault);
        messages.put(loadedMessageKey, loadedMessageDefault);
        
        try {
            boolean langExists = config.isConfigurationSection(locale);

            if (!langExists) {
                throw new Exception("Language not found!");
            }

            ConfigurationSection language = config.getConfigurationSection(locale);
            
            for(String key : messages.keySet()) {
                try {
                    messages.put(key, language.getString(key));
                }
                catch(Exception e) {
                    InventoriesLogger.logError("Failed to load language key, using default: " + key);
                }
            }
        }
        catch(Exception e) {
            InventoriesLogger.logError("Failed to load language '" + locale + "', using defaults: " + e.getMessage());
            return false;
        }
        
        return true;      
    }
}