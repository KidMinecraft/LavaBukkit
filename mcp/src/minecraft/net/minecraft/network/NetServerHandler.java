package net.minecraft.network;

import immibis.lavabukkit.BukkitInventoryHelper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;

import cpw.mods.fml.common.network.FMLNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBeacon;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet0KeepAlive;
import net.minecraft.network.packet.Packet101CloseWindow;
import net.minecraft.network.packet.Packet102WindowClick;
import net.minecraft.network.packet.Packet103SetSlot;
import net.minecraft.network.packet.Packet106Transaction;
import net.minecraft.network.packet.Packet107CreativeSetSlot;
import net.minecraft.network.packet.Packet108EnchantItem;
import net.minecraft.network.packet.Packet10Flying;
import net.minecraft.network.packet.Packet130UpdateSign;
import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.network.packet.Packet13PlayerLookMove;
import net.minecraft.network.packet.Packet14BlockDig;
import net.minecraft.network.packet.Packet15Place;
import net.minecraft.network.packet.Packet16BlockItemSwitch;
import net.minecraft.network.packet.Packet18Animation;
import net.minecraft.network.packet.Packet19EntityAction;
import net.minecraft.network.packet.Packet202PlayerAbilities;
import net.minecraft.network.packet.Packet203AutoComplete;
import net.minecraft.network.packet.Packet204ClientInfo;
import net.minecraft.network.packet.Packet205ClientCommand;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet255KickDisconnect;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.network.packet.Packet53BlockChange;
import net.minecraft.network.packet.Packet6SpawnPosition;
import net.minecraft.network.packet.Packet7UseEntity;
import net.minecraft.network.packet.Packet9Respawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.BanEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class NetServerHandler extends NetHandler
{
    /** The logging system. */
    public static Logger logger = Logger.getLogger("Minecraft");

    /** The underlying network manager for this server handler. */
    public INetworkManager netManager;

    /** This is set to true whenever a player disconnects from the server. */
    public boolean connectionClosed = false;

    /** Reference to the MinecraftServer object. */
    private MinecraftServer mcServer;

    /** Reference to the EntityPlayerMP object. */
    public EntityPlayerMP playerEntity;

    /** incremented each tick */
    private int currentTicks;

    /**
     * player is kicked if they float for over 80 ticks without flying enabled
     */
    public int ticksForFloatKick;
    private boolean field_72584_h;
    private int keepAliveRandomID;
    private long keepAliveTimeSent;
    private static Random randomGenerator = new Random();
    private long ticksOfLastKeepAlive;
    private int chatSpamThresholdCount = 0;
    private int creativeItemCreationSpamThresholdTally = 0;

    /** The last known x position for this connection. */
    private double lastPosX;

    /** The last known y position for this connection. */
    private double lastPosY;

    /** The last known z position for this connection. */
    private double lastPosZ;

    /** is true when the player has moved since his last movement packet */
    public boolean hasMoved = true; // CraftBukkit - private -> public. CB name: checkMovement
    private IntHashMap field_72586_s = new IntHashMap();

    public NetServerHandler(MinecraftServer par1, INetworkManager par2, EntityPlayerMP par3)
    {
        this.mcServer = par1;
        this.netManager = par2;
        par2.setNetHandler(this);
        this.playerEntity = par3;
        par3.playerNetServerHandler = this;
    }
    
    // CraftBukkit start
    private static final int PLACE_DISTANCE_SQUARED = 6 * 6;

    // Get position of last block hit for BlockDamageLevel.STOPPED
    private double cblastPosX = Double.MAX_VALUE;
    private double cblastPosY = Double.MAX_VALUE;
    private double cblastPosZ = Double.MAX_VALUE;
    private float cblastPitch = Float.MAX_VALUE;
    private float cblastYaw = Float.MAX_VALUE;
    private boolean justTeleported = false;

    // For the packet15 hack :(
    Long lastPacket;

    // Store the last block right clicked and what type it was
    private int lastMaterial;

    private final static HashSet<Integer> invalidItems = new HashSet<Integer>(java.util.Arrays.asList(8, 9, 10, 11, 26, 34, 36, 43, 51, 52, 55, 59, 60, 62, 63, 64, 68, 71, 74, 75, 83, 90, 92, 93, 94, 95, 104, 105, 115, 117, 118, 119, 125, 127, 132, 137, 140, 141, 142, 144)); // TODO: Check after every update.
    // CraftBukkit end

    /**
     * run once each game tick
     */
    public void networkTick()
    {
        this.field_72584_h = false;
        ++this.currentTicks;
        this.mcServer.theProfiler.startSection("packetflow");
        this.netManager.processReadPackets();
        this.mcServer.theProfiler.endStartSection("keepAlive");

        if ((long)this.currentTicks - this.ticksOfLastKeepAlive > 20L)
        {
            this.ticksOfLastKeepAlive = (long)this.currentTicks;
            this.keepAliveTimeSent = System.nanoTime() / 1000000L;
            this.keepAliveRandomID = randomGenerator.nextInt();
            this.sendPacketToPlayer(new Packet0KeepAlive(this.keepAliveRandomID));
        }

        if (this.chatSpamThresholdCount > 0)
        {
            --this.chatSpamThresholdCount;
        }

        if (this.creativeItemCreationSpamThresholdTally > 0)
        {
            --this.creativeItemCreationSpamThresholdTally;
        }

        this.mcServer.theProfiler.endStartSection("playerTick");
        this.mcServer.theProfiler.endSection();
    }

    public void kickPlayerFromServer(String par1Str)
    {
        if (!this.connectionClosed)
        {
            this.playerEntity.mountEntityAndWakeUp();
            this.sendPacketToPlayer(new Packet255KickDisconnect(par1Str));
            this.netManager.serverShutdown();
            this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new Packet3Chat("\u00a7e" + this.playerEntity.username + " left the game."));
            this.mcServer.getConfigurationManager().playerLoggedOut(this.playerEntity);
            this.connectionClosed = true;
        }
    }

    public void handleFlying(Packet10Flying par1Packet10Flying)
    {
        WorldServer var2 = this.mcServer.worldServerForDimension(this.playerEntity.dimension);
        this.field_72584_h = true;

        if (!this.playerEntity.playerConqueredTheEnd)
        {
            double var3;

            if (!this.hasMoved)
            {
                var3 = par1Packet10Flying.yPosition - this.lastPosY;

                if (par1Packet10Flying.xPosition == this.lastPosX && var3 * var3 < 0.01D && par1Packet10Flying.zPosition == this.lastPosZ)
                {
                    this.hasMoved = true;
                }
            }
            
            // CraftBukkit start
            Player player = this.playerEntity.getBukkitEntity();
            Location from = new Location(player.getWorld(), cblastPosX, cblastPosY, cblastPosZ, cblastYaw, cblastPitch); // Get the Players previous Event location.
            Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

            // If the packet contains movement information then we update the To location with the correct XYZ.
            if (par1Packet10Flying.moving && !(par1Packet10Flying.moving && par1Packet10Flying.yPosition == -999.0D && par1Packet10Flying.stance == -999.0D)) {
                to.setX(par1Packet10Flying.xPosition);
                to.setY(par1Packet10Flying.yPosition);
                to.setZ(par1Packet10Flying.zPosition);
            }

            // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
            if (par1Packet10Flying.rotating) {
                to.setYaw(par1Packet10Flying.yaw);
                to.setPitch(par1Packet10Flying.pitch);
            }
            
            // Prevent 40 event-calls for less than a single pixel of movement >.>
            double delta = Math.pow(this.cblastPosX - to.getX(), 2) + Math.pow(this.cblastPosY - to.getY(), 2) + Math.pow(this.cblastPosZ - to.getZ(), 2);
            float deltaAngle = Math.abs(this.cblastYaw - to.getYaw()) + Math.abs(this.cblastPitch - to.getPitch());

            if ((delta > 1f / 256 || deltaAngle > 10f) && (this.hasMoved && !this.playerEntity.isDead)) {
                this.cblastPosX = to.getX();
                this.cblastPosY = to.getY();
                this.cblastPosZ = to.getZ();
                this.cblastYaw = to.getYaw();
                this.cblastPitch = to.getPitch();

                // Skip the first time we do this
                if (from.getX() != Double.MAX_VALUE) {
                    PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                    playerEntity.worldObj.getServer().getPluginManager().callEvent(event);

                    // If the event is cancelled we move the player back to their old location.
                    if (event.isCancelled()) {
                        this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet13PlayerLookMove(from.getX(), from.getY() + 1.6200000047683716D, from.getY(), from.getZ(), from.getYaw(), from.getPitch(), false));
                        return;
                    }

                    /* If a Plugin has changed the To destination then we teleport the Player
                    there to avoid any 'Moved wrongly' or 'Moved too quickly' errors.
                    We only do this if the Event was not cancelled. */
                    if (!to.equals(event.getTo()) && !event.isCancelled()) {
                        this.playerEntity.getBukkitEntity().teleport(event.getTo(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        return;
                    }

                    /* Check to see if the Players Location has some how changed during the call of the event.
                    This can happen due to a plugin teleporting the player instead of using .setTo() */
                    if (!from.equals(this.getPlayer().getBukkitEntity().getLocation()) && this.justTeleported) {
                        this.justTeleported = false;
                        return;
                    }
                }
            }

            if (Double.isNaN(par1Packet10Flying.xPosition) || Double.isNaN(par1Packet10Flying.yPosition) || Double.isNaN(par1Packet10Flying.zPosition) || Double.isNaN(par1Packet10Flying.stance)) {
                player.teleport(player.getWorld().getSpawnLocation(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN);
                System.err.println(player.getName() + " was caught trying to crash the server with an invalid position.");
                player.kickPlayer("Nope!");
                return;
            }

            if (this.hasMoved && !this.playerEntity.isDead) {
                // CraftBukkit end
                double var5;
                double var7;
                double var9;
                double var13;

                if (this.playerEntity.ridingEntity != null)
                {
                    float var34 = this.playerEntity.rotationYaw;
                    float var4 = this.playerEntity.rotationPitch;
                    this.playerEntity.ridingEntity.updateRiderPosition();
                    var5 = this.playerEntity.posX;
                    var7 = this.playerEntity.posY;
                    var9 = this.playerEntity.posZ;
                    double var35 = 0.0D;
                    var13 = 0.0D;

                    if (par1Packet10Flying.rotating)
                    {
                        var34 = par1Packet10Flying.yaw;
                        var4 = par1Packet10Flying.pitch;
                    }

                    if (par1Packet10Flying.moving && par1Packet10Flying.yPosition == -999.0D && par1Packet10Flying.stance == -999.0D)
                    {
                        if (Math.abs(par1Packet10Flying.xPosition) > 1.0D || Math.abs(par1Packet10Flying.zPosition) > 1.0D)
                        {
                            System.err.println(this.playerEntity.username + " was caught trying to crash the server with an invalid position.");
                            this.kickPlayerFromServer("Nope!");
                            return;
                        }

                        var35 = par1Packet10Flying.xPosition;
                        var13 = par1Packet10Flying.zPosition;
                    }

                    this.playerEntity.onGround = par1Packet10Flying.onGround;
                    this.playerEntity.onUpdateEntity();
                    this.playerEntity.moveEntity(var35, 0.0D, var13);
                    this.playerEntity.setPositionAndRotation(var5, var7, var9, var34, var4);
                    this.playerEntity.motionX = var35;
                    this.playerEntity.motionZ = var13;

                    if (this.playerEntity.ridingEntity != null)
                    {
                        var2.uncheckedUpdateEntity(this.playerEntity.ridingEntity, true);
                    }

                    if (this.playerEntity.ridingEntity != null)
                    {
                        this.playerEntity.ridingEntity.updateRiderPosition();
                    }

                    if (!this.hasMoved) //Fixes teleportation kick while riding entities
                    {
                        return;
                    }

                    this.mcServer.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);
                    this.lastPosX = this.playerEntity.posX;
                    this.lastPosY = this.playerEntity.posY;
                    this.lastPosZ = this.playerEntity.posZ;
                    var2.updateEntity(this.playerEntity);
                    return;
                }

                if (this.playerEntity.isPlayerSleeping())
                {
                    this.playerEntity.onUpdateEntity();
                    this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                    var2.updateEntity(this.playerEntity);
                    return;
                }

                var3 = this.playerEntity.posY;
                this.lastPosX = this.playerEntity.posX;
                this.lastPosY = this.playerEntity.posY;
                this.lastPosZ = this.playerEntity.posZ;
                var5 = this.playerEntity.posX;
                var7 = this.playerEntity.posY;
                var9 = this.playerEntity.posZ;
                float var11 = this.playerEntity.rotationYaw;
                float var12 = this.playerEntity.rotationPitch;

                if (par1Packet10Flying.moving && par1Packet10Flying.yPosition == -999.0D && par1Packet10Flying.stance == -999.0D)
                {
                    par1Packet10Flying.moving = false;
                }

                if (par1Packet10Flying.moving)
                {
                    var5 = par1Packet10Flying.xPosition;
                    var7 = par1Packet10Flying.yPosition;
                    var9 = par1Packet10Flying.zPosition;
                    var13 = par1Packet10Flying.stance - par1Packet10Flying.yPosition;

                    if (!this.playerEntity.isPlayerSleeping() && (var13 > 1.65D || var13 < 0.1D))
                    {
                        this.kickPlayerFromServer("Illegal stance");
                        logger.warning(this.playerEntity.username + " had an illegal stance: " + var13);
                        return;
                    }

                    if (Math.abs(par1Packet10Flying.xPosition) > 3.2E7D || Math.abs(par1Packet10Flying.zPosition) > 3.2E7D)
                    {
                    	// CraftBukkit - teleport to previous position instead of kicking, players get stuck
                        this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                        return;
                    }
                }

                if (par1Packet10Flying.rotating)
                {
                    var11 = par1Packet10Flying.yaw;
                    var12 = par1Packet10Flying.pitch;
                }

                this.playerEntity.onUpdateEntity();
                this.playerEntity.ySize = 0.0F;
                this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, var11, var12);

                if (!this.hasMoved)
                {
                    return;
                }

                var13 = var5 - this.playerEntity.posX;
                double var15 = var7 - this.playerEntity.posY;
                double var17 = var9 - this.playerEntity.posZ;
                // CraftBukkit start - min to max
                double var19 = Math.max(Math.abs(var13), Math.abs(this.playerEntity.motionX));
                double var21 = Math.max(Math.abs(var15), Math.abs(this.playerEntity.motionY));
                double var23 = Math.max(Math.abs(var17), Math.abs(this.playerEntity.motionZ));
                // CraftBukkit end
                double var25 = var19 * var19 + var21 * var21 + var23 * var23;

                if (var25 > 100.0D && (!this.mcServer.isSinglePlayer() || !this.mcServer.getServerOwner().equals(this.playerEntity.username)))
                {
                    logger.warning(this.playerEntity.username + " moved too quickly! " + var13 + "," + var15 + "," + var17 + " (" + var19 + ", " + var21 + ", " + var23 + ")");
                    this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                    return;
                }

                float var27 = 0.0625F;
                boolean var28 = var2.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.boundingBox.copy().contract((double)var27, (double)var27, (double)var27)).isEmpty();

                if (this.playerEntity.onGround && !par1Packet10Flying.onGround && var15 > 0.0D)
                {
                    this.playerEntity.addExhaustion(0.2F);
                }

                if (!this.hasMoved) //Fixes "Moved Too Fast" kick when being teleported while moving
                {
                    return;
                }

                this.playerEntity.moveEntity(var13, var15, var17);
                this.playerEntity.onGround = par1Packet10Flying.onGround;
                this.playerEntity.addMovementStat(var13, var15, var17);
                double var29 = var15;
                var13 = var5 - this.playerEntity.posX;
                var15 = var7 - this.playerEntity.posY;

                if (var15 > -0.5D || var15 < 0.5D)
                {
                    var15 = 0.0D;
                }

                var17 = var9 - this.playerEntity.posZ;
                var25 = var13 * var13 + var15 * var15 + var17 * var17;
                boolean var31 = false;

                if (var25 > 0.0625D && !this.playerEntity.isPlayerSleeping() && !this.playerEntity.theItemInWorldManager.isCreative())
                {
                    var31 = true;
                    logger.warning(this.playerEntity.username + " moved wrongly!");
                }

                if (!this.hasMoved) //Fixes "Moved Too Fast" kick when being teleported while moving
                {
                    return;
                }

                this.playerEntity.setPositionAndRotation(var5, var7, var9, var11, var12);
                boolean var32 = var2.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.boundingBox.copy().contract((double)var27, (double)var27, (double)var27)).isEmpty();

                if (var28 && (var31 || !var32) && !this.playerEntity.isPlayerSleeping() && !this.playerEntity.noClip)
                {
                    this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, var11, var12);
                    return;
                }

                AxisAlignedBB var33 = this.playerEntity.boundingBox.copy().expand((double)var27, (double)var27, (double)var27).addCoord(0.0D, -0.55D, 0.0D);

                if (!this.mcServer.isFlightAllowed() && !this.playerEntity.capabilities.allowFlying && !var2.isAABBNonEmpty(var33)  && !this.playerEntity.capabilities.allowFlying) // CraftBukkit - check abilities instead of creative mode
                {
                    if (var29 >= -0.03125D)
                    {
                        ++this.ticksForFloatKick;

                        if (this.ticksForFloatKick > 80)
                        {
                            logger.warning(this.playerEntity.username + " was kicked for floating too long!");
                            this.kickPlayerFromServer("Flying is not enabled on this server");
                            return;
                        }
                    }
                }
                else
                {
                    this.ticksForFloatKick = 0;
                }

                if (!this.hasMoved) //Fixes "Moved Too Fast" kick when being teleported while moving
                {
                    return;
                }

                this.playerEntity.onGround = par1Packet10Flying.onGround;
                this.mcServer.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);
                if (this.playerEntity.capabilities.isCreativeMode) return; // CraftBukkit - fixed fall distance accumulating while being in Creative mode.
                this.playerEntity.updateFlyingState(this.playerEntity.posY - var3, par1Packet10Flying.onGround);
            }
        }
    }

    /**
     * Moves the player to the specified destination and rotation
     */
    public void setPlayerLocation(double par1, double par3, double par5, float par7, float par8)
    {
    	// CraftBukkit start - Delegate to teleport(Location)
        Player player = this.getPlayer().getBukkitEntity();
        Location from = player.getLocation();
        Location to = new Location(this.getPlayer().getBukkitEntity().getWorld(), par1, par3, par5, par7, par8);
        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, to, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        playerEntity.worldObj.getServer().getPluginManager().callEvent(event);

        from = event.getFrom();
        to = event.isCancelled() ? from : event.getTo();

        this.teleport(to);
    }

    public void teleport(Location dest) {
    	double par1, par3, par5;
    	float par7, par8;
    	
    	par1 = dest.getX();
    	par3 = dest.getY();
    	par5 = dest.getZ();
    	par7 = dest.getYaw();
    	par8 = dest.getPitch();
    	
    	// TODO: make sure this is the best way to address this.
        if (Float.isNaN(par7)) {
            par7 = 0;
        }

        if (Float.isNaN(par8)) {
            par8 = 0;
        }
        
        this.cblastPosX = par1;
        this.cblastPosY = par3;
        this.cblastPosZ = par5;
        this.cblastYaw = par7;
        this.cblastPitch = par8;
        this.justTeleported = true;
        // CraftBukkit end
        
        this.hasMoved = false;
        this.lastPosX = par1;
        this.lastPosY = par3;
        this.lastPosZ = par5;
        this.playerEntity.setPositionAndRotation(par1, par3, par5, par7, par8);
        this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet13PlayerLookMove(par1, par3 + 1.6200000047683716D, par3, par5, par7, par8, false));
    }

    public void handleBlockDig(Packet14BlockDig par1Packet14BlockDig)
    {
    	if (this.playerEntity.isDead) return; // CraftBukkit
    	
        WorldServer var2 = this.mcServer.worldServerForDimension(this.playerEntity.dimension);

        if (par1Packet14BlockDig.status == 4)
        {
            this.playerEntity.dropOneItem(false);
        }
        else if (par1Packet14BlockDig.status == 3)
        {
            this.playerEntity.dropOneItem(true);
        }
        else if (par1Packet14BlockDig.status == 5)
        {
            this.playerEntity.stopUsingItem();
        }
        else
        {
            int var3 = this.mcServer.getSpawnProtectionSize();
            boolean var4 = var2.provider.dimensionId != 0 || this.mcServer.getConfigurationManager().getOps().isEmpty() || this.mcServer.getConfigurationManager().areCommandsAllowed(this.playerEntity.username) || var3 <= 0 || this.mcServer.isSinglePlayer();
            boolean var5 = false;

            if (par1Packet14BlockDig.status == 0 || par1Packet14BlockDig.status == 1) // CraftBukkit - check cancelled
            {
                var5 = true;
            }

            if (par1Packet14BlockDig.status == 1)
            {
                var5 = true;
            }

            if (par1Packet14BlockDig.status == 2)
            {
                var5 = true;
            }

            int var6 = par1Packet14BlockDig.xPosition;
            int var7 = par1Packet14BlockDig.yPosition;
            int var8 = par1Packet14BlockDig.zPosition;

            if (var5)
            {
                double var9 = this.playerEntity.posX - ((double)var6 + 0.5D);
                double var11 = this.playerEntity.posY - ((double)var7 + 0.5D) + 1.5D;
                double var13 = this.playerEntity.posZ - ((double)var8 + 0.5D);
                double var15 = var9 * var9 + var11 * var11 + var13 * var13;

                double dist = playerEntity.theItemInWorldManager.getBlockReachDistance() + 1;
                dist *= dist;

                if (var15 > dist)
                {
                    return;
                }

                if (var7 >= this.mcServer.getBuildLimit())
                {
                    return;
                }
            }

            ChunkCoordinates var17 = var2.getSpawnPoint();
            int var10 = MathHelper.abs_int(var6 - var17.posX);
            int var18 = MathHelper.abs_int(var8 - var17.posZ);

            if (var10 > var18)
            {
                var18 = var10;
            }

            if (par1Packet14BlockDig.status == 0)
            {
                if (var18 <= var3 && !var4)
                {
                	// CraftBukkit start
                	CraftEventFactory.callPlayerInteractEvent(playerEntity, org.bukkit.event.block.Action.LEFT_CLICK_BLOCK, var6, var7, var8, 0, playerEntity.inventory.getCurrentItem());
                    ForgeEventFactory.onPlayerInteract(playerEntity, Action.LEFT_CLICK_BLOCK, var6, var7, var8, 0);
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(var6, var7, var8, var2));
                    // Update any tile entity data for this block
                    TileEntity tileentity = var2.getBlockTileEntity(var6, var7, var8);
                    if (tileentity != null) {
                    	Packet dp = tileentity.getDescriptionPacket();
                    	if(dp != null)
                    		this.playerEntity.playerNetServerHandler.sendPacketToPlayer(dp);
                    }
                    // CraftBukkit end
                }
                else
                {
                    this.playerEntity.theItemInWorldManager.onBlockClicked(var6, var7, var8, par1Packet14BlockDig.face);
                }
            }
            else if (par1Packet14BlockDig.status == 2)
            {
                this.playerEntity.theItemInWorldManager.uncheckedTryHarvestBlock(var6, var7, var8);

                if (var2.getBlockId(var6, var7, var8) != 0)
                {
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(var6, var7, var8, var2));
                }
            }
            else if (par1Packet14BlockDig.status == 1)
            {
                this.playerEntity.theItemInWorldManager.cancelDestroyingBlock(var6, var7, var8);

                if (var2.getBlockId(var6, var7, var8) != 0)
                {
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(var6, var7, var8, var2));
                }
            }
        }
    }

    public void handlePlace(Packet15Place par1Packet15Place)
    {
        WorldServer var2 = this.mcServer.worldServerForDimension(this.playerEntity.dimension);
        
        // CraftBukkit start
        if(this.playerEntity.isDead) return;
        
        // This is a horrible hack needed because the client sends 2 packets on 'right mouse click'
        // aimed at a block. We shouldn't need to get the second packet if the data is handled
        // but we cannot know what the client will do, so we might still get it
        //
        // If the time between packets is small enough, and the 'signature' similar, we discard the
        // second one. This sadly has to remain until Mojang makes their packets saner. :(
        //  -- Grum

        if (par1Packet15Place.getDirection() == 255) {
            if (par1Packet15Place.getItemStack() != null && par1Packet15Place.getItemStack().itemID == this.lastMaterial && this.lastPacket != null && par1Packet15Place.creationTimeMillis - this.lastPacket < 100) {
                this.lastPacket = null;
                return;
            }
        } else {
            this.lastMaterial = par1Packet15Place.getItemStack() == null ? -1 : par1Packet15Place.getItemStack().itemID;
            this.lastPacket = par1Packet15Place.creationTimeMillis;
        }
        
        // CraftBukkit - if rightclick decremented the item, always send the update packet.
        // this is not here for CraftBukkit's own functionality; rather it is to fix
        // a notch bug where the item doesn't update correctly.
        boolean always = false;
        
        // CraftBukkit end
        
        ItemStack var3 = this.playerEntity.inventory.getCurrentItem();
        boolean var4 = false;
        int var5 = par1Packet15Place.getXPosition();
        int var6 = par1Packet15Place.getYPosition();
        int var7 = par1Packet15Place.getZPosition();
        int var8 = par1Packet15Place.getDirection();
        int var9 = this.mcServer.getSpawnProtectionSize();
        boolean var10 = var2.provider.dimensionId != 0 || this.mcServer.getConfigurationManager().getOps().isEmpty() || this.mcServer.getConfigurationManager().areCommandsAllowed(this.playerEntity.username) || var9 <= 0 || this.mcServer.isSinglePlayer();

        if (par1Packet15Place.getDirection() == 255)
        {
            if (var3 == null)
            {
                return;
            }
            
            // CraftBukkit start
            int itemstackAmount = var3.stackSize;
            org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(playerEntity, org.bukkit.event.block.Action.RIGHT_CLICK_AIR, var3);
            if (event.useItemInHand() != org.bukkit.event.Event.Result.DENY) {
            	PlayerInteractEvent event2 = ForgeEventFactory.onPlayerInteract(playerEntity, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1);
            	if(event2.useItem != Event.Result.DENY) {
            		this.playerEntity.theItemInWorldManager.tryUseItem(this.playerEntity, var2, var3);
            	}
            }

            // CraftBukkit - notch decrements the counter by 1 in the above method with food,
            // snowballs and so forth, but he does it in a place that doesn't cause the
            // inventory update packet to get sent
            always = (var3.stackSize != itemstackAmount);
            // CraftBukkit end
        }
        else if (par1Packet15Place.getYPosition() >= this.mcServer.getBuildLimit() - 1 && (par1Packet15Place.getDirection() == 1 || par1Packet15Place.getYPosition() >= this.mcServer.getBuildLimit()))
        {
            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat("\u00a77Height limit for building is " + this.mcServer.getBuildLimit()));
            var4 = true;
        }
        else
        {
            ChunkCoordinates var11 = var2.getSpawnPoint();
            int var12 = MathHelper.abs_int(var5 - var11.posX);
            int var13 = MathHelper.abs_int(var7 - var11.posZ);

            if (var12 > var13)
            {
                var13 = var12;
            }

            double dist = playerEntity.theItemInWorldManager.getBlockReachDistance() + 1;
            dist *= dist;
            if (this.hasMoved && this.playerEntity.getDistanceSq((double)var5 + 0.5D, (double)var6 + 0.5D, (double)var7 + 0.5D) < dist && (var13 > var9 || var10))
            {
                this.playerEntity.theItemInWorldManager.activateBlockOrUseItem(this.playerEntity, var2, var3, var5, var6, var7, var8, par1Packet15Place.getXOffset(), par1Packet15Place.getYOffset(), par1Packet15Place.getZOffset());
            }

            var4 = true;
        }

        if (var4)
        {
            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(var5, var6, var7, var2));

            if (var8 == 0)
            {
                --var6;
            }

            if (var8 == 1)
            {
                ++var6;
            }

            if (var8 == 2)
            {
                --var7;
            }

            if (var8 == 3)
            {
                ++var7;
            }

            if (var8 == 4)
            {
                --var5;
            }

            if (var8 == 5)
            {
                ++var5;
            }

            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(var5, var6, var7, var2));
        }

        var3 = this.playerEntity.inventory.getCurrentItem();

        if (var3 != null && var3.stackSize == 0)
        {
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = null;
            var3 = null;
        }

        if (var3 == null || var3.getMaxItemUseDuration() == 0)
        {
            this.playerEntity.playerInventoryBeingManipulated = true;
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = ItemStack.copyItemStack(this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem]);
            Slot var14 = this.playerEntity.openContainer.getSlotFromInventory(this.playerEntity.inventory, this.playerEntity.inventory.currentItem);
            this.playerEntity.openContainer.detectAndSendChanges();
            this.playerEntity.playerInventoryBeingManipulated = false;

            // CraftBukkit - TODO CHECK IF NEEDED -- new if structure might not need 'always'. Kept it in for now, but may be able to remove in future
            if (!ItemStack.areItemStacksEqual(this.playerEntity.inventory.getCurrentItem(), par1Packet15Place.getItemStack()) || always)
            {
                this.sendPacketToPlayer(new Packet103SetSlot(this.playerEntity.openContainer.windowId, var14.slotNumber, this.playerEntity.inventory.getCurrentItem()));
            }
        }
    }

    public void handleErrorMessage(String par1Str, Object[] par2ArrayOfObj)
    {
    	if(this.connectionClosed) return; // CraftBukkit - rarely it would send a disconnect line twice
    	
    	
        logger.info(this.playerEntity.username + " lost connection: " + par1Str);
        // CraftBukkit start - we need to handle custom quit messages
        String quitMessage = this.mcServer.getConfigurationManager().playerLoggedOut(this.playerEntity);
        if ((quitMessage != null) && (quitMessage.length() > 0)) {
            this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new Packet3Chat(quitMessage));
        }
        // CraftBukkit end
        this.connectionClosed = true;

        if (this.mcServer.isSinglePlayer() && this.playerEntity.username.equals(this.mcServer.getServerOwner()))
        {
            logger.info("Stopping singleplayer server as player logged out");
            this.mcServer.initiateShutdown();
        }
    }

    /**
     * Default handler called for packets that don't have their own handlers in NetClientHandler; currentlly does
     * nothing.
     */
    public void unexpectedPacket(Packet par1Packet)
    {
    	if(this.connectionClosed) return; // CraftBukkit
        logger.warning(this.getClass() + " wasn\'t prepared to deal with a " + par1Packet.getClass());
        this.kickPlayerFromServer("Protocol error, unexpected packet");
    }

    /**
     * addToSendQueue. if it is a chat packet, check before sending it
     */
    public void sendPacketToPlayer(Packet par1Packet)
    {
        if (par1Packet instanceof Packet3Chat)
        {
            Packet3Chat var2 = (Packet3Chat)par1Packet;
            int var3 = this.playerEntity.getChatVisibility();

            if (var3 == 2)
            {
                return;
            }

            if (var3 == 1 && !var2.getIsServer())
            {
                return;
            }
            
            // CraftBukkit start
            String message = var2.message;
            for (final String line : org.bukkit.craftbukkit.TextWrapper.wrapText(message)) {
                this.netManager.addToSendQueue(new Packet3Chat(line));
            }
            return;
            // CraftBukkit end
        }
        
        // CraftBukkit start
        if (par1Packet == null) {
            return;
        } else if (par1Packet instanceof Packet6SpawnPosition) {
            Packet6SpawnPosition packet6 = (Packet6SpawnPosition) par1Packet;
            this.playerEntity.compassTarget = new Location(this.getPlayer().worldObj.getWorld(), packet6.xPosition, packet6.yPosition, packet6.zPosition);
        }
        // CraftBukkit end

        this.netManager.addToSendQueue(par1Packet);
    }

    public void handleBlockItemSwitch(Packet16BlockItemSwitch par1Packet16BlockItemSwitch)
    {
    	if(this.playerEntity.isDead) return; // CraftBukkit
    	
        if (par1Packet16BlockItemSwitch.id >= 0 && par1Packet16BlockItemSwitch.id < InventoryPlayer.getHotbarSize())
        {
        	// CraftBukkit start
        	PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getPlayer().getBukkitEntity(), this.playerEntity.inventory.currentItem, par1Packet16BlockItemSwitch.id);
        	this.mcServer.server.getPluginManager().callEvent(event);
        	// CraftBukkit end
        	
            this.playerEntity.inventory.currentItem = par1Packet16BlockItemSwitch.id;
        }
        else
        {
            logger.warning(this.playerEntity.username + " tried to set an invalid carried item");
            kickPlayerFromServer("Nope!"); // CraftBukkit
        }
    }

    public void handleChat(Packet3Chat par1Packet3Chat)
    {
        par1Packet3Chat = FMLNetworkHandler.handleChatMessage(this, par1Packet3Chat);
        if (this.playerEntity.getChatVisibility() == 2)
        {
            this.sendPacketToPlayer(new Packet3Chat("Cannot send chat message."));
        }
        else
        {
            String var2 = par1Packet3Chat.message;

            if (var2.length() > 100)
            {
                this.kickPlayerFromServer("Chat message too long");
            }
            else
            {
                var2 = var2.trim();

                for (int var3 = 0; var3 < var2.length(); ++var3)
                {
                    if (!ChatAllowedCharacters.isAllowedCharacter(var2.charAt(var3)))
                    {
                        this.kickPlayerFromServer("Illegal characters in chat");
                        return;
                    }
                }

                if (var2.startsWith("/"))
                {
                    this.handleSlashCommand(var2);
                }
                else
                {
                    if (this.playerEntity.getChatVisibility() == 1)
                    {
                        this.sendPacketToPlayer(new Packet3Chat("Cannot send chat message."));
                        return;
                    }
                    ServerChatEvent event = new ServerChatEvent(this.playerEntity, var2, "<" + this.playerEntity.username + "> " + var2);
                    if (MinecraftForge.EVENT_BUS.post(event))
                    {
                        return;
                    }
                    var2 = event.line;
                    logger.info(var2);
                    this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new Packet3Chat(var2, false));
                }

                this.chatSpamThresholdCount += 20;

                if (this.chatSpamThresholdCount > 200 && !this.mcServer.getConfigurationManager().areCommandsAllowed(this.playerEntity.username))
                {
                    this.kickPlayerFromServer("disconnect.spam");
                }
            }
        }
    }

    /**
     * Processes a / command
     */
    private void handleSlashCommand(String par1Str)
    {
        this.mcServer.getCommandManager().executeCommand(this.playerEntity, par1Str);
    }

    public void handleAnimation(Packet18Animation par1Packet18Animation)
    {
        if (par1Packet18Animation.animate == 1)
        {
            this.playerEntity.swingItem();
        }
    }

    /**
     * runs registerPacket on the given Packet19EntityAction
     */
    public void handleEntityAction(Packet19EntityAction par1Packet19EntityAction)
    {
        if (par1Packet19EntityAction.state == 1)
        {
            this.playerEntity.setSneaking(true);
        }
        else if (par1Packet19EntityAction.state == 2)
        {
            this.playerEntity.setSneaking(false);
        }
        else if (par1Packet19EntityAction.state == 4)
        {
            this.playerEntity.setSprinting(true);
        }
        else if (par1Packet19EntityAction.state == 5)
        {
            this.playerEntity.setSprinting(false);
        }
        else if (par1Packet19EntityAction.state == 3)
        {
            this.playerEntity.wakeUpPlayer(false, true, true);
            // this.hasMoved = false; // CraftBukkit - this is handled in teleport
        }
    }

    public void handleKickDisconnect(Packet255KickDisconnect par1Packet255KickDisconnect)
    {
        this.netManager.networkShutdown("disconnect.quitting", new Object[0]);
    }

    /**
     * returns 0 for memoryMapped connections
     */
    public int packetSize()
    {
        return this.netManager.packetSize();
    }

    public void handleUseEntity(Packet7UseEntity par1Packet7UseEntity)
    {
    	if(playerEntity.isDead) return; // CraftBukkit
    	
        WorldServer var2 = this.mcServer.worldServerForDimension(this.playerEntity.dimension);
        Entity var3 = var2.getEntityByID(par1Packet7UseEntity.targetEntity);

        if (var3 != null)
        {
            boolean var4 = this.playerEntity.canEntityBeSeen(var3);
            double var5 = 36.0D;

            if (!var4)
            {
                var5 = 9.0D;
            }

            if (this.playerEntity.getDistanceSqToEntity(var3) < var5)
            {
                if (par1Packet7UseEntity.isLeftClick == 0)
                {
                    this.playerEntity.interactWith(var3);
                }
                else if (par1Packet7UseEntity.isLeftClick == 1)
                {
                    this.playerEntity.attackTargetEntityWithCurrentItem(var3);
                }
            }
        }
    }

    public void handleClientCommand(Packet205ClientCommand par1Packet205ClientCommand)
    {
        if (par1Packet205ClientCommand.forceRespawn == 1)
        {
            if (this.playerEntity.playerConqueredTheEnd)
            {
                this.playerEntity = this.mcServer.getConfigurationManager().respawnPlayer(this.playerEntity, 0, true);
            }
            else if (this.playerEntity.getServerForPlayer().getWorldInfo().isHardcoreModeEnabled())
            {
                if (this.mcServer.isSinglePlayer() && this.playerEntity.username.equals(this.mcServer.getServerOwner()))
                {
                    this.playerEntity.playerNetServerHandler.kickPlayerFromServer("You have died. Game over, man, it\'s game over!");
                    this.mcServer.deleteWorldAndStopServer();
                }
                else
                {
                    BanEntry var2 = new BanEntry(this.playerEntity.username);
                    var2.setBanReason("Death in Hardcore");
                    this.mcServer.getConfigurationManager().getBannedPlayers().put(var2);
                    this.playerEntity.playerNetServerHandler.kickPlayerFromServer("You have died. Game over, man, it\'s game over!");
                }
            }
            else
            {
                if (this.playerEntity.getHealth() > 0)
                {
                    return;
                }

                this.playerEntity = this.mcServer.getConfigurationManager().respawnPlayer(this.playerEntity, playerEntity.dimension, false);
            }
        }
    }

    /**
     * If this returns false, all packets will be queued for the main thread to handle, even if they would otherwise be
     * processed asynchronously. Used to avoid processing packets on the client before the world has been downloaded
     * (which happens on the main thread)
     */
    public boolean canProcessPacketsAsync()
    {
        return true;
    }

    /**
     * respawns the player
     */
    public void handleRespawn(Packet9Respawn par1Packet9Respawn) {}

    public void handleCloseWindow(Packet101CloseWindow par1Packet101CloseWindow)
    {
        this.playerEntity.closeInventory();
    }

    public void handleWindowClick(Packet102WindowClick par1Packet102WindowClick)
    {
    	if (this.playerEntity.isDead) return; // CraftBukkit
    	
        if (this.playerEntity.openContainer.windowId == par1Packet102WindowClick.window_Id && this.playerEntity.openContainer.isPlayerNotUsingContainer(this.playerEntity))
        {
            ItemStack var2;

            // CraftBukkit start - fire InventoryClickEvent
            InventoryView inventory = BukkitInventoryHelper.getBukkitView(playerEntity.openContainer);
            SlotType type = CraftInventoryView.getSlotType(inventory, par1Packet102WindowClick.inventorySlot);

            InventoryClickEvent event = new InventoryClickEvent(inventory, type, par1Packet102WindowClick.inventorySlot, par1Packet102WindowClick.mouseClick != 0, par1Packet102WindowClick.holdingShift == 1);
            org.bukkit.inventory.Inventory top = inventory.getTopInventory();
            if (par1Packet102WindowClick.inventorySlot == 0 && top instanceof CraftingInventory) {
                org.bukkit.inventory.Recipe recipe = ((CraftingInventory) top).getRecipe();
                if (recipe != null) {
                    event = new CraftItemEvent(recipe, inventory, type, par1Packet102WindowClick.inventorySlot, par1Packet102WindowClick.mouseClick != 0, par1Packet102WindowClick.holdingShift == 1);
                }
            }
            mcServer.server.getPluginManager().callEvent(event);

            boolean defaultBehaviour = false;
            
            var2 = null;

            switch(event.getResult()) {
            case DEFAULT:
            	var2 = this.playerEntity.openContainer.slotClick(par1Packet102WindowClick.inventorySlot, par1Packet102WindowClick.mouseClick, par1Packet102WindowClick.holdingShift, this.playerEntity);
                defaultBehaviour = true;
                break;
            case DENY: // Deny any change, including changes from the event
            	break;
            case ALLOW: // Allow changes unconditionally
                org.bukkit.inventory.ItemStack cursor = event.getCursor();
                if (cursor == null) {
                    this.playerEntity.inventory.setItemStack((ItemStack) null);
                } else {
                    this.playerEntity.inventory.setItemStack(CraftItemStack.asNMSCopy(cursor));
                }
                org.bukkit.inventory.ItemStack item = event.getCurrentItem();
                if (item != null) {
                    var2 = CraftItemStack.asNMSCopy(item);
                    if (par1Packet102WindowClick.inventorySlot == -999) {
                        this.playerEntity.dropPlayerItem(var2);
                    } else {
                        this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot).putStack(var2);
                    }
                } else if (par1Packet102WindowClick.inventorySlot != -999) {
                    this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot).putStack((ItemStack) null);
                }
                break;
            }
            // CraftBukkit end
            
            if (ItemStack.areItemStacksEqual(par1Packet102WindowClick.itemStack, var2))
            {
                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet106Transaction(par1Packet102WindowClick.window_Id, par1Packet102WindowClick.action, true));
                this.playerEntity.playerInventoryBeingManipulated = true;
                this.playerEntity.openContainer.detectAndSendChanges();
                this.playerEntity.updateHeldItem();
                this.playerEntity.playerInventoryBeingManipulated = false;
            }
            else
            {
                this.field_72586_s.addKey(this.playerEntity.openContainer.windowId, Short.valueOf(par1Packet102WindowClick.action));
                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet106Transaction(par1Packet102WindowClick.window_Id, par1Packet102WindowClick.action, false));
                this.playerEntity.openContainer.setPlayerIsPresent(this.playerEntity, false);
                ArrayList var3 = new ArrayList();

                for (int var4 = 0; var4 < this.playerEntity.openContainer.inventorySlots.size(); ++var4)
                {
                    var3.add(((Slot)this.playerEntity.openContainer.inventorySlots.get(var4)).getStack());
                }

                this.playerEntity.sendContainerAndContentsToPlayer(this.playerEntity.openContainer, var3);
            }
        }
    }

    public void handleEnchantItem(Packet108EnchantItem par1Packet108EnchantItem)
    {
        if (this.playerEntity.openContainer.windowId == par1Packet108EnchantItem.windowId && this.playerEntity.openContainer.isPlayerNotUsingContainer(this.playerEntity))
        {
            this.playerEntity.openContainer.enchantItem(this.playerEntity, par1Packet108EnchantItem.enchantment);
            this.playerEntity.openContainer.detectAndSendChanges();
        }
    }

    /**
     * Handle a creative slot packet.
     */
    public void handleCreativeSetSlot(Packet107CreativeSetSlot par1Packet107CreativeSetSlot)
    {
        if (this.playerEntity.theItemInWorldManager.isCreative())
        {
            boolean var2 = par1Packet107CreativeSetSlot.slot < 0;
            ItemStack var3 = par1Packet107CreativeSetSlot.itemStack;
            boolean var4 = par1Packet107CreativeSetSlot.slot >= 1 && par1Packet107CreativeSetSlot.slot < 36 + InventoryPlayer.getHotbarSize();
            // CraftBukkit - added invalidItems check
            boolean var5 = var3 == null || var3.itemID < Item.itemsList.length && var3.itemID >= 0 && Item.itemsList[var3.itemID] != null && !invalidItems.contains(var3.itemID);
            boolean var6 = var3 == null || var3.getItemDamage() >= 0 && var3.getItemDamage() >= 0 && var3.stackSize <= 64 && var3.stackSize > 0;
            
            // CraftBukkit start - Fire INVENTORY_CLICK event
            org.bukkit.entity.HumanEntity player = this.playerEntity.getBukkitEntity();
            InventoryView inventory = new CraftInventoryView(player, player.getInventory(), this.playerEntity.inventoryContainer);
            SlotType slot = SlotType.QUICKBAR;
            if (par1Packet107CreativeSetSlot.slot == -1) {
                slot = SlotType.OUTSIDE;
            }

            InventoryClickEvent event = new InventoryClickEvent(inventory, slot, slot == SlotType.OUTSIDE ? -999 : par1Packet107CreativeSetSlot.slot, false, false);
            mcServer.server.getPluginManager().callEvent(event);
            org.bukkit.inventory.ItemStack item = event.getCurrentItem();

            switch (event.getResult()) {
            case ALLOW:
                if (slot == SlotType.QUICKBAR) {
                    if (item == null) {
                        this.playerEntity.inventoryContainer.putStackInSlot(par1Packet107CreativeSetSlot.slot, (ItemStack) null);
                    } else {
                        this.playerEntity.inventoryContainer.putStackInSlot(par1Packet107CreativeSetSlot.slot, CraftItemStack.asNMSCopy(item));
                    }
                } else if (item != null) {
                    this.playerEntity.dropPlayerItem(CraftItemStack.asNMSCopy(item));
                }
                return;
            case DENY:
                // TODO: Will this actually work?
                if (par1Packet107CreativeSetSlot.slot > -1) {
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(this.playerEntity.inventoryContainer.windowId, par1Packet107CreativeSetSlot.slot, CraftItemStack.asNMSCopy(item)));
                }
                return;
            case DEFAULT:
                // We do the stuff below
                break;
            default:
                return;
            }
            // CraftBukkit end
			if (var4 && var5 && var6)
            {
                if (var3 == null)
                {
                    this.playerEntity.inventoryContainer.putStackInSlot(par1Packet107CreativeSetSlot.slot, (ItemStack)null);
                }
                else
                {
                    this.playerEntity.inventoryContainer.putStackInSlot(par1Packet107CreativeSetSlot.slot, var3);
                }

                this.playerEntity.inventoryContainer.setPlayerIsPresent(this.playerEntity, true);
            }
            else if (var2 && var5 && var6 && this.creativeItemCreationSpamThresholdTally < 200)
            {
                this.creativeItemCreationSpamThresholdTally += 20;
                EntityItem var7 = this.playerEntity.dropPlayerItem(var3);

                if (var7 != null)
                {
                    var7.func_70288_d();
                }
            }
        }
    }

    public void handleTransaction(Packet106Transaction par1Packet106Transaction)
    {
        Short var2 = (Short)this.field_72586_s.lookup(this.playerEntity.openContainer.windowId);

        if (var2 != null && par1Packet106Transaction.shortWindowId == var2.shortValue() && this.playerEntity.openContainer.windowId == par1Packet106Transaction.windowId && !this.playerEntity.openContainer.isPlayerNotUsingContainer(this.playerEntity))
        {
            this.playerEntity.openContainer.setPlayerIsPresent(this.playerEntity, true);
        }
    }

    /**
     * Updates Client side signs
     */
    public void handleUpdateSign(Packet130UpdateSign par1Packet130UpdateSign)
    {
        WorldServer var2 = this.mcServer.worldServerForDimension(this.playerEntity.dimension);

        if (var2.blockExists(par1Packet130UpdateSign.xPosition, par1Packet130UpdateSign.yPosition, par1Packet130UpdateSign.zPosition))
        {
            TileEntity var3 = var2.getBlockTileEntity(par1Packet130UpdateSign.xPosition, par1Packet130UpdateSign.yPosition, par1Packet130UpdateSign.zPosition);

            if (var3 instanceof TileEntitySign)
            {
                TileEntitySign var4 = (TileEntitySign)var3;

                if (!var4.isEditable())
                {
                    this.mcServer.logWarning("Player " + this.playerEntity.username + " just tried to change non-editable sign");
                    return;
                }
            }

            int var6;
            int var8;

            for (var8 = 0; var8 < 4; ++var8)
            {
                boolean var5 = true;

                if (par1Packet130UpdateSign.signLines[var8].length() > 15)
                {
                    var5 = false;
                }
                else
                {
                    for (var6 = 0; var6 < par1Packet130UpdateSign.signLines[var8].length(); ++var6)
                    {
                        if (ChatAllowedCharacters.allowedCharacters.indexOf(par1Packet130UpdateSign.signLines[var8].charAt(var6)) < 0)
                        {
                            var5 = false;
                        }
                    }
                }

                if (!var5)
                {
                    par1Packet130UpdateSign.signLines[var8] = "!?";
                }
            }

            if (var3 instanceof TileEntitySign)
            {
                var8 = par1Packet130UpdateSign.xPosition;
                int var9 = par1Packet130UpdateSign.yPosition;
                var6 = par1Packet130UpdateSign.zPosition;
                TileEntitySign var7 = (TileEntitySign)var3;
                
                // CraftBukkit start
                Player player = this.mcServer.server.getPlayer(this.playerEntity);
                SignChangeEvent event = new SignChangeEvent((org.bukkit.craftbukkit.block.CraftBlock) player.getWorld().getBlockAt(var7.xCoord, var7.yCoord, var7.zCoord), this.mcServer.server.getPlayer(this.playerEntity), par1Packet130UpdateSign.signLines);
                this.mcServer.server.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    for (int l = 0; l < 4; ++l) {
                        var7.signText[l] = event.getLine(l);
                        if(var7.signText[l] == null) {
                            var7.signText[l] = "";
                        }
                    }
                    var7.setEditable(false);
                }
                // CraftBukkit end
                
                var7.onInventoryChanged();
                var2.markBlockForUpdate(var8, var9, var6);
            }
        }
    }

    /**
     * Handle a keep alive packet.
     */
    public void handleKeepAlive(Packet0KeepAlive par1Packet0KeepAlive)
    {
        if (par1Packet0KeepAlive.randomId == this.keepAliveRandomID)
        {
            int var2 = (int)(System.nanoTime() / 1000000L - this.keepAliveTimeSent);
            this.playerEntity.ping = (this.playerEntity.ping * 3 + var2) / 4;
        }
    }

    /**
     * determine if it is a server handler
     */
    public boolean isServerHandler()
    {
        return true;
    }

    /**
     * Handle a player abilities packet.
     */
    public void handlePlayerAbilities(Packet202PlayerAbilities par1Packet202PlayerAbilities)
    {
        this.playerEntity.capabilities.isFlying = par1Packet202PlayerAbilities.getFlying() && this.playerEntity.capabilities.allowFlying;
    }

    public void handleAutoComplete(Packet203AutoComplete par1Packet203AutoComplete)
    {
        StringBuilder var2 = new StringBuilder();
        String var4;

        for (Iterator var3 = this.mcServer.getPossibleCompletions(this.playerEntity, par1Packet203AutoComplete.getText()).iterator(); var3.hasNext(); var2.append(var4))
        {
            var4 = (String)var3.next();

            if (var2.length() > 0)
            {
                var2.append("\u0000");
            }
        }

        this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet203AutoComplete(var2.toString()));
    }

    public void handleClientInfo(Packet204ClientInfo par1Packet204ClientInfo)
    {
        this.playerEntity.updateClientInfo(par1Packet204ClientInfo);
    }

    public void handleCustomPayload(Packet250CustomPayload par1Packet250CustomPayload)
    {
        FMLNetworkHandler.handlePacket250Packet(par1Packet250CustomPayload, netManager, this);
    }

    public void handleVanilla250Packet(Packet250CustomPayload par1Packet250CustomPayload)
    {
        DataInputStream var2;
        ItemStack var3;
        ItemStack var4;

        if ("MC|BEdit".equals(par1Packet250CustomPayload.channel))
        {
            try
            {
                var2 = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                var3 = Packet.readItemStack(var2);

                if (!ItemWritableBook.validBookTagPages(var3.getTagCompound()))
                {
                    throw new IOException("Invalid book tag!");
                }

                var4 = this.playerEntity.inventory.getCurrentItem();

                if (var3 != null && var3.itemID == Item.writableBook.itemID && var3.itemID == var4.itemID)
                {
                    var4.setTagInfo("pages", var3.getTagCompound().getTagList("pages"));
                }
            }
            catch (Exception var12)
            {
                var12.printStackTrace();
            }
        }
        else if ("MC|BSign".equals(par1Packet250CustomPayload.channel))
        {
            try
            {
                var2 = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                var3 = Packet.readItemStack(var2);

                if (!ItemEditableBook.validBookTagContents(var3.getTagCompound()))
                {
                    throw new IOException("Invalid book tag!");
                }

                var4 = this.playerEntity.inventory.getCurrentItem();

                if (var3 != null && var3.itemID == Item.writtenBook.itemID && var4.itemID == Item.writableBook.itemID)
                {
                    var4.setTagInfo("author", new NBTTagString("author", this.playerEntity.username));
                    var4.setTagInfo("title", new NBTTagString("title", var3.getTagCompound().getString("title")));
                    var4.setTagInfo("pages", var3.getTagCompound().getTagList("pages"));
                    var4.itemID = Item.writtenBook.itemID;
                }
            }
            catch (Exception var11)
            {
                var11.printStackTrace();
            }
        }
        else
        {
            int var14;

            if ("MC|TrSel".equals(par1Packet250CustomPayload.channel))
            {
                try
                {
                    var2 = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                    var14 = var2.readInt();
                    Container var16 = this.playerEntity.openContainer;

                    if (var16 instanceof ContainerMerchant)
                    {
                        ((ContainerMerchant)var16).setCurrentRecipeIndex(var14);
                    }
                }
                catch (Exception var10)
                {
                    var10.printStackTrace();
                }
            }
            else
            {
                int var18;

                if ("MC|AdvCdm".equals(par1Packet250CustomPayload.channel))
                {
                    if (!this.mcServer.isCommandBlockEnabled())
                    {
                        this.playerEntity.sendChatToPlayer(this.playerEntity.translateString("advMode.notEnabled", new Object[0]));
                    }
                    else if (this.playerEntity.canCommandSenderUseCommand(2, "") && this.playerEntity.capabilities.isCreativeMode)
                    {
                        try
                        {
                            var2 = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                            var14 = var2.readInt();
                            var18 = var2.readInt();
                            int var5 = var2.readInt();
                            String var6 = Packet.readString(var2, 256);
                            TileEntity var7 = this.playerEntity.worldObj.getBlockTileEntity(var14, var18, var5);

                            if (var7 != null && var7 instanceof TileEntityCommandBlock)
                            {
                                ((TileEntityCommandBlock)var7).setCommand(var6);
                                this.playerEntity.worldObj.markBlockForUpdate(var14, var18, var5);
                                this.playerEntity.sendChatToPlayer("Command set: " + var6);
                            }
                        }
                        catch (Exception var9)
                        {
                            var9.printStackTrace();
                        }
                    }
                    else
                    {
                        this.playerEntity.sendChatToPlayer(this.playerEntity.translateString("advMode.notAllowed", new Object[0]));
                    }
                }
                else if ("MC|Beacon".equals(par1Packet250CustomPayload.channel))
                {
                    if (this.playerEntity.openContainer instanceof ContainerBeacon)
                    {
                        try
                        {
                            var2 = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                            var14 = var2.readInt();
                            var18 = var2.readInt();
                            ContainerBeacon var17 = (ContainerBeacon)this.playerEntity.openContainer;
                            Slot var19 = var17.getSlot(0);

                            if (var19.getHasStack())
                            {
                                var19.decrStackSize(1);
                                TileEntityBeacon var20 = var17.getBeacon();
                                var20.func_82128_d(var14);
                                var20.func_82127_e(var18);
                                var20.onInventoryChanged();
                            }
                        }
                        catch (Exception var8)
                        {
                            var8.printStackTrace();
                        }
                    }
                }
                else if ("MC|ItemName".equals(par1Packet250CustomPayload.channel) && this.playerEntity.openContainer instanceof ContainerRepair)
                {
                    ContainerRepair var13 = (ContainerRepair)this.playerEntity.openContainer;

                    if (par1Packet250CustomPayload.data != null && par1Packet250CustomPayload.data.length >= 1)
                    {
                        String var15 = ChatAllowedCharacters.filerAllowedCharacters(new String(par1Packet250CustomPayload.data));

                        if (var15.length() <= 30)
                        {
                            var13.updateItemName(var15);
                        }
                    }
                    else
                    {
                        var13.updateItemName("");
                    }
                }
            }
        }
    }
    

    @Override

    /**
     * Contains logic for handling packets containing arbitrary unique item data. Currently this is only for maps.
     */
    public void handleMapData(Packet131MapData par1Packet131MapData)
    {
        FMLNetworkHandler.handlePacket131Packet(this, par1Packet131MapData);
    }

    // modloader compat -- yuk!
    @Override
    public EntityPlayerMP getPlayer()
    {
        return playerEntity;
    }

    // CraftBukkit start
	public Player getPlayer2() {
		return getPlayer().getBukkitEntity();
	}
	// CraftBukkit end
}
