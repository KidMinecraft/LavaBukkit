package immibis.lavabukkit.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ASMUtils {
	public static void unboxAndReturn(MethodVisitor mv, Type requiredType) {
		switch(requiredType.getSort()) {
		case Type.VOID:
			mv.visitInsn(Opcodes.POP);
			mv.visitInsn(Opcodes.RETURN);
			break;
		case Type.BOOLEAN:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case Type.BYTE:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case Type.CHAR:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case Type.SHORT:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case Type.INT:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case Type.FLOAT:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
			mv.visitInsn(Opcodes.FRETURN);
			break;
		case Type.LONG:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
			mv.visitInsn(Opcodes.LRETURN);
			break;
		case Type.DOUBLE:
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
			mv.visitInsn(Opcodes.DRETURN);
			break;
		case Type.ARRAY:
			mv.visitTypeInsn(Opcodes.CHECKCAST, requiredType.getDescriptor());
			mv.visitInsn(Opcodes.ARETURN);
			break;
		case Type.OBJECT:
			mv.visitTypeInsn(Opcodes.CHECKCAST, requiredType.getInternalName());
			mv.visitInsn(Opcodes.ARETURN);
			break;
		default:
			throw new IllegalArgumentException("Invalid type for unboxing: "+requiredType);
		}
	}
}
