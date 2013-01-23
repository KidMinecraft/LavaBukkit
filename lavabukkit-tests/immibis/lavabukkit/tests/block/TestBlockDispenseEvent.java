package immibis.lavabukkit.tests.block;

import java.util.concurrent.Callable;

import org.bukkit.Material;
import org.bukkit.event.block.BlockDispenseEvent;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraftforge.common.DimensionManager;
import immibis.lavabukkit.tests.EventCallback;
import immibis.lavabukkit.tests.Test;

/**
 * Tests:
 * <ul>
 * <li>That a BlockDispenseEvent is triggered when a dispenser fires.
 * <li>That an item is consumed from the dispenser when it fires.
 * <li>That an item is not consumed from the dispenser if the event's item is changed.
 * <li>That an item is not consumed from the dispenser if the event is cancelled.
 * </ul>
 * Does not test:
 * <ul>
 * <li>That the original item is correctly dispensed.
 * <li>That the changed item is correctly dispensed.
 * <li>That no item is dispensed when the event is cancelled.
 * </ul>
 */
public class TestBlockDispenseEvent extends Test {
	private int getCount() {
		return onserver(new Callable<Integer>() {public Integer call() {
			TileEntityDispenser te = (TileEntityDispenser)DimensionManager.getWorld(0).getBlockTileEntity(XOFF, YOFF, ZOFF);
			return te.getStackInSlot(0).stackSize;
		}});
	}
	
	public void test() throws Exception {
		// meta 3 = facing +Z
		setBlock(0, 0, 0, Block.dispenser, 3);
		
		// redstone torch supporting block
		setBlock(0, -2, 0, Block.dirt, 0);
		
		teleportThePlayer(0.5, 2.62, 0.5);
		lookAt(0.5, 0, 2);
		
		onserver(new Callable<Void>() {public Void call() {
			TileEntityDispenser te = (TileEntityDispenser)DimensionManager.getWorld(0).getBlockTileEntity(XOFF, YOFF, ZOFF);
			te.setInventorySlotContents(0, new ItemStack(Block.dirt, 64));
			return null;
		}});
		
		System.out.println("Testing BlockDispenseEvent - default");
		
		// trigger dispenser
		setBlock(0, -1, 0, Block.torchRedstoneActive, 0);
		Thread.sleep(500);
		setBlock(0, -1, 0, 0, 0);
		
		BlockDispenseEvent evt = assertReceivedEvent(BlockDispenseEvent.class);
		assertCoordsEqual(evt.getBlock(), 0, 0, 0);
		if(evt.getItem().getTypeId() != Block.dirt.blockID)
			throw new AssertionError("BlockDispenseEvent had wrong type ID");
		if(getCount() != 63)
			throw new AssertionError("wrong stack size left in dispenser");
		
		System.out.println("Testing BlockDispenseEvent - changing item");
		
		runOnNextEvent(BlockDispenseEvent.class, new EventCallback<BlockDispenseEvent>() {
			public void handle(BlockDispenseEvent event) throws Exception {
				event.setItem(new org.bukkit.inventory.ItemStack(Material.ARROW));
			}
		});
		
		// trigger dispenser
		setBlock(0, -1, 0, Block.torchRedstoneActive, 0);
		Thread.sleep(500);
		setBlock(0, -1, 0, 0, 0);
		
		evt = assertReceivedEvent(BlockDispenseEvent.class);
		assertCoordsEqual(evt.getBlock(), 0, 0, 0);
		if(evt.getItem().getTypeId() != Item.arrow.itemID)
			throw new AssertionError("BlockDispenseEvent had wrong type ID");
		if(getCount() != 63)
			throw new AssertionError("wrong stack size left in dispenser");
		
		System.out.println("Testing BlockDispenseEvent - cancelling");
		
		cancelNextEvent(BlockDispenseEvent.class);
		
		// trigger dispenser
		setBlock(0, -1, 0, Block.torchRedstoneActive, 0);
		Thread.sleep(500);
		setBlock(0, -1, 0, 0, 0);
		
		assertReceivedEvent(BlockDispenseEvent.class);
		
		if(getCount() != 63)
			throw new AssertionError("wrong stack size left in dispenser");
		
	}
}
