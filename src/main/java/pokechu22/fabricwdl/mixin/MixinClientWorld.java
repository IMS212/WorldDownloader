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

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;

import net.minecraft.world.DimensionType;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import net.minecraft.world.storage.ISpawnWorldInfo;
import wdl.WDLHooks;
import wdl.ducks.IBaseChangesApplied;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientWorld.class})
public abstract class MixinClientWorld extends World implements IBaseChangesApplied {


  protected MixinClientWorld(ISpawnWorldInfo iSpawnWorldInfo, RegistryKey<World> registryKey, DimensionType dimensionType, Supplier<IProfiler> supplier, boolean bl, boolean bl2, long l) {
    super(iSpawnWorldInfo, registryKey, dimensionType, supplier, bl, bl2, l);
  }

  @Inject(method = "tick", at = @At("RETURN"))
  private void onTick(CallbackInfo ci) {
    WDLHooks.onWorldClientTick((ClientWorld) (Object) this);
  }

  @Inject(method = "removeEntityFromWorld", at = @At("HEAD"))
  private void onRemoveEntityFromWorld(int p_73028_1_, CallbackInfo ci) {
    /* WDL >>> */
    wdl.WDLHooks.onWorldClientRemoveEntityFromWorld((ClientWorld) (Object) this, p_73028_1_);
    /* <<< WDL */
    //more down here
  }
}

