package com.nyxclient.gui;

import com.nyxclient.NyxClient;
import com.nyxclient.config.ConfigManager;
import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.BoolSetting;
import com.nyxclient.setting.DoubleSetting;
import com.nyxclient.setting.IntSetting;
import com.nyxclient.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ClickGuiScreen extends Screen {

	private static final int MIN_WIDTH = 58;
	private static final int PADDING_X = 5;
	private static final int HEADER_HEIGHT = 15;
	private static final int ROW_HEIGHT = 13;
	private static final int SETTING_ROW_HEIGHT = 12;
	private static final int SETTING_INDENT = 9;
	private static final int ARROW_ZONE = 12;
	private static final int RADIUS = 3;
	private static final int CHECKBOX_SIZE = 6;
	private static final int TRACK_HEIGHT = 2;
	private static final int BIND_GAP = 6;
	private static final int SETTINGS_DOT_ZONE = 6;
	private static final String LISTENING_LABEL = "...";

	// OpenSans (SIL OFL license, see licenses/OFL-OpenSans.txt) in place of Minecraft's bitmap
	// default font. Each font.json falls back to minecraft:default for any glyph it doesn't cover.
	private static final Identifier FONT_REGULAR = Identifier.of("nyxclient", "sans");
	private static final Identifier FONT_HEADER = Identifier.of("nyxclient", "sans_semibold");

	private static final int PANEL_BG = 0x38808080;
	private static final int HEADER_BG = 0xF0000000;
	private static final int TEXT_HEADER = 0xFFA5A5AC;
	private static final int TEXT_ENABLED = 0xFFF5F5F5;
	private static final int TEXT_DISABLED = 0xFF9A9AA0;
	private static final int TEXT_SETTING = 0xFFC2C2C8;
	private static final int TEXT_SETTING_VALUE = 0xFF8A8A90;
	private static final int ROW_HOVER = 0x26FFFFFF;
	private static final int SETTINGS_BG = 0x30000000;
	private static final int ARROW_COLOR = 0xFF8A8A90;
	private static final int TRACK_BG = 0x50FFFFFF;
	private static final int TRACK_FILL = 0xFFE8E8EE;
	private static final int CHECKBOX_OFF = 0x50FFFFFF;
	private static final int CHECKBOX_ON = 0xFFE8E8EE;
	private static final int TEXT_BIND = 0xFF8A8A90;
	private static final int TEXT_BIND_LISTENING = 0xFFF5F5F5;
	private static final int TEXT_BIND_CONFLICT = 0xFFE0A64B;

	// Static so drag/collapse/expand state survives closing and reopening the GUI within the same session.
	private static final Map<Category, Panel> PANELS = new EnumMap<>(Category.class);
	private static final Set<Module> EXPANDED = new HashSet<>();

	private final List<Panel> panels = new ArrayList<>();
	private Panel dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	// The module waiting for the next key press to become its bind, or null when nothing is listening.
	private Module binding;

	private Setting<?> draggingSlider;
	private int sliderTrackX1;
	private int sliderTrackX2;

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
		// A click while listening means "never mind" - swallowed rather than passed on, so aiming at
		// a module row to cancel doesn't also toggle it.
		if (binding != null) {
			stopBinding();
			return true;
		}

		for (int i = panels.size() - 1; i >= 0; i--) {
			Panel panel = panels.get(i);

			if (button == 0 && panel.isOverArrow(mouseX, mouseY)) {
				panel.collapsed = !panel.collapsed;
				return true;
			}
			if (button == 0 && panel.isOverHeader(mouseX, mouseY)) {
				dragging = panel;
				dragOffsetX = (int) mouseX - panel.x;
				dragOffsetY = (int) mouseY - panel.y;
				panels.remove(i);
				panels.add(panel);
				return true;
			}

			RowHit hit = panel.rowAt(mouseX, mouseY);
			if (hit == null) continue;

			if (hit.module != null) {
				// Left toggles the module, right opens its settings - so a module with settings is
				// still one click to turn on, same as one without. Middle starts listening for a
				// bind, the one button neither of those already claims.
				if (button == 0) {
					hit.module.toggle();
				} else if (button == 1 && !hit.module.getSettings().isEmpty()) {
					if (!EXPANDED.remove(hit.module)) {
						EXPANDED.add(hit.module);
					}
					panel.fitWidth();
				} else if (button == 2) {
					binding = hit.module;
					panel.fitWidth();
				}
				return true;
			}

			if (button == 0) {
				if (hit.setting instanceof BoolSetting bool) {
					bool.toggle();
				} else {
					draggingSlider = hit.setting;
					sliderTrackX1 = hit.trackX1;
					sliderTrackX2 = hit.trackX2;
					applySlider(mouseX);
				}
			}
			return true;
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
		if (draggingSlider != null) {
			applySlider(mouseX);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = null;
		draggingSlider = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void applySlider(double mouseX) {
		int trackWidth = sliderTrackX2 - sliderTrackX1;
		if (trackWidth <= 0) return;
		double fraction = Math.max(0, Math.min(1, (mouseX - sliderTrackX1) / trackWidth));

		if (draggingSlider instanceof DoubleSetting d) {
			d.set(d.getMin() + fraction * (d.getMax() - d.getMin()));
		} else if (draggingSlider instanceof IntSetting n) {
			n.set((int) Math.round(n.getMin() + fraction * (n.getMax() - n.getMin())));
		}
	}

	private static double fractionOf(Setting<?> setting) {
		if (setting instanceof DoubleSetting d) {
			double span = d.getMax() - d.getMin();
			return span <= 0 ? 0 : (d.get() - d.getMin()) / span;
		}
		if (setting instanceof IntSetting n) {
			double span = n.getMax() - n.getMin();
			return span <= 0 ? 0 : (n.get() - n.getMin()) / span;
		}
		return 0;
	}

	private static String valueLabel(Setting<?> setting) {
		if (setting instanceof DoubleSetting d) {
			return String.format("%.2f", d.get());
		}
		return String.valueOf(setting.get());
	}

	/**
	 * While a module is listening, every key belongs to it - including Escape, which would otherwise
	 * close the screen out from under the bind that was being set. Escape leaves the existing bind
	 * alone; Delete and Backspace clear it; anything else becomes the new bind.
	 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (binding == null) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
			boolean clear = keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE;
			binding.setKeyCode(clear ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
			// Binds are set rarely and deliberately, so persist on the spot rather than trusting the
			// client to shut down cleanly enough for the save on CLIENT_STOPPING to run.
			ConfigManager.save();
		}

		stopBinding();
		return true;
	}

	private void stopBinding() {
		binding = null;
		for (Panel panel : panels) {
			panel.fitWidth();
		}
	}

	/** Right-hand label on a module row: the key it toggles on, or nothing when it has no bind. */
	private String bindLabel(Module module) {
		if (module == binding) {
			return LISTENING_LABEL;
		}
		int keyCode = module.getKeyCode();
		if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
			return "";
		}
		return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString().toUpperCase(Locale.ROOT);
	}

	private int bindColor(Module module) {
		if (module == binding) {
			return TEXT_BIND_LISTENING;
		}
		// A module sharing the GUI's own key toggles on the very press that opens the GUI. That is
		// allowed - it is the player's key to spend - but it is worth being able to see.
		return module.getKeyCode() == NyxClient.getOpenGuiKeyCode() ? TEXT_BIND_CONFLICT : TEXT_BIND;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static Text styled(String text, Identifier font) {
		return Text.literal(text).setStyle(Style.EMPTY.withFont(font));
	}

	/** What sits under the cursor: either a module row, or one setting row plus its slider track bounds. */
	private static final class RowHit {
		final Module module;
		final Setting<?> setting;
		final int trackX1;
		final int trackX2;

		private RowHit(Module module, Setting<?> setting, int trackX1, int trackX2) {
			this.module = module;
			this.setting = setting;
			this.trackX1 = trackX1;
			this.trackX2 = trackX2;
		}

		static RowHit ofModule(Module module) {
			return new RowHit(module, null, 0, 0);
		}

		static RowHit ofSetting(Setting<?> setting, int trackX1, int trackX2) {
			return new RowHit(null, setting, trackX1, trackX2);
		}
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
		// name plus its collapse arrow, the longest module name, or an expanded setting's name+value.
		void fitWidth() {
			int widest = PADDING_X + textRenderer.getWidth(styled(category.name(), FONT_HEADER)) + 4 + ARROW_ZONE;

			for (Module module : modules) {
				int rowWidth = PADDING_X + textRenderer.getWidth(styled(module.getName(), FONT_REGULAR))
						+ bindWidth(module) + rightReserve(module) + PADDING_X;
				widest = Math.max(widest, rowWidth);

				if (!EXPANDED.contains(module)) continue;
				for (Setting<?> setting : module.getSettings()) {
					int nameWidth = textRenderer.getWidth(styled(setting.getName(), FONT_REGULAR));
					int valueWidth = textRenderer.getWidth(styled(valueLabel(setting), FONT_REGULAR));
					widest = Math.max(widest, SETTING_INDENT + nameWidth + 8 + valueWidth + PADDING_X);
				}
			}

			width = Math.max(MIN_WIDTH, widest);
		}

		private int bindWidth(Module module) {
			String label = bindLabel(module);
			return label.isEmpty() ? 0 : BIND_GAP + textRenderer.getWidth(styled(label, FONT_REGULAR));
		}

		private int rightReserve(Module module) {
			return module.getSettings().isEmpty() ? 0 : SETTINGS_DOT_ZONE;
		}

		int height() {
			if (collapsed) return HEADER_HEIGHT;

			int h = HEADER_HEIGHT;
			for (Module module : modules) {
				h += ROW_HEIGHT + settingsHeight(module);
			}
			return h;
		}

		private int settingsHeight(Module module) {
			if (!EXPANDED.contains(module)) return 0;
			return module.getSettings().size() * SETTING_ROW_HEIGHT;
		}

		boolean isOverHeader(double mouseX, double mouseY) {
			return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
		}

		boolean isOverArrow(double mouseX, double mouseY) {
			return mouseX >= x + width - ARROW_ZONE && mouseX <= x + width
					&& mouseY >= y && mouseY <= y + HEADER_HEIGHT;
		}

		RowHit rowAt(double mouseX, double mouseY) {
			if (collapsed) return null;
			if (mouseX < x || mouseX > x + width) return null;

			int rowY = y + HEADER_HEIGHT;
			for (Module module : modules) {
				if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
					return RowHit.ofModule(module);
				}
				rowY += ROW_HEIGHT;

				if (!EXPANDED.contains(module)) continue;
				for (Setting<?> setting : module.getSettings()) {
					if (mouseY >= rowY && mouseY < rowY + SETTING_ROW_HEIGHT) {
						return RowHit.ofSetting(setting, x + SETTING_INDENT, x + width - PADDING_X);
					}
					rowY += SETTING_ROW_HEIGHT;
				}
			}
			return null;
		}

		void render(DrawContext context, int mouseX, int mouseY) {
			int bodyHeight = height();

			boolean hasBody = !collapsed && !modules.isEmpty();
			if (hasBody) {
				RoundedRect.fill(context, x, y, x + width, y + bodyHeight, RADIUS, PANEL_BG);
				RoundedRect.fillTop(context, x, y, x + width, y + HEADER_HEIGHT, RADIUS, HEADER_BG);
			} else {
				RoundedRect.fill(context, x, y, x + width, y + HEADER_HEIGHT, RADIUS, HEADER_BG);
			}

			context.drawTextWithShadow(textRenderer, styled(category.name(), FONT_HEADER), x + PADDING_X, y + (HEADER_HEIGHT - 8) / 2, TEXT_HEADER);
			context.drawTextWithShadow(textRenderer, Text.literal(collapsed ? "▶" : "▼"), x + width - 10, y + (HEADER_HEIGHT - 8) / 2, ARROW_COLOR);

			if (collapsed) return;

			int rowY = y + HEADER_HEIGHT;
			for (Module module : modules) {
				boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
				if (hovered) {
					context.fill(x, rowY, x + width, rowY + ROW_HEIGHT, ROW_HOVER);
				}

				int textColor = module.isEnabled() ? TEXT_ENABLED : TEXT_DISABLED;
				int textY = rowY + (ROW_HEIGHT - 8) / 2;
				context.drawTextWithShadow(textRenderer, styled(module.getName(), FONT_REGULAR), x + PADDING_X, textY, textColor);

				String bind = bindLabel(module);
				if (!bind.isEmpty()) {
					int bindX = x + width - PADDING_X - rightReserve(module)
							- textRenderer.getWidth(styled(bind, FONT_REGULAR));
					context.drawTextWithShadow(textRenderer, styled(bind, FONT_REGULAR), bindX, textY, bindColor(module));
				}

				// A faint dot marks modules that have settings worth right-clicking for.
				if (!module.getSettings().isEmpty()) {
					boolean expanded = EXPANDED.contains(module);
					context.fill(x + width - PADDING_X - 2, rowY + ROW_HEIGHT / 2 - 1, x + width - PADDING_X, rowY + ROW_HEIGHT / 2 + 1,
							expanded ? TEXT_ENABLED : TEXT_SETTING_VALUE);
				}

				rowY += ROW_HEIGHT;
				if (!EXPANDED.contains(module)) continue;

				int settingsHeight = settingsHeight(module);
				if (settingsHeight > 0) {
					context.fill(x, rowY, x + width, rowY + settingsHeight, SETTINGS_BG);
				}

				for (Setting<?> setting : module.getSettings()) {
					renderSetting(context, setting, rowY);
					rowY += SETTING_ROW_HEIGHT;
				}
			}
		}

		private void renderSetting(DrawContext context, Setting<?> setting, int rowY) {
			int textY = rowY + 1;
			context.drawTextWithShadow(textRenderer, styled(setting.getName(), FONT_REGULAR), x + SETTING_INDENT, textY, TEXT_SETTING);

			if (setting instanceof BoolSetting bool) {
				int boxX2 = x + width - PADDING_X;
				int boxX1 = boxX2 - CHECKBOX_SIZE;
				int boxY1 = rowY + (SETTING_ROW_HEIGHT - CHECKBOX_SIZE) / 2;
				context.fill(boxX1, boxY1, boxX2, boxY1 + CHECKBOX_SIZE, bool.get() ? CHECKBOX_ON : CHECKBOX_OFF);
				return;
			}

			String value = valueLabel(setting);
			int valueWidth = textRenderer.getWidth(styled(value, FONT_REGULAR));
			context.drawTextWithShadow(textRenderer, styled(value, FONT_REGULAR), x + width - PADDING_X - valueWidth, textY, TEXT_SETTING_VALUE);

			int trackX1 = x + SETTING_INDENT;
			int trackX2 = x + width - PADDING_X;
			int trackY = rowY + SETTING_ROW_HEIGHT - TRACK_HEIGHT - 1;
			context.fill(trackX1, trackY, trackX2, trackY + TRACK_HEIGHT, TRACK_BG);

			int fillWidth = (int) Math.round((trackX2 - trackX1) * fractionOf(setting));
			if (fillWidth > 0) {
				context.fill(trackX1, trackY, trackX1 + fillWidth, trackY + TRACK_HEIGHT, TRACK_FILL);
			}
		}
	}
}
