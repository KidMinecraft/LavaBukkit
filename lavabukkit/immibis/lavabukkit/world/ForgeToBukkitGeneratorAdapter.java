package immibis.lavabukkit.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class ForgeToBukkitGeneratorAdapter extends ChunkGenerator {
	private final IChunkProvider wraps;
	private final WorldProvider wrapsWP;
	
	public ForgeToBukkitGeneratorAdapter(IChunkProvider wraps, WorldProvider wrapsWP) {
		this.wraps = wraps;
		this.wrapsWP = wrapsWP;
	}
	
	@Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return null;
    }
    
	@Override
    public boolean canSpawn(World world, int x, int z) {
		return wrapsWP.canCoordinateBeSpawn(x, z);
    }
	
	private void copyBiomesFromChunk(BiomeGrid biomes, Chunk chunk) {
		for(int z2 = 0; z2 < 16; z2++)
			for(int x2 = 0; x2 < 16; x2++) {
				byte id = chunk.getBiomeArray()[(z2 << 4) | x2];
				if(id >= 0 && id < BiomeGenBase.biomeList.length && BiomeGenBase.biomeList[id] != null)
					biomes.setBiome(x2, z2, CraftBlock.biomeBaseToBiome(BiomeGenBase.biomeList[id]));
			}
	}
	
	@Override
	public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
		Chunk chunk = wraps.provideChunk(x, z);
		ExtendedBlockStorage[] ebsArray = chunk.getBlockStorageArray();
		
		byte[][] rv = new byte[ebsArray.length][];
		for(int k = 0; k < rv.length; k++)
			rv[k] = ebsArray[k] == null ? null : ebsArray[k].getBlockLSBArray();
		
		copyBiomesFromChunk(biomes, chunk);
		
		return rv;
	}
	
	@Override
	public short[][] generateExtBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
		Chunk chunk = wraps.provideChunk(x, z);
		
		ExtendedBlockStorage[] ebsArray = chunk.getBlockStorageArray();
		
		short[][] rv = new short[ebsArray.length][];
		for(int k = 0; k < rv.length; k++) {
			ExtendedBlockStorage ebs = ebsArray[k];
			
			if(ebs == null) {
				rv[k] = null;
				continue;
			}
			
			short[] rvs = rv[k] = new short[4096];
			
			byte[] lsb = ebs.getBlockLSBArray();
			NibbleArray msb = ebs.getBlockMSBArray();
			
			if(msb == null) {
				for(int i = 0; i < 4096; i++)
					rvs[i] = (short)(lsb[i] & 255);
			} else {
				for(int i = 0; i < 4096; i++)
					rvs[i] = (short)((lsb[i] & 255) | (msb.get(i, 0, 0) << 8));
			}
		}
		
		copyBiomesFromChunk(biomes, chunk);
		
		return rv;
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		return new ArrayList<BlockPopulator>(); // TODO return default populators
	}
}
