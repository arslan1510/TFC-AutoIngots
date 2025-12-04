package com.tfcautoingots.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.tfcautoingots.TFCAutoIngots;
import com.tfcautoingots.client.DynamicIngotTexturePack;
import com.tfcautoingots.util.Constants;
import com.tfcautoingots.util.TagExtractor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Generates pile textures for ingots from any mod by recoloring TFC's template texture.
 * Textures are generated in-memory and stored in the dynamic resource pack.
 */
public class IngotTextureGenerator {
    /**
     * Generate a texture for the given metal and register it in the dynamic pack.
     * 
     * @param metalName The name of the metal (e.g., "lead")
     * @param ingotItem The ingot item to extract color from
     * @return true if generation was successful, false otherwise
     */
    public static boolean generateTexture(String metalName, Item ingotItem) {
        if (DynamicIngotTexturePack.hasTexture(metalName)) {
            return true;
        }

        try {
            int targetColor = extractPrimaryColorFromIngot(ingotItem);
            NativeImage pileImage = recolorTemplateTexture(targetColor);
            
            DynamicIngotTexturePack.registerTexture(metalName, pileImage);
            TFCAutoIngots.getLogger().debug("Generated texture for '{}'", metalName);
            return true;
        } catch (Exception e) {
            TFCAutoIngots.getLogger().error("Failed to generate texture for '{}': {}", metalName, e.getMessage());
            return false;
        }
    }

    /**
     * Extract the primary color from an ingot item's texture.
     */
    private static int extractPrimaryColorFromIngot(Item ingotItem) {
        int primaryColor = Constants.DEFAULT_COLOR;

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
                if (alpha < Constants.TRANSPARENCY_THRESHOLD) {
                    continue;
                }

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
            return Constants.DEFAULT_COLOR;
        }

        return (0xFF << 24) | ((totalR / pixelCount) << 16) | ((totalG / pixelCount) << 8) | (totalB / pixelCount);
    }

    /**
     * Load the template texture.
     */
    private static NativeImage loadTemplate() throws IOException {
        Optional<Resource> templateOpt = Minecraft.getInstance().getResourceManager().getResource(Constants.TEMPLATE_TEXTURE);
        if (!templateOpt.isPresent()) {
            TFCAutoIngots.getLogger().debug("Template not found at {}, trying TFC fallback...", Constants.TEMPLATE_TEXTURE);
            templateOpt = Minecraft.getInstance().getResourceManager().getResource(Constants.TFC_TEMPLATE_FALLBACK);
            if (!templateOpt.isPresent()) {
                throw new IOException("Template texture not found at " + Constants.TEMPLATE_TEXTURE + " or " + Constants.TFC_TEMPLATE_FALLBACK);
            }
        }

        try (InputStream stream = templateOpt.get().open()) {
            return NativeImage.read(stream);
        }
    }

    /**
     * Calculate color mapping parameters from template and target colors.
     */
    private static ColorMapping calculateColorMapping(int templateColor, int targetColor) {
        int templateR = (templateColor >> 16) & 0xFF;
        int templateG = (templateColor >> 8) & 0xFF;
        int templateB = templateColor & 0xFF;
        
        int targetR = (targetColor >> 16) & 0xFF;
        int targetG = (targetColor >> 8) & 0xFF;
        int targetB = targetColor & 0xFF;
        
        float templateBrightness = (templateR + templateG + templateB) / 3.0f;
        
        return new ColorMapping(targetR, targetG, targetB, templateBrightness);
    }

    /**
     * Apply recoloring to the template image.
     */
    private static void applyRecoloring(NativeImage template, NativeImage result, ColorMapping mapping) {
        for (int y = 0; y < template.getHeight(); y++) {
            for (int x = 0; x < template.getWidth(); x++) {
                int pixel = template.getPixelRGBA(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                
                if (alpha < Constants.TRANSPARENCY_THRESHOLD) {
                    result.setPixelRGBA(x, y, pixel);
                    continue;
                }
                
                // Extract original RGB (ABGR format)
                int origR = pixel & 0xFF;
                int origG = (pixel >> 8) & 0xFF;
                int origB = (pixel >> 16) & 0xFF;
                
                // Calculate relative brightness compared to template average
                float pixelBrightness = (origR + origG + origB) / 3.0f;
                float brightnessFactor = mapping.templateBrightness > 0 ? pixelBrightness / mapping.templateBrightness : 1.0f;
                
                // Apply target color with preserved brightness variation
                int newR = Math.min(255, Math.max(0, (int) (mapping.targetR * brightnessFactor)));
                int newG = Math.min(255, Math.max(0, (int) (mapping.targetG * brightnessFactor)));
                int newB = Math.min(255, Math.max(0, (int) (mapping.targetB * brightnessFactor)));
                
                // Convert back to ABGR format
                int newPixel = (alpha << 24) | (newB << 16) | (newG << 8) | newR;
                result.setPixelRGBA(x, y, newPixel);
            }
        }
    }

    /**
     * Load the TFC template texture and recolor it to match the target color.
     * Preserves the pattern and lighting/shading, just changes the base color.
     */
    private static NativeImage recolorTemplateTexture(int targetColor) throws IOException {
        try (NativeImage template = loadTemplate()) {
            // Extract average color from template (for color mapping)
            int templateColor = extractAverageColor(template);
            
            // Create new image with same dimensions
            NativeImage result = new NativeImage(template.getWidth(), template.getHeight(), true);
            
            // Calculate color mapping
            ColorMapping mapping = calculateColorMapping(templateColor, targetColor);
            
            // Apply recoloring
            applyRecoloring(template, result, mapping);
            
            return result;
        }
    }

    /**
     * Helper class to hold color mapping parameters.
     */
    private static class ColorMapping {
        final int targetR, targetG, targetB;
        final float templateBrightness;

        ColorMapping(int targetR, int targetG, int targetB, float templateBrightness) {
            this.targetR = targetR;
            this.targetG = targetG;
            this.targetB = targetB;
            this.templateBrightness = templateBrightness;
        }
    }
}
