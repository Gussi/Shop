package com.snowgears.shop.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import com.snowgears.shop.PlayerClickData;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.events.PlayerCreateShopEvent;
import com.snowgears.shop.events.PlayerDestroyShopEvent;
import com.snowgears.shop.events.PlayerPreCreateShopEvent;
import com.snowgears.shop.events.PlayerShopExchangeEvent;

public class MiscListener implements Listener{

	public Shop plugin = Shop.getPlugin();
	private HashMap<String, PlayerClickData> playersInLockedCreative = new HashMap<String, PlayerClickData>();

	public MiscListener(Shop instance)	   {
		plugin = instance;
	}
	
	//give player specified amount of currency on first login
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.hasPlayedBefore() == false) {
			if (plugin.getStartingCurrency() > 0) {
				if (plugin.getEconomy() == null) {
					ItemStack startItems = plugin.getEconomyItem().clone();
					startItems.setAmount(plugin.getStartingCurrency());
					player.getInventory().addItem(startItems);
				}
				else {
					plugin.getEconomy().depositPlayer(player.getName(), plugin.getStartingCurrency());
				}
			}
		}
	}
	
	//prevent shops and their signs from being deleted in an explosion
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onExplosion(EntityExplodeEvent event) {
		List<Block> list = event.blockList();

		final LinkedList<ShopObject> shopsInExplosion = new LinkedList<ShopObject>();
		for(int i=0; i<list.size(); i++) {
			Block b = list.get(i);
			if (b.getType() == Material.CHEST) {
				event.blockList().remove(i);
				ShopObject shop = plugin.getShopHandler().getShop(b.getLocation());
				if (shop != null)
					shopsInExplosion.add(shop);
			}
			else if (b.getType() == Material.WALL_SIGN) {
				event.blockList().remove(i);
			}
		}
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
			public void run() { 
				for(ShopObject shop : shopsInExplosion) {
					shop.getDisplayItem().refresh(); 
				} 
			}
		}, 100L); 
	}
	
	//prevent empying of bucket when player clicks on shop sign
	//also prevent when emptying on display item itself
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Block b = event.getBlockClicked();
		
		if (b.getType() == Material.WALL_SIGN) {
			org.bukkit.material.Sign sign = (org.bukkit.material.Sign)event.getBlockClicked().getState().getData();
			ShopObject shop = plugin.getShopHandler().getShop(b.getRelative(sign.getAttachedFace()).getLocation());
			if (shop != null) {
				event.setCancelled(true);
			}
		}
		Block blockToFill = event.getBlockClicked().getRelative(event.getBlockFace());
		ShopObject shop = plugin.getShopHandler().getShop(blockToFill.getRelative(BlockFace.DOWN).getLocation());
		if (shop != null) {
			event.setCancelled(true);
		}
	}
	
	//=============FROM HERE DOWN : THESE METHODS CALL CUSTOM EVENTS=========================//
	
	//player places a sign on a chest and is about to create a shop, call PlayerPrepareCreateShopEvent
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onSignEdit(SignChangeEvent event) {
		Block b = event.getBlock();
		Player player = event.getPlayer();
		final org.bukkit.material.Sign sign = (org.bukkit.material.Sign)b.getState().getData();
		if (sign.isWallSign() == true)
			return;
		Block relBlock = b.getRelative(sign.getFacing().getOppositeFace());

		double price = 0;
		int amount = 0;
		ShopType type = ShopType.SELLING; //TODO make this barter
		if (relBlock.getType() == Material.CHEST) {
			final Sign signBlock = (Sign)b.getState();
			if (event.getLine(0).equalsIgnoreCase("[shop]")) {
				if (plugin.usePerms() && ! (player.hasPermission("shop.create"))) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
					return;
				}
				
				if (isNumber(event.getLine(1)) == true) {
					amount = Integer.parseInt(event.getLine(1));
					if (amount < 1) {
						player.sendMessage(ChatColor.RED+"The amount (line 2) needs to be positive.");
						return;
					}
				}
				else {
					player.sendMessage(ChatColor.RED+"The amount (line 2) needs to be a number.");
					return;
				}
				
				if (isNumber(event.getLine(2)) == true) {
					price = Double.parseDouble(event.getLine(2));
					if (price < 1) {
						player.sendMessage(ChatColor.RED+"The price (line 3) needs to be positive.");
						return;
					}
				}
				else {
					player.sendMessage(ChatColor.RED+"The price (line 3) needs to be a number.");
					return;
				}

				if (event.getLine(3).isEmpty() || event.getLine(3).toLowerCase().contains("s")) {
					type = ShopType.SELLING;
				} else if (event.getLine(3).toLowerCase().contains("b")) { //TODO this will be "buy" when barter shops are added
					type = ShopType.BUYING;
				}
				
				boolean isAdmin = false;
				if (event.getLine(3).toLowerCase().contains("admin")) {
					if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
						isAdmin = true;
					}
				}
						
				relBlock.getRelative(sign.getFacing()).setType(Material.WALL_SIGN);
				
				final Sign newSign = (Sign)relBlock.getRelative(sign.getFacing()).getState();
				newSign.setLine(0, ChatColor.BOLD+"[shop]");
				if (type == ShopType.SELLING) {
					newSign.setLine(1, "Selling: "+ChatColor.BOLD+ amount);
				} else {
					newSign.setLine(1, "Buying: "+ChatColor.BOLD+ amount);
				}
				
				if (plugin.useVault()) {
					newSign.setLine(2, ChatColor.RED+""+ price +" "+ plugin.getEconomyDisplayName());
				} else {
					newSign.setLine(2, ChatColor.RED+""+ (int)price +" "+ plugin.getEconomyDisplayName());
				}
				
				if (isAdmin) {
					newSign.setLine(3, "admin");
				} else {
					newSign.setLine(3, "");
				}
				
				org.bukkit.material.Sign matSign = new org.bukkit.material.Sign(Material.WALL_SIGN);
				matSign.setFacingDirection(sign.getFacing());
				
				newSign.setData(matSign);
				newSign.update();

				signBlock.update();
				
				PlayerPreCreateShopEvent e = new PlayerPreCreateShopEvent(player, newSign.getLocation(), relBlock.getLocation(), price, amount, isAdmin, type);
				plugin.getServer().getPluginManager().callEvent(e);
			}
		}
	}
	
	//player clicks on (pre-shop)sign with an item, call PlayerCreateShopEvent 
	// FIXME: Here be dragons, 1.8 breaks here
	@EventHandler
	public void onPreShopSignClick(PlayerInteractEvent event) {
		if (event.isCancelled()) {
			return;
		}
		final Player player = event.getPlayer();
		
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			final Block clicked = event.getClickedBlock();
			
			if (clicked.getType() == Material.WALL_SIGN) {
				if (!plugin.getShopListener().signsAwaitingItems.containsKey(clicked.getLocation())) {
					return;
				}

				if (plugin.usePerms() && ! (player.hasPermission("shop.create"))) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
					return;
				}

				final Sign sign = (Sign)clicked.getState();
				
				int amount = Integer.parseInt(sign.getLine(1).replaceAll("\\D+", ""));
				double price = Double.parseDouble(sign.getLine(2).substring(2, sign.getLine(2).indexOf(" ")));
				
				ShopType type = ShopType.BUYING; //TODO this will be barter in the future
				if (sign.getLine(1).toLowerCase().contains("sell")) {
					type = ShopType.SELLING;
				} else if (sign.getLine(1).toLowerCase().contains("buy")) {
					type = ShopType.BUYING;
				}
				
				boolean isAdmin = false;
				if (sign.getLine(3).equalsIgnoreCase("admin")) {
					isAdmin = true;
				}

				String owner = player.getName();
				if (isAdmin == false) {
					sign.setLine(3, player.getName());
				} else {
					owner = "admin";
				}

				org.bukkit.material.Sign s = (org.bukkit.material.Sign)clicked.getState().getData();
				Block chest = clicked.getRelative(s.getAttachedFace());
				
				if (player.getItemInHand().getType() == Material.AIR) {
					if (type == ShopType.SELLING) {
						player.sendMessage(ChatColor.RED+"You must be holding the item you want to sell!");
					}
					else {
						PlayerClickData pcd = new PlayerClickData(player, clicked.getLocation(), price, amount, isAdmin, type);
						playersInLockedCreative.put(player.getName(), pcd);
						player.setGameMode(GameMode.CREATIVE);
						player.sendMessage("_____________________________________________________");
						player.sendMessage(ChatColor.GRAY+"You are now in locked creative mode so you can choose the item you want to receive.");
						player.sendMessage(ChatColor.WHITE+"To select the item, pick it up and drop it outside of the inventory window.");
						player.sendMessage(ChatColor.GOLD+"Open your inventory and select the item you want to receive.");
						player.sendMessage("_____________________________________________________");
					}
					return;
				}
				
				Block aboveShop = chest.getLocation().getBlock().getRelative(BlockFace.UP);
				if (aboveShop.getType() == Material.AIR) {
					final ShopObject shop = new ShopObject(chest.getLocation(), 
							clicked.getLocation(),
							owner,
							player.getItemInHand(),
							price,
							amount, 
							isAdmin,
							type,
							0);
					
					PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
				}
				else {
					player.sendMessage(ChatColor.RED+"This shop could not created because there is no room for a display item.");
					if (plugin.getShopListener().signsAwaitingItems.containsKey(sign.getLocation())) {
						plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
							public void run() { 
								plugin.getShopListener().signsAwaitingItems.remove(sign.getLocation());
								} 
						}, 1); //1 tick
					}
					sign.setLine(0, "");
					sign.setLine(1, "");
					sign.setLine(2, "");
					sign.setLine(3, "");
					sign.update(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerMoveWhenSelecting(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		PlayerClickData pcd = playersInLockedCreative.get(player.getName());
		if (pcd != null) {
			event.setTo(event.getFrom());
		}
			
	}
	
	@EventHandler
	public void onPlayerInteractWhenSelecting(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		PlayerClickData pcd = playersInLockedCreative.get(player.getName());
		if (pcd != null) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void inventoryClickWhenSelecting(InventoryCreativeEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		Player player = (Player)event.getWhoClicked();
		PlayerClickData pcd = playersInLockedCreative.get(player.getName());
		if (pcd != null) {
			if (event.getSlotType() == SlotType.OUTSIDE) {
				if (pcd.getShopType() == ShopType.BUYING) {
					final ShopObject shop = new ShopObject(pcd.getChestLocation(), 
							pcd.getSignLocation(),
							player.getName(),
							event.getCursor(),
							pcd.getShopPrice(),
							pcd.getShopAmount(), 
							pcd.getShopAdmin(),
							pcd.getShopType(),
							0);

					PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
				}
				player.closeInventory();
				player.setGameMode(pcd.getOldGameMode());
				playersInLockedCreative.remove(player.getName());
				//TODO barter shop will have two items so you will have to figure out how to do this cleanly
			}
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void inventoryCloseWhenSelecting(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player)event.getPlayer();

		PlayerClickData pcd = playersInLockedCreative.get(player.getName());
		if (pcd != null) {
			player.setGameMode(pcd.getOldGameMode());
			playersInLockedCreative.remove(player.getName());
			System.out.println("Inventory close event called. Setting gamemode back to normal.");
		}
	}
	
	//player destroys shop, call PlayerDestroyShopEvent
	@EventHandler (priority = EventPriority.HIGHEST)
	public void shopDestroy(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		Block b = event.getBlock();
		
		if (plugin.getShopListener().signsAwaitingItems.containsKey(b.getLocation())) {
			event.setCancelled(true);
			return;
		}
	
		Player player = event.getPlayer();
		
		if (b.getType() == Material.WALL_SIGN) {
			org.bukkit.material.Sign sign = (org.bukkit.material.Sign)b.getState().getData();
			ShopObject shop = plugin.getShopHandler().getShop(b.getRelative(sign.getAttachedFace()).getLocation());
			if (shop == null) {
				return;
			}

			//player trying to break their own shop
			if (shop.getOwner().equals(player.getName())) {
				if (plugin.usePerms() && ! (player.hasPermission("shop.destroy"))) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED+"You are not authorized to destroy shops.");
					return;
				}

				PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
				plugin.getServer().getPluginManager().callEvent(e);
				if (e.isCancelled()) {
					event.setCancelled(true);
				}

				return;
			}
			//player trying to break other players shop
			else {
				if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
					PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
					if (e.isCancelled()) {
						event.setCancelled(true);
					}
				}
				else
					event.setCancelled(true);
			}
		}
		else if (b.getType() == Material.CHEST) {
			Chest chest = (Chest)b.getState();
			InventoryHolder ih = chest.getInventory().getHolder();
			
			if (ih instanceof DoubleChest) {
				DoubleChest dchest = (DoubleChest)ih;
				Chest chestLeft = (Chest)dchest.getLeftSide();
				Chest chestRight = (Chest)dchest.getRightSide();
				
				ShopObject shopLeft = plugin.getShopHandler().getShop(chestLeft.getLocation());
				ShopObject shopRight = plugin.getShopHandler().getShop(chestRight.getLocation());
				
				if (shopLeft != null) {
					event.setCancelled(true);
					//player trying to break their own shop
					if (shopLeft.getOwner().equals(player.getName())) {
						player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
						return;
					}
				}
				else if (shopRight != null) {
					event.setCancelled(true);
					//player trying to break their own shop
					if (shopRight.getOwner().equals(player.getName())) {
						player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
					}
				}
			}
			//instance of single chest
			else {
				ShopObject shop = plugin.getShopHandler().getShop(b.getLocation());
				if (shop == null) {
					return;
				}
				event.setCancelled(true);
				//player trying to break their own shop
				if (shop.getOwner().equals(player.getName())) {
					player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
					return;
				}
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onShopSignClick(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN) {
				event.setCancelled(true);
				
				org.bukkit.material.Sign sign = (org.bukkit.material.Sign)event.getClickedBlock().getState().getData();
				ShopObject shop = plugin.getShopHandler().getShop(event.getClickedBlock().getRelative(sign.getAttachedFace()).getLocation());
				if (shop == null) {
					return;
				}

				Player player = event.getPlayer();
				
				if (plugin.usePerms() && ! (player.hasPermission("shop.use"))) {
					player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
					return;
				}
				
				//player right clicked own sign
				if (shop.getOwner().equals(player.getName())) {
					if (shop.getType() == ShopType.SELLING && !plugin.useVault()) {
						int amountOfMoney = getAmount(shop.getInventory(), plugin.getEconomyItem());
						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+plugin.getEconomyDisplayName()+".");
					}
					else if (shop.getType() == ShopType.BUYING) {
						int amountOfItems = getAmount(shop.getInventory(), shop.getDisplayItem().getItemStack());
						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getItemStack().getType().name().replace("_", " ").toLowerCase()+".");
					}

					player.sendMessage(ChatColor.GRAY+"This shop has been used a total of "+ChatColor.WHITE+shop.getTimesUsed()+ChatColor.GRAY+" time(s).");
				}
				//player right clicked other shops' sign
				else {
					//check that both shop and player have enough funds to complete transaction
					
					//check if shop can accept another transaction
					if (!shop.canAcceptAnotherTransaction()) {
						player.sendMessage(ChatColor.RED+"This shop is out of items, is too full, or the owner is out of funds.");
						return;
					}
					
					//check that player has enough funds to complete transaction
					if (shop.getType() == ShopType.SELLING) {
						//using item economy
						if (plugin.getEconomy() == null) {
							int currencyPlayerHas = getAmount(player.getInventory(), plugin.getEconomyItem());
							if (currencyPlayerHas < shop.getPrice()) {
								player.sendMessage(ChatColor.RED+"You do not have enough "+plugin.getEconomyDisplayName()+" to buy from this shop.");
								return;
							}
						}
						//using vault economy
						else {
							double currencyPlayerHas = plugin.getEconomy().getBalance(player.getName());
							if (currencyPlayerHas < shop.getPrice()) {
								player.sendMessage(ChatColor.RED+"You do not have enough "+plugin.getEconomyDisplayName()+" to buy from this shop.");
								return;
							}
						}
					}
					else if (shop.getType() == ShopType.BUYING) {
						//check that player has enough items to sell to shop
						int amountOfItemsPlayerHas = getAmount(player.getInventory(), shop.getDisplayItem().getItemStack());
						if (amountOfItemsPlayerHas < shop.getAmount()) {
							player.sendMessage(ChatColor.RED+"You do not have enough items to sell to this shop.");
							return;
						}
					}
					
					PlayerShopExchangeEvent e = new PlayerShopExchangeEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
				}
			}
		}
	}

	//get amount of itemstack in inventory
	public int getAmount(Inventory inventory, ItemStack is)	{
		MaterialData md = is.getData();
		ItemMeta im = is.getItemMeta();
		ItemStack[] items = inventory.getContents();
		int has = 0;
		for (ItemStack item : items) {
			// Skip on empty
			if (item == null) {
				continue;
			}
			
			// Check if material data doesn't match 
			if (!item.getData().equals(md)) {
				continue;
			}
			
			// Check if item meta data doesn't match
			if (!item.getItemMeta().equals(im)) {
				continue;
			}
			
			// Add to item count
			if (item.getAmount() > 0) {
				has += item.getAmount();
			}
		}
		return has;
	}
	
	public boolean isNumber(String s) {
		try { 
			Double.parseDouble(s); 
		} catch(NumberFormatException e) { 
			return false; 
		}
		return true;
	}
}
