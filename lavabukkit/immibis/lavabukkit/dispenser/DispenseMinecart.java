package immibis.lavabukkit.dispenser;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;

import net.minecraft.block.BlockRail;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class DispenseMinecart extends BehaviorDefaultDispenseItem
{
    /** Reference to the BehaviorDefaultDispenseItem object. */
    private final BehaviorDefaultDispenseItem defaultItemDispenseBehavior = new BehaviorDefaultDispenseItem();

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing var3 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        World var4 = par1IBlockSource.getWorld();
        double var5 = par1IBlockSource.getX() + (double)((float)var3.func_82601_c() * 1.125F);
        double var7 = par1IBlockSource.getY();
        double var9 = par1IBlockSource.getZ() + (double)((float)var3.func_82599_e() * 1.125F);
        int var11 = par1IBlockSource.getXInt() + var3.func_82601_c();
        int var12 = par1IBlockSource.getYInt();
        int var13 = par1IBlockSource.getZInt() + var3.func_82599_e();
        int var14 = var4.getBlockId(var11, var12, var13);
        double var15;

        if (BlockRail.isRailBlock(var14))
        {
            var15 = 0.0D;
        }
        else
        {
            if (var14 != 0 || !BlockRail.isRailBlock(var4.getBlockId(var11, var12 - 1, var13)))
            {
                return this.defaultItemDispenseBehavior.dispense(par1IBlockSource, par2ItemStack);
            }

            var15 = -1.0D;
        }
        
        // CraftBukkit start
        BlockDispenseEvent event = EventHelper.raiseEvent(this, par1IBlockSource, par2ItemStack, var5, var7 + var15, var9);
        if(event == null)
        	return par2ItemStack;

        par2ItemStack = CraftItemStack.asNMSCopy(event.getItem());
        EntityMinecart var17 = new EntityMinecart(var4, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), ((ItemMinecart) par2ItemStack.getItem()).minecartType);
        // CraftBukkit end

        var4.spawnEntityInWorld(var17);
        par2ItemStack.splitStack(1);
        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1000, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }
}
