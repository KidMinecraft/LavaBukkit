package immibis.lavabukkit.world;

import java.io.File;
import java.io.FileOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;

public class WorldSaveHandler {
	@ForgeSubscribe
	public void onSave(WorldEvent.Save evt) {
		if(evt.world instanceof WorldServer && evt.world.getWorldInfo() instanceof LBBukkitWorldInfo) {
			NBTTagCompound tag = evt.world.getWorldInfo().cloneNBTCompound(null);
			
			File var1 = new File(((WorldServer)evt.world).getChunkSaveLocation(), "level.dat");
			try {
				CompressedStreamTools.writeCompressed(tag, new FileOutputStream(var1));
			} catch(Exception e) {
				new Exception("Failed to write level.dat for "+evt.world.getWorldInfo().getWorldName()+" ("+evt.world.provider.dimensionId+")", e).printStackTrace();
			}
	        
		}
	}
}
