package net.minecraft.server;

import org.bukkit.craftbukkit.CraftBlock;
import org.bukkit.craftbukkit.CraftItemStack;
import org.bukkit.craftbukkit.CraftPlayer;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerItemEvent;


public class ItemFlintAndSteel extends Item {

    public ItemFlintAndSteel(int i) {
        super(i);
        aX = 1;
        aY = 64;
    }

    public boolean a(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l) {
        // Craftbukkit start - get the clicked block
        CraftBlock blockClicked = (CraftBlock) ((WorldServer) world).getWorld().getBlockAt(i, j, k);
        
        if (l == 0) {
            j--;
        }
        if (l == 1) {
            j++;
        }
        if (l == 2) {
            k--;
        }
        if (l == 3) {
            k++;
        }
        if (l == 4) {
            i--;
        }
        if (l == 5) {
            i++;
        }
        int i1 = world.a(i, j, k);

        if (i1 == 0) {
            // Craftbukkit start
            // Flint and steel
            
            CraftItemStack itemInHand = new CraftItemStack(itemstack);
            CraftPlayer thePlayer = new CraftPlayer(((WorldServer) world).getServer(), (EntityPlayerMP) entityplayer);
            PlayerItemEvent pie = new PlayerItemEvent(Type.PLAYER_ITEM, thePlayer, itemInHand, blockClicked, CraftBlock.notchToBlockFace(l));
            
            ((WorldServer) world).getServer().getPluginManager().callEvent(pie);
            
            boolean preventLighter = pie.isCancelled();
            
            if (preventLighter) {
                return false;
            } else {
                world.a((double) i + 0.5D, (double) j + 0.5D, (double) k + 0.5D, "fire.ignite", 1.0F, b.nextFloat() * 0.4F + 0.8F);
                world.d(i, j, k, Block.ar.bh);
            }
        }
        itemstack.b(1);
        return true;
    }
}
