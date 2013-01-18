package net.minecraft.world.gen.feature;

import immibis.lavabukkit.nms.MCPBlockChangeDelegate;
import immibis.lavabukkit.nms.NMSUtils;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling.TreeGenerator;
import net.minecraft.world.World;

public class WorldGenShrub extends WorldGenerator implements TreeGenerator // CraftBukkit add interface
{
    private int field_76527_a;
    private int field_76526_b;

    public WorldGenShrub(int par1, int par2)
    {
        this.field_76526_b = par1;
        this.field_76527_a = par2;
    }

    // CraftBukkit change signature
    public boolean generate(MCPBlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5)
    {
        int var15;

        Block block = null;
        do 
        {
            block = Block.blocksList[par1World.getBlockId(par3,  par4, par5)];
            // LavaBukkit fix MCPBCD call
            if (block != null && !par1World.isLeaves(block, par3, par4, par5))
            {
                break;
            }
            par4--;
        } while (par4 > 0);

        int var7 = par1World.getBlockId(par3, par4, par5);

        if (var7 == Block.dirt.blockID || var7 == Block.grass.blockID)
        {
            ++par4;
            this.setBlockAndMetadata(par1World, par3, par4, par5, Block.wood.blockID, this.field_76526_b);

            for (int var8 = par4; var8 <= par4 + 2; ++var8)
            {
                int var9 = var8 - par4;
                int var10 = 2 - var9;

                for (int var11 = par3 - var10; var11 <= par3 + var10; ++var11)
                {
                    int var12 = var11 - par3;

                    for (int var13 = par5 - var10; var13 <= par5 + var10; ++var13)
                    {
                        int var14 = var13 - par5;

                        block = Block.blocksList[par1World.getBlockId(var11, var8, var13)];

                        if ((Math.abs(var12) != var10 || Math.abs(var14) != var10 || par2Random.nextInt(2) != 0) &&
                        	// LavaBukkit fix MCPBCD call
                            (block == null || par1World.canBeReplacedByLeaves(block, var11, var8, var13)))
                        {
                            this.setBlockAndMetadata(par1World, var11, var8, var13, Block.leaves.blockID, this.field_76527_a);
                        }
                    }
                }
            }
        }

        return true;
    }
    
    // CraftBukkit
	@Override public boolean generate(World var1, Random var2, int var3, int var4, int var5) {return generate(NMSUtils.createBCD(var1),var2,var3,var4,var5);}
}
