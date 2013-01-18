package immibis.lavabukkit.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.bukkit.generator.ChunkGenerator;

public class BukkitWorldDescriptor {
	public final String name;
	public final int dimensionID; // dimension ID on the server
	public final boolean isBukkitCreatedWorld;	
	public int clientDimensionID; // dimension ID that is sent to clients
	public long seed;
	public ChunkGenerator generator; // not persisted
	public WorldType worldType;
	public boolean generateStructures;
	
	public BukkitWorldDescriptor(String name, int serverDimension, boolean isBukkitCreatedWorld) {
		this.name = name;
		this.dimensionID = serverDimension;
		this.isBukkitCreatedWorld = isBukkitCreatedWorld;
	}

	public static BukkitWorldDescriptor read(DataInputStream in) throws IOException {
		int version = in.readInt();
		if(version > 2)
			throw new IOException("This lavaworlds.dat is from a newer version of LavaBukkit and cannot be read");
		if(version < 2)
			throw new IOException("Corrupted lavaworlds.dat");
		
		String name = in.readUTF();
		int serverDim = in.readInt();
		boolean isBukkitCreatedWorld = in.readBoolean();
		BukkitWorldDescriptor bwd = new BukkitWorldDescriptor(name, serverDim, isBukkitCreatedWorld);
		bwd.clientDimensionID = in.readInt();
		bwd.seed = in.readLong();
		bwd.worldType = WorldType.parseWorldType(in.readUTF());
		bwd.generateStructures = in.readBoolean();
		
		return bwd;
	}
	
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(2); // version
		out.writeUTF(name);
		out.writeInt(dimensionID);
		out.writeBoolean(isBukkitCreatedWorld);
		out.writeInt(clientDimensionID);
		out.writeLong(seed);
		out.writeUTF(worldType.getWorldTypeName());
		out.writeBoolean(generateStructures);
	}

	public WorldSettings toWorldSettings() {
		WorldSettings ws = new WorldSettings(seed, MinecraftServer.getServer().worldServerForDimension(0).getWorldInfo().getGameType(), generateStructures, false, worldType);
		return ws;
	}
}
