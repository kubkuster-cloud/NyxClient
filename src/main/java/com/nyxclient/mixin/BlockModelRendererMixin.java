package com.nyxclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.nyxclient.module.modules.render.XrayModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * render() picks the smooth (ambient occlusion) lighting path for ore blocks, since they're regular
 * opaque full cubes with AO enabled by default. That path shades each face from the light level of the
 * neighboring cells around it (BlockModelRenderer$AmbientOcclusionCalculator samples each neighbor's own
 * BlockState/BlockPos) rather than from the rendered block's own position. WorldRendererMixin's full-bright
 * override only fires when the *queried* BlockState is an ore, so on the smooth path it never triggers -
 * the neighbors being sampled are the (still dark, un-mined) stone around the ore, not the ore itself, and
 * the ore renders pitch black despite its faces correctly drawing.
 *
 * Forcing useAmbientOcclusion() to false for ore blocks while Xray is active routes them through
 * renderFlat() instead, whose single lightmap lookup is keyed on the block's own BlockState/BlockPos -
 * exactly what WorldRendererMixin overrides to full bright.
 */
@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {

	@ModifyExpressionValue(
			method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/BakedModel;useAmbientOcclusion()Z")
	)
	private boolean nyxclient$forceFlatForOres(boolean original, @Local(argsOnly = true) BlockState state) {
		if (XrayModule.active && XrayModule.shouldRender(state)) {
			return false;
		}
		return original;
	}
}
