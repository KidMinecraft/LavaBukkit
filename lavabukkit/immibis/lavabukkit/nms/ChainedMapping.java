package immibis.lavabukkit.nms;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.Method;

public class ChainedMapping implements Mapping {
	
	private final Mapping a, b;
	
	public ChainedMapping(Mapping a, Mapping b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public String mapClass(String c) {
		return b.mapClass(a.mapClass(c));
	}

	@Override
	public Field mapField(Field f) {
		return b.mapField(a.mapField(f));
	}

	@Override
	public Method mapMethod(Method m) {
		return b.mapMethod(a.mapMethod(m));
	}

	@Override
	public Mapping reverse() {
		return new ChainedMapping(b.reverse(), a.reverse());
	}

}
