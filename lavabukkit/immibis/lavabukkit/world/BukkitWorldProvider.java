package immibis.lavabukkit.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BukkitWorldProvider extends WorldProvider {
	
	private BukkitWorldDescriptor desc;
	private WorldProvider base;
	
	@Override
	public void setDimension(int dim) {
		super.setDimension(dim);
		
		desc = BukkitWorldRegistry.getInstance().getByDimension(dim);
		base = DimensionManager.createProviderFor(desc.clientDimensionID);
	}

	@Override
	public String getDimensionName() {
		return desc.name;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float[] calcSunriseSunsetColors(float par1, float par2) {
		return base.calcSunriseSunsetColors(par1, par2);
	}
	
	@Override
	public float calculateCelestialAngle(long par1, float par3) {
		return base.calculateCelestialAngle(par1, par3);
	}
	
	@Override
	public void calculateInitialWeather() {
		base.calculateInitialWeather();
	}
	
	@Override
	public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
		return base.canBlockFreeze(x, y, z, byWater);
	}
	
	@Override
	public boolean canCoordinateBeSpawn(int par1, int par2) {
		return base.canCoordinateBeSpawn(par1, par2);
	}
	
	@Override
	public boolean canDoLightning(Chunk chunk) {
		return base.canDoLightning(chunk);
	}
	
	@Override
	public boolean canDoRainSnowIce(Chunk chunk) {
		return base.canDoRainSnowIce(chunk);
	}
	
	@Override
	public boolean canMineBlock(EntityPlayer player, int x, int y, int z) {
		return base.canMineBlock(player, x, y, z);
	}
	
	@Override
	public boolean canRespawnHere() {
		return base.canRespawnHere();
	}
	
	@Override
	public boolean canSnowAt(int x, int y, int z) {
		return base.canSnowAt(x, y, z);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean doesXZShowFog(int par1, int par2) {
		return base.doesXZShowFog(par1, par2);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 drawClouds(float partialTicks) {
		return base.drawClouds(partialTicks);
	}
	
	@Override
	protected void generateLightBrightnessTable() {
		this.lightBrightnessTable = base.lightBrightnessTable;
	}
	
	@Override
	public int getActualHeight() {
		return base.getActualHeight();
	}
	
	@Override
	public int getAverageGroundLevel() {
		return base.getAverageGroundLevel();
	}
	
	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		return base.getBiomeGenForCoords(x, z);
	}
	
	@Override
	public IChunkProvider createChunkGenerator() {
		if(desc.generator == null)
			return base.createChunkGenerator();
		else
			return new BukkitToForgeGeneratorAdapter(desc.generator, worldObj);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getCloudHeight() {
		return base.getCloudHeight();
	}
	
	@Override
	public String getDepartMessage() {
		return base.getDepartMessage();
	}
	
	@Override
	public ChunkCoordinates getEntrancePortalLocation() {
		return base.getEntrancePortalLocation();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getFogColor(float par1, float par2) {
		return base.getFogColor(par1, par2);
	}
	
	@Override
	public int getHeight() {
		return base.getHeight();
	}
	
	@Override
	public double getHorizon() {
		return base.getHorizon();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getMoonPhase(long par1, float par3) {
		return base.getMoonPhase(par1, par3);
	}
	
	@Override
	public double getMovementFactor() {
		return base.getMovementFactor();
	}
	
	@Override
	public ChunkCoordinates getRandomizedSpawnPoint() {
		return base.getRandomizedSpawnPoint();
	}
	
	@Override
	public String getSaveFolder() {
		return desc.name;
	}
	
	@Override
	public long getSeed() {
		return base.getSeed();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getSkyColor(Entity cameraEntity, float partialTicks) {
		return base.getSkyColor(cameraEntity, partialTicks);
	}
	
	@Override
	public ChunkCoordinates getSpawnPoint() {
		return base.getSpawnPoint();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getStarBrightness(float par1) {
		return base.getStarBrightness(par1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getVoidFogYFactor() {
		return base.getVoidFogYFactor();
	}
	
	@Override
	public String getWelcomeMessage() {
		return base.getWelcomeMessage();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean getWorldHasVoidParticles() {
		return base.getWorldHasVoidParticles();
	}
	
	@Override
	public long getWorldTime() {
		return base.getWorldTime();
	}
	
	@Override
	public boolean isBlockHighHumidity(int x, int y, int z) {
		return base.isBlockHighHumidity(x, y, z);
	}
	
	@Override
	public boolean isDaytime() {
		return base.isDaytime();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isSkyColored() {
		return base.isSkyColored();
	}
	
	@Override
	public boolean isSurfaceWorld() {
		return base.isSurfaceWorld();
	}
	
	@Override
	protected void registerWorldChunkManager() {
		base.registerWorld(worldObj);
		this.worldChunkMgr = base.worldChunkMgr;
	}
	
	@Override
	public void resetRainAndThunder() {
		base.resetRainAndThunder();
	}
	
	@Override
	public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
		base.setAllowedSpawnTypes(allowHostile, allowPeaceful);
	}
	
	@Override
	public void setSpawnPoint(int x, int y, int z) {
		base.setSpawnPoint(x, y, z);
	}
	
	@Override
	public void setWorldTime(long time) {
		base.setWorldTime(time);
	}
	
	@Override
	public void toggleRain() {
		base.toggleRain();
	}

	@Override
	public void updateWeather() {
		base.updateWeather();
	}
}
