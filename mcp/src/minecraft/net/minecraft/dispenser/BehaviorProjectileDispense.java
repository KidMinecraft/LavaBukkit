package net.minecraft.dispenser;

import org.bukkit.event.block.BlockDispenseEvent;

import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public abstract class BehaviorProjectileDispense extends BehaviorDefaultDispenseItem
{
    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        World var3 = par1IBlockSource.getWorld();
        IPosition var4 = BlockDispenser.func_82525_a(par1IBlockSource);
        EnumFacing var5 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        IProjectile var6 = this.getProjectileEntity(var3, var4);
        
        // CraftBukkit start
        BlockDispenseEvent event = immibis.lavabukkit.dispenser.EventHelper.raiseEvent(this, par1IBlockSource, par2ItemStack, var5.func_82601_c(), 0.1, var5.func_82599_e());
        if(event == null)
        	return par2ItemStack;

        var6.setThrowableHeading(event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), this.func_82500_b(), this.func_82498_a());
        // CraftBukkit end
        
        var3.spawnEntityInWorld((Entity)var6);
        par2ItemStack.splitStack(1);
        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1002, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }

    /**
     * Return the projectile entity spawned by this dispense behavior.
     */
    protected abstract IProjectile getProjectileEntity(World var1, IPosition var2);

    protected float func_82498_a()
    {
        return 6.0F;
    }

    protected float func_82500_b()
    {
        return 1.1F;
    }
}
