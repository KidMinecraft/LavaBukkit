package immibis.lavabukkit.tests.block;

import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBed;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import immibis.lavabukkit.tests.EventCallback;
import immibis.lavabukkit.tests.Test;

/**
 * Tests that:
 * <ul>
 * <li>BlockPlaceEvents are triggered when the player places "normal" blocks, attachable blocks, doors and beds.
 * <li>Cancelling the event prevents the placement.
 * </ul>
 *
 */
public class TestBlockPlaceEvent extends Test {
	public void test() throws Exception {
		for(int gamemode = 0; gamemode <= 1; gamemode++) {
			setGameMode(gamemode);
			
			// where player stands
			setBlock(0, 0, 0, Block.bedrock, 0);
			setBlock(0, 1, 0, 0, 0);
			setBlock(0, 2, 0, 0, 0);
			
			// supporting blocks
			setBlock(1, 0, 0, Block.bedrock, 0);
			setBlock(2, 0, 0, Block.bedrock, 0);
			
			teleportThePlayer(0.5, 1.62, 0.5);
			lookAt(1.5, 1, 0.5); // top centre of closest supporting block
			
			for(int cancel = 0; cancel <= 1; cancel++) {
				boolean doCancel = (cancel == 1);
				
				for(int heldID : new int[] {
					Block.dirt.blockID, // a normal block
					Item.bed.itemID, // a bed
					Block.stoneSingleSlab.blockID, // a slab
					Item.doorWood.itemID, // a door
				}) {
					
					System.out.println("testing BlockPlaceEvent, id="+heldID+", gamemode="+gamemode+", cancel="+doCancel);
					
					setHeldItem(new ItemStack(heldID, 1, 0));
					setkey("keyBindUseItem", true);
					setkey("keyBindUseItem", false);
					
					if(doCancel)
						cancelNextEvent(BlockPlaceEvent.class);
					BlockPlaceEvent b = waitForEvent(BlockPlaceEvent.class);
					assertCoordsEqual(b.getBlock(), 1, 1, 0);
					if(b.getItemInHand().getTypeId() != heldID)
						throw new AssertionError("Wrong material ID");
					
					boolean placed = getBlock(1, 1, 0) != 0;
					if(placed && doCancel)
						throw new AssertionError("cancelled but block still placed");
					if(!placed && !doCancel)
						throw new AssertionError("placement failed without being cancelled");
					
					Item item = Item.itemsList[heldID];
					if(item instanceof ItemBed) {
						if((getBlock(2, 1, 0) == 0) != doCancel)
							throw new AssertionError("bed placed improperly");
						if((getClientBlock(2, 1, 0) == 0) != doCancel)
							throw new AssertionError("bed synced improperly");
					}
					if(item instanceof ItemDoor) {
						if((getBlock(1, 2, 0) == 0) != doCancel)
							throw new AssertionError("door placed improperly");
						if((getClientBlock(1, 2, 0) == 0) != doCancel)
							throw new AssertionError("door synced improperly");
					}
					
					setBlock(2, 1, 0, 0, 0);
					setBlock(1, 1, 0, 0, 0);
					setBlock(1, 2, 0, 0, 0);
				}
			}
		}
	}
}
