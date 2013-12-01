package com.snowgears.shop.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.events.PlayerCreateShopEvent;
import com.snowgears.shop.events.PlayerDestroyShopEvent;
import com.snowgears.shop.events.PlayerPreCreateShopEvent;
import com.snowgears.shop.events.PlayerShopExchangeEvent;

public class MiscListener implements Listener{

	public Shop plugin = Shop.plugin;
	ArrayList<Location> invincibleSigns = new ArrayList<Location>();

	public MiscListener(Shop instance)
    {
        plugin = instance;
    }
	
	//give player specified amount of currency on first login
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		if(player.hasPlayedBefore() == false){
			if(plugin.currencyToStart > 0){
				if(plugin.econ == null){
					ItemStack startItems = new ItemStack(plugin.economyMaterial, plugin.currencyToStart);
					player.getInventory().addItem(startItems);
				}
				else{
					plugin.econ.depositPlayer(player.getName(), plugin.currencyToStart);
				}
			}
		}
	}
	
	//prevent shops and their signs from being deleted in an explosion
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onExplosion(EntityExplodeEvent event){
		List<Block> list = event.blockList();

		final LinkedList<ShopObject> shopsInExplosion = new LinkedList<ShopObject>();
		for(int i=0; i<list.size(); i++){
			Block b = list.get(i);
			if(b.getType() == Material.CHEST){
				event.blockList().remove(i);
				ShopObject shop = plugin.shopHandler.getShop(b.getLocation());
				if(shop != null)
					shopsInExplosion.add(shop);
			}
			else if(b.getType() == Material.WALL_SIGN){
				event.blockList().remove(i);
			}
		}
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
			public void run() { 
				for(ShopObject shop : shopsInExplosion){
					shop.setDisplayItem(shop.getDisplayItem().getItemStack()); //refresh shop item
				} 
			}
		}, 100L); 
	}
	
	//prevent empying of bucket when player clicks on shop sign
	//also prevent when emptying on display item itself
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onBucketEmpty(PlayerBucketEmptyEvent event){
		if(event.isCancelled()){
			return;
		}

		Block b = event.getBlockClicked();
		
		if(b.getType() == Material.WALL_SIGN){
			org.bukkit.material.Sign sign = (org.bukkit.material.Sign)event.getBlockClicked().getState().getData();
			ShopObject shop = plugin.shopHandler.getShop(b.getRelative(sign.getAttachedFace()).getLocation());
			if(shop != null)
				event.setCancelled(true);
		}
		Block blockToFill = event.getBlockClicked().getRelative(event.getBlockFace());
		ShopObject shop = plugin.shopHandler.getShop(blockToFill.getRelative(BlockFace.DOWN).getLocation());
		if(shop != null)
			event.setCancelled(true);
	}
	
	//=============FROM HERE DOWN : THESE METHODS CALL CUSTOM EVENTS=========================//
	
	//player places a sign on a chest and is about to create a shop, call PlayerPrepareCreateShopEvent
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onSignEdit(SignChangeEvent event){
		Block b = event.getBlock();
		Player player = event.getPlayer();
		final org.bukkit.material.Sign sign = (org.bukkit.material.Sign)b.getState().getData();
		if(sign.isWallSign() == true)
			return;
		Block relBlock = b.getRelative(sign.getFacing().getOppositeFace());

		double price = 0;
		int amount = 0;
		ShopType type = ShopType.BUYING; //TODO make this barter
		if(relBlock.getType() == Material.CHEST){
			final Sign signBlock = (Sign)b.getState();
			if(event.getLine(0).equalsIgnoreCase("[shop]")){
				if(plugin.usePerms && ! (player.hasPermission("shop.create"))){
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
					return;
				}
				
					if(isNumber(event.getLine(1)) == true){
						amount = Integer.parseInt(event.getLine(1));
						if(amount < 1){
							player.sendMessage(ChatColor.RED+"The amount (line 2) needs to be positive.");
							return;
						}
					}
					else{
						player.sendMessage(ChatColor.RED+"The amount (line 2) needs to be a number.");
						return;
					}
					
					if(isNumber(event.getLine(2)) == true){
						price = Double.parseDouble(event.getLine(2));
						if(price < 1){
							player.sendMessage(ChatColor.RED+"The price (line 3) needs to be positive.");
							return;
						}
					}
					else{
						player.sendMessage(ChatColor.RED+"The price (line 3) needs to be a number.");
						return;
					}

					if(event.getLine(3).isEmpty() || event.getLine(3).toLowerCase().contains("s"))
						type = ShopType.SELLING;
					else if(event.getLine(3).toLowerCase().contains("buy"))
						type = ShopType.BUYING;
					
					boolean isAdmin = false;
					if(event.getLine(3).toLowerCase().contains("admin"))
						if(player.isOp() || (plugin.usePerms && player.hasPermission("shop.operator")))
							isAdmin = true;
					
					signBlock.update(true);
					
					relBlock.getRelative(sign.getFacing()).setTypeId(Material.WALL_SIGN.getId());
					
					final Sign newSign = (Sign)relBlock.getRelative(sign.getFacing()).getState();
					newSign.setLine(0, ChatColor.BOLD+event.getLine(0));
					if(type == ShopType.SELLING)
						newSign.setLine(1, "Selling: "+ChatColor.BOLD+ amount);
					else
						newSign.setLine(1, "Buying: "+ChatColor.BOLD+ amount);
					
					if(plugin.econ == null)
						newSign.setLine(2, ChatColor.RED+""+ (int)price +" "+ plugin.economyDisplayName);
					else
						newSign.setLine(2, ChatColor.RED+""+ price +" "+ plugin.economyDisplayName);
					
					if(isAdmin)
						newSign.setLine(3, "admin");
					else
						newSign.setLine(3, "");
					
					org.bukkit.material.Sign matSign = new org.bukkit.material.Sign(Material.WALL_SIGN);
					matSign.setFacingDirection(sign.getFacing());
					
					newSign.setData(matSign);
					newSign.update();
					
					PlayerPreCreateShopEvent e = new PlayerPreCreateShopEvent(player, newSign.getLocation(), relBlock.getLocation(), price, amount, isAdmin, type);
					plugin.getServer().getPluginManager().callEvent(e);
			}
		}
	}
	
	//player clicks on (pre-shop)sign with an item, call PlayerCreateShopEvent 
	@EventHandler
	public void onPreShopSignClick(PlayerInteractEvent event){
		if(event.isCancelled()){
			return;
		}
		final Player player = event.getPlayer();
		
		if(event.getAction() == Action.LEFT_CLICK_BLOCK){
			final Block clicked = event.getClickedBlock();
			
			if(clicked.getType() == Material.WALL_SIGN){
				if(plugin.shopListener.getValues(clicked.getLocation()) != null){
					if(plugin.usePerms && ! (player.hasPermission("shop.create"))){
						event.setCancelled(true);
						player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
						return;
					}
					ArrayList<Double> values = plugin.shopListener.getValues(clicked.getLocation());
					
					if(player.getItemInHand().getType() == Material.AIR){
						player.sendMessage(ChatColor.RED+"You must be holding an item!");
						return;
					}
					Sign sign = (Sign)clicked.getState();
					
					ShopType type = ShopType.BARTER;
					if(sign.getLine(3).isEmpty() || sign.getLine(3).contains("s"))
						type = ShopType.SELLING;
					else if(sign.getLine(3).contains("buy"))
						type = ShopType.BUYING;
					
					boolean isAdmin = false;
					if(sign.getLine(3).equals("admin")){
						if(player.isOp() || (plugin.usePerms && player.hasPermission("shop.operator"))){
							isAdmin = true;
						}
					}
					String owner = player.getName();
					if(isAdmin == false)
						sign.setLine(3, player.getName());
					else
						owner = "admin";

					org.bukkit.material.Sign s = (org.bukkit.material.Sign)clicked.getState().getData();
					Block chest = clicked.getRelative(s.getAttachedFace());
					final ShopObject shop = new ShopObject(chest.getLocation(), 
							clicked.getLocation(),
							owner,
							player.getItemInHand(),
							values.get(0),
							values.get(1).intValue(), 
							isAdmin,
							type);
					
					PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
				}
			}
		}
	}
	
	//player destroys shop, call PlayerDestroyShopEvent
	@EventHandler (priority = EventPriority.HIGHEST)
	public void shopDestroy(BlockBreakEvent event){
		if(event.isCancelled())
			return;
		
		Block b = event.getBlock();
		
		if(invincibleSigns.contains(b.getLocation())){
			event.setCancelled(true);
			return;
		}
		
//		ShopObject shop = plugin.shopHandler.getShop(b.getLocation());
//		if(shop == null)
//			return;
//			
		Player player = event.getPlayer();
		
		if(b.getType() == Material.WALL_SIGN){
			org.bukkit.material.Sign sign = (org.bukkit.material.Sign)b.getState().getData();
			ShopObject shop = plugin.shopHandler.getShop(b.getRelative(sign.getAttachedFace()).getLocation());
			if(shop == null)
				return;
			//player trying to break their own shop
			if(shop.getOwner().equals(player.getName())){
				if(plugin.usePerms && ! (player.hasPermission("shop.destroy"))){
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED+"You are not authorized to destroy shops.");
					return;
				}
				PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
				plugin.getServer().getPluginManager().callEvent(e);
				if(e.isCancelled()){
					event.setCancelled(true);
				}
				return;
			}
			//player trying to break other players shop
			else{
				if(player.isOp() || (plugin.usePerms && player.hasPermission("shop.operator"))){
					PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
					if(e.isCancelled()){
						event.setCancelled(true);
					}
					return;
				}
				event.setCancelled(true);
				return;
			}
		}
		else if(b.getType() == Material.CHEST){
			Chest chest = (Chest)b.getState();
			InventoryHolder ih = chest.getInventory().getHolder();
			
			if(ih instanceof DoubleChest){
				DoubleChest dchest = (DoubleChest)ih;
				Chest chestLeft = (Chest)dchest.getLeftSide();
				Chest chestRight = (Chest)dchest.getRightSide();
				
				ShopObject shopLeft = plugin.shopHandler.getShop(chestLeft.getLocation());
				ShopObject shopRight = plugin.shopHandler.getShop(chestRight.getLocation());
				
				//player trying to break their own shop
				if(shopLeft != null){
					if(shopLeft.getOwner().equals(player.getName())){
						player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
						event.setCancelled(true);
						return;
					}
				}
				else if(shopRight != null){
					if(shopRight.getOwner().equals(player.getName())){
						player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
						event.setCancelled(true);
						return;
					}
				}
			}
			//instance of single chest
			else{
				ShopObject shop = plugin.shopHandler.getShop(b.getLocation());
				if(shop == null)
					return;
				event.setCancelled(true);
				//player trying to break their own shop
				if(shop.getOwner().equals(player.getName())){
					player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
					return;
				}
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onShopSignClick(PlayerInteractEvent event){
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
			if(event.getClickedBlock().getType() == Material.WALL_SIGN){
				org.bukkit.material.Sign sign = (org.bukkit.material.Sign)event.getClickedBlock().getState().getData();
				ShopObject shop = plugin.shopHandler.getShop(event.getClickedBlock().getRelative(sign.getAttachedFace()).getLocation());
				if(shop == null)
					return;
				Player player = event.getPlayer();
				
				//player right clicked own sign
				if(shop.getOwner().equals(player.getName())){
					if(shop.getType() == ShopType.SELLING && plugin.econ == null){
						int amountOfMoney = plugin.shopListener.getAmount(shop.getInventory(), new ItemStack(plugin.economyMaterial));
						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+plugin.economyDisplayName+".");
					}
					else if(shop.getType() == ShopType.BUYING){
						int amountOfItems = plugin.shopListener.getAmount(shop.getInventory(), shop.getDisplayItem().getItemStack());
						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getItemStack().getType().name().replace("_", " ").toLowerCase()+".");
					}
				}
				//player right clicked other shops' sign
				else{
					//check that both shop and player have enough funds to complete transaction
					
					
					
					PlayerShopExchangeEvent e = new PlayerShopExchangeEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
				}
			}
		}
	}
	
//	//TODO after you make it so players cannot put items into the shop that don't match the shop item
//	//it should be easier to select an item at random and remove it from the inventory and give to player
//	@EventHandler
//	public void onShopSignClick(PlayerInteractEvent event){
//		if(event.isCancelled()){
//			return;
//		}
//		final Player player = event.getPlayer();
//		
//		if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
//			Block clicked = event.getClickedBlock();
//
//			if(clicked.getType() == Material.WALL_SIGN && plugin.econ==null){
//				org.bukkit.material.Sign sign = (org.bukkit.material.Sign)clicked.getState().getData();
//				ShopObject shop = plugin.shopHandler.getShop(clicked.getRelative(sign.getAttachedFace()).getLocation());
//				if(shop == null)
//					return;
//				ItemStack displayStack = shop.getDisplayItem().getItemStack();
//						
//				if(plugin.usePerms && ! (player.hasPermission("shop.use"))){
//					event.setCancelled(true);
//					player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
//					return;
//				}
//						
//				//player clicked on shop that was not their own
//				if(!shop.getOwner().equals(player.getName())){
//					if(shop.getType() == ShopType.SELLING){
//						ItemStack itemPrice = new ItemStack(plugin.economyMaterial);
//					
//						if(!player.getInventory().containsAtLeast(itemPrice, (int)shop.getPrice())){
//							player.sendMessage(ChatColor.RED+"You do not have enough "+ plugin.economyDisplayName+" to buy from this shop.");
//							return;
//						}
//						
//						if(shop.isAdminShop()){
//							itemPrice.setAmount((int)shop.getPrice());
//							ItemStack item = new ItemStack(displayStack.getType(), shop.getAmount(), displayStack.getData().getData());
//							player.getInventory().removeItem(itemPrice);
//							player.getInventory().addItem(item);
//							player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName+".");
//							player.updateInventory();
//							return;
//						}
//						
//						Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//						ItemStack item = new ItemStack(displayStack.getType(), 1, displayStack.getData().getData());
//						if(!chest.getInventory().containsAtLeast(item, shop.getAmount())){
//							player.sendMessage(ChatColor.RED+"This shop is out of stock.");
//							return;
//						}
//						Player owner = Bukkit.getPlayer(shop.getOwner());
//						if(owner != null)
//							owner.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+shop.getPrice()+plugin.economyDisplayName+".");
//							
//						itemPrice.setAmount((int)shop.getPrice());
//						item.setAmount(shop.getAmount());
//						player.getInventory().removeItem(itemPrice);
//						HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//	                    if (!leftOver.isEmpty()) 
//	                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//						chest.getInventory().removeItem(item);
//						chest.getInventory().addItem(itemPrice);
//						player.updateInventory();
//						player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName+".");
//						return;
//					}
//					else{
//						ItemStack itemPrice = new ItemStack(plugin.economyMaterial);
//						
//						ItemStack item = new ItemStack(displayStack.getType(), 1, displayStack.getData().getData());
//						if(!player.getInventory().containsAtLeast(item, shop.getAmount())){
//							player.sendMessage(ChatColor.RED+"You do not have enough "+ shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this shop.");
//							return;
//						}
//						
//						if(shop.isAdminShop()){
//							itemPrice.setAmount((int)shop.getPrice());
//							item.setAmount(shop.getAmount());
//							player.getInventory().removeItem(item);
//							HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
//		                    if (!leftOver.isEmpty()) 
//		                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//							player.updateInventory();
//							player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName+".");
//							return;
//						}
//						
//						Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//						if(!chest.getInventory().containsAtLeast(itemPrice, (int)shop.getPrice())){
//							player.sendMessage(ChatColor.RED+"This shop is out of funds.");
//							return;
//						}
//						if(chest.getInventory().firstEmpty() == -1){
//							player.sendMessage(ChatColor.RED+"This chest is currently too full to sell to.");
//							return;
//						}
//
//						Player owner = Bukkit.getPlayer(shop.getOwner());
//						if(owner != null)
//							owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+shop.getPrice()+plugin.economyDisplayName+".");
//							
//						itemPrice.setAmount((int)shop.getPrice());
//						item.setAmount(shop.getAmount());
//						chest.getInventory().removeItem(itemPrice);
//						chest.getInventory().addItem(item);
//						player.getInventory().removeItem(item);
//						HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
//	                    if (!leftOver.isEmpty()) 
//	                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//						player.updateInventory();
//						player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+shop.getOwner()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName+".");
//						return;
//					}
//				}
//				//the player has clicked on their own shop
//				else{
////					Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
////					
//					if(shop.getType() == ShopType.SELLING){
//						int amountOfMoney = plugin.shopListener.getAmount(shop.getInventory(), new ItemStack(plugin.economyMaterial));
//						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+plugin.economyDisplayName+".");
//					}
//					else{
//						int amountOfItems = plugin.shopListener.getAmount(shop.getInventory(), displayStack);
//						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
//					}	
//				}
//				player.updateInventory();
//			}
//			else if(clicked.getType() == Material.WALL_SIGN && plugin.econ != null){
//				org.bukkit.material.Sign sign = (org.bukkit.material.Sign)clicked.getState().getData();
//				ShopObject shop = plugin.shopHandler.getShop(clicked.getRelative(sign.getAttachedFace()).getLocation());
//				if(shop == null)
//					return;
//				ItemStack displayStack = shop.getDisplayItem().getItemStack();
//						
//				if(plugin.usePerms && ! (player.hasPermission("shop.use"))){
//					event.setCancelled(true);
//					player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
//					return;
//				}
//						
//				//player clicked on shop that was not their own
//				if(!shop.getOwner().equals(player.getName())){
//					if(shop.getType() == ShopType.SELLING){
//						double balance = plugin.econ.getBalance(player.getName());
//						
//						if(balance < shop.getPrice()){
//							player.sendMessage(ChatColor.RED+"You do not have enough "+ plugin.economyDisplayName+" to buy from this shop.");
//							return;
//						}
//						
//						if(shop.isAdminShop()){
//							ItemStack item = new ItemStack(displayStack.getType(), shop.getAmount(), displayStack.getData().getData());
//							HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//		                    if (!leftOver.isEmpty()) 
//		                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//							plugin.econ.withdrawPlayer(player.getName(), shop.getPrice());
//							player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName+".");
//							return;
//						}
//						
//						Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//						ItemStack item = new ItemStack(displayStack.getType(), 1, displayStack.getData().getData());
//						if(!chest.getInventory().containsAtLeast(item, shop.getAmount())){
//							player.sendMessage(ChatColor.RED+"This shop is out of stock.");
//							return;
//						}
//
//						item.setAmount(shop.getAmount());
//						EconomyResponse r = plugin.econ.withdrawPlayer(player.getName(), shop.getPrice());
//			            if(r.transactionSuccess()) 
//			            	player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName +".");
//			            else 
//			                player.sendMessage(String.format("An error occured: %s", r.errorMessage));
//
//			            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//	                    if (!leftOver.isEmpty()) 
//	                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//						chest.getInventory().removeItem(item);
//						
//						EconomyResponse er = plugin.econ.depositPlayer(shop.getOwner(), shop.getPrice());
//			            if(er.transactionSuccess()){ 
//			            	Player p = Bukkit.getPlayer(shop.getOwner());
//			            	if(p != null)
//			            		p.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+shop.getPrice()+plugin.economyDisplayName+".");
//			            }
//			            else 
//			                System.out.println(String.format("An error occured: %s", er.errorMessage));
//
//						player.updateInventory();
//						return;
//					}
//					else{
//						double balance = plugin.econ.getBalance(shop.getOwner());	
//						
//						ItemStack item = new ItemStack(displayStack.getType(), 1, displayStack.getData().getData());
//						if(!player.getInventory().containsAtLeast(item,shop.getAmount())){
//							player.sendMessage(ChatColor.RED+"You do not have enough "+ shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this shop.");
//							return;
//						}
//						
//						if(shop.isAdminShop()){
//							item.setAmount(shop.getAmount());
//							player.getInventory().removeItem(item);
//							plugin.econ.depositPlayer(player.getName(), shop.getPrice());
//							player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName+".");
//							return;
//						}
//						
//						if(balance < shop.getPrice()){
//							player.sendMessage(ChatColor.RED+"This shop's owner is out of funds.");
//							return;
//						}
//							
//						EconomyResponse r = plugin.econ.withdrawPlayer(shop.getOwner(), shop.getPrice());
//			            if(r.transactionSuccess()){ 
//			            	Player owner = Bukkit.getPlayer(shop.getOwner());
//							if(owner != null)
//								owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+shop.getPrice()+plugin.economyDisplayName+".");
//			            }
//			            else 
//			                System.out.println(String.format("An error occured: %s", r.errorMessage));
//			            
//			            Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//			            
//			            if(chest.getInventory().firstEmpty() == -1){
//							player.sendMessage(ChatColor.RED+"This chest is currently too full to sell to.");
//							return;
//						}
//			            
//						item.setAmount(shop.getAmount());
//						chest.getInventory().addItem(item);
//						player.getInventory().removeItem(item);
//						
//						EconomyResponse er = plugin.econ.depositPlayer(player.getName(), shop.getPrice());
//			            if(er.transactionSuccess()){ 
//			            	player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+shop.getOwner()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ plugin.economyDisplayName+".");
//			            }
//			            else 
//			                System.out.println(String.format("An error occured: %s", er.errorMessage));
//			            
//						player.updateInventory();
//						return;
//					}
//				}
//				//the player has clicked on their own shop
//				else{
////					Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
////					
//					if(shop.getType() == ShopType.SELLING && plugin.econ == null){
//						int amountOfMoney = plugin.shopListener.getAmount(shop.getInventory(), new ItemStack(plugin.economyMaterial));
//						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+plugin.economyDisplayName+".");
//					}
//					else if(shop.getType() == ShopType.BUYING){
//						int amountOfItems = plugin.shopListener.getAmount(shop.getInventory(), displayStack);
//						player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
//					}
//				}
//			}
//			player.updateInventory();
//		}
//	}
	
	public boolean isNumber(String s) {
	    try { 
	        Double.parseDouble(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    return true;
	}
}
