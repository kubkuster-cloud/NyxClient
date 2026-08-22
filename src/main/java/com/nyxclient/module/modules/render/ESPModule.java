package com.nyxclient.module.modules.render;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.BoolSetting;
import com.nyxclient.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Uses vanilla's glow-outline render pass (the same one used by the Glowing status effect / spectator glow)
 * to highlight entities through walls, instead of hand-rolling GL line drawing. That pass already ignores
 * depth testing, so this needs no mixin or custom RenderLayer.
 */
public class ESPModule extends Module {

	private final DoubleSetting range = addSetting(new DoubleSetting("Range", 64.0, 8.0, 256.0));
	private final BoolSetting players = addSetting(new BoolSetting("Players", true));
	private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", true));

	private final Set<Entity> glowing = new HashSet<>();

	public ESPModule() {
		super("ESP", "Highlights nearby entities through walls via the vanilla glow outline effect (client-side only).", Category.RENDER);
	}

	@Override
	protected void onDisable() {
		for (Entity entity : glowing) {
			entity.setGlowing(false);
		}
		glowing.clear();
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) return;

		Set<Entity> stillTracked = new HashSet<>();
		double r = range.get();

		for (Entity entity : client.world.getEntities()) {
			if (entity == client.player) continue;
			if (!(entity instanceof LivingEntity)) continue;

			boolean wanted = (entity instanceof PlayerEntity) ? players.get() : mobs.get();
			if (wanted && client.player.squaredDistanceTo(entity) <= r * r) {
				entity.setGlowing(true);
				stillTracked.add(entity);
			}
		}

		for (Entity entity : glowing) {
			if (!stillTracked.contains(entity)) {
				entity.setGlowing(false);
			}
		}

		glowing.clear();
		glowing.addAll(stillTracked);
	}
}
