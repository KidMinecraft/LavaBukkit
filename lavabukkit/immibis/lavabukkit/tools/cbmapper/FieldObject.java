package immibis.lavabukkit.tools.cbmapper;

import org.objectweb.asm.tree.FieldNode;

public final class FieldObject extends MappableObject<FieldObject> {
	public static boolean includeNames;
	public final ClassObject cl;
	public final FieldNode fn;
	public String type;

	public FieldObject(ClassObject cl, FieldNode fn) {
		this.cl = cl;
		this.fn = fn;
		type = fn.desc;
	}
	
	@Override
	public void updateSelfSignature() {
		selfSignature = fn.access+" "+ClassSignature.getTypeIdentifier(type)+" "+ClassSignature.getTypeIdentifier(cl);
		if(includeNames)
			selfSignature += " " + fn.name;
	}
	
	@Override
	public String toHumanString() {
		return "Field "+cl.cn.name+"/"+fn.name+", desc "+fn.desc;
	}

}
