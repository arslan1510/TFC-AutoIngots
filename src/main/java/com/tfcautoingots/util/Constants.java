package com.tfcautoingots.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Constants used throughout the mod.
 */
public final class Constants {
    private Constants() {}

    /** Default gray color used when color extraction fails */
    public static final int DEFAULT_COLOR = 0xFFAAAAAA;

    /** Transparency threshold for pixel processing (0-255) */
    public static final int TRANSPARENCY_THRESHOLD = 128;

    /** Resource pack format version */
    public static final int PACK_FORMAT = 34;

    /** Template texture location from mod resources */
    public static final ResourceLocation TEMPLATE_TEXTURE = ResourceLocation.fromNamespaceAndPath("tfcautoingots", "textures/block/metal/smooth/template.png");

    /** Fallback template texture from TFC */
    public static final ResourceLocation TFC_TEMPLATE_FALLBACK = ResourceLocation.fromNamespaceAndPath("tfc", "textures/block/metal/smooth/copper.png");

    /** Namespace for generated textures */
    public static final String TFC_NAMESPACE = "tfc";

    /** Path prefix for ingot pile textures */
    public static final String TEXTURE_PATH_PREFIX = "textures/block/metal/smooth/";
}

