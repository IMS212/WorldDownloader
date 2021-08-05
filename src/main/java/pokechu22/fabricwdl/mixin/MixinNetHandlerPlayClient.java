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

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdl.ducks.IBaseChangesApplied;

@Mixin(ClientPacketListener.class)
public abstract class MixinNetHandlerPlayClient implements ClientGamePacketListener, IBaseChangesApplied {
  // Automatic remapping sometimes fails; see
  // https://github.com/Pokechu22/WorldDownloader/issues/175
  @Shadow
  private ClientLevel level;

  @Inject(method="handleForgetLevelChunk", at=@At("HEAD"))
  private void onProcessChunkUnload(ClientboundForgetLevelChunkPacket packetIn, CallbackInfo ci) {
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleChunkUnload((ClientPacketListener)(Object)this, this.level, packetIn);
    /* <<< WDL */
    //more down here
  }
  @Inject(method="onDisconnect", at=@At("HEAD"))
  private void onDisconnect(Component reason, CallbackInfo ci) {
    /* WDL >>> */
    wdl.WDLHooks.onNHPCDisconnect((ClientPacketListener)(Object)this, reason);
    /* <<< WDL */
    //more down here
  }
  @Inject(method="handleChat", at=@At("RETURN"))
  private void onHandleChat(ClientboundChatPacket p_147251_1_, CallbackInfo ci) {
    //more up here
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleChat((ClientPacketListener)(Object)this, p_147251_1_);
    /* <<< WDL */
  }
  @Inject(method="handleBlockEvent", at=@At("RETURN"))
  private void onHandleBlockAction(ClientboundBlockEventPacket packetIn, CallbackInfo ci) {
    //more up here
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleBlockAction((ClientPacketListener)(Object)this, packetIn);
    /* <<< WDL */
  }
  @Inject(method="handleMapItemData", at=@At("RETURN"))
  private void onHandleMaps(ClientboundMapItemDataPacket packetIn, CallbackInfo ci) {
    //more up here
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleMaps((ClientPacketListener)(Object)this, packetIn);
    /* <<< WDL */
  }
  @Inject(method="handleCustomPayload", at=@At("HEAD"))
  private void onHandleCustomPayload(ClientboundCustomPayloadPacket packetIn, CallbackInfo ci) {
    // Inject at HEAD because otherwise Forge will read the packet content first,
    // which irreversibly clears the buffer (without doing other weird hacky things)
    /* WDL >>> */
    wdl.WDLHooks.onNHPCHandleCustomPayload((ClientPacketListener)(Object)this, packetIn);
    /* <<< WDL */
    //more down here
  }
}