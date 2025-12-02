package com.tfcautoingots.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.tfcautoingots.TFCAutoIngots;
import com.tfcautoingots.util.TagExtractor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Generates pile textures for ingots from any mod by recoloring TFC's template texture.
 * Saves to resourcepacks/tfcautoingots_generated/assets/tfc/textures/block/metal/smooth/
 */
public class IngotTextureGenerator {
    private static final Set<String> generatedMetals = new HashSet<>();
    private static Path resourcePackPath = null;
    private static final ResourceLocation TEMPLATE_TEXTURE = ResourceLocation.fromNamespaceAndPath("tfcautoingots", "textures/block/metal/smooth/template.png");
    private static final ResourceLocation TFC_TEMPLATE_FALLBACK = ResourceLocation.fromNamespaceAndPath("tfc", "textures/block/metal/smooth/copper.png");

    public static void initResourcePack(Path gameDir) {
        resourcePackPath = gameDir.resolve("resourcepacks").resolve("tfcautoingots_generated");
        
        try {
            Path texturesPath = resourcePackPath.resolve("assets/tfc/textures/block/metal/smooth");
            Files.createDirectories(texturesPath);
            
            Path packMetaPath = resourcePackPath.resolve("pack.mcmeta");
            if (!Files.exists(packMetaPath)) {
                Files.writeString(packMetaPath, """
                    {
                      "pack": {
                        "description": "Auto-generated TFC ingot pile textures",
                        "pack_format": 34
                      }
                    }
                    """);
                TFCAutoIngots.getLogger().info("Created resource pack at: {}", resourcePackPath);
            }
        } catch (IOException e) {
            TFCAutoIngots.getLogger().error("Failed to create resource pack: {}", e.getMessage());
        }
    }

    public static boolean generateAndSaveTexture(String metalName, Item ingotItem) {
        if (generatedMetals.contains(metalName) || resourcePackPath == null) {
            return generatedMetals.contains(metalName);
        }

        try {
            int targetColor = extractPrimaryColorFromIngot(ingotItem);
            NativeImage pileImage = recolorTemplateTexture(targetColor);
            
            Path texturePath = resourcePackPath.resolve("assets/tfc/textures/block/metal/smooth/" + metalName + ".png");
            pileImage.writeToFile(texturePath);
            pileImage.close();
            generatedMetals.add(metalName);
            TFCAutoIngots.getLogger().info("Generated texture for '{}' at {}", metalName, texturePath);
            return true;
        } catch (Exception e) {
            TFCAutoIngots.getLogger().error("Failed to generate texture for '{}': {}", metalName, e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static Path getResourcePackPath() {
        return resourcePackPath;
    }

    /**
     * Extract the primary color from an ingot item's texture.
     */
    private static int extractPrimaryColorFromIngot(Item ingotItem) {
        int primaryColor = 0xFFAAAAAA; // Default gray

        ResourceLocation textureLocation = TagExtractor.getIngotTextureLocation(ingotItem);

        try {
            Optional<Resource> resourceOpt = Minecraft.getInstance().getResourceManager().getResource(textureLocation);
            if (resourceOpt.isPresent()) {
                try (InputStream stream = resourceOpt.get().open();
                     NativeImage image = NativeImage.read(stream)) {
                    primaryColor = extractAverageColor(image);
                }
            }
        } catch (IOException e) {
            TFCAutoIngots.getLogger().debug("Using default color for {}", BuiltInRegistries.ITEM.getKey(ingotItem));
        }

        return primaryColor;
    }

    /**
     * Extract average color from an image (ignoring transparent pixels).
     */
    private static int extractAverageColor(NativeImage image) {
        int totalR = 0, totalG = 0, totalB = 0, pixelCount = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getPixelRGBA(x, y);
                int alpha = (color >> 24) & 0xFF;
                if (alpha < 128) continue; // Skip transparent pixels

                // NativeImage uses ABGR format
                int r = color & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = (color >> 16) & 0xFF;

                totalR += r;
                totalG += g;
                totalB += b;
                pixelCount++;
            }
        }

        if (pixelCount == 0) {
            return 0xFFAAAAAA; // Default gray
        }

        return (0xFF << 24) | ((totalR / pixelCount) << 16) | ((totalG / pixelCount) << 8) | (totalB / pixelCount);
    }

    /**
     * Load the TFC template texture and recolor it to match the target color.
     * Preserves the pattern and lighting/shading, just changes the base color.
     */
    private static NativeImage recolorTemplateTexture(int targetColor) throws IOException {
        // Try to load template texture from our mod first, fallback to TFC's copper texture
        Optional<Resource> templateOpt = Minecraft.getInstance().getResourceManager().getResource(TEMPLATE_TEXTURE);
        if (!templateOpt.isPresent()) {
            TFCAutoIngots.getLogger().debug("Template not found at {}, trying TFC fallback...", TEMPLATE_TEXTURE);
            templateOpt = Minecraft.getInstance().getResourceManager().getResource(TFC_TEMPLATE_FALLBACK);
            if (!templateOpt.isPresent()) {
                throw new IOException("Template texture not found at " + TEMPLATE_TEXTURE + " or " + TFC_TEMPLATE_FALLBACK);
            }
        }

        try (InputStream stream = templateOpt.get().open();
             NativeImage template = NativeImage.read(stream)) {
            
            // Extract average color from template (for color mapping)
            int templateColor = extractAverageColor(template);
            
            // Create new image with same dimensions
            NativeImage result = new NativeImage(template.getWidth(), template.getHeight(), true);
            
            // Extract RGB components from target and template
            int targetR = (targetColor >> 16) & 0xFF;
            int targetG = (targetColor >> 8) & 0xFF;
            int targetB = targetColor & 0xFF;
            
            int templateR = (templateColor >> 16) & 0xFF;
            int templateG = (templateColor >> 8) & 0xFF;
            int templateB = templateColor & 0xFF;
            
            // Calculate average brightness of template for normalization
            float templateBrightness = (templateR + templateG + templateB) / 3.0f;
            
            // Recolor each pixel while preserving relative brightness and pattern
            for (int y = 0; y < template.getHeight(); y++) {
                for (int x = 0; x < template.getWidth(); x++) {
                    int pixel = template.getPixelRGBA(x, y);
                    int alpha = (pixel >> 24) & 0xFF;
                    
                    if (alpha < 128) {
                        // Transparent pixel - keep as is
                        result.setPixelRGBA(x, y, pixel);
                        continue;
                    }
                    
                    // Extract original RGB (ABGR format)
                    int origR = pixel & 0xFF;
                    int origG = (pixel >> 8) & 0xFF;
                    int origB = (pixel >> 16) & 0xFF;
                    
                    // Calculate relative brightness compared to template average
                    float pixelBrightness = (origR + origG + origB) / 3.0f;
                    float brightnessFactor = templateBrightness > 0 ? pixelBrightness / templateBrightness : 1.0f;
                    
                    // Apply target color with preserved brightness variation
                    int newR = Math.min(255, Math.max(0, (int) (targetR * brightnessFactor)));
                    int newG = Math.min(255, Math.max(0, (int) (targetG * brightnessFactor)));
                    int newB = Math.min(255, Math.max(0, (int) (targetB * brightnessFactor)));
                    
                    // Convert back to ABGR format
                    int newPixel = (alpha << 24) | (newB << 16) | (newG << 8) | newR;
                    result.setPixelRGBA(x, y, newPixel);
                }
            }
            
            return result;
        }
    }
}
