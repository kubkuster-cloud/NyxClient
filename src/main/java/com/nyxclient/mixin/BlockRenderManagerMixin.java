package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.XrayModule;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BlockRenderManager#renderBlock is invoked per-block while a chunk section's mesh is being built.
 * Cancelling it for non-ore blocks while Xray is active removes them from the built mesh entirely,
 * leaving ores visible through the (now unrendered) stone around them.
 *
 * That alone isn't enough though: face culling during mesh building is computed from the real
 * (unmodified) neighboring BlockState, not from whether we chose to render it. An ore fully embedded
 * in stone has every face touching still-"solid" stone, so without the redirect below every one of
 * its faces gets culled away and it renders as nothing at all - the opposite of visible-through-walls.
 * Forcing cull=false on the delegate call for ore blocks makes all their faces draw regardless of
 * neighbor solidity.
 */
@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {

	@Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
	private void nyxclient$xray(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices,
	                             VertexConsumer vertexConsumer, boolean cull, Random random, CallbackInfo ci) {
		if (XrayModule.active && !state.isAir() && !XrayModule.shouldRender(state)) {
			ci.cancel();
		}
	}

	@Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockModelRenderer;render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V"))
	private void nyxclient$uncullOres(BlockModelRenderer renderer, BlockRenderView world, BakedModel model, BlockState state,
	                                    BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull,
	                                    Random random, long seed, int overlay) {
		boolean actualCull = (XrayModule.active && XrayModule.shouldRender(state)) ? false : cull;
		renderer.render(world, model, state, pos, matrices, vertexConsumer, actualCull, random, seed, overlay);
	}
}
