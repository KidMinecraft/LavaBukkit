package net.minecraft.world.gen.feature;

import immibis.lavabukkit.nms.MCPBlockChangeDelegate;
import immibis.lavabukkit.nms.NMSUtils;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling.TreeGenerator;
import net.minecraft.world.World;

public class WorldGenTaiga1 extends WorldGenerator implements TreeGenerator // CraftBukkit add interface
{
	// CraftBukkit change signature
    public boolean generate(MCPBlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5)
    {
        int var6 = par2Random.nextInt(5) + 7;
        int var7 = var6 - par2Random.nextInt(2) - 3;
        int var8 = var6 - var7;
        int var9 = 1 + par2Random.nextInt(var8 + 1);
        boolean var10 = true;

        if (par4 >= 1 && par4 + var6 + 1 <= 128)
        {
            int var11;
            int var13;
            int var14;
            int var15;
            int var18;

            for (var11 = par4; var11 <= par4 + 1 + var6 && var10; ++var11)
            {
                boolean var12 = true;

                if (var11 - par4 < var7)
                {
                    var18 = 0;
                }
                else
                {
                    var18 = var9;
                }

                for (var13 = par3 - var18; var13 <= par3 + var18 && var10; ++var13)
                {
                    for (var14 = par5 - var18; var14 <= par5 + var18 && var10; ++var14)
                    {
                        if (var11 >= 0 && var11 < 128)
                        {
                            var15 = par1World.getBlockId(var13, var11, var14);

                            Block block = Block.blocksList[var15];

                            // LavaBukkit fix MCPBCD call
                            if (var15 != 0 && (block == null || !par1World.isLeaves(block, var13, var11, var14)))
                            {
                                var10 = false;
                            }
                        }
                        else
                        {
                            var10 = false;
                        }
                    }
                }
            }

            if (!var10)
            {
                return false;
            }
            else
            {
                var11 = par1World.getBlockId(par3, par4 - 1, par5);

                if ((var11 == Block.grass.blockID || var11 == Block.dirt.blockID) && par4 < 128 - var6 - 1)
                {
                    this.setBlock(par1World, par3, par4 - 1, par5, Block.dirt.blockID);
                    var18 = 0;

                    for (var13 = par4 + var6; var13 >= par4 + var7; --var13)
                    {
                        for (var14 = par3 - var18; var14 <= par3 + var18; ++var14)
                        {
                            var15 = var14 - par3;

                            for (int var16 = par5 - var18; var16 <= par5 + var18; ++var16)
                            {
                                int var17 = var16 - par5;

                                Block block = Block.blocksList[par1World.getBlockId(var14, var13, var16)];

                                if ((Math.abs(var15) != var18 || Math.abs(var17) != var18 || var18 <= 0) &&
                                	// LavaBukkit fix MCPBCD call
                                    (block == null || par1World.canBeReplacedByLeaves(block, var14, var13, var16)))
                                {
                                    this.setBlockAndMetadata(par1World, var14, var13, var16, Block.leaves.blockID, 1);
                                }
                            }
                        }

                        if (var18 >= 1 && var13 == par4 + var7 + 1)
                        {
                            --var18;
                        }
                        else if (var18 < var9)
                        {
                            ++var18;
                        }
                    }

                    for (var13 = 0; var13 < var6 - 1; ++var13)
                    {
                        var14 = par1World.getBlockId(par3, par4 + var13, par5);

                        Block block = Block.blocksList[var14];

                        // LavaBukkit fix MCPBCD call
                        if (var14 == 0 || block == null || par1World.isLeaves(block, par3, par4 + var13, par5))
                        {
                            this.setBlockAndMetadata(par1World, par3, par4 + var13, par5, Block.wood.blockID, 1);
                        }
                    }

                    return true;
                }
                else
                {
                    return false;
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
