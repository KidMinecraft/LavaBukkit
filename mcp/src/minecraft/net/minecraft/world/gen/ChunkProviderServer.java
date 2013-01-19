package net.minecraft.world.gen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Server;
import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.craftbukkit.util.LongHashSet;
import org.bukkit.craftbukkit.util.LongObjectHashMap;
import org.bukkit.event.world.ChunkUnloadEvent;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.BlockSand;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;

public class ChunkProviderServer implements IChunkProvider
{
    /**
     * used by unload100OldestChunks to iterate the loadedChunkHashMap for unload (underlying assumption, first in,
     * first out)
     */
    public LongHashSet chunksToUnload = new LongHashSet(); // CraftBukkit; Set/HashSet -> LongHashSet; private -> public
    public Chunk defaultEmptyChunk; // CraftBukkit; private -> public
    public IChunkProvider currentChunkProvider; // CraftBukkit; private -> public
    public IChunkLoader currentChunkLoader;

    /**
     * if this is false, the defaultEmptyChunk will be returned by the provider
     */
    public boolean loadChunkOnProvideRequest = false; // CraftBukkit; true -> false 
    public LongObjectHashMap<Chunk> loadedChunkHashMap = new LongObjectHashMap<Chunk>(); // CraftBukkit; LongHashMap -> LongObjectHashMap<Chunk>, private -> public
    private List loadedChunks = new ArrayList();
    public WorldServer worldObj; // CraftBukkit - private -> public

    public ChunkProviderServer(WorldServer par1WorldServer, IChunkLoader par2IChunkLoader, IChunkProvider par3IChunkProvider)
    {
        this.defaultEmptyChunk = new EmptyChunk(par1WorldServer, 0, 0);
        this.worldObj = par1WorldServer;
        this.currentChunkLoader = par2IChunkLoader;
        this.currentChunkProvider = par3IChunkProvider;
    }

    /**
     * Checks to see if a chunk exists at x, y
     */
    public boolean chunkExists(int par1, int par2)
    {
        return this.loadedChunkHashMap.containsKey(LongHash.toLong(par1, par2)); // CraftBukkit
    }

    /**
     * marks chunk for unload by "unload100OldestChunks"  if there is no spawn point, or if the center of the chunk is
     * outside 200 blocks (x or z) of the spawn
     */
    public void unloadChunksIfNotNearSpawn(int par1, int par2)
    {
        if (this.worldObj.provider.canRespawnHere() && DimensionManager.shouldLoadSpawn(this.worldObj.provider.dimensionId))
        {
            ChunkCoordinates var3 = this.worldObj.getSpawnPoint();
            int var4 = par1 * 16 + 8 - var3.posX;
            int var5 = par2 * 16 + 8 - var3.posZ;
            short var6 = 128;

            if (var4 < -var6 || var4 > var6 || var5 < -var6 || var5 > var6)
            {
            	// CraftBukkit start
                this.chunksToUnload.add(par1, par2);
                
                Chunk c = loadedChunkHashMap.get(LongHash.toLong(par1, par2));
                if(c != null)
                	c.mustSave = true;
                // CraftBukkit end
            }
        }
        else
        {
        	// CraftBukkit start
            this.chunksToUnload.add(par1, par2);
            
            Chunk c = loadedChunkHashMap.get(LongHash.toLong(par1, par2));
            if(c != null)
            	c.mustSave = true;
            // CraftBukkit end
        }
    }

    /**
     * marks all chunks for unload, ignoring those near the spawn
     */
    public void unloadAllChunks()
    {
        Iterator var1 = this.loadedChunkHashMap.values().iterator(); // CraftBukkit

        while (var1.hasNext())
        {
            Chunk var2 = (Chunk)var1.next();
            this.unloadChunksIfNotNearSpawn(var2.xPosition, var2.zPosition);
        }
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int par1, int par2)
    {
    	// CraftBukkit start - add async variant, provide compatibility
    	return loadChunk(par1, par2, null);
    }
    
    public Chunk loadChunk(int par1, int par2, Runnable runnable)
    {
        long var3 = ChunkCoordIntPair.chunkXZ2Int(par1, par2);
        this.chunksToUnload.remove(LongHash.toLong(par1, par2)); // CraftBukkit
        Chunk var5 = (Chunk)this.loadedChunkHashMap.get(LongHash.toLong(par1, par2)); // CraftBukkit
        boolean newChunk = false;
        // CraftBukkit end

        if (var5 == null)
        {
            var5 = ForgeChunkManager.fetchDormantChunk(var3, this.worldObj);
            if (var5 == null)
            {
            	// CraftBukkit start
            	// LavaBukkit - TODO disabled async chunkloading for now
            	if(false && runnable != null && currentChunkLoader instanceof AnvilChunkLoader && ((AnvilChunkLoader)currentChunkLoader).chunkExists(worldObj, par1, par2)) {
            		ChunkIOExecutor.queueChunkLoad(worldObj, (AnvilChunkLoader)currentChunkLoader, this, par1, par2, runnable);
            		return null;
            	}
            	// CraftBukkit end
                var5 = this.safeLoadChunk(par1, par2);
            }

            if (var5 == null)
            {
                if (this.currentChunkProvider == null)
                {
                    var5 = this.defaultEmptyChunk;
                }
                else
                {
                    try
                    {
                        var5 = this.currentChunkProvider.provideChunk(par1, par2);
                    }
                    catch (Throwable var9)
                    {
                        CrashReport var7 = CrashReport.makeCrashReport(var9, "Exception generating new chunk");
                        CrashReportCategory var8 = var7.makeCategory("Chunk to be generated");
                        var8.addCrashSection("Location", String.format("%d,%d", new Object[] {Integer.valueOf(par1), Integer.valueOf(par2)}));
                        var8.addCrashSection("Position hash", Long.valueOf(var3));
                        var8.addCrashSection("Generator", this.currentChunkProvider.makeString());
                        throw new ReportedException(var7);
                    }
                }
                newChunk = true; // CraftBukkit
            }

            this.loadedChunkHashMap.put(LongHash.toLong(par1, par2), var5); // CraftBukkit
            this.loadedChunks.add(var5);

            if (var5 != null)
            {
                var5.onChunkLoad();
            }
            
            // CraftBukkit start
            Server server = this.worldObj.getServer();
            if (server != null) {
                /*
                 * If it's a new world, the first few chunks are generated inside
                 * the World constructor. We can't reliably alter that, so we have
                 * no way of creating a CraftWorld/CraftServer at that point.
                 */
                server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(var5.bukkitChunk, newChunk));
            }
            // CraftBukkit end

            var5.populateChunk(this, this, par1, par2);
        }
        

        // CraftBukkit start - If we didn't need to load the chunk run the callback now
        if (runnable != null) {
            runnable.run();
        }
        // CraftBukkit end

        return var5;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int par1, int par2)
    {
    	// CraftBukkit start
        Chunk chunk = (Chunk) this.loadedChunkHashMap.get(LongHash.toLong(par1, par2));

        chunk = chunk == null ? (!this.worldObj.findingSpawnPoint && !this.loadChunkOnProvideRequest ? this.defaultEmptyChunk : this.loadChunk(par1, par2)) : chunk;
        if (chunk == this.defaultEmptyChunk) return chunk;
        if (par1 != chunk.xPosition || par2 != chunk.zPosition) {
            MinecraftServer.logger.severe("Chunk (" + chunk.xPosition + ", " + chunk.zPosition + ") stored at  (" + par1 + ", " + par2 + ") in world '" + worldObj.getWorld().getName() + "'");
            MinecraftServer.logger.severe(chunk.getClass().getName());
            Throwable ex = new Throwable();
            ex.fillInStackTrace();
            ex.printStackTrace();
        }
        return chunk;
        // CraftBukkit end
    }

    /**
     * used by loadChunk, but catches any exceptions if the load fails.
     */
    public Chunk safeLoadChunk(int par1, int par2) // CraftBukkit - private -> public. CB name: loadChunk
    {
        if (this.currentChunkLoader == null)
        {
            return null;
        }
        else
        {
            try
            {
                Chunk var3 = this.currentChunkLoader.loadChunk(this.worldObj, par1, par2);

                if (var3 != null)
                {
                    var3.lastSaveTime = this.worldObj.getTotalWorldTime();

                    if (this.currentChunkProvider != null)
                    {
                        this.currentChunkProvider.recreateStructures(par1, par2);
                    }
                }

                return var3;
            }
            catch (Exception var4)
            {
                var4.printStackTrace();
                return null;
            }
        }
    }

    /**
     * used by saveChunks, but catches any exceptions if the save fails.
     */
    public void safeSaveExtraChunkData(Chunk par1Chunk) // CraftBukkit: private -> public. CB name: saveChunkNOP
    {
        if (this.currentChunkLoader != null)
        {
            try
            {
                this.currentChunkLoader.saveExtraChunkData(this.worldObj, par1Chunk);
            }
            catch (Exception var3)
            {
                var3.printStackTrace();
            }
        }
    }

    /**
     * used by saveChunks, but catches any exceptions if the save fails.
     */
    public void safeSaveChunk(Chunk par1Chunk) // CraftBukkit: private -> public. CB name: saveChunk
    {
        if (this.currentChunkLoader != null)
        {
            try
            {
                par1Chunk.lastSaveTime = this.worldObj.getTotalWorldTime();
                this.currentChunkLoader.saveChunk(this.worldObj, par1Chunk);
            }
            catch (Exception var3) // CraftBukkit: IOException -> Exception
            {
                var3.printStackTrace();
            }
            // CraftBukkit start - remove extra exception
            /*catch (MinecraftException var4)
            {
                var4.printStackTrace();
            }*/
            // CraftBukkit end
        }
    }

    /**
     * Populates chunk with ores etc etc
     */
    public void populate(IChunkProvider par1IChunkProvider, int par2, int par3)
    {
        Chunk var4 = this.provideChunk(par2, par3);

        if (!var4.isTerrainPopulated)
        {
            var4.isTerrainPopulated = true;

            if (this.currentChunkProvider != null)
            {
                this.currentChunkProvider.populate(par1IChunkProvider, par2, par3);
                
                // CraftBukkit start
                if(var4.bukkitChunk == null)
                	return; // LavaBukkit bugfix?
                
                // TODO: move BlockPopulator calls to an IWorldGenerator
                BlockSand.fallInstantly = true;
                Random random = new Random();
                random.setSeed(worldObj.getSeed());
                long xRand = random.nextLong() / 2L * 2L + 1L;
                long zRand = random.nextLong() / 2L * 2L + 1L;
                random.setSeed((long) par2 * xRand + (long) par3 * zRand ^ worldObj.getSeed());

                org.bukkit.World world = worldObj.getWorld();
                if (world != null) {
                    for (org.bukkit.generator.BlockPopulator populator : world.getPopulators()) {
                        populator.populate(world, random, var4.bukkitChunk);
                    }
                }	
                BlockSand.fallInstantly = false;
                worldObj.getServer().getPluginManager().callEvent(new org.bukkit.event.world.ChunkPopulateEvent(var4.bukkitChunk));
                // CraftBukkit end


                GameRegistry.generateWorld(par2, par3, worldObj, currentChunkProvider, par1IChunkProvider);
                var4.setChunkModified();
            }
        }
    }

    /**
     * Two modes of operation: if passed true, save all Chunks in one go.  If passed false, save up to two chunks.
     * Return true if all chunks have been saved.
     */
    public boolean saveChunks(boolean par1, IProgressUpdate par2IProgressUpdate)
    {
        int var3 = 0;

        for(Chunk var5 : this.loadedChunkHashMap.values()) { // CraftBukkit
        
            if (par1)
            {
                this.safeSaveExtraChunkData(var5);
            }

            if (var5.needsSaving(par1))
            {
                this.safeSaveChunk(var5);
                var5.isModified = false;
                ++var3;

                if (var3 == 24 && !par1)
                {
                    return false;
                }
            }
        }

        if (par1)
        {
            if (this.currentChunkLoader == null)
            {
                return true;
            }

            this.currentChunkLoader.saveExtraData();
        }

        return true;
    }

    /**
     * Unloads the 100 oldest chunks from memory, due to a bug with chunkSet.add() never being called it thinks the list
     * is always empty and will not remove any chunks.
     */
    public boolean unload100OldestChunks()
    {
        if (!this.worldObj.canNotSave)
        {
            for (ChunkCoordIntPair forced : this.worldObj.getPersistentChunks().keySet())
            {
                this.chunksToUnload.remove(ChunkCoordIntPair.chunkXZ2Int(forced.chunkXPos, forced.chunkZPos));
            }
            
            // CraftBukkit start
            Server server = this.worldObj.getServer();
            for (int i = 0; i < 100 && !this.chunksToUnload.isEmpty(); i++) {
                long chunkcoordinates = this.chunksToUnload.popFirst();
                Chunk var3 = this.loadedChunkHashMap.get(chunkcoordinates);
                if (var3 == null) continue;

                ChunkUnloadEvent event = new ChunkUnloadEvent(var3.bukkitChunk);
                server.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    var3.onChunkUnload();
                    this.safeSaveChunk(var3);
                    this.safeSaveExtraChunkData(var3);
                    // this.chunksToUnload.remove(integer);
                    this.loadedChunkHashMap.remove(chunkcoordinates); // CraftBukkit
                    this.loadedChunks.remove(var3);
                    
                    ForgeChunkManager.putDormantChunk(ChunkCoordIntPair.chunkXZ2Int(var3.xPosition, var3.zPosition), var3);
                    if(loadedChunks.size() == 0 && ForgeChunkManager.getPersistentChunksFor(this.worldObj).size() == 0 && !DimensionManager.shouldLoadSpawn(this.worldObj.provider.dimensionId)) {
                        DimensionManager.unloadWorld(this.worldObj.provider.dimensionId);
                        return currentChunkProvider.unload100OldestChunks();
                    }
                }
            }

            if (this.currentChunkLoader != null)
            {
                this.currentChunkLoader.chunkTick();
            }
        }

        return this.currentChunkProvider.unload100OldestChunks();
    }

    /**
     * Returns if the IChunkProvider supports saving.
     */
    public boolean canSave()
    {
        return !this.worldObj.canNotSave;
    }

    /**
     * Converts the instance data to a readable string.
     */
    public String makeString()
    {
        return "ServerChunkCache: " + this.loadedChunkHashMap.size() + " Drop: " + this.chunksToUnload.size(); // CraftBukkit
    }

    /**
     * Returns a list of creatures of the specified type that can spawn at the given location.
     */
    public List getPossibleCreatures(EnumCreatureType par1EnumCreatureType, int par2, int par3, int par4)
    {
        return this.currentChunkProvider.getPossibleCreatures(par1EnumCreatureType, par2, par3, par4);
    }

    /**
     * Returns the location of the closest structure of the specified type. If not found returns null.
     */
    public ChunkPosition findClosestStructure(World par1World, String par2Str, int par3, int par4, int par5)
    {
        return this.currentChunkProvider.findClosestStructure(par1World, par2Str, par3, par4, par5);
    }

    public int getLoadedChunkCount()
    {
        return this.loadedChunkHashMap.size(); // CraftBukkit
    }

    public void recreateStructures(int par1, int par2) {}
}
