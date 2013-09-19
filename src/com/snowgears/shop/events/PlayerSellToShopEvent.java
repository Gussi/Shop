package com.snowgears.shop.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;

public class PlayerSellToShopEvent extends Event implements Cancellable{

	 private static final HandlerList handlers = new HandlerList();
	    private Player player;
	 	private ShopObject shop;
	 	private ItemStack itemSold;
	 	private double moneyReceived = 0; //this could be an itemstack if not using vault
	 	private ItemStack itemReceived; //this could be an itemstack if not using vault
	 	private boolean cancelled;
	    
		public PlayerSellToShopEvent(Player p, ShopObject s, ItemStack is, double received) {
			player = p;
			shop = s;
			itemSold = is;
			moneyReceived = received;
	    }
		
		public PlayerSellToShopEvent(Player p, ShopObject s, ItemStack is, ItemStack received) {
			player = p;
			shop = s;
			itemSold = is;
			itemReceived = received;
	    }
		
		public Player getPlayer(){
			return player;
		}
	 
	    public ShopObject getShop() {
	        return shop;
	    }
	    
	    public ItemStack getItemSold() {
	        return itemSold;
	    }
	    
	    public boolean tradedItems() {
	       if(Shop.plugin.useVault == false)
	    	   return true;
	       return false;
	    }
	    
	    public ItemStack getItemReceived(){
	    	if(tradedItems())
	    		return itemReceived;
	    	return new ItemStack(Material.AIR);
	    }
	    
	    public double getMoneyReceived(){
	    	return moneyReceived;
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
