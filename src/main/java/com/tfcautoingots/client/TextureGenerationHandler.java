package com.tfcautoingots.client;

import com.tfcautoingots.TFCAutoIngots;
import com.tfcautoingots.texture.IngotTextureGenerator;
import com.tfcautoingots.util.TagExtractor;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.nio.file.Path;
import java.util.Map;

/**
 * Handles texture generation for ingots.
 * Generates textures early and on-demand to prevent caching issues.
 */
@EventBusSubscriber(modid = TFCAutoIngots.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TextureGenerationHandler {
    private static boolean texturesGenerated = false;

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            // Initialize resource pack directory early
            try {
                Path gameDir = FMLPaths.GAMEDIR.get();
                IngotTextureGenerator.initResourcePack(gameDir);
                TFCAutoIngots.getLogger().info("Initialized resource pack directory for texture generation");
            } catch (Exception e) {
                TFCAutoIngots.getLogger().warn("Could not initialize resource pack during AddPackFindersEvent: {}", e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            generateAllTextures();
        });
    }

    /**
     * Generate textures on-demand when player interacts with ingots.
     * This ensures textures exist before TFC tries to render them.
     */
    @EventBusSubscriber(modid = TFCAutoIngots.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class IngotInteractionHandler {
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide()) {
                ItemStack stack = event.getEntity().getMainHandItem();
                if (!stack.isEmpty()) {
                    String metalName = TagExtractor.getMetalName(stack.getItem());
                    if (metalName != null && !metalName.isEmpty()) {
                        // Generate texture on-demand if not already generated
                        IngotTextureGenerator.generateAndSaveTexture(metalName, stack.getItem());
                    }
                }
            }
        }
    }

    private static void generateAllTextures() {
        if (texturesGenerated) {
            return;
        }

        TFCAutoIngots.getLogger().info("Generating ingot textures...");
        
        try {
            // Ensure resource pack is initialized
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            IngotTextureGenerator.initResourcePack(gameDir);
            
            // Generate textures for all known ingots
            Map<String, Item> metalToIngot = TagExtractor.extractMetalNames();
            TFCAutoIngots.getLogger().info("Found {} unique metals", metalToIngot.size());
            
            int successCount = 0;
            for (Map.Entry<String, Item> entry : metalToIngot.entrySet()) {
                if (IngotTextureGenerator.generateAndSaveTexture(entry.getKey(), entry.getValue())) {
                    successCount++;
                }
            }
            
            TFCAutoIngots.getLogger().info("Generated textures for {}/{} metals", successCount, metalToIngot.size());
            texturesGenerated = true;
            
            Path packPath = IngotTextureGenerator.getResourcePackPath();
            if (packPath != null && successCount > 0) {
                // Verify textures exist
                Path texturesDir = packPath.resolve("assets/tfc/textures/block/metal/smooth");
                if (texturesDir.toFile().exists()) {
                    long textureCount = 0;
                    try {
                        textureCount = java.nio.file.Files.list(texturesDir)
                            .filter(p -> p.toString().endsWith(".png"))
                            .count();
                    } catch (Exception e) {
                        // Ignore
                    }
                    
                    TFCAutoIngots.getLogger().info("═══════════════════════════════════════════════════════");
                    TFCAutoIngots.getLogger().info("TFC AutoIngots: Generated {} texture files", textureCount);
                    TFCAutoIngots.getLogger().info("Textures saved to: {}", texturesDir);
                    TFCAutoIngots.getLogger().info("═══════════════════════════════════════════════════════");
                }
            } else if (successCount == 0) {
                TFCAutoIngots.getLogger().warn("No textures were generated! Check logs above for errors.");
            }
        } catch (Exception e) {
            TFCAutoIngots.getLogger().error("Error generating textures: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
