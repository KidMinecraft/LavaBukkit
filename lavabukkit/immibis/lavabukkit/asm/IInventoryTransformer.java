package immibis.lavabukkit.asm;

import immibis.lavabukkit.nms.Mappings;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class IInventoryTransformer extends ASMTransformerBase {

	private Method getMaxStackSize = Mappings.MCP_to_current.mapMethod(new Method("net/minecraft/inventory/IInventory", "getInventoryStackLimit", "()I"));
	
	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		
		// Note: We add __lbMaxStackSize to ANY class which has a getMaxStackSize method
		// regardless of whether it implements IInventory.
		// It will never get set for non-IInventory objects.
		
		return new ClassVisitor(Opcodes.ASM4, parent) {
			
			private String classInternalName;
			private boolean isInterface;
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				
				classInternalName = name;
				
				isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
				
				super.visit(version, access, name, signature, superName, interfaces);
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				
				final ClassVisitor cv = this;
				
				if(name.equals(getMaxStackSize.name) && desc.equals(getMaxStackSize.desc) && !isInterface && (access & Opcodes.ACC_STATIC) == 0)
					mv = new MethodVisitor(Opcodes.ASM4, mv) {
						@Override
						public void visitEnd() {
							super.visitEnd();
							
							cv.visitField(Opcodes.ACC_PUBLIC, "__lbMaxStackSize", "I", null, 0);
						}
						
						@Override
						public void visitCode() {
							
							super.visitCode();
							// if(__lbMaxStackSize > 0) return __lbMaxStackSize;
							
							Label skipLabel = new Label();
							super.visitVarInsn(Opcodes.ALOAD, 0);
							super.visitFieldInsn(Opcodes.GETFIELD, classInternalName, "__lbMaxStackSize", "I");
							super.visitInsn(Opcodes.ICONST_0);
							super.visitJumpInsn(Opcodes.IF_ICMPLE, skipLabel);
							super.visitVarInsn(Opcodes.ALOAD, 0);
								super.visitFieldInsn(Opcodes.GETFIELD, classInternalName, "__lbMaxStackSize", "I");
								super.visitInsn(Opcodes.IRETURN);
							super.visitLabel(skipLabel);
							super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
						}
						
						@Override
						public void visitMaxs(int maxStack, int maxLocals) {
							if(maxStack < 2)
								maxStack = 2;
							
							super.visitMaxs(maxStack, maxLocals);
						}
					};
				return mv;
			}
		};
	}

}
