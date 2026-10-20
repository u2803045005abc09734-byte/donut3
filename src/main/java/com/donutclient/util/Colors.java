package com.donutclient.util;

public final class Colors {
	private Colors() {}

	/** h, s, v in 0..1. Returns 0xRRGGBB. */
	public static int hsvToRgb(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float hh = h * 6f;
		int i = (int) hh;
		float f = hh - i;
		float p = v * (1f - s);
		float q = v * (1f - f * s);
		float t = v * (1f - (1f - f) * s);
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
	}

	/** Returns {h, s, v} in 0..1 for 0xRRGGBB. */
	public static float[] rgbToHsv(int rgb) {
		float r = ((rgb >> 16) & 255) / 255f;
		float g = ((rgb >> 8) & 255) / 255f;
		float b = (rgb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h;
		if (d == 0f) {
			h = 0f;
		} else if (max == r) {
			h = (((g - b) / d) % 6f) / 6f;
		} else if (max == g) {
			h = (((b - r) / d) + 2f) / 6f;
		} else {
			h = (((r - g) / d) + 4f) / 6f;
		}
		if (h < 0f) h += 1f;
		float s = max == 0f ? 0f : d / max;
		return new float[] {h, s, max};
	}

	/** Linear mix of two ARGB colors. */
	public static int mix(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
		int ar = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int ag = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int ab = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return (aa << 24) | (ar << 16) | (ag << 8) | ab;
	}

	public static int opaque(int rgb) {
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}

	public static int withAlpha(int rgb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}
}
