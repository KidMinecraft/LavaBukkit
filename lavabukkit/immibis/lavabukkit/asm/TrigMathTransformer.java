package immibis.lavabukkit.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TrigMathTransformer extends ASMTransformerBase {
	public ClassVisitor transform(String name, ClassVisitor parent) {
		if(!name.startsWith("net.minecraft.") && name.contains("."))
			return parent; // only affect base classes
		
		return new ClassVisitor(Opcodes.ASM4, parent) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MethodVisitor(Opcodes.ASM4, super.visitMethod(access, name, desc, signature, exceptions)) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						if(opcode == Opcodes.INVOKESTATIC && owner.equals("java/lang/Math") && (name.equals("atan") || name.equals("atan2")))
							owner = "org/bukkit/craftbukkit/TrigMath";
						super.visitMethodInsn(opcode, owner, name, desc);
					}
				};
			}
		};
	}
}
