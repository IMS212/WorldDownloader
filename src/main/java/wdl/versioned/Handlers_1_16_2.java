/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl.versioned;


import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Lifecycle;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.PrimaryLevelData;
import wdl.handler.block.BarrelHandler;
import wdl.handler.block.BeaconHandler;
import wdl.handler.block.BlastFurnaceHandler;
import wdl.handler.block.BlockHandler;
import wdl.handler.block.BrewingStandHandler;
import wdl.handler.block.ChestHandler;
import wdl.handler.block.DispenserHandler;
import wdl.handler.block.DropperHandler;
import wdl.handler.block.FurnaceHandler;
import wdl.handler.block.HopperHandler;
import wdl.handler.block.LecternHandler;
import wdl.handler.block.ShulkerBoxHandler;
import wdl.handler.block.SmokerHandler;
import wdl.handler.block.TrappedChestHandler;
import wdl.handler.blockaction.BlockActionHandler;
import wdl.handler.entity.EntityHandler;
import wdl.handler.entity.HopperMinecartHandler;
import wdl.handler.entity.HorseHandler;
import wdl.handler.entity.StorageMinecartHandler;
import wdl.handler.entity.VillagerHandler;

final class HandlerFunctions {
	private HandlerFunctions() { throw new AssertionError(); }

	// NOTE: func_239770_b_ creates a new instance each time!  Even this use might be wrong;
	// probably vanilla's should be in use.  (XXX)
	static final RegistryAccess.RegistryHolder DYNAMIC_REGISTRIES = RegistryAccess.builtin();

	static final DimensionWrapper NETHER = new DimensionWrapper(DimensionType.NETHER_LOCATION, Level.NETHER);
	static final DimensionWrapper OVERWORLD = new DimensionWrapper(DimensionType.OVERWORLD_LOCATION, Level.OVERWORLD);
	static final DimensionWrapper END = new DimensionWrapper(DimensionType.END_LOCATION, Level.END);

	// TODO: This doesn't interact with the values above, but I'm not sure how to best handle that
	private static Map<Level, DimensionWrapper> dimensions = new WeakHashMap<>();

	/* (non-javadoc)
	 * @see VersionedFunctions#getDimension
	 */
	static DimensionWrapper getDimension(Level world) {
		return dimensions.computeIfAbsent(world, DimensionWrapper::new);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#hasSkyLight
	 */
	static boolean hasSkyLight(Level world) {
		// 1.11+: use hasSkyLight
		return getDimension(world).getType().hasSkyLight();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#BLOCK_HANDLERS
	 */
	static final ImmutableList<BlockHandler<?, ?>> BLOCK_HANDLERS = ImmutableList.of(
			new BarrelHandler(),
			new BeaconHandler(),
			new BrewingStandHandler(),
			new BlastFurnaceHandler(),
			new ChestHandler(),
			new DispenserHandler(),
			new DropperHandler(),
			new FurnaceHandler(),
			new HopperHandler(),
			new LecternHandler(),
			new ShulkerBoxHandler(),
			new SmokerHandler(),
			new TrappedChestHandler()
	);

	/* (non-javadoc)
	 * @see VersionedFunctions#BLOCK_ACTION_HANDLERS
	 */
	static final ImmutableList<BlockActionHandler<?, ?>> BLOCK_ACTION_HANDLERS = ImmutableList.of();

	/* (non-javadoc)
	 * @see VersionedFunctions#ENTITY_HANDLERS
	 */
	static final ImmutableList<EntityHandler<?, ?>> ENTITY_HANDLERS = ImmutableList.of(
			new HopperMinecartHandler(),
			new HorseHandler(),
			new StorageMinecartHandler(),
			new VillagerHandler()
	);

	/* (non-javadoc)
	 * @see VersionedFunctions#shouldImportBlockEntity
	 */
	static boolean shouldImportBlockEntity(String entityID, BlockPos pos,
			Block block, CompoundTag blockEntityNBT, LevelChunk chunk) {
		// Note sBlock do not have a block entity in this version.
		if (block instanceof ChestBlock && entityID.equals("minecraft:chest")) {
			return true;
		} else if (block instanceof TrappedChestBlock && entityID.equals("minecraft:trapped_chest")) {
			// Separate block entity from regular chests in this version.
			return true;
		} else if (block instanceof DispenserBlock && entityID.equals("minecraft:dispenser")) {
			return true;
		} else if (block instanceof DropperBlock && entityID.equals("minecraft:dropper")) {
			return true;
		} else if (block instanceof FurnaceBlock && entityID.equals("minecraft:furnace")) {
			return true;
		} else if (block instanceof BrewingStandBlock && entityID.equals("minecraft:brewing_stand")) {
			return true;
		} else if (block instanceof HopperBlock && entityID.equals("minecraft:hopper")) {
			return true;
		} else if (block instanceof BeaconBlock && entityID.equals("minecraft:beacon")) {
			return true;
		} else if (block instanceof ShulkerBoxBlock && entityID.equals("minecraft:shulker_box")) {
			return true;
		} else if (block instanceof CommandBlock && entityID.equals("minecraft:command_block")) {
			// Only import command sBlock if the current world doesn't have a command set
			// for the one there, as WDL doesn't explicitly save them so we need to use the
			// one currently present in the world.
			BlockEntity temp = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
			if (temp == null || !(temp instanceof CommandBlockEntity)) {
				// Bad/missing data currently there, import the old data
				return true;
			}
			CommandBlockEntity te = (CommandBlockEntity) temp;
			boolean currentBlockHasCommand = !te.getCommandBlock().getCommand().isEmpty();
			// Only import if the current command block has no command.
			return !currentBlockHasCommand;
		} else {
			return false;
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#createNewBlockEntity
	 */
	@Nullable
	static BlockEntity createNewBlockEntity(Level world, BaseEntityBlock block, BlockState state) {
		return block.newBlockEntity(world);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getSaveHandler
	 */
	static ISaveHandlerWrapper getSaveHandler(Minecraft minecraft, String worldName) throws Exception {
		return new LevelSaveWrapper(minecraft.getLevelSource().createAccess(worldName));
	}

	static class LevelSaveWrapper implements ISaveHandlerWrapper {
		public final LevelStorageAccess save;
		public LevelSaveWrapper(LevelStorageAccess save) {
			this.save = save;
		}

		@Override
		public void close() throws Exception {
			this.save.close();
		}

		@Override
		public File getWorldDirectory() {
			// XXX: This is rather dubious
			return this.save.getDimensionPath(OVERWORLD.getWorldKey());
		}

		@Override
		public void checkSessionLock() throws Exception {
			// Happens automatically?  func_237301_i_ does it, but it's not public.
			// Use func_237298_f_, which calls it and otherwise doesn't do much (it gets icon.png)
			this.save.getIconFile();
		}

		@Override
		public Object getWrapped() {
			return this.save;
		}

		@Override
		public String toString() {
			return "LevelSaveWrapper [save=" + save + "]";
		}
	}

	static class DimensionWrapper implements IDimensionWrapper {
		private final DimensionType dimensionType;
		private final ResourceKey<Level> worldKey;

		public DimensionWrapper(Level world) {
			this.dimensionType = world.dimensionType();
			this.worldKey = world.dimension();
		}

		public DimensionWrapper(ResourceKey<DimensionType> dimensionTypeKey,
				ResourceKey<Level> worldKey) {
			Registry<DimensionType> dimTypeReg = DYNAMIC_REGISTRIES.dimensionTypes();
			this.dimensionType = dimTypeReg.get(dimensionTypeKey);
			this.worldKey = worldKey;
		}

		@Override
		public String getFolderName() {
			if (this.worldKey == Level.END) {
				return "DIM1";
			} else if (this.worldKey == Level.NETHER) {
				return "DIM-1";
			}
			return null;
		}

		@Override
		public DimensionType getType() {
			return this.dimensionType;
		}

		@Override
		public ResourceKey<DimensionType> getTypeKey() {
			return null;
		}

		@Override
		public ResourceKey<Level> getWorldKey() {
			return this.worldKey;
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#writeAdditionalPlayerData
	 */
	static void writeAdditionalPlayerData(LocalPlayer player, CompoundTag nbt) {
		nbt.putString("Dimension", player.level.dimensionType().getFileSuffix());
		// TODO: handle everything in ServerPlayerEntity (but nothing is completely required)
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getWorldInfoNbt
	 */
	static CompoundTag getWorldInfoNbt(ClientLevel world, CompoundTag playerNBT) {
		ClientLevelData clientInfo = world.getLevelData();
		LevelSettings worldSettings = new LevelSettings("LevelName", GameType.CREATIVE, false,
				clientInfo.getDifficulty(), true, new GameRules(), DataPackConfig.DEFAULT);
		RegistryAccess dynamicRegistries = world.registryAccess();
		Registry<DimensionType> dimType = dynamicRegistries.dimensionTypes();
		Registry<Biome> biomes = dynamicRegistries.registryOrThrow(Registry.BIOME_REGISTRY);
		// TODO: figure out why using the world's registries causes a crash with
		// Missing registry: ResourceKey[minecraft:root / minecraft:worldgen/noise_settings]
		// in the following call (probably something with it not being sync'd?)
		Registry<NoiseGeneratorSettings> dimSettings = DYNAMIC_REGISTRIES.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
		WorldGenSettings genSettings = WorldGenSettings.makeDefault(dimType, biomes, dimSettings);
		PrimaryLevelData worldInfo = new PrimaryLevelData(worldSettings, genSettings, Lifecycle.stable());
		return worldInfo.createTag(dynamicRegistries, playerNBT);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#VANILLA_VILLAGER_CAREERS
	 */
	static final Int2ObjectMap<BiMap<String, Integer>> VANILLA_VILLAGER_CAREERS = new Int2ObjectArrayMap<>();
	static {
		BiMap<String, Integer> farmer = HashBiMap.create(4);
		farmer.put("entity.minecraft.villager.farmer", 1);
		farmer.put("entity.minecraft.villager.fisherman", 2);
		farmer.put("entity.minecraft.villager.shepherd", 3);
		farmer.put("entity.minecraft.villager.fletcher", 4);
		BiMap<String, Integer> librarian = HashBiMap.create(2);
		librarian.put("entity.minecraft.villager.librarian", 1);
		librarian.put("entity.minecraft.villager.cartographer", 2);
		BiMap<String, Integer> priest = HashBiMap.create(1);
		priest.put("entity.minecraft.villager.cleric", 1);
		BiMap<String, Integer> blacksmith = HashBiMap.create(3);
		blacksmith.put("entity.minecraft.villager.armorer", 1);
		blacksmith.put("entity.minecraft.villager.weapon_smith", 2);
		blacksmith.put("entity.minecraft.villager.tool_smith", 3);
		BiMap<String, Integer> butcher = HashBiMap.create(2);
		butcher.put("entity.minecraft.villager.butcher", 1);
		butcher.put("entity.minecraft.villager.leatherworker", 2);
		BiMap<String, Integer> nitwit = HashBiMap.create(1);
		nitwit.put("entity.minecraft.villager.nitwit", 1);

		VANILLA_VILLAGER_CAREERS.put(0, farmer);
		VANILLA_VILLAGER_CAREERS.put(1, librarian);
		VANILLA_VILLAGER_CAREERS.put(2, priest);
		VANILLA_VILLAGER_CAREERS.put(3, blacksmith);
		VANILLA_VILLAGER_CAREERS.put(4, butcher);
		VANILLA_VILLAGER_CAREERS.put(5, nitwit);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getEntityX
	 */
	static double getEntityX(Entity e) {
		return e.getX();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getEntityY
	 */
	static double getEntityY(Entity e) {
		return e.getY();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getEntityZ
	 */
	static double getEntityZ(Entity e) {
		return e.getZ();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#setEntityPos
	 */
	static void setEntityPos(Entity e, double x, double y, double z) {
		e.setPosRaw(x, y, z);
	}
}
