package com.snowgears.shop.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.snowgears.shop.ShopObject;

public class PlayerCreateShopEvent extends Event implements Cancellable{

	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private ShopObject shop;
	private Location location;
	private boolean cancelled;
		
	public PlayerCreateShopEvent(Player p, ShopObject s) {
		player = p;
		shop = s;
		location = s.getLocation();
	}
	
	public Player getPlayer(){
		return player;
	}
	
	public ShopObject getShop() {
		return shop;
	}
	
	public Location getLocation(){
		return location;
	}
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean set) {
		cancelled = set;
	}
}
