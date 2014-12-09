package com.snowgears.shop;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PlayerClickData {

	private String player;
	private GameMode oldGameMode;
	private Location signLocation;
	private double shopPrice;
	private int shopAmount;
	private boolean isAdminShop;
	private ShopType shopType;
	
	public PlayerClickData(Player p, Location l, double price, int amt, boolean admin, ShopType type) {
		player = p.getName();
		oldGameMode = p.getGameMode();
		signLocation = l;
		shopPrice = price;
		shopAmount = amt;
		isAdminShop = admin;
		shopType = type;
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(player);
	}
	
	public GameMode getOldGameMode() {
		return oldGameMode;
	}
	
	public Location getChestLocation() {
		Block b = signLocation.getBlock();
		org.bukkit.material.Sign sign = (org.bukkit.material.Sign)b.getState().getData();
		return b.getRelative(sign.getAttachedFace()).getLocation();
		
	}
	
	public Location getSignLocation() {
		return signLocation;
	}

	public double getShopPrice() {
		return shopPrice;
	}
	
	public int getShopAmount() {
		return shopAmount;
	}
	
	public boolean getShopAdmin() {
		return isAdminShop;
	}
	
	public ShopType getShopType() {
		return shopType;
	}
}
