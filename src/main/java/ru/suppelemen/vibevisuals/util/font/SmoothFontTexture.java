package ru.suppelemen.vibevisuals.util.font;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holds the single instance of the smooth-text atlas + its GPU texture
 * registration. Loaded lazily on first access; safe to call from main thread.
 *
 * <p>The texture is registered under {@link #TEXTURE_ID} so any RenderPipeline
 * can sample it via {@code context.drawTexture(pipeline, TEXTURE_ID, ...)}.
 *
 * <p>{@link #tryForceLinearSampler(NativeImageBackedTexture)} attempts to
 * swap the texture's NEAREST sampler for a LINEAR one via reflection.  In
 * MC 1.21.11 the GPU abstraction stores the sampler in
 * {@code AbstractTexture.sampler} (field_63613).  If that fails we still have
 * a working bitmap atlas, just with nearest sampling — same quality as
 * vanilla, no regression.
 */
public final class SmoothFontTexture {

    public static final Identifier TEXTURE_ID =
            Identifier.of(VibeVisualsClient.MOD_ID, "smoothfont/atlas");
    public static final Identifier TEXTURE_ID_BOLD =
            Identifier.of(VibeVisualsClient.MOD_ID, "smoothfont/atlas_bold");

    private static SmoothFontAtlas atlas;
    private static SmoothFontAtlas atlasBold;
    private static net.minecraft.client.texture.AbstractTexture texture;
    private static net.minecraft.client.texture.AbstractTexture textureBold;
    private static boolean attempted;
    private static String diagnosticMessage;

    private SmoothFontTexture() {}

    public static SmoothFontAtlas atlas() {
        ensureInitialised();
        return atlas;
    }

    public static SmoothFontAtlas atlasBold() {
        ensureInitialised();
        return atlasBold;
    }

    /**
     * Bake the atlas immediately (safe at mod init — uses classloader to load
     * the TTF, no ResourceManager dependency). GPU texture registration is
     * deferred to {@link #ensureGpuReady()} because TextureManager isn't ready
     * until the first frame is being rendered.
     */
    public static synchronized void ensureInitialised() {
        if (attempted) return;
        attempted = true;
        try {
            // ---- Regular atlas ----
            try (InputStream ttf = SmoothFontTexture.class.getResourceAsStream(
                    "/assets/" + VibeVisualsClient.MOD_ID + "/font/inter.ttf")) {
                if (ttf == null) {
                    System.err.println("[vibevisuals] inter.ttf not found in resources for smooth-font baking");
                    return;
                }
                atlas = SmoothFontAtlas.bake(ttf);
            }

            // ---- Bold/SemiBold atlas ----
            // Prefer a real cut at inter-semibold.ttf; fall back to AWT-synthesised
            // bold on the regular inter.ttf (cheap weighting, looks ~SemiBold-ish).
            try (InputStream sbTtf = SmoothFontTexture.class.getResourceAsStream(
                    "/assets/" + VibeVisualsClient.MOD_ID + "/font/inter-semibold.ttf")) {
                if (sbTtf != null) {
                    atlasBold = SmoothFontAtlas.bake(sbTtf);
                    System.out.println("[vibevisuals] bold atlas baked from inter-semibold.ttf");
                } else {
                    try (InputStream regTtf = SmoothFontTexture.class.getResourceAsStream(
                            "/assets/" + VibeVisualsClient.MOD_ID + "/font/inter.ttf")) {
                        atlasBold = SmoothFontAtlas.bake(regTtf, true);
                        System.out.println("[vibevisuals] bold atlas baked (AWT-synthesised from inter.ttf)");
                    }
                }
            }

            // Dump atlases to disk for visual verification.
            try {
                Path baseDir = FabricLoader.getInstance().getConfigDir().resolve("vibevisuals");
                Files.createDirectories(baseDir);
                atlas.atlas().writeTo(baseDir.resolve("smoothfont-atlas-debug.png"));
                if (atlasBold != null) {
                    atlasBold.atlas().writeTo(baseDir.resolve("smoothfont-atlas-bold-debug.png"));
                }
                diagnosticMessage = "[vibevisuals] smooth font atlases baked → " + baseDir;
            } catch (Exception ignored) {
            }
            System.out.println(diagnosticMessage != null
                    ? diagnosticMessage
                    : "[vibevisuals] smooth font atlases baked (atlas dump skipped)");
        } catch (IOException e) {
            System.err.println("[vibevisuals] Failed to bake smooth font atlas");
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("[vibevisuals] Unexpected error in SmoothFontTexture init");
            t.printStackTrace();
        }
    }

    /**
     * Register the GPU texture once MC has booted enough for TextureManager
     * to exist. Idempotent: safe to call every frame.
     */
    public static synchronized void ensureGpuReady() {
        if (atlas == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getTextureManager() == null) return;
        try {
            if (texture == null) {
                // MipmappedAtlasTexture: GPU texture with full mip pyramid +
                // LINEAR_MIPMAP_LINEAR sampler. Critical for clean downscale: plain
                // bilinear on a 4096 atlas drawn at ~8 px produces undersampling
                // artefacts that look identical to NEAREST.
                texture = new MipmappedAtlasTexture(TEXTURE_ID.toString(), atlas.atlas());
                mc.getTextureManager().registerTexture(TEXTURE_ID, texture);
                System.out.println("[vibevisuals] smooth font GPU texture registered as " + TEXTURE_ID
                        + " (mipmapped, trilinear)");
            }
            if (textureBold == null && atlasBold != null) {
                textureBold = new MipmappedAtlasTexture(TEXTURE_ID_BOLD.toString(), atlasBold.atlas());
                mc.getTextureManager().registerTexture(TEXTURE_ID_BOLD, textureBold);
                System.out.println("[vibevisuals] bold smooth font GPU texture registered as " + TEXTURE_ID_BOLD);
            }
        } catch (Throwable t) {
            System.err.println("[vibevisuals] Failed to register smooth-font GPU texture");
            t.printStackTrace();
        }
    }

    /**
     * Build a LINEAR-min/LINEAR-mag sampler via {@code GpuDevice.createSampler}
     * and overwrite {@code AbstractTexture.sampler} on our atlas texture.
     *
     * <p>In MC 1.21.11 the GPU abstraction is:
     * <pre>
     * RenderSystem.getDevice().createSampler(
     *   AddressMode u, AddressMode v,
     *   FilterMode min, FilterMode mag,
     *   int maxAnisotropy, OptionalDouble maxLod)
     * </pre>
     * which returns a {@code net.minecraft.client.gl.GpuSampler}. We then set
     * {@code AbstractTexture.sampler} so any draw call using this texture
     * binds our LINEAR sampler instead of the vanilla NEAREST one.
     */
    private static void tryForceLinearSampler(NativeImageBackedTexture tex) {
        try {
            Class<?> renderSystemClass = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            Class<?> filterModeClass = Class.forName("com.mojang.blaze3d.textures.FilterMode");
            Object linearMode = enumConst(filterModeClass, "LINEAR");
            if (linearMode == null) {
                System.out.println("[vibevisuals] LINEAR sampler hook: FilterMode.LINEAR missing");
                return;
            }

            // Use the exact same code path as vanilla NativeImageBackedTexture
            // but pass LINEAR instead of NEAREST. This returns a cached, shared
            // sampler object that MC's render pipeline definitely accepts.
            Object samplerCache = renderSystemClass.getMethod("getSamplerCache").invoke(null);
            Object linearSampler = samplerCache.getClass()
                    .getMethod("getRepeated", filterModeClass)
                    .invoke(samplerCache, linearMode);

            Field samplerField = findSamplerField(tex.getClass());
            if (samplerField == null) {
                System.out.println("[vibevisuals] LINEAR sampler hook: AbstractTexture.sampler field not found");
                return;
            }
            samplerField.setAccessible(true);
            Object before = samplerField.get(tex);
            samplerField.set(tex, linearSampler);
            Object after = samplerField.get(tex);

            // Verify by querying the filter mode through the GpuSampler getter.
            Object minFilter = after.getClass().getMethod("getMinFilterMode").invoke(after);
            Object magFilter = after.getClass().getMethod("getMagFilterMode").invoke(after);

            System.out.println("[vibevisuals] LINEAR sampler hook:"
                    + " before=" + (before == null ? "null" : before.getClass().getSimpleName())
                    + " after=" + after.getClass().getSimpleName()
                    + " min=" + minFilter + " mag=" + magFilter
                    + " same-instance=" + (before == after));
        } catch (Throwable t) {
            System.err.println("[vibevisuals] LINEAR sampler hook failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static Field findSamplerField(Class<?> start) {
        Class<?> c = start;
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType().getSimpleName().equals("GpuSampler")) return f;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /** No-op now — {@link MipmappedAtlasTexture} sets the sampler in its
     *  constructor and nothing in MC resets it for our standalone texture. */
    public static synchronized void reapplyLinearSampler() {}

    private static Object enumConst(Class<?> enumClass, String name) {
        for (Object c : enumClass.getEnumConstants()) {
            if (((Enum<?>) c).name().equals(name)) return c;
        }
        return null;
    }
}
