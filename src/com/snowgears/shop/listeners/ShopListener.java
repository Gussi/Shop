package com.snowgears.shop.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
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
	public HashMap<Location, String> signsAwaitingItems = new HashMap<Location, String>(); //location of sign, person who created sign
	private HashMap<String, ShopObject> playersViewingShops = new HashMap<String, ShopObject>(); //player name, is viewing shop
	
	public ShopListener(Shop instance)
    {
        plugin = instance;
    }
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPlayerShopExchange(PlayerShopExchangeEvent event){
		if(event.isCancelled())
			return;
		//event is only called if both player and shop can complete transaction
		Player player = event.getPlayer();
		ShopObject shop = event.getShop();
		
		//TODO do not take or remove things from admin shops
		//TODO make private method to display message to player. (use item name if not null, otherwise, material name)
			
			if(shop.getType() == ShopType.SELLING){
				//remove item shop is selling to player
				if(!shop.isAdminShop())
					shop.getInventory().removeItem(event.getItemPlayerReceived());
				//add item player purchased from the shop
				HashMap<Integer, ItemStack> excess = player.getInventory().addItem(event.getItemPlayerReceived());
				for (Map.Entry<Integer, ItemStack> me : excess.entrySet()) {
					player.getWorld().dropItem(player.getLocation(), me.getValue());
				}
				
				if(plugin.useVault){
					//remove money from player
					plugin.econ.withdrawPlayer(player.getName(), event.getMoneyShopReceived());
					//pay that money to the shop
					if(!shop.isAdminShop())
						plugin.econ.depositPlayer(shop.getOwner(), event.getMoneyShopReceived());
				}
				//using item economy
				else{
					//remove item money from player
					player.getInventory().removeItem(event.getItemShopReceived());
					//pay that item money to the shop
					if(!shop.isAdminShop())
						shop.getInventory().addItem(event.getItemShopReceived());
				}
				
			}
			else if(shop.getType() == ShopType.BUYING){
				//remove item player is selling to shop
				player.getInventory().removeItem(event.getItemShopReceived());
				//add the item being purchased by the shop to the shops inventory
				if(!shop.isAdminShop())
					shop.getInventory().addItem(event.getItemShopReceived());
				
				if(plugin.useVault){
					//remove money from shop
					if(!shop.isAdminShop())
						plugin.econ.withdrawPlayer(shop.getOwner(), event.getMoneyPlayerReceived());
					//pay that money to the player
					plugin.econ.depositPlayer(player.getName(), event.getMoneyPlayerReceived());
				}
				//using item economy
				else{
					//remove item money from shop
					if(!shop.isAdminShop())
						shop.getInventory().removeItem(event.getItemPlayerReceived());
					//pay that item money to the player
					HashMap<Integer, ItemStack> excess = player.getInventory().addItem(event.getItemPlayerReceived());
					for (Map.Entry<Integer, ItemStack> me : excess.entrySet()) {
						player.getWorld().dropItem(player.getLocation(), me.getValue());
					}
				}
			}
			
			player.updateInventory();
			sendExchangeMessages(shop, player);
	}
	
	private void sendExchangeMessages(ShopObject shop, Player player){
		if(shop.getType() == ShopType.SELLING){
			ItemStack itemPurchased = shop.getDisplayItem().getItemStack();
			ItemMeta im = itemPurchased.getItemMeta();
			String purchasedName;
			if(im.getDisplayName() == null)
				purchasedName = (itemPurchased.getType().name().replace("_", " ").toLowerCase())+"(s)";
			else
				purchasedName = im.getDisplayName()+"(s)";
			player.sendMessage(ChatColor.GRAY+"You bought "+ChatColor.WHITE+ shop.getAmount()+" "+ purchasedName +ChatColor.GRAY+ " from "+shop.getOwner()+" for "+ChatColor.GREEN+shop.getPrice()+" "+plugin.economyDisplayName+ChatColor.GRAY+".");
			
			Player owner = Bukkit.getPlayer(shop.getOwner());
			if(owner != null)
				owner.sendMessage(ChatColor.GRAY+player.getName()+" bought "+ChatColor.WHITE+ shop.getAmount()+" "+ purchasedName +ChatColor.GRAY+ " from you for "+ChatColor.GREEN+shop.getPrice()+" "+plugin.economyDisplayName+ChatColor.GRAY+".");
		}
		else if(shop.getType() == ShopType.BUYING){
			ItemStack itemSold = shop.getDisplayItem().getItemStack();
			ItemMeta im = itemSold.getItemMeta();
			String soldName;
			if(im.getDisplayName() == null)
				soldName = (itemSold.getType().name().replace("_", " ").toLowerCase())+"(s)";
			else
				soldName = im.getDisplayName()+"(s)";
			player.sendMessage(ChatColor.GRAY+"You sold "+ChatColor.WHITE+ shop.getAmount()+" "+ soldName +ChatColor.GRAY+ " to "+shop.getOwner()+" for "+ChatColor.GREEN+shop.getPrice()+" "+plugin.economyDisplayName+ChatColor.GRAY+".");
			
			Player owner = Bukkit.getPlayer(shop.getOwner());
			if(owner != null)
				owner.sendMessage(ChatColor.GRAY+player.getName()+" sold "+ChatColor.WHITE+ shop.getAmount()+" "+ soldName +ChatColor.GRAY+ " to you for "+ChatColor.GREEN+shop.getPrice()+" "+plugin.economyDisplayName+ChatColor.GRAY+".");
		}
	}

	//cancel putting other items into shop
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onClick(final InventoryClickEvent event){
		if(!(event.getWhoClicked() instanceof Player)){
			return;
		}
		final Player player = (Player)event.getWhoClicked();
		
		ShopObject shop = getShopPlayerIsViewing(player);
		if(shop == null)
			return;
		
		if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY){
			//trying to put in shop part of inventory
			if(event.getRawSlot() != event.getSlot()){
				if(!canPutItemInShop(shop, event.getCurrentItem(), player)){
					event.setCancelled(true);
				}
			}
		}
		//TODO once you cancel this one, the item on cursor becomes invisible. Try doing event.setCursor()
		else if(!event.getCursor().getType().equals(Material.AIR)){ //have an item 
			//trying to put in shop part of inventory
			if(event.getRawSlot() == event.getSlot()){ 
				ItemStack cursor = new ItemStack(event.getCursor());
				if(!canPutItemInShop(shop, cursor, player)){
					event.setCancelled(true);
					event.setCursor(new ItemStack(Material.AIR));
					player.getInventory().addItem(cursor);
				}
			}
		}
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
			public void run() { 
				player.updateInventory();
			}
		}, 5L); 
	}
	
	//cancel putting other items into shop
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onDrag(final InventoryDragEvent event){
		if(!(event.getWhoClicked() instanceof Player)){
			return;
		}
		final Player player = (Player)event.getWhoClicked();
		
		ShopObject shop = getShopPlayerIsViewing(player);
		if(shop == null)
			return;
		
		boolean wasDraggedInShop = false;
		for(int slot : event.getRawSlots()){
			if(slot < 27){
				wasDraggedInShop = true;
				break;
			}
		}
		if(!wasDraggedInShop)
			return;
		
		ItemStack toPutInShop = event.getOldCursor();
		boolean canPutInShop = canPutItemInShop(shop, toPutInShop, player);
		
		if(!canPutInShop){
			event.setCancelled(true);
		}
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
			public void run() { 
				player.updateInventory();
			}
		}, 5L); 
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
			Potion potion = Potion.fromItemStack(di);
			player.sendMessage(ChatColor.AQUA+"Potion Type: "+ChatColor.WHITE+ capitalize(potion.getType().name().replace("_", " ").toLowerCase()) + ChatColor.GRAY + ", Level "+potion.getLevel());
			player.sendMessage(ChatColor.AQUA+"Potion Effects: ");
			for(PotionEffect effect : potion.getEffects()){
				player.sendMessage(ChatColor.WHITE+"   - "+ChatColor.LIGHT_PURPLE+capitalize(effect.getType().getName().replace("_", " ").toLowerCase())+effect.getAmplifier()+ChatColor.GRAY+ " ("+convertDurationToString(effect.getDuration())+")");
//				System.out.println(effect.getDuration());
			}
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
	
	private String convertDurationToString(int duration){
		duration = duration/20;
		if(duration < 10)
			return "0:0"+duration;
		else if(duration < 60)
			return "0:"+duration;
		double mins = duration/60;
		double secs = (mins - (int)mins);
		secs = (double)Math.round(secs * 100000) / 100000; //round to 2 decimal places
		if(secs == 0)
			return (int)mins+":00";
		else if(secs < 10)
			return (int)mins+":0"+(int)secs;
		else
			return (int)mins+":"+(int)secs;
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

		signsAwaitingItems.put(event.getSignLocation(), player.getName());
		
		if(event.getShopType() == ShopType.SELLING)
			player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with the item you want to sell to other players!");
		else
			player.sendMessage(ChatColor.GOLD+"[Shop] Now just hit the sign with the item you want to buy from other players!");
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onCreateShop(final PlayerCreateShopEvent event){
		if(signsAwaitingItems.containsKey(event.getShop().getSignLocation())){
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
				public void run() { 
					signsAwaitingItems.remove(event.getShop().getSignLocation());
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
			player.sendMessage(ChatColor.GRAY+"You have removed this "+event.getShop().getType().name()+" shop.");
		}
		else{
			player.sendMessage(ChatColor.GRAY+"You have removed a "+event.getShop().getType().name()+" shop owned by "+event.getShop().getOwner());
		}
	}
	
	public boolean canPutItemInShop(ShopObject shop, ItemStack item, Player p){
		if(!plugin.useVault){
			if(item.getType() == plugin.economyMaterial)
				return true;
		}
		ItemStack di = shop.getDisplayItem().getItemStack();
		
		if(item.getType() != di.getType()){
			p.sendMessage(ChatColor.GRAY+"This item's type does not match the shop's item's type.");
			return false;
		}
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
		if(!(itemMeta.equals(displayItemMeta))){
	    	p.sendMessage(ChatColor.GRAY+"This item's name and/or lore do not match the shop's item's name and/or lore.");
			return false;
		}
        if(! di.getEnchantments().equals(item.getEnchantments())){
        	p.sendMessage(ChatColor.GRAY+"This item's enchantments do not match the shop's item's enchantments.");
        	return false;
        }
        return true;
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
//	
//	public ArrayList<Double> getValues(Location loc)
//	{
//	    if(signsAwaitingItems.containsKey(loc))
//	      return signsAwaitingItems.get(loc);
//	    else
//	    	return null;
//	}
//	
//	public void setValues(Location loc, ArrayList<Double> al)
//	{
//		signsAwaitingItems.put(loc, al);
//	}
//	
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
