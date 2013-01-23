package immibis.lavabukkit.tests.block;

import net.minecraft.block.Block;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

import immibis.lavabukkit.tests.Test;

/**
 * Tests:
 * <ul>
 * <li>That BlockBreakEvents are fired when the player breaks blocks.
 * <li>That at least one BlockDamageEvent is fired while the player is breaking a block.
 * 		(CB bug? CB doesn't appear to send it continuously, but it looks like it should)
 * <li>That cancelling the event prevents the block being broken.
 * <li>That cancelling the event does not cause the client to desync.
 * </ul>
 *
 */
public class TestBlockBreakEvent extends Test {
	public void test() throws Exception {
		for(int gamemode = 0; gamemode <= 1; gamemode++) {
			
			setGameMode(gamemode);
			
			for(int cancel = 0; cancel <= 1; cancel++) {
			
				for(int[] id_and_meta : new int[][] {
					new int[] {Block.mushroomCapBrown.blockID, 0}, // (normal block) - was mushroomCapBrown but that's too fast to send 2 damage events
					new int[] {Block.torchWood.blockID, 1}, // attached to +X face (attachable block)
					new int[] {Block.glass.blockID, 0}, // (block with no drops)
					new int[] {Block.glowStone.blockID, 0}, // (block that doesn't drop itself)
				}) {
				
					final int id = id_and_meta[0];
					final int meta = id_and_meta[1];
					
					// place supporting block
					setBlock(-1, 0, 0, Block.blockSnow, 0);
									
					// place block under test
					setBlock(0, 0, 0, id, meta);
						
					// place block for player to stand on
					setBlock(1, 0, 0, Block.bedrock.blockID, 0);
					
					if(cancel == 1)
						cancelNextEvent(BlockBreakEvent.class);
					
					System.out.println("Testing BlockBreakEvent in "+(gamemode == 0 ? "survival" : "creative")+", cancelling: "+(cancel==1)+" on "+id+":"+meta);
					
					// teleport player to block, point at test block
					teleportThePlayer(1.5, 1.63, 0.5);
					lookAt(0, 0.5, 0.5);
					
					setkey("keyBindAttack", true);
					
					if(gamemode == 0 && id != Block.torchWood.blockID)
						waitForEvent(BlockDamageEvent.class);
					
					BlockBreakEvent evt = waitForEvent(BlockBreakEvent.class);
					if(evt.getBlock().getX() != XOFF || evt.getBlock().getY() != YOFF || evt.getBlock().getZ() != ZOFF)
						throw new AssertionError("BlockBreakEvent had wrong coords");
					if(!evt.getPlayer().getName().equals(getPlayerUsername()))
						throw new AssertionError("BlockBreakEvent had wrong username");
					
					setkey("keyBindAttack", false);
					
					
					
					boolean doesBlockRemain = getBlock(0, 0, 0) != 0;
					
					if(cancel == 1 && !doesBlockRemain)
						throw new AssertionError("cancelled event but block still destroyed");
					if(cancel == 0 && doesBlockRemain)
						throw new AssertionError("didn't cancel event but block not destroyed");
					
					boolean doesBlockRemainOnClient = getClientBlock(0, 0, 0) != 0;
					
					if(doesBlockRemain != doesBlockRemainOnClient)
						throw new AssertionError("update wasn't sent to client");
				}
			}
		}
	}
}
