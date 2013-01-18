package immibis.lavabukkit.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DebuggingMethodVisitor extends MethodVisitor {

	public DebuggingMethodVisitor(MethodVisitor parent) {
		super(Opcodes.ASM4, parent);
	}
	
	@Override
	public void visitCode() {
		System.err.println("visitCode");
		super.visitCode();
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		System.err.println("visitAnnotation("+desc+","+visible+")");
		return super.visitAnnotation(desc, visible);
	}
	
	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		System.err.println("visitAnnotationDefault");
		return super.visitAnnotationDefault();
	}
	
	@Override
	public void visitAttribute(Attribute attr) {
		System.err.println("visitAttribute");
		super.visitAttribute(attr);
	}
	
	@Override
	public void visitEnd() {
		System.err.println("visitEnd");
		super.visitEnd();
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		System.err.println("visitFieldInsn "+opcode+" "+owner+"/"+name+" "+desc);
		super.visitFieldInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack,
			Object[] stack) {
		System.err.println("visitFrame "+type+" "+nLocal+" "+nStack);
		super.visitFrame(type, nLocal, local, nStack, stack);
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		System.err.println("visitIincInsn "+var+" "+increment);
		super.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitInsn(int opcode) {
		System.out.println("visitInsn "+opcode);
		super.visitInsn(opcode);
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		System.out.println("visitIntInsn "+opcode+" "+operand);
		super.visitIntInsn(opcode, operand);
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
			Object... bsmArgs) {
		System.out.println("visitInvokeDynamicInsn");
		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		System.out.println("visitJumpInsn "+opcode);
		super.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLabel(Label label) {
		System.out.println("visitLabel");
		super.visitLabel(label);
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		System.out.println("visitLdcInsn "+cst);
		super.visitLdcInsn(cst);
	}
	
	@Override
	public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, start);
	}
	
	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		super.visitLocalVariable(name, desc, signature, start, end, index);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		System.out.println("visitLookupSwitchInsn");
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		System.out.println("visitMaxs "+maxStack+" "+maxLocals);
		super.visitMaxs(maxStack, maxLocals);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc) {
		System.out.println("visitMethodInsn "+opcode+" "+owner+"/"+name+desc);
		super.visitMethodInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		System.out.println("visitMultiANewArrayInsn "+desc+" "+dims);
		super.visitMultiANewArrayInsn(desc, dims);
	}
	
	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter,
			String desc, boolean visible) {
		System.out.println("visitParameterAnnotation "+parameter+" "+desc+" "+visible);
		return super.visitParameterAnnotation(parameter, desc, visible);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt,
			Label... labels) {
		System.out.println("visitTableSwitchInsn");
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler,
			String type) {
		super.visitTryCatchBlock(start, end, handler, type);
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		System.out.println("visitTypeInsn "+opcode+" "+type);
		super.visitTypeInsn(opcode, type);
	}
	 
	 @Override
	public void visitVarInsn(int opcode, int var) {
		System.out.println("visitVarInsn "+opcode+" "+var);
		super.visitVarInsn(opcode, var);
	}

}
