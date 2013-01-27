package immibis.lavabukkit.nms;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.Method;

public class IdentityMapping implements Mapping {

	@Override
	public String mapClass(String c) {
		return c;
	}

	@Override
	public Field mapField(Field f) {
		return f;
	}

	@Override
	public Method mapMethod(Method m) {
		return m;
	}
	
	@Override
	public Mapping reverse() {
		return this;
	}

}
