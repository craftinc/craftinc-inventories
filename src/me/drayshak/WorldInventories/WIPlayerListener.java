package me.drayshak.WorldInventories;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class WIPlayerListener extends PlayerListener
{
    private static WorldInventories plugin;
 
    public WIPlayerListener(final WorldInventories tplugin)
    {
        plugin = tplugin;
    }
    
    @Override
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
    {
        Player player = event.getPlayer();
        
        String fromworld = event.getFrom().getName();
        String toworld = player.getLocation().getWorld().getName();
        
        if(!fromworld.equals(toworld))
        {
            WorldInventories.logStandard("Player " + player.getName() + " moved from world " + fromworld + " to " + toworld);
            
            Group fromgroup = WorldInventories.findFirstGroupForWorld(fromworld);
            Group togroup = WorldInventories.findFirstGroupForWorld(toworld);
            
            plugin.savePlayerInventory(player, fromgroup, plugin.getPlayerInventory(player));
      
            String fromgroupname = "default";
            if(fromgroup != null) fromgroupname = fromgroup.getName();             
            
            String togroupname = "default";
            if(togroup != null) togroupname = togroup.getName();            

            if(!fromgroupname.equals(togroupname))
            {
                plugin.setPlayerInventory(player, plugin.loadPlayerInventory(player, togroup));
                if(WorldInventories.doNotifications)
                {
                    player.sendMessage(ChatColor.GREEN + "Changed inventory set to group: " + togroupname);
                }
            }
            else
            {
                if(WorldInventories.doNotifications)
                {
                    player.sendMessage(ChatColor.GREEN + "No inventory change necessary for group: " + togroupname);
                }
            }
        }
    }
    
    @Override
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        String world = player.getLocation().getWorld().getName();
        
        WorldInventories.logStandard("Player " + player.getName() + " quit from world: " + world);
        
        Group tGroup = WorldInventories.findFirstGroupForWorld(world);
        
        // Don't save if we don't care where we are (default group)
        if(tGroup != null)
        {    
            WorldInventories.logStandard("Saving inventory of " + player.getName());
            plugin.savePlayerInventory(player, WorldInventories.findFirstGroupForWorld(world), plugin.getPlayerInventory(player));
        }
    }
}
