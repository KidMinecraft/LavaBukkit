package immibis.lavabukkit.tests.block;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockFadeEvent;

import net.minecraft.block.Block;
import immibis.lavabukkit.tests.EventCallback;
import immibis.lavabukkit.tests.EventMatcher;
import immibis.lavabukkit.tests.Test;

/**
 * Tests that:
 * <ul>
 * <li>Grass fires a BlockFadeEvent when it reverts to dirt.
 * <li>Mycelium fires a BlockFadeEvent when it reverts to dirt.
 * <li>Farmland fires a BlockFadeEvent when it reverts to dirt due to not being near water.
 * <li>Snow fires a BlockFadeEvent when it disappears due to not being on a solid block.
 * <li>Ice fires a BlockFadeEvent when it melts.
 * <li>Snow fires a BlockFadeEvent when it melts.
 * <li>Fire fires a BlockFadeEvent when it burns out.
 * <li>Cancelling BlockFadeEvents prevents all of the above things from happening.
 * </ul>
 */
public class TestBlockFadeEvent extends Test {
	private void cancelAllFades() {
		setEventHandler(new EventCallback<Event>() {
			public void handle(Event event_) {
				if(event_ instanceof BlockFadeEvent) {
					BlockFadeEvent event = (BlockFadeEvent)event_;
					//System.out.println("cancel "+event.getBlock().getX()+" "+event.getBlock().getY()+" "+event.getBlock().getZ());
					if(event.getBlock().getX() == XOFF
						&& event.getBlock().getY() == YOFF - 1
						&& event.getBlock().getZ() == ZOFF)
						event.setCancelled(true);
				}
			}
		});
	}
	private void waitForEvent() throws Exception {
		waitForEvent(BlockFadeEvent.class, new EventMatcher<BlockFadeEvent>() {
			public boolean matches(BlockFadeEvent event) throws Exception {
				//System.out.println("fade "+event.getBlock().getX()+" "+event.getBlock().getY()+" "+event.getBlock().getZ()+" "+event.isCancelled());
				return event.getBlock().getX() == XOFF
					&& event.getBlock().getY() == YOFF - 1
					&& event.getBlock().getZ() == ZOFF;
			}
		});
	}
	public void test() throws Exception {
		
		for(int cancel_ = 0; cancel_ <= 1; cancel_++) {
			boolean cancel = cancel_ == 1;
			
			resetState();
			
			setBlock(0, 0, 0, Block.bedrock, 0);
			
			setBlock(0, -2, 2, Block.bedrock, 0);
			teleportThePlayer(0.5, -1.0, 2.5);
			lookAt(0.5, -0.5, 0.5);
			
			setTimeMultiplier(100);
			
			if(cancel)
				cancelAllFades();
			
			System.out.println("Testing grass fade event, cancel="+cancel);
			setBlock(0, -1, 0, Block.grass, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == Block.grass.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing mycelium fade event, cancel="+cancel);
			setBlock(0, -1, 0, Block.mycelium, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == Block.mycelium.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing snow fade event, cancel="+cancel);
			setBlock(0, -2, 0, Block.dirt, 0);
			setBlock(0, -1, 0, Block.snow, 0);
			setBlock(0, -2, 0, 0, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == Block.snow.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing farmland fade event, cancel="+cancel);
			setBlock(0, 0, 0, 0, 0);
			setBlock(0, -1, 0, Block.tilledField, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == Block.tilledField.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, 0, 0, Block.bedrock, 0);
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing ice melt event, cancel="+cancel);
			setBlock(0, -1, 0, Block.ice, 0);
			setBlock(-1, 0, 0, Block.glowStone, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == Block.ice.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing snow melt event, cancel="+cancel);
			setBlock(0, -2, 0, Block.bedrock, 0);
			setBlock(0, -1, 0, Block.snow, 0);
			setBlock(-1, 0, 0, Block.glowStone, 0);
			waitForEvent();
			if((getBlock(0, -1, 0) == Block.snow.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing fire fade event, cancel="+cancel);
			setBlock(0, -1, 0, Block.fire, 0);
			setBlock(-1, -1, 0, Block.cloth, 5);
			waitForEvent();
			if((getBlock(0, -1, 0) == Block.fire.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
		}
	}
}
