package immibis.lavabukkit.dispenser;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class DispenseBucketFull extends BehaviorDefaultDispenseItem
{
    /** Reference to the BehaviorDefaultDispenseItem object. */
    private final BehaviorDefaultDispenseItem defaultItemDispenseBehavior = new BehaviorDefaultDispenseItem();

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        ItemBucket var3 = (ItemBucket)par2ItemStack.getItem();
        int var4 = par1IBlockSource.getXInt();
        int var5 = par1IBlockSource.getYInt();
        int var6 = par1IBlockSource.getZInt();
        EnumFacing var7 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        
        // CraftBukkit start
        World world = par1IBlockSource.getWorld();
        int i2 = var4 + var7.func_82601_c();
        int k2 = var6 + var7.func_82599_e();
        if (world.isAirBlock(i2, var5, k2) || world.getBlockMaterial(i2, var5, k2).isReplaceable()) {
        	
        	BlockDispenseEvent evt = EventHelper.raiseEvent(this, par1IBlockSource, par2ItemStack, 0, 0, 0);
        	if(evt == null)
        		return par2ItemStack;

            var3 = (ItemBucket) CraftItemStack.asNMSCopy(evt.getItem()).getItem();
        }
        // CraftBukkit end

        if (var3.tryPlaceContainedLiquid(par1IBlockSource.getWorld(), (double)var4, (double)var5, (double)var6, var4 + var7.func_82601_c(), var5, var6 + var7.func_82599_e()))
        {
            par2ItemStack.itemID = Item.bucketEmpty.itemID;
            par2ItemStack.stackSize = 1;
            return par2ItemStack;
        }
        else
        {
            return this.defaultItemDispenseBehavior.dispense(par1IBlockSource, par2ItemStack);
        }
    }
}
