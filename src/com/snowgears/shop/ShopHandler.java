package com.snowgears.shop;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;


public class ShopHandler {
	
	public Shop plugin = Shop.plugin;
	
	private HashMap<Location, ShopObject> allShops = new HashMap<Location, ShopObject>();
	
	public ShopHandler(Shop instance)
    {
        plugin = instance;
    }
	
	public ShopObject getShop(Location loc){
		return allShops.get(loc);
	}
	
	public ShopObject addShop(ShopObject shop){
		return allShops.put(shop.getLocation(), shop);
	}
	
	public boolean removeShop(ShopObject shop){
		if(allShops.containsKey(shop.getLocation())){
			allShops.remove(shop.getLocation());
			shop.getDisplayItem().remove();
			return true;
		}
		return false;
	}

	public void removeShopItems(){
    	for(ShopObject shop : allShops.values())
    		shop.getDisplayItem().remove();
    }
	
	public int getNumberOfShops(){
		return allShops.size();
	}
	
//	private Map<String, ArrayList<ShopObject>> groupedShopMap(){
//		HashMap<String, ArrayList<ShopObject>> map = new HashMap<String, ArrayList<ShopObject>>();
//		
//		ArrayList<String> shopOwners = new ArrayList<String>();
//		for(ShopObject s : allShops.values()){
//			if(!shopOwners.contains(s.getOwner())){
//				shopOwners.add(s.getOwner());
//				map.put(s.getOwner(), new ArrayList<ShopObject>());
//			}
//		}
//		Collections.sort(shopOwners);
//		
//		for(Entry<String, ArrayList<ShopObject>> set : map.entrySet()){
//			ArrayList<ShopObject> shopList = set.getValue();
//			for(ShopObject s : allShops.values()){
//				if(s.getOwner().equals(set.getKey())){
//					shopList.add(s);
//					map.put(set.getKey(), shopList);
//				}
//			}
//		}
//		
//		Map<String, ArrayList<ShopObject>> sortedMap = new TreeMap<String, ArrayList<ShopObject>>(map);
//		return sortedMap;
//	}
	
	private ArrayList<ShopObject> orderedShopList(){
		ArrayList<ShopObject> list = new ArrayList<ShopObject>(allShops.values());
		Collections.sort(list, new Comparator<ShopObject>(){
			@Override
			public int compare(ShopObject o1, ShopObject o2) {
				return o1.getOwner().toLowerCase().compareTo(o2.getOwner().toLowerCase());
			}
		});
		return list;
	}
	
	public void saveShops(){
		File fileDirectory = new File(plugin.getDataFolder(), "Data");
		if(!fileDirectory.exists())
			fileDirectory.mkdir();
		File shopFile = new File(fileDirectory + "/shops.yml");
		if(! shopFile.exists()){ // file doesn't exist
			try {
				shopFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{ //does exist, clear it for future saving
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(shopFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			writer.print("");
			writer.close();
		}
		
		YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
		ArrayList<ShopObject> shopList = orderedShopList();
		
		int shopNumber = 1;
		for(int i=0; i<shopList.size(); i++)
		{
			ShopObject s = shopList.get(i);
			config.set("shops."+s.getOwner()+"."+shopNumber+".location", locationToString(s.getLocation()));
			config.set("shops."+s.getOwner()+"."+shopNumber+".signLocation", locationToString(s.getSignLocation()));
			config.set("shops."+s.getOwner()+"."+shopNumber+".price", s.getPrice());
			config.set("shops."+s.getOwner()+"."+shopNumber+".amount", s.getAmount()); 
			String type = "";
			if(s.isAdminShop())
				type = "admin ";
			type = type + s.getType().getName();
			config.set("shops."+s.getOwner()+"."+shopNumber+".type", type);
			
			ItemStack displayStack = s.getDisplayItem().getItemStack();
			ItemMeta im = displayStack.getItemMeta();
			if(im.getDisplayName() != null)
				config.set("shops."+s.getOwner()+"."+shopNumber+".item.name", im.getDisplayName());
			else
				config.set("shops."+s.getOwner()+"."+shopNumber+".item.name", "");
			config.set("shops."+s.getOwner()+"."+shopNumber+".item.data", displayStack.getData().toString());
			config.set("shops."+s.getOwner()+"."+shopNumber+".item.durability", displayStack.getDurability());
			config.set("shops."+s.getOwner()+"."+shopNumber+".item.enchantments", enchantmentsToString(displayStack.getEnchantments()));
			if(im.getLore() != null)
				config.set("shops."+s.getOwner()+"."+shopNumber+".item.lore", im.getLore().toString());
			else
				config.set("shops."+s.getOwner()+"."+shopNumber+".item.lore", "[]");
			System.out.println(allShops.size());
			s.delete();
			System.out.println(allShops.size());
			
			shopNumber++;
			//reset shop number if next shop has a different owner
			if(i < shopList.size()-1){
				if(!(s.getOwner().equals(shopList.get(i+1).getOwner())))
					shopNumber = 1;
			}
		}
		 
		 try
		    {
		        config.save(shopFile);
		    }
		    catch(IOException ioe)
		    {
		        ioe.printStackTrace();
		    }
	}
	
	public void loadShops(){
		File fileDirectory = new File(plugin.getDataFolder(), "Data");
		if(!fileDirectory.exists())
			return;
		File shopFile = new File(fileDirectory + "/shops.yml");
		if(! shopFile.exists())
			return;
		if(shopFile.length() > 0){ //file contains something
			File backupShopFile = new File(fileDirectory + "/shopsBackup.yml");
			if(! backupShopFile.exists()){
				try {
					backupShopFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(backupShopFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			writer.print("");
			writer.close(); //clear contents of the backup file
			
			try {
				copyFile(shopFile, backupShopFile); //make a backup of the file before loading in case of corruption
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		
		YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
		loadShopsFromConfig(config);
	}
	
	private void loadShopsFromConfig(YamlConfiguration config){

		if(config.getConfigurationSection("shops") == null)
			return;
		Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

		for(String shopOwner : allShopOwners){
			Set<String> allShopNumbers = config.getConfigurationSection("shops."+shopOwner).getKeys(false);
			for(String shopNumber : allShopNumbers){
				Location loc = locationFromString(config.getString("shops."+shopOwner+"."+shopNumber+".location"));
				Location signLoc = locationFromString(config.getString("shops."+shopOwner+"."+shopNumber+".signLocation"));
				double price = Double.parseDouble(config.getString("shops."+shopOwner+"."+shopNumber+".price"));
				int amount = Integer.parseInt(config.getString("shops."+shopOwner+"."+shopNumber+".amount"));
				String type = config.getString("shops."+shopOwner+"."+shopNumber+".type");
				boolean isAdmin = false;
				if(type.contains("admin"))
					isAdmin = true;
				ShopType shopType = typeFromString(type);
				
				MaterialData data = dataFromString(config.getString("shops."+shopOwner+"."+shopNumber+".item.data"));
				ItemStack is = new ItemStack(data.getItemType());
				is.setData(data);
				ItemMeta im = is.getItemMeta();
				String name = config.getString("shops."+shopOwner+"."+shopNumber+".item.name");
				if(!name.isEmpty())
					im.setDisplayName(config.getString("shops."+shopOwner+"."+shopNumber+".item.name"));
				im.setLore(loreFromString(config.getString("shops."+shopOwner+"."+shopNumber+".item.lore")));
				is.setItemMeta(im);
				is.addEnchantments(enchantmentsFromString(config.getString("shops."+shopOwner+"."+shopNumber+".item.enchantments")));
				
				ShopObject shop = new ShopObject(loc, signLoc, shopOwner, is, price, amount, isAdmin, shopType);
				shop.updateSign();
				this.addShop(shop);
			}
		}
	}
	
	private String locationToString(Location loc){
		return loc.getWorld().getName()+","+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
	}
	
	private Location locationFromString(String locString){
		String[] parts = locString.split(",");
		return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
	}
	
	private List<String> loreFromString(String loreString){
		loreString = loreString.substring(1, loreString.length()-1); //get rid of []
		String[] loreParts = loreString.split(", ");
		return Arrays.asList(loreParts);
	}
	
	private String enchantmentsToString(Map<Enchantment, Integer> enchantments){
		HashMap<String, Integer> futureString = new HashMap<String, Integer>();
		for(Entry<Enchantment, Integer> s : enchantments.entrySet()){
			futureString.put(s.getKey().getName(), s.getValue());
		}
		return futureString.toString();
	}
	
	private HashMap<Enchantment, Integer> enchantmentsFromString(String enchantments){
		HashMap<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
		enchantments = enchantments.substring(1, enchantments.length()-1); //get rid of {}
		if(enchantments.isEmpty())
			return enchants;
		String[] enchantParts = enchantments.split(", ");
		for(String whole : enchantParts){
			String[] pair = whole.split("=");
			enchants.put(Enchantment.getByName(pair[0]), Integer.parseInt(pair[1]));
		}
		return enchants;
	}
	
	private MaterialData dataFromString(String dataString){
		int index = dataString.indexOf("(");
//		System.out.println(dataString.substring(0, index));
//		System.out.println(dataString.substring(index+1, dataString.length()-1));
		Material m = Material.getMaterial(dataString.substring(0, index));
		int data = Integer.parseInt(dataString.substring(index+1, dataString.indexOf(")")));
		return new MaterialData(m, (byte)data);
	}
	
	private ShopType typeFromString(String typeString){
		if(typeString.contains("selling"))
			return ShopType.SELLING;
		else if(typeString.contains("buying"))
			return ShopType.BUYING;
		else
			return ShopType.BARTER;
	}
	
	private static void copyFile(File source, File dest)
	        throws IOException {
	    InputStream input = null;
	    OutputStream output = null;
	    try {
	        input = new FileInputStream(source);
	        output = new FileOutputStream(dest);
	        byte[] buf = new byte[1024];
	        int bytesRead;
	        while ((bytesRead = input.read(buf)) > 0) {
	            output.write(buf, 0, bytesRead);
	        }
	    } finally {
	        input.close();
	        output.close();
	    }
	}
}