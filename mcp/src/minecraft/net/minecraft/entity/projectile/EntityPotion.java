package net.minecraft.entity.projectile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EntityPotion extends EntityThrowable
{
    /**
     * The damage value of the thrown potion that this EntityPotion represents.
     */
    private ItemStack potionDamage;

    public EntityPotion(World par1World)
    {
        super(par1World);
    }

    public EntityPotion(World par1World, EntityLiving par2EntityLiving, int par3)
    {
        this(par1World, par2EntityLiving, new ItemStack(Item.potion, 1, par3));
    }

    public EntityPotion(World par1World, EntityLiving par2EntityLiving, ItemStack par3ItemStack)
    {
        super(par1World, par2EntityLiving);
        this.potionDamage = par3ItemStack;
    }

    @SideOnly(Side.CLIENT)
    public EntityPotion(World par1World, double par2, double par4, double par6, int par8)
    {
        this(par1World, par2, par4, par6, new ItemStack(Item.potion, 1, par8));
    }

    public EntityPotion(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack)
    {
        super(par1World, par2, par4, par6);
        this.potionDamage = par8ItemStack;
    }

    /**
     * Gets the amount of gravity to apply to the thrown entity with each tick.
     */
    protected float getGravityVelocity()
    {
        return 0.05F;
    }

    protected float func_70182_d()
    {
        return 0.5F;
    }

    protected float func_70183_g()
    {
        return -20.0F;
    }

    public void setPotionDamage(int par1)
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }

        this.potionDamage.setItemDamage(par1);
    }

    /**
     * Returns the damage value of the thrown potion that this EntityPotion represents.
     */
    public int getPotionDamage()
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }

        return this.potionDamage.getItemDamage();
    }

    /**
     * Called when this EntityThrowable hits a block or entity.
     */
    protected void onImpact(MovingObjectPosition par1MovingObjectPosition)
    {
        if (!this.worldObj.isRemote)
        {
            List var2 = Item.potion.getEffects(this.potionDamage);

            if (var2 != null && !var2.isEmpty())
            {
                AxisAlignedBB var3 = this.boundingBox.expand(4.0D, 2.0D, 4.0D);
                List var4 = this.worldObj.getEntitiesWithinAABB(EntityLiving.class, var3);

                if (var4 != null && !var4.isEmpty())
                {
                    Iterator var5 = var4.iterator();
                    
                    // CraftBukkit
                    HashMap<LivingEntity, Double> affected = new HashMap<LivingEntity, Double>();

                    while (var5.hasNext())
                    {
                        EntityLiving var6 = (EntityLiving)var5.next();
                        double var7 = this.getDistanceSqToEntity(var6);

                        if (var7 < 16.0D)
                        {
                            double var9 = 1.0D - Math.sqrt(var7) / 4.0D;

                            if (var6 == par1MovingObjectPosition.entityHit)
                            {
                                var9 = 1.0D;
                            }
                            
                            // CraftBukkit start
                            affected.put((LivingEntity) var6.getBukkitEntity(), var9);
                        }
                    }
                    
                    org.bukkit.event.entity.PotionSplashEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callPotionSplashEvent(this, affected);
                    
                    if (!event.isCancelled()) {
                        for (LivingEntity victim : event.getAffectedEntities()) {
                            if (!(victim instanceof CraftLivingEntity)) {
                                continue;
                            }

                            EntityLiving var6 = ((CraftLivingEntity) victim).getHandle();
                            double var9 = event.getIntensity(victim);
                            // CraftBukkit end


                            Iterator var11 = var2.iterator();

                            while (var11.hasNext())
                            {
                                PotionEffect var12 = (PotionEffect)var11.next();
                                int var13 = var12.getPotionID();
                                
                                // CraftBukkit start - abide by PVP settings - for players only!
                                if (!this.worldObj.pvpMode && this.getThrower() instanceof EntityPlayer && var6 instanceof EntityPlayer && var6 != this.getThrower()) {
                                	if(Potion.potionTypes[var13].isBadEffect())
                                		continue;
                                }
                                // CraftBukkit end

                                if (Potion.potionTypes[var13].isInstant())
                                {
                                	// CraftBukkit - added 'this'
                                    Potion.potionTypes[var13].affectEntity(this.getThrower(), var6, var12.getAmplifier(), var9, this);
                                }
                                else
                                {
                                    int var14 = (int)(var9 * (double)var12.getDuration() + 0.5D);

                                    if (var14 > 20)
                                    {
                                        var6.addPotionEffect(new PotionEffect(var13, var14, var12.getAmplifier()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.worldObj.playAuxSFX(2002, (int)Math.round(this.posX), (int)Math.round(this.posY), (int)Math.round(this.posZ), this.getPotionDamage());
            this.setDead();
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);

        if (par1NBTTagCompound.hasKey("Potion"))
        {
            this.potionDamage = ItemStack.loadItemStackFromNBT(par1NBTTagCompound.getCompoundTag("Potion"));
        }
        else
        {
            this.setPotionDamage(par1NBTTagCompound.getInteger("potionValue"));
        }

        if (this.potionDamage == null)
        {
            this.setDead();
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);

        if (this.potionDamage != null)
        {
            par1NBTTagCompound.setCompoundTag("Potion", this.potionDamage.writeToNBT(new NBTTagCompound()));
        }
    }
}