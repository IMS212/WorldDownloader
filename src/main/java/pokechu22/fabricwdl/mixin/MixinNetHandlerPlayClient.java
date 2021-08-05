package pokechu22.fabricwdl.mixin;/*
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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.play.server.SBlockActionPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.network.play.server.SMapDataPacket;
import net.minecraft.network.play.server.SUnloadChunkPacket;
import net.minecraft.util.text.ITextComponent;
import wdl.ducks.IBaseChangesApplied;

@Mixin(ClientPlayNetHandler.class)
public abstract class MixinNetHandlerPlayClient implements IClientPlayNetHandler, IBaseChangesApplied {
  // Automatic remapping sometimes fails; see
  // https://github.com/Pokechu22/WorldDownloader/issues/175
  @Shadow
  private ClientWorld world;

  @Inject(method="processChunkUnload", at=@At("HEAD"))
  private void onProcessChunkUnload(SUnloadChunkPacket packetIn, CallbackInfo ci) {
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleChunkUnload((ClientPlayNetHandler)(Object)this, this.world, packetIn);
    /* <<< WDL */
    //more down here
  }
  @Inject(method="onDisconnect", at=@At("HEAD"))
  private void onDisconnect(ITextComponent reason, CallbackInfo ci) {
    /* WDL >>> */
    wdl.WDLHooks.onNHPCDisconnect((ClientPlayNetHandler)(Object)this, reason);
    /* <<< WDL */
    //more down here
  }
  @Inject(method="handleChat", at=@At("RETURN"))
  private void onHandleChat(SChatPacket p_147251_1_, CallbackInfo ci) {
    //more up here
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleChat((ClientPlayNetHandler)(Object)this, p_147251_1_);
    /* <<< WDL */
  }
  @Inject(method="handleBlockAction", at=@At("RETURN"))
  private void onHandleBlockAction(SBlockActionPacket packetIn, CallbackInfo ci) {
    //more up here
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleBlockAction((ClientPlayNetHandler)(Object)this, packetIn);
    /* <<< WDL */
  }
  @Inject(method="handleMaps", at=@At("RETURN"))
  private void onHandleMaps(SMapDataPacket packetIn, CallbackInfo ci) {
    //more up here
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleMaps((ClientPlayNetHandler)(Object)this, packetIn);
    /* <<< WDL */
  }
  @Inject(method="handleCustomPayload", at=@At("HEAD"))
  private void onHandleCustomPayload(SCustomPayloadPlayPacket packetIn, CallbackInfo ci) {
    // Inject at HEAD because otherwise Forge will read the packet content first,
    // which irreversibly clears the buffer (without doing other weird hacky things)
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleCustomPayload((ClientPlayNetHandler)(Object)this, packetIn);
    /* <<< WDL */
    //more down here
  }
}