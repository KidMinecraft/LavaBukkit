package immibis.lavabukkit.world;

import immibis.lavabukkit.LavaBukkitMod;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;

import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;

import com.google.common.collect.ImmutableMap;

/**
 * On starting a server:
 * 1. BukkitWorldRegistry is created. Saved BWD's are loaded.
 * 2. Worlds are loaded. Any missing BWD's are added.
 *    If a Bukkit-created world is missing a BWD, this is a fatal error.
 *
 * A BukkitWorldDescriptor stores Bukkit-visible information about a world.
 * For Bukkit-created worlds, it is the definitive source for this information.
 * For Forge-created worlds, the information is copied to it when the world is loaded.
 * 
 * Bukkit-created worlds have a LBBukkitWorldInfo which retrieves world-specific
 * information from the BWD.
 * Forge-created worlds have a LBMultiWorldInfo, which adjusts only the world name.
 * 
 * Bukkit-created worlds use a BukkitWorldProvider.
 */
public class BukkitWorldRegistry {
	private final File dataFile;
	
	private Map<Integer, BukkitWorldDescriptor> byDimension = new HashMap<Integer, BukkitWorldDescriptor>();
	private Map<String, BukkitWorldDescriptor> byName = new HashMap<String, BukkitWorldDescriptor>();
	
	public static BukkitWorldRegistry getInstance() {
		CraftServer server = (CraftServer)Bukkit.getServer();
		if(server != null)
			return server.getWorldRegistry();
		else
			return null;
	}
	
	public BukkitWorldRegistry(File dataDir, String baseWorldName) {
		this.dataFile = new File(dataDir, "lavaworlds.dat");
		
		if(dataFile.exists()) {
			try {
				DataInputStream in = new DataInputStream(new FileInputStream(dataFile));
				int version = in.readInt();
				if(version > 1) {
					in.close();
					throw new IOException("This lavaworlds.dat is from a newer version of LavaBukkit and cannot be read");
				}
				if(version < 1) {
					in.close();
					throw new IOException("Corrupted lavaworlds.dat");
				}
				
				
				int count = in.readInt();
				
				for(int k = 0; k < count; k++) {
					BukkitWorldDescriptor bwd = BukkitWorldDescriptor.read(in);
					byDimension.put(bwd.dimensionID, bwd);
					byName.put(bwd.name, bwd);
				}
				
				in.close();
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			//BukkitWorldDescriptor overworld = new BukkitWorldDescriptor(baseWorldName, 0, false);
			//copyWorldInfo(overworld);
			//add(overworld);
		}
		
		//addPreregisteredDimensions();
	}
	
	private void copyWorldInfo(BukkitWorldDescriptor bwd, WorldServer world) {
		bwd.worldType = world.getWorldInfo().getTerrainType();
		bwd.generateStructures = world.getWorldInfo().isMapFeaturesEnabled();
		bwd.seed = world.getSeed();
		if(!bwd.isBukkitCreatedWorld || bwd.dimensionID == 0 || bwd.dimensionID == -1 || bwd.dimensionID == 1)
			bwd.clientDimensionID = bwd.dimensionID;
		else
			bwd.clientDimensionID = 0;
	}

	private void save() {
		try {
			DataOutputStream out = new DataOutputStream(new FileOutputStream(dataFile));
			
			out.writeInt(1); // version
			out.writeInt(byDimension.size());
			
			for(BukkitWorldDescriptor bwd : byDimension.values())
				bwd.write(out);

			out.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public BukkitWorldRegistry(ISaveHandler saveHandler) {
		this(((SaveHandler)saveHandler).getSaveDirectory(), saveHandler.getSaveDirectoryName());
	}

	
	
	// An error is thrown if the world doesn't exist when getting it by dimension ID,
	// but not when by name. Because if the world didn't exist, you wouldn't have a
	// dimension ID to look up.
	public BukkitWorldDescriptor getByDimension(int dimensionId) {
		BukkitWorldDescriptor bwd = byDimension.get(dimensionId);
		if(bwd == null)
			throw new AssertionError("No BukkitWorldDescriptor for dimension "+dimensionId);
		return bwd;
	}
	
	public BukkitWorldDescriptor getByName(String name) {
		return byName.get(name);
	}
	
	private void add(BukkitWorldDescriptor bwd) {
		if(byDimension.containsKey(bwd.dimensionID))
			throw new IllegalArgumentException("A world with dimension ID "+bwd.dimensionID+" is already known (existing name: "+byDimension.get(bwd.dimensionID)+", new name: "+bwd.name+")");
		if(byName.containsKey(bwd.name))
			throw new IllegalArgumentException("A world with name "+bwd.name+" is already known (existing dim ID: "+byName.get(bwd.name).dimensionID+", new dim ID: "+bwd.dimensionID+")");
		byDimension.put(bwd.dimensionID, bwd);
		byName.put(bwd.name, bwd);
		
		save();
	}
	
	private static final ImmutableMap<Integer, String> defaultWorlds = ImmutableMap.<Integer, String>builder()
		.put(-1, "world_nether")
		.put(0, "world")
		.put(1, "world_the_end")
		.build();

	public void addBukkitWorld(BukkitWorldDescriptor bwd) {
		add(bwd);
	}

	public BukkitWorldDescriptor onDimensionLoad(int dim, WorldServer world) {
		if(byDimension.containsKey(dim))
			return byDimension.get(dim);
		
		int provider = DimensionManager.getProviderType(dim);
		if(!byDimension.containsKey(dim)) {
			if(provider == LavaBukkitMod.LB_PROVIDER_TYPE_ID)
				throw new AssertionError("Bukkit-created world has no loaded descriptor");
			
			String name;
			if(defaultWorlds.containsKey(provider)) {
				name = defaultWorlds.get(provider);
				if(dim != provider)
					name += "_" + dim;
			} else {
				name = "world_dim" + dim;
			}
			System.out.println("Registering Bukkit world");
			BukkitWorldDescriptor bwd = new BukkitWorldDescriptor(name, dim, false);
			copyWorldInfo(bwd, world);
			add(bwd);
		}

		return byDimension.get(dim);
	}
	
	public static void initWorldServer(WorldServer world) {
		int dim = world.provider.dimensionId;
		BukkitWorldDescriptor bwd = world.getServer().getWorldRegistry().onDimensionLoad(dim, world);
		if(bwd.isBukkitCreatedWorld) {
			
			File var1 = new File(world.getChunkSaveLocation(), "level.dat");
	        NBTTagCompound var2;
	        NBTTagCompound var3;
	        WorldInfo worldInfo = null;
	        if (var1.exists())
	        {
	            try
	            {
	                var2 = CompressedStreamTools.readCompressed(new FileInputStream(var1));
	                var3 = var2.getCompoundTag("Data");
	                world.worldInfo = new LBBukkitWorldInfo(var3, bwd);
	            }
	            catch (Exception var5)
	            {
	                var5.printStackTrace();
	            }
	        } else
	        	world.worldInfo = new LBBukkitWorldInfo(bwd);
	        
		}
		world.world = new CraftWorld((WorldServer)world); // LavaBukkit
		world.getServer().addWorld(world.getWorld());
	}
	
	public static void createWorld(WorldCreator creator) {
		BukkitWorldDescriptor bwd = BukkitWorldRegistry.getInstance().getByName(creator.name());
		boolean creating = false;
		if(bwd == null) {
			bwd = new BukkitWorldDescriptor(creator.name(), DimensionManager.getNextFreeDimId(), true);
			creating = true;
		}
		
		bwd.clientDimensionID = creator.environment().getId();
		bwd.seed = creator.seed();
		bwd.worldType = WorldType.parseWorldType(creator.type().getName());
		bwd.generator = creator.generator();
		bwd.generateStructures = creator.generateStructures();
		
		if(creating)
			BukkitWorldRegistry.getInstance().addBukkitWorld(bwd);
		
		DimensionManager.registerDimension(bwd.dimensionID, LavaBukkitMod.LB_PROVIDER_TYPE_ID);
		WorldServer world = MinecraftServer.getServer().worldServerForDimension(bwd.dimensionID);
		
		world.difficultySetting = 1;
        world.setAllowedSpawnTypes(true, true);
        
        Bukkit.getServer().getPluginManager().callEvent(new WorldInitEvent(world.getWorld()));
		
		if (world.getWorld().getKeepSpawnInMemory()) {
			ChunkCoordinates spawn = world.getSpawnPoint();
            short short1 = 196;
            long i = System.currentTimeMillis();
            for (int j = -short1; j <= short1; j += 16) {
                for (int k = -short1; k <= short1; k += 16) {
                    long l = System.currentTimeMillis();

                    if (l < i) {
                        i = l;
                    }

                    if (l > i + 1000L) {
                        int i1 = (short1 * 2 + 1) * (short1 * 2 + 1);
                        int j1 = (j + short1) * (short1 * 2 + 1) + k + 1;

                        System.out.println("Preparing spawn area for " + creator.name() + ", " + (j1 * 100 / i1) + "%");
                        i = l;
                    }

                    
                    world.theChunkProviderServer.provideChunk(spawn.posX + j >> 4, spawn.posZ + k >> 4);
                }
            }
        }
	}
}
