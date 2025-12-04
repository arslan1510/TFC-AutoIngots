package com.tfcautoingots.client;

import com.tfcautoingots.TFCAutoIngots;
import com.tfcautoingots.texture.IngotTextureGenerator;
import com.tfcautoingots.util.TagExtractor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handles texture generation for ingots and registration of the dynamic resource pack.
 */
@EventBusSubscriber(modid = TFCAutoIngots.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TextureGenerationHandler {
    private static boolean texturesGenerated = false;

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            PackLocationInfo locationInfo = new PackLocationInfo(
                "tfcautoingots_generated",
                Component.literal("TFC AutoIngots Generated"),
                PackSource.BUILT_IN,
                Optional.empty()
            );
            
            Pack.ResourcesSupplier resourcesSupplier = DynamicIngotTexturePack.createResourcesSupplier();
            
            event.addRepositorySource((profileAdder) -> {
                try {
                    PackSelectionConfig selectionConfig = new PackSelectionConfig(
                        true,  // required
                        Pack.Position.TOP,  // defaultPosition
                        true   // fixedPosition
                    );
                    
                    Pack pack = Pack.readMetaAndCreate(
                        locationInfo,
                        resourcesSupplier,
                        PackType.CLIENT_RESOURCES,
                        selectionConfig
                    );
                    profileAdder.accept(pack);
                } catch (Exception e) {
                    TFCAutoIngots.getLogger().error("Failed to create dynamic pack: {}", e.getMessage(), e);
                }
            });
            TFCAutoIngots.getLogger().info("Registered dynamic resource pack for ingot textures");
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (!texturesGenerated) {
                generateAllTextures();
            }
        });
    }

    /**
     * Generate textures for all discovered ingots.
     */
    private static void generateAllTextures() {
        if (texturesGenerated) {
            return;
        }

        TFCAutoIngots.getLogger().info("Generating ingot textures...");
        
        try {
            Map<String, net.minecraft.world.item.Item> metalToIngot = discoverMetals();
            TFCAutoIngots.getLogger().info("Found {} unique metals", metalToIngot.size());
            
            int successCount = generateBatch(metalToIngot);
            texturesGenerated = true;
            
            logResults(successCount, metalToIngot.size());
        } catch (Exception e) {
            TFCAutoIngots.getLogger().error("Error generating textures: {}", e.getMessage(), e);
        }
    }

    /**
     * Discover all ingot metals from registered items.
     */
    private static Map<String, net.minecraft.world.item.Item> discoverMetals() {
        return TagExtractor.extractMetalNames();
    }

    /**
     * Generate textures for a batch of metals.
     */
    private static int generateBatch(Map<String, net.minecraft.world.item.Item> metalToIngot) {
        int successCount = 0;
        for (Map.Entry<String, net.minecraft.world.item.Item> entry : metalToIngot.entrySet()) {
            if (IngotTextureGenerator.generateTexture(entry.getKey(), entry.getValue())) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * Log generation results.
     */
    private static void logResults(int successCount, int totalCount) {
        TFCAutoIngots.getLogger().info("Generated textures for {}/{} metals", successCount, totalCount);
        
        Set<String> registeredMetals = DynamicIngotTexturePack.getRegisteredMetals();
        if (registeredMetals.size() > 0) {
            TFCAutoIngots.getLogger().info("═══════════════════════════════════════════════════════");
            TFCAutoIngots.getLogger().info("TFC AutoIngots: Generated {} texture files", registeredMetals.size());
            TFCAutoIngots.getLogger().info("Textures are available at runtime - no resource pack selection needed");
            TFCAutoIngots.getLogger().info("═══════════════════════════════════════════════════════");
        } else {
            TFCAutoIngots.getLogger().warn("No textures were generated! Check logs above for errors.");
        }
    }
}
