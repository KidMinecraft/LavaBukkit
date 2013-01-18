package immibis.lavabukkit.dispenser;

import java.util.Random;

import org.bukkit.event.block.BlockDispenseEvent;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class DispenseFireball extends BehaviorDefaultDispenseItem
{
    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing var3 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        IPosition var4 = BlockDispenser.func_82525_a(par1IBlockSource);
        double var5 = var4.getX() + (double)((float)var3.func_82601_c() * 0.3F);
        double var7 = var4.getY();
        double var9 = var4.getZ() + (double)((float)var3.func_82599_e() * 0.3F);
        World var11 = par1IBlockSource.getWorld();
        Random var12 = var11.rand;
        double var13 = var12.nextGaussian() * 0.05D + (double)var3.func_82601_c();
        double var15 = var12.nextGaussian() * 0.05D;
        double var17 = var12.nextGaussian() * 0.05D + (double)var3.func_82599_e();
        
        // CraftBukkit start
        BlockDispenseEvent event = EventHelper.raiseEvent(this, par1IBlockSource, par2ItemStack, var13, var15, var17);
        if(event == null)
        	return par2ItemStack;

        var11.spawnEntityInWorld(new EntitySmallFireball(var11, var5, var7, var9, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ()));
        // CraftBukkit end
        
        par2ItemStack.splitStack(1);
        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1009, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }
}
