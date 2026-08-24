package com.nyxclient.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Vanilla DrawContext only draws flat rectangles. This fills an area scanline-by-scanline, insetting
 * each row near the top/bottom by how far a quarter-circle of the given radius would inset it, which is
 * enough to fake soft corners without a texture asset.
 */
final class RoundedRect {

	private RoundedRect() {
	}

	static void fill(DrawContext context, int x1, int y1, int x2, int y2, int radius, int color) {
		fill(context, x1, y1, x2, y2, radius, color, true);
	}

	// Rounds only the top corners, leaving the bottom edge flat - for a header strip that has to seam
	// cleanly into a body rect drawn right below it, instead of cutting an unwanted notch into it.
	static void fillTop(DrawContext context, int x1, int y1, int x2, int y2, int radius, int color) {
		fill(context, x1, y1, x2, y2, radius, color, false);
	}

	private static void fill(DrawContext context, int x1, int y1, int x2, int y2, int radius, int color, boolean roundBottom) {
		int width = x2 - x1;
		int height = y2 - y1;
		if (width <= 0 || height <= 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

		for (int row = 0; row < height; row++) {
			int inset = cornerInset(row, height, r, roundBottom);
			context.fill(x1 + inset, y1 + row, x2 - inset, y1 + row + 1, color);
		}
	}

	private static int cornerInset(int row, int height, int r, boolean roundBottom) {
		if (r <= 0) return 0;

		int distFromEdge;
		if (row < r) {
			distFromEdge = row;
		} else if (roundBottom && row >= height - r) {
			distFromEdge = height - 1 - row;
		} else {
			return 0;
		}

		double dy = r - distFromEdge - 0.5;
		double inside = (double) r * r - dy * dy;
		return (int) Math.round(r - Math.sqrt(Math.max(0, inside)));
	}
}
