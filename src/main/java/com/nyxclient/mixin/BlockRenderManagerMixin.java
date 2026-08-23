package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.XrayModule;
import com.nyxclient.render.XrayVertexConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * BlockRenderManager#renderBlock is invoked per-block while a chunk section's mesh is being built.
 * With Xray active nothing is cancelled - every block still renders. Ore is forced to draw all its
 * faces regardless of neighbor solidity (cull=false), so it stays visible no matter how deep it's
 * buried - the same real, per-block geometry cost as before. Every other block keeps the real `cull`
 * value untouched: normal neighbor-based culling still applies, so only the same outer "shell" faces
 * vanilla would already draw get rendered - just wrapped in XrayVertexConsumer, whose reduced alpha
 * reads as see-through once RenderLayersMixin has routed that shell into the translucent layer (alpha
 * is meaningless on the normal solid/cutout layers, which don't blend).
 *
 * An earlier version forced cull=false for every block, not just ore, to try to see through arbitrarily
 * many stacked layers of terrain at once. That backfired two ways: it multiplied the mesh's face count by
 * however many blocks deep the world extends (heavy overdraw - visible as a stall while chunks rebuilt),
 * and alpha-blending dozens of ~16%-opacity faces directly behind each other along the same view ray
 * compounds back toward full opacity, so deep terrain visually looked solid again anyway. Ore doesn't
 * have this problem because there's only ever one ore block along a given line of sight at a time - its
 * single forced-visible face isn't stacked behind others the way plain terrain would be.
 */
@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {

	@Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockModelRenderer;render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V"))
	private void nyxclient$xrayRender(BlockModelRenderer renderer, BlockRenderView world, BakedModel model, BlockState state,
	                                    BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull,
	                                    Random random, long seed, int overlay) {
		if (XrayModule.active && !state.isAir()) {
			if (XrayModule.shouldRender(state)) {
				renderer.render(world, model, state, pos, matrices, vertexConsumer, false, random, seed, overlay);
			} else {
				renderer.render(world, model, state, pos, matrices, new XrayVertexConsumer(vertexConsumer), cull, random, seed, overlay);
			}
			return;
		}
		renderer.render(world, model, state, pos, matrices, vertexConsumer, cull, random, seed, overlay);
	}
}
