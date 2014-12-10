package com.snowgears.shop.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;

public class PlayerShopExchangeEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private ShopObject shop;
	private ItemStack itemPlayerReceived;
	private ItemStack itemShopReceived;
	private double moneyPlayerReceived;
	private double moneyShopReceived;
	private boolean cancelled;
	
	public PlayerShopExchangeEvent(Player p, ShopObject s) {
		player = p;
		shop = s;
		
		if (playerReceivedItem()){
			if (shop.getType() == ShopType.SELLING){
				itemPlayerReceived = shop.getDisplayItem().getItemStack();
				itemPlayerReceived.setAmount(shop.getAmount());
			}
			else if (shop.getType() == ShopType.BUYING){
				itemPlayerReceived = Shop.getPlugin().getEconomyItem().clone();
				itemPlayerReceived.setAmount((int)shop.getPrice());
			}
		}
		else {
			itemPlayerReceived = null;
		}
		
		if (shopReceivedItem()){
			if (shop.getType() == ShopType.SELLING){
				itemShopReceived = Shop.getPlugin().getEconomyItem().clone();
				itemShopReceived.setAmount((int)shop.getPrice());
			}
			else if (shop.getType() == ShopType.BUYING){
				itemShopReceived = shop.getDisplayItem().getItemStack();
				itemShopReceived.setAmount(shop.getAmount());
			}
		}
		else {
			itemShopReceived = null;
		}
		
		if (playerReceivedMoney()){
			moneyPlayerReceived = shop.getPrice();
		}
		else {
			moneyPlayerReceived = 0;
		}
		
		if (shopReceivedMoney()){
			moneyShopReceived = shop.getPrice();
		}
		else {
			moneyShopReceived = 0;
		}
	}

	public Player getPlayer(){
		return player;
	}
	
	public ShopObject getShop() {
		return shop;
	}
	
	public boolean playerReceivedItem(){
		return (shop.getType() == ShopType.SELLING || (shop.getType() == ShopType.BUYING && Shop.getPlugin().getEconomy() == null));
	}
	
	public ItemStack getItemPlayerReceived(){
		return itemPlayerReceived;
	}
	
	public boolean shopReceivedItem(){
		return shop.getType() == ShopType.BUYING || (shop.getType() == ShopType.SELLING && Shop.getPlugin().getEconomy() == null);
	}

	public ItemStack getItemShopReceived() {
		return itemShopReceived;
	}
	
	public boolean playerReceivedMoney(){
		return shop.getType() == ShopType.BUYING && Shop.getPlugin().getEconomy() != null;
	}

	public double getMoneyPlayerReceived(){
		return moneyPlayerReceived;
	}
	
	public boolean shopReceivedMoney(){
		return shop.getType() == ShopType.SELLING && Shop.getPlugin().getEconomy() != null;
	}
	
	public double getMoneyShopReceived(){
		return moneyShopReceived;
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
