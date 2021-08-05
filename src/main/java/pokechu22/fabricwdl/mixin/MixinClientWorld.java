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

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import wdl.WDLHooks;
import wdl.ducks.IBaseChangesApplied;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientLevel.class})
public abstract class MixinClientWorld extends Level implements IBaseChangesApplied {


  protected MixinClientWorld(WritableLevelData iSpawnWorldInfo, ResourceKey<Level> registryKey, DimensionType dimensionType, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
    super(iSpawnWorldInfo, registryKey, dimensionType, supplier, bl, bl2, l);
  }

  @Inject(method = "tick", at = @At("RETURN"))
  private void onTick(CallbackInfo ci) {
    WDLHooks.onWorldClientTick((ClientLevel) (Object) this);
  }

  @Inject(method = "removeEntity", at = @At("HEAD"))
  private void onRemoveEntityFromWorld(int p_73028_1_, CallbackInfo ci) {
    /* WDL >>> */
    wdl.WDLHooks.onWorldClientRemoveEntityFromWorld((ClientLevel) (Object) this, p_73028_1_);
    /* <<< WDL */
    //more down here
  }
}

