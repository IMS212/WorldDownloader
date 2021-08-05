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
package wdl;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import wdl.MapDataHandler.MapDataResult;
import wdl.api.IBlockEventListener;
import wdl.api.IChatMessageListener;
import wdl.api.IGuiHooksListener;
import wdl.api.IPluginChannelListener;
import wdl.api.IWorldLoadListener;
import wdl.api.WDLApi;
import wdl.api.WDLApi.ModInfo;
import wdl.config.settings.GeneratorSettings;
import wdl.gui.GuiTurningCameraBase;
import wdl.gui.GuiWDL;
import wdl.gui.GuiWDLAbout;
import wdl.gui.GuiWDLChunkOverrides;
import wdl.gui.GuiWDLPermissions;
import wdl.gui.widget.WDLButton;
import wdl.handler.HandlerException;
import wdl.handler.block.BlockHandler;
import wdl.handler.blockaction.BlockActionHandler;
import wdl.handler.entity.EntityHandler;
import wdl.update.WDLUpdateChecker;
import wdl.versioned.VersionedFunctions;

/**
 * Handles all of the events for WDL.
 *
 * These should be called regardless of whether downloading is
 * active; they handle that logic themselves.
 */
public class WDLEvents {
	public static void createListener(WDL wdl) {
		// TODO: Actually store this instance somewhere, instead of having it just floating about
		WDLEvents wdlEvents = new WDLEvents(wdl);
		WDLHooks.listener = new HooksListener(wdlEvents);
	}

	private WDLEvents(WDL wdl) {
		this.wdl = wdl;
	}

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * If set, enables the profiler.  For unknown reasons, the profiler seems
	 * to use up some memory even when not enabled; see
	 * <a href="https://github.com/Pokechu22/WorldDownloader/pull/77">pull request 77</a>
	 * for more information.
	 *
	 * The compiler should eliminate all references to the profiler when set to false,
	 * as per <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1-110-C">JLS §13.1</a>
	 * constants must be inlined.  It is not guaranteed that the compiler eliminates
	 * code in an <code>if (false)</code> condition (per JLS §14.9.1) but javac does
	 * optimize it out, as may be verified by javap.
	 */
	private static final boolean ENABLE_PROFILER = false;
	private static final ProfilerFiller PROFILER = ENABLE_PROFILER ? Minecraft.getInstance().getProfiler() : null;

	private final WDL wdl;

	/**
	 * Must be called after the static World object in Minecraft has been
	 * replaced.
	 */
	public void onWorldLoad(ClientLevel world) {
		if (ENABLE_PROFILER) PROFILER.push("Core");

		if (wdl.minecraft.isLocalServer()) {
			// Don't do anything else in single player

			if (ENABLE_PROFILER) PROFILER.pop();  // "Core"
			return;
		}

		// If already downloading
		if (WDL.downloading) {
			// If not currently saving, stop the current download and start
			// saving now
			if (!WDL.saving) {
				wdl.saveForWorldChange();
			}

			if (ENABLE_PROFILER) PROFILER.pop();  // "Core"
			return;
		}

		boolean sameServer = wdl.loadWorld();

		WDLUpdateChecker.startIfNeeded();  // TODO: Always check for updates, even in single player

		if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

		for (ModInfo<IWorldLoadListener> info : WDLApi
				.getImplementingExtensions(IWorldLoadListener.class)) {
			if (ENABLE_PROFILER) PROFILER.push(info.id);
			info.mod.onWorldLoad(world, sameServer);
			if (ENABLE_PROFILER) PROFILER.pop();  // info.id
		}
	}

	/**
	 * Must be called when a chunk is no longer needed and is about to be removed.
	 */
	public void onChunkNoLongerNeeded(LevelChunk unneededChunk) {
		if (!WDL.downloading) { return; }

		if (unneededChunk == null) {
			return;
		}

		if (WDLPluginChannels.canSaveChunk(unneededChunk)) {
			WDLMessages.chatMessageTranslated(
					WDL.serverProps,
					WDLMessageTypes.ON_CHUNK_NO_LONGER_NEEDED,
					"wdl.messages.onChunkNoLongerNeeded.saved", unneededChunk.getPos().x, unneededChunk.getPos().z);
			wdl.saveChunk(unneededChunk);
		} else {
			WDLMessages.chatMessageTranslated(
					WDL.serverProps,
					WDLMessageTypes.ON_CHUNK_NO_LONGER_NEEDED,
					"wdl.messages.onChunkNoLongerNeeded.didNotSave", unneededChunk.getPos().x, unneededChunk.getPos().z);
		}
	}

	/**
	 * Must be called when a GUI that receives item stacks from the server is
	 * shown.
	 */
	public void onItemGuiOpened() {
		if (!WDL.downloading) { return; }

		HitResult result = wdl.minecraft.hitResult;
		if (result == null) {
			// This case previously was hit via https://bugs.mojang.com/browse/MC-79925
			// but that was fixed in 1.14, so this should be impossible now.
			wdl.lastEntity = null;
			wdl.lastClickedBlock = null;
			return;
		}

		switch (result.getType()) {
		case ENTITY:
			wdl.lastEntity = ((EntityHitResult)result).getEntity();
			wdl.lastClickedBlock = null;
			break;
		case BLOCK:
			wdl.lastEntity = null;
			wdl.lastClickedBlock = ((BlockHitResult)result).getBlockPos();
			break;
		case MISS:
			wdl.lastEntity = null;
			wdl.lastClickedBlock = null;
		}
	}

	/**
	 * Must be called when a GUI that triggered an onItemGuiOpened is no longer
	 * shown.
	 */
	public boolean onItemGuiClosed() {
		if (!WDL.downloading) { return true; }

		AbstractContainerMenu windowContainer = wdl.windowContainer;

		if (windowContainer == null ||
				ReflectionUtils.isCreativeContainer(windowContainer.getClass())) {
			// Can't do anything with null containers or the creative inventory
			return true;
		}

		Entity ridingEntity = wdl.player.getVehicle();
		if (ridingEntity != null) {
			// Check for ridden entities.  See EntityHandler.checkRiding for
			// more info about why this is useful.
			EntityHandler<?, ?> handler = EntityHandler.getHandler(ridingEntity.getClass(), windowContainer.getClass());
			if (handler != null) {
				if (handler.checkRidingCasting(windowContainer, ridingEntity)) {
					if (!WDLPluginChannels.canSaveEntities(
							ridingEntity.xChunk,
							ridingEntity.zChunk)) {
						// Run this check now that we've confirmed that we're saving
						// the entity being ridden. If we're riding a pig but opening
						// a chest in another chunk, that should go to the other check.
						WDLMessages.chatMessageTranslated(WDL.serverProps,
								WDLMessageTypes.ON_GUI_CLOSED_INFO, "wdl.messages.onGuiClosedInfo.cannotSaveEntities");
						return true;
					}

					try {
						Component msg = handler.copyDataCasting(windowContainer, ridingEntity, true);
						WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
						return true;
					} catch (HandlerException e) {
						WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey, e.args);
						return false;
					}
				}
			} else {
				// A null handler is perfectly normal -- consider a player
				// riding a pig and then opening a chest
			}
		}

		// If the last thing clicked was an ENTITY
		Entity entity = wdl.lastEntity;
		if (entity != null) {
			if (!WDLPluginChannels.canSaveEntities(entity.xChunk, entity.zChunk)) {
				WDLMessages.chatMessageTranslated(WDL.serverProps,
						WDLMessageTypes.ON_GUI_CLOSED_INFO, "wdl.messages.onGuiClosedInfo.cannotSaveEntities");
				return true;
			}

			EntityHandler<?, ?> handler = EntityHandler.getHandler(entity.getClass(), windowContainer.getClass());
			if (handler != null) {
				try {
					Component msg = handler.copyDataCasting(windowContainer, entity, true);
					WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
					return true;
				} catch (HandlerException e) {
					WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey, e.args);
					return false;
				}
			} else {
				return false;
			}
		}

		// Else, the last thing clicked was a BLOCK ENTITY
		if (wdl.lastClickedBlock == null) {
			WDLMessages.chatMessageTranslated(WDL.serverProps,
					WDLMessageTypes.ON_GUI_CLOSED_WARNING,
					"wdl.messages.onGuiClosedWarning.noCoordinates");
			return true; // nothing else can handle this
		}

		// Get the block entity which we are going to update the inventory for
		BlockEntity te = wdl.worldClient.getBlockEntity(wdl.lastClickedBlock);

		if (te == null) {
			//TODO: Is this a good way to stop?  Is the event truely handled here?
			WDLMessages.chatMessageTranslated(
					WDL.serverProps,
					WDLMessageTypes.ON_GUI_CLOSED_WARNING,
					"wdl.messages.onGuiClosedWarning.couldNotGetTE", wdl.lastClickedBlock);
			return true;
		}

		//Permissions check.
		if (!WDLPluginChannels.canSaveContainers(te.getBlockPos().getX() >> 4, te
				.getBlockPos().getZ() >> 4)) {
			WDLMessages.chatMessageTranslated(WDL.serverProps,
					WDLMessageTypes.ON_GUI_CLOSED_INFO, "wdl.messages.onGuiClosedInfo.cannotSaveTileEntities");
			return true;
		}

		BlockHandler<? extends BlockEntity, ? extends AbstractContainerMenu> handler =
				BlockHandler.getHandler(te.getClass(), wdl.windowContainer.getClass());
		if (handler != null) {
			try {
				Component msg = handler.handleCasting(wdl.lastClickedBlock, wdl.windowContainer,
						te, wdl.worldClient, wdl::saveTileEntity);
				WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
				return true;
			} catch (HandlerException e) {
				WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey, e.args);
				return false;
			}
		} else if (wdl.windowContainer instanceof ChestMenu
				&& te instanceof EnderChestBlockEntity) {
			PlayerEnderChestContainer inventoryEnderChest = wdl.player
					.getEnderChestInventory();
			int inventorySize = inventoryEnderChest.getContainerSize();
			int containerSize = wdl.windowContainer.slots.size();

			for (int i = 0; i < containerSize && i < inventorySize; i++) {
				Slot slot = wdl.windowContainer.getSlot(i);
				if (slot.hasItem()) {
					inventoryEnderChest.setItem(i, slot.getItem());
				}
			}

			WDLMessages.chatMessageTranslated(WDL.serverProps,
					WDLMessageTypes.ON_GUI_CLOSED_INFO, "wdl.messages.onGuiClosedInfo.savedTileEntity.enderChest");
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Must be called when a block event/block action packet is received.
	 */
	public void onBlockEvent(BlockPos pos, Block block, int data1, int data2) {
		if (!WDL.downloading) { return; }

		if (!WDLPluginChannels.canSaveTileEntities(pos.getX() >> 4,
				pos.getZ() >> 4)) {
			return;
		}

		BlockEntity blockEntity = wdl.worldClient.getBlockEntity(pos);
		if (blockEntity == null) {
			return;
		}

		BlockActionHandler<? extends Block, ? extends BlockEntity> handler =
				BlockActionHandler.getHandler(block.getClass(), blockEntity.getClass());
		if (handler != null) {
			try {
				Component msg = handler.handleCasting(pos, block, blockEntity,
						data1, data2, wdl.worldClient, wdl::saveTileEntity);
				WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
			} catch (HandlerException e) {
				WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey, e.args);
			}
		}
	}

	/**
	 * Must be called when a Map Data packet is received, to store the image on
	 * the map item.
	 */
	public void onMapDataLoaded(int mapID, @Nonnull MapItemSavedData mapData) {
		if (!WDL.downloading) { return; }

		if (!WDLPluginChannels.canSaveMaps()) {
			return;
		}

		// Assume that the current dimension is the right one
		LocalPlayer player = wdl.player;
		assert player != null;
		MapDataResult result = MapDataHandler.repairMapData(mapID, mapData, wdl.player);

		wdl.newMapDatas.put(mapID, result.map);

		WDLMessages.chatMessageTranslated(WDL.serverProps,
				WDLMessageTypes.ON_MAP_SAVED, "wdl.messages.onMapSaved", mapID, result.toComponent());
	}

	/**
	 * Must be called whenever a plugin channel message / custom payload packet
	 * is received.
	 */
	public void onPluginChannelPacket(ClientPacketListener sender,
			String channel, byte[] bytes) {
		WDLPluginChannels.onPluginChannelPacket(sender, channel, bytes);
	}

	/**
	 * Must be called when an entity is about to be removed from the world.
	 */
	public void onRemoveEntityFromWorld(Entity entity) {
		// If the entity is being removed and it's outside the default tracking
		// range, go ahead and remember it until the chunk is saved.
		if (WDL.downloading && entity != null
				&& WDLPluginChannels.canSaveEntities(entity.xChunk,
						entity.zChunk)) {
			if (!EntityUtils.isEntityEnabled(entity)) {
				WDLMessages.chatMessageTranslated(
						WDL.serverProps,
						WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.allowingRemoveUserPref", entity);
				return;
			}

			int threshold = EntityUtils.getEntityTrackDistance(entity);

			if (threshold < 0) {
				WDLMessages.chatMessageTranslated(
						WDL.serverProps,
						WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.allowingRemoveUnrecognizedDistance", entity);
				return;
			}

			int serverViewDistance = 10; // XXX hardcoded for now

			if (EntityUtils.isWithinSavingDistance(entity, wdl.player,
					threshold, serverViewDistance)) {
				WDLMessages.chatMessageTranslated(
						WDL.serverProps,
						WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.savingDistance", entity,
						entity.position().toString(), wdl.player.position(), threshold, serverViewDistance);
				ChunkPos pos = new ChunkPos(entity.xChunk, entity.zChunk);
				UUID uuid = entity.getUUID();
				if (wdl.entityPositions.containsKey(uuid)) {
					// Remove previous entity, to avoid saving the same one in multiple chunks.
					ChunkPos prevPos = wdl.entityPositions.get(uuid);
					boolean removedSome = wdl.newEntities.get(pos).removeIf(e -> e.getUUID().equals(uuid));
					LOGGER.info("Replacing entity with UUID {} previously located at {} with new position {}.  There was an entity at old position (should be true): {}", uuid, prevPos, pos, removedSome);
				}
				wdl.newEntities.put(pos, entity);
				wdl.entityPositions.put(uuid, pos);
			} else {
				WDLMessages.chatMessageTranslated(
						WDL.serverProps,
						WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.allowingRemoveDistance", entity,
						entity.position().toString(), wdl.player.position(), threshold, serverViewDistance);
			}
		}
	}

	/**
	 * Called upon any chat message.  Used for getting the seed.
	 */
	public void onChatMessage(String msg) {
		if (WDL.downloading && msg.startsWith("Seed: ")) {
			String seed = msg.substring(6);
			if (seed.startsWith("[") && seed.endsWith("]")) {
				// In 1.13, the seed is enclosed by brackets (and is also selectable on click)
				// We don't want those brackets.
				seed = seed.substring(1, seed.length() - 1);
			}
			wdl.worldProps.setValue(GeneratorSettings.SEED, seed);

			if (wdl.worldProps.getValue(GeneratorSettings.GENERATOR) ==
					GeneratorSettings.Generator.VOID) {

				wdl.worldProps.setValue(GeneratorSettings.GENERATOR,
						GeneratorSettings.Generator.DEFAULT);

				WDLMessages.chatMessageTranslated(WDL.serverProps,
						WDLMessageTypes.INFO, "wdl.messages.generalInfo.seedAndGenSet", seed);
			} else {
				WDLMessages.chatMessageTranslated(WDL.serverProps,
						WDLMessageTypes.INFO, "wdl.messages.generalInfo.seedSet", seed);
			}
		}
	}

	public static class HooksListener implements WDLHooks.IHooksListener {
		public HooksListener(WDLEvents wdlEvents) {
			this.wdlEvents = wdlEvents;
			this.wdl = wdlEvents.wdl;
		}
		private final WDLEvents wdlEvents;
		private final WDL wdl;

		@Override
		public void onWorldClientTick(ClientLevel sender) {
			try {
				if (ENABLE_PROFILER) PROFILER.push("wdl");

				if (sender != wdl.worldClient) {
					if (ENABLE_PROFILER) PROFILER.push("onWorldLoad");
					if (WDL.worldLoadingDeferred) {
						return;
					}

					wdlEvents.onWorldLoad(sender);
					if (ENABLE_PROFILER) PROFILER.pop();  // "onWorldLoad"
				} else {
					if (ENABLE_PROFILER) PROFILER.push("inventoryCheck");
					if (WDL.downloading && wdl.player != null) {
						if (wdl.player.containerMenu != wdl.windowContainer) {
							if (wdl.player.containerMenu == wdl.player.inventoryMenu) {
								boolean handled;

								if (ENABLE_PROFILER) PROFILER.push("onItemGuiClosed");
								if (ENABLE_PROFILER) PROFILER.push("Core");
								handled = wdlEvents.onItemGuiClosed();
								if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

								AbstractContainerMenu container = wdl.player.containerMenu;
								if (wdl.lastEntity != null) {
									Entity entity = wdl.lastEntity;

									for (ModInfo<IGuiHooksListener> info : WDLApi
											.getImplementingExtensions(IGuiHooksListener.class)) {
										if (handled) {
											break;
										}

										if (ENABLE_PROFILER) PROFILER.push(info.id);
										handled = info.mod.onEntityGuiClosed(
												sender, entity, container);
										if (ENABLE_PROFILER) PROFILER.pop();  // info.id
									}

									if (!handled) {
										WDLMessages.chatMessageTranslated(
												WDL.serverProps,
												WDLMessageTypes.ON_GUI_CLOSED_WARNING,
												"wdl.messages.onGuiClosedWarning.unhandledEntity", entity);
									}
								} else if (wdl.lastClickedBlock != null) {
									BlockPos pos = wdl.lastClickedBlock;
									for (ModInfo<IGuiHooksListener> info : WDLApi
											.getImplementingExtensions(IGuiHooksListener.class)) {
										if (handled) {
											break;
										}

										if (ENABLE_PROFILER) PROFILER.push(info.id);
										handled = info.mod.onBlockGuiClosed(
												sender, pos, container);
										if (ENABLE_PROFILER) PROFILER.pop();  // info.id
									}

									if (!handled) {
										WDLMessages.chatMessageTranslated(
												WDL.serverProps,
												WDLMessageTypes.ON_GUI_CLOSED_WARNING,
												"wdl.messages.onGuiClosedWarning.unhandledTileEntity", pos, sender.getBlockEntity(pos));
									}
								}

								if (ENABLE_PROFILER) PROFILER.pop();  // onItemGuiClosed
							} else {
								if (ENABLE_PROFILER) PROFILER.push("onItemGuiOpened");
								if (ENABLE_PROFILER) PROFILER.push("Core");
								wdlEvents.onItemGuiOpened();
								if (ENABLE_PROFILER) PROFILER.pop();  // "Core"
								if (ENABLE_PROFILER) PROFILER.pop();  // "onItemGuiOpened"
							}

							wdl.windowContainer = wdl.player.containerMenu;
						}
					}
					if (ENABLE_PROFILER) PROFILER.pop();  // "inventoryCheck"
				}

				if (ENABLE_PROFILER) PROFILER.push("camera");
				GuiTurningCameraBase.onWorldTick();
				if (ENABLE_PROFILER) PROFILER.pop();  // "camera"
				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onWorldClientTick event");
			}
		}
		@Override
		public void onWorldClientRemoveEntityFromWorld(ClientLevel sender,
				int eid) {
			try {
				if (!WDL.downloading) { return; }

				if (ENABLE_PROFILER) PROFILER.push("wdl.onRemoveEntityFromWorld");

				Entity entity = sender.getEntity(eid);

				if (ENABLE_PROFILER) PROFILER.push("Core");
				wdlEvents.onRemoveEntityFromWorld(entity);
				if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onRemoveEntityFromWorld"
			} catch (Throwable e) {
				wdl.crashed(e,
						"WDL mod: exception in onWorldRemoveEntityFromWorld event");
			}
		}
		@Override
		public void onNHPCHandleChunkUnload(ClientPacketListener sender,
				ClientLevel world, ClientboundForgetLevelChunkPacket packet) {
			try {
				if (!wdl.minecraft.isSameThread()) {
					return;
				}

				if (!WDL.downloading) { return; }

				if (ENABLE_PROFILER) PROFILER.push("wdl.onChunkNoLongerNeeded");
				LevelChunk chunk = world.getChunk(packet.getX(), packet.getZ());

				if (ENABLE_PROFILER) PROFILER.push("Core");
				wdlEvents.onChunkNoLongerNeeded(chunk);
				if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onChunkNoLongerNeeded"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onNHPCHandleChunkUnload event");
			}
		}

		@Override
		public void onNHPCHandleChat(ClientPacketListener sender,
				ClientboundChatPacket packet) {
			try {
				if (!wdl.minecraft.isSameThread()) {
					return;
				}

				if (!WDL.downloading) { return; }

				if (ENABLE_PROFILER) PROFILER.push("wdl.onChatMessage");

				String chatMessage = packet.getMessage().getString();

				if (ENABLE_PROFILER) PROFILER.push("Core");
				wdlEvents.onChatMessage(chatMessage);
				if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

				for (ModInfo<IChatMessageListener> info : WDLApi
						.getImplementingExtensions(IChatMessageListener.class)) {
					if (ENABLE_PROFILER) PROFILER.push(info.id);
					info.mod.onChat(wdl.worldClient, chatMessage);
					if (ENABLE_PROFILER) PROFILER.pop();  // info.id
				}

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onChatMessage"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onNHPCHandleChat event");
			}
		}
		@Override
		public void onNHPCHandleMaps(ClientPacketListener sender,
				ClientboundMapItemDataPacket packet) {
			try {
				if (!wdl.minecraft.isSameThread()) {
					return;
				}

				if (!WDL.downloading) { return; }

				if (ENABLE_PROFILER) PROFILER.push("wdl.onMapDataLoaded");

				MapItemSavedData mapData = VersionedFunctions.getMapData(wdl.worldClient, packet);

				if (mapData != null) {
					if (ENABLE_PROFILER) PROFILER.push("Core");
					wdlEvents.onMapDataLoaded(packet.getMapId(), mapData);
					if (ENABLE_PROFILER) PROFILER.pop();  // "Core"
				} else {
					LOGGER.warn("Received a null map data: " + packet.getMapId());
				}

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onMapDataLoaded"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onNHPCHandleMaps event");
			}
		}
		@Override
		public void onNHPCHandleCustomPayload(ClientPacketListener sender,
				ClientboundCustomPayloadPacket packet) {
			try {
				if (!wdl.minecraft.isSameThread()) {
					return;
				}
				if (ENABLE_PROFILER) PROFILER.push("wdl.onPluginMessage");

				if (ENABLE_PROFILER) PROFILER.push("Parse");
				String channel = packet.getIdentifier().toString(); // 1.13: ResourceLocation -> String; otherwise no-op
				ByteBuf buf = packet.getData();
				int refCnt = buf.refCnt();
				if (refCnt <= 0) {
					// The buffer has already been released.  Just break out now.
					// This happens with e.g. the MC|TrList packet (villager trade list),
					// which closes the buffer after reading it.
					if (ENABLE_PROFILER) PROFILER.pop();  // "Parse"
					if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onPluginMessage"
					return;
				}

				// Something else may have already read the payload; return to the start
				buf.markReaderIndex();
				buf.readerIndex(0);
				byte[] payload = new byte[buf.readableBytes()];
				buf.readBytes(payload);
				// OK, now that we've done our reading, return to where it was before
				// (which could be the end, or other code might not have read it yet)
				buf.resetReaderIndex();
				// buf will be released by the packet handler, eventually.
				// It definitely is NOT our responsibility to release it, as
				// doing so would probably break other code outside of wdl.
				// Perhaps we might want to call retain once at the start of this method
				// and then release at the end, but that feels excessive (since there
				// _shouldn't_ be multiple threads at play at this point, and if there
				// were we'd be in trouble anyways).

				if (ENABLE_PROFILER) PROFILER.pop();  // "Parse"

				if (ENABLE_PROFILER) PROFILER.push("Core");
				wdlEvents.onPluginChannelPacket(sender, channel, payload);
				if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

				for (ModInfo<IPluginChannelListener> info : WDLApi
						.getImplementingExtensions(IPluginChannelListener.class)) {
					if (ENABLE_PROFILER) PROFILER.push(info.id);
					info.mod.onPluginChannelPacket(wdl.worldClient, channel,
							payload);
					if (ENABLE_PROFILER) PROFILER.pop();  // info.id
				}

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onPluginMessage"
			} catch (Throwable e) {
				wdl.crashed(e,
						"WDL mod: exception in onNHPCHandleCustomPayload event");
			}
		}
		@Override
		public void onNHPCHandleBlockAction(ClientPacketListener sender,
				ClientboundBlockEventPacket packet) {
			try {
				if (!wdl.minecraft.isSameThread()) {
					return;
				}

				if (!WDL.downloading) { return; }

				if (ENABLE_PROFILER) PROFILER.push("wdl.onBlockEvent");

				BlockPos pos = packet.getPos();
				Block block = packet.getBlock();
				int data1 = packet.getB0();
				int data2 = packet.getB1();

				if (ENABLE_PROFILER) PROFILER.push("Core");
				wdlEvents.onBlockEvent(pos, block, data1, data2);
				if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

				for (ModInfo<IBlockEventListener> info : WDLApi
						.getImplementingExtensions(IBlockEventListener.class)) {
					if (ENABLE_PROFILER) PROFILER.push(info.id);
					info.mod.onBlockEvent(wdl.worldClient, pos, block,
							data1, data2);
					if (ENABLE_PROFILER) PROFILER.pop();  // info.id
				}

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onBlockEvent"
			} catch (Throwable e) {
				wdl.crashed(e,
						"WDL mod: exception in onNHPCHandleBlockAction event");
			}
		}
		@Override
		public void onNHPCDisconnect(ClientPacketListener sender, Component reason) {
			if (WDL.downloading) {
				// This is likely to be called from an unexpected thread, so queue a task
				// if on a different thread (execute will run it immediately if on the right thread)
				wdl.minecraft.execute(wdl::stopDownload);

				// This code was present on older versions of WDL which weren't missing
				// the onDisconnect handler before.
				// It presumably makes sure that the disconnect doesn't propagate to other state variables,
				// but I don't completely trust it
				try {
					Thread.sleep(2000L);
				} catch (InterruptedException e) { }
			}
		}
		@Override
		public void onCrashReportPopulateEnvironment(CrashReport report) {
			wdl.addInfoToCrash(report);
		}

		public class StartDownloadButton extends WDLButton {
			public StartDownloadButton(Screen menu, int x, int y, int width, int height) {
				super(x, y, width, height, new TextComponent(""));
				this.menu = menu;
			}

			// The GuiScreen containing this button, as a parent for other GUIs
			private final Screen menu;

			@Override
			public void beforeDraw() {
				final Component displayString;
				final boolean enabled;
				if (wdl.minecraft.isLocalServer()) {
					// Singleplayer
					displayString = new TranslatableComponent(
							"wdl.gui.ingameMenu.downloadStatus.singlePlayer");
					enabled = false;
				} else if (!WDLPluginChannels.canDownloadAtAll()) {
					if (WDLPluginChannels.canRequestPermissions()) {
						// Allow requesting permissions.
						displayString = new TranslatableComponent(
								"wdl.gui.ingameMenu.downloadStatus.request");
						enabled = true;
					} else {
						// Out of date plugin :/
						displayString = new TranslatableComponent(
								"wdl.gui.ingameMenu.downloadStatus.disabled");
						enabled = false;
					}
				} else if (WDL.saving) {
					// Normally not accessible; only happens as a major fallback...
					displayString = new TranslatableComponent(
							"wdl.gui.ingameMenu.downloadStatus.saving");
					enabled = false;
				} else if (WDL.downloading) {
					displayString = new TranslatableComponent(
							"wdl.gui.ingameMenu.downloadStatus.stop");
					enabled = true;
				} else {
					displayString = new TranslatableComponent(
							"wdl.gui.ingameMenu.downloadStatus.start");
					enabled = true;
				}
				this.setEnabled(enabled);
				this.setMessage(displayString);
			}

			@Override
			public void performAction() {
				if (wdl.minecraft.isLocalServer()) {
					return; // WDL not available if in singleplayer or LAN server mode
				}

				if (WDL.downloading) {
					wdl.stopDownload();
					setEnabled(false); // Disable to stop double-clicks
				} else {
					if (!WDLPluginChannels.canDownloadAtAll()) {
						// If they don't have any permissions, let the player
						// request some.
						if (WDLPluginChannels.canRequestPermissions()) {
							wdl.minecraft.setScreen(new GuiWDLPermissions(menu, wdl));
						} else {
							// Should never happen
						}
					} else if (WDLPluginChannels.hasChunkOverrides()
							&& !WDLPluginChannels.canDownloadInGeneral()) {
						// Handle the "only has chunk overrides" state - notify
						// the player of limited areas.
						wdl.minecraft.setScreen(new GuiWDLChunkOverrides(menu, wdl));
					} else {
						wdl.startDownload();
						setEnabled(false); // Disable to stop double-clicks
					}
				}
			}
		}

		public class SettingsButton extends WDLButton {
			public SettingsButton(Screen menu, int x, int y, int width, int height, Component displayString) {
				super(x, y, width, height, displayString);
				this.menu = menu;
			}

			// The GuiScreen containing this button, as a parent for other GUIs
			private final Screen menu;

			@Override
			public void performAction() {
				if (wdl.minecraft.isLocalServer()) {
					wdl.minecraft.setScreen(new GuiWDLAbout(menu, wdl));
				} else {
					if (wdl.promptForInfoForSettings("changeOptions", false, this::performAction, () -> wdl.minecraft.setScreen(null))) {
						return;
					}
					wdl.minecraft.setScreen(new GuiWDL(menu, wdl));
				}
			}
		}

		private boolean isAdvancementsButton(Button button) {
			Object message = button.getMessage(); // String or ITextComponent
			if (message instanceof String) {
				return message.equals(I18n.get("gui.advancements"));
			} else if (message instanceof TranslatableComponent) {
				// Though the method returns an ITextComponent,
				// for the screen it'll be a translation component.
				return ((TranslatableComponent) message).getKey().equals("gui.advancements");
			} else {
				return false;
			}
		}

		@Override
		public void injectWDLButtons(PauseScreen gui, Collection<AbstractWidget> buttonList,
				Consumer<AbstractWidget> addButton) {
			int insertAtYPos = 0;

			for (Object o : buttonList) {
				if (!(o instanceof Button)) {
					continue;
				}
				Button btn = (Button)o;
				if (isAdvancementsButton(btn)) {
					insertAtYPos = btn.y + 24;
					break;
				}
			}

			// Move other buttons down one slot (= 24 height units)
			for (Object o : buttonList) {
				if (!(o instanceof Button)) {
					continue;
				}
				Button btn = (Button)o;
				if (btn.y >= insertAtYPos) {
					btn.y += 24;
				}
			}

			// Insert wdl buttons.
			addButton.accept(new StartDownloadButton(gui,
					gui.width / 2 - 102, insertAtYPos, 174, 20));

			addButton.accept(new SettingsButton(gui,
					gui.width / 2 + 74, insertAtYPos, 28, 20,
					new TranslatableComponent("wdl.gui.ingameMenu.settings")));
		}

		@Override
		public void handleWDLButtonClick(PauseScreen gui, Button button) {
			if (button.getMessage().equals(I18n.get("menu.disconnect"))) { // "Disconnect", from vanilla
				wdl.stopDownload();
				// Disable the button to prevent double-clicks
				button.active = false;
			}
		}
	}
}
