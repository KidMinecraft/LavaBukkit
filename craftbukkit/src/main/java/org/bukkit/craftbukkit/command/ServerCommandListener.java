package org.bukkit.craftbukkit.command;

import java.lang.reflect.Method;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.StringTranslate;

import org.bukkit.command.CommandSender;

public class ServerCommandListener implements ICommandSender {
    private final CommandSender commandSender;
    private final String prefix;

    public ServerCommandListener(CommandSender commandSender) {
        this.commandSender = commandSender;
        String[] parts = commandSender.getClass().getName().split("\\.");
        this.prefix = parts[parts.length - 1];
    }

    @Override
    public void sendChatToPlayer(String msg) {
        this.commandSender.sendMessage(msg);
    }

    public CommandSender getSender() {
        return commandSender;
    }

    @Override
    public String getCommandSenderName() {
        try {
            Method getName = commandSender.getClass().getMethod("getName");

            return (String) getName.invoke(commandSender);
        } catch (Exception e) {}

        return this.prefix;
    }

    @Override
    public String translateString(String s, Object... aobject) {
        return StringTranslate.getInstance().translateKeyFormat(s, aobject);
    }

    @Override
    public boolean canCommandSenderUseCommand(int i, String s) {
        return true;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return new ChunkCoordinates(0, 0, 0);
    }
}
