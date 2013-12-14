package com.snowgears.shop;

import java.util.Random;

import me.minebuilders.clearlag.Clearlag;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class DisplayItem{
	private Item item;
	private ItemStack itemStack;
	private ShopObject shop;
	
	public DisplayItem(ItemStack is, ShopObject s){
		itemStack = is.clone();
		itemStack.setAmount(1);
		shop = s;
		item = null;
		spawn();
	}
	
	public void spawn(){
		if(item != null)
			return;
		ItemStack is = itemStack.clone();
		ItemMeta im = is.getItemMeta();
		Random rand = new Random();
		im.setDisplayName(ChatColor.GRAY+"DisplayItem"+ChatColor.MAGIC+ rand.nextInt(1000));
		is.setItemMeta(im);
		final Item i = shop.getLocation().getWorld().dropItem(shop.getLocation().clone().add(0.5,1.2,0.5), is);
		i.setVelocity(new Vector(0, 0.1, 0));
		i.setMetadata("DisplayItem", new FixedMetadataValue(Shop.plugin,0));
		this.item = i;
		
		if(Shop.plugin.hasClearLag){
			Clearlag.getEntityManager.addUnremovableEntity(i.getUniqueId().getMostSignificantBits()); //TODO change this to what it should be
		}
	}
	
	public void remove(){
		if(item == null)
			return;
		if(Shop.plugin.hasClearLag){
			Clearlag.getEntityManager.removeUnremovableEntity(item.getUniqueId().getMostSignificantBits()); //TODO change this to what it should be
		}
		item.remove();
		item = null;
	}
	
	public void refresh(){
		remove();
		spawn();
	}
	
	public ItemStack getItemStack(){
		return itemStack;
	}
	
	public Item getRawItem(){
		return item;
	}
	
	public ShopObject getShop(){
		return shop;
	}

}
