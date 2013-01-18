package net.minecraft.entity.projectile;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEggThrowEvent;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class EntityEgg extends EntityThrowable
{
    public EntityEgg(World par1World)
    {
        super(par1World);
    }

    public EntityEgg(World par1World, EntityLiving par2EntityLiving)
    {
        super(par1World, par2EntityLiving);
    }

    public EntityEgg(World par1World, double par2, double par4, double par6)
    {
        super(par1World, par2, par4, par6);
    }

    /**
     * Called when this EntityThrowable hits a block or entity.
     */
    protected void onImpact(MovingObjectPosition par1MovingObjectPosition)
    {
        if (par1MovingObjectPosition.entityHit != null)
        {
            par1MovingObjectPosition.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 0);
        }

        // CraftBukkit start
        if (!this.worldObj.isRemote)
        {
        	boolean hatching = this.rand.nextInt(8) == 0;
        	
            byte var2 = 1;

            if (this.rand.nextInt(32) == 0)
            {
                var2 = 4;
            }
            
            EntityType hatchingType = EntityType.CHICKEN;
            
            EntityLiving thrower = super.getThrower();
            org.bukkit.entity.Entity bthrower = thrower == null ? null : thrower.getBukkitEntity();
            if(bthrower instanceof Player) {
            	PlayerEggThrowEvent event = new PlayerEggThrowEvent((Player)bthrower, (org.bukkit.entity.Egg)getBukkitEntity(), hatching, var2, hatchingType);
            	
            	worldObj.getServer().getPluginManager().callEvent(event);
            	
            	hatching = event.isHatching();
            	var2 = event.getNumHatches();
            	hatchingType = event.getHatchingType();
            }
            
            if (hatching) {
	            for (int k = 0; k < var2; k++) {
	                org.bukkit.entity.Entity entity = worldObj.getWorld().spawn(new org.bukkit.Location(worldObj.getWorld(), this.posX, this.posY, this.posZ, this.rotationYaw, 0.0F), hatchingType.getEntityClass(), org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.EGG);
	                if (entity instanceof Ageable) {
	                    ((Ageable) entity).setBaby();
	                }
	            }
	        }
        }
        // CraftBukkit end

        for (int var5 = 0; var5 < 8; ++var5)
        {
            this.worldObj.spawnParticle("snowballpoof", this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
        }

        if (!this.worldObj.isRemote)
        {
            this.setDead();
        }
    }
}
