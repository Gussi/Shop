package com.snowgears.shop.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerPrepareCreateShopEvent extends Event implements Cancellable{

	 private static final HandlerList handlers = new HandlerList();
	    private Player player;
	    private Location signLocation;
	 	private Location chestLocation;
		private Double price = null;
		private Integer amount = null;
		private boolean isSellingShop = true;
		private boolean isAdminShop = true;
	 	private boolean cancelled;
	    
		public PlayerPrepareCreateShopEvent(Player p, Location sl, Location cl, double pr, int a, boolean sell, boolean admin) {
			player = p;
			signLocation = sl;
			chestLocation = cl;
			price = pr;
			amount = a;
			isSellingShop = sell;
			isAdminShop = admin;
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
	    
	    public boolean isSellingShop() {
	        return isSellingShop;
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
