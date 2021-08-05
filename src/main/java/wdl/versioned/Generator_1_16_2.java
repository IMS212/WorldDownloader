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
package wdl.versioned;


import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.PresetFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryWriteOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import wdl.WDL;
import wdl.config.settings.GeneratorSettings.Generator;

final class GeneratorFunctions {
	private GeneratorFunctions() { throw new AssertionError(); }
	private static final Logger LOGGER = LogManager.getLogger();

	/* (non-javadoc)
	 * @see VersionedFunctions#isAvailableGenerator
	 */
	static boolean isAvaliableGenerator(Generator generator) {
		return generator != Generator.CUSTOMIZED && generator != Generator.BUFFET;
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#makeGeneratorSettingsGui
	 */
	static Screen makeGeneratorSettingsGui(Generator generator, Screen parent,
			String generatorConfig, Consumer<String> callback) {
		// NOTE: These give SNBT values, but the actual code expects NBT.
		switch (generator) {
		case FLAT:
			return new PresetFlatWorldScreen(new GuiCreateFlatWorldProxy(parent, generatorConfig, callback));
		case SINGLE_BIOME_SURFACE:
		case SINGLE_BIOME_CAVES:
		case SINGLE_BIOME_FLOATING_ISLANDS:
			return new CreateBuffetWorldScreen(parent, HandlerFunctions.DYNAMIC_REGISTRIES, convertBiomeToConfig(callback), convertConfigToBiome(generatorConfig));
		default:
			LOGGER.warn("Generator lacks extra settings; cannot make a settings GUI: " + generator);
			return parent;
		}
	}

	private static Consumer<Biome> convertBiomeToConfig(Consumer<String> callback) {
		return biome -> {
			Registry<Biome> biomesReg = HandlerFunctions.DYNAMIC_REGISTRIES.registryOrThrow(Registry.BIOME_REGISTRY);
			ResourceLocation name = biomesReg.getKey(biome);
			String biomeName;
			if (name != null) {
				biomeName = name.toString();
			} else {
				LOGGER.warn("[WDL] Failed to get name for biome " + biome);
				biomeName = "minecraft:plains";
			}
			callback.accept(biomeName);
		};
	}

	private static Biome convertConfigToBiome(String config) {
		ResourceLocation name;
		try {
			name = new ResourceLocation(config);
		} catch (ResourceLocationException ex) {
			LOGGER.warn("[WDL] Failed to get biome for name " + config, ex);
			name = new ResourceLocation("minecraft", "plains");
		}
		Registry<Biome> biomesReg = HandlerFunctions.DYNAMIC_REGISTRIES.registryOrThrow(Registry.BIOME_REGISTRY);
		return biomesReg.get(name);
	}

	/**
	 * Fake implementation of {@link GuiCreateFlatWorldProxy} that allows use of
	 * {@link PresetFlatWorldScreen}.  Doesn't actually do anything; just passed in
	 * to the constructor to forward the information we need and to switch
	 * back to the main GUI afterwards.
	 */
	private static class GuiCreateFlatWorldProxy extends CreateFlatWorldScreen {
		private final Screen parent;
		private final Consumer<String> callback;

		public GuiCreateFlatWorldProxy(Screen parent, String config, Consumer<String> callback) {
			super(CreateWorldScreenProxy.create(),
					settings -> LOGGER.warn("[WDL] Unexpected GuiCreateFlatWorldProxy callback, ignoring: " + settings),
					convertConfigToSettings(config));
			this.parent = parent;
			this.callback = callback;
		}

		private static String convertSettingsToConfig(FlatLevelGeneratorSettings settings) {
			RegistryWriteOps<JsonElement> ops = RegistryWriteOps.create(JsonOps.INSTANCE,
					HandlerFunctions.DYNAMIC_REGISTRIES);
			return FlatLevelGeneratorSettings.CODEC
					.encodeStart(ops, settings)
					.resultOrPartial(LOGGER::error)
					.map(JsonElement::toString)
					.get();
		}

		private static FlatLevelGeneratorSettings convertConfigToSettings(String config) {
			RegistryReadOps<JsonElement> ops = RegistryReadOps.create(JsonOps.INSTANCE,
					WDL.getInstance().minecraft.getResourceManager(), HandlerFunctions.DYNAMIC_REGISTRIES);
			JsonObject jsonobject = config.isEmpty() ? new JsonObject() : GsonHelper.parse(config);
			return FlatLevelGeneratorSettings.CODEC
					.parse(ops, jsonobject)
					.resultOrPartial(LOGGER::error)
					.orElseGet(() -> {
						Registry<Biome> biomesReg = HandlerFunctions.DYNAMIC_REGISTRIES.registryOrThrow(Registry.BIOME_REGISTRY);
						return FlatLevelGeneratorSettings.getDefault(biomesReg);
					});
		}

		@Override
		public void init() {
			// The flat presets screen only can have a CreateFlatWorld screen as a parent.  Thus,
			// this proxy acts as its parent but directly changes to the real parent.
			minecraft.setScreen(parent);
			// We can't directly get a callback from the flat presets screen,
			// and the callback in the constructor is only used when the done button here is clicked.
			// Instead, call our callback when exiting this screen.
			callback.accept(convertSettingsToConfig(this.settings()));
		}

		@Override
		public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
			// Do nothing
		}
	}

	/**
	 * Further fake implementations, which make things annoying.  Needed because {@link PresetFlatWorldScreen} has
	 * <pre>Registry<Biome> registry = this.parentScreen.createWorldGui.field_238934_c_.func_239055_b_().func_243612_b(Registry.field_239720_u_);</pre>
	 *
	 * (field_238934_c_ is a WorldOptionsScreen, but CreateWorldScreen creates it in the constructor)
	 */
	private static class CreateWorldScreenProxy extends CreateWorldScreen {
		public CreateWorldScreenProxy(@Nullable Screen p_i242064_1_, LevelSettings p_i242064_2_,
				WorldGenSettings p_i242064_3_, @Nullable Path p_i242064_4_, DataPackConfig p_i242064_5_,
				RegistryAccess.RegistryHolder p_i242064_6_) {
			super(p_i242064_1_, p_i242064_2_, p_i242064_3_, p_i242064_4_, p_i242064_5_, p_i242064_6_);
		}

		public static CreateWorldScreenProxy create() {
			LevelSettings worldSettings = new LevelSettings("LevelName", GameType.CREATIVE, false,
					Difficulty.NORMAL, true, new GameRules(), DataPackConfig.DEFAULT);
			Registry<DimensionType> dimType = HandlerFunctions.DYNAMIC_REGISTRIES.dimensionTypes();
			Registry<Biome> biomes = HandlerFunctions.DYNAMIC_REGISTRIES.registryOrThrow(Registry.BIOME_REGISTRY);
			Registry<NoiseGeneratorSettings> dimSettings = HandlerFunctions.DYNAMIC_REGISTRIES.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
			WorldGenSettings genSettings = WorldGenSettings.makeDefault(dimType, biomes, dimSettings);
			return new CreateWorldScreenProxy(null, worldSettings, genSettings,
					null, DataPackConfig.DEFAULT, HandlerFunctions.DYNAMIC_REGISTRIES);
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#makeBackupToast
	 */
	static void makeBackupToast(String name, long fileSize) {
		// See GuiWorldEdit.createBackup
		Minecraft.getInstance().execute(() -> {
			ToastComponent guitoast = Minecraft.getInstance().getToasts();
			Component top = new TranslatableComponent("selectWorld.edit.backupCreated", name);
			Component bot = new TranslatableComponent("selectWorld.edit.backupSize", Mth.ceil(fileSize / 1048576.0));
			guitoast.addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_BACKUP, top, bot));
		});
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#makeBackupFailedToast
	 */
	static void makeBackupFailedToast(IOException ex) {
		// See GuiWorldEdit.createBackup
		String message = ex.getMessage();
		Minecraft.getInstance().execute(() -> {
			ToastComponent guitoast = Minecraft.getInstance().getToasts();
			// NOTE: vanilla translation string was missing (MC-137308) until 1.14
			Component top = new TranslatableComponent("wdl.toast.backupFailed");
			Component bot = new TextComponent(message);
			guitoast.addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_BACKUP, top, bot));
		});
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#VOID_FLAT_CONFIG
	 */
	static final String VOID_FLAT_CONFIG = "{features:0,lakes:0,layers:[{block:\"minecraft:air\",height:1b}],biome:\"minecraft:the_void\",structures:{structures:{}}}";

	/* (non-javadoc)
	 * @see VersionedFunctions#writeGeneratorOptions
	 *
	 * An example (normal terrain):
	 * 
	 * <pre>
	 * + WorldGenSettings: 4 entries
	 *   + bonus_chest: 0
	 *   + generate_features: 1
	 *   + seed: -4511540289422318412
	 *   + dimensions: 3 entries
	 *     + minecraft:overworld: 2 entries
	 *     | + type: minecraft:overworld
	 *     | + generator: 4 entries
	 *     |   + seed: -4511540289422318412
	 *     |   + settings: minecraft:overworld
	 *     |   + type: minecraft:noise
	 *     |   + biome_source: 3 entries
	 *     |     + large_biomes: 0
	 *     |     + seed: -4511540289422318412
	 *     |     + type: minecraft:vanilla_layered
	 *     + minecraft:the_end: 2 entries
	 *     | + type: minecraft:the_end
	 *     | + generator: 4 entries
	 *     |   + seed: -4511540289422318412
	 *     |   + settings: minecraft:end
	 *     |   + type: minecraft:noise
	 *     |   + biome_source: 2 entries
	 *     |     + seed: -4511540289422318412
	 *     |     + type: minecraft:the_end
	 *     + minecraft:the_nether: 2 entries
	 *       + type: minecraft:the_nether
	 *       + generator: 4 entries
	 *         + seed: -4511540289422318412
	 *         + settings: minecraft:nether
	 *         + type: minecraft:noise
	 *         + biome_source: 3 entries
	 *           + seed: -4511540289422318412
	 *           + preset: minecraft:nether
	 *           + type: minecraft:multi_noise
	 * </pre>
	 */
	static void writeGeneratorOptions(CompoundTag worldInfoNBT, long randomSeed, boolean mapFeatures, String generatorName, String generatorOptions, int generatorVersion) {
		CompoundTag genSettings = new CompoundTag();
		genSettings.putBoolean("bonus_chest", false);
		genSettings.putBoolean("generate_features", mapFeatures);
		genSettings.putLong("seed", randomSeed);
		CompoundTag dimensions = new CompoundTag();
		dimensions.put("minecraft:overworld", createOverworld(randomSeed, generatorName, generatorOptions, generatorVersion));
		dimensions.put("minecraft:the_end", createDefaultEnd(randomSeed));
		dimensions.put("minecraft:the_nether", createDefaultNether(randomSeed));
		genSettings.put("dimensions", dimensions);
		worldInfoNBT.put("WorldGenSettings", genSettings);
	}

	private static CompoundTag createOverworld(long seed, String name, String options, int version) {
		// TODO: This implementation is rather jank (hardcoding strings that are present
		// in GeneratorSettings)
		if (name.equals("flat")) {
			return createFlatGenerator(seed, options);
		}
		if (name.equals("single_biome_surface") || name.equals("single_biome_caves") || name.equals("single_biome_floating_islands")) {
			if (name.equals("single_biome_caves")) {
				return createBuffetGenerator(seed, "minecraft:caves", options);
			} else if (name.equals("single_biome_floating_islands")) {
				return createBuffetGenerator(seed, "minecraft:floating_islands", options);
			} else {
				return createBuffetGenerator(seed, "minecraft:overworld", options);
			}
		}
		boolean isAmplified = name.equals("amplified");
		boolean isLargeBiomes = name.equals("largeBiomes");
		boolean isLegacy = name.equals("default_1_1") || (name.equals("default") && version == 0);
		return createOverworldGenerator(seed, isAmplified, isLargeBiomes, isLegacy);
	}

	private static CompoundTag createFlatGenerator(long seed, String options) {
		CompoundTag result = new CompoundTag();
		result.putString("type", "minecraft:overworld");
		CompoundTag generator = new CompoundTag();
		generator.putString("type", "minecraft:flat");
		CompoundTag settings;
		try {
			settings = TagParser.parseTag(options);
		} catch (CommandSyntaxException e) {
			settings = new CompoundTag();
		}
		generator.put("settings", settings);
		result.put("generator", generator);
		return result;
	}

	private static CompoundTag createBuffetGenerator(long seed, String settings, String biome) {
		CompoundTag result = new CompoundTag();
		result.putString("type", "minecraft:overworld");
		CompoundTag generator = new CompoundTag();
		generator.putString("type", "minecraft:noise");
		generator.putString("settings", settings);
		generator.putLong("seed", seed);
		CompoundTag biomeSource = new CompoundTag();
		biomeSource.putString("type", "minecraft:fixed");
		biomeSource.putString("biome", biome);
		generator.put("biome_source", biomeSource);
		result.put("generator", generator);
		return result;
	}

	private static CompoundTag createOverworldGenerator(long seed, boolean amplified, boolean largeBiomes, boolean legacy) {
		// Refer to WorldGenSetting.func_233427_a_ and func_233423_a_
		CompoundTag result = new CompoundTag();
		result.putString("type", "minecraft:overworld");
		CompoundTag generator = new CompoundTag();
		generator.putLong("seed", seed);
		generator.putString("settings", amplified ? "minecraft:amplified" : "minecraft:overworld");
		generator.putString("type", "minecraft:noise");
		CompoundTag biomeSource = new CompoundTag();
		biomeSource.putBoolean("large_biomes", largeBiomes);
		biomeSource.putLong("seed", seed);
		biomeSource.putString("type", "minecraft:vanilla_layered");
		if (legacy) {
			biomeSource.putBoolean("legacy_biome_init_layer", true);
		}
		generator.put("biome_source", biomeSource);
		result.put("generator", generator);
		return result;
	}

	// TODO: These should be configurable
	private static CompoundTag createDefaultNether(long seed) {
		CompoundTag result = new CompoundTag();
		result.putString("type", "minecraft:the_nether");
		CompoundTag generator = new CompoundTag();
		generator.putLong("seed", seed);
		generator.putString("settings", "minecraft:nether");
		generator.putString("type", "minecraft:noise");
		CompoundTag biomeSource = new CompoundTag();
		biomeSource.putLong("seed", seed);
		biomeSource.putString("preset", "minecraft:nether");
		biomeSource.putString("type", "minecraft:multi_noise");
		generator.put("biome_source", biomeSource);
		result.put("generator", generator);
		return result;
	}

	private static CompoundTag createDefaultEnd(long seed) {
		CompoundTag result = new CompoundTag();
		result.putString("type", "minecraft:the_end");
		CompoundTag generator = new CompoundTag();
		generator.putLong("seed", seed);
		generator.putString("settings", "minecraft:end");
		generator.putString("type", "minecraft:noise");
		CompoundTag biomeSource = new CompoundTag();
		biomeSource.putLong("seed", seed);
		biomeSource.putString("type", "minecraft:the_end");
		generator.put("biome_source", biomeSource);
		result.put("generator", generator);
		return result;
	}
}
