package com.nyxclient.gui;

import com.nyxclient.NyxClient;
import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {

	private static final int COLUMN_WIDTH = 120;
	private static final int BUTTON_HEIGHT = 20;
	private static final int PADDING = 6;
	private static final int TOP = 30;

	public ClickGuiScreen() {
		super(Text.literal("Nyx Client"));
	}

	@Override
	protected void init() {
		Category[] categories = Category.values();
		int startX = 10;

		for (int col = 0; col < categories.length; col++) {
			Category category = categories[col];
			int x = startX + col * (COLUMN_WIDTH + PADDING);
			int y = TOP;

			for (Module module : NyxClient.moduleManager.getModules(category)) {
				addDrawableChild(makeButton(module, x, y));
				y += BUTTON_HEIGHT + 2;
			}
		}
	}

	private ButtonWidget makeButton(Module module, int x, int y) {
		return ButtonWidget.builder(buttonText(module), button -> {
			module.toggle();
			button.setMessage(buttonText(module));
		}).dimensions(x, y, COLUMN_WIDTH, BUTTON_HEIGHT).build();
	}

	private Text buttonText(Module module) {
		String state = module.isEnabled() ? "ON" : "OFF";
		return Text.literal(module.getName() + " [" + state + "]");
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawTextWithShadow(textRenderer, title, 10, 10, 0xFFFFFF);

		Category[] categories = Category.values();
		int startX = 10;
		for (int col = 0; col < categories.length; col++) {
			int x = startX + col * (COLUMN_WIDTH + PADDING);
			context.drawTextWithShadow(textRenderer, Text.literal(categories[col].name()), x, 20, 0xAAAAAA);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
