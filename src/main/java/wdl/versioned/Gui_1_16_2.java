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

import static org.lwjgl.opengl.GL11.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.Level;

/**
 * Versioned functions related to GUIs.
 */
final class GuiFunctions {
	private GuiFunctions() { throw new AssertionError(); }

	/* (non-javadoc)
	 * @see VersionedFunctions#makePlayer
	 */
	static LocalPlayer makePlayer(Minecraft minecraft, Level world, ClientPacketListener nhpc, LocalPlayer base) {
		return new LocalPlayer(minecraft, (ClientLevel)world, nhpc,
				base.getStats(), base.getRecipeBook(), false, false);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getPointOfView
	 */
	static Object getPointOfView(Options settings) {
		return settings.getCameraType();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#setFirstPersonPointOfView
	 */
	static void setFirstPersonPointOfView(Options settings) {
		settings.setCameraType(CameraType.FIRST_PERSON);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#restorePointOfView
	 */
	static void restorePointOfView(Options settings, Object value) {
		settings.setCameraType((CameraType)value);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#drawDarkBackground
	 */
	static void drawDarkBackground(int top, int left, int bottom, int right) {
		RenderSystem.disableLighting();
		RenderSystem.disableFog();

		Tesselator t = Tesselator.getInstance();
		BufferBuilder b = t.getBuilder();

		Minecraft.getInstance().getTextureManager().bind(GuiComponent.BACKGROUND_LOCATION);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		float textureSize = 32.0F;
		b.begin(7, DefaultVertexFormat.POSITION_COLOR_TEX);
		b.vertex(0, bottom, 0).color(32, 32, 32, 255).uv(0 / textureSize, bottom / textureSize).endVertex();
		b.vertex(right, bottom, 0).color(32, 32, 32, 255).uv(right / textureSize, bottom / textureSize).endVertex();
		b.vertex(right, top, 0).color(32, 32, 32, 255).uv(right / textureSize, top / textureSize).endVertex();
		b.vertex(left, top, 0).color(32, 32, 32, 255).uv(left / textureSize, top / textureSize).endVertex();
		t.end();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#drawBorder
	 */
	static void drawBorder(int topMargin, int bottomMargin, int top, int left, int bottom, int right) {
		RenderSystem.disableLighting();
		RenderSystem.disableFog();
		RenderSystem.disableDepthTest();
		byte padding = 4;

		Minecraft.getInstance().getTextureManager().bind(GuiComponent.BACKGROUND_LOCATION);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		float textureSize = 32.0F;

		Tesselator t = Tesselator.getInstance();
		BufferBuilder b = t.getBuilder();

		// Box code is GuiSlot.overlayBackground
		// Upper box
		int upperBoxEnd = top + topMargin;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		b.begin(7, DefaultVertexFormat.POSITION_COLOR_TEX);
		b.vertex(left, upperBoxEnd, 0.0D).color(64, 64, 64, 255).uv(0, upperBoxEnd / textureSize).endVertex();
		b.vertex(right, upperBoxEnd, 0.0D).color(64, 64, 64, 255).uv(right / textureSize, upperBoxEnd / textureSize)
				.endVertex();
		b.vertex(right, top, 0.0D).color(64, 64, 64, 255).uv(right / textureSize, top / textureSize).endVertex();
		b.vertex(left, top, 0.0D).color(64, 64, 64, 255).uv(0, top / textureSize).endVertex();
		t.end();

		// Lower box
		int lowerBoxStart = bottom - bottomMargin;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		b.begin(7, DefaultVertexFormat.POSITION_COLOR_TEX);
		b.vertex(left, bottom, 0.0D).color(64, 64, 64, 255).uv(0, bottom / textureSize).endVertex();
		b.vertex(right, bottom, 0.0D).color(64, 64, 64, 255).uv(right / textureSize, bottom / textureSize).endVertex();
		b.vertex(right, lowerBoxStart, 0.0D).color(64, 64, 64, 255).uv(right / textureSize, lowerBoxStart / textureSize)
				.endVertex();
		b.vertex(left, lowerBoxStart, 0.0D).color(64, 64, 64, 255).uv(0, lowerBoxStart / textureSize).endVertex();
		t.end();

		// Gradients
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 0, 1);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(GL_SMOOTH);
		RenderSystem.disableTexture();
		b.begin(7, DefaultVertexFormat.POSITION_COLOR_TEX);
		b.vertex(left, upperBoxEnd + padding, 0.0D).color(0, 0, 0, 0).uv(0, 1).endVertex();
		b.vertex(right, upperBoxEnd + padding, 0.0D).color(0, 0, 0, 0).uv(1, 1).endVertex();
		b.vertex(right, upperBoxEnd, 0.0D).color(0, 0, 0, 255).uv(1, 0).endVertex();
		b.vertex(left, upperBoxEnd, 0.0D).color(0, 0, 0, 255).uv(0, 0).endVertex();
		t.end();
		b.begin(7, DefaultVertexFormat.POSITION_COLOR_TEX);
		b.vertex(left, lowerBoxStart, 0.0D).color(0, 0, 0, 255).uv(0, 1).endVertex();
		b.vertex(right, lowerBoxStart, 0.0D).color(0, 0, 0, 255).uv(1, 1).endVertex();
		b.vertex(right, lowerBoxStart - padding, 0.0D).color(0, 0, 0, 0).uv(1, 0).endVertex();
		b.vertex(left, lowerBoxStart - padding, 0.0D).color(0, 0, 0, 0).uv(0, 0).endVertex();
		t.end();

		RenderSystem.enableTexture();
		RenderSystem.shadeModel(GL_FLAT);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#setClipboardString
	 */
	static void setClipboardString(String text) {
		Minecraft.getInstance().keyboardHandler.setClipboard(text);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#openLink
	 */
	static void openLink(String url) {
		Util.getPlatform().openUri(url);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#glColor4f
	 */
	static void glColor4f(float r, float g, float b, float a) {
		RenderSystem.color4f(r, g, b, a);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#glTranslatef
	 */
	static void glTranslatef(float x, float y, float z) {
		RenderSystem.translatef(x, y, z);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#applyLinkFormatting
	 */
	static Style createLinkFormatting(String url) {
		return Style.EMPTY
				.withColor(TextColor.fromLegacyFormat(ChatFormatting.BLUE))
				.applyFormat(ChatFormatting.UNDERLINE)
				.withClickEvent(new ClickEvent(Action.OPEN_URL, url));
	}

	/* (non-javadoc)
	 * @See VersionedFunctions#createConfirmScreen
	 */
	static ConfirmScreen createConfirmScreen(BooleanConsumer action, Component line1,
			Component line2, Component confirm, Component cancel) {
		return new ConfirmScreen(action, line1, line2, confirm, cancel);
	}
}
