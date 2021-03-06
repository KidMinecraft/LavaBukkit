package net.minecraft.command;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumGameType;

public class CommandServerPublishLocal extends CommandBase
{
    public String getCommandName()
    {
        return "publish";
    }

    /**
     * Return the required permission level for this command.
     */
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    public void processCommand(ICommandSender par1ICommandSender, String[] par2ArrayOfStr)
    {
        String var3 = MinecraftServer.getServer().shareToLAN(EnumGameType.SURVIVAL, false);

        if (var3 != null)
        {
            notifyAdmins(par1ICommandSender, "commands.publish.started", new Object[] {var3});
        }
        else
        {
            notifyAdmins(par1ICommandSender, "commands.publish.failed", new Object[0]);
        }
    }
}
