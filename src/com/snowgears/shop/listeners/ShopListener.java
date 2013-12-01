package com.snowgears.shop.listeners;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionType;
import org.bukkit.block.Sign;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.events.PlayerCreateShopEvent;
import com.snowgears.shop.events.PlayerDestroyShopEvent;
import com.snowgears.shop.events.PlayerPreCreateShopEvent;
import com.snowgears.shop.events.PlayerShopExchangeEvent;


public class ShopListener implements Listener{
	
	private Shop plugin = Shop.plugin;
	private HashMap<Location, ArrayList<Double>> signsAwaitingItems = new HashMap<Location, ArrayList<Double>>(); //location of sign, arraylist of values for shop (price, amount)
	private HashMap<String, ShopObject> playersViewingShops = new HashMap<String, ShopObject>(); //player name, is viewing shop
	
	public ShopListener(Shop instance)
    {
        plugin = instance;
    }
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPlayerShopExchange(PlayerShopExchangeEvent event){
		if(event.isCancelled())
			return;
		
	}

	//TODO if item putting into shop is different from shop item in any way, cancel action
	//TODO also make sure to cancel InventoryDragEvent with the same thing
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent event){
		if(!(event.getWhoClicked() instanceof Player)){
			return;
		}
		Player player = (Player)event.getWhoClicked();
		
		ShopObject shop = getShopPlayerIsViewing(player);
		if(shop == null)
			return;
		
		if(!event.getCursor().getType().equals(Material.AIR)){ //have an item
			if(event.getRawSlot() == event.getSlot()){ //trying to put in shop part of inventory
				ItemStack cursor = event.getCursor();
				if(!canPutItemInShop(shop, cursor, player)){
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onShopChestClick(PlayerInteractEvent event){
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
			if(event.getClickedBlock().getType() == Material.CHEST){
				Player player = event.getPlayer();
				ShopObject shop = plugin.shopHandler.getShop(event.getClickedBlock().getLocation());
				if(shop == null)
					return;
				//player is trying to open own shop
				if(shop.getOwner().equals(player.getName())){
					playersViewingShops.put(player.getName(), shop);
				}
				else if((plugin.usePerms && player.hasPermission("plugin.operator")) || player.isOp()){
					if(shop.isAdminShop()){
						event.setCancelled(true);
						showDisplayItemStats(shop, player);
					}
					else
						player.sendMessage(ChatColor.GRAY+"You are opening a shop owned by "+shop.getOwner());
				}
				else{
					event.setCancelled(true);
					showDisplayItemStats(shop, player);
				}
			}
		}
	}
	
	@EventHandler
	public void onShopClose(InventoryCloseEvent event){
		if(playersViewingShops.containsKey(event.getPlayer().getName())){
			playersViewingShops.remove(event.getPlayer().getName());
		}
	}
	
	private void showDisplayItemStats(ShopObject shop, Player player){
		ItemStack di = shop.getDisplayItem().getItemStack();
		ItemMeta im = di.getItemMeta();
		
		player.sendMessage("");
		if(im.getDisplayName() != null)
			player.sendMessage(ChatColor.WHITE+im.getDisplayName());
		else
			player.sendMessage(ChatColor.WHITE+capitalize(di.getType().name().replace("_", " ").toLowerCase()));
		Map<Enchantment,Integer> enchantments = di.getEnchantments();
		
		int durability = getDurabilityPercent(di);
		if(durability != 0){
			if(durability == 100)
				player.sendMessage(ChatColor.GRAY+"Durability: "+durability+"%");
			else
				player.sendMessage(ChatColor.GRAY+"Durability: "+durability+"% or higher");
		}
		
		if(!enchantments.isEmpty()){
			player.sendMessage(ChatColor.YELLOW+"Enchantments:");
			for(Entry<Enchantment, Integer> s : enchantments.entrySet()){
				player.sendMessage(ChatColor.YELLOW+ capitalize(s.getKey().getName().replace("_", " ").toLowerCase()) + ChatColor.WHITE + " level: "+s.getValue()); 
			}
			player.sendMessage("");
		}
		if(di.getType() == Material.POTION){
			PotionType potion = PotionType.getByDamageValue(di.getDurability());
			player.sendMessage(ChatColor.AQUA+"Potion Type: "+ChatColor.WHITE+potion.name());
		}
		player.sendMessage("");
		if(shop.getType() == ShopType.SELLING){
			player.sendMessage(ChatColor.GREEN+"You can buy "+ChatColor.WHITE+shop.getAmount()+ChatColor.GREEN+" "+di.getType().name().replace("_", " ").toLowerCase()+" from this shop for "+ChatColor.WHITE+shop.getPrice()+ChatColor.GREEN+" "+plugin.economyDisplayName+".");
			double pricePer = shop.getPrice()/shop.getAmount();
			pricePer = Math.round(pricePer * 100);
			pricePer = pricePer/100;
			player.sendMessage(ChatColor.GRAY+"That is "+pricePer+" "+plugin.economyDisplayName+" per "+di.getType().name().replace("_", " ").toLowerCase()+".");
		}
		else{
			player.sendMessage(ChatColor.GREEN+"You can sell "+ChatColor.WHITE+shop.getAmount()+ChatColor.GREEN+" "+di.getType().name().replace("_", " ").toLowerCase()+" to this shop for "+ChatColor.WHITE+shop.getPrice()+ChatColor.GREEN+" "+plugin.economyDisplayName+".");
			double pricePer = shop.getPrice()/shop.getAmount();
			pricePer = Math.round(pricePer * 100);
			pricePer = pricePer/100;
			player.sendMessage(ChatColor.GRAY+"That is "+pricePer+" "+plugin.economyDisplayName+" per "+di.getType().name().replace("_", " ").toLowerCase()+".");
		}
		player.sendMessage("");
		return;
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPrepareShop(PlayerPreCreateShopEvent event){
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
		
		if(event.getShopType() == ShopType.SELLING)
			player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with the item you want to sell to other players!");
		else
			player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with the item you want to buy from other players!");
		plugin.miscListener.invincibleSigns.add(event.getSignLocation());
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onCreateShop(final PlayerCreateShopEvent event){
		plugin.shopListener.setValues(event.getShop().getSignLocation(), null);
		
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
		Sign sign = (Sign)event.getShop().getSignLocation().getBlock().getState();
		if(plugin.econ == null)
			sign.setLine(2, ChatColor.GREEN+""+ (int)event.getShop().getPrice() +" "+ plugin.economyDisplayName);
		else
			sign.setLine(2, ChatColor.GREEN+""+ event.getShop().getPrice() +" "+ plugin.economyDisplayName);
		if(!event.getShop().isAdminShop())
			sign.setLine(3, event.getPlayer().getName());
		sign.update(true);
		
		Player player = event.getPlayer();
		
		plugin.shopHandler.addShop(event.getShop());

		ItemStack di = event.getShop().getDisplayItem().getItemStack();
		if(event.getShop().getType() == ShopType.SELLING)
			player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that sells "+ChatColor.GOLD+ di.getType().name().replace("_", " ").toLowerCase()+"(s)"+ChatColor.YELLOW+".");
		else
			player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that buys "+ChatColor.GOLD+di.getType().name().replace("_", " ").toLowerCase()+"(s)"+ChatColor.YELLOW+".");		
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onShopDestroy(PlayerDestroyShopEvent event){
		if(event.isCancelled()){
			return;
		}
		Player player = event.getPlayer();
		
		event.getShop().delete();
		
		if(event.getShop().getOwner().equals(player.getName())){
			player.sendMessage(ChatColor.GRAY+"You have removed this shop.");
		}
		else{
			player.sendMessage(ChatColor.GRAY+"You have removed a shop owned by "+event.getShop().getOwner());
		}
	}
	
	//TODO find out whats different about this method from non-econ one and merge the two
//	@EventHandler
//	public void onSignEditEcon(SignChangeEvent event){
//		if(plugin.econ == null)
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
//				if(plugin.usePerms && ! (player.hasPermission("plugin.create"))){
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
//						if(player.isOp() || (plugin.usePerms && player.hasPermission("plugin.operator")))
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
//					newSign.setLine(2, ChatColor.RED+""+ price +" "+ plugin.economyItemName);
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
//					if(plugin.usePerms && ! (player.hasPermission("plugin.create"))){
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
//						if(player.isOp() || (plugin.usePerms && player.hasPermission("plugin.operator"))){
//							isAdmin = true;
//							player.sendMessage(ChatColor.GRAY+"You have made an admin plugin.");
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
//					sign.setLine(2, ChatColor.GREEN+""+ plugin.getPrice() +" "+ plugin.economyItemName);
//					sign.update(true);
//					
//					plugin.shopMap.put("load", allShops);
//					plugin.saveHashMapTo(plugin.shopMap, plugin.shopFile);
//					
////					plugin.getDisplayItem().respawn();
//					
//					if(isSelling)
//						player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that sells "+ChatColor.GOLD+ plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+ChatColor.YELLOW+".");
//					else
//						player.sendMessage(ChatColor.YELLOW+"You have successfully created a shop that buys "+ChatColor.GOLD+plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+ChatColor.YELLOW+".");
//					
//					setValues(clicked.getLocation(), null);
//					
//					if(invincibleSigns.contains(clicked.getLocation())){
//						plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
//							public void run() { 
//								invincibleSigns.remove(clicked.getLocation());
//								//i.teleport(plugin.showcaseLocation.deserialize());
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
//						if(plugin.getLocation().equals(chestLeft.getLocation()) || plugin.getLocation().equals(chestRight.getLocation())){
//							//player clicked on shop that was not their own
//							if(!plugin.getOwner().equals(player.getName())){
//								if(player.isOp() || (plugin.usePerms && player.hasPermission("plugin.operator"))){
//									player.sendMessage(ChatColor.GRAY+"You are opening a shop owned by "+plugin.getOwner());
//									return;
//								}
//								event.setCancelled(true);
//								player.sendMessage(ChatColor.RED+"You must right click the sign to use this plugin.");
//								return;
//							}
//						}
//					}
//				}
//				else{
//					for(ShopObject shop : allShops){
//						if(plugin.getLocation().equals(chest.getLocation())){
//							//player clicked on shop that was not their own
//							if(!plugin.getOwner().equals(player.getName())){
//								if(player.isOp() || (plugin.usePerms && player.hasPermission("plugin.operator"))){
//									player.sendMessage(ChatColor.GRAY+"You are opening a shop owned by "+plugin.getOwner());
//									return;
//								}
//								event.setCancelled(true);
//								player.sendMessage(ChatColor.RED+"You must right click the sign to use this plugin.");
//								return;
//							}
//						}
//					}
//				}
//			}
	
//			else if(clicked.getType() == Material.WALL_SIGN && plugin.econ==null){
//				for(ShopObject shop : allShops){
//					//left clicked on a different shop
//					if(plugin.getSignLocation().equals(clicked.getLocation())){
//						
//						if(plugin.usePerms && ! (player.hasPermission("plugin.use"))){
//							event.setCancelled(true);
//							player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
//							return;
//						}
//						
//						//player clicked on shop that was not their own
//						if(!plugin.getOwner().equals(player.getName())){
//							if(plugin.isSellingShop()){
//								ItemStack itemPrice = new ItemStack(plugin.economyItemId);
//							
//								if(!player.getInventory().containsAtLeast(itemPrice, (int)plugin.getPrice())){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ plugin.economyItemName+" to buy from this plugin.");
//									return;
//								}
//								
//								if(plugin.isAdminShop()){
//									itemPrice.setAmount((int)plugin.getPrice());
//									ItemStack item = new ItemStack(plugin.getDisplayItem().getType(), plugin.getAmount(), plugin.getDisplayItem().getData());
//									player.getInventory().removeItem(itemPrice);
//									player.getInventory().addItem(item);
//									player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName+".");
//									player.updateInventory();
//									return;
//								}
//								
//								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//								ItemStack item = new ItemStack(plugin.getDisplayItem().getType(), 1, plugin.getDisplayItem().getData());
//								if(!chest.getInventory().containsAtLeast(item, plugin.getAmount())){
//									player.sendMessage(ChatColor.RED+"This shop is out of stock.");
//									return;
//								}
//								Player owner = Bukkit.getPlayer(plugin.getOwner());
//								if(owner != null)
//									owner.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+plugin.getPrice()+plugin.economyItemName+".");
//									
//								itemPrice.setAmount((int)plugin.getPrice());
//								item.setAmount(plugin.getAmount());
//								player.getInventory().removeItem(itemPrice);
//								HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//			                    if (!leftOver.isEmpty()) 
//			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//								chest.getInventory().removeItem(item);
//								chest.getInventory().addItem(itemPrice);
//								player.updateInventory();
//								player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName+".");
//								return;
//							}
//							else{
//								ItemStack itemPrice = new ItemStack(plugin.economyItemId);
//								
//								ItemStack item = new ItemStack(plugin.getDisplayItem().getType(), 1, plugin.getDisplayItem().getData());
//								if(!player.getInventory().containsAtLeast(item, plugin.getAmount())){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this plugin.");
//									return;
//								}
//								
//								if(plugin.isAdminShop()){
//									itemPrice.setAmount((int)plugin.getPrice());
//									item.setAmount(plugin.getAmount());
//									player.getInventory().removeItem(item);
//									HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
//				                    if (!leftOver.isEmpty()) 
//				                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//									player.updateInventory();
//									player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName+".");
//									return;
//								}
//								
//								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//								if(!chest.getInventory().containsAtLeast(itemPrice, (int)plugin.getPrice())){
//									player.sendMessage(ChatColor.RED+"This shop is out of funds.");
//									return;
//								}
//								if(chest.getInventory().firstEmpty() == -1){
//									player.sendMessage(ChatColor.RED+"This chest is currently too full to sell to.");
//									return;
//								}
//		
//								Player owner = Bukkit.getPlayer(plugin.getOwner());
//								if(owner != null)
//									owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+plugin.getPrice()+plugin.economyItemName+".");
//									
//								itemPrice.setAmount((int)plugin.getPrice());
//								item.setAmount(plugin.getAmount());
//								chest.getInventory().removeItem(itemPrice);
//								chest.getInventory().addItem(item);
//								player.getInventory().removeItem(item);
//								HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemPrice);
//			                    if (!leftOver.isEmpty()) 
//			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//								player.updateInventory();
//								player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+plugin.getOwner()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName+".");
//								return;
//							}
//						}
//						//the player has clicked on their own shop
//						else{
//							Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//							
//							if(plugin.isSellingShop()){
//								int amountOfMoney = getAmount(chest.getInventory(), plugin.economyItemId);
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+plugin.economyItemName+".");
//							}
//							else{
//								int amountOfItems = getAmount(chest.getInventory(), plugin.getDisplayItem().getType().getId(), plugin.getDisplayItem().getData());
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
//							}
//						}
//					}
//					
//				}
//				player.updateInventory();
//			}
//			else if(clicked.getType() == Material.WALL_SIGN && plugin.econ != null){
//				for(ShopObject shop : allShops){
//					//left clicked on a different shop
//					if(plugin.getSignLocation().equals(clicked.getLocation())){
//						
//						if(plugin.usePerms && ! (player.hasPermission("plugin.use"))){
//							event.setCancelled(true);
//							player.sendMessage(ChatColor.RED+"You are not authorized to use shops.");
//							return;
//						}
//						
//						//player clicked on shop that was not their own
//						if(!plugin.getOwner().equals(player.getName())){
//							if(plugin.isSellingShop()){
//								double balance = plugin.econ.getBalance(player.getName());
//								
//								if(balance < plugin.getPrice()){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ plugin.economyItemName+" to buy from this plugin.");
//									return;
//								}
//								
//								if(plugin.isAdminShop()){
//									ItemStack item = new ItemStack(plugin.getDisplayItem().getType(), plugin.getAmount(), plugin.getDisplayItem().getData());
//									HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//				                    if (!leftOver.isEmpty()) 
//				                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//									plugin.econ.withdrawPlayer(player.getName(), plugin.getPrice());
//									player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName+".");
//									return;
//								}
//								
//								Chest chest = (Chest)clicked.getRelative(((org.bukkit.material.Sign) clicked.getState().getData()).getAttachedFace()).getState();
//								ItemStack item = new ItemStack(plugin.getDisplayItem().getType(), 1, plugin.getDisplayItem().getData());
//								if(!chest.getInventory().containsAtLeast(item, plugin.getAmount())){
//									player.sendMessage(ChatColor.RED+"This shop is out of stock.");
//									return;
//								}
//
//								item.setAmount(plugin.getAmount());
//								EconomyResponse r = plugin.econ.withdrawPlayer(player.getName(), plugin.getPrice());
//					            if(r.transactionSuccess()) 
//					            	player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName +".");
//					            else 
//					                player.sendMessage(String.format("An error occured: %s", r.errorMessage));
//
//					            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
//			                    if (!leftOver.isEmpty()) 
//			                        player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
//								chest.getInventory().removeItem(item);
//								
//								EconomyResponse er = plugin.econ.depositPlayer(plugin.getOwner(), plugin.getPrice());
//					            if(er.transactionSuccess()){ 
//					            	Player p = Bukkit.getPlayer(plugin.getOwner());
//					            	if(p != null)
//					            		p.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString()+" from you for "+ChatColor.GOLD+plugin.getPrice()+plugin.economyItemName+".");
//					            }
//					            else 
//					                System.out.println(String.format("An error occured: %s", er.errorMessage));
//
//								player.updateInventory();
//								return;
//							}
//							else{
//								double balance = plugin.econ.getBalance(plugin.getOwner());	
//								
//								ItemStack item = new ItemStack(plugin.getDisplayItem().getType(), 1, plugin.getDisplayItem().getData());
//								if(!player.getInventory().containsAtLeast(item,plugin.getAmount())){
//									player.sendMessage(ChatColor.RED+"You do not have enough "+ plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to sell to this plugin.");
//									return;
//								}
//								
//								if(plugin.isAdminShop()){
//									item.setAmount(plugin.getAmount());
//									player.getInventory().removeItem(item);
//									plugin.econ.depositPlayer(player.getName(), plugin.getPrice());
//									player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString().replace("_", " ").toLowerCase()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName+".");
//									return;
//								}
//								
//								if(balance < plugin.getPrice()){
//									player.sendMessage(ChatColor.RED+"This shop's owner is out of funds.");
//									return;
//								}
//									
//								EconomyResponse r = plugin.econ.withdrawPlayer(plugin.getOwner(), plugin.getPrice());
//					            if(r.transactionSuccess()){ 
//					            	Player owner = Bukkit.getPlayer(plugin.getOwner());
//									if(owner != null)
//										owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().toString()+" to you for "+ChatColor.GOLD+plugin.getPrice()+plugin.economyItemName+".");
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
//								item.setAmount(plugin.getAmount());
//								chest.getInventory().addItem(item);
//								player.getInventory().removeItem(item);
//								
//								EconomyResponse er = plugin.econ.depositPlayer(player.getName(), plugin.getPrice());
//					            if(er.transactionSuccess()){ 
//					            	player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.GOLD+ plugin.getAmount() +" "+ChatColor.GRAY+plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+" to "+plugin.getOwner()+" for "+ChatColor.GOLD+plugin.getPrice()+" "+ plugin.economyItemName+".");
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
//							if(plugin.isSellingShop() && plugin.econ == null){
//								int amountOfMoney = getAmount(chest.getInventory(), plugin.economyItemId);
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfMoney+ChatColor.GRAY+" "+plugin.economyItemName+".");
//							}
//							else if(plugin.isSellingShop() == false){
//								int amountOfItems = getAmount(chest.getInventory(), plugin.getDisplayItem().getType().getId(), plugin.getDisplayItem().getData());
//								player.sendMessage(ChatColor.GRAY+"This shop contains "+ChatColor.GREEN+amountOfItems+ChatColor.GRAY+" "+plugin.getDisplayItem().getType().name().replace("_", " ").toLowerCase()+".");
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
	
	
//	//TODO method for getting all matching items in chest inventory
//	public void buyFromShop(Player player, ShopObject shop){
//		ItemStack di = shop.getDisplayItem().getItemStack();
//		ItemStack item = new ItemStack(di.getType(), shop.getAmount(), di.getMaterialData().getData());
//		item.setDurability(di.getDurability());
//	
//	}
	
	public boolean canPutItemInShop(ShopObject shop, ItemStack item, Player p){
		ItemStack di = shop.getDisplayItem().getItemStack();
		
		if(item.getType() != di.getType()){
			p.sendMessage(ChatColor.GRAY+"This item's type does not match the shop's item's type.");
			return false;
		}
		System.out.println("Item data: "+item.getData().getData());
		System.out.println("Display item data: "+di.getData());
		if(! item.getData().equals(di.getData())){
			p.sendMessage(ChatColor.GRAY+"This item's data-type does not match the shop's item's data-type.");
			return false;
		}
		if(item.getDurability() < di.getDurability()){
			p.sendMessage(ChatColor.GRAY+"This item's durability is less than the shop's item's durability.");
			return false;
		}

		ItemMeta itemMeta = item.getItemMeta();
		ItemMeta displayItemMeta = di.getItemMeta();
		//one of the items is named
//		p.sendMessage("Item name: "+im.getDisplayName());
//		p.sendMessage("DisplayItem name: "+di.getDisplayName());
//		p.sendMessage("DisplayItem isNamed: "+di.hasCustomName());
		if(!(itemMeta.equals(displayItemMeta))){
	    	p.sendMessage(ChatColor.GRAY+"This item's name and/or lore do not match the shop's item's name and/or lore.");
			return false;
		}
        if(! di.getEnchantments().equals(item.getEnchantments())){
        	p.sendMessage(ChatColor.GRAY+"This item's enchantments do not match the shop's item's enchantments.");
        	return false;
        }
        
        //TODO check if its a potion and then check values
        return true;
	}
	
	
//	public ArrayList<ShopObject> getAllShopsInChunk(Chunk chunk){
//		ArrayList<ShopObject> shops = new ArrayList<ShopObject>();
//		
//		for(ShopObject shop : allShops){
//			if(plugin.getLocation().getChunk().equals(chunk))
//				shops.add(shop);
//		}
//		return shops;
//	}
	
	//get amount of itemstack in shop
	public int getAmount(Inventory inventory, ItemStack is)
	{
		MaterialData md = is.getData();
       	ItemStack[] items = inventory.getContents();
        int has = 0;
        for (ItemStack item : items)
        {
            if ((item != null) && (item.getAmount() > 0) && (item.getData().equals(md)))
            {
                has += item.getAmount();
            }
        }
        return has;
	}
	
	public ShopObject getShopPlayerIsViewing(Player player){
		if(playersViewingShops.containsKey(player.getName())){
			return playersViewingShops.get(player.getName());
		}
		return null;
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
	
	public String capitalize(String line)
	{
		String[] spaces = line.split("\\s+");
		String capped = "";
		for(String s : spaces){
			if(s.length() > 1)
				capped = capped + Character.toUpperCase(s.charAt(0)) + s.substring(1)+" ";
			else{
				capped = capped + s.toUpperCase()+" ";
			}
		}
		return capped;
	}
	
	public int getDurabilityPercent(ItemStack is){
		if(is.getType().getMaxDurability() > 0){
			double top = is.getType().getMaxDurability() - is.getDurability();
			double d = top / is.getType().getMaxDurability();
			d = d * 100;
			return (int)d;
		}
		return 0;
	}
}
