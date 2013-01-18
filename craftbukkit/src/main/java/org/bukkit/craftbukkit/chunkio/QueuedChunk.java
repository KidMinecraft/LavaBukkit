package org.bukkit.craftbukkit.chunkio;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;

class QueuedChunk {
    long coords;
    AnvilChunkLoader loader;
    World world;
    ChunkProviderServer provider;
    NBTTagCompound compound;

    public QueuedChunk(long coords, AnvilChunkLoader loader, World world, ChunkProviderServer provider) {
        this.coords = coords;
        this.loader = loader;
        this.world = world;
        this.provider = provider;
    }

    @Override
    public int hashCode() {
        return (int) coords ^ world.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof QueuedChunk) {
            QueuedChunk other = (QueuedChunk) object;
            return coords == other.coords && world == other.world;
        }

        return false;
    }
}
