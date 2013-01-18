package immibis.lavabukkit.tools.cbmapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.BiMap;

public final class ClassObject extends MappableObject<ClassObject> {
	public ClassObject(ClassSet set, byte[] data) {
		this.set = set;
		cn = new ClassNode();
		new ClassReader(data).accept(cn, 0);
		sig = new ClassSignature(cn);
		
		for(FieldNode fn : (List<FieldNode>)cn.fields)
			fields.add(new FieldObject(this, fn));
		
		for(MethodNode mn : (List<MethodNode>)cn.methods)
			methods.add(new MethodObject(this, mn));
	}
	
	
	@Override
	public void updateSelfSignature() {
		selfSignature = sig.toString();
	}
	
	@Override
	public void addReferences() {
		for(MethodObject m : methods) m.referenceSet.add("member-"+this);
		for(FieldObject f : fields) f.referenceSet.add("member-"+this);
		ClassObject s = getSuperclass();
		if(s != null)
			s.referenceSet.add("subcl-"+this);
		for(String in : (Iterable<String>)cn.interfaces) {
			s = set.classes.get(in);
			if(s != null)
				s.referenceSet.add("impl-"+this);
		}
	}
	
	@Override
	public String toHumanString() {
		return "Class "+cn.name;
	}
	
	public ClassSet set;
	public ClassNode cn;
	public ClassSignature sig;
	
	public List<FieldObject> fields = new ArrayList<FieldObject>();
	public List<MethodObject> methods = new ArrayList<MethodObject>();
	
	public MethodObject getMethod(String name, String desc) {
		for(MethodObject m : methods)
			if(m.mn.name.equals(name) && m.mn.desc.equals(desc))
				return m;
		return null;
	}


	public FieldObject getField(String name) {
		for(FieldObject m : fields)
			if(m.fn.name.equals(name))
				return m;
		return null;
	}
	
	public ClassObject getSuperclass() {
		return set.classes.get(cn.superName);
	}


	public List<ClassObject> getInterfaces() {
		if(cn.interfaces.size() == 0)
			return Collections.emptyList();
		ArrayList<ClassObject> rv = new ArrayList<ClassObject>(cn.interfaces.size());
		for(String in : (Iterable<String>)cn.interfaces) {
			ClassObject s = set.classes.get(in);
			if(s != null)
				rv.add(s);
		}
		return rv;
	}

}
