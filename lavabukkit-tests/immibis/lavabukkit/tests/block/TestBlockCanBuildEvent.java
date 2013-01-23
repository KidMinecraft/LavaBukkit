package immibis.lavabukkit.tests.block;

import org.bukkit.event.block.BlockCanBuildEvent;

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
 * <li>BlockCanBuildEvents are triggered when the player places "normal" blocks.
 * <li>Calling event.setBuildable(false) disallows the placement.
 * </ul>
 * Does not test that:
 * <ul>
 * <li>Calling event.setBuildable(true) allows the placement.
 * </ul>
 *
 */
public class TestBlockCanBuildEvent extends Test {
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
					//Item.bed.itemID, // a bed (event not triggered for this; TODO LB or CB bug or neither?)
					Block.stoneSingleSlab.blockID, // a slab
					//Item.doorWood.itemID, // a door (event not triggered for this; TODO LB or CB bug or neither?)
				}) {
					
					System.out.println("testing BlockCanBuildEvent, id="+heldID+", gamemode="+gamemode+", cancel="+doCancel);
					
					setHeldItem(new ItemStack(heldID, 1, 0));
					setkey("keyBindUseItem", true);
					setkey("keyBindUseItem", false);
					
					if(doCancel)
						runOnNextEvent(BlockCanBuildEvent.class, new EventCallback<BlockCanBuildEvent>() {
							@Override
							public void handle(BlockCanBuildEvent event)throws Exception {
								event.setBuildable(false);
							}
						});
					BlockCanBuildEvent b = waitForEvent(BlockCanBuildEvent.class);
					assertCoordsEqual(b.getBlock(), 1, 1, 0);
					if(b.getMaterialId() != heldID)
						throw new AssertionError("Wrong material ID");
					if(b.getMaterial().getId() != heldID)
						throw new AssertionError("Wrong material");
					
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
