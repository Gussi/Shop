package com.snowgears.shop;


import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;

import com.snowgears.shop.listeners.DisplayItemListener;
import com.snowgears.shop.listeners.MiscListener;
import com.snowgears.shop.listeners.ShopListener;
import com.snowgears.shop.utils.Metrics;

import net.milkbowl.vault.economy.Economy;

public class Shop extends JavaPlugin{
	
	public final ShopListener alisten = new ShopListener(this);
	public final DisplayItemListener displaylistener = new DisplayItemListener(this);
	public final MiscListener miscListener = new MiscListener(this);
	public static Shop plugin;
	protected FileConfiguration config; 
	public static File shopFile = null;
	
	public static boolean usePerms = false;
	public static boolean useVault = false;
	private static final Logger log = Logger.getLogger("Minecraft");
	public static Economy econ = null;
	public static int economyItemId = 0;
	public static String economyItemName = "";
	public static int itemsToStart = 0;
	public static int durabilityMargin = 0;
	
	public static HashMap<String, ArrayList<ShopObject>> shopMap = new HashMap<String, ArrayList<ShopObject>>();//string, all portals

	public void onEnable(){
		plugin = this;
		getServer().getPluginManager().registerEvents(alisten, this);
		getServer().getPluginManager().registerEvents(displaylistener, this);
		getServer().getPluginManager().registerEvents(miscListener, this);
		
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
		    // Failed to submit the stats
		}

		File configFile = new File(this.getDataFolder() + "/config.yml");
		if(!configFile.exists())
		{
		  this.saveDefaultConfig();
		}

		File fileDirectory = new File(this.getDataFolder(), "Data");
		if(!fileDirectory.exists())
		{
			boolean success = false;
			success = (fileDirectory.mkdirs());
			if (!success) {
				getServer().getConsoleSender().sendMessage("[Shop]"+ChatColor.RED+" Data folder could not be created.");
			}
		}

		shopFile = new File(fileDirectory + "/shop.yml");
		
		if(! shopFile.exists()){ // file doesn't exist
			try {
				shopFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{ //file does exist
			if (shopFile.length() > 0 ) { //file contains something
				HashMap<String, ArrayList<ShopObject>> temp = loadHashMapFrom(shopFile);
				ArrayList<ShopObject> shop = temp.get("load");
				
				alisten.allShops = shop;
			}
		}

		usePerms = getConfig().getBoolean("usePermissions");
		useVault = getConfig().getConfigurationSection("Economy").getBoolean("useVault");
		economyItemId = getConfig().getConfigurationSection("Economy").getInt("itemId");
		economyItemName = getConfig().getConfigurationSection("Economy").getString("name");
		itemsToStart = getConfig().getConfigurationSection("Economy").getInt("amountToStart");
		durabilityMargin = getConfig().getInt("durabilityMargin");
		
		if(useVault == true){
			if (setupEconomy() == false) {
				log.severe(String.format(ChatColor.RED+"[Shop] Plugin disabled due to no Vault dependency found on server!", getDescription().getName()));
				System.out.println("[Shop] If you do not wish to use Vault with Shop, make sure to set 'useVault' in the config file to false.");
	            getServer().getPluginManager().disablePlugin(this);
	            return;
	        }
			else{
				System.out.println("[Shop] Vault dependancy found. Setting shops to use Vault economy.");
			}
		}
		else{
			System.out.println("[Shop] Shops will use "+Material.getMaterial(economyItemId).name().replace("_", " ").toLowerCase()+" as the physical economy on the server.");
		}
			
		//TODO for all shops when loading, set location to new serializableLocation so no errors
		
//		//refresh shop showcase items
//		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() { 
//				public void run() { 
//					respawnAllShopItems();
//				} 
//		}, 0L, 36000L);
	}
	
	public void onDisable(){
		shopMap.put("load", alisten.allShops);
		saveHashMapTo(shopMap, shopFile);
		
//		removeAllShopItems();
		
		plugin = null;
		shopMap = null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length == 1){
			if ((cmd.getName().equalsIgnoreCase("shops") && args[0].equalsIgnoreCase("list"))) {
				sender.sendMessage("There are "+ChatColor.GOLD+alisten.allShops.size()+ChatColor.WHITE+" shops registered.");
			}
			return true;
		}
        return false;
    }
	
	public static <K, V> void saveHashMapTo(HashMap<K, V> hashmap, File file) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(hashmap);
            oos.flush();
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
   
    /**
    * Loads a HashMap<K, V> from a file.
    * @param file : The file from which the HashMap will be loaded.
    * @return Returns a HashMap that was saved in the file.
    */
    @SuppressWarnings("unchecked")
    public <K, V> HashMap<K, V> loadHashMapFrom(File file) {
        HashMap<K, V> result = null;
        ObjectInputStream ois = null;
       
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            result = (HashMap<K, V>) ois.readObject();
            ois.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
       
        return result;
    }
    
//    public void respawnAllShopItems(){
//    	for(final ShopObject shop : alisten.allShops){
//			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { 
//				public void run() { 
//					shop.displayItem.respawn();
//					} 
//			}, 5L); 
//		}
//    }
//    
//    public void removeAllShopItems(){
//    	for(Item i : displaylistener.allShopItems)
//    		i.remove();
//    }
    
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}