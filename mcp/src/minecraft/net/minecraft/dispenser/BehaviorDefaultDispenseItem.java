package net.minecraft.dispenser;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;

import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BehaviorDefaultDispenseItem implements IBehaviorDispenseItem
{
    /**
     * Dispenses the specified ItemStack from a dispenser.
     */
    public final ItemStack dispense(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        ItemStack var3 = this.dispenseStack(par1IBlockSource, par2ItemStack);
        this.playDispenseSound(par1IBlockSource);
        this.spawnDispenseParticles(par1IBlockSource, EnumFacing.func_82600_a(par1IBlockSource.func_82620_h()));
        return var3;
    }

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    protected ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing var3 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        IPosition var4 = BlockDispenser.func_82525_a(par1IBlockSource);
        ItemStack var5 = par2ItemStack.splitStack(1);
        
        // CraftBukkit start
        if(!func_82486_a(par1IBlockSource.getWorld(), var5, 6, var3, par1IBlockSource))
        	par2ItemStack.stackSize++;
        // CraftBukkit end
        
        return par2ItemStack;
    }

    // CraftBukkit start - void -> boolean return, IPosition -> ISourceBlock last argument
    public static boolean func_82486_a(World par0World, ItemStack par1ItemStack, int par2, EnumFacing par3EnumFacing, IBlockSource par4IBlockSource)
    {
    	IPosition par4IPosition = BlockDispenser.func_82525_a(par4IBlockSource);
    	// CraftBukkit end
        double var5 = par4IPosition.getX();
        double var7 = par4IPosition.getY();
        double var9 = par4IPosition.getZ();
        EntityItem var11 = new EntityItem(par0World, var5, var7 - 0.3D, var9, par1ItemStack);
        double var12 = par0World.rand.nextDouble() * 0.1D + 0.2D;
        var11.motionX = (double)par3EnumFacing.func_82601_c() * var12;
        var11.motionY = 0.20000000298023224D;
        var11.motionZ = (double)par3EnumFacing.func_82599_e() * var12;
        var11.motionX += par0World.rand.nextGaussian() * 0.007499999832361937D * (double)par2;
        var11.motionY += par0World.rand.nextGaussian() * 0.007499999832361937D * (double)par2;
        var11.motionZ += par0World.rand.nextGaussian() * 0.007499999832361937D * (double)par2;
        
        // CraftBukkit start
        org.bukkit.block.Block block = par0World.getWorld().getBlockAt(par4IBlockSource.getXInt(), par4IBlockSource.getYInt(), par4IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(par1ItemStack);

        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(var11.motionX, var11.motionY, var11.motionZ));
        if (!BlockDispenser.eventFired) {
            par0World.getServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            return false;
        }
        
        var11.func_92013_a(CraftItemStack.asNMSCopy(event.getItem()));
        var11.motionX = event.getVelocity().getX();
        var11.motionY = event.getVelocity().getY();
        var11.motionZ = event.getVelocity().getZ();

        if (!event.getItem().equals(craftItem)) {
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            IBehaviorDispenseItem idispensebehavior = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.func_82594_a(eventStack.getItem());
            if (idispensebehavior != IBehaviorDispenseItem.itemDispenseBehaviorProvider && idispensebehavior.getClass() != BehaviorDefaultDispenseItem.class) {
                idispensebehavior.dispense(par4IBlockSource, eventStack);
            } else {
                par0World.spawnEntityInWorld(var11);
            }
            return false;
        }
        
        par0World.spawnEntityInWorld(var11);
        
        return true;
        // CraftBukkit end
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1000, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }

    /**
     * Order clients to display dispense particles from the specified block and facing.
     */
    protected void spawnDispenseParticles(IBlockSource par1IBlockSource, EnumFacing par2EnumFacing)
    {
        par1IBlockSource.getWorld().playAuxSFX(2000, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), this.func_82488_a(par2EnumFacing));
    }

    private int func_82488_a(EnumFacing par1EnumFacing)
    {
        return par1EnumFacing.func_82601_c() + 1 + (par1EnumFacing.func_82599_e() + 1) * 3;
    }
}