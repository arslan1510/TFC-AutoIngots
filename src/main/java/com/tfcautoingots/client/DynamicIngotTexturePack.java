package com.tfcautoingots.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.tfcautoingots.TFCAutoIngots;
import com.tfcautoingots.util.Constants;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dynamic resource pack that provides generated ingot textures at runtime.
 * Textures are stored in memory and served on-demand.
 */
public class DynamicIngotTexturePack implements net.minecraft.server.packs.PackResources {
    private static final Map<String, NativeImage> TEXTURE_CACHE = new HashMap<>();
    private static final String PACK_NAME = "TFC AutoIngots Generated";
    private final PackLocationInfo locationInfo;

    public DynamicIngotTexturePack(PackLocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    /**
     * Register a generated texture in the cache.
     */
    public static void registerTexture(String metalName, NativeImage texture) {
        TEXTURE_CACHE.put(metalName, texture);
    }

    /**
     * Check if a texture exists in the cache.
     */
    public static boolean hasTexture(String metalName) {
        return TEXTURE_CACHE.containsKey(metalName);
    }

    /**
     * Get all registered metal names.
     */
    public static Set<String> getRegisteredMetals() {
        return TEXTURE_CACHE.keySet();
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) {
            return null;
        }

        // Check if this is a texture we generated
        if (Constants.TFC_NAMESPACE.equals(location.getNamespace())) {
            String path = location.getPath();
            if (path.startsWith(Constants.TEXTURE_PATH_PREFIX) && path.endsWith(".png")) {
                String metalName = path.substring(Constants.TEXTURE_PATH_PREFIX.length(), path.length() - 4);
                
                NativeImage texture = TEXTURE_CACHE.get(metalName);
                if (texture != null) {
                    return () -> {
                        try {
                            byte[] imageData = texture.asByteArray();
                            return new ByteArrayInputStream(imageData);
                        } catch (IOException e) {
                            TFCAutoIngots.getLogger().error("Failed to convert texture to bytes for {}: {}", metalName, e.getMessage());
                            return null;
                        }
                    };
                }
            }
        }

        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !Constants.TFC_NAMESPACE.equals(namespace)) {
            return;
        }

        String textureDir = Constants.TEXTURE_PATH_PREFIX.substring(0, Constants.TEXTURE_PATH_PREFIX.length() - 1);
        if (path.equals(textureDir)) {
            for (String metalName : TEXTURE_CACHE.keySet()) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    Constants.TFC_NAMESPACE,
                    Constants.TEXTURE_PATH_PREFIX + metalName + ".png"
                );
                IoSupplier<InputStream> supplier = getResource(type, location);
                if (supplier != null) {
                    output.accept(location, supplier);
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type == PackType.CLIENT_RESOURCES) {
            return Set.of(Constants.TFC_NAMESPACE);
        }
        return Set.of();
    }

    @Override
    public PackLocationInfo location() {
        return locationInfo;
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer.getMetadataSectionName().equals("pack")) {
            @SuppressWarnings("unchecked")
            T result = (T) new net.minecraft.server.packs.metadata.pack.PackMetadataSection(
                Component.literal("Auto-generated TFC ingot pile textures"),
                Constants.PACK_FORMAT
            );
            return result;
        }
        return null;
    }

    @Override
    public void close() {
        // Textures are managed externally, don't close them here
    }

    /**
     * Create a Pack.ResourcesSupplier for registration.
     */
    public static Pack.ResourcesSupplier createResourcesSupplier() {
        PackLocationInfo locationInfo = new PackLocationInfo(
            PACK_NAME,
            Component.literal(PACK_NAME),
            PackSource.BUILT_IN,
            Optional.empty()
        );
        
        return new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo location) {
                return new DynamicIngotTexturePack(locationInfo);
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new DynamicIngotTexturePack(locationInfo);
            }
        };
    }
}

