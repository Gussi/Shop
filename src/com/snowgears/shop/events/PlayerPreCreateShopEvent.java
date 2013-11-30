package com.snowgears.shop.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.snowgears.shop.ShopType;

public class PlayerPreCreateShopEvent extends Event implements Cancellable{

	 private static final HandlerList handlers = new HandlerList();
	    private Player player;
	    private Location signLocation;
	 	private Location chestLocation;
		private Double price = null;
		private Integer amount = null;
		private boolean isAdminShop = true;
		private ShopType shopType;
	 	private boolean cancelled;
	    
		public PlayerPreCreateShopEvent(Player p, Location sl, Location cl, double pr, int a, boolean admin, ShopType t) {
			player = p;
			signLocation = sl;
			chestLocation = cl;
			price = pr;
			amount = a;
			isAdminShop = admin;
			shopType = t;
	    }
		
		public Player getPlayer(){
			return player;
		}
	 
	    public Location getSignLocation() {
	        return signLocation;
	    }
	    
	    public Location getChestLocation() {
	        return chestLocation;
	    }
	    
	    public double getShopPrice() {
	        return price;
	    }
	    
	    public int getShopAmount() {
	        return amount;
	    }
	    
	    public ShopType getShopType() {
	        return shopType;
	    }
	    
	    public boolean isAdminShop() {
	        return isAdminShop;
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
