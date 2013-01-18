package immibis.lavabukkit.world;

import net.minecraft.world.chunk.Chunk;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

public class BiomeGridAdapter implements BiomeGrid {
	
	private Chunk chunk;

	public BiomeGridAdapter(Chunk chunk) {
		this.chunk = chunk;
	}

	@Override
	public Biome getBiome(int x, int z) {
		return CraftBlock.biomeBaseToBiome(chunk.getBiomeGenForWorldCoords(x, z, chunk.worldObj.getWorldChunkManager()));}

	@Override
	public void setBiome(int x, int z, Biome bio) {
		chunk.getBiomeArray()[(z << 4) | x] = (byte)CraftBlock.biomeToBiomeBase(bio).biomeID;
	}

}
