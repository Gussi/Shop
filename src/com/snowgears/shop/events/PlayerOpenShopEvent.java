package com.snowgears.shop.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;

import com.snowgears.shop.ShopObject;

public class PlayerOpenShopEvent extends Event{

	 	private static final HandlerList handlers = new HandlerList();
	    private Player player;
	 	private ShopObject shop;
	 	private Inventory inventory;
	    
		public PlayerOpenShopEvent(Player p, ShopObject s, Inventory inv) {
			player = p;
			shop = s;
			inventory = inv;
	    }

		public Player getPlayer(){
			return player;
		}
	 
	    public ShopObject getShop() {
	        return shop;
	    }
	    
	    public Inventory getInventory() {
	        return inventory;
	    }

	    public HandlerList getHandlers() {
	        return handlers;
	    }
	 
	    public static HandlerList getHandlerList() {
	        return handlers;
	    }
}
