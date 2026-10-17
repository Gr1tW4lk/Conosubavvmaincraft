package dev.konocraft.client.screen;

import dev.konocraft.client.ClientState;
import dev.konocraft.story.Locations;
import dev.konocraft.world.Blueprint;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.CompletableFuture;

/**
 * Hand-drawn style map of the KonoSuba world (key M): roofs of Axel, roads, the lake, the castle
 * hill, named places, the current objective and the player.
 */
public class MapScreen extends Screen {
	/** World area covered by the map and blocks per map pixel. */
	private static final int MIN_X = -420, MIN_Z = -440, MAX_X = 440, MAX_Z = 660, SCALE = 2;
	private static final int W = (MAX_X - MIN_X) / SCALE, H = (MAX_Z - MIN_Z) / SCALE;

	private record Label(String key, boolean minor) {
	}

	private static final Label[] LABELS = {
		new Label("axel", false), new Label("north_gate", false), new Label("south_gate", false), new Label("toad_plains", false),
		new Label("lake", false), new Label("viewpoint", false), new Label("castle_gate", false), new Label("cabbage_fields", false),
		new Label("graveyard", false), new Label("dungeon", false), new Label("defense_line", false),
		new Label("guild", true), new Label("wiz_shop", true), new Label("church", true), new Label("stable", true), new Label("mansion", true),
		new Label("market", true), new Label("inn", true), new Label("bath", true), new Label("smithy", true), new Label("police", true),
		new Label("alderp", true), new Label("dustiness", true), new Label("park", true), new Label("barracks", true), new Label("warehouses", true), new Label("casino", true),
	};

	private static Identifier texture;
	private static CompletableFuture<NativeImage> pending;

	private double zoom = -1;
	private double centerX, centerZ;

	public MapScreen() {
		super(Text.literal("Карта"));
	}

	@Override
	protected void init() {
		if (texture == null && pending == null) pending = CompletableFuture.supplyAsync(MapScreen::paint);
		if (zoom < 0) {
			zoom = Math.max(0.5, Math.min(width / (double) W, height / (double) H) * 0.95);
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.player != null && KonoWorld.is(mc.player.getWorld()) && Regions.inTown(mc.player.getBlockX(), mc.player.getBlockZ())) {
				zoom *= 3;
				centerX = mc.player.getX();
				centerZ = mc.player.getZ();
			} else {
				centerX = (MIN_X + MAX_X) / 2.0;
				centerZ = (MIN_Z + MAX_Z) / 2.0;
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/** Paints the map once (off the render thread): structure roofs over shaded terrain. */
	private static NativeImage paint() {
		int[] top = new int[W * H];
		int[] topY = new int[W * H];
		java.util.Arrays.fill(topY, Integer.MIN_VALUE);
		Blueprint bp = Blueprint.get();
		bp.forEachBlock((x, y, z, s) -> {
			if (x < MIN_X || x >= MAX_X || z < MIN_Z || z >= MAX_Z || s.isAir()) return;
			int i = ((z - MIN_Z) / SCALE) * W + (x - MIN_X) / SCALE;
			if (y > topY[i] && y < Regions.AFTERLIFE_Y - 20) {
				topY[i] = y;
				top[i] = color(s);
			}
		});
		NativeImage img = new NativeImage(W, H, false);
		int prevRow[] = new int[W];
		for (int pz = 0; pz < H; pz++) {
			for (int px = 0; px < W; px++) {
				int x = MIN_X + px * SCALE, z = MIN_Z + pz * SCALE;
				int h = Terrain.heightAt(x, z);
				int i = pz * W + px;
				int rgb;
				double shade;
				if (topY[i] != Integer.MIN_VALUE && topY[i] >= h) {
					rgb = top[i];
					int north = pz > 0 ? prevRow[px] : topY[i];
					shade = topY[i] > north ? 1.12 : topY[i] < north ? 0.82 : 1.0;
					prevRow[px] = topY[i];
				} else {
					if (h < Regions.SEA_LEVEL) {
						rgb = h < Regions.SEA_LEVEL - 4 ? 0x3060B0 : 0x4A80C8;
						shade = 1;
					} else {
						rgb = Terrain.onRoad(x, z) ? 0xB89A6A : biome(x, z, h);
						int north = pz > 0 ? prevRow[px] : h;
						shade = h > north ? 1.1 : h < north ? 0.85 : 1.0;
					}
					prevRow[px] = h;
				}
				img.setColor(px, pz, abgr(rgb, shade));
			}
		}
		return img;
	}

	private static int color(BlockState s) {
		MapColor c = s.getBlock().getDefaultMapColor();
		if (c == null || c == MapColor.CLEAR) return 0x808080;
		return c.color;
	}

	private static int biome(int x, int z, int h) {
		return switch (Regions.biomeAt(x, z)) {
			case FOREST -> 0x3E6B2E;
			case FARMS -> 0x8FA84A;
			case TOAD_PLAINS -> 0x9DBB52;
			case CASTLE -> 0x7C8A6A;
			case BATTLEFIELD -> 0x8E9E5A;
			case LAKE -> 0x6FA05A;
			default -> h > 80 ? 0x8E9A86 : 0x79A84F;
		};
	}

	private static int abgr(int rgb, double shade) {
		int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * shade), 0, 255);
		int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * shade), 0, 255);
		int b = MathHelper.clamp((int) ((rgb & 0xFF) * shade), 0, 255);
		return 0xFF000000 | (b << 16) | (g << 8) | r;
	}

	// ------------------------------------------------------------------------------------------
	private double sx(double worldX) {
		return width / 2.0 + (worldX - centerX) / SCALE * zoom;
	}

	private double sy(double worldZ) {
		return height / 2.0 + (worldZ - centerZ) / SCALE * zoom;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		ctx.fill(0, 0, width, height, 0xFF1A1410);
		if (texture == null && pending != null && pending.isDone()) {
			texture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("konocraft_world_map", new NativeImageBackedTexture(pending.join()));
			pending = null;
		}
		if (texture == null) {
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Рисуем карту..."), width / 2, height / 2, 0xFFE8D8B0);
			return;
		}
		ctx.getMatrices().push();
		ctx.getMatrices().translate(sx(MIN_X), sy(MIN_Z), 0);
		ctx.getMatrices().scale((float) zoom, (float) zoom, 1);
		ctx.drawTexture(texture, 0, 0, W, H, 0, 0, W, H, W, H);
		ctx.getMatrices().pop();

		// labels
		for (Label l : LABELS) {
			if (l.minor && zoom < 2.2) continue;
			Locations.Loc loc = Locations.get(l.key);
			if (loc == null) continue;
			int x = (int) sx(loc.x()), y = (int) sy(loc.z());
			if (x < -50 || y < -10 || x > width + 50 || y > height + 10) continue;
			ctx.fill(x - 1, y - 1, x + 2, y + 2, 0xFF3A2A1A);
			String title = l.key.equals("axel") ? "Аксель" : loc.title();
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(title), x, y - 11, l.minor ? 0xFFF0E6C8 : 0xFFFFE070);
		}
		// objective marker
		Vec3d target = ClientState.objectiveTarget;
		if (target != null) {
			int x = (int) sx(target.x), y = (int) sy(target.z);
			int pulse = (int) (System.currentTimeMillis() / 250 % 2);
			ctx.fill(x - 3 - pulse, y - 3 - pulse, x + 4 + pulse, y + 4 + pulse, 0xFFE03030);
			ctx.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
		}
		// player
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null && KonoWorld.is(mc.player.getWorld())) {
			double px = sx(mc.player.getX()), py = sy(mc.player.getZ());
			float yaw = mc.player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			double fx = -MathHelper.sin(yaw), fz = MathHelper.cos(yaw);
			for (int i = 0; i <= 6; i++) {
				int ax = (int) (px + fx * i), ay = (int) (py + fz * i);
				int w = Math.max(0, 3 - i / 2);
				ctx.fill(ax - w, ay - w, ax + w + 1, ay + w + 1, i == 6 ? 0xFFFFFFFF : 0xFF2050FF);
			}
		} else {
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Вы сейчас не в мире KonoSuba"), width / 2, 24, 0xFFFF9090);
		}
		ctx.fill(0, height - 16, width, height, 0xC0000000);
		ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Колесо — масштаб, перетаскивание — сдвиг, M или Esc — закрыть"), width / 2, height - 12, 0xFFCCCCCC);
		ctx.fill(0, 0, width, 18, 0xC0000000);
		ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Карта королевства Бельзерг: окрестности Акселя"), width / 2, 5, 0xFFFFE070);
		super.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		double wx = centerX + (mouseX - width / 2.0) * SCALE / zoom, wz = centerZ + (mouseY - height / 2.0) * SCALE / zoom;
		zoom = MathHelper.clamp(zoom * (amount > 0 ? 1.25 : 0.8), 0.3, 12);
		centerX = wx - (mouseX - width / 2.0) * SCALE / zoom;
		centerZ = wz - (mouseY - height / 2.0) * SCALE / zoom;
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		centerX -= deltaX * SCALE / zoom;
		centerZ -= deltaY * SCALE / zoom;
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (dev.konocraft.client.KonoKeys.MAP.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
