package net.minecraft.world.gen.feature;

import immibis.lavabukkit.nms.MCPBlockChangeDelegate;
import immibis.lavabukkit.nms.NMSUtils;

import java.util.Random;

import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling.TreeGenerator;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class WorldGenBigMushroom extends WorldGenerator implements TreeGenerator // CraftBukkit add interface
{
    /** The mushroom type. 0 for brown, 1 for red. */
    private int mushroomType = -1;

    public WorldGenBigMushroom(int par1)
    {
        super(true);
        this.mushroomType = par1;
    }

    public WorldGenBigMushroom()
    {
        super(false);
    }

    // CraftBukkit start - change signature, delegate
    public boolean generate(MCPBlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5)
    {
    	return generate(par1World, par2Random, par3, par4, par5, null, null, null);
    }
    
    public boolean generate(MCPBlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5, org.bukkit.event.world.StructureGrowEvent event, ItemStack itemstack, org.bukkit.craftbukkit.CraftWorld bukkitWorld)
    {
    	// CraftBukkit end
        int var6 = par2Random.nextInt(2);

        if (this.mushroomType >= 0)
        {
            var6 = this.mushroomType;
        }

        int var7 = par2Random.nextInt(3) + 4;
        boolean var8 = true;

        if (par4 >= 1 && par4 + var7 + 1 < 256)
        {
            int var9;
            int var11;
            int var12;
            int var13;

            for (var9 = par4; var9 <= par4 + 1 + var7; ++var9)
            {
                byte var10 = 3;

                if (var9 <= par4 + 3)
                {
                    var10 = 0;
                }

                for (var11 = par3 - var10; var11 <= par3 + var10 && var8; ++var11)
                {
                    for (var12 = par5 - var10; var12 <= par5 + var10 && var8; ++var12)
                    {
                        if (var9 >= 0 && var9 < 256)
                        {
                            var13 = par1World.getBlockId(var11, var9, var12);

                            Block block = Block.blocksList[var13];
                            
                            // LavaBukkit fix MCPBCD call
                            if (var13 != 0 && block != null && !par1World.isLeaves(block, var11, var9, var12))
                            {
                                var8 = false;
                            }
                        }
                        else
                        {
                            var8 = false;
                        }
                    }
                }
            }

            if (!var8)
            {
                return false;
            }
            else
            {
                var9 = par1World.getBlockId(par3, par4 - 1, par5);

                if (var9 != Block.dirt.blockID && var9 != Block.grass.blockID && var9 != Block.mycelium.blockID)
                {
                    return false;
                }
                else
                {
                	// CraftBukkit start
                    if (event == null) {
                        this.setBlockAndMetadata(par1World, par3, par4 - 1, par5, Block.dirt.blockID, 0);
                    } else {
                        BlockState dirtState = bukkitWorld.getBlockAt(par3, par4 - 1, par5).getState();
                        dirtState.setTypeId(Block.dirt.blockID);
                        event.getBlocks().add(dirtState);
                    }
                    // CraftBukkit end
                    
                    int var16 = par4 + var7;

                    if (var6 == 1)
                    {
                        var16 = par4 + var7 - 3;
                    }

                    for (var11 = var16; var11 <= par4 + var7; ++var11)
                    {
                        var12 = 1;

                        if (var11 < par4 + var7)
                        {
                            ++var12;
                        }

                        if (var6 == 0)
                        {
                            var12 = 3;
                        }

                        for (var13 = par3 - var12; var13 <= par3 + var12; ++var13)
                        {
                            for (int var14 = par5 - var12; var14 <= par5 + var12; ++var14)
                            {
                                int var15 = 5;

                                if (var13 == par3 - var12)
                                {
                                    --var15;
                                }

                                if (var13 == par3 + var12)
                                {
                                    ++var15;
                                }

                                if (var14 == par5 - var12)
                                {
                                    var15 -= 3;
                                }

                                if (var14 == par5 + var12)
                                {
                                    var15 += 3;
                                }

                                if (var6 == 0 || var11 < par4 + var7)
                                {
                                    if ((var13 == par3 - var12 || var13 == par3 + var12) && (var14 == par5 - var12 || var14 == par5 + var12))
                                    {
                                        continue;
                                    }

                                    if (var13 == par3 - (var12 - 1) && var14 == par5 - var12)
                                    {
                                        var15 = 1;
                                    }

                                    if (var13 == par3 - var12 && var14 == par5 - (var12 - 1))
                                    {
                                        var15 = 1;
                                    }

                                    if (var13 == par3 + (var12 - 1) && var14 == par5 - var12)
                                    {
                                        var15 = 3;
                                    }

                                    if (var13 == par3 + var12 && var14 == par5 - (var12 - 1))
                                    {
                                        var15 = 3;
                                    }

                                    if (var13 == par3 - (var12 - 1) && var14 == par5 + var12)
                                    {
                                        var15 = 7;
                                    }

                                    if (var13 == par3 - var12 && var14 == par5 + (var12 - 1))
                                    {
                                        var15 = 7;
                                    }

                                    if (var13 == par3 + (var12 - 1) && var14 == par5 + var12)
                                    {
                                        var15 = 9;
                                    }

                                    if (var13 == par3 + var12 && var14 == par5 + (var12 - 1))
                                    {
                                        var15 = 9;
                                    }
                                }

                                if (var15 == 5 && var11 < par4 + var7)
                                {
                                    var15 = 0;
                                }

                                Block block = Block.blocksList[par1World.getBlockId(var13, var11, var14)];

                                // LavaBukkit fix MCPBCD call
                                if ((var15 != 0 || par4 >= par4 + var7 - 1) && (block == null || par1World.canBeReplacedByLeaves(block, var13, var11, var14)))
                                {
                                	// CraftBukkit start
                                    if (event == null) {
                                       this.setBlockAndMetadata(par1World, var13, var11, var14, Block.mushroomCapBrown.blockID + var6, var15);
                                    } else {
                                        BlockState state = bukkitWorld.getBlockAt(var13, var11, var14).getState();
                                        state.setTypeId(Block.mushroomCapBrown.blockID + var6);
                                        state.setData(new MaterialData(Block.mushroomCapBrown.blockID + var6, (byte) var15));
                                        event.getBlocks().add(state);
                                    }
                                    // CraftBukkit end
                                    
                                    
                                }
                            }
                        }
                    }

                    for (var11 = 0; var11 < var7; ++var11)
                    {
                        var12 = par1World.getBlockId(par3, par4 + var11, par5);

                        Block block = Block.blocksList[var12];

                        // LavaBukkit fix MCPBCD call
                        if (block == null || par1World.canBeReplacedByLeaves(block, par3, par4 + var11, par5))
                        {
                        	// CraftBukkit start
                            if (event == null) {
                                this.setBlockAndMetadata(par1World, par3, par4 + var11, par5, Block.mushroomCapBrown.blockID + var6, 10);
                            } else {
                                BlockState state = bukkitWorld.getBlockAt(par3, par4 + var11, par5).getState();
                                state.setTypeId(Block.mushroomCapBrown.blockID + var6);
                                state.setData(new MaterialData(Block.mushroomCapBrown.blockID + var6, (byte) 10));
                                event.getBlocks().add(state);
                            }
                            // CraftBukkit end
                            
                            
                        }
                    }

                    return true;
                }
            }
        }
        else
        {
            return false;
        }
    }
    
    // CraftBukkit
	@Override public boolean generate(World var1, Random var2, int var3, int var4, int var5) {return generate(NMSUtils.createBCD(var1),var2,var3,var4,var5);}
}
