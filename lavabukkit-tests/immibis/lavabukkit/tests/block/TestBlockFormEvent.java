package immibis.lavabukkit.tests.block;

import java.util.concurrent.Callable;

import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;

import immibis.lavabukkit.tests.EventCallback;
import immibis.lavabukkit.tests.EventMatcher;
import immibis.lavabukkit.tests.Test;

/**
 * Tests that:
 * <ul>
 * <li>A BlockFormEvent is called when snow appears on top of a solid block.
 * <li>A BlockFormEvent is called when water freezes into ice.
 * <li>Cancelling the event prevents the block from changing.
 * </ul>
 *
 */
public class TestBlockFormEvent extends Test {
	private void cancelAllForms() {
		setEventHandler(new EventCallback<Event>() {
			public void handle(Event event_) {
				if(event_ instanceof BlockFormEvent) {
					BlockFormEvent event = (BlockFormEvent)event_;
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
		waitForEvent(BlockFormEvent.class, new EventMatcher<BlockFormEvent>() {
			public boolean matches(BlockFormEvent event) throws Exception {
				//System.out.println("fade "+event.getBlock().getX()+" "+event.getBlock().getY()+" "+event.getBlock().getZ()+" "+event.isCancelled());
				return event.getBlock().getX() == XOFF
					&& event.getBlock().getY() == YOFF - 1
					&& event.getBlock().getZ() == ZOFF;
			}
		});
	}
	
	public void test() throws Exception {
		// set biome to ice plains
		onserver(new Callable<Void>() {public Void call() {
			DimensionManager.getWorld(0).getChunkFromBlockCoords(XOFF, ZOFF).getBiomeArray()
				[(ZOFF & 15) << 4 | (XOFF & 15)] = (byte)BiomeGenBase.icePlains.biomeID;
			return null;
		}});
		
		for(int cancel_ = 0; cancel_ <= 1; cancel_++) {
			boolean cancel = cancel_ == 1;
			
			resetState();
			
			setBlock(0, -2, 0, Block.bedrock, 0);
			
			setBlock(0, -2, 2, Block.bedrock, 0);
			teleportThePlayer(0.5, -1.0, 2.5);
			lookAt(0.5, -0.5, 0.5);
			
			if(cancel)
				cancelAllForms();
			
			System.out.println("Testing snow form event, cancel="+cancel);
			setBlock(0, -1, 0, 0, 0);
			setTimeMultiplier(1000);
			setRaining(true);
			waitForEvent();
			setTimeMultiplier(1);
			if((getBlock(0, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
			System.out.println("Testing ice form event, cancel="+cancel);
			setBlock(0, -1, 0, Block.waterStill, 0);
			setTimeMultiplier(1000);
			setRaining(true);
			waitForEvent();
			setTimeMultiplier(1);
			if((getBlock(0, -1, 0) == Block.waterStill.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0); discardEvents();
			
		}
	}
}
