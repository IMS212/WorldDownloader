/*
 * This file is part of FabricWDL.  FabricWDL contains the fabric-specific
 * code for World Downloader: A mod to make backups of your multiplayer worlds.
 * http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2520465
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2018 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see http://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package pokechu22.fabricwdl.mixin;


import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdl.ducks.IBaseChangesApplied;

@Mixin(PauseScreen.class)
public class MixinGuiIngameMenu extends Screen implements IBaseChangesApplied {

  protected MixinGuiIngameMenu(Component iTextComponent) {
    super(iTextComponent);
  }

  @Inject(method="init", at=@At("RETURN"))
  private void onInitGui(CallbackInfo ci) {
    wdl.WDLHooks.injectWDLButtons((PauseScreen)(Object)this, buttons, this::addButton);
  }
}
