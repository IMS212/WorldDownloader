/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */

package wdl;

import static net.minecraft.ChatFormatting.*;
import static wdl.MessageTypeCategory.*;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import wdl.api.IMessageTypeAdder;
import wdl.api.IWDLMessageType;

/**
 * Enum containing WDL's default {@link IWDLMessageType}s.
 *
 * <b>Mostly intended for internal use.</b> Extensions may use {@link #INFO} and
 * {@link #ERROR}, but if they need something more complex, they should
 * implement {@link IMessageTypeAdder} and create new ones with that unless
 * it's a perfect fit.
 */
public enum WDLMessageTypes implements IWDLMessageType {
	INFO("wdl.messages.message.info", RED, GOLD, true, CORE_RECOMMENDED),
	ERROR("wdl.messages.message.error", DARK_GREEN, DARK_RED, true, CORE_RECOMMENDED),
	UPDATES("wdl.messages.message.updates", RED, GOLD, true, CORE_RECOMMENDED),
	LOAD_TILE_ENTITY("wdl.messages.message.loadingTileEntity", false),
	ON_WORLD_LOAD("wdl.messages.message.onWorldLoad",false),
	ON_BLOCK_EVENT("wdl.messages.message.blockEvent", true),
	ON_MAP_SAVED("wdl.messages.message.mapDataSaved", false),
	ON_CHUNK_NO_LONGER_NEEDED("wdl.messages.message.chunkUnloaded", false),
	ON_GUI_CLOSED_INFO("wdl.messages.message.guiClosedInfo", true),
	ON_GUI_CLOSED_WARNING("wdl.messages.message.guiClosedWarning", true),
	SAVING("wdl.messages.message.saving", true),
	REMOVE_ENTITY("wdl.messages.message.removeEntity", false),
	PLUGIN_CHANNEL_MESSAGE("wdl.messages.message.pluginChannel", false),
	UPDATE_DEBUG("wdl.messages.message.updateDebug", false);

	/**
	 * Constructor with the default values for a debug message.
	 */
	private WDLMessageTypes(String i18nKey,
			boolean enabledByDefault) {
		this(i18nKey, DARK_GREEN, GOLD, enabledByDefault, CORE_DEBUG);
	}
	/**
	 * Constructor that allows specification of all values.
	 */
	private WDLMessageTypes(String i18nKey, ChatFormatting titleColor,
			ChatFormatting textColor, boolean enabledByDefault,
			MessageTypeCategory category) {
		this.displayTextKey = i18nKey + ".text";
		this.titleColor = titleColor;
		this.textColor = textColor;
		this.descriptionKey = i18nKey + ".description";
		this.enabledByDefault = enabledByDefault;
		this.category = category;
	}

	/**
	 * I18n key for the text to display on a button for this enum value.
	 */
	private final String displayTextKey;
	/**
	 * Format code for the '[WorldDL]' label.
	 */
	private final ChatFormatting titleColor;
	/**
	 * Format code for the text after the label.
	 */
	private final ChatFormatting textColor;
	/**
	 * I18n key for the description text.
	 */
	private final String descriptionKey;
	/**
	 * Whether this type of message is enabled by default.
	 */
	private final boolean enabledByDefault;
	/**
	 * The category of this type.  Field is only used for registration.
	 */
	final MessageTypeCategory category;

	@Override
	public Component getDisplayName() {
		return new TranslatableComponent(displayTextKey);
	}

	@Override
	public ChatFormatting getTitleColor() {
		return titleColor;
	}

	@Override
	public ChatFormatting getTextColor() {
		return textColor;
	}

	@Override
	public Component getDescription() {
		return new TranslatableComponent(descriptionKey);
	}

	@Override
	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}
}
