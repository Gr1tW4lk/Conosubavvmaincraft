package dev.konocraft.net;

import dev.konocraft.KonoCraft;
import net.minecraft.util.Identifier;

/** Channel ids shared by client and server. */
public final class Packets {
	// server -> client
	public static final Identifier DIALOGUE_LINE = KonoCraft.id("dialogue_line");
	public static final Identifier DIALOGUE_CLOSE = KonoCraft.id("dialogue_close");
	public static final Identifier PLAYER_SYNC = KonoCraft.id("player_sync");
	public static final Identifier OBJECTIVE = KonoCraft.id("objective");
	public static final Identifier FX = KonoCraft.id("fx");
	/** Effect at a position in the world: explosion charge and blast. */
	public static final Identifier FX_AT = KonoCraft.id("fx_at");
	/** Big centre-screen title that scales and wraps to fit any window. */
	public static final Identifier TITLE = KonoCraft.id("title");
	public static final Identifier OPEN_SCREEN = KonoCraft.id("open_screen");
	public static final Identifier MUSIC = KonoCraft.id("music");
	public static final Identifier TOAST = KonoCraft.id("toast");

	// client -> server
	public static final Identifier DIALOGUE_ADVANCE = KonoCraft.id("dialogue_advance");
	public static final Identifier CAST_SKILL = KonoCraft.id("cast_skill");
	public static final Identifier SELECT_SKILL = KonoCraft.id("select_skill");
	public static final Identifier LEARN_SKILL = KonoCraft.id("learn_skill");
	public static final Identifier SHOP_BUY = KonoCraft.id("shop_buy");
	public static final Identifier BOARD_ACCEPT = KonoCraft.id("board_accept");
	public static final Identifier REQUEST_SYNC = KonoCraft.id("request_sync");

	// FX kinds
	public static final int FX_FLASH = 0;
	public static final int FX_SHAKE = 1;
	public static final int FX_LETTERBOX = 2;
	public static final int FX_AT_CHARGE = 0;
	public static final int FX_AT_BLAST = 1;

	// Screens
	public static final int SCREEN_SHOP = 0;
	public static final int SCREEN_BOARD = 1;
	public static final int SCREEN_CARD = 2;
	public static final int SCREEN_CREDITS = 3;

	private Packets() {
	}
}
