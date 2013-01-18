package immibis.lavabukkit.dispenser;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.util.Vector;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;

public class EventHelper {
	/**
	 * Calls a BlockDispenseEvent and either:
	 * <ul>
	 * <li>Calls another handler and returns null, or
	 * <li>Returns the event, which may contain an overridden stack or velocity.
	 * </ul>
	 */
	public static BlockDispenseEvent raiseEvent(IBehaviorDispenseItem behaviour, IBlockSource ibs, ItemStack stack, double velX, double velY, double velZ) {
		org.bukkit.block.Block block = ibs.getWorld().getWorld().getBlockAt(ibs.getXInt(), ibs.getYInt(), ibs.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(stack);

        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(velX, velY, velZ));
        if (!BlockDispenser.eventFired) {
            ibs.getWorld().getServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            return null;
        }

        if (!event.getItem().equals(craftItem)) {
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            IBehaviorDispenseItem idispensebehavior = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.func_82594_a(eventStack.getItem());
            if (idispensebehavior != IBehaviorDispenseItem.itemDispenseBehaviorProvider && idispensebehavior != behaviour) {
                idispensebehavior.dispense(ibs, eventStack);
                return null;
            }
        }
        
        return event;
	}
	
	public static BlockDispenseEvent raiseEventAtBlock(IBehaviorDispenseItem behaviour, IBlockSource ibs, int blockX, int blockY, int blockZ, ItemStack stack, double velX, double velY, double velZ) {
		org.bukkit.block.Block block = ibs.getWorld().getWorld().getBlockAt(blockX, blockY, blockZ);
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(stack);

        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(velX, velY, velZ));
        if (!BlockDispenser.eventFired) {
            ibs.getWorld().getServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            return null;
        }

        if (!event.getItem().equals(craftItem)) {
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            IBehaviorDispenseItem idispensebehavior = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.func_82594_a(eventStack.getItem());
            if (idispensebehavior != IBehaviorDispenseItem.itemDispenseBehaviorProvider && idispensebehavior != behaviour) {
                idispensebehavior.dispense(ibs, eventStack);
                return null;
            }
        }
        
        return event;
	}
}
