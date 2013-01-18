package net.minecraft.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import net.minecraft.util.CryptManager;

class ThreadLoginVerifier extends Thread
{
    /** The login handler that spawned this thread. */
    final NetLoginHandler loginHandler;

    // CraftBukkit start
    CraftServer server;
    ThreadLoginVerifier(NetLoginHandler par1NetLoginHandler, CraftServer server)
    {
    	this.server = server;
    	// CraftBukkit end
        this.loginHandler = par1NetLoginHandler;
    }

    public void run()
    {
        try
        {
            String var1 = (new BigInteger(CryptManager.getServerIdHash(NetLoginHandler.getServerId(this.loginHandler), NetLoginHandler.getLoginMinecraftServer(this.loginHandler).getKeyPair().getPublic(), NetLoginHandler.getSharedKey(this.loginHandler)))).toString(16);
            URL var2 = new URL("http://session.minecraft.net/game/checkserver.jsp?user=" + URLEncoder.encode(NetLoginHandler.getClientUsername(this.loginHandler), "UTF-8") + "&serverId=" + URLEncoder.encode(var1, "UTF-8"));
            BufferedReader var3 = new BufferedReader(new InputStreamReader(var2.openStream()));
            String var4 = var3.readLine();
            var3.close();

            if (!"YES".equals(var4))
            {
                this.loginHandler.raiseErrorAndDisconnect("Failed to verify username!");
                return;
            }
            
            // CraftBukkit start
            if (this.loginHandler.getSocket() == null) {
                return;
            }

            AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(NetLoginHandler.getClientUsername(this.loginHandler), this.loginHandler.getSocket().getInetAddress());
            this.server.getPluginManager().callEvent(asyncEvent);

            // LavaBukkit: removed deprecated PlayerPreLoginEvent
            if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                this.loginHandler.raiseErrorAndDisconnect(asyncEvent.getKickMessage());
                return;
            }
            // CraftBukkit end

            NetLoginHandler.func_72531_a(this.loginHandler, true);
            // CraftBukkit start
        } catch (java.io.IOException exception) {
            this.loginHandler.raiseErrorAndDisconnect("Failed to verify username, session authentication server unavailable!");
        } catch (Exception exception) {
            this.loginHandler.raiseErrorAndDisconnect("Failed to verify username!");
            server.getLogger().log(java.util.logging.Level.WARNING, "Exception verifying " + NetLoginHandler.getClientUsername(this.loginHandler), exception);
            // CraftBukkit end
        }
    }
}
