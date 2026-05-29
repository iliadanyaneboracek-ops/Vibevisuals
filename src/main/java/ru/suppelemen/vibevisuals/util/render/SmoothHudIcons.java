package ru.suppelemen.vibevisuals.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.util.font.MipmappedAtlasTexture;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for HUD icon PNGs that render with full mipmap pyramids and
 * {@code LINEAR_MIPMAP_LINEAR} sampling — i.e. they stay crisp at any
 * on-screen size, instead of the pixelated look MC's default
 * NEAREST-sampled GUI atlas gives at small sizes.
 *
 * <p>Usage:
 * <pre>
 *   SmoothHudIcons.ensureRegistered();
 *   ctx.drawTexture(..., SmoothHudIcons.id("potion_frame"), ...);
 * </pre>
 *
 * <p>To add a new smooth icon: drop the PNG into
 * {@code src/main/resources/assets/vibevisuals/textures/gui/<name>.png}
 * and append the name to {@link #ICON_NAMES}.
 */
public final class SmoothHudIcons {

    private static final String[] ICON_NAMES = {
            "potion_frame",
            "potion_liquid",
    };

    private static final Map<String, Identifier> IDS = new HashMap<>();
    private static boolean attempted;

    private SmoothHudIcons() {}

    /**
     * Loads each icon PNG from {@code resources/assets/vibevisuals/textures/gui/}
     * once, wraps it in a {@link MipmappedAtlasTexture} so it gets a full
     * mip-pyramid + LINEAR_MIPMAP_LINEAR sampler, and registers it under a
     * stable Identifier reachable via {@link #id(String)}.
     */
    public static synchronized void ensureRegistered() {
        if (attempted) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getTextureManager() == null) return;
        attempted = true;

        for (String name : ICON_NAMES) {
            String path = "/assets/" + VibeVisualsClient.MOD_ID + "/textures/gui/" + name + ".png";
            try (InputStream in = SmoothHudIcons.class.getResourceAsStream(path)) {
                if (in == null) {
                    System.err.println("[vibevisuals] SmoothHudIcons: missing " + path);
                    continue;
                }
                NativeImage img = NativeImage.read(in);
                Identifier id = Identifier.of(VibeVisualsClient.MOD_ID, "smoothicons/" + name);
                MipmappedAtlasTexture tex = new MipmappedAtlasTexture(id.toString(), img);
                mc.getTextureManager().registerTexture(id, tex);
                IDS.put(name, id);
                System.out.println("[vibevisuals] SmoothHudIcons: registered " + id
                        + " (" + img.getWidth() + "×" + img.getHeight() + ", mipmapped)");
            } catch (Throwable t) {
                System.err.println("[vibevisuals] SmoothHudIcons: failed to load " + name);
                t.printStackTrace();
            }
        }
    }

    /** Smooth-sampled Identifier for the given icon name, or {@code null} if not registered. */
    public static Identifier id(String name) {
        ensureRegistered();
        return IDS.get(name);
    }
}
