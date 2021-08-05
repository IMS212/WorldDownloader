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
package wdl.gui;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import wdl.WDL;
import wdl.gui.widget.WDLScreen;
import wdl.versioned.VersionedFunctions;

/**
 * Base GUI with the player in the background turning slowly to show the
 * entire world.
 */
public abstract class GuiTurningCameraBase extends WDLScreen {
	private final WDL wdl;

	/**
	 * Current yaw.
	 */
	private float yaw;
	/**
	 * The previous mode for the camera (First person, 3rd person, etc)
	 */
	private Object oldCameraMode;
	/**
	 * The previous state as to whether the hud was hidden with F1.
	 */
	private boolean oldHideHud;
	/**
	 * The previous state as to whether the debug menu was enabled with F#.
	 */
	private boolean oldShowDebug;
	/**
	 * The previous chat visibility.
	 */
	private ChatVisiblity oldChatVisibility;
	/**
	 * The player to preview.
	 */
	private LocalPlayer cam;
	/**
	 * The previous render view entity (the entity which Minecraft uses
	 * for the camera)
	 */
	private Entity oldRenderViewEntity;
	/**
	 * Whether the camera has been set up.
	 */
	private boolean initializedCamera = false;

	protected GuiTurningCameraBase(WDL wdl, String titleI18nKey) {
		super(titleI18nKey);
		this.wdl = wdl;
	}

	protected GuiTurningCameraBase(WDL wdl, Component title) {
		super(title);
		this.wdl = wdl;
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	@Override
	@OverridingMethodsMustInvokeSuper
	public void init() {
		if (!initializedCamera) {
			this.cam = VersionedFunctions.makePlayer(wdl.minecraft, wdl.worldClient, wdl.player.connection, wdl.player);
			this.cam.moveTo(VersionedFunctions.getEntityX(wdl.player),
					VersionedFunctions.getEntityY(wdl.player), VersionedFunctions.getEntityZ(wdl.player),
					wdl.player.yRot, 0.0F);
			this.yaw = wdl.player.yRot;
			this.oldCameraMode = VersionedFunctions.getPointOfView(wdl.minecraft.options);
			this.oldHideHud = wdl.minecraft.options.hideGui;
			this.oldShowDebug = wdl.minecraft.options.renderDebug;
			this.oldChatVisibility = wdl.minecraft.options.chatVisibility;
			this.oldRenderViewEntity = wdl.minecraft.getCameraEntity();

			initializedCamera = true;
		}
	}

	/**
	 * Speed for a rotation, as a rough scale, in degrees per frame.
	 */
	private static final float ROTATION_SPEED = 1.0f;
	/**
	 * Change between the slowest speed and the average speed.
	 */
	private static final float ROTATION_VARIANCE = .7f;

	/**
	 * Increment yaw to the yaw for the next tick.
	 */
	@Override
	public void tick() {
		if (minecraft.level != null && this.initializedCamera) {
			this.cam.xRotO = this.cam.xRot = 0.0F;
			this.cam.yRotO = this.yaw;
			this.cam.yOld = this.cam.yo = VersionedFunctions.getEntityY(this.cam);
			this.cam.xOld = this.cam.xo = VersionedFunctions.getEntityX(this.cam);
			this.cam.zOld = this.cam.zo = VersionedFunctions.getEntityZ(this.cam);

			// TODO: Rewrite this function as a function of time, rather than
			// an incremental function, if it's possible to do so.
			// Due to the fact that it refers to itself, I have no idea how to
			// approach the problem - it's some kind of integration that would be
			// needed, but it's really complex.

			// Yaw is in degrees, but Math.cos is in radians. The
			// "(this.yaw + 45) / 45.0 * Math.PI)" portion basically makes cosine
			// give the lowest values in each cardinal direction and the highest
			// while looking diagonally.  These are then multiplied by .7 and added
			// to 1, which creates a speed varying from .3 to 1.7.  This causes it
			// to speed through diagonals and go slow in cardinal directions, which
			// is the behavior we want.
			this.yaw = (this.yaw + ROTATION_SPEED
					* (float) (1 + ROTATION_VARIANCE
							* Math.cos((this.yaw + 45) / 45.0 * Math.PI)));

			this.cam.yRot = this.yaw;

			double x = Math.cos(yaw / 180.0D * Math.PI);
			double z = Math.sin((yaw - 90) / 180.0D * Math.PI);

			double distance = truncateDistanceIfBlockInWay(x, z, .5);
			double posX = VersionedFunctions.getEntityX(wdl.player) - distance * x;
			double posY = VersionedFunctions.getEntityY(wdl.player);
			double posZ = VersionedFunctions.getEntityZ(wdl.player) + distance * z;
			VersionedFunctions.setEntityPos(this.cam, posX, posY, posZ);

			this.cam.xChunk = Mth.floor(posX / 16.0D);
			this.cam.yChunk = Mth.floor(posY / 16.0D);
			this.cam.zChunk = Mth.floor(posZ / 16.0D);
		}

		this.deactivateRenderViewEntity();

		super.tick();
	}

	/**
	 * Truncates the distance so that the camera does not clip into blocks.
	 * Based off of {@link net.minecraft.client.renderer.EntityRenderer#orientCamera(float)}.
	 * @param camX X-position of the camera.
	 * @param camZ Z-position of the camera.
	 * @param currentDistance Current distance from the camera.
	 * @return A new distance, equal to or less than <code>currentDistance</code>.
	 */
	private double truncateDistanceIfBlockInWay(double camX, double camZ, double currentDistance) {
		Vec3 playerPos = wdl.player.position().add(0, wdl.player.getEyeHeight(), 0);
		Vec3 offsetPos = playerPos.add(-currentDistance * camX, 0, currentDistance * camZ);

		// NOTE: Vec3.addVector and Vec3.add return new vectors and leave the
		// current vector unmodified.
		for (int i = 0; i < 9; i++) {
			// Check offset slightly in all directions.
			float offsetX = ((i & 0x01) != 0) ? -.1f : .1f;
			float offsetY = ((i & 0x02) != 0) ? -.1f : .1f;
			float offsetZ = ((i & 0x04) != 0) ? -.1f : .1f;

			if (i == 8) {
				offsetX = 0;
				offsetY = 0;
				offsetZ = 0;
			}

			Vec3 from = playerPos.add(offsetX, offsetY, offsetZ);
			Vec3 to = offsetPos.add(offsetX, offsetY, offsetZ);

			HitResult pos = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, wdl.player));

			double distance = pos.getLocation().distanceTo(playerPos);
			if (distance < currentDistance && distance > 0) {
				currentDistance = distance;
			}
		}

		return currentDistance - .25;
	}

	@Override
	public void removed() {
		super.removed();
		this.deactivateRenderViewEntity();
	}

	/**
	 * Draws a dirt background if there is no world.  Subclasses must call this,
	 * to prevent <a href="https://user-images.githubusercontent.com/8334194/35199077-95ed2e84-fef0-11e7-83d7-bd6088169c84.png">
	 * graphical issues</a> when there is no world.
	 */
	@Override
	public void renderBackground() {
		if (minecraft.level == null) {
			this.renderDirtBackground(0);
		}
	}

	/**
	 * Called when the client world ticks, from a static context.
	 */
	public static void onWorldTick() {
		Screen screen = WDL.getInstance().minecraft.screen;
		if (screen instanceof GuiTurningCameraBase) {
			((GuiTurningCameraBase) screen).onWorldTick0();
		}
	}

	/**
	 * Called when the world ticks.
	 * Note that we do this in the world tick instead of the normal GUI tick,
	 * because the GUI tick happens before the world ticks entities while this happens
	 * after entities have been ticked.  We don't want the camera to be active when
	 * entities are being ticked, because that causes some subtle issues.
	 */
	private void onWorldTick0() {
		this.activateRenderViewEntity();
	}

	/**
	 * Sets the render view entity to the custom camera.
	 */
	private void activateRenderViewEntity() {
		if (!this.initializedCamera) return;

		VersionedFunctions.setFirstPersonPointOfView(wdl.minecraft.options);
		wdl.minecraft.options.hideGui = true;
		wdl.minecraft.options.renderDebug = false;
		wdl.minecraft.options.chatVisibility = ChatVisiblity.HIDDEN;
		wdl.minecraft.setCameraEntity(this.cam);
	}

	/**
	 * Returns the render view entity to the normal player.
	 */
	private void deactivateRenderViewEntity() {
		if (!this.initializedCamera) return;

		VersionedFunctions.restorePointOfView(wdl.minecraft.options, this.oldCameraMode);
		wdl.minecraft.options.hideGui = oldHideHud;
		wdl.minecraft.options.renderDebug = oldShowDebug;
		wdl.minecraft.options.chatVisibility = oldChatVisibility;
		wdl.minecraft.setCameraEntity(this.oldRenderViewEntity);
	}
}
