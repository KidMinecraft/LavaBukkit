package immibis.lavabukkit.nms;

import immibis.lavabukkit.BukkitInventoryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.BlockMycelium;
import net.minecraft.block.BlockNetherStalk;
import net.minecraft.block.BlockRedstoneLight;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockVine;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIArrowAttack;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityFallingSand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.TcpConnection;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet101CloseWindow;
import net.minecraft.network.packet.Packet18Animation;
import net.minecraft.network.packet.Packet19EntityAction;
import net.minecraft.network.packet.Packet255KickDisconnect;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.network.packet.Packet7UseEntity;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.village.Village;
import net.minecraft.village.VillageSiege;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import cpw.mods.fml.relauncher.ReflectionHelper;

public class Hooks {
	public static final Object EXECUTE_NORMALLY = new Object();
	
	public static boolean onMushroomSpread(World world, int toX, int toY, int toZ, int block, BlockMushroom block2, World world2, int fromX, int fromY, int fromZ, Random random) {
		if(world.isRemote) return false;
		
        org.bukkit.World bworld = world.getWorld();
        BlockState blockState = bworld.getBlockAt(toX, toY, toZ).getState();
        blockState.setTypeId(block);

        BlockSpreadEvent event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(fromX, fromY, fromZ), blockState);
        world.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            blockState.update(true);
            return true;
        }
        else
        	return false;
	}
	
	public static boolean onMyceliumSpreadOrFade(World world, int toX, int toY, int toZ, int block, BlockMycelium block2, World world2, int fromX, int fromY, int fromZ, Random random) {
		if(world.isRemote) return false;
		
		org.bukkit.World bworld = world.getWorld();
        BlockState blockState = bworld.getBlockAt(toX, toY, toZ).getState();
        blockState.setTypeId(block);
        
        BlockEvent event;
        
		if(block == Block.dirt.blockID)
            event = new BlockFadeEvent(blockState.getBlock(), blockState);
		else
			event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(fromX, fromY, fromZ), blockState);
		
		world.getServer().getPluginManager().callEvent(event);

        if (!((Cancellable)event).isCancelled()) {
            blockState.update(true);
            return true;
        } else
        	return false;
	}
	
	public static void onNetherwartGrow(World world, int x, int y, int z, int meta, BlockNetherStalk block, World world2, int x2, int y2, int z2, Random random) {
		if(world.isRemote) return;
		
		org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockGrowEvent(world, x, y, z, block.blockID, meta);
	}
	
	public static boolean onRedstoneLampOn1(World world, int x, int y, int z, int newBlock, BlockRedstoneLight oldBlock, World world2, int x2, int y2, int z2) {
		if(world.isRemote) return false;
		
		if (CraftEventFactory.callRedstoneChange(world, x, y, z, 0, 15).getNewCurrent() != 15) {
            return false;
        }
		return world.setBlockWithNotify(x, y, z, newBlock);
	}
	
	public static boolean onRedstoneLampOn2(World world, int x, int y, int z, int newBlock, BlockRedstoneLight oldBlock, World world2, int x2, int y2, int z2, int unused) {
		if(world.isRemote) return false;
		
		if (CraftEventFactory.callRedstoneChange(world, x, y, z, 0, 15).getNewCurrent() != 15) {
            return false;
        }
		return world.setBlockWithNotify(x, y, z, newBlock);
	}
	
	public static boolean onRedstoneLampOff(World world, int x, int y, int z, int newBlock, BlockRedstoneLight oldBlock, World world2, int x2, int y2, int z2, Random random) {
		if(world.isRemote) return false;
		
		if (CraftEventFactory.callRedstoneChange(world, x, y, z, 15, 0).getNewCurrent() != 0) {
            return false;
        }
		return world.setBlockWithNotify(x, y, z, newBlock);
	}
	
	public static Object onRedstoneOreWalking(BlockRedstoneOre block, World world, int x, int y, int z, Entity entity) {
		if(world.isRemote) return false;
		
		if (entity instanceof EntityPlayerMP) {
            org.bukkit.event.player.PlayerInteractEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerInteractEvent((EntityPlayerMP) entity, org.bukkit.event.block.Action.PHYSICAL, x, y, z, -1, null);
            if (event.isCancelled())
            	return null;
        } else if(!world.isRemote) {
            EntityInteractEvent event = new EntityInteractEvent(entity.getBukkitEntity(), world.getWorld().getBlockAt(x, y, z));
            world.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled())
            	return null;
        }
		
		return EXECUTE_NORMALLY;
	}
	
	public static boolean onReedGrow(World world, int x, int y, int z, int newBlock, BlockReed block, World world2, int x2, int y2, int z2, Random random) {
		if(world.isRemote) return false;
		
		org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockGrowEvent(world, x, y, z, newBlock, 0);
		return true;
	}
	
	public static boolean onSnowFade(World world, int x, int y, int z, int newBlock, BlockSnow oldBlock, World world2, int x2, int y2, int z2) {
		if(world.isRemote) return false;
		
		// called when snow is on an invalid block and receives a block update
		
		// event not in craftbukkit, seems to be a bug there?
		org.bukkit.block.Block bblock = world.getWorld().getBlockAt(x, y, z);
		BlockState newState = bblock.getState();
		newState.setTypeId(newBlock);
		newState.setRawData((byte)0);
		BlockFadeEvent event = new BlockFadeEvent(bblock, newState);
		world.getServer().getPluginManager().callEvent(event);
		if(event.isCancelled())
			return false;
		
		// setBlockWithNotify in vanilla; can cause stack overflows
		// this fix is also in CraftBukkit
		world.setBlock(x, y, z, newBlock);
		world.markBlockForUpdate(x, y, z);
		return true;
	}
	
	public static boolean onSnowMelt(World world, int x, int y, int z, int newBlock, BlockSnow block, World world2, int x2, int y2, int z2, Random random) {
        if(world.isRemote) return false;
		
        if (CraftEventFactory.callBlockFadeEvent(world.getWorld().getBlockAt(x, y, z), newBlock).isCancelled())
            return false;
        world.setBlockWithNotify(x, y, z, newBlock);
		return true;
	}
	
	public static boolean onTNTPlacedPowerCheck(World world, int x, int y, int z, BlockTNT block, World world2, int x2, int y2, int z2) {
		if(world.isRemote) return false;
		
		return !world.editingBlocks && world.isBlockIndirectlyGettingPowered(x, y, z);
	}
	
	public static void onCropsGrow(World world, int x, int y, int z, int newMeta, BlockCrops block, World world2, int x2, int y2, int z2, Random random) {
		if(world.isRemote) return;
		
		CraftEventFactory.handleBlockGrowEvent(world2, x, y, z, block.blockID, newMeta);
	}
	
	public static boolean onGrassSpreadOrFade(World world, int newX, int newY, int newZ, int newBlockID, BlockGrass block, World world2, int oldX, int oldY, int oldZ, Random random) {
		if(world.isRemote) return false;
		
		if(newBlockID == Block.dirt.blockID) {
            org.bukkit.World bworld = world.getWorld();
            BlockState blockState = bworld.getBlockAt(newX, newY, newZ).getState();
            blockState.setTypeId(newBlockID);

            BlockFadeEvent event = new BlockFadeEvent(blockState.getBlock(), blockState);
            world.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                blockState.update(true);
            }
            
		} else if(newBlockID == Block.grass.blockID) {
            org.bukkit.World bworld = world.getWorld();
            BlockState blockState = bworld.getBlockAt(newX, newY, newZ).getState();
            blockState.setTypeId(newBlockID);

            BlockSpreadEvent event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(oldX, oldY, oldZ), blockState);
            world.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                blockState.update(true);
            }
            
		} else {
			world.setBlockWithNotify(newX, newY, newZ, newBlockID);
		}
		return true;
	}
	
	public static boolean onIceMelt(World world, int x, int y, int z, int newBlockID, BlockIce block, World world2, int x2, int y2, int z2, Random random) {
        if(world.isRemote) return false;
		
        if(!CraftEventFactory.callBlockFadeEvent(world.getWorld().getBlockAt(x, y, z), newBlockID).isCancelled())
            world.setBlockWithNotify(x, y, z, newBlockID);
        
		return true;
	}
	
	public static Object onLeafDecay(BlockLeaves block, World world, int x, int y, int z) {
        if(world.isRemote) return null;
		
        LeavesDecayEvent event = new LeavesDecayEvent(world.getWorld().getBlockAt(x, y, z));
        world.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled())
            return null;
        else
        	return EXECUTE_NORMALLY;
	}
	
	public static boolean onFarmlandDry(World world, int x, int y, int z, int newBlockID, BlockFarmland block, World world2, int x2, int y2, int z2, Random random) {
        if(world.isRemote) return false;
		
        if(!CraftEventFactory.callBlockFadeEvent(world.getWorld().getBlockAt(x, y, z), newBlockID).isCancelled())
            world.setBlockWithNotify(x, y, z, newBlockID);
        return true;
	}
	
	public static boolean onFarmlandTrampled(World world, int x, int y, int z, int newBlockID, BlockFarmland block, World world2, int x2, int y2, int z2, Entity entity, float fallDist) {
        if(world.isRemote) return false;
		
        org.bukkit.event.Cancellable cancellable;
        if (entity instanceof EntityPlayerMP) {
            cancellable = CraftEventFactory.callPlayerInteractEvent((EntityPlayerMP) entity, org.bukkit.event.block.Action.PHYSICAL, x, y, z, -1, null);
        } else {
            cancellable = new EntityInteractEvent(entity.getBukkitEntity(), world.getWorld().getBlockAt(x, y, z));
            world.getServer().getPluginManager().callEvent((EntityInteractEvent) cancellable);
        }

        if(!cancellable.isCancelled())
        	world.setBlockWithNotify(x, y, z, newBlockID);
        return true;
	}
	
	public static void onStemGrow(World world, int x, int y, int z, int newMeta, BlockStem block, World world2, int x2, int y2, int z2, Random random) {
        if(world.isRemote) return;
		
        CraftEventFactory.handleBlockGrowEvent(world, x, y, z, block.blockID, newMeta);
	}
	
	public static boolean onStemProduce(World world, int x, int y, int z, int newBlockID, BlockStem block, World world2, int x2, int y2, int z2, Random random) {
		if(world.isRemote) return false;
		
		CraftEventFactory.handleBlockGrowEvent(world, x, y, z, newBlockID, 0);
        return true;
	}
	
	public static boolean onVineSpread(World world, int newX, int newY, int newZ, int newBlockID, int newMeta, BlockVine block, World world2, int oldX, int oldY, int oldZ, Random random) {
		if(world.isRemote) return false;
		
		org.bukkit.World bw = world.getWorld();
		CraftEventFactory.handleBlockSpreadEvent(bw.getBlockAt(newX, newY, newZ), bw.getBlockAt(oldX, oldY, oldZ), newBlockID, newMeta);
        return true;
	}
	
	public static boolean onEntityOnFireDamage(Entity ent, DamageSource source, int damage, Entity ent2) {
		if(ent.worldObj.isRemote) return false;
		
		if(ent != ent2 || source != DamageSource.onFire || !(ent instanceof EntityLiving))
			return ent.attackEntityFrom(source, damage);
		
		org.bukkit.event.entity.EntityDamageEvent event = new org.bukkit.event.entity.EntityDamageEvent(ent.getBukkitEntity(), org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK, damage);
		if(event.isCancelled())
			return false;
		
		event.getEntity().setLastDamageCause(event);
		return ent.attackEntityFrom(source, event.getDamage());
	}
	
	public static Object onEntityInLavaDamage(Entity ent) {
		if(ent.worldObj.isRemote) return false;
		
		if(!(ent instanceof EntityLiving) || ent.isImmuneToFire())
			return EXECUTE_NORMALLY;
		
		Server server = ent.worldObj.getServer();

        org.bukkit.block.Block damager = null;
        org.bukkit.entity.Entity damagee = ent.getBukkitEntity();

        EntityDamageByBlockEvent event = new EntityDamageByBlockEvent(damager, damagee, org.bukkit.event.entity.EntityDamageEvent.DamageCause.LAVA, 4);
        server.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            damagee.setLastDamageCause(event);
            ent.attackEntityFrom(DamageSource.lava, event.getDamage());
        }

        if (ent.fire <= 0) {
            // not on fire yet
            EntityCombustEvent combustEvent = new org.bukkit.event.entity.EntityCombustByBlockEvent(damager, damagee, 15);
            server.getPluginManager().callEvent(combustEvent);

            if (!combustEvent.isCancelled()) {
                ent.setFire(combustEvent.getDuration());
            }
        } else {
            // This will be called every single tick the entity is in lava, so don't throw an event
            ent.setFire(15);
        }
		
		return null;
	}
	
	public static void onEntityCombustFromLava(Entity ent, int fireSeconds, Entity ent2, double ignore1, double ignore2, double ignore3) {
		if(ent.worldObj.isRemote) return;
		
		if(ent.fire <= 0 && !ent.worldObj.isRemote) {
			// only throw events on the first combust, otherwise it spams
			EntityCombustEvent event = new EntityCombustEvent(ent.getBukkitEntity(), fireSeconds);
			ent.worldObj.getServer().getPluginManager().callEvent(event);
			if(event.isCancelled())
				return;
			
			fireSeconds = event.getDuration();
		}
		ent.setFire(fireSeconds);
	}
	
	public static Object onEntityInFireDamage(Entity ent, int damage) {
		if(!(ent instanceof EntityLiving) || ent.isImmuneToFire() || ent.worldObj.isRemote)
			return EXECUTE_NORMALLY;
		
		org.bukkit.event.entity.EntityDamageEvent event = new org.bukkit.event.entity.EntityDamageEvent(ent.getBukkitEntity(), org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE, damage);
		ent.worldObj.getServer().getPluginManager().callEvent(event);
		if(event.isCancelled())
			return null;
		
		event.getEntity().setLastDamageCause(event);
		ent.attackEntityFrom(DamageSource.inFire, event.getDamage());
				
		return null;
	}
	
	public static Object onEntitySetWorld(Entity ent, World world) {
		if(world == null) {
			ent.setDead();
			ent.worldObj = DimensionManager.getWorld(0);
		} else
			ent.worldObj = world;
		return null;
	}
	
	public static Object onEntityWriteToNBT(Entity ent, NBTTagCompound tag) {
		tag.setInteger("Bukkit.updateLevel", Entity.CURRENT_LEVEL);
		return EXECUTE_NORMALLY;
	}
	
	public static void onEntityCombustFromLightning(Entity victim, int fireSeconds, Entity victim2, EntityLightningBolt lightning) {
		if(victim.worldObj.isRemote) return;
		
		if(victim.fire <= 0 && victim instanceof EntityLiving) {
            EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(lightning.getBukkitEntity(), victim.getBukkitEntity(), fireSeconds);
            victim.worldObj.getServer().getPluginManager().callEvent(entityCombustEvent);
            if (!entityCombustEvent.isCancelled()) {
                victim.setFire(entityCombustEvent.getDuration());
            }
		} else
			victim.setFire(fireSeconds);
	}
	
	public static Object onEntityTravelToDimension(Entity ent, int newDim) {
		return null; // CraftBukkit - disable entity portal support for now.
	}
	
	public static void onAgeableWriteToNBT(EntityAgeable ent, NBTTagCompound tag) {
		tag.setBoolean("AgeLocked", ent.ageLocked);
	}
	
	public static void onAgeableReadFromNBT(EntityAgeable ent, NBTTagCompound tag) {
		ent.ageLocked = tag.getBoolean("AgeLocked");
	}
	
	public static void onAgeableUpdateGrowingAge(EntityAgeable ent, int newAge, EntityAgeable ent2) {
		if(!ent.ageLocked)
			ent.setGrowingAge(newAge);
	}
	
	public static void onThunderChange(WorldInfo worldInfo, boolean newState, World world) {
		if(world.isRemote) {
			worldInfo.setThundering(newState);
			return;
		}
		
		ThunderChangeEvent thunder = new ThunderChangeEvent(world.getWorld(), newState);
        world.getServer().getPluginManager().callEvent(thunder);
        if (!thunder.isCancelled()) {
            worldInfo.setThundering(newState);
        }
	}
	
	public static void onRainChange(WorldInfo worldInfo, boolean newState, World world) {
		if(world.isRemote) {
			worldInfo.setRaining(newState);
			return;
		}
		
		WeatherChangeEvent weather = new WeatherChangeEvent (world.getWorld(), newState);
        world.getServer().getPluginManager().callEvent(weather);
        if (!weather.isCancelled()) {
            worldInfo.setRaining(newState);
        }
	}
	
	public static boolean doBlockCanBuildEvent(boolean defaultReturn, World world, int blockID, int x, int y, int z, boolean ignore1, int ignore2, Entity ignore3) {
		if(world.isRemote)
			return defaultReturn;
		
		BlockCanBuildEvent event = new BlockCanBuildEvent(world.getWorld().getBlockAt(x, y, z), blockID, defaultReturn);
        world.getServer().getPluginManager().callEvent(event);

        return event.isBuildable();
	}
	
	public static void onEntityCombustByArrow(Entity victim, int fireSeconds, EntityArrow arrow) {
		if(victim.worldObj.isRemote) return;
		
		if (arrow.isBurning() && (!(victim instanceof EntityPlayer) || !(arrow.shootingEntity instanceof EntityPlayer) || arrow.worldObj.pvpMode)) { // CraftBukkit - abide by pvp setting if destination is a player.
            EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(arrow.getBukkitEntity(), victim.getBukkitEntity(), fireSeconds);
            org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);

            if (!combustEvent.isCancelled()) {
                victim.setFire(combustEvent.getDuration());
            }
        }
	}
	
	public static boolean onDragonAttackEntity(Entity victim, DamageSource source, int damage, EntityDragon dragon, List ignore) {
		if(victim.worldObj.isRemote) return false;
		
		// The EntityHuman case is handled in EntityHuman, so don't throw it here
        if (!(victim instanceof EntityPlayer)) {
            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(dragon.getBukkitEntity(), victim.getBukkitEntity(), org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
            dragon.worldObj.getServer().getPluginManager().callEvent(damageEvent);

            if (!damageEvent.isCancelled()) {
                victim.getBukkitEntity().setLastDamageCause(damageEvent);
                return victim.attackEntityFrom(source, damageEvent.getDamage());
            } else
            	return false;
        } else {
            return victim.attackEntityFrom(source, damage);
        }
	}
	
	public static Object betterXPSplit(int par0) {
		if (par0 > 162670129) return par0 - 100000;
        if (par0 > 81335063) return 81335063;
        if (par0 > 40667527) return 40667527;
        if (par0 > 20333759) return 20333759;
        if (par0 > 10166857) return 10166857;
        if (par0 > 5083423) return 5083423;
        if (par0 > 2541701) return 2541701;
        if (par0 > 1270849) return 1270849;
        if (par0 > 635413) return 635413;
        if (par0 > 317701) return 317701;
        if (par0 > 158849) return 158849;
        if (par0 > 79423) return 79423;
        if (par0 > 39709) return 39709;
        if (par0 > 19853) return 19853;
        if (par0 > 9923) return 9923;
        if (par0 > 4957) return 4957;
        return EXECUTE_NORMALLY;
	}
	
	public static boolean onAnvilAttackEntity(Entity victim, DamageSource source, int damage, EntityFallingSand anvil, float fallHeight) {
		if(victim.worldObj.isRemote) return false;
		
		EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(anvil, victim, EntityDamageEvent.DamageCause.FALLING_BLOCK, damage);
        if (event.isCancelled()) {
            return false;
        }

        return victim.attackEntityFrom(source, event.getDamage());
	}
	
	public static void onSetPlayerBedLocation(EntityPlayer player, ChunkCoordinates newLoc, boolean force) {
		if(newLoc == null || player.worldObj.isRemote)
			player.spawnWorld = "";
		else
			player.spawnWorld = player.worldObj.getWorld().getName();
	}
	
	public static Object dontMergeItemsWithEnchantments(EntityItem e1, EntityItem e2) {
		if(e1.getItem().stackTagCompound != null && !e1.getItem().stackTagCompound.hasNoTags())
			return false;
		if(e2.getItem().stackTagCompound != null && !e2.getItem().stackTagCompound.hasNoTags())
			return false;
		return EXECUTE_NORMALLY;
	}
	
	public static Explosion onLargeFireballExplosion(World world, Entity ignore, double expX, double expY, double expZ, float expPower, boolean setFire, boolean breakBlocks, EntityLargeFireball fireball, MovingObjectPosition impactPos) {
        if(world.isRemote) return null;
		
		ExplosionPrimeEvent event = new ExplosionPrimeEvent((org.bukkit.entity.Explosive) fireball.getBukkitEntity());
        world.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            // give 'this' instead of (Entity) null so we know what causes the damage
            return world.newExplosion(fireball, expX, expY, expZ, event.getRadius(), event.getFire(), breakBlocks);
        } else
        	return null;
	}
	
	public static boolean onBlockIgniteByLightning(World world, int x, int y, int z, int newBlockID, EntityLightningBolt bolt, World world2, double b_x, double b_y, double b_z, boolean isEffect) {
		if(world.isRemote) return false;
		
		if(bolt.isEffect)
			return false;
		if(newBlockID == Block.fire.blockID) {
			BlockIgniteEvent event = new BlockIgniteEvent(bolt.worldObj.getWorld().getBlockAt(x, y, z), BlockIgniteEvent.IgniteCause.LIGHTNING, null);
            world.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return false;
            }
		}
		return world.setBlockWithNotify(x, y, z, newBlockID);
	}
	
	public static boolean onBlockIgniteByLightningUpdate(World world, int x, int y, int z, int newBlockID, EntityLightningBolt bolt) {
		if(world.isRemote) return false;
		
		if(bolt.isEffect)
			return false;
		if(newBlockID == Block.fire.blockID) {
			BlockIgniteEvent event = new BlockIgniteEvent(bolt.worldObj.getWorld().getBlockAt(x, y, z), BlockIgniteEvent.IgniteCause.LIGHTNING, null);
            world.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return false;
            }
		}
		return world.setBlockWithNotify(x, y, z, newBlockID);
	}
	
	public static void onLightningStrikeEntity(Entity victim, EntityLightningBolt bolt, EntityLightningBolt bolt2) {
		if(!bolt.isEffect)
			victim.onStruckByLightning(bolt);
	}
	
	public static boolean dontSuffocateEnderDragons(Entity ent, EntityLiving ent2) {
		return !(ent instanceof EnderDragon) && ent.isEntityInsideOpaqueBlock();
	}
	
	public static boolean onLivingUpdateDamage(Entity ent, DamageSource source, int damage, EntityLiving ent2) {
		if(ent.worldObj.isRemote) return false; // LavaBukkit bugfix
		
		if(source == DamageSource.inWall) {
			EntityDamageEvent event = new EntityDamageEvent(ent.getBukkitEntity(), EntityDamageEvent.DamageCause.SUFFOCATION, damage);
            ent.worldObj.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                event.getEntity().setLastDamageCause(event);
                return ent.attackEntityFrom(source, event.getDamage());
            }
            return false;
            
		} else if(source == DamageSource.drown) {
			EntityDamageEvent event = new EntityDamageEvent(ent.getBukkitEntity(), EntityDamageEvent.DamageCause.DROWNING, damage);
            ent.worldObj.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                event.getEntity().setLastDamageCause(event);
                return ent.attackEntityFrom(source, event.getDamage());
            }
            return false;
            
		} else
			return ent.attackEntityFrom(source, damage);
	}
	
	public static void onlySetAirIfNecessary(EntityLiving ent, int newAir, EntityLiving ent2) {
		if(ent.getAir() != newAir)
			ent.setAir(newAir);
	}
	
	public static boolean onLivingVoidDamage(Entity ent, DamageSource source, int amt, EntityLiving ent2) {
        if(ent.worldObj.isRemote) return false;
		
		EntityDamageByBlockEvent event = new EntityDamageByBlockEvent(null, ent.getBukkitEntity(), EntityDamageEvent.DamageCause.VOID, amt);
        ent.worldObj.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled() || event.getDamage() == 0) {
            return false;
        }

        event.getEntity().setLastDamageCause(event);
        return ent.attackEntityFrom(source, event.getDamage());
	}
	
	public static Object beforeShearEntity(Entity ent, ItemStack stack, World world, int x, int y, int z, int fortune) {
		if(world.isRemote) return false;
		
		for(EntityPlayer ply : (List<EntityPlayer>)world.playerEntities) {
			if(ply.inventory.getCurrentItem() == stack && ply.getBukkitEntity() instanceof Player) {
				// CraftBukkit start
	            PlayerShearEntityEvent event = new PlayerShearEntityEvent((org.bukkit.entity.Player) ply.getBukkitEntity(), ent.getBukkitEntity());
	            world.getServer().getPluginManager().callEvent(event);
	
	            if (event.isCancelled()) {
	                return new ArrayList<ItemStack>();
	            }
	            // CraftBukkit end
			}
		}
		return EXECUTE_NORMALLY;
	}
	
	public static void onSheepRegrow(EntitySheep ent, boolean ignore, EntitySheep ent2) {
        if(ent.worldObj.isRemote) return;
		
		SheepRegrowWoolEvent event = new SheepRegrowWoolEvent((org.bukkit.entity.Sheep) ent.getBukkitEntity());
        ent.worldObj.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            ent.setSheared(false);
        }
	}
	
	public static void onUndeadCombust(Entity ent, int fireSeconds, Entity ent2) {
        if(ent.worldObj.isRemote) return;
		
		EntityCombustEvent event = new EntityCombustEvent(ent.getBukkitEntity(), fireSeconds);
        ent.worldObj.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            ent.setFire(event.getDuration());
        }
	}
	
	public static void onEntityCombustByFireball(Entity victim, int fireSeconds, Entity fireball, MovingObjectPosition rayTraceResult) {
		if(victim.worldObj.isRemote) return;
		
		EntityCombustByEntityEvent event = new EntityCombustByEntityEvent((org.bukkit.entity.Projectile) fireball.getBukkitEntity(), victim.getBukkitEntity(), fireSeconds);
        victim.worldObj.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            victim.setFire(event.getDuration());
        }
	}
	
	public static boolean onBlockCombustByFireball(World world, int x, int y, int z, int newBlockID, Entity fireball, MovingObjectPosition rayTraceResult) {
		if(world.isRemote) return false;
		
		if(newBlockID != Block.fire.blockID)
			return world.setBlockWithNotify(x, y, z, newBlockID);
		
		org.bukkit.block.Block block = world.getWorld().getBlockAt(x, y, z);
        BlockIgniteEvent event = new BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.FIREBALL, null);
        world.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            return world.setBlockWithNotify(x, y, z, newBlockID);
        }
        return false;
	}
	
	public static boolean onSnowmanEnvironmentalDamage(Entity victim, DamageSource damageSource, int damage, EntitySnowman snowman) {
		if(victim != snowman)
			return victim.attackEntityFrom(damageSource, damage);
		
		if(victim.worldObj.isRemote) return false;
		
		if(damageSource == DamageSource.drown) {
			EntityDamageEvent event = new EntityDamageEvent(snowman.getBukkitEntity(), EntityDamageEvent.DamageCause.DROWNING, damage);
            snowman.worldObj.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                event.getEntity().setLastDamageCause(event);
                return snowman.attackEntityFrom(damageSource, event.getDamage());
            }
            return false;
		}
		
		if(damageSource == DamageSource.onFire) {
			EntityDamageEvent event = new EntityDamageEvent(snowman.getBukkitEntity(), EntityDamageEvent.DamageCause.MELTING, damage);
            snowman.worldObj.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                event.getEntity().setLastDamageCause(event);
                return snowman.attackEntityFrom(damageSource, event.getDamage());
            }
            return false;
		}
		
		return snowman.attackEntityFrom(damageSource, damage);
	}
	
	public static boolean onSnowmanPlaceTrail(World world, int x, int y, int z, int newBlockID, EntitySnowman snowman) {
		
		if(world.isRemote)
			return false;
		
		org.bukkit.block.BlockState blockState = world.getWorld().getBlockAt(x, y, z).getState();
        blockState.setTypeId(newBlockID);

        EntityBlockFormEvent event = new EntityBlockFormEvent(snowman.getBukkitEntity(), blockState.getBlock(), blockState);
        world.getServer().getPluginManager().callEvent(event);

        if(!event.isCancelled()) {
            blockState.update(true);
            return true;
        }
        
        return false;
	}
	
	public static Explosion onWitherExplode(World world, Entity centre, double expX, double expY, double expZ, float power, boolean fire, boolean blocks, EntityWither wither) {
		if(world.isRemote)
			return null;
		
		ExplosionPrimeEvent event = new ExplosionPrimeEvent(wither.getBukkitEntity(), power, fire);
        world.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
        	return world.newExplosion(centre, expX, expY, expZ, event.getRadius(), event.getFire(), blocks);
        }
        return null;
	}
	
	public static void onWitherHealFromSkullImpact(EntityLiving wither, int healAmount, EntityWitherSkull skull, MovingObjectPosition rayTraceResult) {
		wither.heal(healAmount, EntityRegainHealthEvent.RegainReason.WITHER);
	}
	
	public static Explosion onWitherSkullExplode(World world, Entity centre, double expX, double expY, double expZ, float power, boolean fire, boolean blocks, EntityWitherSkull skull, MovingObjectPosition rayTraceResult) {
		if(world.isRemote)
			return null;
		
		ExplosionPrimeEvent event = new ExplosionPrimeEvent(skull.getBukkitEntity(), 1.0F, false);
        world.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
        	return world.newExplosion(centre, expX, expY, expZ, event.getRadius(), event.getFire(), blocks);
        }
        return null;
	}
	
	public static void onFullHungerHeal(EntityLiving player, int healAmount, FoodStats foodStats, EntityPlayer player2) {
		player.heal(healAmount, RegainReason.SATIATED);
	}
	public static boolean onPlayerStarve(Entity player, DamageSource source, int damage, FoodStats foodStats, EntityPlayer player2) {
		if(source != DamageSource.starve)
			return player.attackEntityFrom(source, damage);
		
		if(player.worldObj.isRemote)
			return false;
		
		EntityDamageEvent event = new EntityDamageEvent(player.getBukkitEntity(), EntityDamageEvent.DamageCause.STARVATION, damage);
        player.worldObj.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            event.getEntity().setLastDamageCause(event);
            return player.attackEntityFrom(source, event.getDamage());
        }
        return false;
	}
	
	public static Object getPlugins(MinecraftServer ms) {
		CraftServer server = ms.server;
		
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();

        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && server.getQueryPlugins()) {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
	}
	
	public static Object executeCommandThreadSafely(final MinecraftServer ms, final String command) {
		if(ms.server.isPrimaryThread())
			return EXECUTE_NORMALLY;
		
		Waitable<String> waitable = new Waitable<String>() {
            @Override
            protected String evaluate() {
            	RConConsoleSource.consoleBuffer.resetLog();
			    
            	// Event changes start
                RemoteServerCommandEvent event = new RemoteServerCommandEvent(ms.remoteConsole, command);
                ms.server.getPluginManager().callEvent(event);
                // Event changes end
                
                ServerCommand servercommand = new ServerCommand(event.getCommand(), RConConsoleSource.consoleBuffer);
                ms.server.dispatchServerCommand(ms.remoteConsole, servercommand); // CraftBukkit
                
                return RConConsoleSource.consoleBuffer.getChatBuffer();
            }};
        ms.processQueue.add(waitable);
        try {
            return waitable.get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Exception processing rcon command " + command, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Maintain interrupted state
            throw new RuntimeException("Interrupted processing rcon command " + command, e);
        }
	}
	
	public static Object getServerModName(MinecraftServer ms) {
		return "lavabukkit,craftbukkit,fml,forge";
	}
	
	public static Object getPossibleTabCompletions(MinecraftServer ms, ICommandSender sender, String command) {
		return ms.server.tabComplete(sender, command);
	}
	
	public static void onPotionRegen(EntityLiving ent, int healAmount, Potion potionEffect, EntityLiving ent2, int potionStrength) {
		ent.heal(healAmount, RegainReason.MAGIC_REGEN);
	}
	
	public static boolean onPotionDamage(EntityLiving ent, DamageSource source, int damageAmt, Potion potionEffect, EntityLiving ent2, int potionStrength) {
		EntityDamageEvent.DamageCause cause = null;
		
		if(ent.worldObj.isRemote)
			return false;
		
		if(potionEffect == Potion.poison)
			cause = EntityDamageEvent.DamageCause.POISON;
		else if(source == DamageSource.wither)
			cause = EntityDamageEvent.DamageCause.WITHER;
		else
			cause = EntityDamageEvent.DamageCause.MAGIC;
		
        EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(null, ent, cause, 1);

        if (!event.isCancelled() && event.getDamage() > 0) {
            return ent.attackEntityFrom(source, event.getDamage());
        } else
        	return false;
	}
	
	public static Object onPlayerKick(NetServerHandler handler, String message) {
		MinecraftServer mcServer = MinecraftServer.getServer();
		if (!handler.connectionClosed)
        {
			// CraftBukkit start
            String leaveMessage = "\u00A7e" + handler.playerEntity.username + " left the game.";

            PlayerKickEvent event = new PlayerKickEvent(mcServer.server.getPlayer(handler.playerEntity), message, leaveMessage);

            if (mcServer.isServerRunning()) {
                mcServer.server.getPluginManager().callEvent(event);
            }

            if (event.isCancelled()) {
                // Do not kick the player
                return null;
            }
            // Send the possibly modified leave message
            message = event.getReason();
            // CraftBukkit end
            
            handler.playerEntity.mountEntityAndWakeUp();
            handler.sendPacketToPlayer(new Packet255KickDisconnect(message));
            handler.netManager.serverShutdown();
            
            // CraftBukkit start
            leaveMessage = event.getLeaveMessage();
            if (leaveMessage != null && leaveMessage.length() > 0) {
                mcServer.getConfigurationManager().sendPacketToAllPlayers(new Packet3Chat(leaveMessage));
            }
            handler.playerEntity.getBukkitEntity().disconnect(message);
            // CraftBukkit end
            
            mcServer.getConfigurationManager().playerLoggedOut(handler.playerEntity);
            handler.connectionClosed = true;
        }
		return null;
	}
	
	public static void threadSafelyKickPlayer(final NetServerHandler handler, final String reason, NetServerHandler handler2, Packet3Chat ignore) {
		if(Bukkit.getServer().isPrimaryThread()) {
			handler.kickPlayerFromServer(reason);
		} else {
			Waitable waitable = new Waitable() {
                @Override
                protected Object evaluate() {
                    handler.kickPlayerFromServer(reason);
                    return null;
                }
            };

            MinecraftServer.getServer().processQueue.add(waitable);

            try {
                waitable.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
		}
	}
	
	public static Object processBukkitCommand(NetServerHandler handler, String command) {
		CraftPlayer player = handler.getPlayer().getBukkitEntity();

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, command, new LazyPlayerSet());
        player.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return null;
        }

        try {
            NetServerHandler.logger.info(event.getPlayer().getName() + " issued server command: " + event.getMessage());
            if (player.getServer().dispatchCommand(event.getPlayer(), event.getMessage().substring(1))) {
                return null;
            }
        } catch (org.bukkit.command.CommandException ex) {
            player.sendMessage(org.bukkit.ChatColor.RED + "An internal error occurred while attempting to perform this command");
            Logger.getLogger(NetServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
		return null;
	}
	
	public static void onSwingItem(EntityLiving player, NetServerHandler handler, Packet18Animation packet) {
		// CraftBukkit start - raytrace to look for 'rogue armswings'
		if(player.isDead) return;
		
        float f = 1.0F;
        float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double) f;
        double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double) f + 1.62D - (double) player.height;
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double) f;
        Vec3 vec3d = player.worldObj.getWorldVec3Pool().getVecFromPool(d0, d1, d2);

        float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
        float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
        float f5 = -MathHelper.cos(-f1 * 0.017453292F);
        float f6 = MathHelper.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0D;
        Vec3 vec3d1 = vec3d.addVector((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
        MovingObjectPosition movingobjectposition = player.worldObj.rayTraceBlocks_do(vec3d, vec3d1, true);

        if (movingobjectposition == null || movingobjectposition.typeOfHit != EnumMovingObjectType.TILE) {
            CraftEventFactory.callPlayerInteractEvent(handler.playerEntity, org.bukkit.event.block.Action.LEFT_CLICK_AIR, handler.playerEntity.inventory.getCurrentItem());
        }

        // Arm swing animation
        PlayerAnimationEvent event = new PlayerAnimationEvent(handler.playerEntity.getBukkitEntity());
        player.worldObj.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        // CraftBukkit end
        
        player.swingItem();
	}
	
	public static Object onEntityActionPacket(NetServerHandler handler, Packet19EntityAction packet19entityaction) {
		if (handler.playerEntity.isDead) return null;
		
		CraftServer server = (CraftServer)Bukkit.getServer();

        if (packet19entityaction.state == 1 || packet19entityaction.state == 2) {
            PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(handler.getPlayer().getBukkitEntity(), packet19entityaction.state == 1);
            server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }
        }

        if (packet19entityaction.state == 4 || packet19entityaction.state == 5) {
            PlayerToggleSprintEvent event = new PlayerToggleSprintEvent(handler.getPlayer().getBukkitEntity(), packet19entityaction.state == 4);
            server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }
        }
        
        return EXECUTE_NORMALLY;
	}
	
	public static Object onPlayerDisconnect(NetServerHandler handler, Packet255KickDisconnect packet) {
		handler.getPlayer().getBukkitEntity().disconnect("disconnect.quitting");
		return EXECUTE_NORMALLY;
	}
	
	public static boolean onPlayerRightClickEntity(EntityPlayer player, Entity entity, NetServerHandler handler, Packet7UseEntity packet) {
		ItemStack itemInHand = player.inventory.getCurrentItem();
		
		PlayerInteractEntityEvent event = new PlayerInteractEntityEvent((Player) player.getBukkitEntity(), entity.getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }
        
        player.interactWith(entity);
        
        if (itemInHand != null && itemInHand.stackSize <= -1) {
        	handler.playerEntity.sendContainerToPlayer(handler.playerEntity.openContainer);
        }
        return true;
	}
	
	public static void onPlayerLeftClickEntity(EntityPlayer player, Entity entity, NetServerHandler handler, Packet7UseEntity packet) {
		
		if(entity instanceof EntityXPOrb || entity instanceof EntityItem || entity instanceof EntityArrow) {
			String type = entity.getClass().getSimpleName();
			if(entity instanceof EntityXPOrb)
				type = "XP orb";
			else if(entity instanceof EntityItem)
				type = "item";
			else if(entity instanceof EntityArrow)
				type = "arrow";
            handler.kickPlayerFromServer("Attacking a " + type + " is not permitted");
            System.out.println("Player " + player.username + " tried to attack an " + type + ", so I have disconnected them for exploiting.");
            return;
		}
		ItemStack itemInHand = player.inventory.getCurrentItem();
		
		player.attackTargetEntityWithCurrentItem(entity);
        
        if (itemInHand != null && itemInHand.stackSize <= -1) {
        	handler.playerEntity.sendContainerToPlayer(handler.playerEntity.openContainer);
        }
	}
	
	public static Object onPlayerCloseWindow(NetServerHandler handler, Packet101CloseWindow packet) {
		if(handler.playerEntity.isDead)
			return null;
		
		InventoryCloseEvent event = new InventoryCloseEvent(BukkitInventoryHelper.getBukkitView(handler.playerEntity.openContainer));
        handler.playerEntity.worldObj.getServer().getPluginManager().callEvent(event);
        
        return EXECUTE_NORMALLY;
	}
	
	public static Object checkPlayerDead(NetServerHandler handler, Packet packet) {
		if(handler.playerEntity.isDead)
			return null;
		else
			return EXECUTE_NORMALLY;
	}
	
	public static void checkNetHandlerDisconnected(Packet p, NetHandler handler, TcpConnection connection) {
		if(handler instanceof NetLoginHandler) {
			if(((NetLoginHandler)handler).connectionComplete)
				return;
		} else if(handler instanceof NetServerHandler) {
			if(((NetServerHandler)handler).connectionClosed)
				return;
		}
		p.processPacket(handler);
	}
	
	public static void dontSpamConsoleOnUnexpectedDisconnect(Throwable exception, Object thread) {
	}
	
	public static Object onArrowAttackTaskReset(EntityAIArrowAttack ai) {
		EntityLiving target = ReflectionHelper.getPrivateValue(EntityAIArrowAttack.class, ai, 2);
		EntityLiving host = ReflectionHelper.getPrivateValue(EntityAIArrowAttack.class, ai, 0);
		EntityTargetEvent.TargetReason reason = target.isEntityAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;
        org.bukkit.craftbukkit.event.CraftEventFactory.callEntityTargetEvent(host, null, reason);
        return EXECUTE_NORMALLY;
	}
	
	public static boolean onEntitySpawnBaby(World world, Entity baby, EntityAIMate ai) {
		return world.spawnEntityInWorld(baby, SpawnReason.BREEDING);
	}
	
	public static Object onMeleeAttackTaskReset(EntityAIAttackOnCollide ai) {
		EntityLiving target = ReflectionHelper.getPrivateValue(EntityAIAttackOnCollide.class, ai, 2);
		EntityLiving host = ReflectionHelper.getPrivateValue(EntityAIAttackOnCollide.class, ai, 1);
		EntityTargetEvent.TargetReason reason = target.isEntityAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;
        org.bukkit.craftbukkit.event.CraftEventFactory.callEntityTargetEvent(host, null, reason);
        return EXECUTE_NORMALLY;
	}
	
	public static Object detectListNameConflict(ServerConfigurationManager scm, EntityPlayerMP player) {
		scm.cserver.detectListNameConflict(player);
		return EXECUTE_NORMALLY;
	}
	
	public static void recalcPermissionsOnOperatorChange(ServerConfigurationManager scm, String username) {
		Player player = scm.cserver.getPlayer(username);
        if (player != null) {
            player.recalculatePermissions();
        }
	}
	
	public static void useCustomShutdownMessage(NetServerHandler handler, String defaultMessage, ServerConfigurationManager scm) {
		handler.kickPlayerFromServer(scm.cserver.getShutdownMessage());
	}
	
	public static int callFurnaceBurnEvent(ItemStack fuel, TileEntityFurnace furnace) {
		if(furnace.worldObj.isRemote)
			return TileEntityFurnace.getItemBurnTime(fuel);
		
		org.bukkit.block.Block block = furnace.worldObj.getWorld().getBlockAt(furnace.xCoord, furnace.yCoord, furnace.zCoord);
		FurnaceBurnEvent furnaceBurnEvent = new FurnaceBurnEvent(block, CraftItemStack.asCraftMirror(fuel), TileEntityFurnace.getItemBurnTime(fuel));
	    furnace.worldObj.getServer().getPluginManager().callEvent(furnaceBurnEvent);
	
	    return furnaceBurnEvent.isCancelled() ? 0 : furnaceBurnEvent.getBurnTime();
	}
	
	public static ItemStack callFurnaceSmeltEvent(FurnaceRecipes recipes, ItemStack input, TileEntityFurnace furnace) {
		if(furnace.worldObj.isRemote)
			return recipes.getSmeltingResult(input);
		
		ItemStack defaultOutput = recipes.getSmeltingResult(input);
		
		CraftItemStack source = CraftItemStack.asCraftMirror(input);
        CraftItemStack result = CraftItemStack.asCraftMirror(defaultOutput);

        FurnaceSmeltEvent furnaceSmeltEvent = new FurnaceSmeltEvent(furnace.worldObj.getWorld().getBlockAt(furnace.xCoord, furnace.yCoord, furnace.zCoord), source, result);
        furnace.worldObj.getServer().getPluginManager().callEvent(furnaceSmeltEvent);

        if (furnaceSmeltEvent.isCancelled()) {
        	// furnace always decrements input stack size, so compensate for that
        	input.stackSize++;
            return null;
        }

        return CraftItemStack.asNMSCopy(furnaceSmeltEvent.getResult());
	}
	
	public static boolean useMobSpawnerSpawnReason(World world, Entity spawnedEntity, TileEntityMobSpawner spawner) {
		return world.spawnEntityInWorld(spawnedEntity, SpawnReason.SPAWNER);
	}
	
	public static void callNotePlayEvent(World world, int x, int y, int z, int blockID, int instrument, int note, TileEntityNote tile, World world2, int x2, int y2, int z2) {
		if(world.isRemote) return;
		org.bukkit.event.block.NotePlayEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callNotePlayEvent(world, x, y, z, (byte)instrument, (byte)note);
        if (!event.isCancelled()) {
            world.addBlockEvent(x, y, z, blockID, event.getInstrument().getType(), event.getNote().getId());
        }
	}
	
	public static boolean useVillageDefenseSpawnReason(World world, Entity spawnedEntity, Village village, int ticks) {
		return world.spawnEntityInWorld(spawnedEntity, SpawnReason.VILLAGE_DEFENSE);
	}
	
	public static boolean useVillageInvasionSpawnReason(World world, Entity spawnedEntity, VillageSiege siege) {
		return world.spawnEntityInWorld(spawnedEntity, SpawnReason.VILLAGE_INVASION);
	}
	
	public static Object resetRainAndThunder(WorldServer world) {
		WeatherChangeEvent weather = new WeatherChangeEvent(world.getWorld(), false);
        world.getServer().getPluginManager().callEvent(weather);

        ThunderChangeEvent thunder = new ThunderChangeEvent(world.getWorld(), false);
        world.getServer().getPluginManager().callEvent(thunder);
        if (!weather.isCancelled()) {
            world.getWorldInfo().setRainTime(0);
            world.getWorldInfo().setRaining(false);
        }
        if (!thunder.isCancelled()) {
            world.getWorldInfo().setThunderTime(0);
            world.getWorldInfo().setThundering(false);
        }
		return null;
	}
	
	public static Object callLightningStrikeEvent(WorldServer world, Entity bolt) {
		LightningStrikeEvent lightning = new LightningStrikeEvent(world.getWorld(), (org.bukkit.entity.LightningStrike) bolt.getBukkitEntity());
        world.getServer().getPluginManager().callEvent(lightning);

        return lightning.isCancelled() ? null : EXECUTE_NORMALLY;
	}
}
