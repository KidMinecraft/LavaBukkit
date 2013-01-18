package immibis.lavabukkit.nms;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public interface MCPBlockChangeDelegate {

	void setBlockAndMetadataWithNotify(int x, int y, int z, int id, int meta);

	void setBlockAndMetadata(int x, int y, int z, int id, int meta);

	int getBlockId(int x, int y, int z);

	boolean isLeaves(Block block, int x, int y, int z);

	boolean canBeReplacedByLeaves(Block block, int x, int y, int z);

	void setBlock(int x, int y, int z, int id);

	boolean isAirBlock(int x, int y, int z);

	boolean isWood(Block block, int x, int y, int z);

	Material getBlockMaterial(int x, int y, int z);
	
}
