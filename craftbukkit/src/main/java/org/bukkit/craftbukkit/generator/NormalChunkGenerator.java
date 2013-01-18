package org.bukkit.craftbukkit.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;

public class NormalChunkGenerator extends InternalChunkGenerator {
    private final IChunkProvider provider;

    public NormalChunkGenerator(World world, long seed) {
        provider = world.provider.createChunkGenerator();
    }

    public byte[] generate(org.bukkit.World world, Random random, int x, int z) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public boolean canSpawn(org.bukkit.World world, int x, int z) {
        return ((CraftWorld) world).getHandle().provider.canCoordinateBeSpawn(x, z);
    }

    public List<BlockPopulator> getDefaultPopulators(org.bukkit.World world) {
        return new ArrayList<BlockPopulator>();
    }

    @Override
    public boolean chunkExists(int i, int i1) {
        return provider.chunkExists(i, i1);
    }

    @Override
    public Chunk provideChunk(int i, int i1) {
        return provider.provideChunk(i, i1);
    }

    @Override
    public Chunk loadChunk(int i, int i1) {
        return provider.loadChunk(i, i1);
    }

    @Override
    public void populate(IChunkProvider icp, int i, int i1) {
        provider.populate(icp, i, i1);
    }

    @Override
    public boolean saveChunks(boolean bln, IProgressUpdate ipu) {
        return provider.saveChunks(bln, ipu);
    }

    @Override
    public boolean unload100OldestChunks() {
        return provider.unload100OldestChunks();
    }

    @Override
    public boolean canSave() {
        return provider.canSave();
    }

    @Override
    public List<?> getPossibleCreatures(EnumCreatureType ect, int i, int i1, int i2) {
        return provider.getPossibleCreatures(ect, i, i1, i2);
    }

    @Override
    public ChunkPosition findClosestStructure(World world, String string, int i, int i1, int i2) {
        return provider.findClosestStructure(world, string, i, i1, i2);
    }

    @Override
    public void recreateStructures(int i, int j) {
        provider.recreateStructures(i, j);
    }

    // n.m.s implementations always return 0. (The true implementation is in ChunkProviderServer)
    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public String makeString() {
        return "NormalWorldGenerator";
    }
}
