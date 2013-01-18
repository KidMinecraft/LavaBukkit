package immibis.lavabukkit.asm;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.CheckMethodAdapter;

import cpw.mods.fml.relauncher.IClassTransformer;

public abstract class ASMTransformerBase implements IClassTransformer {
	
	public boolean VERIFY_BYTECODE;
	
	public int getClassWriterFlags() {return 0;}

	public abstract ClassVisitor transform(String name, ClassVisitor parent);
	
	// The next ClassWriter to use; saves a very small amount of time and memory
	// since we need one before we know whether we'll actually use it or not
	// TODO: is this "optimization" actually useful?
	private ThreadLocal<ClassWriter> nextWriter = new ThreadLocal<ClassWriter>();
	
	@Override
	public final byte[] transform(String name, byte[] bytes) {
		if(bytes == null || name.startsWith("immibis.lavabukkit.asm."))
			return bytes;
		
		ClassWriter writer = nextWriter.get();
		nextWriter.set(null);
		if(writer == null)
			writer = new ClassWriter(getClassWriterFlags());
		
		//ClassVisitor visitor = transform(name, new CheckClassAdapter(writer, true));
		ClassVisitor visitor = transform(name, writer);
		
		if(visitor == writer) {
			// not doing any transformation
			nextWriter.set(writer);
			return bytes;
		}
		
		try {
			new ClassReader(bytes).accept(visitor, 0);
		} catch(Exception e) {
			throw new RuntimeException(getClass().getName()+" threw an exception while transforming "+name, e);
		}
		bytes = writer.toByteArray();
		
		if(VERIFY_BYTECODE) {
			try {
				CheckClassAdapter.verify(new ClassReader(bytes), true, new PrintWriter(new File("temp.txt")));
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return bytes;
	}

}
