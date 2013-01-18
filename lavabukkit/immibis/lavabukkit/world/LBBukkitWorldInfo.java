package immibis.lavabukkit.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldInfo;

public class LBBukkitWorldInfo extends WorldInfo {

	public LBBukkitWorldInfo(NBTTagCompound tag, BukkitWorldDescriptor bwd) {
		super(tag);
	}
	
	public LBBukkitWorldInfo(BukkitWorldDescriptor bwd) {
		super(bwd.toWorldSettings(), bwd.name);
	}
}
