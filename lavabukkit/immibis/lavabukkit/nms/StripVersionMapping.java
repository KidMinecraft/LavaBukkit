package immibis.lavabukkit.nms;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.Method;

public class StripVersionMapping implements Mapping {
	
	private final String version;
	
	public StripVersionMapping(String version) {
		this.version = version;
	}

	@Override
	public String mapClass(String c) {
		if(c.startsWith("org/bukkit/craftbukkit/" + version) || c.startsWith("net/minecraft/server/"+version))
			return c.replace("/"+version, "");
		return c;
	}

	@Override
	public Field mapField(Field f) {
		String c = mapClass(f.owner);
		if(c == f.owner)
			return f;
		return new Field(c, f.name);
	}

	@Override
	public Method mapMethod(Method m) {
		return new Method(mapClass(m.owner), m.name, Mappings.mapMethodDescriptor(this, m.desc));
	}

	@Override
	public Mapping reverse() {
		return new AddVersionMapping(version);
	}

}
