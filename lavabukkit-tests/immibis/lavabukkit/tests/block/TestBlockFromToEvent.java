package immibis.lavabukkit.tests.block;

import java.util.concurrent.Callable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDragonEgg;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;

import immibis.lavabukkit.tests.EventCallback;
import immibis.lavabukkit.tests.EventMatcher;
import immibis.lavabukkit.tests.Test;

/**
 * Tests that:
 * <ul>
 * <li>Water fires a BlockFromToEvent when it flows.
 * <li>Lava fires a BlockFromToEvent when it flows.
 * <li>Dragon eggs fire a BlockFromToEvent when they teleport.
 * </ul>
 *
 */
public class TestBlockFromToEvent extends Test {
	private void cancelAllMoves() {
		setEventHandler(new EventCallback<Event>() {
			public void handle(Event event_) {
				if(event_ instanceof BlockFromToEvent) {
					BlockFromToEvent event = (BlockFromToEvent)event_;
					//System.out.println("cancel "+event.getBlock().getX()+" "+event.getBlock().getY()+" "+event.getBlock().getZ());
					if(event.getBlock().getX() == XOFF
						&& event.getBlock().getY() == YOFF - 1
						&& event.getBlock().getZ() == ZOFF)
						event.setCancelled(true);
				}
			}
		});
	}
	private BlockFromToEvent waitForEvent() throws Exception {
		return waitForEvent(BlockFromToEvent.class, new EventMatcher<BlockFromToEvent>() {
			public boolean matches(BlockFromToEvent event) throws Exception {
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
			setBlock(0, -2, 0, Block.bedrock, 0);
			
			setBlock(0, -2, 2, Block.bedrock, 0);
			teleportThePlayer(0.5, -1.0, 2.5);
			lookAt(0.5, -0.5, 0.5);
			
			if(cancel)
				cancelAllMoves();
			
			System.out.println("Testing water from-to event, cancel="+cancel);
			setBlock(0, -1, 0, Block.waterMoving, 0);
			waitForEvent();
			if((getBlock(-1, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0);
			setBlock(-1, -1, 0, 0, 0);
			setBlock(1, -1, 0, 0, 0);
			setBlock(0, -1, 1, 0, 0);
			setBlock(0, -1, -1, 0, 0);
			discardEvents();
			
			System.out.println("Testing lava from-to event, cancel="+cancel);
			setBlock(0, -1, 0, Block.lavaMoving, 0);
			waitForEvent();
			if((getBlock(-1, -1, 0) == 0) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0);
			setBlock(-1, -1, 0, 0, 0);
			setBlock(1, -1, 0, 0, 0);
			setBlock(0, -1, 1, 0, 0);
			setBlock(0, -1, -1, 0, 0);
			discardEvents();
			
			System.out.println("Testing dragon egg from-to event, cancel="+cancel);
			setBlock(0, -1, 0, Block.dragonEgg, 0);
			onserver(new Callable<Void>() {public Void call() {
				((BlockDragonEgg)Block.dragonEgg).onBlockActivated(
					DimensionManager.getWorld(0),
					XOFF, YOFF-1, ZOFF,
					null, 0, 0, 0, 0);
				return null;
			}});
			BlockFromToEvent evt = waitForEvent();
			if((getBlock(0, -1, 0) == Block.dragonEgg.blockID) != cancel)
				throw new AssertionError(cancel ? "Cancelled event but block still changed" : "Didn't cancel event but block didn't change");
			setBlock(0, -1, 0, 0, 0);
			discardEvents();
		}
	}
}
