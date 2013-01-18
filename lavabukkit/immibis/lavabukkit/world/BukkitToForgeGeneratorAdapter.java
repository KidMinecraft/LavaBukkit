package immibis.lavabukkit.world;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

public class BukkitToForgeGeneratorAdapter implements IChunkProvider {
	
	private ChunkGenerator wraps;
	private World world;
	private Random rand = new Random();
	private List<BlockPopulator> populators;
	
	public BukkitToForgeGeneratorAdapter(ChunkGenerator wraps, World world) {
		this.wraps = wraps;
		this.world = world;
		this.populators = wraps.getDefaultPopulators(world.getWorld());
	}

	@Override
	public boolean chunkExists(int x, int z) {
		return true;
	}

	@Override
	public Chunk provideChunk(int x, int z) {
		Chunk chunk = new Chunk(world, x, z);
		rand.setSeed((long)x * 341873128712L + (long)z * 132897987541L + world.getSeed());
		
		BiomeGrid biomes = new BiomeGridAdapter(chunk);
		short[][] extArray = wraps.generateExtBlockSections(world.getWorld(), rand, x, z, biomes);
		ExtendedBlockStorage[] ebsArray = chunk.getBlockStorageArray();
		if(extArray != null) {
			for(int k = 0; k < 16; k++) {
				if(extArray[k] != null) {
					ExtendedBlockStorage ebs = ebsArray[k] = new ExtendedBlockStorage(k, true);
					short[] ext = extArray[k];
					
					NibbleArray msb = ebs.createBlockMSBArray();
					byte[] lsb = ebs.getBlockLSBArray();
					
					for(int i = 0; i < 4096; i++) {
						short blockID = ext[i];
						lsb[i] = (byte)blockID;
						msb.set(i, 0, 0, blockID >> 8);
					}
					
				} else {
					ebsArray[k] = null;
				}
			}
			
		} else {
			byte[][] array = wraps.generateBlockSections(world.getWorld(), rand, x, z, biomes);
			if(array != null) {
				for(int k = 0; k < 16; k++) {
					if(array[k] != null) {
						ebsArray[k] = new ExtendedBlockStorage(k, true, array[k], null);
					} else {
						ebsArray[k] = null;
					}
				}
				
			} else {
				byte[] oldArray = wraps.generate(world.getWorld(), rand, x, z);
				int pos = 0;
				for(int x2 = 0; x2 < 16; x2++)
					for(int z2 = 0; z2 < 16; z2++)
						for(int y2 = 0; y2 < 128; y2++)
							if(oldArray[pos] != 0)
								chunk.setBlockID(x2, y2, z2, oldArray[pos] & 0xFF);
			}
		}
		
		chunk.generateSkylightMap();
		
		return chunk;
	}

	@Override
	public Chunk loadChunk(int var1, int var2) {
		return provideChunk(var1, var2);
	}

	@Override
	public void populate(IChunkProvider var1, int x, int z) {
		rand.setSeed(world.getSeed());
        long var7 = this.rand.nextLong() / 2L * 2L + 1L;
        long var9 = this.rand.nextLong() / 2L * 2L + 1L;
        long seed = (long)x * var7 + (long)z * var9 ^ world.getSeed();
        
        
        org.bukkit.Chunk bchunk = world.getWorld().getChunkAt(x, z);
		for(BlockPopulator bp : populators)
			bp.populate(world.getWorld(), rand, bchunk);
	}

	@Override
	public boolean saveChunks(boolean var1, IProgressUpdate var2) {
		return true;
	}

	@Override
	public boolean unload100OldestChunks() {
		return true;
	}

	@Override
	public boolean canSave() {
		return true;
	}

	@Override
	public String makeString() {
		return "Bukkit[" + wraps + "]";
	}

	@Override
	public List getPossibleCreatures(EnumCreatureType var1, int var2, int var3, int var4) {
		return world.getBiomeGenForCoords(var2, var4).getSpawnableList(var1);
	}

	@Override
	public ChunkPosition findClosestStructure(World var1, String var2, int var3, int var4, int var5) {
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		return 0;
	}

	@Override
	public void recreateStructures(int var1, int var2) {
	}

	public ChunkGenerator getWrappedProvider() {
		return wraps;
	}

}
