package immibis.lavabukkit.tests.block;

import immibis.lavabukkit.tests.Test;
import net.minecraft.block.Block;

import org.bukkit.event.block.BlockBurnEvent;

/**
 * Tests:
 * <ul>
 * <li> That BlockBurnEvents are fired (no pun intended) when a block is consumed by fire.
 * <li> That cancelling the event prevents the block from being consumed.
 * </ul>
 *
 */
public class TestBlockBurnEvent extends Test {
	public void test() throws Exception {
		// place block for player to stand on
		setBlock(1, 0, 0, Block.bedrock.blockID, 0);
						
		// place block under test and set it on fire
		setBlock(0, 0, 0, Block.cloth.blockID, 0);
		setBlock(0, 1, 0, Block.fire.blockID, 0);
		
		setDifficulty(3);
		
		System.out.println("Testing BlockBurnEvent, without cancelling");
		teleportThePlayer(1.5, 1.62, 0.5);
		lookAt(0.5, 1, 0.5);
		
		// accelerate time until block is gone
		setTimeMultiplier(20);
		
		while(getBlock(0, 0, 0) != 0)
			Thread.sleep(50);
		
		// confirm BlockBurnEvent was fired
		BlockBurnEvent evt = assertReceivedEvent(BlockBurnEvent.class);
		assertCoordsEqual(evt.getBlock(), 0, 0, 0);
		
		System.out.println("Testing BlockBurnEvent, cancelling");
		
		// now do that again but cancelling the event
		setBlock(0, 0, 0, Block.cloth, 0);
		setBlock(0, 1, 0, Block.fire, 0);
		
		cancelNextEvent(BlockBurnEvent.class);
		evt = waitForEvent(BlockBurnEvent.class);
		assertCoordsEqual(evt.getBlock(), 0, 0, 0);
		
		if(getBlock(0, 0, 0) == 0)
			throw new AssertionError("Block was destroyed after cancelling burn event");

		resetState();
	}
}
