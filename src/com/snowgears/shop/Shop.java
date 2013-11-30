package com.snowgears.shop;

import java.io.File;
import java.io.IOException;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.snowgears.shop.listeners.DisplayItemListener;
import com.snowgears.shop.listeners.MiscListener;
import com.snowgears.shop.listeners.ShopListener;
import com.snowgears.shop.utils.Metrics;

import net.milkbowl.vault.economy.Economy;

public class Shop extends JavaPlugin{
	
	public final ShopListener shopListener = new ShopListener(this);
	public final DisplayItemListener displayListener = new DisplayItemListener(this);
	public final MiscListener miscListener = new MiscListener(this);
	public final ShopHandler shopHandler = new ShopHandler(this);
	public static Shop plugin;
	
	protected FileConfiguration config; 
	protected File shopFile = null;
	
	public boolean usePerms = false;
	public boolean useVault = false;
	public Material economyMaterial = null;
	public String economyDisplayName = "";
	public int currencyToStart = 0;
	public int durabilityMargin = 0;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	public Economy econ = null;

	public void onEnable(){
		plugin = this;
		getServer().getPluginManager().registerEvents(shopListener, this);
		getServer().getPluginManager().registerEvents(displayListener, this);
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

		usePerms = getConfig().getBoolean("usePermissions");
		useVault = getConfig().getConfigurationSection("Economy").getBoolean("useVault");
		economyMaterial = Material.getMaterial(getConfig().getConfigurationSection("Economy").getString("itemCurrency (non-vault)"));
		economyDisplayName = getConfig().getConfigurationSection("Economy").getString("displayName");
		currencyToStart = getConfig().getConfigurationSection("Economy").getInt("currencyToStartWith");
		durabilityMargin = getConfig().getInt("maxDurabilityMargin");
		
		//TODO may need to add "[Shop]" string to front of log messages if they do not show the plugin name
		if(useVault == true){
			if (setupEconomy() == false) {
				log.severe("[Shop]"+ChatColor.RED+"Plugin disabled due to no Vault dependency found on server!");
				log.info("[Shop] If you do not wish to use Vault with Shop, make sure to set 'useVault' in the config file to false.");
	            getServer().getPluginManager().disablePlugin(this);
	            return;
	        }
			else{
				log.info("[Shop] Vault dependancy found. Shops will use the vault economy for currency on the server.");
			}
		}
		else{
			if(economyMaterial == null){
				log.severe("[Shop]"+ChatColor.RED+"Plugin disabled due to an invalid material name in the configuration section \"Economy.itemCurrency (non-vault)\".");
				log.info("[Shop]"+"Go to "+ChatColor.BLUE+"http://jd.bukkit.org/rb/apidocs/org/bukkit/Material.html"+ChatColor.BLACK+"for the full list of valid item names.");
				getServer().getPluginManager().disablePlugin(this);
			}
			else
				log.info("[Shop] Shops will use "+economyMaterial.name().replace("_", " ").toLowerCase()+" as the currency on the server.");
		}
		
		shopFile = new File(fileDirectory + "/shops.yml");
		
		if(! shopFile.exists()){ // file doesn't exist
			try {
				shopFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{ //file does exist
			if (shopFile.length() > 0 ) { //file contains something
				shopHandler.loadShops();
			}
		}
	}
	
	public void onDisable(){
		shopHandler.saveShops();
		plugin = null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length == 1){
			if ((cmd.getName().equalsIgnoreCase("shop") && args[0].equalsIgnoreCase("list"))) {
				sender.sendMessage("There are "+ChatColor.GOLD+shopHandler.getNumberOfShops()+ChatColor.WHITE+" shops registered.");
			}
			else if ((cmd.getName().equalsIgnoreCase("shop") && args[0].equalsIgnoreCase("reload"))) {
				if(sender instanceof Player){
					Player player = (Player)sender;
					if((usePerms && !player.hasPermission("shop.operator")) || !player.isOp()){
						player.sendMessage(ChatColor.RED+"You are not authorized to use that command.");
						return true;
					}
				}
				shopHandler.saveShops();
				shopHandler.loadShops();
			}
			return true;
		}
        return false;
    }
	
//    public void respawnAllShopItems(){
//    	for(final ShopObject shop : shopListener.allShops){
//			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { 
//				public void run() { 
//					shop.displayItem.respawn();
//					} 
//			}, 5L); 
//		}
//    }
//    
  
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