package me.drayshak.WorldInventories;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import uk.co.tggl.pluckerpluck.multiinv.MultiInv;
import uk.co.tggl.pluckerpluck.multiinv.MultiInvAPI;
import uk.co.tggl.pluckerpluck.multiinv.inventory.MIInventory;

public class WorldInventories extends JavaPlugin
{
    public static final Logger log = Logger.getLogger("Minecraft");
    public static PluginManager pluginManager = null;
    public static Server bukkitServer = null;
    public static ArrayList<Group> groups = null;
    public static List<String> exempts = null;
    public static Timer saveTimer = new Timer();
    public static String statsFileVersion = "v5";
    public static String inventoryFileVersion = "v5";

    public void setPlayerInventory(Player player, InventoryHelper playerInventory)
    {
        if (playerInventory != null)
        {
            player.getInventory().setContents(playerInventory.getInventory());
            player.getInventory().setArmorContents(playerInventory.getArmour());
        }
    }

    public void setPlayerStats(Player player, PlayerStats playerstats)
    {
        // Never kill a player - must be a bug if it was 0
        player.setHealth(Math.max(playerstats.getHealth(), 1));
        player.setFoodLevel(playerstats.getFoodLevel());
        player.setExhaustion(playerstats.getExhaustion());
        player.setSaturation(playerstats.getSaturation());
        player.setLevel(playerstats.getLevel());
        player.setExp(playerstats.getExp());
        
	for (PotionEffect effect : player.getActivePotionEffects())
        {
            player.removePotionEffect(effect.getType());
        }
        
        Collection<PotionEffect> potioneffects = playerstats.getPotionEffects();
        if(potioneffects != null)
        {
            player.addPotionEffects(playerstats.getPotionEffects());
        }
    }

    public void savePlayers(boolean outputtoconsole)
    {
        if(outputtoconsole)
        {
            WorldInventories.logStandard("Saving player information...");
        }

        for (Player player : WorldInventories.bukkitServer.getOnlinePlayers())
        {
            String world = player.getLocation().getWorld().getName();
            Group tGroup = WorldInventories.findFirstGroupThenDefault(world);

            InventoryHelper helper = new InventoryHelper();
            
            // Don't save if we don't care where we are (default group)
            if (!"default".equals(tGroup.getName()))
            {
                helper.setInventory(player.getInventory().getContents());
                helper.setArmour(player.getInventory().getArmorContents());
                savePlayerInventory(player.getName(), WorldInventories.findFirstGroupThenDefault(world), InventoryType.INVENTORY, helper);
                
                if (getConfig().getBoolean("dostats"))
                {
                    savePlayerStats(player, WorldInventories.findFirstGroupThenDefault(world));
                }
            }
        }

        if(outputtoconsole)
        {
            WorldInventories.logStandard("Done.");
        }
    }

    public void savePlayerInventory(String player, Group group, InventoryType type, InventoryHelper inventory)
    {
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdir();
        }

        String path = File.separator + group.getName();

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }      
        
        String sType = "unknown";
        if(type == InventoryType.INVENTORY)
        {
            sType = "inventory";
        }
        else if(type == InventoryType.ENDERCHEST)
        {
            sType = "enderchest";
        }

        path += File.separator + player + "." + sType + "." + inventoryFileVersion + ".yml";
        
        file = new File(path);

        try
        {
            file.createNewFile();
            FileConfiguration pc = YamlConfiguration.loadConfiguration(new File(path));
            
            if(type == InventoryType.INVENTORY)
            {
                pc.set("armour", inventory.getArmour());
                pc.set("inventory", inventory.getInventory());
            }
            else if(type == InventoryType.ENDERCHEST)
            {
                pc.set("enderchest", inventory.getInventory());
            }
            
            pc.save(file);
        }        
        catch (Exception e)
        {
            WorldInventories.logError("Failed to save " + sType + " for player: " + player + ": " + e.getMessage());
        }    
        
        WorldInventories.logDebug("Saved " + sType + " for player: " + player + " " + path);
    }
    
    public InventoryHelper loadPlayerInventory(Player player, Group group, InventoryType type)
    {
        String path = File.separator + group.getName();

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }         
        
        String sType = "unknown";
        if(type == InventoryType.INVENTORY)
        {
            sType = "inventory";
        }
        else if(type == InventoryType.ENDERCHEST)
        {
            sType = "enderchest";
        }

        path += File.separator + player.getName() + "." + sType + "." + inventoryFileVersion + ".yml";       
        
        file = new File(path);
        FileConfiguration pc = null;
        try
        {
            file.createNewFile();
            pc = YamlConfiguration.loadConfiguration(new File(path));        
        }
        catch (Exception e)
        {
            WorldInventories.logError("Failed to load " + sType + " for player: " + player + ": " + e.getMessage());
        }    

        List armour = null;
        List inventory = null;
        
        ItemStack[] iArmour = new ItemStack[4];
        ItemStack[] iInventory = null;
        
        if(type == InventoryType.INVENTORY)
        {
            armour = pc.getList("armour", null);
            inventory = pc.getList("inventory", null);
            
            if(armour == null)
            {
                WorldInventories.logDebug("Player " + player.getName() + " will get new armour on next save (clearing now).");
                player.getInventory().clear();                 

                for (int i = 0; i < 4; i++)
                {
                    iArmour[i] = new ItemStack(Material.AIR);
                }            
            }
            else
            {
                for(int i = 0; i < 4; i++)
                {
                    iArmour[i] = (ItemStack)armour.get(i);
                }
            }
            
            iInventory = new ItemStack[36];
            if(inventory == null)
            {
                WorldInventories.logDebug("Player " + player.getName() + " will get new items on next save (clearing now).");
                player.getInventory().clear();            

                for (int i = 0; i < 36; i++)
                {
                    iInventory[i] = new ItemStack(Material.AIR);
                }              
            }
            else
            {
                for (int i = 0; i < 36; i++)
                {
                    iInventory[i] = (ItemStack)inventory.get(i);
                }  
            }            
        }
        else if(type == InventoryType.ENDERCHEST)
        {
            inventory = pc.getList("enderchest", null);
            iInventory = new ItemStack[27];
            if(inventory == null)
            {
                
                for (int i = 0; i < 27; i++)
                {
                    iInventory[i] = new ItemStack(Material.AIR);
                }                  
            }
            else
            {
                for(int i = 0; i < 27; i++)
                {
                    iInventory[i] = (ItemStack)inventory.get(i);
                }
            }            
        }
        
        InventoryHelper ret = new InventoryHelper();
        ret.setArmour(iArmour);
        ret.setInventory(iInventory);
        
        WorldInventories.logDebug("Loaded " + sType + " for player: " + player + " " + path);

        return ret;
    }

    public PlayerStats loadPlayerStats(Player player, Group group)
    {
        String path = File.separator + group.getName();

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }         

        path += File.separator + player.getName() + ".stats." + statsFileVersion + ".yml";      
        
        file = new File(path);
        FileConfiguration pc = null;
        try
        {
            file.createNewFile();
            pc = YamlConfiguration.loadConfiguration(new File(path));        
        }
        catch (Exception e)
        {
            WorldInventories.logError("Failed to load stats for player: " + player + ": " + e.getMessage());
        }
        
/*
        int health, int foodlevel, float exhaustion, float saturation
        int level, float exp, Collection<PotionEffect> potioneffects
 */        
        
        int health;
        int foodlevel;
        double exhaustion;
        double saturation;
        int level;
        double exp;
        List<PotionEffect> potioneffects;
        
        health = pc.getInt("health", -1);
        foodlevel = pc.getInt("foodlevel", 20);
        exhaustion = pc.getDouble("exhaustion", 0);
        saturation = pc.getDouble("saturation", 0);
        level = pc.getInt("level", 0);
        exp = pc.getDouble("exp", 0);
        potioneffects = (List<PotionEffect>) pc.getList("potioneffects", null);
        
        PlayerStats playerstats = new PlayerStats(20, 20, 0, 0, 0, 0F, null);
        
        if(health == -1)
        {
            WorldInventories.logDebug("Player " + player.getName() + " will get a new stats file on next save (clearing now).");           
        }
        else
        {
            playerstats = new PlayerStats(health, foodlevel, (float)exhaustion, (float)saturation, level, (float)exp, potioneffects);
        }
        
        this.setPlayerStats(player, playerstats);  
        
        WorldInventories.logDebug("Loaded stats for player: " + player + " " + path);

        return playerstats;
    }

    public void savePlayerStats(Player player, Group group)
    {
        savePlayerStats(player.getName(), group, new PlayerStats(player.getHealth(), player.getFoodLevel(), player.getExhaustion(), player.getSaturation(), player.getLevel(), player.getExp(), player.getActivePotionEffects()));
    }
    public void savePlayerStats(String player, Group group, PlayerStats playerstats)
    {
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdir();
        }

        String path = File.separator + group.getName();

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }      

        path += File.separator + player + ".stats." + statsFileVersion + ".yml";

        file = new File(path);

        try
        {
            file.createNewFile();
            FileConfiguration pc = YamlConfiguration.loadConfiguration(file);
            //int health, int foodlevel, float exhaustion, float saturation
            //int level, float exp, Collection<PotionEffect> potioneffects
            pc.set("health", playerstats.getHealth());
            pc.set("foodlevel", playerstats.getFoodLevel());
            pc.set("exhaustion", playerstats.getExhaustion());
            pc.set("saturation", playerstats.getSaturation());
            pc.set("level", playerstats.getLevel());
            pc.set("exp", playerstats.getExp());
            pc.set("potioneffects", playerstats.getPotionEffects());
            
            pc.save(file);
        }    
        catch (Exception e)
        {
            WorldInventories.logError("Failed to save stats for player: " + player + ": " + e.getMessage());
        }
        
        WorldInventories.logDebug("Saved stats for player: " + player + " " + path);
    }

  

    public boolean importMultiInvData()
    {
        int importedgroups = 0;
        int importedinventories = 0;
        
        Plugin pMultiInv = WorldInventories.pluginManager.getPlugin("MultiInv");
        if (pMultiInv == null)
        {
            WorldInventories.logError("Failed to import MultiInv shares - Bukkit couldn't find MultiInv. Make sure it is installed and enabled whilst doing the import, then when successful remove it.");
            return false;
        }
        
        MultiInvAPI mapi = new MultiInvAPI((MultiInv) pMultiInv);
        HashMap<String, String> mgroups = mapi.getGroups();

        HashMap<String, Group> importgroups = new HashMap();
        for (String group : mgroups.values())
        {
            if(!importgroups.containsKey(group))
            {
                importgroups.put(group, new Group(group));
                importedgroups++;
            }
        }
        
        for(Map.Entry<String, String> worldgroup : mgroups.entrySet())
        {
            String world = worldgroup.getKey();
            String group = worldgroup.getValue();
            
            Group togroup = importgroups.get(group);
            togroup.addWorld(world);
        }
        
        if(importgroups.values().size() <= 0)
        {
            WorldInventories.logStandard("Didn't find any MultiInv groups to import!");
            return false;
        }
        
        getConfig().set("groups", null);
        
        for(Group group : importgroups.values())
        {
            getConfig().set("groups." + group.getName(), group.getWorlds());
        }
        
        groups.clear();
        groups.addAll(importgroups.values());
        
        this.saveConfig();
        
        for(World world : this.getServer().getWorlds())
        {
            for (OfflinePlayer player : this.getServer().getOfflinePlayers())
            {
                MIInventory minventory = mapi.getPlayerInventory(player.getName(), world.getName(), GameMode.getByValue(getConfig().getInt("miimportmode", 0)));
                if(minventory != null)
                {
                    ItemStack[] armour = MultiInvImportHelper.MIItemStacktoItemStack(minventory.getArmorContents());
                    ItemStack[] inventory = MultiInvImportHelper.MIItemStacktoItemStack(minventory.getInventoryContents());
                    
                    InventoryHelper helper = new InventoryHelper();
                    helper.setArmour(armour);
                    helper.setInventory(inventory);
                    
                    savePlayerInventory(player.getName(), findFirstGroupThenDefault(world.getName()), InventoryType.INVENTORY, helper);
                    importedinventories++;
                }
            }
        }

        WorldInventories.logStandard("Attempted to import " + Integer.toString(importedgroups) + " groups and " + Integer.toString(importedinventories) + " inventories from MultiInv.");
        this.getServer().getPluginManager().disablePlugin((MultiInv)pMultiInv);
        return true;
    }
    
    public boolean importVanilla()
    {
        int imported = 0;
        int failed = 0;
        
        WorldInventories.logStandard("Starting vanilla players import...");
        
        Group group = findFirstGroupThenDefault(getConfig().getString("vanillatogroup"));
        if(group == null)
        {
            WorldInventories.logStandard("Warning: importing from vanilla in to the default group (does the group specified exist?)");
        }
        
        OfflinePlayer[] offlineplayers = getServer().getOfflinePlayers();

        if(offlineplayers.length <= 0)
        {
            WorldInventories.logStandard("Found no offline players to import!");
            return false;
        }
        
        for(OfflinePlayer offlineplayer : offlineplayers)
        {
            Player player = null;
            try
            {
                player = (Player) offlineplayer;
            }
            catch(Exception e)
            {
                WorldInventories.logError("  (Warning) Couldn't convert a player.");
            }
            
            if(player == null)
            {
                WorldInventories.logStandard("Failed to import " + offlineplayer.getName() + ", couldn't create EntityPlayer.");
            }
            else
            {
                savePlayerStats(player, group);
                
                InventoryHelper helper = new InventoryHelper();
                helper.setArmour(player.getInventory().getArmorContents());
                helper.setInventory(player.getInventory().getContents());
                
                savePlayerInventory(player.getName(), group, InventoryType.INVENTORY, helper);
                
                helper.setArmour(null);
                helper.setInventory(((HumanEntity)player).getEnderChest().getContents());

                this.savePlayerInventory(player.getName(), group, InventoryType.ENDERCHEST, helper);
                
                imported++;
            }
        }
        
        WorldInventories.logStandard("Imported " + Integer.toString(imported) + "/" + Integer.toString(offlineplayers.length) + " (" + Integer.toString(failed) + " failures).");
        return (failed <= 0);
    }
   
    /*public boolean import78Data()
    {
        boolean allImported = true;
        int groupsFound = 0;
        int inventoriesFound = 0;
        
        WorldInventories.logStandard("Starting pre 78 build inventory import...");
        
        for(File fGroup : this.getDataFolder().listFiles())
        {
            if(fGroup.isDirectory() && fGroup.exists())
            {
                groupsFound++;
                
                for(File fInventory : new File(this.getDataFolder(), fGroup.getName()).listFiles())
                {
                    if(fInventory.isFile())
                    {
                        boolean is78Inventory = fInventory.getName().endsWith(".inventory");
                        if(is78Inventory)
                        {
                            inventoriesFound++;
                            
                            WIPlayerInventory oldinventory = Import78Helper.load78PlayerInventory(fInventory);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fInventory.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerInventory(fInventory.getName().split("\\.")[0], new Group(fGroup.getName()), new PlayerInventoryHelperOld(oldinventory.getItems(), oldinventory.getArmour()));
                            }
                        }
                    }
                }                
            }            
        }
        
        WorldInventories.logStandard("Attempted conversion of " + Integer.toString(groupsFound) + " groups and " + Integer.toString(inventoriesFound) + " associated inventories");
        
        return allImported;
    }    */
    
    /*public boolean import141Data()
    {
        boolean allImported = true;
        int groupsFound = 0;
        int inventoriesFound = 0;
        int statsFound = 0;
        int enderChestsFound = 0;
        
        WorldInventories.logStandard("Starting pre 141 build inventory import...");
        
        for(File fGroup : this.getDataFolder().listFiles())
        {
            if(fGroup.isDirectory() && fGroup.exists())
            {
                groupsFound++;
                
                for(File fFile : new File(this.getDataFolder(), fGroup.getName()).listFiles())
                {
                    if(fFile.isFile())
                    {
                        boolean is141Inventory = fFile.getName().endsWith(".inventory.v3");
                        if(is141Inventory)
                        {
                            inventoriesFound++;
                            
                            PlayerInventoryHelperOld oldinventory = Import141Helper.load141PlayerInventory(fFile);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerInventory(fFile.getName().split("\\.")[0], new Group(fGroup.getName()), oldinventory);
                            }
                        }
                        
                        boolean is141EnderChest = fFile.getName().endsWith(".enderchest.v3");
                        if(is141EnderChest)
                        {
                            enderChestsFound++;
                            
                            EnderChestHelper oldinventory = Import141Helper.load141EnderChest(fFile);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerEnderChest(fFile.getName().split("\\.")[0], new Group(fGroup.getName()), oldinventory);
                            }
                        }
                        
                        boolean is141Stats = fFile.getName().endsWith(".stats");
                        if(is141Stats)
                        {
                            statsFound++;
                            
                            PlayerStats oldstats = Import141Helper.load141PlayerStats(fFile);
                            if(oldstats == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerStats(fFile.getName().split("\\.")[0], new Group(fGroup.getName()), oldstats);
                            }
                        }                        
                    }
                }                
            }            
        }
        
        WorldInventories.logStandard("Attempted conversion of " + Integer.toString(groupsFound) + " groups including: " + Integer.toString(inventoriesFound) + " inventories, " + Integer.toString(enderChestsFound) + " Ender Chests and " + Integer.toString(statsFound) + " player stats.");
        
        return allImported;
    }*/
    
    public boolean import15Data()
    {
        boolean allImported = true;
        int groupsFound = 0;
        int inventoriesFound = 0;
        int statsFound = 0;
        int enderChestsFound = 0;
        
        WorldInventories.logStandard("Starting pre 15 build inventory import...");
        
        XStream xstream = new XStream()
        {
            // Taken from XStream test class
            //  Ignores and wipes any unrecognised fields instead of throwing an exception
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next)
            {
                return new MapperWrapper(next)
                {
                    @Override
                    public boolean shouldSerializeMember(Class definedIn, String fieldName)
                    {
                        if (definedIn == Object.class)
                        {
                            return false;
                        }

                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };
        
        //xstream.alias("potioneffecttype", org.bukkit.craftbukkit.v1_4_5.potion.CraftPotionEffectType.class);
        xstream.alias("playerstats", me.drayshak.WorldInventories.PlayerStats.class);
        xstream.alias("inventorieslists", me.drayshak.WorldInventories.InventoriesLists.class);
        //xstream.alias("potioneffect", org.bukkit.potion.PotionEffect.class);    
        
        //xstream.aliasPackage("org.bukkit.craftbukkit", "org.bukkit.craftbukkit.v1__4__5");        
        
        for(File fGroup : this.getDataFolder().listFiles())
        {
            if(fGroup.isDirectory() && fGroup.exists())
            {
                groupsFound++;
                
                for(File fFile : new File(this.getDataFolder(), fGroup.getName()).listFiles())
                {
                    if(fFile.isFile())
                    {
                        boolean is141Inventory = fFile.getName().endsWith(".inventory.v4.xml");
                        if(is141Inventory)
                        {
                            inventoriesFound++;
                            
                            PlayerInventoryHelperOld oldinventory = Import15Helper.load15PlayerInventory(fFile, xstream);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                InventoryHelper helper = new InventoryHelper();
                                helper.setArmour(oldinventory.getArmour());
                                helper.setInventory(oldinventory.getItems());
                                savePlayerInventory(fFile.getName().split("\\.")[0], new Group(fGroup.getName()), InventoryType.INVENTORY, helper);
                            }
                        }
                        
                        boolean is141EnderChest = fFile.getName().endsWith(".enderchest.v4.xml");
                        if(is141EnderChest)
                        {
                            enderChestsFound++;
                            
                            EnderChestHelperOld oldinventory = Import15Helper.load15PlayerEnderChest(fFile, xstream);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                InventoryHelper helper = new InventoryHelper();
                                helper.setArmour(null);
                                helper.setInventory(oldinventory.getItems());
                                savePlayerInventory(fFile.getName().split("\\.")[0], new Group(fGroup.getName()), InventoryType.ENDERCHEST, helper);
                            }
                        }
                        
                        boolean is141Stats = fFile.getName().endsWith(".stats.v4.xml");
                        if(is141Stats)
                        {
                            statsFound++;
                            
                            PlayerStats oldstats = Import15Helper.load15PlayerStats(fFile, xstream);
                            if(oldstats == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerStats(fFile.getName().split("\\.")[0], new Group(fGroup.getName()), oldstats);
                            }
                        }                        
                    }
                }                
            }            
        }
        
        WorldInventories.logStandard("Attempted conversion of " + Integer.toString(groupsFound) + " groups including: " + Integer.toString(inventoriesFound) + " inventories, " + Integer.toString(enderChestsFound) + " Ender Chests and " + Integer.toString(statsFound) + " player stats.");
        
        return allImported;
    }    

    // NetBeans complains about these log lines but message formatting breaks for me
    public static void logStandard(String line)
    {
        log.log(Level.INFO, "[WorldInventories] " + line);
    }

    public static void logError(String line)
    {
        log.log(Level.SEVERE, "[WorldInventories] " + line);
    }

    public static void logDebug(String line)
    {
        log.log(Level.FINE, "[WorldInventories] " + line);
    }

    private boolean loadConfigAndCreateDefaultsIfNecessary()
    {
        //saveDefaultConfig();

        try
        {
            YamlConfiguration config = new YamlConfiguration();
            config.load(new File(this.getDataFolder().getPath(), "config.yml"));            
        }
        catch(FileNotFoundException e)
        {
            saveDefaultConfig();
            return true;
        }
        catch(Exception e)
        {
            WorldInventories.logError("Failed to load configuration: " + e.getMessage());

            return false;
        }
        
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        return true;        
    }

    public List<Group> getGroups()
    {
        return groups;
    }

    private boolean loadConfiguration()
    {
        WorldInventories.groups = new ArrayList<Group>();

        String defaultmode = getConfig().getString("gamemodes.default", "SURVIVAL");
        
        Set<String> nodes = null;
        try
        {
            nodes = getConfig().getConfigurationSection("groups").getKeys(false);
        }
        catch(NullPointerException e)
        {
            nodes = new HashSet<String>();
            WorldInventories.logError("Warning: No groups found. Everything will be in the 'default' group.");
        }
        
        List<String> empty = Collections.emptyList();
        Group defaultgroup = new Group("default", empty, GameMode.valueOf(getConfig().getString("gamemodes.default", defaultmode)));
        WorldInventories.groups.add(defaultgroup);
        
        for (String sgroup : nodes)
        {
            List<String> worldnames = getConfig().getStringList("groups." + sgroup);
            if (worldnames != null)
            {
                Group group = new Group(sgroup, worldnames, GameMode.valueOf(getConfig().getString("gamemodes." + sgroup, defaultmode)));
                WorldInventories.groups.add(group);
                for (String world : worldnames)
                {
                    WorldInventories.logDebug("Adding " + sgroup + ":" + world + ":" + group.getGameMode().toString());
                }
                
                
            }
        }

        try
        {
            WorldInventories.exempts = getConfig().getStringList("exempt");
        }
        catch(NullPointerException e)
        {
            WorldInventories.exempts = new ArrayList<String>();
        }
        
        for (String player : WorldInventories.exempts)
        {
            WorldInventories.logDebug("Adding " + player + " to exemption list");
        }
        
        WorldInventories.logStandard("Loaded " + Integer.toString(exempts.size()) + " player exemptions.");
        
        return true;
    }

    public static Group findFirstGroupThenDefault(String world)
    {
        for (Group tGroup : WorldInventories.groups)
        {
            int index = tGroup.getWorlds().indexOf(world);
            if(index != -1)
            {
                return tGroup;
            }
        }

        return groups.get(0);
    }

    @Override
    public void onEnable()
    {
        WorldInventories.logStandard("Initialising...");

        //this.setXStreamAliases();
        
        boolean bInitialised = true;

        WorldInventories.bukkitServer = this.getServer();
        WorldInventories.pluginManager = WorldInventories.bukkitServer.getPluginManager();

        WorldInventories.logStandard("Loading configuration...");
        boolean loaded = this.loadConfigAndCreateDefaultsIfNecessary();
        if(!loaded)
        {
            WorldInventories.logError("Failed to load configuration! See the message above for details.");
            pluginManager.disablePlugin(this);
            return;
        }

        boolean bConfiguration = this.loadConfiguration();

        if (!bConfiguration)
        {
            WorldInventories.logError("Failed to load configuration.");
            bInitialised = false;
        }
        else
        {
            WorldInventories.logStandard("Loaded configuration successfully");
        }

        if (bInitialised)
        {
            if(getConfig().getBoolean("dovanillaimport"))
            {
                boolean bSuccess = this.importVanilla();
                
                this.getConfig().set("dovanillaimport", false);
                this.saveConfig();
                
                if(bSuccess)
                {
                    WorldInventories.logStandard("Vanilla saves import was a success!");
                }                
            }
            
            /*if(getConfig().getBoolean("do78import") || !getConfig().getBoolean("auto78updated"))
            {
                if(!getConfig().getBoolean("auto78updated"))
                {
                    WorldInventories.logStandard("This appears to be the first time you've run WorldInventories after build 78, automatically trying to import pre-78 data.");
                }
                
                boolean bSuccess = this.import78Data();
                
                this.getConfig().set("do78import", false);
                this.saveConfig();
                
                if(bSuccess)
                {
                    WorldInventories.logStandard("Pre 78 build saves import was a success!");
                    getConfig().set("auto78updated", true);
                    this.saveConfig();
                }
            }*/

            /*if(getConfig().getBoolean("do141import") || !getConfig().getBoolean("auto141updated"))
            {
                if(!getConfig().getBoolean("auto141updated"))
                {
                    WorldInventories.logStandard("This appears to be the first time you've run WorldInventories after version 141, automatically trying to import version 141 data.");
                }
                
                boolean bSuccess = this.import141Data();
                
                this.getConfig().set("do141import", false);
                this.saveConfig();
                
                if(bSuccess)
                {
                    WorldInventories.logStandard("Pre 141 build saves import was a success!");
                    getConfig().set("auto141updated", true);
                    this.saveConfig();
                }
            } */           
            
            if(getConfig().getBoolean("do15import") || !getConfig().getBoolean("auto15updated"))
            {
                if(!getConfig().getBoolean("auto15updated"))
                {
                    WorldInventories.logStandard("This appears to be the first time you've run WorldInventories after version 1.5, automatically trying to import version 1.5 data.");
                }
                
                boolean bSuccess = this.import15Data();
                
                this.getConfig().set("do15import", false);
                this.saveConfig();
                
                if(bSuccess)
                {
                    WorldInventories.logStandard("Pre 1.5 build saves import was a success!");
                    getConfig().set("auto15updated", true);
                    this.saveConfig();
                }
            }            
            
            if (getConfig().getBoolean("domiimport"))
            {
                boolean bSuccess = this.importMultiInvData();

                this.getConfig().set("domiimport", false);
                this.saveConfig();

                if (bSuccess)
                {
                    WorldInventories.logStandard("MultiInv data import was a success!");
                }
            }            
            
            getServer().getPluginManager().registerEvents(new EntityListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException e) {
                WorldInventories.logDebug("Failed to submit Metrics statistics.");
            }            
            
            WorldInventories.logStandard("Initialised successfully!");

            if (getConfig().getInt("saveinterval") >= 30)
            {
                saveTimer.scheduleAtFixedRate(new SaveTask(this), getConfig().getInt("saveinterval") * 1000, getConfig().getInt("saveinterval") * 1000);
            }

        }
        else
        {
            WorldInventories.logError("Failed to initialise.");
        }

    }

    @Override
    public void onDisable()
    {
        savePlayers(true);

        WorldInventories.logStandard("Plugin disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        String command = cmd.getName();

        if (command.equalsIgnoreCase("wireload"))
        {
            if (args.length == 0)
            {
                if (sender.hasPermission("worldinventories.reload"))
                {
                    WorldInventories.logStandard("Reloading configuration...");
                    reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Reloaded WorldInventories configuration successfully");
                }
            }

            return true;
        }
        else if (command.equalsIgnoreCase("wiexempt"))
        {
            if(sender.hasPermission("worldinventories.exempt"))
            {
                if(args.length != 2)
                {
                    sender.sendMessage(ChatColor.RED + "Wrong number of arguments given. Usage is /wiexempt <add/remove> <player>");
                    return true;
                }
                
                args[1] = args[1].toLowerCase();
                
                if(args[0].equalsIgnoreCase("add"))
                {
                    if(WorldInventories.exempts.contains(args[1]))
                    {
                        sender.sendMessage(ChatColor.RED + "That player is already in the exemption list.");
                    }
                    else
                    {
                        WorldInventories.exempts.add(args[1]);
                        sender.sendMessage(ChatColor.GREEN + "Added " + args[1] + " to the exemption list successfully.");
                        getConfig().set("exempt", WorldInventories.exempts);
                        saveConfig();
                    }
                }
                else if(args[0].equalsIgnoreCase("remove"))
                {
                    if(!WorldInventories.exempts.contains(args[1].toLowerCase()))
                    {
                        sender.sendMessage(ChatColor.RED + "That player isn't in the exemption list.");
                    }
                    else
                    {
                        WorldInventories.exempts.remove(args[1]);
                        sender.sendMessage(ChatColor.GREEN + "Removed " + args[1] + " from the exemption list successfully.");
                        getConfig().set("exempt", WorldInventories.exempts);
                        saveConfig();
                    }
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "Argument invalid. Usage is /wiexempt <add/remove> <player>");
                }
                
                return true;
            }
        }

        return false;
    }
}
