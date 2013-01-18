package immibis.lavabukkit.tools.cbmapper;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class MethodObject extends MappableObject<MethodObject> {

	public static boolean includeNames;
	public static boolean includeIndices;
	public final ClassObject cl;
	public final MethodNode mn;
	public String[] params;
	public String rettype;
	
	private class MethodRef {
		int index;
		private String str;
		
		public MethodRef(int i) {
			index = i;
			str = "[MR" + index + " " + MethodObject.this + "]";
		}
		
		@Override
		public String toString() {
			return str;
		}
	}

	public MethodObject(ClassObject cl, MethodNode mn) {
		this.cl = cl;
		this.mn = mn;
		
		Type[] p = Type.getMethodType(mn.desc).getArgumentTypes();
		params = new String[p.length];
		for(int k = 0; k < params.length; k++)
			params[k] = p[k].getDescriptor();
		
		rettype = Type.getMethodType(mn.desc).getReturnType().getDescriptor();
	}
	
	@Override
	public void updateSelfSignature() {
		final StringBuilder sb = new StringBuilder();
		
		sb.append(mn.access);
		if(includeNames || mn.name.charAt(0) == '<') sb.append(mn.name);
		if(includeIndices) sb.append(" " + getIndex());
		sb.append(" ");
		sb.append(ClassSignature.getTypeIdentifier(rettype));
		for(String p : params)
			sb.append(ClassSignature.getTypeIdentifier(p));
		
		String firstPart = sb.toString();
		
		sb.setLength(0);
		
		mn.accept(new MethodVisitor(Opcodes.ASM4) {
			
			@Override
			public void visitInsn(int opcode) {
				sb.append(" ");
				sb.append(opcode);
			}
			
			@Override
			public void visitIntInsn(int opcode, int operand) {
				sb.append(" ");
				sb.append(opcode);
				sb.append("-");
				sb.append(operand);
			}
			
			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				sb.append(" ");
				sb.append(opcode);
				sb.append(ClassSignature.getMethodDescIdentifier(desc));
			}
			
			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				sb.append(" ");
				sb.append(opcode);
				sb.append(ClassSignature.getTypeIdentifier(desc));
			}
			
			@Override
			public void visitLdcInsn(Object cst) {
				if(cst instanceof String)
					sb.append((String)cst);
			}
		});
		
		selfSignature = firstPart + sb.toString().hashCode();
	}
	
	private int getIndex() {
		return cl.methods.indexOf(this);
	}

	public void addReferences() {
		cl.referenceSet.add(this);
		
		for(String s : params) {
			if(!s.startsWith("L"))
				break;
			ClassObject c = cl.set.classes.get(s.substring(1, s.length() - 1));
			if(c != null) {
				c.referenceSet.add(this);
				referenceSet.add(c);
			}
		}
		
		int k = 0;
		InsnList insnList = mn.instructions;
		Iterator<AbstractInsnNode> it = insnList.iterator();
		while(it.hasNext()) {
			AbstractInsnNode ain = it.next();
			if(ain instanceof MethodInsnNode) {
				MethodInsnNode n = (MethodInsnNode)ain;
				ClassObject co = cl.set.classes.get(n.owner);
				if(co != null) {
					co.referenceSet.add(new MethodRef(k++));
					referenceSet.add(co);
					MethodObject mo = co.getMethod(n.name, n.desc);
					if(mo != null) {
						mo.referenceSet.add(new MethodRef(k-1));
						referenceSet.add(mo);
					}
				}
			} else if(ain instanceof FieldInsnNode) {
				FieldInsnNode n = (FieldInsnNode)ain;
				ClassObject co = cl.set.classes.get(n.owner);
				if(co != null) {
					co.referenceSet.add(new MethodRef(k++));
					referenceSet.add(co);
					FieldObject mo = co.getField(n.name);
					if(mo != null) {
						mo.referenceSet.add(new MethodRef(k-1));
						referenceSet.add(mo);
					}
				}
			}
		}
		
		if(cl.getSuperclass() != null) {
			MethodObject overrides = cl.getSuperclass().getMethod(mn.name, mn.desc);
			if(overrides != null) {
				overrides.referenceSet.add(this);
				this.referenceSet.add(overrides);
			}
		}
		
		for(ClassObject i : cl.getInterfaces()) {
			MethodObject overrides = i.getMethod(mn.name, mn.desc);
			if(overrides != null) {
				overrides.referenceSet.add(this);
				this.referenceSet.add(overrides);
			}
		}
	}
	
	@Override
	public String toHumanString() {
		return "Method "+cl.cn.name+"/"+mn.name + mn.desc;
	}
}
