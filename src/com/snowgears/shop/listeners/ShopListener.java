package com.snowgears.shop.listeners;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map.Entry;


import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.block.Sign;

import com.snowgears.shop.DisplayItem;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.events.PlayerCloseShopEvent;
import com.snowgears.shop.events.PlayerCreateShopEvent;
import com.snowgears.shop.events.PlayerDestroyShopEvent;
import com.snowgears.shop.events.PlayerOpenShopEvent;
import com.snowgears.shop.events.PlayerPrepareCreateShopEvent;


public class ShopListener implements Listener{
	
	public Shop plugin = Shop.plugin;
	public ArrayList<ShopObject> allShops = new ArrayList<ShopObject>();
	public HashMap<Location, ArrayList<Double>> signsAwaitingItems = new HashMap<Location, ArrayList<Double>>(); //location of sign, arraylist of values for shop (price, amount)
	public HashMap<Inventory, ShopObject> openShopInventories = new HashMap<Inventory, ShopObject>();
	
	public ShopListener(Shop instance)
    {
        plugin = instance;
    }
	
	public ArrayList<ShopObject> getallShops()
	{
	    if(Shop.shopMap.containsKey("load")){
	      return Shop.shopMap.get("load");
	    }
	    else
	        return new ArrayList<ShopObject>(); 
	}
	
	//TODO intead of having important things grouped into compex methods, make complex methods call 
	//custom events. (Make all custom events work)
	
	//TODO if item putting into shop is different from shop item in any way, cancel action
	//TODO also make sure to cancel InventoryDragEvent with the same thing
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent event){
		if(!(event.getWhoClicked() instanceof Player)){
			return;
		}
		Player player = (Player)event.getWhoClicked();
		
		if(!openShopInventories.containsKey(event.getInventory()))
			return;
		System.out.println("openShopInventories contains this shop inventory.");
		ShopObject shop = openShopInventories.get(event.getInventory());

		if(event.getCursor() != null){
			if(event.getRawSlot() > -1 || event.getRawSlot() < 27){
				ItemStack cursor = event.getCursor();
				System.out.println("canPutItemInShop called.");
				if(!canPutItemInShop(shop, cursor, player)){
					event.setCancelled(true);
				}
			}
		}
	}
	
	//TODO when players open inventory, if inventory is a chest shop inventory, store it with shop item
	//TODO when players close inventory, if there is a stored value, clear it
//	@EventHandler
//	public void onInventoryClose(InventoryCloseEvent event){
//		event.get
//		clickedShopInventories.put(((Chest)clicked.getState()).getInventory(), item);
//	}

	//when player clicks display item, show all stats of the item being sold to the player
	@EventHandler 
	public void onPlayerClickItem(PlayerInteractEvent event){ 
		Player player = event.getPlayer();
//		if(event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK){
//			Block clicked = event.getClickedBlock();
//			if(clicked.getType() == Material.CHEST){
//				for(ShopObject shop : allShops){
//					if(shop.getLocation().equals(clicked.getLocation())){
//						ItemStack item = new ItemStack(shop.getDisplayItem().getType(), shop.getAmount(), shop.getDisplayItem().getData());
//						item.setDurability(shop.getDisplayItem().getDurability());
//						ItemMeta im = item.getItemMeta();
//						im.setDisplayName(shop.getDisplayItem().getDisplayName());
//						im.setLore(shop.getDisplayItem().getLore());
//						item.setItemMeta(im);
//						clickedShopInventories.put(((Chest)clicked.getState()).getInventory(), item);
//						return;
//					}
//				}
//				return;
//			}
//			else if(clicked.getType() == Material.WALL_SIGN){
//				return;
//			}
//		}
		if(player.getLocation().getPitch() < 0 || player.getLocation().getPitch() > 35){
			return;
		}
		BlockFace bf = yawToFace(player.getLocation().getYaw());
		Block block = player.getLocation().getBlock().getRelative(bf);
		Location itemLoc = null;
		if(block.getType() == Material.CHEST){
			itemLoc = block.getRelative(BlockFace.UP).getLocation();
		}
		else if(block.getType() == Material.WALL_SIGN){
			itemLoc = block.getRelative(bf).getRelative(BlockFace.UP).getLocation();
		}
		else{
			return;
		}
		
		for(ShopObject shop : allShops){
			DisplayItem di = shop.getDisplayItem();
			if(di.getLocation().getBlock().getLocation().equals(itemLoc)){
				player.sendMessage("");
				player.sendMessage(ChatColor.WHITE+di.getDisplayName());
				ArrayList<String> enchantments = di.getListOfEnchantments();
				
				int durability = di.getDurabilityPercent();
				if(durability != 0){
					if(durability == 100)
						player.sendMessage(ChatColor.GRAY+"Durability: "+di.getDurabilityPercent()+"%");
					else
						player.sendMessage(ChatColor.GRAY+"Durability: "+di.getDurabilityPercent()+"% or higher");
				}
				
				if(!enchantments.isEmpty()){
					player.sendMessage(ChatColor.YELLOW+"Enchantments:");
					for(String s : enchantments){
						player.sendMessage(ChatColor.YELLOW+ s);
					}
					player.sendMessage("");
				}
				if(di.getType() == Material.POTION){
					PotionType potion = PotionType.getByDamageValue(di.getDurability());
					player.sendMessage(ChatColor.AQUA+"Potion Type: "+ChatColor.WHITE+potion.name());
				}
				player.sendMessage("");
				if(shop.isSellingShop()){
					player.sendMessage(ChatColor.GREEN+"You can buy "+ChatColor.WHITE+shop.getAmount()+ChatColor.GREEN+" "+di.getType().name().replace("_", " ").toLowerCase()+" from this shop for "+ChatColor.WHITE+shop.getPrice()+ChatColor.GREEN+" "+Shop.economyItemName+".");
					double pricePer = shop.getPrice()/shop.getAmount();
					pricePer = Math.round(pricePer * 100);
					pricePer = pricePer/100;
					player.sendMessage(ChatColor.GRAY+"That is "+pricePer+" "+Shop.economyItemName+" per "+di.getType().name().replace("_", " ").toLowerCase()+".");
				}
				else{
					player.sendMessage(ChatColor.GREEN+"You can sell "+ChatColor.WHITE+shop.getAmount()+ChatColor.GREEN+" "+di.getType().name().replace("_", " ").toLowerCase()+" to this shop for "+ChatColor.WHITE+shop.getPrice()+ChatColor.GREEN+" "+Shop.economyItemName+".");
					double pricePer = shop.getPrice()/shop.getAmount();
					pricePer = Math.round(pricePer * 100);
					pricePer = pricePer/100;
					player.sendMessage(ChatColor.GRAY+"That is "+pricePer+" "+Shop.economyItemName+" per "+di.getType().name().replace("_", " ").toLowerCase()+".");
				}
				player.sendMessage("");
				return;
			}
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPrepareShop(PlayerPrepareCreateShopEvent event){
		System.out.println("PlayerPrepareCreateShopEvent called.");
		if(event.isCancelled()){
			Sign sign = (Sign)event.getSignLocation().getBlock().getState();
			sign.setLine(0, "");
			sign.setLine(1, "");
			sign.setLine(2, "");
			sign.setLine(3, "");
			sign.update(true);
			return;
		}
		Player player = event.getPlayer();

		ArrayList<Double> values = new ArrayList<Double>();
		values.add((double)event.getShopPrice());
		values.add((double)event.getShopAmount());
		setValues(event.getSignLocation(), values);
		
		if(event.isSellingShop())
			player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with the item you want to sell to other players!");
		else
			player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with the item you want to buy from other players!");
		plugin.miscListener.invincibleSigns.add(event.getSignLocation());
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onCreateShop(final PlayerCreateShopEvent event){
		System.out.println("PlayerCreateShopEvent called.");
		plugin.alisten.setValues(event.getShop().getSignLocation(), null);
		
		if(plugin.miscListener.invincibleSigns.contains(event.getShop().getSignLocation())){
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
				public void run() { 
					plugin.miscListener.invincibleSigns.remove(event.getShop().getSignLocation());
					} 
			}, 40); //2 seconds 
		}
		
		if(event.isCancelled()){
			Sign sign = (Sign)event.getShop().getSignLocation().getBlock().getState();
			sign.setLine(0, "");
			sign.setLine(1, "");
			sign.setLine(2, "");
			sign.setLine(3, "");
			sign.update(true);
			event.getShop().getDisplayItem().remove();
			return;
		}
		Player player = event.getPlayer();
		
		plugin.alisten.allShops.add(event.getShop());
		
		Shop.shopMap.put("load", plugin.alisten.allShops);
		Shop.saveHashMapTo(Shop.shopMap, Shop.shopFile);
		
		if(event.getShop().isSellingShop())
			player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that sells "+ChatColor.GOLD+ event.getShop().getDisplayItem().getType().name().replace("_", " ").toLowerCase()+ChatColor.YELLOW+".");
		else
			player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that buys "+ChatColor.GOLD+event.getShop().getDisplayItem().getType().name().replace("_", " ").toLowerCase()+ChatColor.YELLOW+".");		
	}
	
	//test
	@EventHandler
	public void onShopDestroyTest(PlayerDestroyShopEvent event){
		Player player = event.getPlayer();
		if(player.getItemInHand().getType() == Material.STICK)
			event.setCancelled(true);
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onShopDestroy(PlayerDestroyShopEvent event){
		System.out.println("PlayerDestroyShopEvent called.");
		if(event.isCancelled()){
			return;
		}
		Player player = event.getPlayer();
		
		event.getShop().getDisplayItem().remove();
		allShops.remove(event.getShop());
		
		if(event.getShop().getOwner().equals(player.getName())){
			player.sendMessage(ChatColor.GRAY+"You have removed this shop.");
		}
		else{
			player.sendMessage(ChatColor.GRAY+"You have removed a shop owned by "+event.getShop().getOwner());
		}
		
		Shop.shopMap.put("load", allShops);
		Shop.saveHashMapTo(Shop.shopMap, Shop.shopFile);
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onShopOpen(PlayerOpenShopEvent event){
		System.out.println("PlayerOpenShopEvent called.");
		if(!openShopInventories.containsKey(event.getInventory()))
			openShopInventories.put(event.getInventory(), event.getShop());
		Player player = event.getPlayer();
		if(!player.getName().equals(event.getShop().getOwner())){
			player.sendMessage(ChatColor.GRAY+"You are opening a shop owned by "+event.getShop().getOwner());
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onShopClose(PlayerCloseShopEvent event){
		System.out.println("PlayerCloseShopEvent called.");
		if(openShopInventories.containsKey(event.getInventory()))
			openShopInventories.remove(event.getInventory());
	}
	
	//TODO find out whats different about this method from non-econ one and merge the two
//	@EventHandler
//	public void onSignEditEcon(SignChangeEvent event){
//		if(Shop.econ == null)
//			return;
//		Block b = event.getBlock();
//		Player player = event.getPlayer();
//		final org.bukkit.material.Sign sign = (org.bukkit.material.Sign)b.getState().getData();
//		if(sign.isWallSign() == true)
//			return;
//		Block relBlock = b.getRelative(sign.getFacing().getOppositeFace());
//
//		double price = 0;
//		int amount = 0;
//		boolean isSelling = true;
//		if(relBlock.getType() == Material.CHEST){
//			final Sign signBlock = (Sign)b.getState();
//			if(event.getLine(0).equals("[shop]")){
//					
//				if(Shop.usePerms && ! (player.hasPermission("shop.create"))){
//					event.setCancelled(true);
//					player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
//					return;
//				}
//				
//					if(isInteger(event.getLine(1)) == true){
//						amount = Integer.parseInt(event.getLine(1));
//						if(amount < 1){
//							player.sendMessage(ChatColor.RED+"The amount (line 2) needs to be positive.");
//							return;
//						}
//					}
//					else{
//						player.sendMessage(ChatColor.RED+"The amount (line 2) needs to be a number.");
//						return;
//					}
//					
//					if(isDouble(event.getLine(2)) == true){
//						price = Double.parseDouble(event.getLine(2));
//						if(price <= 0){
//							player.sendMessage(ChatColor.RED+"The price (line 3) needs to be positive.");
//							return;
//						}
//					}
//					else{
//						player.sendMessage(ChatColor.RED+"The price (line 3) needs to be a number.");
//						return;
//					}
//
//					if(event.getLine(3).toLowerCase().contains("b"))
//						isSelling = false;
//					else
//						isSelling = true;
//					
//					boolean isAdmin = false;
//					if(event.getLine(3).toLowerCase().contains("admin"))
//						if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator")))
//							isAdmin = true;
//
//					signBlock.update(true);
//					
//					relBlock.getRelative(sign.getFacing()).setTypeId(Material.WALL_SIGN.getId());
//					
//					final Sign newSign = (Sign)relBlock.getRelative(sign.getFacing()).getState();
//					newSign.setLine(0, ChatColor.BOLD+event.getLine(0));
//					if(isSelling)
//						newSign.setLine(1, "Selling: "+ChatColor.BOLD+ amount);
//					else
//						newSign.setLine(1, "Buying: "+ChatColor.BOLD+ amount);
//					newSign.setLine(2, ChatColor.RED+""+ price +" "+ Shop.economyItemName);
//					
//					if(isAdmin)
//						newSign.setLine(3, "admin");
//					else
//						newSign.setLine(3, "");
//	
//					org.bukkit.material.Sign matSign = new org.bukkit.material.Sign(Material.WALL_SIGN);
//					matSign.setFacingDirection(sign.getFacing());
//					
//					newSign.setData(matSign);
//					newSign.update();
//					
//					ArrayList<Double> values = new ArrayList<Double>();
//					values.add(price);
//					values.add((double)amount);
//					setValues(newSign.getLocation(), values);
//					
//					if(isSelling)
//						player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with whatever item you want to sell!");
//					else
//						player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with whatever item you want to buy!");
//					invincibleSigns.add(newSign.getLocation());
//			}
//		}
//	}
	
//	@EventHandler
//	public void onBlockClick(PlayerInteractEvent event){
//		if(event.isCancelled()){
//			return;
//		}
//		final Player player = event.getPlayer();
//		
//		if(event.getAction() == Action.LEFT_CLICK_BLOCK){
//			final Block clicked = event.getClickedBlock();
//			
//			if(clicked.getType() == Material.WALL_SIGN){
//				if(getValues(clicked.getLocation()) != null){
//					if(Shop.usePerms && ! (player.hasPermission("shop.create"))){
//						event.setCancelled(true);
//						player.sendMessage(ChatColor.RED+"You are not authorized to create shops.");
//						return;
//					}
//					ArrayList<Double> values = getValues(clicked.getLocation());
//					
//					if(player.getItemInHand().getType() == Material.AIR){
//						player.sendMessage(ChatColor.RED+"You must be holding an item!");
//						return;
//					}
//					Sign sign = (Sign)clicked.getState();
//					
//					boolean isSelling = true;
//					if(sign.getLine(1).isEmpty() || sign.getLine(1).substring(0, 1).equalsIgnoreCase("b"))
//						isSelling = false;
//					
//					boolean isAdmin = false;
//					if(sign.getLine(3).equals("admin")){
//						if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator"))){
//							isAdmin = true;
//							player.sendMessage(ChatColor.GRAY+"You have made an admin shop.");
//						}
//					}
//					String owner = player.getName();
//					if(isAdmin == false)
//						sign.setLine(3, player.getName());
//					else
//						owner = "admin";
//
//					org.bukkit.material.Sign s = (org.bukkit.material.Sign)clicked.getState().getData();
//					Block chest = clicked.getRelative(s.getAttachedFace());
//					final ShopObject shop = new ShopObject(new SerializableLocation(chest.getLocation()), 
//							new SerializableLocation(clicked.getLocation()),
//							new SerializableLocation(chest.getLocation().clone().add(0.5,1.2,0.5)),
//							owner,
//							player.getItemInHand(),
//							values.get(0),
//							values.get(1).intValue(), 
//							isSelling,
//							isAdmin);
//					allShops.add(shop);
//					
//					sign.setLine(2, ChatColor.GREEN+""+ shop.getPrice() +" "+ Shop.economyItemName);
//					sign.update(true);
//					
//					Shop.shopMap.put("load", allShops);
//					Shop.saveHashMapTo(Shop.shopMap, Shop.shopFile);
//					
////					shop.getDisplayItem().respawn();
//					
//					if(isSelling)
//						player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that sells "+ChatColor.GOLD+ shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+ChatColor.YELLOW+".");
//					else
//						player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that buys "+ChatColor.GOLD+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+ChatColor.YELLOW+".");
//					
//					setValues(clicked.getLocation(), null);
//					
//					if(invincibleSigns.contains(clicked.getLocation())){
//						plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
//							public void run() { 
//								invincibleSigns.remove(clicked.getLocation());
//								//i.teleport(shop.showcaseLocation.deserialize());
//								} 
//						}, 40); //2 seconds 
//					}
//
//				}
//			}
//		}
	//TODO put these in miscListener separately and make them call corresponding custom events
//		else if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
//			Block clicked = event.getClickedBlock();
//			
//			if(clicked.getType() == Material.CHEST){
//				Chest chest = (Chest)clicked.getState();
//				InventoryHolder ih = chest.getInventory().getHolder();
//				
//				if(ih instanceof DoubleChest){
//					DoubleChest dchest = (DoubleChest)ih;
//					Chest chestLeft = (Chest)dchest.getLeftSide();
//					Chest chestRight = (Chest)dchest.getRightSide();
//					
//					for(ShopObject shop : allShops){
//						if(shop.getLocation().equals(chestLeft.getLocation()) || shop.getLocation().equals(chestRight.getLocation())){
//							//player clicked on shop that was not their own
//							if(!shop.getOwner().equals(player.getName())){
//								if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator"))){
//									player.sendMessage(ChatColor.GRAY+"You are opening a shop owned by "+shop.getOwner());
//									return;
//								}
//								event.setCancelled(true);
//								player.sendMessage(ChatColor.RED+"You must right click the sign to use this shop.");
//								return;
//							}
//						}
//					}
//				}
//				else{
//					for(ShopObject shop : allShops){
//						if(shop.getLocation().equals(chest.getLocation())){
//							//player clicked on shop that was not their own
//							if(!shop.getOwner().equals(player.getName())){
//								if(player.isOp() || (Shop.usePerms && player.hasPermission("shop.operator"))){
//									player.sendMessage(ChatColor.GRAY+"You are opening a shop owned by "+shop.getOwner());
//									return;
//								}
//								event.setCancelled(true);
//								player.sendMessage(ChatColor.RED+"You must right click the sign to use this shop.");
//								return;
//							}
//						}
//					}
//				}
//			}
	
//			else if(clicked.getType() == Material.WALL_SIGN && Shop.econ==null){
//				for(ShopObject shop : allShops){
//					//left clicked on a different shop
//					if(shop.getSignLocation().equals(clicked.getLocation())){
//						
//						if(Shop.usePerms && ! (player.hasPermission("shop.use"))){
//							event.setCancelled(true);
//							player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
//							return;
//						}
//						
//						//player clicked on shop that was not their own
//						if(!shop.getOwner().equals(player.getName())){
//							if(shop.isSellingShop()){
//								ItemStack itemPrice = new ItemStack(Shop.economyItemId);
//							
//								if(!player.getInventory().containsAtLeast(itemPrice, (int)shop.getPrice())){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ Shop.economyItemName+" to buy from this shop.");
//									return;
//								}
//								
//								if(shop.isAdminShop()){
//									itemPrice.setAmount((int)shop.getPrice());
//									ItemStack item = new ItemStack(shop.getDisplayItem().getType(), shop.getAmount(), shop.getDisplayItem().getData());
//									player.getInventory().removeItem(itemPrice);
//									player.getInventory().addItem(item);
//									player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
//									player.updateInventory();
//									return;
//								}
//								
//								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
//								if(!chest.getInventory().containsAtLeast(item, shop.getAmount())){
//									player.sendMessage(ChatColor.RED+"This shop is out of stock.");
//									return;
//								}
//								Player owner = Bukkit.getPlayer(shop.getOwner());
//								if(owner != null)
//									owner.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
//									
//								itemPrice.setAmount((int)shop.getPrice());
//								item.setAmount(shop.getAmount());
//								player.getInventory().removeItem(itemPrice);
//								HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//			                    if (!leftOver.isEmpty()) 
//			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//								chest.getInventory().removeItem(item);
//								chest.getInventory().addItem(itemPrice);
//								player.updateInventory();
//								player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
//								return;
//							}
//							else{
//								ItemStack itemPrice = new ItemStack(Shop.economyItemId);
//								
//								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
//								if(!player.getInventory().containsAtLeast(item, shop.getAmount())){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this shop.");
//									return;
//								}
//								
//								if(shop.isAdminShop()){
//									itemPrice.setAmount((int)shop.getPrice());
//									item.setAmount(shop.getAmount());
//									player.getInventory().removeItem(item);
//									HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
//				                    if (!leftOver.isEmpty()) 
//				                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//									player.updateInventory();
//									player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
//									return;
//								}
//								
//								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//								if(!chest.getInventory().containsAtLeast(itemPrice, (int)shop.getPrice())){
//									player.sendMessage(ChatColor.RED+"This shop is out of funds.");
//									return;
//								}
//								if(chest.getInventory().firstEmpty() == -1){
//									player.sendMessage(ChatColor.RED+"This chest is currently too full to sell to.");
//									return;
//								}
//		
//								Player owner = Bukkit.getPlayer(shop.getOwner());
//								if(owner != null)
//									owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
//									
//								itemPrice.setAmount((int)shop.getPrice());
//								item.setAmount(shop.getAmount());
//								chest.getInventory().removeItem(itemPrice);
//								chest.getInventory().addItem(item);
//								player.getInventory().removeItem(item);
//								HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
//			                    if (!leftOver.isEmpty()) 
//			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//								player.updateInventory();
//								player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+shop.getOwner()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
//								return;
//							}
//						}
//						//the player has clicked on their own shop
//						else{
//							Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//							
//							if(shop.isSellingShop()){
//								int amountOfMoney = getAmount(chest.getInventory(), Shop.economyItemId);
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+Shop.economyItemName+".");
//							}
//							else{
//								int amountOfItems = getAmount(chest.getInventory(), shop.getDisplayItem().getType().getId(), shop.getDisplayItem().getData());
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
//							}
//						}
//					}
//					
//				}
//				player.updateInventory();
//			}
//			else if(clicked.getType() == Material.WALL_SIGN && Shop.econ != null){
//				for(ShopObject shop : allShops){
//					//left clicked on a different shop
//					if(shop.getSignLocation().equals(clicked.getLocation())){
//						
//						if(Shop.usePerms && ! (player.hasPermission("shop.use"))){
//							event.setCancelled(true);
//							player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
//							return;
//						}
//						
//						//player clicked on shop that was not their own
//						if(!shop.getOwner().equals(player.getName())){
//							if(shop.isSellingShop()){
//								double balance = Shop.econ.getBalance(player.getName());
//								
//								if(balance < shop.getPrice()){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ Shop.economyItemName+" to buy from this shop.");
//									return;
//								}
//								
//								if(shop.isAdminShop()){
//									ItemStack item = new ItemStack(shop.getDisplayItem().getType(), shop.getAmount(), shop.getDisplayItem().getData());
//									HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//				                    if (!leftOver.isEmpty()) 
//				                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//									Shop.econ.withdrawPlayer(player.getName(), shop.getPrice());
//									player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
//									return;
//								}
//								
//								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
//								if(!chest.getInventory().containsAtLeast(item, shop.getAmount())){
//									player.sendMessage(ChatColor.RED+"This shop is out of stock.");
//									return;
//								}
//
//								item.setAmount(shop.getAmount());
//								EconomyResponse r = Shop.econ.withdrawPlayer(player.getName(), shop.getPrice());
//					            if(r.transactionSuccess()) 
//					            	player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName +".");
//					            else 
//					                player.sendMessage(String.format("An error occured: %s", r.errorMessage));
//
//					            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//			                    if (!leftOver.isEmpty()) 
//			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//								chest.getInventory().removeItem(item);
//								
//								EconomyResponse er = Shop.econ.depositPlayer(shop.getOwner(), shop.getPrice());
//					            if(er.transactionSuccess()){ 
//					            	Player p = Bukkit.getPlayer(shop.getOwner());
//					            	if(p != null)
//					            		p.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
//					            }
//					            else 
//					                System.out.println(String.format("An error occured: %s", er.errorMessage));
//
//								player.updateInventory();
//								return;
//							}
//							else{
//								double balance = Shop.econ.getBalance(shop.getOwner());	
//								
//								ItemStack item = new ItemStack(shop.getDisplayItem().getType(), 1, shop.getDisplayItem().getData());
//								if(!player.getInventory().containsAtLeast(item,shop.getAmount())){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this shop.");
//									return;
//								}
//								
//								if(shop.isAdminShop()){
//									item.setAmount(shop.getAmount());
//									player.getInventory().removeItem(item);
//									Shop.econ.depositPlayer(player.getName(), shop.getPrice());
//									player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
//									return;
//								}
//								
//								if(balance < shop.getPrice()){
//									player.sendMessage(ChatColor.RED+"This shop's owner is out of funds.");
//									return;
//								}
//									
//								EconomyResponse r = Shop.econ.withdrawPlayer(shop.getOwner(), shop.getPrice());
//					            if(r.transactionSuccess()){ 
//					            	Player owner = Bukkit.getPlayer(shop.getOwner());
//									if(owner != null)
//										owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+shop.getPrice()+Shop.economyItemName+".");
//					            }
//					            else 
//					                System.out.println(String.format("An error occured: %s", r.errorMessage));
//					            
//					            Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//					            
//					            if(chest.getInventory().firstEmpty() == -1){
//									player.sendMessage(ChatColor.RED+"This chest is currently too full to sell to.");
//									return;
//								}
//					            
//								item.setAmount(shop.getAmount());
//								chest.getInventory().addItem(item);
//								player.getInventory().removeItem(item);
//								
//								EconomyResponse er = Shop.econ.depositPlayer(player.getName(), shop.getPrice());
//					            if(er.transactionSuccess()){ 
//					            	player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ shop.getAmount() +" "+ChatColor.GRAY+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+shop.getOwner()+" for "+ChatColor.GOLD+shop.getPrice()+" "+ Shop.economyItemName+".");
//					            }
//					            else 
//					                System.out.println(String.format("An error occured: %s", er.errorMessage));
//					            
//								player.updateInventory();
//								return;
//							}
//						}
//						//the player has clicked on their own shop
//						else{
//							Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//							
//							if(shop.isSellingShop() && Shop.econ == null){
//								int amountOfMoney = getAmount(chest.getInventory(), Shop.economyItemId);
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+Shop.economyItemName+".");
//							}
//							else if(shop.isSellingShop() == false){
//								int amountOfItems = getAmount(chest.getInventory(), shop.getDisplayItem().getType().getId(), shop.getDisplayItem().getData());
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+shop.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
//							}
//						}
//					}
//					
//				}
//			}
//			player.updateInventory();
//		}
//	}

	//TODO make these call player destroy shop event
	//DEALS WITH THE DESTRUCTION OF SHOPS AND THEIR SIGNS
	
	
	//TODO method for getting all matching items in chest inventory
	public void buyFromShop(Player player, ShopObject shop){
		DisplayItem di = shop.getDisplayItem();
		ItemStack item = new ItemStack(di.getType(), shop.getAmount(), di.getData());
		item.setDurability(di.getDurability());
	
	}
	
	public boolean canPutItemInShop(ShopObject shop, ItemStack item, Player p){
		DisplayItem di = shop.getDisplayItem();
		if(item.getType() != di.getType()){
			p.sendMessage(ChatColor.GRAY+"This item's type does not match the shop's item's type.");
			return false;
		}
		if(item.getData().getData() != di.getData()){
			p.sendMessage(ChatColor.GRAY+"This item's sub-type does not match the shop's item's sub-type.");
			return false;
		}
		if(item.getDurability() < di.getDurability()){
			p.sendMessage(ChatColor.GRAY+"This item's durability is less than the shop's item's durability.");
			return false;
		}
		String name = "";
		ItemMeta im = item.getItemMeta();
        if(im.getDisplayName() != null && !im.getDisplayName().equals("air"))
        	name = im.getDisplayName();
        else
        	name = item.getType().name().replace("_", " ").toLowerCase();
        
        if(!di.getDisplayName().equalsIgnoreCase(name)){
        	p.sendMessage(ChatColor.GRAY+"This item's name does not match the shop's item's name.");
			return false;
		}
        if(!im.getLore().equals(di.getLore())){
        	p.sendMessage(ChatColor.GRAY+"This item's description does not match the shop's item's description.");
        	return false;
        }
        for(Entry<Integer, Integer> diEntry : di.getEnchantments().entrySet()){
        	for(Entry<Enchantment, Integer> iEntry : item.getEnchantments().entrySet()){
        		if(iEntry.getKey().getId() != diEntry.getKey().intValue()){
                	p.sendMessage(ChatColor.GRAY+"This item's enchantments do not match the shop's item's enchantments.");
                	return false;
        		}
        		if(iEntry.getValue().intValue() != diEntry.getValue().intValue()){
        			p.sendMessage(ChatColor.GRAY+"This item's enchantment levels do not match the shop's item's enchantment levels.");
                	return false;
        		}
        	}
        }
        
        //TODO check if its a potion and then check values
        return true;
	}
	
	
	public ArrayList<ShopObject> getAllShopsInChunk(Chunk chunk){
		ArrayList<ShopObject> shops = new ArrayList<ShopObject>();
		
		for(ShopObject shop : allShops){
			if(shop.getLocation().getChunk().equals(chunk))
				shops.add(shop);
		}
		return shops;
	}
	
	public int getAmount(Inventory inventory, int id, byte data)
	{
	       	ItemStack[] items = inventory.getContents();
	        int has = 0;
	        for (ItemStack item : items)
	        {
	            if ((item != null) && (item.getTypeId() == id) && (item.getAmount() > 0) && (item.getData().getData() == data))
	            {
	                has += item.getAmount();
	            }
	        }
	        return has;
	}
	
	public int getAmount(Inventory inventory, int id)
	{
	        ItemStack[] items = inventory.getContents();
	        int has = 0;
	        for (ItemStack item : items)
	        {
	            if ((item != null) && (item.getTypeId() == id) && (item.getAmount() > 0))
	            {
	                has += item.getAmount();
	            }
	        }
	        return has;
	    }
	
	public boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    return true;
	}
	
	public boolean isDouble(String s) {
	    try { 
	        Double.parseDouble(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    return true;
	}
	
	public ArrayList<Double> getValues(Location loc)
	{
	    if(signsAwaitingItems.containsKey(loc))
	      return signsAwaitingItems.get(loc);
	    else
	    	return null;
	}
	
	public void setValues(Location loc, ArrayList<Double> al)
	{
		signsAwaitingItems.put(loc, al);
	}
	
//	public ItemStack getClickedShopItem(Inventory inv){
//		if(clickedShopInventories.containsKey(inv)){
//			return clickedShopInventories.get(inv);
//		}
//		return null;
//	}
	
	public BlockFace yawToFace(float yaw){
		final BlockFace[] axis = { BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST };
	    return axis[Math.round(yaw / 90f) & 0x3];
	}
}
