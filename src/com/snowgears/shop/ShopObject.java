package com.snowgears.shop;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class ShopObject{

	private Location location = null;
	private Location signLocation = null;
	private String owner = null;
//	private DisplayItem displayItem = null;
	private Item displayItem = null;
	private Double price = null;
	private Integer amount = null;
	private boolean isAdminShop = true;
	private ShopType type = ShopType.SELLING;

	public ShopObject(Location loc, 
			Location signLoc, 
			String player, 
			ItemStack is,
			double pri,
			Integer amt,
			Boolean admin,
			ShopType t){ 
		location = loc;
		signLocation = signLoc;
		owner = player;
		price = pri;
		amount = amt;
		isAdminShop = admin;
		type = t;
		
		this.spawnDisplayItem(is);
	}
	
	private void spawnDisplayItem(ItemStack is){
		is.setAmount(1);
		final Item i = location.getWorld().dropItem(location.clone().add(0.5,1.2,0.5), is);
		i.setVelocity(new Vector(0, 0.1, 0));
		i.setMetadata("DisplayItem", new FixedMetadataValue(Shop.plugin,0));
		this.displayItem = i;
	}
	
	public void removeDisplayItem(){
		displayItem.remove();
	}
	
	public Location getLocation(){
		return location;
	}
	
	public Inventory getInventory(){
		Chest chest = (Chest)location.getBlock().getState();
		return chest.getInventory();
	}
	
	public Location getSignLocation(){
		return signLocation;
	}

	public String getOwner(){
		return owner;
	}
	
	public Item getDisplayItem(){
		return displayItem;
	}
	
	public double getPrice(){
		return price;
	}
	
	public int getAmount(){
		return amount;
	}

	public boolean isAdminShop(){
		return isAdminShop;
	}
	
	public ShopType getType(){
		return type;
	}
	
	public void setDisplayItem(ItemStack is){
		this.removeDisplayItem();
		this.spawnDisplayItem(is);
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
	
	public void setType(ShopType t){
		type = t;
		updateSign();
	}
	
	public void setAdmin(boolean b){
		isAdminShop = b;
		updateSign();
	}
	
	public void updateSign(){
		Sign signBlock = (Sign)signLocation.getBlock().getState();
		
		signBlock.setLine(0, ChatColor.BOLD+"[shop]");
		if(type == ShopType.SELLING)
			signBlock.setLine(1, "Selling: "+ChatColor.BOLD+ amount);
		else if(type == ShopType.BUYING)
			signBlock.setLine(1, "Buying: "+ChatColor.BOLD+ amount);
		else
			signBlock.setLine(1, "Bartering: "); //TODO, "Bartering: Dirt for Stone, etc...
		
		if((price % 1) == 0){
			signBlock.setLine(2, ChatColor.GREEN+""+ price.intValue() +" "+ Shop.plugin.economyDisplayName);
		}
		else{
			signBlock.setLine(2, ChatColor.GREEN+""+ price +" "+ Shop.plugin.economyDisplayName);
		}
		
		if(isAdminShop){
			signBlock.setLine(3, "admin");
		}
		else{
			signBlock.setLine(3, this.owner);
		}
		signBlock.update(true);
	}
	
	public void delete(){
		Shop.plugin.shopHandler.removeShop(this);
		this.getDisplayItem().remove();
		
		Block b = this.getSignLocation().getBlock();
		if(b.getType() == Material.WALL_SIGN){
			Sign signBlock = (Sign)b.getState();
			signBlock.setLine(0, "");
			signBlock.setLine(1, "");
			signBlock.setLine(2, "");
			signBlock.setLine(3, "");
			signBlock.update(true);
		}
	}
	
	@Override
	public String toString(){
		return owner + "." + displayItem.getItemStack().getType().toString();
	}
}
