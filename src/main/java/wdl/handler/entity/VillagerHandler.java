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
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import wdl.ReflectionUtils;
import wdl.handler.HandlerException;

public class VillagerHandler extends EntityHandler<Villager, MerchantMenu> {
	public VillagerHandler() {
		super(Villager.class, MerchantMenu.class);
	}

	@Override
	public Component copyData(MerchantMenu container, Villager villager, boolean riding) throws HandlerException {
		Merchant merchant = ReflectionUtils.findAndGetPrivateField(
				container, Merchant.class);
		MerchantOffers recipes = merchant.getOffers();
		ReflectionUtils.findAndSetPrivateField(villager, AbstractVillager.class, MerchantOffers.class, recipes);
		villager.setVillagerXp(merchant.getVillagerXp());

		return new TranslatableComponent("wdl.messages.onGuiClosedInfo.savedEntity.villager.tradesOnly");
		// Other data is actually transfered properly now, fortunately
	}
}
