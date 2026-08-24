package com.nyxclient.module.modules.combat;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotemModule extends Module {

	// SWAP with button 40 is the same client-side action vanilla's "swap to offhand" keybind (F)
	// performs on whichever slot is hovered - button 40 signals "offhand" as the swap target.
	private static final int OFFHAND_SWAP_BUTTON = 40;

	public AutoTotemModule() {
		super("AutoTotem", "Keeps a Totem of Undying in your offhand, refilling it from your inventory whenever it runs out.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.interactionManager == null) return;
		if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
			ItemStack stack = inventory.main.get(i);
			if (stack.getItem() != Items.TOTEM_OF_UNDYING) continue;

			client.interactionManager.clickSlot(player.playerScreenHandler.syncId, toScreenSlot(i), OFFHAND_SWAP_BUTTON, SlotActionType.SWAP, player);
			return;
		}
	}

	// PlayerInventory.main packs the hotbar into indices 0-8, but the player screen handler numbers
	// them 36-44 (0-8 there are the crafting/armor slots); storage slots 9-35 line up directly.
	private int toScreenSlot(int inventoryIndex) {
		return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
	}
}
