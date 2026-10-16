package dev.konocraft.registry;

import dev.konocraft.KonoCraft;
import dev.konocraft.entity.AquaEntity;
import dev.konocraft.entity.BarrierCrystalEntity;
import dev.konocraft.entity.BeldiaEntity;
import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.entity.DarknessEntity;
import dev.konocraft.entity.DestroyerCoreEntity;
import dev.konocraft.entity.DestroyerEntity;
import dev.konocraft.entity.DestroyerGolemEntity;
import dev.konocraft.entity.FlyingCabbageEntity;
import dev.konocraft.entity.GiantToadEntity;
import dev.konocraft.entity.KonoMonsterEntity;
import dev.konocraft.entity.KonoNpcEntity;
import dev.konocraft.entity.MagicBoltEntity;
import dev.konocraft.entity.MeguminEntity;
import dev.konocraft.entity.TownsfolkEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public final class ModEntities {
	private static final Map<String, EntityType<? extends KonoNpcEntity>> CHARACTERS = new HashMap<>();

	// Story characters
	public static final EntityType<KonoNpcEntity> ERIS = npc("eris");
	public static final EntityType<KonoNpcEntity> KAZUMA = npc("kazuma");
	public static final EntityType<KonoNpcEntity> LUNA = npc("luna");
	public static final EntityType<KonoNpcEntity> WIZ = npc("wiz");
	public static final EntityType<KonoNpcEntity> VANIR = npc("vanir");
	public static final EntityType<KonoNpcEntity> YUNYUN = npc("yunyun");
	public static final EntityType<MeguminEntity> MEGUMIN = character("megumin", FabricEntityTypeBuilder.create(SpawnGroup.MISC, MeguminEntity::new)
		.dimensions(EntityDimensions.fixed(0.6f, 1.75f)).trackRangeBlocks(80).build());
	public static final EntityType<AquaEntity> AQUA = character("aqua", FabricEntityTypeBuilder.create(SpawnGroup.MISC, AquaEntity::new)
		.dimensions(EntityDimensions.fixed(0.6f, 1.8f)).trackRangeBlocks(80).build());
	public static final EntityType<DarknessEntity> DARKNESS = character("darkness", FabricEntityTypeBuilder.create(SpawnGroup.MISC, DarknessEntity::new)
		.dimensions(EntityDimensions.fixed(0.6f, 1.85f)).trackRangeBlocks(80).build());
	public static final EntityType<TownsfolkEntity> TOWNSFOLK = register("townsfolk", FabricEntityTypeBuilder.create(SpawnGroup.MISC, TownsfolkEntity::new)
		.dimensions(EntityDimensions.fixed(0.6f, 1.8f)).trackRangeBlocks(64).build());

	// Monsters
	public static final EntityType<GiantToadEntity> GIANT_TOAD = register("giant_toad", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GiantToadEntity::new)
		.dimensions(EntityDimensions.fixed(3.3f, 2.9f)).trackRangeBlocks(96).build());
	public static final EntityType<GiantToadEntity> TOAD_KING = register("toad_king", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GiantToadEntity::new)
		.dimensions(EntityDimensions.fixed(6.4f, 5.4f)).trackRangeBlocks(128).build());
	public static final EntityType<FlyingCabbageEntity> FLYING_CABBAGE = register("flying_cabbage", FabricEntityTypeBuilder.create(SpawnGroup.MISC, FlyingCabbageEntity::new)
		.dimensions(EntityDimensions.fixed(0.6f, 0.6f)).trackRangeBlocks(64).build());
	public static final EntityType<KonoMonsterEntity.Goblin> GOBLIN = register("goblin", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, KonoMonsterEntity.Goblin::new)
		.dimensions(EntityDimensions.fixed(0.55f, 1.3f)).trackRangeBlocks(64).build());
	public static final EntityType<KonoMonsterEntity.UndeadKnight> UNDEAD_KNIGHT = register("undead_knight", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, KonoMonsterEntity.UndeadKnight::new)
		.dimensions(EntityDimensions.fixed(0.6f, 1.95f)).trackRangeBlocks(64).build());
	public static final EntityType<DestroyerGolemEntity> DESTROYER_GOLEM = register("destroyer_golem", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DestroyerGolemEntity::new)
		.dimensions(EntityDimensions.fixed(1.3f, 2.6f)).trackRangeBlocks(80).build());

	// Bosses
	public static final EntityType<BeldiaEntity> BELDIA = register("beldia", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, BeldiaEntity::new)
		.dimensions(EntityDimensions.fixed(0.9f, 2.5f)).trackRangeBlocks(96).fireImmune().build());
	public static final EntityType<DestroyerEntity> DESTROYER = register("destroyer", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DestroyerEntity::new)
		.dimensions(EntityDimensions.fixed(DestroyerEntity.WIDTH, 11f)).trackRangeBlocks(256).trackedUpdateRate(2).fireImmune().build());
	public static final EntityType<BarrierCrystalEntity> BARRIER_CRYSTAL = register("barrier_crystal", FabricEntityTypeBuilder.create(SpawnGroup.MISC, BarrierCrystalEntity::new)
		.dimensions(EntityDimensions.fixed(1.2f, 2.6f)).trackRangeBlocks(160).trackedUpdateRate(2).fireImmune().build());
	public static final EntityType<DestroyerCoreEntity> DESTROYER_CORE = register("destroyer_core", FabricEntityTypeBuilder.create(SpawnGroup.MISC, DestroyerCoreEntity::new)
		.dimensions(EntityDimensions.fixed(2.2f, 2.2f)).trackRangeBlocks(160).fireImmune().build());

	// Projectiles
	public static final EntityType<MagicBoltEntity> MAGIC_BOLT = register("magic_bolt", FabricEntityTypeBuilder.<MagicBoltEntity>create(SpawnGroup.MISC, MagicBoltEntity::new)
		.dimensions(EntityDimensions.fixed(0.4f, 0.4f)).trackRangeBlocks(80).trackedUpdateRate(10).build());

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
		return Registry.register(Registries.ENTITY_TYPE, KonoCraft.id(name), type);
	}

	private static <T extends KonoNpcEntity> EntityType<T> character(String name, EntityType<T> type) {
		EntityType<T> t = register(name, type);
		CHARACTERS.put(name, t);
		return t;
	}

	private static EntityType<KonoNpcEntity> npc(String name) {
		return character(name, FabricEntityTypeBuilder.<KonoNpcEntity>create(SpawnGroup.MISC, KonoNpcEntity::new)
			.dimensions(EntityDimensions.fixed(0.6f, name.equals("vanir") ? 2.0f : 1.8f)).trackRangeBlocks(80).build());
	}

	public static EntityType<? extends KonoNpcEntity> byCharacter(String key) {
		return CHARACTERS.get(key);
	}

	public static void init() {
		for (EntityType<? extends KonoNpcEntity> t : new EntityType[]{ERIS, KAZUMA, LUNA, WIZ, VANIR, YUNYUN}) {
			FabricDefaultAttributeRegistry.register(t, KonoNpcEntity.createAttributes());
		}
		FabricDefaultAttributeRegistry.register(TOWNSFOLK, KonoNpcEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MEGUMIN, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(AQUA, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(DARKNESS, DarknessEntity.createDarknessAttributes());
		FabricDefaultAttributeRegistry.register(GIANT_TOAD, GiantToadEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TOAD_KING, GiantToadEntity.createKingAttributes());
		FabricDefaultAttributeRegistry.register(FLYING_CABBAGE, FlyingCabbageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GOBLIN, KonoMonsterEntity.Goblin.createAttributes());
		FabricDefaultAttributeRegistry.register(UNDEAD_KNIGHT, KonoMonsterEntity.UndeadKnight.createAttributes());
		FabricDefaultAttributeRegistry.register(DESTROYER_GOLEM, DestroyerGolemEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BELDIA, BeldiaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(DESTROYER, DestroyerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BARRIER_CRYSTAL, BarrierCrystalEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(DESTROYER_CORE, DestroyerCoreEntity.createAttributes());
	}
}
