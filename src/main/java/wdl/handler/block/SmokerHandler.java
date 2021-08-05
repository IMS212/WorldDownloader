/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2019 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl.handler.block;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

public class SmokerHandler extends BaseFurnaceHandler<SmokerBlockEntity, SmokerMenu> {
	public SmokerHandler() {
		super(SmokerBlockEntity.class, SmokerMenu.class, "container.smoker");
	}

	@Override
	protected TranslatableComponent getMessage() {
		return new TranslatableComponent("wdl.messages.onGuiClosedInfo.savedTileEntity.smoker");
	}
}
