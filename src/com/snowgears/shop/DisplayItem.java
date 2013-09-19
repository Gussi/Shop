package com.snowgears.shop;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.snowgears.shop.utils.SerializableLocation;


public class DisplayItem implements Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 3099489010508308627L;
	
	private ShopObject shop;
	private UUID uuid;
    private SerializableLocation location;
    private String name;
    private List<String> lore;
    private HashMap<Integer, Integer> enchantments = new HashMap<Integer, Integer>();
    private int material;
    private short durability;
    private byte data;

	    public DisplayItem(ShopObject s, Location loc, ItemStack is)
	    {
	    	shop = s;
	        location = new SerializableLocation(loc);
	        material = is.getType().getId();
	        
	        ItemMeta im = is.getItemMeta();
	        if(im.getDisplayName() != null && !im.getDisplayName().equals("air"))
	        	name = im.getDisplayName();
	        else
	        	name = capitalize(getType().name().replace("_", " ").toLowerCase());
	        
	        lore = im.getLore();
	        
	        for(Entry<Enchantment, Integer> e : is.getEnchantments().entrySet()){
	        	enchantments.put(e.getKey().getId(), e.getValue());
	        }
	        
	        durability = is.getDurability();
	        data = is.getData().getData();
	        
	        spawn(is);
	    }

		private boolean remove(){
			Location displayLoc = location.deserialize().getBlock().getLocation();
			
			boolean removed = false;

			Chunk c = displayLoc.getChunk();
			for (Entity e : c.getEntities()) {
				if(e.getUniqueId() == uuid){
					e.remove();
					removed = true;	
				}
				else if(e instanceof Item && e.getLocation().distanceSquared(displayLoc) < 1){
					e.remove();
					removed = true;
				}
			}
			return removed;
		}
		
		public Item spawn(ItemStack is){
			final Item i = location.deserialize().getWorld().dropItem(location.deserialize(), is);
			i.setVelocity(new Vector(0, 0.1, 0));
			i.setMetadata("DisplayItem", new FixedMetadataValue(Shop.plugin,0));
			uuid = i.getUniqueId();

			ItemStack iStack = i.getItemStack();
			ItemMeta im = iStack.getItemMeta();
			im.setDisplayName(ChatColor.GRAY+"Display Item");
			iStack.setItemMeta(im);
			i.setItemStack(iStack);
	
			return i;
		}
		
		public void respawn(){
			remove();
			ItemStack is = new ItemStack(getType());
			is.getData().setData(getData());
			spawn(is);
		}
		
		public ShopObject getShop(){
			return shop;
		}
		
		public UUID getUniqueId(){
			return uuid;
		}
		
		public Location getLocation(){
			return location.deserialize();
		}
		
		public Material getType(){
			return Material.getMaterial(material);
		}
		
		public short getDurability(){
			return durability;
		}
		
		public int getDurabilityPercent(){
			if(getType().getMaxDurability() > 0){
				double top = getType().getMaxDurability() - durability;
				double d = top / getType().getMaxDurability();
				d = d * 100;
				return (int)d;
			}
			return 0;
		}
		
		public byte getData(){
			return data;
		}
		
		public String getDisplayName(){
			return name;
		}
		
		public List<String> getLore(){
			return lore;
		}
		
		public HashMap<Integer, Integer> getEnchantments(){
			return enchantments;
		}
		
		public ArrayList<String> getListOfEnchantments(){
			ArrayList<String> list = new ArrayList<String>();
			for(Entry<Integer, Integer> e : enchantments.entrySet()){
				list.add(capitalize(Enchantment.getById(e.getKey()).getName().replace("_", " ").toLowerCase() + " "+e.getValue()));
			}
			return list;
		}
		
//	    public void respawn()
//	    {
//	    	Location loc = location.deserialize();
//	    	Item item = null;
//	    	
//	        if(loc.getChunk().isLoaded()){
//	        	Chunk c = loc.getChunk();
//
//				for (Entity e : c.getEntities()) {
//					if(e.getUniqueId() == uuid)
//						item = (Item)e;
//				}
//				
//				if(item == null){
//					spawn();
//				}
//				else{
//					remove();
//					spawn();
//				}
//	        }
//	        removeDuplicateItems();
//	    }
//
//	    private void removeDuplicateItems(){
//			Block b = location.deserialize().getBlock();
//			for(Entity e : b.getChunk().getEntities()){
//				if((!e.getUniqueId().equals(uuid)) && e.getLocation().getBlock().equals(b) && e.hasMetadata("DisplayItem"))
//					e.remove();
//			}
//		}
//	    
//	    private void removeDuplicateItems()
//	    {
//	    	Location loc = location.deserialize();
//	    	
//	        for(Entity e : loc.getChunk().getEntities())
//	        {
//	            if(e.getLocation().getBlock().equals(loc.getBlock()) && e instanceof Item){
//	            	if(!e.getUniqueId().equals(uuid))
//	            		e.remove();
//	            }
//	        }
//	    }
		
		private String capitalize(String line)
		{
			String[] spaces = line.split("\\s+");
			String capped = "";
			for(String s : spaces){
				if(s.length() > 1)
					capped = capped + Character.toUpperCase(s.charAt(0)) + s.substring(1)+" ";
				else{
					capped = capped + s.toUpperCase()+" ";
				}
			}
			return capped;
		}
}
