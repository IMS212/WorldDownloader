/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2018 Pokechu22, julialy
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
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import wdl.ReflectionUtils;
import wdl.handler.HandlerException;

public class ShulkerBoxHandler extends BlockHandler<ShulkerBoxBlockEntity, ShulkerBoxMenu> {
	public ShulkerBoxHandler() {
		super(ShulkerBoxBlockEntity.class, ShulkerBoxMenu.class, "container.shulkerBox");
	}

	@Override
	public Component handle(BlockPos clickedPos, ShulkerBoxMenu container,
			ShulkerBoxBlockEntity blockEntity, BlockGetter world,
			BiConsumer<BlockPos, ShulkerBoxBlockEntity> saveMethod) throws HandlerException {
		Container shulkerInventory = ReflectionUtils.findAndGetPrivateField(
				container, Container.class);
		String title = getCustomDisplayName(shulkerInventory);
		saveContainerItems(container, blockEntity, 0);
		if (title != null) {
			blockEntity.setCustomName(customName(title));
		}
		saveMethod.accept(clickedPos, blockEntity);
		return new TranslatableComponent("wdl.messages.onGuiClosedInfo.savedTileEntity.shulkerBox");
	}
}
