package net.minecraft.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;

import org.bukkit.craftbukkit.event.CraftEventFactory;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockRedstoneRepeater extends BlockDirectional
{
    /** The offsets for the two torches in redstone repeater blocks. */
    public static final double[] repeaterTorchOffset = new double[] { -0.0625D, 0.0625D, 0.1875D, 0.3125D};

    /** The states in which the redstone repeater blocks can be. */
    private static final int[] repeaterState = new int[] {1, 2, 3, 4};

    /** Tells whether the repeater is powered or not */
    private final boolean isRepeaterPowered;

    protected BlockRedstoneRepeater(int par1, boolean par2)
    {
        super(par1, 6, Material.circuits);
        this.isRepeaterPowered = par2;
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    /**
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    /**
     * Checks to see if its valid to put this block at the specified coordinates. Args: world, x, y, z
     */
    public boolean canPlaceBlockAt(World par1World, int par2, int par3, int par4)
    {
        return !par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) ? false : super.canPlaceBlockAt(par1World, par2, par3, par4);
    }

    /**
     * Can this block stay at this position.  Similar to canPlaceBlockAt except gets checked often with plants.
     */
    public boolean canBlockStay(World par1World, int par2, int par3, int par4)
    {
        return !par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) ? false : super.canBlockStay(par1World, par2, par3, par4);
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        int var6 = par1World.getBlockMetadata(par2, par3, par4);
        boolean var7 = this.func_82523_e(par1World, par2, par3, par4, var6);

        if (!var7)
        {
            boolean var8 = this.ignoreTick(par1World, par2, par3, par4, var6);

            if (this.isRepeaterPowered && !var8)
            {
            	// CraftBukkit start
                if (CraftEventFactory.callRedstoneChange(par1World, par2, par3, par4, 15, 0).getNewCurrent() != 0) {
                    return;
                }
                // CraftBukkit end
                
                par1World.setBlockAndMetadataWithNotify(par2, par3, par4, Block.redstoneRepeaterIdle.blockID, var6);
            }
            else if (!this.isRepeaterPowered)
            {
            	// CraftBukkit start
                if (CraftEventFactory.callRedstoneChange(par1World, par2, par3, par4, 0, 15).getNewCurrent() != 15) {
                    return;
                }
                // CraftBukkit end
                
                par1World.setBlockAndMetadataWithNotify(par2, par3, par4, Block.redstoneRepeaterActive.blockID, var6);

                if (!var8)
                {
                    int var9 = (var6 & 12) >> 2;
                    par1World.scheduleBlockUpdate(par2, par3, par4, Block.redstoneRepeaterActive.blockID, repeaterState[var9] * 2);
                }
            }
        }
    }

    /**
     * From the specified side and block metadata retrieves the blocks texture. Args: side, metadata
     */
    public int getBlockTextureFromSideAndMetadata(int par1, int par2)
    {
        return par1 == 0 ? (this.isRepeaterPowered ? 99 : 115) : (par1 == 1 ? (this.isRepeaterPowered ? 147 : 131) : 5);
    }

    @SideOnly(Side.CLIENT)

    /**
     * Returns true if the given side of this block type should be rendered, if the adjacent block is at the given
     * coordinates.  Args: blockAccess, x, y, z, side
     */
    public boolean shouldSideBeRendered(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        return par5 != 0 && par5 != 1;
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 15;
    }

    /**
     * Returns the block texture based on the side being looked at.  Args: side
     */
    public int getBlockTextureFromSide(int par1)
    {
        return this.getBlockTextureFromSideAndMetadata(par1, 0);
    }

    /**
     * Returns true if the block is emitting direct/strong redstone power on the specified side. Args: World, X, Y, Z,
     * side. Note that the side is reversed - eg it is 1 (up) when checking the bottom of the block.
     */
    public boolean isProvidingStrongPower(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        return this.isProvidingWeakPower(par1IBlockAccess, par2, par3, par4, par5);
    }

    /**
     * Returns true if the block is emitting indirect/weak redstone power on the specified side. If isBlockNormalCube
     * returns true, standard redstone propagation rules will apply instead and this will not be called. Args: World, X,
     * Y, Z, side. Note that the side is reversed - eg it is 1 (up) when checking the bottom of the block.
     */
    public boolean isProvidingWeakPower(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        if (!this.isRepeaterPowered)
        {
            return false;
        }
        else
        {
            int var6 = getDirection(par1IBlockAccess.getBlockMetadata(par2, par3, par4));
            return var6 == 0 && par5 == 3 ? true : (var6 == 1 && par5 == 4 ? true : (var6 == 2 && par5 == 2 ? true : var6 == 3 && par5 == 5));
        }
    }

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World par1World, int par2, int par3, int par4, int par5)
    {
        if (!this.canBlockStay(par1World, par2, par3, par4))
        {
            this.dropBlockAsItem(par1World, par2, par3, par4, par1World.getBlockMetadata(par2, par3, par4), 0);
            par1World.setBlockWithNotify(par2, par3, par4, 0);
            par1World.notifyBlocksOfNeighborChange(par2 + 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2 - 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 + 1, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 - 1, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 - 1, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 + 1, par4, this.blockID);
        }
        else
        {
            int var6 = par1World.getBlockMetadata(par2, par3, par4);
            boolean var7 = this.func_82523_e(par1World, par2, par3, par4, var6);

            if (!var7)
            {
                boolean var8 = this.ignoreTick(par1World, par2, par3, par4, var6);
                int var9 = (var6 & 12) >> 2;

                if (this.isRepeaterPowered && !var8 || !this.isRepeaterPowered && var8)
                {
                    byte var10 = 0;

                    if (this.func_83011_d(par1World, par2, par3, par4, var6))
                    {
                        var10 = -1;
                    }

                    par1World.func_82740_a(par2, par3, par4, this.blockID, repeaterState[var9] * 2, var10);
                }
            }
        }
    }

    private boolean ignoreTick(World par1World, int par2, int par3, int par4, int par5)
    {
        int var6 = getDirection(par5);

        switch (var6)
        {
            case 0:
                return par1World.isBlockIndirectlyProvidingPowerTo(par2, par3, par4 + 1, 3) || par1World.getBlockId(par2, par3, par4 + 1) == Block.redstoneWire.blockID && par1World.getBlockMetadata(par2, par3, par4 + 1) > 0;
            case 1:
                return par1World.isBlockIndirectlyProvidingPowerTo(par2 - 1, par3, par4, 4) || par1World.getBlockId(par2 - 1, par3, par4) == Block.redstoneWire.blockID && par1World.getBlockMetadata(par2 - 1, par3, par4) > 0;
            case 2:
                return par1World.isBlockIndirectlyProvidingPowerTo(par2, par3, par4 - 1, 2) || par1World.getBlockId(par2, par3, par4 - 1) == Block.redstoneWire.blockID && par1World.getBlockMetadata(par2, par3, par4 - 1) > 0;
            case 3:
                return par1World.isBlockIndirectlyProvidingPowerTo(par2 + 1, par3, par4, 5) || par1World.getBlockId(par2 + 1, par3, par4) == Block.redstoneWire.blockID && par1World.getBlockMetadata(par2 + 1, par3, par4) > 0;
            default:
                return false;
        }
    }

    public boolean func_82523_e(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        int var6 = getDirection(par5);

        switch (var6)
        {
            case 0:
            case 2:
                return par1IBlockAccess.isBlockProvidingPowerTo(par2 - 1, par3, par4, 4) && func_82524_c(par1IBlockAccess.getBlockId(par2 - 1, par3, par4)) || par1IBlockAccess.isBlockProvidingPowerTo(par2 + 1, par3, par4, 5) && func_82524_c(par1IBlockAccess.getBlockId(par2 + 1, par3, par4));
            case 1:
            case 3:
                return par1IBlockAccess.isBlockProvidingPowerTo(par2, par3, par4 + 1, 3) && func_82524_c(par1IBlockAccess.getBlockId(par2, par3, par4 + 1)) || par1IBlockAccess.isBlockProvidingPowerTo(par2, par3, par4 - 1, 2) && func_82524_c(par1IBlockAccess.getBlockId(par2, par3, par4 - 1));
            default:
                return false;
        }
    }

    /**
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9)
    {
        int var10 = par1World.getBlockMetadata(par2, par3, par4);
        int var11 = (var10 & 12) >> 2;
        var11 = var11 + 1 << 2 & 12;
        par1World.setBlockMetadataWithNotify(par2, par3, par4, var11 | var10 & 3);
        return true;
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return true;
    }

    /**
     * Called when the block is placed in the world.
     */
    public void onBlockPlacedBy(World par1World, int par2, int par3, int par4, EntityLiving par5EntityLiving)
    {
        int var6 = ((MathHelper.floor_double((double)(par5EntityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) + 2) % 4;
        par1World.setBlockMetadataWithNotify(par2, par3, par4, var6);
        boolean var7 = this.ignoreTick(par1World, par2, par3, par4, var6);

        if (var7)
        {
            par1World.scheduleBlockUpdate(par2, par3, par4, this.blockID, 1);
        }
    }

    /**
     * Called whenever the block is added into the world. Args: world, x, y, z
     */
    public void onBlockAdded(World par1World, int par2, int par3, int par4)
    {
        par1World.notifyBlocksOfNeighborChange(par2 + 1, par3, par4, this.blockID);
        par1World.notifyBlocksOfNeighborChange(par2 - 1, par3, par4, this.blockID);
        par1World.notifyBlocksOfNeighborChange(par2, par3, par4 + 1, this.blockID);
        par1World.notifyBlocksOfNeighborChange(par2, par3, par4 - 1, this.blockID);
        par1World.notifyBlocksOfNeighborChange(par2, par3 - 1, par4, this.blockID);
        par1World.notifyBlocksOfNeighborChange(par2, par3 + 1, par4, this.blockID);
    }

    /**
     * Called right before the block is destroyed by a player.  Args: world, x, y, z, metaData
     */
    public void onBlockDestroyedByPlayer(World par1World, int par2, int par3, int par4, int par5)
    {
        if (this.isRepeaterPowered)
        {
            par1World.notifyBlocksOfNeighborChange(par2 + 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2 - 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 + 1, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 - 1, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 - 1, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 + 1, par4, this.blockID);
        }

        super.onBlockDestroyedByPlayer(par1World, par2, par3, par4, par5);
    }

    /**
     * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
     * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
     */
    public boolean isOpaqueCube()
    {
        return false;
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    public int idDropped(int par1, Random par2Random, int par3)
    {
        return Item.redstoneRepeater.itemID;
    }

    @SideOnly(Side.CLIENT)

    /**
     * A randomly called display update to be able to add particles or other items for display
     */
    public void randomDisplayTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        if (this.isRepeaterPowered)
        {
            int var6 = par1World.getBlockMetadata(par2, par3, par4);
            int var7 = getDirection(var6);
            double var8 = (double)((float)par2 + 0.5F) + (double)(par5Random.nextFloat() - 0.5F) * 0.2D;
            double var10 = (double)((float)par3 + 0.4F) + (double)(par5Random.nextFloat() - 0.5F) * 0.2D;
            double var12 = (double)((float)par4 + 0.5F) + (double)(par5Random.nextFloat() - 0.5F) * 0.2D;
            double var14 = 0.0D;
            double var16 = 0.0D;

            if (par5Random.nextInt(2) == 0)
            {
                switch (var7)
                {
                    case 0:
                        var16 = -0.3125D;
                        break;
                    case 1:
                        var14 = 0.3125D;
                        break;
                    case 2:
                        var16 = 0.3125D;
                        break;
                    case 3:
                        var14 = -0.3125D;
                }
            }
            else
            {
                int var18 = (var6 & 12) >> 2;

                switch (var7)
                {
                    case 0:
                        var16 = repeaterTorchOffset[var18];
                        break;
                    case 1:
                        var14 = -repeaterTorchOffset[var18];
                        break;
                    case 2:
                        var16 = -repeaterTorchOffset[var18];
                        break;
                    case 3:
                        var14 = repeaterTorchOffset[var18];
                }
            }

            par1World.spawnParticle("reddust", var8 + var14, var10, var12 + var16, 0.0D, 0.0D, 0.0D);
        }
    }

    @SideOnly(Side.CLIENT)

    /**
     * only called by clickMiddleMouseButton , and passed to inventory.setCurrentItem (along with isCreative)
     */
    public int idPicked(World par1World, int par2, int par3, int par4)
    {
        return Item.redstoneRepeater.itemID;
    }

    public static boolean func_82524_c(int par0)
    {
        return par0 == Block.redstoneRepeaterActive.blockID || par0 == Block.redstoneRepeaterIdle.blockID;
    }

    public boolean func_83011_d(World par1World, int par2, int par3, int par4, int par5)
    {
        int var6 = getDirection(par5);

        if (func_82524_c(par1World.getBlockId(par2 - Direction.offsetX[var6], par3, par4 - Direction.offsetZ[var6])))
        {
            int var7 = par1World.getBlockMetadata(par2 - Direction.offsetX[var6], par3, par4 - Direction.offsetZ[var6]);
            int var8 = getDirection(var7);
            return var8 != var6;
        }
        else
        {
            return false;
        }
    }
}
