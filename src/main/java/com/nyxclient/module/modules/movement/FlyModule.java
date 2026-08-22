package com.nyxclient.module.modules.movement;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;

public class FlyModule extends Module {

	private final DoubleSetting speed = addSetting(new DoubleSetting("Speed", 1.0, 0.1, 5.0));

	private boolean hadAllowFlying;
	private boolean hadFlying;

	public FlyModule() {
		super("Fly", "Creative-style flight. Only takes effect where the server accepts client-reported flight (creative/spectator, or a server that trusts the client).", Category.MOVEMENT);
	}

	@Override
	protected void onEnable() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		PlayerAbilities abilities = player.getAbilities();
		hadAllowFlying = abilities.allowFlying;
		hadFlying = abilities.flying;

		abilities.allowFlying = true;
		abilities.flying = true;
	}

	@Override
	protected void onDisable() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		PlayerAbilities abilities = player.getAbilities();
		abilities.flying = hadFlying && abilities.allowFlying;
		if (!player.isCreative() && !player.isSpectator()) {
			abilities.allowFlying = hadAllowFlying;
			abilities.flying = false;
		}
	}

	@Override
	public void onTick() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		PlayerAbilities abilities = player.getAbilities();
		abilities.allowFlying = true;
		abilities.flying = true;
		abilities.setFlySpeed((float) (0.05 * speed.get()));
	}
}
