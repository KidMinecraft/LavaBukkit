package immibis.lavabukkit;

import cpw.mods.fml.relauncher.ILibrarySet;

public class LBLibrarySet implements ILibrarySet {

	@Override
	public String[] getLibraries() {
		return new String[] {
			// not guava 10, that's handled in the modified PluginClassLoader
			"commons-lang-2.6.jar",
			"ebean-2.7.7.jar",
			"jline-1.0.jar",
			"jopt-simple-4.3.jar",
			"json-simple-1.1.1.jar",
			"persistence-api-1.0-rev-1.jar",
			"snakeyaml-1.10.jar",
			"sqlitejdbc-v056.jar"
		};
	}

	@Override
	public String[] getHashes() {
		return new String[] {
			"0ce1edb914c94ebc388f086c6827e8bdeec71ac2",
			"29095f58b9f8f917c80c97b79030c75282ec1daf",
			"a29ccacd2c102960dbaf7c246216bf8b308a47a8",
			"88ffca34311a6564a98f14820431e17b4382a069",
			"5d6f9b6a9ddca2a28c0216cc44efb2e20d9c23b5",
			"880c3aedf89391507f23bdc8dfe5375a6b7b78c3",
			"50b289c0e1d719a427a76c619283500aa5d22414",
			"ba5206d1202877e94c417736d079ee83611a9ace"
		};
	}

	@Override
	public String getRootURL() {
		return LBLibrarySet.class.getResource("/libs/%s").toString().replace("%", "%%").replace("%%25s", "%s");
	}

}
