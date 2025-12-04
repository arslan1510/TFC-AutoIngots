package com.tfcautoingots.client;

import com.tfcautoingots.TFCAutoIngots;
import com.tfcautoingots.texture.IngotTextureGenerator;
import com.tfcautoingots.util.TagExtractor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles on-demand texture generation when player interacts with ingots.
 * This ensures textures exist before TFC tries to render them.
 */
@EventBusSubscriber(modid = TFCAutoIngots.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class IngotInteractionHandler {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            ItemStack stack = event.getEntity().getMainHandItem();
            if (!stack.isEmpty()) {
                String metalName = TagExtractor.getMetalName(stack.getItem());
                if (metalName != null && !metalName.isEmpty()) {
                    IngotTextureGenerator.generateTexture(metalName, stack.getItem());
                }
            }
        }
    }
}

