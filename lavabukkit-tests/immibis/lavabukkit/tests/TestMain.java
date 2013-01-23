package immibis.lavabukkit.tests;

import java.io.File;

import net.minecraft.client.Minecraft;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class TestMain {
	public static boolean isTesting = false;
	
	public static void main(String[] args) {
		isTesting = true;
		
		ReflectionHelper.setPrivateValue(Minecraft.class, null, new File("."), "minecraftDir");
		Minecraft.main(new String[0]);
	}
}
