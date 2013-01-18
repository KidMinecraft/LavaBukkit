package net.minecraft.util;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;

public class Vec3Pool
{
    private final int truncateArrayResetThreshold;
    private final int minimumSize;

    /** items at and above nextFreeSpace are assumed to be available */
    // CraftBukkit start
    private final List vec3Cache = new ArrayList();
    private Vec3 freelist = null;
    private Vec3 alloclist = null;
    private Vec3 freelisthead = null;
    private Vec3 alloclisthead = null;
    private int total_size = 0;
    // CraftBukkit end
    
    private int nextFreeSpace = 0;
    private int maximumSizeSinceLastTruncation = 0;
    private int resetCount = 0;

    public Vec3Pool(int par1, int par2)
    {
        this.truncateArrayResetThreshold = par1;
        this.minimumSize = par2;
    }

    /**
     * extends the pool if all vecs are currently "out"
     */
    public final Vec3 getVecFromPool(double par1, double par3, double par5) // CraftBukkit - add final
    {
    	// CraftBukkit - whole method
    	
    	// don't pool objects indefinitely if thread doesn't adhere to contract
    	if(this.resetCount == 0) return Vec3.createVectorHelper(par1, par3, par5);
    	
    	Vec3 vec3;
    	
        if(freelist == null)
        {
            vec3 = new Vec3(this, par1, par3, par5);
            total_size++;
        }
        else
        {
        	vec3 = freelist;
        	freelist = vec3.next;
        	vec3.setComponents(par1, par3, par5);
        }
        
        if(alloclist == null)
        	alloclisthead = vec3;
        vec3.next = alloclist;
        alloclist = vec3;
        
        ++nextFreeSpace;
        return vec3;
    }
    
    // CraftBukkit start - offer back vector (can save LOTS of unneeded bloat) - works about 90% of the time
    public void release(Vec3 v) {
        if (this.alloclist == v) {
            this.alloclist = v.next; // Pop off alloc list
            // Push on to free list
            if (this.freelist == null) this.freelisthead = v;
            v.next = this.freelist;
            this.freelist = v;
            this.nextFreeSpace--;
        }
    }
    // CraftBukkit end

    /**
     * Will truncate the array everyN clears to the maximum size observed since the last truncation.
     */
    public void clear()
    {
        if (!this.func_82589_e())
        {
            if (this.nextFreeSpace > this.maximumSizeSinceLastTruncation)
            {
                this.maximumSizeSinceLastTruncation = this.nextFreeSpace;
            }
            
            // CraftBukkit start - intelligent cache
	        // Take any allocated blocks and put them on free list
	        if (this.alloclist != null) {
	            if (this.freelist == null) {
	                this.freelist = this.alloclist;
	                this.freelisthead = this.alloclisthead;
	            }
	            else {
	                this.alloclisthead.next = this.freelist;
	                this.freelist = this.alloclist;
	                this.freelisthead = this.alloclisthead;
	            }
	            this.alloclist = null;
	        }
	        if ((this.resetCount++ & 0xff) == 0) {
	            int newSize = total_size - (total_size >> 3);
	            if (newSize > this.maximumSizeSinceLastTruncation) { // newSize will be 87.5%, but if we were not in that range, we clear some of the cache
	                for (int i = total_size; i > newSize; i--) {
	                    freelist = freelist.next;
	                }
	                total_size = newSize;
	            }
	            this.maximumSizeSinceLastTruncation = 0;
	            // this.f = 0; // We do not reset to zero; it doubles for a flag
	        }
	        // CraftBukkit end
        }
    }

    @SideOnly(Side.CLIENT)
    public void clearAndFreeCache()
    {
        if (!this.func_82589_e())
        {
            this.nextFreeSpace = 0;
            this.vec3Cache.clear();
        }
    }

    public int getPoolSize()
    {
        return total_size; // CraftBukkit
    }

    public int func_82590_d()
    {
        return this.nextFreeSpace;
    }

    private boolean func_82589_e()
    {
        return this.minimumSize < 0 || this.truncateArrayResetThreshold < 0;
    }
}
