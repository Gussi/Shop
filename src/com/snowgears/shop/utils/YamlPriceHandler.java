package com.snowgears.shop.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;

public class YamlPriceHandler {
	
	public Shop plugin = Shop.getPlugin();
	
	private HashMap<Material, Double> prices = new HashMap<Material, Double>(); //TODO make getters for values
	
	public YamlPriceHandler(Shop instance)
    {
        plugin = instance;
    }
}
