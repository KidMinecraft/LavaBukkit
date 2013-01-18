package org.bukkit.craftbukkit.chunkio;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ChunkLoader;

import org.bukkit.Server;
import org.bukkit.craftbukkit.util.AsynchronousExecutor;
import org.bukkit.craftbukkit.util.LongHash;

class ChunkIOProvider implements AsynchronousExecutor.CallBackProvider<QueuedChunk, Chunk, Runnable, RuntimeException> {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    // async stuff
    public Chunk callStage1(QueuedChunk queuedChunk) throws RuntimeException {
        AnvilChunkLoader loader = queuedChunk.loader;
        try {
	        Object[] data = loader.loadChunkCB(queuedChunk.world, LongHash.msw(queuedChunk.coords), LongHash.lsw(queuedChunk.coords));
	
	        if (data != null) {
	            queuedChunk.compound = (NBTTagCompound) data[1];
	            return (Chunk) data[0];
	        }
	
	        return null;
        } catch(IOException e) {
        	throw new RuntimeException(e);
        }
    }

    // sync stuff
    public void callStage2(QueuedChunk queuedChunk, Chunk chunk) throws RuntimeException {
        if(chunk == null) {
            // If the chunk loading failed just do it synchronously (may generate)
            queuedChunk.provider.provideChunk(LongHash.msw(queuedChunk.coords), LongHash.lsw(queuedChunk.coords));
            return;
        }

        int x = LongHash.msw(queuedChunk.coords);
        int z = LongHash.lsw(queuedChunk.coords);

        // See if someone already loaded this chunk while we were working on it (API, etc)
        if (queuedChunk.provider.loadedChunkHashMap.containsKey(queuedChunk.coords)) {
            // Make sure it isn't queued for unload, we need it
            queuedChunk.provider.chunksToUnload.remove(queuedChunk.coords);
            return;
        }

        queuedChunk.loader.loadEntities(chunk, queuedChunk.compound.getCompoundTag("Level"), queuedChunk.world);
        chunk.lastSaveTime = queuedChunk.provider.worldObj.getWorldTime();
        queuedChunk.provider.loadedChunkHashMap.put(queuedChunk.coords, chunk);
        chunk.onChunkLoad();

        if (queuedChunk.provider.currentChunkProvider != null) {
            queuedChunk.provider.currentChunkProvider.recreateStructures(x, z);
        }

        Server server = queuedChunk.provider.worldObj.getServer();
        if (server != null) {
            server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk.bukkitChunk, false));
        }
        
        chunk.populateChunk(queuedChunk.provider, queuedChunk.provider, x, z);
    }

    public void callStage3(QueuedChunk queuedChunk, Chunk chunk, Runnable runnable) throws RuntimeException {
        runnable.run();
    }

    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "Chunk I/O Executor Thread-" + threadNumber.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
