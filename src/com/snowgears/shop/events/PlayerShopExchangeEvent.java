package com.snowgears.shop.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;

public class PlayerShopExchangeEvent extends Event implements Cancellable{

	 private static final HandlerList handlers = new HandlerList();
	    private Player player;
	 	private ShopObject shop;
	 	private boolean cancelled;
	    
		public PlayerShopExchangeEvent(Player p, ShopObject s) {
			player = p;
			shop = s;
	    }

		public Player getPlayer(){
			return player;
		}
	 
	    public ShopObject getShop() {
	        return shop;
	    }
	    
	    public boolean playerReceivedItem(){
	    	return (shop.getType() == ShopType.SELLING || shop.getType() == ShopType.BARTER);
	    }
	    
	    public ItemStack getItemPlayerReceived(){
	    	if(playerReceivedItem()){
	    		ItemStack item = shop.getDisplayItem().getItemStack();
	    		item.setAmount(shop.getAmount());
	    		return item;
	    	}
	    	return null;
	    }
	    
	    public boolean shopReceivedItem(){
	    	return shop.getType() == ShopType.BUYING;
	    }

	    public ItemStack getItemShopReceived() {
	    	if(shopReceivedItem()){
	    		ItemStack item = shop.getDisplayItem().getItemStack();
	    		item.setAmount(shop.getAmount());
	    		return item;
	    	}
	    	return null;
	    }
	    
	    public boolean playerReceivedMoney(){
	    	return shop.getType() == ShopType.SELLING;
	    }

	    public double getMoneyPlayerReceived(){
	    	if(playerReceivedMoney()){
	    		return shop.getPrice();
	    	}
	    	return 0;
	    }
	    
	    public boolean shopReceivedMoney(){
	    	return shop.getType() == ShopType.BUYING;
	    }
	    
	    public double getMoneyShopReceived(){
	    	if(shopReceivedMoney()){
	    		return shop.getPrice();
	    	}
	    	return 0;
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
