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

import static wdl.versioned.VersionedFunctions.*;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import wdl.handler.HandlerException;

public class BarrelHandler extends BlockHandler<BarrelBlockEntity, ChestMenu> {

	public BarrelHandler() {
		super(BarrelBlockEntity.class, ChestMenu.class, "container.barrel");
	}

	@Override
	public Component handle(BlockPos clickedPos, ChestMenu container, BarrelBlockEntity blockEntity,
			BlockGetter world, BiConsumer<BlockPos, BarrelBlockEntity> saveMethod) throws HandlerException {
		String displayName = getCustomDisplayName(container.getContainer());
		saveContainerItems(container, blockEntity, 0);
		if (displayName != null) {
			blockEntity.setCustomName(customName(displayName));
		}
		saveMethod.accept(clickedPos, blockEntity);
		return new TranslatableComponent("wdl.messages.onGuiClosedInfo.savedTileEntity.barrel");
	}

}
