package com.snowgears.shop.listeners;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.events.PlayerCloseShopEvent;
import com.snowgears.shop.events.PlayerCreateShopEvent;
import com.snowgears.shop.events.PlayerDestroyShopEvent;
import com.snowgears.shop.events.PlayerOpenShopEvent;
import com.snowgears.shop.events.PlayerPrepareCreateShopEvent;
import com.snowgears.shop.utils.SerializableLocation;

public class MiscListener implements Listener{

	public Shop plugin = Shop.plugin;
	ArrayList<Location> invincibleSigns = new ArrayList<Location>();
	public HashMap<String, ShopObject> playersWhoClickedShops = new HashMap<String, ShopObject>();
	
	public MiscListener(Shop instance)
    {
        plugin = instance;
    }
	
	//give player specified amount of currency on first login
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		if(player.hasPlayedBefore() == false){
			if(Shop.itemsToStart > 0){
				if(Shop.econ == null){
					ItemStack startItems = new ItemStack(Shop.economyItemId, Shop.itemsToStart);
					player.getInventory().addItem(startItems);
				}
				else{
					Shop.econ.depositPlayer(player.getName(), Shop.itemsToStart);
				}
			}
		}
	}
	
	//prevent shops and their signs from being deleted in an explosion
	@EventHandler
	public void onExplosion(EntityExplodeEvent event){
		List<Block> list = event.blockList();

		for(int i=0; i<list.size(); i++){
			if(list.get(i).getType() == Material.CHEST || list.get(i).getType() == Material.WALL_SIGN)
				event.blockList().remove(i);
		}
	}
	
	//prevent empying of bucket when player clicks on shop sign
	@EventHandler
	public void onBucketEmpty(PlayerBucketEmptyEvent event){
		if(event.isCancelled()){
			return;
		}

		Block b = event.getBlockClicked();
		
		if(b.getType() == Material.WALL_SIGN){
			for(ShopObject shop : plugin.alisten.allShops){
				if(shop.getSignLocation().equals(b.getLocation())){
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	//=============FROM HERE DOWN : THESE METHODS CALL CUSTOM EVENTS=========================//
	
	//player clicks on shop, add to list
	@EventHandler
	public void onChestClick(PlayerInteractEvent event){
		Player player = event.getPlayer();
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
			Location clicked = event.getClickedBlock().getLocation();
			for(ShopObject shop : plugin.alisten.allShops){
				if(shop.getLocation().equals(clicked)){
					if(!playersWhoClickedShops.containsKey(player.getName()))
						playersWhoClickedShops.put(player.getName(), shop);
					return;
				}
			}
		}
	}
	
	//TODO add playersWhoClickedShops on PlayerOpenShopEvent. Then remove on PlayerCloseShopEvent
	//if player is in list of clicked shops, call PlayerCloseShopEvent
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event){
		if(!(event.getPlayer() instanceof Player)){
			return;
		}
		Player player = (Player)event.getPlayer();
		if(playersWhoClickedShops.containsKey(player.getName())){
			ShopObject shop = playersWhoClickedShops.get(player.getName());
			PlayerCloseShopEvent e = new PlayerCloseShopEvent(player, shop, event.getInventory());
			plugin.getServer().getPluginManager().callEvent(e);
			playersWhoClickedShops.remove(player.getName());
		}
	}
	
	//player places a sign on a chest and is about to create a shop, call PlayerPrepareCreateShopEvent
	@EventHandler
	public void onSignEdit(SignChangeEvent event){
		if(Shop.econ != null)
			return;
		Block b = event.getBlock();
		Player player = event.getPlayer();
		final org.bukkit.material.Sign sign = (org.bukkit.material.Sign)b.getState().getData();
		if(sign.isWallSign() == true)
			return;
		Block relBlock = b.getRelative(sign.getFacing().getOppositeFace());

		int price = 0;
		int amount = 0;
		boolean isSelling = true;
		if(relBlock.getType() == Material.CHEST){
			final Sign signBlock = (Sign)b.getState();
			if(event.getLine(0).equals("[shop]")){
					
				if(Shop.usePerms && ! (player.hasPermission("shop.create"))){
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
					return;
				}
				
					if(isInteger(event.getLine(1)) == true){
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
					
					if(isInteger(event.getLine(2)) == true){
						price = Integer.parseInt(event.getLine(2));
						if(price < 1){
							player.sendMessage(ChatColor.RED+"The price (line 3) needs to be positive.");
							return;
						}
					}
					else{
						player.sendMessage(ChatColor.RED+"The price (line 3) needs to be a number.");
						return;
					}

					if(event.getLine(3).toLowerCase().contains("b"))
						isSelling = false;
					else
						isSelling = true;
					
					boolean isAdmin = false;
					if(event.getLine(3).toLowerCase().contains("admin"))
						if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator")))
							isAdmin = true;
					
					signBlock.update(true);
					
					relBlock.getRelative(sign.getFacing()).setTypeId(Material.WALL_SIGN.getId());
					
					final Sign newSign = (Sign)relBlock.getRelative(sign.getFacing()).getState();
					newSign.setLine(0, ChatColor.BOLD+event.getLine(0));
					if(isSelling)
						newSign.setLine(1, "Selling: "+ChatColor.BOLD+ amount);
					else
						newSign.setLine(1, "Buying: "+ChatColor.BOLD+ amount);
					newSign.setLine(2, ChatColor.RED+""+ price +" "+ Shop.economyItemName);
					
					if(isAdmin)
						newSign.setLine(3, "admin");
					else
						newSign.setLine(3, "");
					
					org.bukkit.material.Sign matSign = new org.bukkit.material.Sign(Material.WALL_SIGN);
					matSign.setFacingDirection(sign.getFacing());
					
					newSign.setData(matSign);
					newSign.update();
					
					PlayerPrepareCreateShopEvent e = new PlayerPrepareCreateShopEvent(player, newSign.getLocation(), relBlock.getLocation(), price, amount, isSelling, isAdmin);
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
				if(plugin.alisten.getValues(clicked.getLocation()) != null){
					if(Shop.usePerms && ! (player.hasPermission("shop.create"))){
						event.setCancelled(true);
						player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
						return;
					}
					ArrayList<Double> values = plugin.alisten.getValues(clicked.getLocation());
					
					if(player.getItemInHand().getType() == Material.AIR){
						player.sendMessage(ChatColor.RED+"You must be holding an item!");
						return;
					}
					Sign sign = (Sign)clicked.getState();
					
					boolean isSelling = true;
					if(sign.getLine(1).isEmpty() || sign.getLine(1).substring(0, 1).equalsIgnoreCase("b"))
						isSelling = false;
					
					boolean isAdmin = false;
					if(sign.getLine(3).equals("admin")){
						if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator"))){
							isAdmin = true;
							player.sendMessage(ChatColor.GRAY+"You have made an admin shop.");
						}
					}
					String owner = player.getName();
					if(isAdmin == false)
						sign.setLine(3, player.getName());
					else
						owner = "admin";

					org.bukkit.material.Sign s = (org.bukkit.material.Sign)clicked.getState().getData();
					Block chest = clicked.getRelative(s.getAttachedFace());
					final ShopObject shop = new ShopObject(new SerializableLocation(chest.getLocation()), 
							new SerializableLocation(clicked.getLocation()),
							new SerializableLocation(chest.getLocation().clone().add(0.5,1.2,0.5)),
							owner,
							player.getItemInHand(),
							values.get(0),
							values.get(1).intValue(), 
							isSelling,
							isAdmin);
					
					sign.setLine(2, ChatColor.GREEN+""+ shop.getPrice() +" "+ Shop.economyItemName);
					sign.update(true);
					
					PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
					plugin.getServer().getPluginManager().callEvent(e);
				}
			}
		}
	}
	
	//player clicks on shop chest, call PlayerShopOpenEvent
	@EventHandler
	public void onShopChestClick(PlayerInteractEvent event){
		if(event.isCancelled()){
			return;
		}
		final Player player = event.getPlayer();
		
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
			Block clicked = event.getClickedBlock();
			
			if(clicked.getType() == Material.CHEST){
				Chest chest = (Chest)clicked.getState();
				InventoryHolder ih = chest.getInventory().getHolder();
				
				if(ih instanceof DoubleChest){
					DoubleChest dchest = (DoubleChest)ih;
					Chest chestLeft = (Chest)dchest.getLeftSide();
					Chest chestRight = (Chest)dchest.getRightSide();
					
					for(ShopObject shop : plugin.alisten.allShops){
						if(shop.getLocation().equals(chestLeft.getLocation()) || shop.getLocation().equals(chestRight.getLocation())){
							//player clicked on shop that was not their own
							if(!shop.getOwner().equals(player.getName())){
								if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator"))){
									PlayerOpenShopEvent e = new PlayerOpenShopEvent(player, shop, dchest.getInventory());
									plugin.getServer().getPluginManager().callEvent(e);
									return;
								}
								event.setCancelled(true);
								player.sendMessage(ChatColor.RED+"You must right click the sign to use this shop.");
								return;
							}
							else{
								PlayerOpenShopEvent e = new PlayerOpenShopEvent(player, shop, dchest.getInventory());
								plugin.getServer().getPluginManager().callEvent(e);
							}
						}
					}
				}
				else{
					for(ShopObject shop : plugin.alisten.allShops){
						if(shop.getLocation().equals(chest.getLocation())){
							//player clicked on shop that was not their own
							if(!shop.getOwner().equals(player.getName())){
								if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator"))){
									PlayerOpenShopEvent e = new PlayerOpenShopEvent(player, shop, chest.getInventory());
									plugin.getServer().getPluginManager().callEvent(e);
									return;
								}
								event.setCancelled(true);
								player.sendMessage(ChatColor.RED+"You must right click the sign to use this shop.");
								return;
							}
							else{
								PlayerOpenShopEvent e = new PlayerOpenShopEvent(player, shop, chest.getInventory());
								plugin.getServer().getPluginManager().callEvent(e);
							}
						}
					}
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
			
			
		Location loc = b.getLocation();
		Player player = event.getPlayer();
		
		if(b.getType() == Material.WALL_SIGN){
			for(ShopObject shop : plugin.alisten.allShops){
				if(shop.getSignLocation().equals(loc)){
					//player trying to break their own shop
					if(shop.getOwner().equals(player.getName())){
						if(Shop.usePerms && ! (player.hasPermission("shop.destroy"))){
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
						if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator"))){
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
			}
		}
		else if(b.getType() == Material.CHEST){
			Chest chest = (Chest)b.getState();
			InventoryHolder ih = chest.getInventory().getHolder();
			
			if(ih instanceof DoubleChest){
				DoubleChest dchest = (DoubleChest)ih;
				Chest chestLeft = (Chest)dchest.getLeftSide();
				Chest chestRight = (Chest)dchest.getRightSide();
				
				for(ShopObject shop : plugin.alisten.allShops){
					if(shop.getLocation().equals(chestLeft.getLocation()) || shop.getLocation().equals(chestRight.getLocation())){
						event.setCancelled(true);
						//player trying to break their own shop
						if(shop.getOwner().equals(player.getName())){
							player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
							return;
						}
					}
				}
			}
			else{
				for(ShopObject shop : plugin.alisten.allShops){
					if(shop.getLocation().equals(chest.getLocation())){
						event.setCancelled(true);
						//player trying to break their own shop
						if(shop.getOwner().equals(player.getName())){
							player.sendMessage(ChatColor.RED+"You must remove the sign from this shop to break it.");
							return;
						}
					}
				}
			}
		}
	}
	
	//TODO after you make it so players cannot put items into the shop that don't match the shop item
	//it should be easier to select an item at random and remove it from the inventory and give to player
	@EventHandler
	public void onShopSignClick(PlayerInteractEvent event){
		if(event.isCancelled()){
			return;
		}
		final Player player = event.getPlayer();
		
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
			Block clicked = event.getClickedBlock();
			
			if(clicked.getType() == Material.WALL_SIGN && Shop.econ==null){
				for(ShopObject shop : plugin.alisten.allShops){
					//left clicked on a different shop
					if(shop.getSignLocation().equals(clicked.getLocation())){
						
						if(Shop.usePerms && ! (player.hasPermission("shop.use"))){
							event.setCancelled(true);
							player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
							return;
						}
						
						//player clicked on shop that was not their own
						if(!shop.getOwner().equals(player.getName())){
							if(shop.isSellingShop()){
								ItemStack itemPrice = new ItemStack(Shop.economyItemId);
							
								if(!player.getInventory().containsAtLeast(itemPrice, (int)shop.getPrice())){
									player.sendMessage(ChatColor.RED+"You do not have enough "+ Shop.economyItemName+" to buy from this shop.");
									return;
								}
								
								if(shop.isAdminShop()){
									itemPrice.setAmount((int)shop.getPrice());
									ItemStack item = new ItemStack(shop.getDisplayItem().getType(), shop.getAmount(), shop.getDisplayItem().getData());
									player.getInventory().removeItem(itemPrice);
									player.getInventory().addItem(item);
									player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
									player.updateInventory();
									return;
								}
								
								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
								if(!chest.getInventory().containsAtLeast(item, shop.getAmount())){
									player.sendMessage(ChatColor.RED+"This shop is out of stock.");
									return;
								}
								Player owner = Bukkit.getPlayer(shop.getOwner());
								if(owner != null)
									owner.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
									
								itemPrice.setAmount((int)shop.getPrice());
								item.setAmount(shop.getAmount());
								player.getInventory().removeItem(itemPrice);
								HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
			                    if (!leftOver.isEmpty()) 
			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
								chest.getInventory().removeItem(item);
								chest.getInventory().addItem(itemPrice);
								player.updateInventory();
								player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
								return;
							}
							else{
								ItemStack itemPrice = new ItemStack(Shop.economyItemId);
								
								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
								if(!player.getInventory().containsAtLeast(item, shop.getAmount())){
									player.sendMessage(ChatColor.RED+"You do not have enough "+ shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this shop.");
									return;
								}
								
								if(shop.isAdminShop()){
									itemPrice.setAmount((int)shop.getPrice());
									item.setAmount(shop.getAmount());
									player.getInventory().removeItem(item);
									HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
				                    if (!leftOver.isEmpty()) 
				                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
									player.updateInventory();
									player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
									return;
								}
								
								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
								if(!chest.getInventory().containsAtLeast(itemPrice, (int)shop.getPrice())){
									player.sendMessage(ChatColor.RED+"This shop is out of funds.");
									return;
								}
								if(chest.getInventory().firstEmpty() == -1){
									player.sendMessage(ChatColor.RED+"This chest is currently too full to sell to.");
									return;
								}
		
								Player owner = Bukkit.getPlayer(shop.getOwner());
								if(owner != null)
									owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
									
								itemPrice.setAmount((int)shop.getPrice());
								item.setAmount(shop.getAmount());
								chest.getInventory().removeItem(itemPrice);
								chest.getInventory().addItem(item);
								player.getInventory().removeItem(item);
								HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
			                    if (!leftOver.isEmpty()) 
			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
								player.updateInventory();
								player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+shop.getOwner()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
								return;
							}
						}
						//the player has clicked on their own shop
						else{
							Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
							
							if(shop.isSellingShop()){
								int amountOfMoney = plugin.alisten.getAmount(chest.getInventory(), Shop.economyItemId);
								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+Shop.economyItemName+".");
							}
							else{
								int amountOfItems = plugin.alisten.getAmount(chest.getInventory(), shop.getDisplayItem().getType().getId(), shop.getDisplayItem().getData());
								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
							}
						}
					}
					
				}
				player.updateInventory();
			}
			else if(clicked.getType() == Material.WALL_SIGN && Shop.econ != null){
				for(ShopObject shop : plugin.alisten.allShops){
					//left clicked on a different shop
					if(shop.getSignLocation().equals(clicked.getLocation())){
						
						if(Shop.usePerms && ! (player.hasPermission("shop.use"))){
							event.setCancelled(true);
							player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
							return;
						}
						
						//player clicked on shop that was not their own
						if(!shop.getOwner().equals(player.getName())){
							if(shop.isSellingShop()){
								double balance = Shop.econ.getBalance(player.getName());
								
								if(balance < shop.getPrice()){
									player.sendMessage(ChatColor.RED+"You do not have enough "+ Shop.economyItemName+" to buy from this shop.");
									return;
								}
								
								if(shop.isAdminShop()){
									ItemStack item = new ItemStack(shop.getDisplayItem().getType(), shop.getAmount(), shop.getDisplayItem().getData());
									HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
				                    if (!leftOver.isEmpty()) 
				                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
									Shop.econ.withdrawPlayer(player.getName(), shop.getPrice());
									player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
									return;
								}
								
								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
								if(!chest.getInventory().containsAtLeast(item, shop.getAmount())){
									player.sendMessage(ChatColor.RED+"This shop is out of stock.");
									return;
								}
	
								item.setAmount(shop.getAmount());
								EconomyResponse r = Shop.econ.withdrawPlayer(player.getName(), shop.getPrice());
					            if(r.transactionSuccess()) 
					            	player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName +".");
					            else 
					                player.sendMessage(String.format("An error occured: %s", r.errorMessage));
	
					            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
			                    if (!leftOver.isEmpty()) 
			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
								chest.getInventory().removeItem(item);
								
								EconomyResponse er = Shop.econ.depositPlayer(shop.getOwner(), shop.getPrice());
					            if(er.transactionSuccess()){ 
					            	Player p = Bukkit.getPlayer(shop.getOwner());
					            	if(p != null)
					            		p.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
					            }
					            else 
					                System.out.println(String.format("An error occured: %s", er.errorMessage));
	
								player.updateInventory();
								return;
							}
							else{
								double balance = Shop.econ.getBalance(shop.getOwner());	
								
								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
								if(!player.getInventory().containsAtLeast(item,shop.getAmount())){
									player.sendMessage(ChatColor.RED+"You do not have enough "+ shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this shop.");
									return;
								}
								
								if(shop.isAdminShop()){
									item.setAmount(shop.getAmount());
									player.getInventory().removeItem(item);
									Shop.econ.depositPlayer(player.getName(), shop.getPrice());
									player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
									return;
								}
								
								if(balance < shop.getPrice()){
									player.sendMessage(ChatColor.RED+"This shop's owner is out of funds.");
									return;
								}
									
								EconomyResponse r = Shop.econ.withdrawPlayer(shop.getOwner(), shop.getPrice());
					            if(r.transactionSuccess()){ 
					            	Player owner = Bukkit.getPlayer(shop.getOwner());
									if(owner != null)
										owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
					            }
					            else 
					                System.out.println(String.format("An error occured: %s", r.errorMessage));
					            
					            Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
					            
					            if(chest.getInventory().firstEmpty() == -1){
									player.sendMessage(ChatColor.RED+"This chest is currently too full to sell to.");
									return;
								}
					            
								item.setAmount(shop.getAmount());
								chest.getInventory().addItem(item);
								player.getInventory().removeItem(item);
								
								EconomyResponse er = Shop.econ.depositPlayer(player.getName(), shop.getPrice());
					            if(er.transactionSuccess()){ 
					            	player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+shop.getOwner()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
					            }
					            else 
					                System.out.println(String.format("An error occured: %s", er.errorMessage));
					            
								player.updateInventory();
								return;
							}
						}
						//the player has clicked on their own shop
						else{
							Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
							
							if(shop.isSellingShop() && Shop.econ == null){
								int amountOfMoney = plugin.alisten.getAmount(chest.getInventory(), Shop.economyItemId);
								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+Shop.economyItemName+".");
							}
							else if(shop.isSellingShop() == false){
								int amountOfItems = plugin.alisten.getAmount(chest.getInventory(), shop.getDisplayItem().getType().getId(), shop.getDisplayItem().getData());
								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
							}
						}
					}
					
				}
			}
			player.updateInventory();
		}
	}
	
	public boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    return true;
	}
}
