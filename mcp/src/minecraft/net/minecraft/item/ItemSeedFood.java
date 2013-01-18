package net.minecraft.item;

import org.bukkit.craftbukkit.block.CraftBlockState;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

public class ItemSeedFood extends ItemFood implements IPlantable
{
    /** Block ID of the crop this seed food should place. */
    private int cropId;

    /** Block ID of the soil this seed food should be planted on. */
    private int soilId;

    public ItemSeedFood(int par1, int par2, float par3, int par4, int par5)
    {
        super(par1, par2, par3, false);
        this.cropId = par4;
        this.soilId = par5;
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        if (par7 != 1)
        {
            return false;
        }
        else if (par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack) && par2EntityPlayer.canPlayerEdit(par4, par5 + 1, par6, par7, par1ItemStack))
        {
            int var11 = par3World.getBlockId(par4, par5, par6);

            if (var11 == this.soilId && par3World.isAirBlock(par4, par5 + 1, par6))
            {
            	CraftBlockState blockState = par3World.isRemote ? null : CraftBlockState.getBlockState(par3World, par4, par5 + 1, par6); // CraftBukkit
            	
                par3World.setBlockWithNotify(par4, par5 + 1, par6, this.cropId);
                
                // CraftBukkit start - seeds
                if(!par3World.isRemote) {
	                org.bukkit.event.block.BlockPlaceEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(par3World, par2EntityPlayer, blockState, par4, par5, par6);
	
	                if (event.isCancelled() || !event.canBuild()) {
	                    event.getBlockPlaced().setTypeId(0);
	                    return false;
	                }
                }
                // CraftBukkit end

                --par1ItemStack.stackSize;
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public EnumPlantType getPlantType(World world, int x, int y, int z)
    {
        return EnumPlantType.Crop;
    }

    @Override
    public int getPlantID(World world, int x, int y, int z)
    {
        return cropId;
    }

    @Override
    public int getPlantMetadata(World world, int x, int y, int z)
    {
        return 0;
    }
}
