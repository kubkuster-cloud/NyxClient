package com.nyxclient.render;

import net.minecraft.client.render.VertexConsumer;

/**
 * Delegates every call straight through except color(), where it clamps alpha so Xray's "everything but
 * ore" blocks read as see-through. The solid/cutout render layers ignore alpha entirely (no GL blending),
 * so this only has a visible effect on geometry that's also been redirected into the translucent layer
 * (see RenderLayersMixin) - the two changes only work together.
 */
public class XrayVertexConsumer implements VertexConsumer {

	private static final int ALPHA = 40;

	private final VertexConsumer delegate;

	public XrayVertexConsumer(VertexConsumer delegate) {
		this.delegate = delegate;
	}

	@Override
	public VertexConsumer vertex(float x, float y, float z) {
		delegate.vertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		delegate.color(red, green, blue, ALPHA);
		return this;
	}

	@Override
	public VertexConsumer texture(float u, float v) {
		delegate.texture(u, v);
		return this;
	}

	@Override
	public VertexConsumer overlay(int u, int v) {
		delegate.overlay(u, v);
		return this;
	}

	@Override
	public VertexConsumer light(int u, int v) {
		delegate.light(u, v);
		return this;
	}

	@Override
	public VertexConsumer normal(float x, float y, float z) {
		delegate.normal(x, y, z);
		return this;
	}
}
