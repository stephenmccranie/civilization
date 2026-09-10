package dev.civilization.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Preserve the baked model's texture, lighting and tint while reducing vertex opacity. */
final class TranslucentVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float opacity;
    TranslucentVertexConsumer(VertexConsumer delegate, float opacity) {
        this.delegate = delegate;
        this.opacity = opacity;
    }
    @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, Math.round(a * opacity)); return this; }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
}
