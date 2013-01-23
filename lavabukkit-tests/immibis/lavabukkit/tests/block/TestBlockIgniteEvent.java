package immibis.lavabukkit.tests.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;

import immibis.lavabukkit.tests.Test;

/**
 * Tests that:
 * <ul>
 * <li>A BlockIgniteEvent is fired when the player right-clicks a flammable block with a flint-and-steel.
 * <li>The fire is not placed if the event is cancelled.
 * <li>The above works in creative and survival.
 * </ul>
 *
 */
public class TestBlockIgniteEvent extends Test {
	public void test() throws Exception {
		for(int gamemode = 0; gamemode <= 1; gamemode++) {
			
			setGameMode(gamemode);
			
			for(int cancel = 0; cancel <= 1; cancel++) {

				// place flammable block
				setBlock(0, 0, 0, Block.cloth, 9);
					
				// place block for player to stand on
				setBlock(1, 0, 0, Block.bedrock.blockID, 0);
				
				// surround in bedrock
				setBlock(-1, 0, 0, Block.bedrock, 0);
				setBlock(0, 0, 1, Block.bedrock, 0);
				setBlock(0, 0, -1, Block.bedrock, 0);
				
				setHeldItem(new ItemStack(Item.flintAndSteel));
				
				System.out.println("Testing BlockIgniteEvent in "+(gamemode == 0 ? "survival" : "creative")+", cancelling: "+(cancel==1));
				
				// teleport player to block, point at test block
				teleportThePlayer(1.5, 1.63, 0.5);
				lookAt(0.5, 1.0, 0.5);
				
				setkey("keyBindUseItem", true);
				setkey("keyBindUseItem", false);
				
				if(cancel == 1)
					cancelNextEvent(BlockIgniteEvent.class);
				
				BlockIgniteEvent evt = waitForEvent(BlockIgniteEvent.class);
				assertCoordsEqual(evt.getBlock(), 0, 1, 0);
				if(!evt.getPlayer().getName().equals(getPlayerUsername()))
					throw new AssertionError("BlockIgniteEvent had wrong username");
				
				
				
				boolean isBlockOnFire = getBlock(0, 1, 0) != 0;
				
				if(cancel == 1 && isBlockOnFire)
					throw new AssertionError("cancelled event but block still on fire");
				if(cancel == 0 && !isBlockOnFire)
					throw new AssertionError("didn't cancel event but block not on fire");
				
				boolean isBlockOnFireOnClient = getClientBlock(0, 1, 0) != 0;
				
				if(isBlockOnFire != isBlockOnFireOnClient)
					throw new AssertionError("update wasn't sent to client");
				
				setBlock(0, 0, 0, 0, 0);
				Thread.sleep(500);
				setBlock(0, 1, 0, 0, 0);
				discardEvents();
			}
		}
	}
}
