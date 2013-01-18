package immibis.lavabukkit.nms;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.bukkit.BlockChangeDelegate;

public class NMSUtils {
	public static MCPBlockChangeDelegate createBCD(BlockChangeDelegate bcd) {
		return new BukkitToMCPAdapter(bcd);
	}
	public static MCPBlockChangeDelegate createBCD(World world) {
		return new WorldToMCPAdapter(world);
	}
	public static BlockChangeDelegate createBukkitBCD(World world) {
		return new WorldToBukkitAdapter(world);
	}
	
	private static final class BukkitToMCPAdapter implements MCPBlockChangeDelegate {
		private final BlockChangeDelegate wraps;
		
		public BukkitToMCPAdapter(BlockChangeDelegate wraps) {
			this.wraps = wraps;
		}

		@Override
		public void setBlockAndMetadataWithNotify(int x, int y, int z, int id,int meta) {
			wraps.setTypeIdAndData(x, y, z, id, meta);
		}

		@Override
		public void setBlockAndMetadata(int x, int y, int z, int id, int meta) {
			wraps.setRawTypeIdAndData(x, y, z, id, meta);
		}

		@Override
		public int getBlockId(int x, int y, int z) {
			return wraps.getTypeId(x, y, z);
		}

		@Override
		public boolean isLeaves(Block block, int x, int y, int z) {
			return block instanceof BlockLeavesBase; 
		}

		@Override
		public boolean canBeReplacedByLeaves(Block block, int x, int y, int z) {
			return block.isOpaqueCube();
		}

		@Override
		public void setBlock(int x, int y, int z, int id) {
			wraps.setRawTypeId(x, y, z, id);
		}

		@Override
		public boolean isAirBlock(int x, int y, int z) {
			return wraps.isEmpty(x, y, z);
		}

		@Override
		public boolean isWood(Block block, int x, int y, int z) {
			return block instanceof BlockLog;
		}

		@Override
		public Material getBlockMaterial(int x, int y, int z) {
			Block block = Block.blocksList[wraps.getTypeId(x, y, z)];
			if(block == null)
				return Material.air;
			return block.blockMaterial;
		}
	}
	
	private static final class WorldToMCPAdapter implements MCPBlockChangeDelegate {
		private final World wraps;
		
		public WorldToMCPAdapter(World wraps) {
			this.wraps = wraps;
		}

		@Override
		public void setBlockAndMetadataWithNotify(int x, int y, int z, int id, int meta) {
			wraps.setBlockAndMetadataWithNotify(x, y, z, id, meta);
		}

		@Override
		public void setBlockAndMetadata(int x, int y, int z, int id, int meta) {
			wraps.setBlockAndMetadata(x, y, z, id, meta);
		}

		@Override
		public int getBlockId(int x, int y, int z) {
			return wraps.getBlockId(x, y, z);
		}

		@Override
		public boolean isLeaves(Block block, int x, int y, int z) {
			return block.isLeaves(wraps, x, y, z);
		}

		@Override
		public boolean canBeReplacedByLeaves(Block block, int x, int y, int z) {
			return block.canBeReplacedByLeaves(wraps, x, y, z);
		}

		@Override
		public void setBlock(int x, int y, int z, int id) {
			wraps.setBlock(x, y, z, id);
		}

		@Override
		public boolean isAirBlock(int x, int y, int z) {
			return wraps.isAirBlock(x, y, z);
		}

		@Override
		public boolean isWood(Block block, int x, int y, int z) {
			return block.isWood(wraps, x, y, z);
		}

		@Override
		public Material getBlockMaterial(int x, int y, int z) {
			return wraps.getBlockMaterial(x, y, z);
		}
	}
	
	private static final class WorldToBukkitAdapter implements BlockChangeDelegate {
		private final World wraps;
		
		public WorldToBukkitAdapter(World wraps) {
			this.wraps = wraps;
		}

		@Override
		public boolean setRawTypeId(int x, int y, int z, int typeId) {
			return wraps.setBlock(x, y, z, typeId);
		}

		@Override
		public boolean setRawTypeIdAndData(int x, int y, int z, int typeId, int data) {
			return wraps.setBlockAndMetadata(x, y, z, typeId, data);
		}

		@Override
		public boolean setTypeId(int x, int y, int z, int typeId) {
			return wraps.setBlockWithNotify(x, y, z, typeId);
		}

		@Override
		public boolean setTypeIdAndData(int x, int y, int z, int typeId, int data) {
			return wraps.setBlockAndMetadataWithNotify(x, y, z, typeId, data);
		}

		@Override
		public int getTypeId(int x, int y, int z) {
			return wraps.getBlockId(x, y, z);
		}

		@Override
		public int getHeight() {
			return wraps.getActualHeight();
		}

		@Override
		public boolean isEmpty(int x, int y, int z) {
			return wraps.isAirBlock(x, y, z);
		}
	}
}
