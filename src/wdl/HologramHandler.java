package wdl;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.BlockPos;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import wdl.api.ISpecialEntityHandler;

/**
 * Handles holograms.
 * <br/> 
 * Right now, the definition of a hologram is an invisible ArmorStand 
 * with a custom name set.  This is how holograms are generated by the
 * <a href="http://dev.bukkit.org/bukkit-plugins/holographic-displays/">
 * Holographic Displays</a> plugin, which is the most popular version.
 * However, the older style of holograms created by Asdjke's method 
 * <a href="https://www.youtube.com/watch?v=q1B19JvX5TE">(showcased in
 * this video)</a> do not get recognized.  I don't think this is a big
 * problem, but it is something to keep in mind.
 * <br/>
 * This is also an example of 
 */
public class HologramHandler implements ISpecialEntityHandler {

	@Override
	public String getName() {
		return "Hologram";
	}

	@Override
	public void onWorldLoad(WorldClient world) {
		
	}

	@Override
	public void onBlockEvent(WorldClient world, BlockPos pos, Block block,
			int data1, int data2) {
		
	}

	@Override
	public void onPluginChannelPacket(WorldClient world, String channel,
			byte[] packetData) {
		
	}

	@Override
	public void onChat(WorldClient world, String message) {
		
	}

	@Override
	public Multimap<String, String> getSpecialEntities() {
		Multimap<String, String> returned = HashMultimap.<String, String>create();
		
		returned.put("ArmorStand", "Hologram");
		
		return returned;
	}

	@Override
	public String getSpecialEntityName(Entity entity) {
		if (entity instanceof EntityArmorStand &&
				entity.isInvisible() &&
				entity.hasCustomName()) {
			return "Hologram";
		}
		
		return null;
	}

	@Override
	public String getSpecialEntityCategory(String name) {
		if (name.equals("Hologram")) {
			return "Other";
		}
		return null;
	}

	@Override
	public int getSpecialEntityTrackDistance(String name) {
		return -1;
	}
}
