package com.snowgears.shop.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
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

public class DisplayItemListener implements Listener{

	public Shop plugin = Shop.plugin;
	
	public DisplayItemListener(Shop instance)
    {
        plugin = instance;
    }

	@EventHandler
	public void onItemPickup(PlayerPickupItemEvent event){
		if(event.getItem().hasMetadata("DisplayItem")){
			event.setCancelled(true);
			return;
		}
		for(ShopObject shop : plugin.alisten.allShops){
			if(shop.getDisplayItem().getUniqueId().equals(event.getItem().getUniqueId())){
				event.setCancelled(true);
				return;
			}	
		}
	}

	@EventHandler
	public void onWaterFlow(BlockFromToEvent event){
		if(event.getToBlock().getRelative(BlockFace.DOWN).getType() == Material.CHEST){
			event.setCancelled(true);
		}
	}

	@EventHandler (priority = EventPriority.HIGHEST)
	public void onItemDespawn(ItemDespawnEvent event){
		if(event.getEntity().hasMetadata("DisplayItem")){
			event.setCancelled(true);
			return;
		}
		for(ShopObject shop : plugin.alisten.allShops){
			if(shop.getDisplayItem().getUniqueId().equals(event.getEntity().getUniqueId())){
				event.setCancelled(true);
//				shop.displayItem.respawn();
				return;
			}	
		}
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event){
		if(event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.CHEST){
			Location loc = event.getBlock().getLocation();
			for(ShopObject shop : plugin.alisten.allShops){
				if(shop.getDisplayItem().getLocation().getBlock().getLocation().equals(loc)){
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onCreatureSpawn(CreatureSpawnEvent event){
		event.getEntity().setCanPickupItems(false);
	}
}
