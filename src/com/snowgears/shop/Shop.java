package com.snowgears.shop;

import java.io.File;
import java.io.IOException;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.snowgears.shop.listeners.DisplayItemListener;
import com.snowgears.shop.listeners.MiscListener;
import com.snowgears.shop.listeners.ShopListener;
import com.snowgears.shop.utils.Metrics;
import com.snowgears.shop.utils.Updater;
import com.snowgears.shop.utils.Updater.UpdateResult;

import net.milkbowl.vault.economy.Economy;

public class Shop extends JavaPlugin{
	
	private static Shop plugin;
	
	private final ShopListener shopListener = new ShopListener(this);
	private final DisplayItemListener displayListener = new DisplayItemListener(this);
	private final MiscListener miscListener = new MiscListener(this);
	private final ShopHandler shopHandler = new ShopHandler(this);
	
	protected FileConfiguration config; 
	protected File shopFile = null;
	
	private boolean usePerms = false;
	private boolean useVault = false;
	private MaterialData economyMaterial = null;
	private String economyDisplayName = "";
	private int currencyToStart = 0;
	private int durabilityMargin = 0;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private Economy econ = null;
	private boolean hasClearLag = false;

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

		final File configFile = new File(this.getDataFolder() + "/config.yml");
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
		
		Updater updater = null;
		boolean autoUpdate = getConfig().getBoolean("AUTO-UPDATE");
		if(autoUpdate)
			updater = new Updater(this, 56083, this.getFile(), Updater.UpdateType.DEFAULT, false);
		
		if(updater != null && updater.getResult() == UpdateResult.SUCCESS){
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { 
				public void run() { 
					configFile.delete();
					getServer().reload();
				}
			}, 1L); 
		}


		usePerms = getConfig().getBoolean("usePermissions");
		useVault = getConfig().getConfigurationSection("Economy").getBoolean("useVault");
		String itemCurrency = getConfig().getConfigurationSection("Economy").getString("itemCurrencyID (non-vault)");
		int itemCurrencyId = -1;
		int itemCurrencyData = 0;
		if(itemCurrency.contains(";")){
			itemCurrencyId = Integer.parseInt(itemCurrency.substring(0, itemCurrency.indexOf(";")));
			itemCurrencyData = Integer.parseInt(itemCurrency.substring(itemCurrency.indexOf(";")+1, itemCurrency.length()));
		}
		else{
			itemCurrencyId = Integer.parseInt(itemCurrency.substring(0, itemCurrency.length()));
		}
		economyMaterial = new MaterialData(itemCurrencyId, (byte)itemCurrencyData);
		
		economyDisplayName = getConfig().getConfigurationSection("Economy").getString("displayName")+"(s)";
		currencyToStart = getConfig().getConfigurationSection("Economy").getInt("currencyToStartWith");
		durabilityMargin = getConfig().getInt("maxDurabilityMargin");
		
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
				log.info("[Shop] Shops will use "+economyMaterial.getItemType().name().replace("_", " ").toLowerCase()+" as the currency on the server.");
		}
		
		if (getServer().getPluginManager().getPlugin("ClearLag") != null) {
            hasClearLag = true;
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
				sender.sendMessage("There are "+ChatColor.GOLD+shopHandler.getNumberOfShops()+ChatColor.WHITE+" shops registered on the server.");
			}
			else if ((cmd.getName().equalsIgnoreCase("shop") && args[0].equalsIgnoreCase("refresh"))) {
				if(sender instanceof Player){
					Player player = (Player)sender;
					if((usePerms && !player.hasPermission("shop.operator")) || !player.isOp()){
						player.sendMessage(ChatColor.RED+"You are not authorized to use that command.");
						return true;
					}
				}
				shopHandler.refreshShopItems();
				sender.sendMessage(ChatColor.GRAY+"The display items on all of the shops have been refreshed.");
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

    public static Shop getPlugin(){
		return plugin;
	}
    
	public ShopListener getShopListener(){
		return shopListener;
	}
	
	public DisplayItemListener getDisplayListener(){
		return displayListener;
	}
	
	public MiscListener getMiscListener(){
		return miscListener;
	}
	
	public ShopHandler getShopHandler(){
		return shopHandler;
	}
	
	public boolean usePerms(){
		return usePerms;
	}
	
	public boolean useVault(){
		return useVault;
	}
	
	public MaterialData getEconomyMaterial(){
		return economyMaterial;
	}
	
	public String getEconomyDisplayName(){
		return economyDisplayName;
	}
	
	public int getStartingCurrency(){
		return currencyToStart;
	}
	
	public int getDurabilityMargin(){
		return durabilityMargin;
	}
	
	public Logger getShopLogger(){
		return log;
	}
	
	public Economy getEconomy(){
		return econ;
	}
	
	public boolean hasClearLag(){
		return hasClearLag;
	}
}