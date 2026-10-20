package com.donutclient.module;

import java.util.ArrayList;
import java.util.List;

import com.donutclient.render.BoxRenderer;
import com.donutclient.setting.BoolSetting;
import com.donutclient.setting.ColorSetting;
import com.donutclient.setting.EnumSetting;
import com.donutclient.setting.NumberSetting;
import com.donutclient.util.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared logic for modules that scan loaded block entities and highlight them.
 * Subclasses only decide which blocks are interesting by implementing {@link #classify}.
 */
public abstract class FinderModule extends ClientModule {
	protected record Highlight(AABB box, Target target, int seed) {}

	/** One kind of block a finder can highlight: an on/off toggle plus its own color. */
	protected final class Target {
		final BoolSetting enabled;
		final ColorSetting color;

		Target(String label, String description, boolean defaultOn, int rgb) {
			this.enabled = add(new BoolSetting("Targets", label, description, defaultOn));
			this.color = add(new ColorSetting("Colors", label + " Color", "Color used for " + label.toLowerCase() + ".", rgb));
			this.color.visibleWhen(() -> colorMode.index() == 0);
		}
	}

	protected final EnumSetting renderMode = add(new EnumSetting("Rendering", "Render Mode",
		"Draw only the outline, only a filled box, or both.", new String[] {"Outline", "Fill", "Outline + Fill"}, 2));
	protected final EnumSetting visibility = add(new EnumSetting("Rendering", "Visibility",
		"Show highlights through walls or only when in line of sight.", new String[] {"Through Walls", "Visible Only"}, 0));
	protected final NumberSetting outlineOpacity = add(new NumberSetting("Rendering", "Outline Opacity",
		"How opaque the outline is.", 90, 0, 100, 1, "%", 0));
	protected final NumberSetting fillOpacity = add(new NumberSetting("Rendering", "Fill Opacity",
		"How opaque the filled box is.", 22, 0, 100, 1, "%", 0));
	protected final NumberSetting thickness = add(new NumberSetting("Rendering", "Outline Thickness",
		"Thickness of the outline in blocks.", 0.03, 0.005, 0.12, 0.005, "", 3));
	protected final NumberSetting padding = add(new NumberSetting("Rendering", "Box Padding",
		"Grow or shrink the box around each block.", 0.0, -0.10, 0.25, 0.01, "", 2));
	protected final BoolSetting distanceFade = add(new BoolSetting("Rendering", "Distance Fade",
		"Fade highlights out the farther away they are.", false));

	protected final EnumSetting colorMode = add(new EnumSetting("Colors", "Color Mode",
		"Use a color per type, one color for everything, or a rainbow.", new String[] {"Per Type", "Single Color", "Rainbow"}, 0));
	protected final ColorSetting singleColor = add(new ColorSetting("Colors", "Single Color",
		"Color used for every highlight.", 0x4F7CFF));
	protected final NumberSetting rainbowSpeed = add(new NumberSetting("Colors", "Rainbow Speed",
		"How fast the rainbow cycles.", 0.25, 0.05, 2.0, 0.05, "x", 2));

	protected final NumberSetting range = add(new NumberSetting("Scanning", "Range",
		"Maximum distance in blocks. Only loaded chunks are scanned.", 128, 16, 512, 16, " blocks", 0));
	protected final NumberSetting maxHighlights = add(new NumberSetting("Scanning", "Max Highlights",
		"Upper limit of boxes drawn at once. Closest ones are kept.", 300, 25, 2000, 25, "", 0));

	private volatile List<Highlight> highlights = List.of();
	private int tickCounter;
	private boolean scanNow = true;

	protected FinderModule(String name, String description, String category, Icon icon) {
		super(name, description, category, icon);
		singleColor.visibleWhen(() -> colorMode.index() == 1);
		rainbowSpeed.visibleWhen(() -> colorMode.index() == 2);
		outlineOpacity.visibleWhen(() -> renderMode.index() != 1);
		thickness.visibleWhen(() -> renderMode.index() != 1);
		fillOpacity.visibleWhen(() -> renderMode.index() != 0);
	}

	/** Returns the target this block belongs to, or null if it should not be highlighted. */
	protected abstract Target classify(Block block, BlockState state);

	@Override
	public String status() {
		return isEnabled() ? highlights.size() + " tracked" : "";
	}

	@Override
	protected void onEnable() {
		scanNow = true;
	}

	@Override
	protected void onDisable() {
		highlights = List.of();
	}

	@Override
	public void onTick(Minecraft mc) {
		if (!isEnabled()) {
			return;
		}
		if (mc.level == null || mc.player == null) {
			highlights = List.of();
			return;
		}
		if (scanNow || ++tickCounter >= 10) {
			tickCounter = 0;
			scanNow = false;
			scan(mc);
		}
	}

	private void scan(Minecraft mc) {
		ClientLevel level = mc.level;
		double px = mc.player.getX();
		double py = mc.player.getY();
		double pz = mc.player.getZ();
		double maxDist = range.getDouble();
		double maxDistSq = maxDist * maxDist;
		int chunkRadius = (int) (maxDist / 16.0) + 1;
		int pcx = mc.player.blockPosition().getX() >> 4;
		int pcz = mc.player.blockPosition().getZ() >> 4;

		List<Highlight> found = new ArrayList<>();
		for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
			for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
				LevelChunk chunk = level.getChunk(cx, cz);
				if (chunk == null) {
					continue;
				}
				List<BlockPos> positions = new ArrayList<>(chunk.getBlockEntities().keySet());
				for (BlockPos pos : positions) {
					double dx = pos.getX() + 0.5 - px;
					double dy = pos.getY() + 0.5 - py;
					double dz = pos.getZ() + 0.5 - pz;
					if (dx * dx + dy * dy + dz * dz > maxDistSq) {
						continue;
					}
					BlockState state = chunk.getBlockState(pos);
					Target target = classify(state.getBlock(), state);
					if (target == null || !target.enabled.on()) {
						continue;
					}
					AABB box = new AABB(pos);
					if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
						ChestType type = state.getValue(ChestBlock.TYPE);
						if (type == ChestType.RIGHT) {
							continue; // drawn together with the left half
						}
						if (type == ChestType.LEFT) {
							Direction other = ChestBlock.getConnectedDirection(state);
							box = box.minmax(new AABB(pos.relative(other)));
						}
					}
					found.add(new Highlight(box, target, pos.hashCode()));
				}
			}
		}

		int limit = maxHighlights.getInt();
		if (found.size() > limit) {
			found.sort((a, b) -> Double.compare(distSq(a, px, py, pz), distSq(b, px, py, pz)));
			found = new ArrayList<>(found.subList(0, limit));
		}
		highlights = found;
		tickCounter = 0;
	}

	private static double distSq(Highlight h, double x, double y, double z) {
		Vec3 c = h.box().getCenter();
		double dx = c.x - x;
		double dy = c.y - y;
		double dz = c.z - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public boolean hasHighlights() {
		return isEnabled() && !highlights.isEmpty();
	}

	/** Called every frame by the world renderer to queue this module's boxes. */
	public void emit(BoxRenderer renderer, Vec3 camera) {
		List<Highlight> list = highlights;
		if (list.isEmpty()) {
			return;
		}
		boolean walls = visibility.index() == 0;
		int mode = renderMode.index(); // 0 = outline, 1 = fill, 2 = both
		double maxDist = range.getDouble();
		double now = System.currentTimeMillis() / 1000.0;
		float outlineA = outlineOpacity.getFloat() / 100f;
		float fillA = fillOpacity.getFloat() / 100f;
		double thick = thickness.getDouble();
		double pad = padding.getDouble();

		for (Highlight h : list) {
			Vec3 c = h.box().getCenter();
			double dx = c.x - camera.x;
			double dy = c.y - camera.y;
			double dz = c.z - camera.z;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (dist > maxDist + 8.0) {
				continue;
			}
			float fade = distanceFade.on() ? (float) (1.0 - 0.75 * Math.min(1.0, dist / maxDist)) : 1f;

			int rgb = switch (colorMode.index()) {
				case 1 -> singleColor.rgb();
				case 2 -> Colors.hsvToRgb((float) (now * rainbowSpeed.getDouble() + (h.seed() & 1023) / 1024.0), 0.75f, 1f);
				default -> h.target().color.rgb();
			};
			float r = ((rgb >> 16) & 255) / 255f;
			float g = ((rgb >> 8) & 255) / 255f;
			float b = (rgb & 255) / 255f;

			AABB box = h.box().inflate(pad);
			if (mode != 1) {
				renderer.outline(box, thick, r, g, b, outlineA * fade, walls);
			}
			if (mode != 0) {
				renderer.fill(box, r, g, b, fillA * fade, walls);
			}
		}
	}
}
