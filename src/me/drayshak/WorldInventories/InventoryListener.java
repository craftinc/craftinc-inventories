package me.drayshak.WorldInventories;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener
{
    private final WorldInventories plugin;
    
    InventoryListener(final WorldInventories plugin)
    {
       this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        Inventory inventory = event.getInventory();
        
        if(inventory.getType() == InventoryType.ENDER_CHEST)
        {
            String player = event.getPlayer().getName();
            String world = event.getPlayer().getWorld().getName();
            
            if(WorldInventories.exempts.contains(player.toLowerCase()))
            {
                WorldInventories.logDebug("Ignoring exempt player Ender Chest open: " + player);
                return;
            }
            
            Group worldgroup = WorldInventories.findFirstGroupThenDefault(world);
            
            WorldInventories.logDebug("Ender Chest opened by " + player + " in world " + world + ", group " + worldgroup);
            inventory.setContents(plugin.loadPlayerInventory((Player)event.getPlayer(), worldgroup, me.drayshak.WorldInventories.InventoryType.ENDERCHEST).getInventory());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClosed(InventoryCloseEvent event)
    {
        Inventory inventory = event.getInventory();
        
        if(inventory.getType() == InventoryType.ENDER_CHEST)
        {
            String player = event.getPlayer().getName();
            String world = event.getPlayer().getWorld().getName();
            
            if(WorldInventories.exempts.contains(player.toLowerCase()))
            {
                WorldInventories.logDebug("Ignoring exempt player Ender Chest close: " + player);
                return;
            }            
            
            Group worldgroup = WorldInventories.findFirstGroupThenDefault(world);
            
            WorldInventories.logDebug("Ender Chest closed by " + player + " in world " + world + ", group " + worldgroup);
            
            Player pplayer = (Player)event.getPlayer();
            InventoryHelper helper = new InventoryHelper();
            helper.setArmour(null);
            helper.setInventory(inventory.getContents());
            
            plugin.savePlayerInventory(player, worldgroup, me.drayshak.WorldInventories.InventoryType.ENDERCHEST, helper);
        }        
    }
}