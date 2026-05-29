package ru.suppelemen.vibevisuals.util.font;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;

/**
 * AbstractTexture variant that creates the GPU texture with a full mipmap
 * pyramid and uploads every level. Required for clean text rendering when the
 * atlas is many times bigger than its on-screen footprint — plain bilinear
 * sampling at high downscale ratios undersamples (only 4 texels read per
 * fragment regardless of how much source falls under the fragment), producing
 * the "still pixelated" look users see.
 *
 * <p>With mipmaps + a {@code LINEAR_MIPMAP_LINEAR} sampler, the GPU picks the
 * two nearest pre-filtered levels and trilinearly interpolates them — i.e.
 * we get a proper area-averaged colour per fragment, which is what makes
 * text edges read as smooth at any size.
 */
public final class MipmappedAtlasTexture extends AbstractTexture {

    public MipmappedAtlasTexture(String label, NativeImage baseImage) {
        var device = RenderSystem.getDevice();
        int w = baseImage.getWidth();
        int h = baseImage.getHeight();
        int mipLevels = computeMipLevels(Math.max(w, h));

        // USAGE flags from GpuTexture: COPY_DST (0x2) | TEXTURE_BINDING (0x4) = 0x6.
        // Using literals to avoid depending on yarn-specific constant names.
        int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;
        this.glTexture = device.createTexture(
                () -> label,
                usage,
                TextureFormat.RGBA8,
                w, h, 1, mipLevels);
        this.glTextureView = device.createTextureView(this.glTexture);

        CommandEncoder enc = device.createCommandEncoder();
        // Upload base level (level 0) — image stays owned by caller.
        enc.writeToTexture(this.glTexture, baseImage);

        // Generate + upload each subsequent mip level via 2×2 box filter.
        // Arg layout (matches vanilla SpriteContents.upload):
        //   writeToTexture(tex, image, mipLevel, dstZ, dstX, dstY, width, height, srcX, srcY)
        NativeImage current = baseImage;
        boolean ownsCurrent = false;
        try {
            for (int level = 1; level < mipLevels; level++) {
                NativeImage next = downsampleHalf(current);
                enc.writeToTexture(this.glTexture, next,
                        level,
                        0,                                   // dstZ
                        0, 0,                                // dstX, dstY
                        next.getWidth(), next.getHeight(),   // width, height
                        0, 0);                               // srcX, srcY
                if (ownsCurrent) current.close();
                current = next;
                ownsCurrent = true;
            }
        } finally {
            if (ownsCurrent) current.close();
        }

        // Trilinear sampler — LINEAR min + LINEAR mag + LINEAR mip blend.
        this.sampler = RenderSystem.getSamplerCache().getRepeated(
                com.mojang.blaze3d.textures.FilterMode.LINEAR, true);
    }

    private static int computeMipLevels(int side) {
        int n = 1;
        while ((side >>= 1) > 0) n++;
        return n;
    }

    /** 2×2 box-average downsample; preserves premultiplied-style alpha math. */
    private static NativeImage downsampleHalf(NativeImage src) {
        int w = Math.max(1, src.getWidth() / 2);
        int h = Math.max(1, src.getHeight() / 2);
        NativeImage dst = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            int sy = y * 2;
            for (int x = 0; x < w; x++) {
                int sx = x * 2;
                int c00 = src.getColorArgb(sx,     sy);
                int c10 = src.getColorArgb(sx + 1, sy);
                int c01 = src.getColorArgb(sx,     sy + 1);
                int c11 = src.getColorArgb(sx + 1, sy + 1);
                dst.setColorArgb(x, y, averageRgba(c00, c10, c01, c11));
            }
        }
        return dst;
    }

    private static int averageRgba(int a, int b, int c, int d) {
        // NativeImage in RGBA format stores as ABGR little-endian per pixel
        // (per the bake code in SmoothFontAtlas). We just average all 4 channels
        // in their natural byte positions — encoding doesn't matter for averaging.
        int r = (((a >>> 0)  & 0xFF) + ((b >>> 0)  & 0xFF) + ((c >>> 0)  & 0xFF) + ((d >>> 0)  & 0xFF)) >>> 2;
        int g = (((a >>> 8)  & 0xFF) + ((b >>> 8)  & 0xFF) + ((c >>> 8)  & 0xFF) + ((d >>> 8)  & 0xFF)) >>> 2;
        int bl= (((a >>> 16) & 0xFF) + ((b >>> 16) & 0xFF) + ((c >>> 16) & 0xFF) + ((d >>> 16) & 0xFF)) >>> 2;
        int al= (((a >>> 24) & 0xFF) + ((b >>> 24) & 0xFF) + ((c >>> 24) & 0xFF) + ((d >>> 24) & 0xFF)) >>> 2;
        return (al << 24) | (bl << 16) | (g << 8) | r;
    }
}
