package immibis.lavabukkit.tests.block;

import net.minecraft.block.Block;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;

import immibis.lavabukkit.tests.EventCallback;
import immibis.lavabukkit.tests.EventMatcher;
import immibis.lavabukkit.tests.Test;

/**
 * Tests that:
 * <ul>
 * <li>BlockGrowEvents are fired when netherwart, wheat, carrots, potatoes, sugarcane, and cactus grow.
 * <li>Cancelling the event prevents the block from growing.
 * </ul> 
 *
 */
public class TestBlockGrowEvent extends Test {
	private void cancelEvents() {
		setEventHandler(new EventCallback<Event>() {
			public void handle(Event event_) {
				if(event_ instanceof BlockGrowEvent) {
					BlockGrowEvent event = (BlockGrowEvent)event_;
					if(event.getBlock().getX() == XOFF
						&& (event.getBlock().getY() == YOFF - 1 || event.getBlock().getY() == YOFF - 2)
						&& event.getBlock().getZ() == ZOFF)
						event.setCancelled(true);
				}
			}
		});
	}
	private BlockGrowEvent waitForEvent() throws Exception {
		return waitForEvent(BlockGrowEvent.class, new EventMatcher<BlockGrowEvent>() {
			public boolean matches(BlockGrowEvent event) throws Exception {
				return event.getBlock().getX() == XOFF
					&& (event.getBlock().getY() == YOFF - 1 || event.getBlock().getY() == YOFF - 2)
					&& event.getBlock().getZ() == ZOFF;
			}
		});
	}
	
	public void test() throws Exception {
		for(int cancel_ = 0; cancel_ <= 1; cancel_++) {
			boolean cancel = cancel_ == 1;
			
			resetState();
			setBlock(0, 1, 0, Block.bedrock, 0);
			
			setBlock(0, -1, 3, Block.bedrock, 0);
			teleportThePlayer(0.5, 0.63, 3.5);
			
			if(cancel)
				cancelEvents();
			
			setTimeMultiplier(1000);
			
			System.out.println("Testing netherwart grow event, cancel="+cancel);
			setBlock(0, -2, 0, Block.slowSand, 0);
			setBlock(0, -1, 0, Block.netherStalk, 0);
			waitForEvent();
			if((getBlockMeta(0, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing wheat grow event, cancel="+cancel);
			setBlock(1, 0, 0, Block.bedrock, 0);
			setBlock(1, -2, 0, Block.waterStill, 0);
			setBlock(0, -2, 0, Block.tilledField, 7);
			setBlock(0, -1, 0, Block.crops, 0);
			waitForEvent();
			if((getBlockMeta(0, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing carrot grow event, cancel="+cancel);
			setBlock(0, -1, 0, Block.carrot, 0);
			waitForEvent();
			if((getBlockMeta(0, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing potato grow event, cancel="+cancel);
			setBlock(0, -1, 0, Block.potato, 0);
			waitForEvent();
			if((getBlockMeta(0, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			setBlock(1, -2, 0, 0, 0);
			setBlock(0, -2, 0, 0, 0);
			setBlock(0, -4, 0, Block.dirt, 0);
			setBlock(0, -3, 0, Block.sand, 0);
			
			System.out.println("Testing cactus grow event, cancel="+cancel);
			setBlock(0, -2, 0, Block.cactus, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -2, 0, 0, 0); discardEvents();
			
			System.out.println("Testing reed grow event, cancel="+cancel);
			setBlock(1, -3, 0, Block.waterStill, 0);
			setBlock(0, -2, 0, Block.reed, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -2, 0, 0, 0); discardEvents();
		}
	}
}
