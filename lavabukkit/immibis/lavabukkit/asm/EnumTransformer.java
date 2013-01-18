package immibis.lavabukkit.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cpw.mods.fml.relauncher.IClassTransformer;

// Allows new values to be added to enums at load time
public class EnumTransformer implements IClassTransformer {

	public class EnumTransformingVisitor extends ClassVisitor {
		public EnumTransformingVisitor(ClassVisitor parent) {
			super(Opcodes.ASM4, parent);
		}
		
		/* Add the following code to the start of each class:
		 *    private static final java.util.List __fbValues;
		 * 
		 * At the start of the static initializer:
		 *    __fbValues = new java.util.ArrayList();
		 *    
		 * Before static initializer returns:
		 *    ENUM$VALUES = null;
		 * 
		 * After calling superclass constructor:
		 *    __fbValues.add(this);
		 *    ENUM$VALUES = null;
		 * 
		 * At start of values():
		 *    if(ENUM$VALUES == null)
		 *        ENUM$VALUES = (<class>[])__fbValues.toArray(new <class>[__fbValues.size()]);
		 * 
		 * For org.bukkit.Material, increase the size of byID to 32768.
		 * (which is the second ANEWARRAY opcode in <clinit>)
		 */
		
		private String classInternalName;
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			
			classInternalName = name;
			
			// private static final java.util.List __fbValues;
			FieldVisitor fv = super.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "__fbValues", "Ljava/util/List;", null, null);
			if(fv != null)
				fv.visitEnd();
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			
			if(name.equals("<clinit>"))
				mv = new MethodVisitor(Opcodes.ASM4, mv) {
					@Override
					public void visitCode() {
						super.visitCode();
						
						// __fbValues = new java.util.ArrayList();
						super.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
						super.visitInsn(Opcodes.DUP);
						super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
						super.visitFieldInsn(Opcodes.PUTSTATIC, classInternalName, "__fbValues", "Ljava/util/List;");
					}
					
					private int numANEWARRAY = 0;
					
					@Override
					public void visitTypeInsn(int opcode, String type) {
						if(opcode == Opcodes.ANEWARRAY) {
							numANEWARRAY++;
							if(numANEWARRAY == 2 && classInternalName.equals("org/bukkit/Material")) {
								super.visitInsn(Opcodes.POP);
								super.visitLdcInsn(32768);
							}
						}
						super.visitTypeInsn(opcode, type);
					}
					
					@Override
					public void visitInsn(int opcode) {
						if(opcode == Opcodes.RETURN) {
							// this.ENUM$VALUES = null;
							super.visitInsn(Opcodes.ACONST_NULL);
							super.visitFieldInsn(Opcodes.PUTSTATIC, classInternalName, "ENUM$VALUES", "[L"+classInternalName+";");
						}
						
						super.visitInsn(opcode);
					}
				};
				
			if(name.equals("<init>"))
				mv = new MethodVisitor(Opcodes.V1_7, mv) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						super.visitMethodInsn(opcode, owner, name, desc);
						
						if(owner.equals("java/lang/Enum") && name.equals("<init>")) {
							// __fbValues.add(this);
							super.visitFieldInsn(Opcodes.GETSTATIC, classInternalName, "__fbValues", "Ljava/util/List;");
							super.visitVarInsn(Opcodes.ALOAD, 0);
							super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
							
							// this.ENUM$VALUES = null;
							super.visitInsn(Opcodes.ACONST_NULL);
							super.visitFieldInsn(Opcodes.PUTSTATIC, classInternalName, "ENUM$VALUES", "[L"+classInternalName+";");
						}
					}
				};
				
			if(name.equals("values") && desc.equals("()[L" + classInternalName + ";"))
				mv = new MethodVisitor(Opcodes.V1_7, mv) {
					@Override
					public void visitCode() {
						super.visitCode();
						
						Label skipLabel = new Label();
						
						// if(ENUM$VALUES == null) {
						super.visitFieldInsn(Opcodes.GETSTATIC, classInternalName, "ENUM$VALUES", "[L"+classInternalName+";");
						super.visitJumpInsn(Opcodes.IFNONNULL, skipLabel);
						
						//    ENUM$VALUES = (<class>[])__fbValues.toArray(new <class>[__fbValues.size()]);
						super.visitFieldInsn(Opcodes.GETSTATIC, classInternalName, "__fbValues", "Ljava/util/List;");
						super.visitFieldInsn(Opcodes.GETSTATIC, classInternalName, "__fbValues", "Ljava/util/List;");
						super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I");
						super.visitTypeInsn(Opcodes.ANEWARRAY, classInternalName);
						super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
						super.visitTypeInsn(Opcodes.CHECKCAST, "[L" + classInternalName + ";");
						super.visitFieldInsn(Opcodes.PUTSTATIC, classInternalName, "ENUM$VALUES", "[L" + classInternalName + ";");
						
						// }
						super.visitLabel(skipLabel);
					}
				};
			
			return mv;
		}
	}

	@Override
	public byte[] transform(String name, byte[] bytes) {
		if(bytes == null || !name.equals("org.bukkit.Material"))
			return bytes;
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		ClassReader reader = new ClassReader(bytes);
		ClassVisitor visitor = new EnumTransformingVisitor(writer);
		
		if(name.equals("org.bukkit.Material")) {
			visitor = new ClassVisitor(Opcodes.ASM4, visitor) {
				@Override
				public void visitEnd() {
					// public static void __fbCreateInstance(String a, int b, int c, int d, int e, Class f) {
					//		new Material(a, b, c, d, e, f);
					// }
					MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "__fbCreateInstance", "(Ljava/lang/String;IIIILjava/lang/Class;)V", null, new String[0]);
					if(mv != null) {
						mv.visitCode();
						mv.visitTypeInsn(Opcodes.NEW, "org/bukkit/Material");
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitVarInsn(Opcodes.ILOAD, 1);
						mv.visitVarInsn(Opcodes.ILOAD, 2);
						mv.visitVarInsn(Opcodes.ILOAD, 3);
						mv.visitVarInsn(Opcodes.ILOAD, 4);
						mv.visitVarInsn(Opcodes.ALOAD, 5);
						mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/bukkit/Material", "<init>", "(Ljava/lang/String;IIIILjava/lang/Class;)V");
						mv.visitInsn(Opcodes.RETURN);
						mv.visitMaxs(0, 0);
						mv.visitEnd();
					}
					
					super.visitEnd();
				}
			};
		}
		
		reader.accept(visitor, 0);
		bytes = writer.toByteArray();
		return bytes;
	}

}
