package com.nyxclient.module.modules.combat;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoArmorModule extends Module {

	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	// The player screen handler numbers the four armor slots 5-8, top-down (helmet first).
	private static final int FIRST_ARMOR_SCREEN_SLOT = 5;

	public AutoArmorModule() {
		super("AutoArmor", "Equips the best armor you're carrying, swapping in any upgrade it finds in your inventory.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.interactionManager == null) return;
		// Moving a stack means picking it up on the cursor first, which fights with whatever the
		// player is doing by hand while a container or the inventory is open.
		if (client.currentScreen != null) return;

		PlayerInventory inventory = player.getInventory();

		for (int slotIndex = 0; slotIndex < ARMOR_SLOTS.length; slotIndex++) {
			EquipmentSlot slot = ARMOR_SLOTS[slotIndex];
			double equippedArmor = armorValue(player.getEquippedStack(slot), slot);

			int bestIndex = -1;
			double bestArmor = equippedArmor;

			for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
				ItemStack stack = inventory.main.get(i);
				if (slotFor(stack) != slot) continue;

				double armor = armorValue(stack, slot);
				if (armor > bestArmor) {
					bestArmor = armor;
					bestIndex = i;
				}
			}

			if (bestIndex < 0) continue;

			// SWAP with the hotbar as the target only reaches slots 0-8; for anything deeper in the
			// bag, pick it up and drop it onto the armor slot instead (two PICKUPs, like a real drag).
			int armorScreenSlot = FIRST_ARMOR_SCREEN_SLOT + slotIndex;
			if (bestIndex < 9) {
				click(client, player, armorScreenSlot, bestIndex, SlotActionType.SWAP);
			} else {
				click(client, player, bestIndex, 0, SlotActionType.PICKUP);
				click(client, player, armorScreenSlot, 0, SlotActionType.PICKUP);
				// Whatever came off the body is now on the cursor - put it back where the upgrade was.
				click(client, player, bestIndex, 0, SlotActionType.PICKUP);
			}
			return; // one swap per tick keeps the click sequence in step with the server
		}
	}

	private void click(MinecraftClient client, ClientPlayerEntity player, int slot, int button, SlotActionType action) {
		client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, button, action, player);
	}

	private EquipmentSlot slotFor(ItemStack stack) {
		if (stack.isEmpty()) return null;
		EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		return equippable == null ? null : equippable.slot();
	}

	private double armorValue(ItemStack stack, EquipmentSlot slot) {
		if (stack.isEmpty()) return 0;

		AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
		if (modifiers == null) return 0;

		double[] total = {0};
		modifiers.applyModifiers(slot, (attribute, modifier) -> {
			if (attribute == EntityAttributes.ARMOR || attribute == EntityAttributes.ARMOR_TOUGHNESS) {
				total[0] += modifier.value();
			}
		});
		return total[0];
	}
}
