package immibis.lavabukkit;

import immibis.lavabukkit.nms.Mappings;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class LBCoreMod implements IFMLLoadingPlugin {

	@Override
	public String[] getLibraryRequestClass() {
		return new String[] {
			"immibis.lavabukkit.LBLibrarySet"
		};
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] {
			"immibis.lavabukkit.asm.EnumTransformer",
			"immibis.lavabukkit.asm.PluginClassLoaderTransformer",
			"immibis.lavabukkit.asm.HookInsertionTransformer",
			"immibis.lavabukkit.asm.RedstoneEventTransformer",
			"immibis.lavabukkit.asm.LBAccessTransformer",
			"immibis.lavabukkit.asm.IInventoryTransformer",
			"immibis.lavabukkit.asm.TrigMathTransformer"
		};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
	}

}
