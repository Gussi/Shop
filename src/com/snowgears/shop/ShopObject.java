package com.snowgears.shop;

import java.io.Serializable;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import com.snowgears.shop.utils.SerializableLocation;

public class ShopObject implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1235046487997062300L;
	/**
	 * 
	 */
	private SerializableLocation location = null;
	private SerializableLocation signLocation = null;
	private SerializableLocation showcaseLocation = null;
	private String owner = null;
	private DisplayItem displayItem = null;
	private Double price = null;
	private Integer amount = null;
	private boolean isSellingShop = true;
	private boolean isAdminShop = true;

	public ShopObject(SerializableLocation loc, 
			SerializableLocation signLoc, 
			SerializableLocation itemLoc,
			String player, 
			ItemStack is,
			double pri,
			Integer amt,
			Boolean selling,
			Boolean admin){ 
		location = loc;
		signLocation = signLoc;
		showcaseLocation = itemLoc;
		owner = player;
		price = pri;
		amount = amt;
		isSellingShop = selling;
		isAdminShop = admin;
		
		displayItem = new DisplayItem(this, itemLoc.deserialize(), is);
	}
	
	public Location getLocation(){
		return location.deserialize();
	}
	
	public Location getSignLocation(){
		return signLocation.deserialize();
	}
	
	public Location getShowcaseLocation(){
		return showcaseLocation.deserialize();
	}
	
	public String getOwner(){
		return owner;
	}
	
	public DisplayItem getDisplayItem(){
		return displayItem;
	}
	
	public double getPrice(){
		return price;
	}
	
	public int getAmount(){
		return amount;
	}
	
	public boolean isSellingShop(){
		return isSellingShop;
	}
	
	public boolean isAdminShop(){
		return isAdminShop;
	}
	
	public void setDisplayItem(ItemStack is){
		displayItem = new DisplayItem(this, displayItem.getLocation(), is);
	}
	
	public void setOwner(String s){
		owner = s;
		updateSign();
	}
	
	public void setPrice(double d){
		price = d;
		updateSign();
	}
	
	public void setAmount(int a){
		amount = a;
		updateSign();
	}
	
	public void setSelling(boolean b){
		isSellingShop = b;
		updateSign();
	}
	
	public void setAdmin(boolean b){
		isAdminShop = b;
		updateSign();
	}
	
	private void updateSign(){
		Sign signBlock = (Sign)getSignLocation().getBlock().getState();
		
		signBlock.setLine(0, ChatColor.BOLD+"[shop]");
		if(isSellingShop)
			signBlock.setLine(1, "Selling: "+ChatColor.BOLD+ amount);
		else
			signBlock.setLine(1, "Buying: "+ChatColor.BOLD+ amount);
		
		if((price % 1) == 0){
			signBlock.setLine(2, ChatColor.GREEN+""+ price.intValue() +" "+ Shop.economyItemName);
		}
		else{
			signBlock.setLine(2, ChatColor.GREEN+""+ price +" "+ Shop.economyItemName);
		}
		
		if(isAdminShop){
			signBlock.setLine(3, "admin");
		}
		else{
			signBlock.setLine(3, "");
		}
		signBlock.update(true);
	}
}
