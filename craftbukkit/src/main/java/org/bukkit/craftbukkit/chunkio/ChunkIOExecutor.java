package org.bukkit.craftbukkit.chunkio;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;

import org.bukkit.craftbukkit.util.AsynchronousExecutor;
import org.bukkit.craftbukkit.util.LongHash;

public class ChunkIOExecutor {
    static final int BASE_THREADS = 1;
    static final int PLAYERS_PER_THREAD = 50;

    private static final AsynchronousExecutor<QueuedChunk, Chunk, Runnable, RuntimeException> instance = new AsynchronousExecutor<QueuedChunk, Chunk, Runnable, RuntimeException>(new ChunkIOProvider(), BASE_THREADS);

    public static void waitForChunkLoad(World world, int x, int z) {
        instance.get(new QueuedChunk(LongHash.toLong(x, z), null, world, null));
    }

    public static void queueChunkLoad(World world, AnvilChunkLoader loader, ChunkProviderServer provider, int x, int z, Runnable runnable) {
        instance.add(new QueuedChunk(LongHash.toLong(x, z), loader, world, provider), runnable);
    }

    public static void adjustPoolSize(int players) {
        int size = Math.max(BASE_THREADS, (int) Math.ceil(players / PLAYERS_PER_THREAD));
        instance.setActiveThreads(size);
    }

    public static void tick() {
        instance.finishActive();
    }
}
