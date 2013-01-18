package org.bukkit.craftbukkit.entity;

import net.minecraft.entity.monster.EntityZombie;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;

public class CraftZombie extends CraftMonster implements Zombie {

    public CraftZombie(CraftServer server, EntityZombie entity) {
        super(server, entity);
    }

    @Override
    public EntityZombie getHandle() {
        return (EntityZombie) entity;
    }

    @Override
    public String toString() {
        return "CraftZombie";
    }

    public EntityType getType() {
        return EntityType.ZOMBIE;
    }

    public boolean isBaby() {
        return getHandle().isChild();
    }

    public void setBaby(boolean flag) {
        getHandle().setChild(flag);
    }

    public boolean isVillager() {
        return getHandle().isVillager();
    }

    public void setVillager(boolean flag) {
        getHandle().setVillager(flag);
    }
}
