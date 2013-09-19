package com.snowgears.shop.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;

public class PlayerBuyFromShopEvent extends Event implements Cancellable{

	 private static final HandlerList handlers = new HandlerList();
	    private Player player;
	 	private ShopObject shop;
	 	private ItemStack itemPurchased;
	 	private double moneyPaid = 0; //this could be an itemstack if not using vault
	 	private ItemStack itemPaid; //this could be an itemstack if not using vault
	 	private boolean cancelled;
	    
		public PlayerBuyFromShopEvent(Player p, ShopObject s, ItemStack is, double paid) {
			player = p;
			shop = s;
			itemPurchased = is;
			moneyPaid = paid;
	    }
		
		public PlayerBuyFromShopEvent(Player p, ShopObject s, ItemStack is, ItemStack paid) {
			player = p;
			shop = s;
			itemPurchased = is;
			itemPaid = paid;
	    }
		
		public Player getPlayer(){
			return player;
		}
	 
	    public ShopObject getShop() {
	        return shop;
	    }
	    
	    public ItemStack getItemPurchased() {
	        return itemPurchased;
	    }
	    
	    public boolean usedVaultCurrency() {
	       return(Shop.useVault);
	    }
	    
	    public ItemStack getItemPaid(){
	    	if(!usedVaultCurrency())
	    		return itemPaid;
	    	return new ItemStack(Material.AIR);
	    }
	    
	    public double getMoneyPaid(){
	    	return moneyPaid;
	    }
	    
	    public void setItemPurchased(ItemStack is){
	    	itemPurchased = is;
	    }
	    
	    public void setItemPaid(ItemStack is){
	    	itemPaid = is;
	    }
	    
	    public void setMoneyPaid(double m){
	    	moneyPaid = m;
	    }
	    
	    public HandlerList getHandlers() {
	        return handlers;
	    }
	 
	    public static HandlerList getHandlerList() {
	        return handlers;
	    }

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void setCancelled(boolean set) {
			cancelled = set;
		}
}
