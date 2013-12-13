package com.snowgears.shop;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopObject{

	private Location location;
	private Location signLocation;
	private String owner;
	private DisplayItem displayItem;
	private Double price;
	private Integer amount;
	private Integer timesUsed;
	private boolean isAdminShop;
	private ShopType type;

	public ShopObject(Location loc, 
			Location signLoc, 
			String player, 
			ItemStack is,
			double pri,
			Integer amt,
			Boolean admin,
			ShopType t,
			int amtUsed){ 
		location = loc;
		signLocation = signLoc;
		owner = player;
		price = pri;
		amount = amt;
		isAdminShop = admin;
		type = t;
		timesUsed = amtUsed;
		
		displayItem = new DisplayItem(is, this);
	}
	
	public Location getLocation(){
		return location;
	}
	
	public Inventory getInventory(){
		return ((Chest)location.getBlock().getState()).getInventory();
	}
	
	public Location getSignLocation(){
		return signLocation;
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

	public boolean isAdminShop(){
		return isAdminShop;
	}
	
	public ShopType getType(){
		return type;
	}
	
	public int getTimesUsed(){
		return timesUsed;
	}
	
	public void addUse(){
		timesUsed++;
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
	
	public boolean canAcceptAnotherTransaction(){
		if(this.isAdminShop)
			return true;
		if(type == ShopType.SELLING){
			int validItemsShopHas = Shop.plugin.miscListener.getAmount(getInventory(), this.getDisplayItem().getItemStack());
			//shop does not have enough items to make another sale
			if(validItemsShopHas < this.getAmount())
				return false;
			
			//using item economy
			if(Shop.plugin.econ == null){
				return inventoryHasRoom(new ItemStack(Shop.plugin.economyMaterial, (int)this.getPrice()));
			}
		}
		else if(type == ShopType.BUYING){
			//using item economy
			if(Shop.plugin.econ == null){
				int currencyShopHas = Shop.plugin.miscListener.getAmount(getInventory(), new ItemStack(Shop.plugin.economyMaterial));
				//shop does not have enough item currency in stock to make another sale
				if(currencyShopHas < this.getPrice())
					return false;
			}
			//using vault economy
			else{
				double currencyShopHas = Shop.plugin.econ.getBalance(this.getOwner());
				//owner of shop does not have enough money for the shop to make a sale
				if(currencyShopHas < this.getPrice())
					return false;
			}
			ItemStack stackToGoInShop = this.getDisplayItem().getItemStack().clone();
			stackToGoInShop.setAmount(this.getAmount());
			return inventoryHasRoom(stackToGoInShop);
		}
		return true;
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
	
	private boolean inventoryHasRoom(ItemStack itemToAdd){
		int freeSpace = 0;
		for (ItemStack i : getInventory()) {
			if(i == null)
				freeSpace += itemToAdd.getType().getMaxStackSize();
			else if (i.getData().equals(itemToAdd.getData())) 
				freeSpace += i.getType().getMaxStackSize() - i.getAmount();
		}
		return (itemToAdd.getAmount() <= freeSpace);
	}
}
