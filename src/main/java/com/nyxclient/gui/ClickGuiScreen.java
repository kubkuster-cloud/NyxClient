package com.nyxclient.gui;

import com.nyxclient.NyxClient;
import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {

	private static final int MIN_WIDTH = 58;
	private static final int PADDING_X = 5;
	private static final int HEADER_HEIGHT = 15;
	private static final int ROW_HEIGHT = 13;
	private static final int ARROW_ZONE = 12;

	private static final int PANEL_BG = 0x38808080;
	private static final int HEADER_BG = 0xF0000000;
	private static final int TEXT_HEADER = 0xFFA5A5AC;
	private static final int TEXT_ENABLED = 0xFFF5F5F5;
	private static final int TEXT_DISABLED = 0xFF9A9AA0;
	private static final int ROW_HOVER = 0x26FFFFFF;
	private static final int ARROW_COLOR = 0xFF8A8A90;

	// Static so drag/collapse state survives closing and reopening the GUI within the same game session.
	private static final Map<Category, Panel> PANELS = new EnumMap<>(Category.class);

	private final List<Panel> panels = new ArrayList<>();
	private Panel dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	public ClickGuiScreen() {
		super(Text.literal("Nyx Client"));
	}

	@Override
	protected void init() {
		panels.clear();

		Category[] categories = Category.values();
		List<Panel> ordered = new ArrayList<>();
		boolean anyNew = false;

		for (Category category : categories) {
			boolean isNew = !PANELS.containsKey(category);
			anyNew |= isNew;
			Panel panel = PANELS.computeIfAbsent(category, c -> new Panel(c, 10, 8));
			panel.modules = NyxClient.moduleManager.getModules(category);
			panel.fitWidth();
			ordered.add(panel);
		}

		// Only lay panels out on their first creation - once the player has dragged one, later
		// re-opens of the GUI must leave it where they put it, not snap it back to the grid.
		if (anyNew) {
			layoutGrid(ordered);
		}

		panels.addAll(ordered);
	}

	private void layoutGrid(List<Panel> ordered) {
		int topRowHeight = 0;
		for (int i = 0; i < ordered.size(); i += 2) {
			topRowHeight = Math.max(topRowHeight, ordered.get(i).height());
		}
		int secondRowY = 8 + topRowHeight + 6;

		for (int i = 0; i < ordered.size(); i++) {
			Panel panel = ordered.get(i);
			int col = i % 2;
			int row = i / 2;
			panel.x = col == 0 ? 10 : ordered.get(i - 1).x + ordered.get(i - 1).width + 6;
			panel.y = row == 0 ? 8 : secondRowY;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		for (Panel panel : panels) {
			panel.render(context, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (int i = panels.size() - 1; i >= 0; i--) {
				Panel panel = panels.get(i);
				if (panel.isOverArrow(mouseX, mouseY)) {
					panel.collapsed = !panel.collapsed;
					return true;
				}
				if (panel.isOverHeader(mouseX, mouseY)) {
					dragging = panel;
					dragOffsetX = (int) mouseX - panel.x;
					dragOffsetY = (int) mouseY - panel.y;
					panels.remove(i);
					panels.add(panel);
					return true;
				}
				Module clicked = panel.moduleAt(mouseX, mouseY);
				if (clicked != null) {
					clicked.toggle();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging != null) {
			dragging.x = (int) mouseX - dragOffsetX;
			dragging.y = (int) mouseY - dragOffsetY;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private final class Panel {
		final Category category;
		List<Module> modules = List.of();
		int x;
		int y;
		int width = MIN_WIDTH;
		boolean collapsed;

		Panel(Category category, int x, int y) {
			this.category = category;
			this.x = x;
			this.y = y;
		}

		// Panels aren't a fixed width: a short list like "Killaura" alone shouldn't drag the header's
		// arrow far away from its name, so the box shrinks to fit whichever is widest - the category
		// name plus its collapse arrow, or the longest module name in the list.
		void fitWidth() {
			int headerWidth = PADDING_X + textRenderer.getWidth(category.name()) + 4 + ARROW_ZONE;
			int rowWidth = 0;
			for (Module module : modules) {
				rowWidth = Math.max(rowWidth, textRenderer.getWidth(module.getName()));
			}
			rowWidth += PADDING_X * 2;
			width = Math.max(MIN_WIDTH, Math.max(headerWidth, rowWidth));
		}

		int height() {
			if (collapsed) return HEADER_HEIGHT;
			return HEADER_HEIGHT + modules.size() * ROW_HEIGHT;
		}

		boolean isOverHeader(double mouseX, double mouseY) {
			return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
		}

		boolean isOverArrow(double mouseX, double mouseY) {
			return mouseX >= x + width - ARROW_ZONE && mouseX <= x + width
					&& mouseY >= y && mouseY <= y + HEADER_HEIGHT;
		}

		Module moduleAt(double mouseX, double mouseY) {
			if (collapsed) return null;
			if (mouseX < x || mouseX > x + width) return null;
			int rowY = y + HEADER_HEIGHT;
			for (Module module : modules) {
				if (mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT) {
					return module;
				}
				rowY += ROW_HEIGHT;
			}
			return null;
		}

		void render(DrawContext context, int mouseX, int mouseY) {
			int bodyHeight = height();

			if (!collapsed && !modules.isEmpty()) {
				context.fill(x, y, x + width, y + bodyHeight, PANEL_BG);
			}
			context.fill(x, y, x + width, y + HEADER_HEIGHT, HEADER_BG);

			context.drawTextWithShadow(textRenderer, Text.literal(category.name()), x + PADDING_X, y + (HEADER_HEIGHT - 8) / 2, TEXT_HEADER);
			context.drawTextWithShadow(textRenderer, Text.literal(collapsed ? "▶" : "▼"), x + width - 10, y + (HEADER_HEIGHT - 8) / 2, ARROW_COLOR);

			if (collapsed) return;

			int rowY = y + HEADER_HEIGHT;
			for (Module module : modules) {
				boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
				if (hovered) {
					context.fill(x, rowY, x + width, rowY + ROW_HEIGHT, ROW_HOVER);
				}

				int textColor = module.isEnabled() ? TEXT_ENABLED : TEXT_DISABLED;
				context.drawTextWithShadow(textRenderer, Text.literal(module.getName()), x + PADDING_X, rowY + (ROW_HEIGHT - 8) / 2, textColor);

				rowY += ROW_HEIGHT;
			}
		}
	}
}
