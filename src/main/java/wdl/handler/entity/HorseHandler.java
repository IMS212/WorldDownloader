/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2018 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl.handler.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import wdl.ReflectionUtils;
import wdl.handler.HandlerException;

public class HorseHandler extends EntityHandler<AbstractHorse, HorseInventoryMenu> {
	/**
	 * The number of slots used for the player inventory, so that the size
	 * of the horse's inventory can be computed.
	 */
	private static final int PLAYER_INVENTORY_SLOTS = 4 * 9;

	public HorseHandler() {
		super(AbstractHorse.class, HorseInventoryMenu.class);
	}

	@Override
	public boolean checkRiding(HorseInventoryMenu container, AbstractHorse riddenHorse) {
		AbstractHorse horseInContainer = ReflectionUtils
				.findAndGetPrivateField(container, AbstractHorse.class);

		// Intentional reference equals
		return horseInContainer == riddenHorse;
	}

	@Override
	public Component copyData(HorseInventoryMenu container, AbstractHorse horse, boolean riding) throws HandlerException {
		SimpleContainer horseInventory = new SimpleContainer(container.slots.size() - PLAYER_INVENTORY_SLOTS);

		for (int i = 0; i < horseInventory.getContainerSize(); i++) {
			Slot slot = container.getSlot(i);
			if (slot.hasItem()) {
				horseInventory.setItem(i, slot.getItem());
			}
		}

		ReflectionUtils.findAndSetPrivateField(horse, AbstractHorse.class, SimpleContainer.class, horseInventory);

		if (riding) {
			return new TranslatableComponent("wdl.messages.onGuiClosedInfo.savedRiddenEntity.horse");
		} else {
			return new TranslatableComponent("wdl.messages.onGuiClosedInfo.savedEntity.horse");
		}
	}

}
