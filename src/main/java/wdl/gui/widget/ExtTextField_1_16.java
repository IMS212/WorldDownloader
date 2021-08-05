/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Extendible text field, to deal with changing constructors between versions.
 * The actual implementation is {@link WDLTextField}.
 */
abstract class ExtTextField extends EditBox {
	/**
	 * @deprecated Do not use; use {@link #setEditable} instead.
	 */
	@Deprecated
	protected static final Void active = null;
	@Nullable
	private PoseStack matrixStack = null;

	public ExtTextField(Font fontRenderer, int x, int y, int width, int height, Component label) {
		super(fontRenderer, x, y, width, height, label);
	}
}
