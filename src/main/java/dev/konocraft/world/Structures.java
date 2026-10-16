package dev.konocraft.world;

import dev.konocraft.world.structure.AfterlifeHall;
import dev.konocraft.world.structure.AxelTown;
import dev.konocraft.world.structure.BeldiaCastle;
import dev.konocraft.world.structure.Outskirts;

/** Builds every hand-made structure of the map into the blueprint. */
public final class Structures {
	private Structures() {
	}

	public static void buildAll(Build b) {
		Outskirts.build(b);
		BeldiaCastle.build(b);
		AxelTown.build(b);
		AfterlifeHall.build(b);
	}
}
