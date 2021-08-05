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


import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdl.ducks.IBaseChangesApplied;

@Mixin(IngameMenuScreen.class)
public class MixinGuiIngameMenu extends Screen implements IBaseChangesApplied {

  protected MixinGuiIngameMenu(ITextComponent iTextComponent) {
    super(iTextComponent);
  }

  @Inject(method="init", at=@At("RETURN"))
  private void onInitGui(CallbackInfo ci) {
    wdl.WDLHooks.injectWDLButtons((IngameMenuScreen)(Object)this, buttons, this::addButton);
  }
}
