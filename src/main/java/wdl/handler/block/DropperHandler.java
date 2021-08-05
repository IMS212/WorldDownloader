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
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import wdl.ReflectionUtils;
import wdl.handler.HandlerException;

public class DropperHandler extends BlockHandler<DropperBlockEntity, DispenserMenu> {
	public DropperHandler() {
		super(DropperBlockEntity.class, DispenserMenu.class, "container.dropper");
	}

	@Override
	public Component handle(BlockPos clickedPos, DispenserMenu container,
			DropperBlockEntity blockEntity, BlockGetter world,
			BiConsumer<BlockPos, DropperBlockEntity> saveMethod) throws HandlerException {
		Container dropperInventory = ReflectionUtils.findAndGetPrivateField(
				container, Container.class);
		String title = getCustomDisplayName(dropperInventory);
		saveContainerItems(container, blockEntity, 0);
		saveMethod.accept(clickedPos, blockEntity);
		if (title != null) {
			blockEntity.setCustomName(customName(title));
		}
		return new TranslatableComponent("wdl.messages.onGuiClosedInfo.savedTileEntity.dropper");
	}
}
