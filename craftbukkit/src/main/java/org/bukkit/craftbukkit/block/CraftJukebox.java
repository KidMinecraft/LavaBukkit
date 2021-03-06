package org.bukkit.craftbukkit.block;

import net.minecraft.block.BlockJukeBox;
import net.minecraft.block.TileEntityRecordPlayer;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.craftbukkit.CraftWorld;

public class CraftJukebox extends CraftBlockState implements Jukebox {
    private final CraftWorld world;
    private final TileEntityRecordPlayer jukebox;

    public CraftJukebox(final Block block) {
        super(block);

        world = (CraftWorld) block.getWorld();
        jukebox = (TileEntityRecordPlayer) world.getTileEntityAt(getX(), getY(), getZ());
    }

    public Material getPlaying() {
        return Material.getMaterial(jukebox.record.itemID);
    }

    public void setPlaying(Material record) {
        if (record == null) {
            record = Material.AIR;
        }

        jukebox.record = new net.minecraft.item.ItemStack(net.minecraft.item.Item.itemsList[record.getId()], 1);
        jukebox.onInventoryChanged();

        if (record == Material.AIR) {
            world.getHandle().setBlockMetadataWithNotify(getX(), getY(), getZ(), 0);
        } else {
            world.getHandle().setBlockMetadataWithNotify(getX(), getY(), getZ(), 1);
        }
        world.playEffect(getLocation(), Effect.RECORD_PLAY, record.getId());
    }

    public boolean isPlaying() {
        return getRawData() == 1;
    }

    public boolean eject() {
        boolean result = isPlaying();
        ((BlockJukeBox) net.minecraft.block.Block.jukebox).ejectRecord(world.getHandle(), getX(), getY(), getZ());
        return result;
    }
}
