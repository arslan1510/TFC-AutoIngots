package com.tfcautoingots.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to extract metal names from ingot items and generate texture paths.
 */
public class TagExtractor {
    private static final TagKey<Item> C_INGOTS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots"));
    private static final Pattern INGOT_PATTERN = Pattern.compile("(?:(.+)_ingot|ingot_(.+))");

    /**
     * Extract metal names from ingot items.
     * Returns a map of metal name -> representative ingot item.
     */
    public static Map<String, Item> extractMetalNames() {
        Map<String, Item> metalToIngot = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            String path = itemId.getPath();
            
            // Check if this is an ingot (by tag or by name)
            boolean isIngot = new ItemStack(item).is(C_INGOTS) || path.contains("ingot");
            
            if (isIngot) {
                String metalName = extractMetalNameFromId(path);
                
                if (metalName != null && !metalName.isEmpty()) {
                    // Skip TFC's own ingots - they already have textures
                    if (itemId.getNamespace().equals("tfc")) {
                        continue;
                    }
                    
                    if (!metalToIngot.containsKey(metalName)) {
                        metalToIngot.put(metalName, item);
                    }
                }
            }
        }

        return metalToIngot;
    }

    /**
     * Extract metal name from item ID path (e.g., lead_ingot -> lead).
     */
    public static String extractMetalNameFromId(String path) {
        Matcher matcher = INGOT_PATTERN.matcher(path);
        if (matcher.find()) {
            String metalName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            
            if (metalName != null) {
                // Handle common prefixes
                if (metalName.startsWith("double_")) {
                    metalName = metalName.substring(7);
                }
                if (metalName.startsWith("raw_")) {
                    metalName = metalName.substring(4);
                }
                return metalName;
            }
        }
        return null;
    }

    /**
     * Get the metal name for a specific item.
     */
    public static String getMetalName(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return extractMetalNameFromId(itemId.getPath());
    }

    /**
     * Get the resource location for an ingot item's texture.
     */
    public static ResourceLocation getIngotTextureLocation(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
    }
}
