/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2019 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl.versioned;

import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * 1.15 hides some NBT constructors; therefore, this is used to call
 * the relevant static methods instead in 1.15.
 */
final class NBTFunctions {
	private NBTFunctions() { throw new AssertionError(); }

	/* (non-javadoc)
	 * @see VersionedFunctions#nbtString
	 */
	static String nbtString(Tag tag) {
		return tag.getPrettyDisplay("    ", 0).getString();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#createFloatListTag
	 */
	static ListTag createFloatListTag(float... values) {
		ListTag result = new ListTag();
		for (float value : values) {
			result.add(FloatTag.valueOf(value));
		}
		return result;
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#createDoubleListTag
	 */
	static ListTag createDoubleListTag(double... values) {
		ListTag result = new ListTag();
		for (double value : values) {
			result.add(DoubleTag.valueOf(value));
		}
		return result;
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#createShortListTag
	 */
	static ListTag createShortListTag(short... values) {
		ListTag result = new ListTag();
		for (short value : values) {
			result.add(ShortTag.valueOf(value));
		}
		return result;
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#createStringTag
	 */
	static StringTag createStringTag(String value) {
		return StringTag.valueOf(value);
	}
}