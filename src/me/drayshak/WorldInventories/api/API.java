package me.drayshak.WorldInventories.api;

import me.drayshak.WorldInventories.InventoryLoadType;
import java.util.ArrayList;
import java.util.List;
import me.drayshak.WorldInventories.Group;
import me.drayshak.WorldInventories.PlayerStats;
import me.drayshak.WorldInventories.WorldInventories;
import static me.drayshak.WorldInventories.WorldInventories.groups;
import org.bukkit.inventory.ItemStack;

public class API {
    private final WorldInventories plugin;
    
    public API(final WorldInventories plugin)
    {
       this.plugin = plugin;
    }    
    
    /*
     * Returns a list of groups currently loaded
     */
    public List<Group> getGroups()
    {
        return groups;
    }
    
    /*
     * Loads and returns inventories based on the InventoryLoadType given.
     * Type and locations are defined in InventoryStoredType.
     * Loading an Enderchest stores it in type INVENTORY
     */
    public ArrayList<ItemStack[]> getPlayerInventory(String player, Group group, InventoryLoadType type)
    {
        return plugin.loadPlayerInventory(player, group, type);
    }
    
    /*
     * Loads and returns all player stats.
     */
    public PlayerStats getPlayerStats(String player, Group group)
    {
        return plugin.loadPlayerStats(player, group);
    }
}
