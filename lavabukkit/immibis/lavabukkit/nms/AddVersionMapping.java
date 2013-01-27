package immibis.lavabukkit.nms;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.Method;

public class AddVersionMapping implements Mapping {
	
	private final String version;
	
	public AddVersionMapping(String version) {
		this.version = version;
	}

	@Override
	public String mapClass(String c) {
		if(c.startsWith("org/bukkit/craftbukkit/"))
			return "org/bukkit/craftbukkit/" + version + "/" + c.substring(23);
		if(c.startsWith("net/minecraft/server/"))
			return "net/minecraft/server/" + version + "/" + c.substring(21);
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
