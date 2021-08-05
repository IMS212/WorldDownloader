/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2018-2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ChunkTickList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import org.jetbrains.annotations.Nullable;
import wdl.config.settings.MiscSettings;
import wdl.versioned.IDimensionWrapper;
import wdl.versioned.ISaveHandlerWrapper;
import wdl.versioned.VersionedFunctions;

/**
 * Alternative implementation of {@link ChunkStorage} that handles editing
 * WDL-specific properties of chunks as they are being saved.
 *
 * This variant is used for chunks from 1.13 and later.
 */
abstract class WDLChunkLoaderBase extends ChunkStorage {

	/**
	 * Gets the save folder for the given WorldProvider, respecting Forge's
	 * dimension names if forge is present.
	 */
	protected static File getWorldSaveFolder(ISaveHandlerWrapper handler,
											 IDimensionWrapper dimension) {
		File baseFolder = handler.getWorldDirectory();
		// XXX No forge support at this time

		File dimensionFolder;
		if (WDL.serverProps.getValue(MiscSettings.FORCE_DIMENSION_TO_OVERWORLD)) {
			dimensionFolder = baseFolder;
		} else {
			@Nullable String dimName = dimension.getFolderName();
			if (dimName == null) {
				// Assume that this is the overworld.
				dimensionFolder = baseFolder;
			} else {
				dimensionFolder = new File(baseFolder, dimName);
			}
		}

		return new File(dimensionFolder, "region");
	}

	protected final WDL wdl;
	/**
	 * Location where chunks are saved.
	 *
	 * In this version, this directly is the region folder for the given dimension;
	 * for the overworld it is world/region and others it is world/DIM#/region.
	 */
	protected final File chunkSaveLocation;

	// XXX HACK this is burried deep, and probably shouldn't be directly accessed
	protected final Long2ObjectLinkedOpenHashMap<RegionFile> cache;

	@SuppressWarnings({ "resource", "unchecked" })
	protected WDLChunkLoaderBase(WDL wdl, File file) {
		super(file, null, /* enable flushing */true);
		this.wdl = wdl;
		this.chunkSaveLocation = file;
		IOWorker worker = ReflectionUtils.findAndGetPrivateField(this, ChunkStorage.class, IOWorker.class);
		RegionFileStorage rfc = ReflectionUtils.findAndGetPrivateField(worker, RegionFileStorage.class);
		this.cache = ReflectionUtils.findAndGetPrivateField(rfc, Long2ObjectLinkedOpenHashMap.class);
	}

	/**
	 * Saves the given chunk.
	 *
	 * Note that while the normal implementation swallows Exceptions, this
	 * version does not.
	 */
	public synchronized void saveChunk(Level world, ChunkAccess chunk) throws Exception {
		wdl.saveHandler.checkSessionLock();

		CompoundTag levelTag = writeChunkToNBT((LevelChunk)chunk, world);

		CompoundTag rootTag = new CompoundTag();
		rootTag.put("Level", levelTag);
		rootTag.putInt("DataVersion", VersionConstants.getDataVersion());

		write(chunk.getPos(), rootTag);

		wdl.unloadChunk(chunk.getPos());
	}

	/**
	 * Writes the given chunk, creating an NBT compound tag.
	 *
	 * Note that this does <b>not</b> override the private method
	 * {@link ChunkStorage#write(ChunkPos, CompoundTag)} (Chunk, World, NBTCompoundNBT)}.
	 * That method is private and cannot be overridden; plus, this version
	 * returns a tag rather than modifying the one passed as an argument.
	 *
	 * @param chunk
	 *            The chunk to write
	 * @param world
	 *            The world the chunk is in, used to determine the modified
	 *            time.
	 * @return A new CompoundNBT
	 */
	private CompoundTag writeChunkToNBT(LevelChunk chunk, Level world) {
		CompoundTag compound = new CompoundTag();

		ChunkPos chunkpos = chunk.getPos();
		compound.putInt("xPos", chunkpos.x);
		compound.putInt("zPos", chunkpos.z);
		compound.putLong("LastUpdate", world.getGameTime());
		compound.putLong("InhabitedTime", chunk.getInhabitedTime());
		compound.putString("Status", ChunkStatus.FULL.getName()); // Make sure that the chunk is considered fully generated
		UpgradeData upgradedata = chunk.getUpgradeData();

		if (!upgradedata.isEmpty()) {
			compound.put("UpgradeData", upgradedata.write());
		}

		LevelChunkSection[] chunkSections = chunk.getSections();
		ListTag chunkSectionList = new ListTag();
		LevelLightEngine worldlightmanager = world.getChunkSource().getLightEngine();

		// XXX: VersionedFunctions.hasSkyLight is inapplicable here presumably, but it might still need to be used somehow
		for (int y = -1; y < 17; ++y) {
			final int f_y = y; // Compiler food
			LevelChunkSection chunkSection = Arrays.stream(chunkSections)
					.filter(section -> section != null && section.bottomBlockY() >> 4 == f_y)
					.findFirst()
					.orElse(LevelChunk.EMPTY_SECTION);
			DataLayer blocklightArray = worldlightmanager.getLayerListener(LightLayer.BLOCK)
					.getDataLayerData(SectionPos.of(chunkpos, y));
			DataLayer skylightArray = worldlightmanager.getLayerListener(LightLayer.SKY)
					.getDataLayerData(SectionPos.of(chunkpos, y));
			if (chunkSection != LevelChunk.EMPTY_SECTION || blocklightArray != null || skylightArray != null) {
				CompoundTag sectionNBT = new CompoundTag();
				sectionNBT.putByte("Y", (byte) (y & 255));
				if (chunkSection != LevelChunk.EMPTY_SECTION) {
					chunkSection.getStates().write(sectionNBT, "Palette", "BlockStates");
				}

				if (blocklightArray != null && !blocklightArray.isEmpty()) {
					sectionNBT.putByteArray("BlockLight", blocklightArray.getData());
				}

				if (skylightArray != null && !skylightArray.isEmpty()) {
					sectionNBT.putByteArray("SkyLight", skylightArray.getData());
				}

				chunkSectionList.add(sectionNBT);
			}
		}

		compound.put("Sections", chunkSectionList);

		if (chunk.isLightCorrect()) {
			compound.putBoolean("isLightOn", true);
		}

		ChunkBiomeContainer biomes = chunk.getBiomes();
		if (biomes != null) {
			compound.putIntArray("Biomes", biomes.writeBiomes());
		}

		chunk.setLastSaveHadEntities(false);
		ListTag entityList = getEntityList(chunk);
		compound.put("Entities", entityList);

		ListTag tileEntityList = getTileEntityList(chunk);
		compound.put("TileEntities", tileEntityList);

		// XXX: Note: This was re-sorted on mojang's end; I've undone that.
		if (world.getBlockTicks() instanceof ServerTickList) {
			compound.put("TileTicks", ((ServerTickList<?>) world.getBlockTicks()).save(chunkpos));
		}
		if (world.getLiquidTicks() instanceof ServerTickList) {
			compound.put("LiquidTicks", ((ServerTickList<?>) world.getLiquidTicks()).save(chunkpos));
		}

		compound.put("PostProcessing", listArrayToTag(chunk.getPostProcessing()));

		if (chunk.getBlockTicks() instanceof ProtoTickList) {
			compound.put("ToBeTicked", ((ProtoTickList<?>) chunk.getBlockTicks()).save());
		}

		// XXX: These are new, and they might conflict with the other one.  Not sure which should be used.
		if (chunk.getBlockTicks() instanceof ChunkTickList) {
			compound.put("TileTicks", ((ChunkTickList<?>) chunk.getBlockTicks())
					.save());
		}

		if (chunk.getLiquidTicks() instanceof ProtoTickList) {
			compound.put("LiquidsToBeTicked", ((ProtoTickList<?>) chunk.getLiquidTicks()).save());
		}

		if (chunk.getLiquidTicks() instanceof ChunkTickList) {
			compound.put("LiquidTicks", ((ChunkTickList<?>) chunk.getLiquidTicks())
					.save());
		}

		CompoundTag heightMaps = new CompoundTag();

		for (Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
			if (chunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
				heightMaps.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
			}
		}

		compound.put("Heightmaps", heightMaps);
		// TODO
		//compound.put("Structures",
		//		writeStructures(chunkpos, chunk.getStructureStarts(), chunk.getStructureReferences()));
		return compound;
	}

	protected abstract ListTag getEntityList(LevelChunk chunk);
	protected abstract ListTag getTileEntityList(LevelChunk chunk);

	/**
	 * Gets a count of how many chunks there are that still need to be written to
	 * disk. (Does not include any chunk that is currently being written to disk)
	 *
	 * @return The number of chunks that still need to be written to disk
	 */
	public synchronized int getNumPendingChunks() {
		return this.cache.size(); // XXX This is actually the number of regions
	}

	private ListTag listArrayToTag(ShortList[] list) {
		ListTag listnbt = new ListTag();

		for (ShortList shortlist : list) {
			ListTag sublist;
			if (shortlist != null) {
				sublist = VersionedFunctions.createShortListTag(shortlist.toShortArray());
			} else {
				sublist = VersionedFunctions.createShortListTag();
			}

			listnbt.add(sublist);
		}

		return listnbt;
	}

	/**
	 * Provided since the constructor changes between versions.
	 */
	protected RegionFile createRegionFile(File file) throws IOException {
		return new RegionFile(file, this.chunkSaveLocation, /*enable flushing*/false);
	}

	public void flush() {
		this.flushWorker();
	}
}