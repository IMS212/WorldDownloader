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
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;

/**
 * The various hooks for wdl. <br/>
 * All of these should be called regardless of any WDL state variables.
 * This class forwards the hooks to the appropriate locations.
 */
public final class WDLHooks {
	private WDLHooks() { throw new AssertionError(); }

	/**
	 * Listener which should receive event calls.
	 */
	@Nonnull
	static IHooksListener listener = new BootstrapHooksListener();

	public static interface IHooksListener {
		void onWorldClientTick(ClientLevel sender);
		void onWorldClientRemoveEntityFromWorld(ClientLevel sender, int eid);
		void onNHPCHandleChunkUnload(ClientPacketListener sender, ClientLevel world, ClientboundForgetLevelChunkPacket packet);
		void onNHPCHandleChat(ClientPacketListener sender, ClientboundChatPacket packet);
		void onNHPCHandleMaps(ClientPacketListener sender, ClientboundMapItemDataPacket packet);
		void onNHPCHandleCustomPayload(ClientPacketListener sender, ClientboundCustomPayloadPacket packet);
		void onNHPCHandleBlockAction(ClientPacketListener sender, ClientboundBlockEventPacket packet);
		void onNHPCDisconnect(ClientPacketListener sender, Component reason);
		void onCrashReportPopulateEnvironment(CrashReport report);
		void injectWDLButtons(PauseScreen gui, Collection<AbstractWidget> buttonList, Consumer<AbstractWidget> addButton);
		void handleWDLButtonClick(PauseScreen gui, Button button);
	}

	private static class BootstrapHooksListener implements IHooksListener {
		// All of these methods other than the crash one first bootstrap WDL,
		// and then forward the event to the new listener (which should have changed)
		private void bootstrap() {
			WDL.bootstrap(Minecraft.getInstance());
			if (listener == this) {
				throw new AssertionError("WDL bootstrap failed to change WDLHooks listener from " + this);
			}
		}

		@Override
		public void onWorldClientTick(ClientLevel sender) {
			bootstrap();
			listener.onWorldClientTick(sender);
		}

		@Override
		public void onWorldClientRemoveEntityFromWorld(ClientLevel sender, int eid) {
			bootstrap();
			listener.onWorldClientRemoveEntityFromWorld(sender, eid);
		}

		@Override
		public void onNHPCHandleChunkUnload(ClientPacketListener sender, ClientLevel world, ClientboundForgetLevelChunkPacket packet) {
			bootstrap();
			listener.onNHPCHandleChunkUnload(sender, world, packet);
		}

		@Override
		public void onNHPCHandleChat(ClientPacketListener sender, ClientboundChatPacket packet) {
			bootstrap();
			listener.onNHPCHandleChat(sender, packet);
		}

		@Override
		public void onNHPCHandleMaps(ClientPacketListener sender, ClientboundMapItemDataPacket packet) {
			bootstrap();
			listener.onNHPCHandleMaps(sender, packet);
		}

		@Override
		public void onNHPCHandleCustomPayload(ClientPacketListener sender, ClientboundCustomPayloadPacket packet) {
			bootstrap();
			listener.onNHPCHandleCustomPayload(sender, packet);
		}

		@Override
		public void onNHPCHandleBlockAction(ClientPacketListener sender, ClientboundBlockEventPacket packet) {
			bootstrap();
			listener.onNHPCHandleBlockAction(sender, packet);
		}

		@Override
		public void onNHPCDisconnect(ClientPacketListener sender, Component reason) {
			bootstrap();
			listener.onNHPCDisconnect(sender, reason);
		}

		@Override
		public void injectWDLButtons(PauseScreen gui, Collection<AbstractWidget> buttonList, Consumer<AbstractWidget> addButton) {
			bootstrap();
			listener.injectWDLButtons(gui, buttonList, addButton);
		}

		@Override
		public void handleWDLButtonClick(PauseScreen gui, Button button) {
			bootstrap();
			listener.handleWDLButtonClick(gui, button);
		}

		// NOTE: This does NOT bootstrap, as we do not want to bootstrap in a crash
		@Override
		public void onCrashReportPopulateEnvironment(CrashReport report) {
			// Trick the crash report handler into not storing a stack trace
			// (we don't want it)
			int stSize;
			try {
				stSize = Thread.currentThread().getStackTrace().length - 1;
			} catch (Exception e) {
				// Ignore
				stSize = 0;
			}
			CrashReportCategory cat = report.addCategory("World Downloader Mod - not bootstrapped yet", stSize);
			cat.setDetail("WDL version", VersionConstants::getModVersion);
			cat.setDetail("Targeted MC version", VersionConstants::getExpectedVersion);
			cat.setDetail("Actual MC version", VersionConstants::getMinecraftVersion);
		}
	}

	/**
	 * Called when {@link ClientLevel#tick()} is called.
	 * <br/>
	 * Should be at end of the method.
	 */
	public static void onWorldClientTick(ClientLevel sender) {
		listener.onWorldClientTick(sender);
	}

	/**
	 * Called when {@link ClientLevel#removeEntity(int)} is called.
	 * <br/>
	 * Should be at the start of the method.
	 *
	 * @param eid
	 *            The entity's unique ID.
	 */
	public static void onWorldClientRemoveEntityFromWorld(ClientLevel sender,
			int eid) {
		listener.onWorldClientRemoveEntityFromWorld(sender, eid);
	}

	/**
	 * Called when {@link ClientPacketListener#handleForgetLevelChunk(ClientboundForgetLevelChunkPacket)} is called.
	 * <br/>
	 * Should be at the start of the method.
	 */
	public static void onNHPCHandleChunkUnload(ClientPacketListener sender,
			ClientLevel world, ClientboundForgetLevelChunkPacket packet) {
		listener.onNHPCHandleChunkUnload(sender, world, packet);
	}

	/**
	 * Called when {@link ClientPacketListener#handleChat(ClientboundChatPacket)} is
	 * called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleChat(ClientPacketListener sender,
			ClientboundChatPacket packet) {
		listener.onNHPCHandleChat(sender, packet);
	}

	/**
	 * Called when {@link ClientPacketListener#handleMapItemData(ClientboundMapItemDataPacket)} is
	 * called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleMaps(ClientPacketListener sender,
			ClientboundMapItemDataPacket packet) {
		listener.onNHPCHandleMaps(sender, packet);
	}

	/**
	 * Called when
	 * {@link ClientPacketListener#handleCustomPayload(ClientboundCustomPayloadPacket)}
	 * is called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleCustomPayload(ClientPacketListener sender,
			ClientboundCustomPayloadPacket packet) {
		listener.onNHPCHandleCustomPayload(sender, packet);
	}

	/**
	 * Called when
	 * {@link ClientPacketListener#handleBlockEvent(ClientboundBlockEventPacket)} is
	 * called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleBlockAction(ClientPacketListener sender,
			ClientboundBlockEventPacket packet) {
		listener.onNHPCHandleBlockAction(sender, packet);
	}

	/**
	 * Called when {@link ClientPacketListener#onDisconnect(Component)} is called.
	 * <br/>
	 * Should be at the start of the method.
	 *
	 * @param reason The reason for the disconnect, as passed to onDisconnect.
	 */
	public static void onNHPCDisconnect(ClientPacketListener sender, Component reason) {
		listener.onNHPCDisconnect(sender, reason);
	}

	/**
	 * Injects WDL information into a crash report.
	 *
	 * Called at the end of {@link CrashReport#initDetails()}.
	 * @param report
	 */
	public static void onCrashReportPopulateEnvironment(CrashReport report) {
		listener.onCrashReportPopulateEnvironment(report);
	}


	/**
	 * Adds WDL's buttons to the pause menu GUI.
	 *
	 * @param gui        The GUI
	 * @param buttonList The list of buttons in the GUI. This list should not be
	 *                   modified directly.
	 * @param addButton  Method to add a button to the GUI.
	 */
	public static void injectWDLButtons(PauseScreen gui, Collection<AbstractWidget> buttonList,
			Consumer<AbstractWidget> addButton) {
		listener.injectWDLButtons(gui, buttonList, addButton);
	}
	/**
	 * Handle clicks to the ingame pause GUI, specifically for the disconnect
	 * button.
	 *
	 * @param gui    The GUI
	 * @param button The button that was clicked.
	 */
	public static void handleWDLButtonClick(PauseScreen gui, Button button) {
		listener.handleWDLButtonClick(gui, button);
	}
}
