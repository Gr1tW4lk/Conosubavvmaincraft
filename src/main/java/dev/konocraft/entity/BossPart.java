package dev.konocraft.entity;

import net.minecraft.entity.Entity;

/** An entity that belongs to a bigger boss (barrier crystals, the Destroyer's core). */
public interface BossPart {
	Entity owner();
}
