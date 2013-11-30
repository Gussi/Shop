package com.snowgears.shop.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;

public class YamlPriceHandler {
	
	public Shop plugin = Shop.plugin;
	
	private HashMap<Material, Double> prices = new HashMap<Material, Double>(); //TODO make getters for values
	
	public YamlPriceHandler(Shop instance)
    {
        plugin = instance;
    }
//
//	public void loadPricesFromFile(){
//		File priceFile = new File(plugin.getDataFolder() + "/resolutePrices.yml");
//		if(! priceFile.exists()){
//			try {
//				priceFile.createNewFile();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		YamlConfiguration config = YamlConfiguration.loadConfiguration(priceFile);
//		ArrayList<ShopObject> allShops = getShopsFromConfig(config);
//		plugin.alisten.allShops.addAll(allShops);
//	}
//	
//	private void getPricesFromConfig(YamlConfiguration config){
//
//		if(config.getConfigurationSection("Resolute Prices") == null)
//			return;
//		Set<String> allPriceMaterials = config.getConfigurationSection("shops").getKeys(false);
////		System.out.println("All shop numbers: "+allShopNumbers);
//		for(String shopNumber : allShopNumbers){
//			Location loc = locationFromString(config.getString("shops."+shopNumber+".location"));
//			Location signLoc = locationFromString(config.getString("shops."+shopNumber+".signLocation"));
//			String owner = config.getString("shops."+shopNumber+".owner");
//			double price = Double.parseDouble(config.getString("shops."+shopNumber+".price"));
//			int amount = Integer.parseInt(config.getString("shops."+shopNumber+".amount"));
//			String type = config.getString("shops."+shopNumber+".type");
//			boolean isAdmin = false;
//			if(type.contains("admin"))
//				isAdmin = true;
//			boolean isSelling = true;
//			if(type.contains("buying"))
//				isSelling = false;
//			
//			MaterialData data = dataFromString(config.getString("shops."+shopNumber+".item.data"));
//			ItemStack is = new ItemStack(data.getItemType());
//			is.setData(data);
//			ItemMeta im = is.getItemMeta();
//			String name = config.getString("shops."+shopNumber+".item.name");
//			if(!name.isEmpty())
//				im.setDisplayName(config.getString("shops."+shopNumber+".item.name"));
//			im.setLore(loreFromString(config.getString("shops."+shopNumber+".item.lore")));
//			is.setItemMeta(im);
//			is.addEnchantments(enchantmentsFromString(config.getString("shops."+shopNumber+".item.enchantments")));
//			
//			ShopObject shop = new ShopObject(loc, signLoc, owner, is, price, amount, isSelling, isAdmin);
//			shop.updateSign();
//			allShops.add(shop);
//		}
//		return allShops;
//	}
//	
//	private String locationToString(Location loc){
//		return loc.getWorld().getName()+","+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
//	}
//	
//	private Location locationFromString(String locString){
//		String[] parts = locString.split(",");
//		return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
//	}
//	
//	private List<String> loreFromString(String loreString){
//		loreString = loreString.substring(1, loreString.length()-1); //get rid of []
//		String[] loreParts = loreString.split(", ");
//		return Arrays.asList(loreParts);
//	}
//	
//	private String enchantmentsToString(HashMap<Enchantment, Integer> enchantments){
//		HashMap<String, Integer> futureString = new HashMap<String, Integer>();
//		for(Entry<Enchantment, Integer> s : enchantments.entrySet()){
//			futureString.put(s.getKey().getName(), s.getValue());
//		}
//		return futureString.toString();
//	}
//	
//	private HashMap<Enchantment, Integer> enchantmentsFromString(String enchantments){
//		HashMap<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
//		enchantments = enchantments.substring(1, enchantments.length()-1); //get rid of {}
//		if(enchantments.isEmpty())
//			return enchants;
//		String[] enchantParts = enchantments.split(", ");
//		for(String whole : enchantParts){
//			String[] pair = whole.split("=");
//			enchants.put(Enchantment.getByName(pair[0]), Integer.parseInt(pair[1]));
//		}
//		return enchants;
//	}
//	
//	private MaterialData dataFromString(String dataString){
//		int index = dataString.indexOf("(");
////		System.out.println(dataString.substring(0, index));
////		System.out.println(dataString.substring(index+1, dataString.length()-1));
//		Material m = Material.getMaterial(dataString.substring(0, index));
//		int data = Integer.parseInt(dataString.substring(index+1, dataString.indexOf(")")));
//		return new MaterialData(m, (byte)data);
//	}
}