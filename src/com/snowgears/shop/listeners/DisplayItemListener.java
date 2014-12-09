package com.snowgears.shop.listeners;

import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;

public class DisplayItemListener implements Listener {

	public Shop plugin = Shop.getPlugin();
	
	public DisplayItemListener(Shop instance) {
		plugin = instance;
	}

	@EventHandler
	public void onItemPickup(PlayerPickupItemEvent event) {
		if (event.getItem().hasMetadata("DisplayItem")) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onWaterFlow(BlockFromToEvent event) {
		ShopObject shop = plugin.getShopHandler().getShop(event.getToBlock().getRelative(BlockFace.DOWN).getLocation());
		if (shop != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler (priority = EventPriority.HIGHEST)
	public void onItemDespawn(ItemDespawnEvent event) {
		if (event.getEntity().hasMetadata("DisplayItem")) {
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
		ShopObject shop = plugin.getShopHandler().getShop(event.getBlock().getRelative(BlockFace.DOWN).getLocation());
		if (shop != null) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		event.getEntity().setCanPickupItems(false);
	}
}
