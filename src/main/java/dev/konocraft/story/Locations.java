package dev.konocraft.story;

import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/** Named places used by the story, dialogues and teleports. */
public final class Locations {
	public record Loc(String name, double x, double y, double z, float yaw, String title) {
		public Vec3d vec() {
			return new Vec3d(x, y, z);
		}

		public BlockPos pos() {
			return BlockPos.ofFloored(x, y, z);
		}
	}

	private static final Map<String, Loc> ALL = new HashMap<>();

	private static Loc add(String name, double x, double y, double z, float yaw, String title) {
		Loc l = new Loc(name, x, y, z, yaw, title);
		ALL.put(name, l);
		return l;
	}

	/** Location standing on the natural ground (y computed from the terrain). */
	private static Loc ground(String name, double x, double z, float yaw, String title) {
		int h = Math.max(Terrain.heightAt((int) Math.floor(x), (int) Math.floor(z)), Regions.SEA_LEVEL);
		return add(name, x, h + 1, z, yaw, title);
	}

	static final int AX = Regions.AFTERLIFE_X, AY = Regions.AFTERLIFE_Y, AZ = Regions.AFTERLIFE_Z;

	public static final Loc AFTERLIFE = add("afterlife", AX + 0.5, AY + 1, AZ + 10.5, 180, "Зал между мирами");
	public static final Loc ERIS = add("eris", AX + 0.5, AY + 3, AZ - 11.5, 0, "Трон Эрис");
	public static final Loc KAZUMA_GHOST = add("kazuma_ghost", AX - 10.5, AY + 1, AZ - 3.5, -90, "Скамейка ожидания");

	public static final Loc AXEL = add("axel", 0.5, 65, 9.5, 180, "Аксель, площадь");
	public static final Loc GUILD = add("guild", -28.5, 65, -11.5, 180, "Гильдия авантюристов");
	public static final Loc GUILD_INSIDE = add("guild_inside", -28.5, 65, -17.5, 180, "Гильдия авантюристов");
	public static final Loc GUILD_DOOR = add("guild_door", -28.5, 65, -15.5, 180, "Гильдия авантюристов");
	public static final Loc LUNA = add("luna", -27.5, 65, -37.5, 0, "Стойка регистрации");
	public static final Loc GUILD_AQUA = add("guild_aqua", -18.5, 65, -28.5, 90, "Таверна гильдии");
	public static final Loc GUILD_MEGUMIN = add("guild_megumin", -18.5, 65, -22.5, 90, "Таверна гильдии");
	public static final Loc GUILD_DARKNESS = add("guild_darkness", -36.5, 65, -21.5, 180, "Гильдия авантюристов");
	public static final Loc LUNA_SIDE = add("luna_side", -23.5, 65, -33.5, 90, "Гильдия авантюристов");
	public static final Loc GUILD_TABLE = add("guild_table", -19.5, 65, -24.5, 90, "Таверна гильдии");
	public static final Loc WIZ = add("wiz", 23.5, 65, -29.5, 0, "Магазин Виз");
	public static final Loc WIZ_SHOP = add("wiz_shop", 23.5, 65, -11.5, 180, "Магазин Виз");
	public static final Loc VANIR = add("vanir", 17.5, 65, -20.5, -90, "Магазин Виз");
	public static final Loc CHURCH = add("church", 28.5, 65, 21.5, 0, "Церковь Эрис");
	public static final Loc GRAVEYARD = add("graveyard", dev.konocraft.world.structure.Graveyard.X2 + 2.5,
		dev.konocraft.world.structure.Graveyard.floor() + 1, dev.konocraft.world.Regions.GRAVE_Z + 0.5, 90, "Кладбище Акселя");
	public static final Loc GRAVEYARD_WIZ = add("graveyard_wiz", dev.konocraft.world.Regions.GRAVE_X - 3.5,
		dev.konocraft.world.structure.Graveyard.floor() + 1, dev.konocraft.world.Regions.GRAVE_Z + 0.5, -90, "Кладбище Акселя");
	public static final Loc DUNGEON = add("dungeon", dev.konocraft.world.Regions.DUNGEON_X + 0.5,
		dev.konocraft.world.structure.KeeleDungeon.surface() + 1, dev.konocraft.world.Regions.DUNGEON_Z + 4.5, 180, "Подземелье Киля");
	public static final Loc KEELE = add("keele", dev.konocraft.world.structure.KeeleDungeon.KEELE_X + 1.5,
		dev.konocraft.world.Regions.DUNGEON_Y, dev.konocraft.world.structure.KeeleDungeon.KEELE_Z + 0.5, -90, "Подземелье Киля");
	public static final Loc YUNYUN = add("yunyun", 11.5, 65, 6.5, 180, "Площадь Акселя");
	public static final Loc STABLE = add("stable", -56.5, 65, 55.5, -90, "Конюшня");
	public static final Loc MANSION = add("mansion", 52.5, 65, -46.5, 180, "Особняк");
	public static final Loc NORTH_GATE = ground("north_gate", 0.5, -88.5, 180, "Северные ворота");
	public static final Loc BELDIA_GATE = ground("beldia_gate", 0.5, -104.5, 0, "Перед северными воротами");
	public static final Loc SOUTH_GATE = ground("south_gate", 0.5, 86.5, 0, "Южные ворота");

	public static final Loc TOAD_PLAINS = ground("toad_plains", 196.5, 18.5, -90, "Жабьи равнины");
	public static final Loc LAKE = ground("lake", -40.5, -176.5, 180, "Озеро");
	public static final Loc VIEWPOINT = add("viewpoint", Regions.VIEW_X + 0.5, Regions.VIEW_Y + 1, Regions.VIEW_Z + 3.5, -135, "Холм Мегумин");
	public static final Loc CASTLE_GATE = ground("castle_gate", Regions.CASTLE_X + 0.5, Regions.CASTLE_Z + 42.5, 180, "Заброшенный замок");
	public static final Loc CASTLE_KEEP = add("castle_keep", Regions.CASTLE_X + 0.5, Regions.CASTLE_Y + 1, Regions.CASTLE_Z - 0.5, 180, "Тронный зал");
	public static final Loc BELDIA_THRONE = add("beldia_throne", Regions.CASTLE_X + 0.5, Regions.CASTLE_Y + 1, Regions.CASTLE_Z - 16.5, 0, "Тронный зал");
	public static final Loc CASTLE_TARGET = add("castle_target", Regions.CASTLE_X - 26.5, Regions.CASTLE_Y + 8, Regions.CASTLE_Z + 26.5, 0, "Стены замка");
	public static final Loc CABBAGE_FIELDS = add("cabbage_fields", -175.5, 65, 6.5, 90, "Капустные поля");
	public static final Loc DEFENSE_LINE = ground("defense_line", 0.5, Regions.DEFENSE_Z - 10.5, 0, "Линия обороны");
	public static final Loc DEFENSE_WIZ = ground("defense_wiz", 4.5, Regions.DEFENSE_Z - 7.5, 0, "Линия обороны");
	public static final Loc DEFENSE_LUNA = ground("defense_luna", 9.5, Regions.DEFENSE_Z - 10.5, 0, "Линия обороны");
	public static final Loc DESTROYER_START = ground("destroyer_start", 0.5, Regions.DESTROYER_START_Z + 0.5, 180, "Южная равнина");

	private Locations() {
	}

	public static Loc get(String name) {
		return ALL.get(name);
	}

	public static Map<String, Loc> all() {
		return ALL;
	}
}
