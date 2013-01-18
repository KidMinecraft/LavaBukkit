package immibis.lavabukkit.dispenser;

// CraftBukkit start
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

public class DispenseBucketEmpty extends BehaviorDefaultDispenseItem {

    private final BehaviorDefaultDispenseItem c;

    public DispenseBucketEmpty() {
        this.c = new BehaviorDefaultDispenseItem();
    }

    public ItemStack dispenseStack(IBlockSource isourceblock, ItemStack itemstack) {
        EnumFacing enumfacing = EnumFacing.func_82600_a(isourceblock.func_82620_h());
        World world = isourceblock.getWorld();
        int i = isourceblock.getXInt() + enumfacing.func_82601_c();
        int j = isourceblock.getYInt();
        int k = isourceblock.getZInt() + enumfacing.func_82599_e();
        Material material = world.getBlockMaterial(i, j, k);
        int l = world.getBlockMetadata(i, j, k);
        Item item;

        if (Material.water.equals(material) && l == 0) {
            item = Item.bucketWater;
        } else {
            if (!Material.lava.equals(material) || l != 0) {
                return super.dispenseStack(isourceblock, itemstack);
            }

            item = Item.bucketLava;
        }

        // CraftBukkit start
        BlockDispenseEvent event = EventHelper.raiseEventAtBlock(this, isourceblock, i, j, k, itemstack, 0, 0, 0);
        if(event == null)
        	return itemstack;
        itemstack = CraftItemStack.asNMSCopy(event.getItem());
        // CraftBukkit end

        world.setBlockWithNotify(i, j, k, 0);
        if (--itemstack.stackSize == 0) {
            itemstack.itemID = item.itemID;
            itemstack.stackSize = 1;
        } else if (((TileEntityDispenser) isourceblock.func_82619_j()).addItem(new ItemStack(item)) < 0) {
            this.c.dispense(isourceblock, new ItemStack(item));
        }

        return itemstack;
    }
}
