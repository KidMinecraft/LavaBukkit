package org.bukkit.craftbukkit.command;

import net.minecraft.tileentity.TileEntityCommandBlock;

import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;

/**
 * Represents input from a command block
 */
public class CraftBlockCommandSender extends ServerCommandSender implements BlockCommandSender {
    private final TileEntityCommandBlock commandBlock;

    public CraftBlockCommandSender(TileEntityCommandBlock commandBlock) {
        super();
        this.commandBlock = commandBlock;
    }

    public Block getBlock() {
        return commandBlock.worldObj.getWorld().getBlockAt(commandBlock.xCoord, commandBlock.yCoord, commandBlock.zCoord);
    }

    public void sendMessage(String message) {
    }

    public void sendRawMessage(String message) {
    }

    public void sendMessage(String[] messages) {
    }

    public String getName() {
        return "@";
    }

    public boolean isOp() {
        return true;
    }

    public void setOp(boolean value) {
        throw new UnsupportedOperationException("Cannot change operator status of a block");
    }
}
