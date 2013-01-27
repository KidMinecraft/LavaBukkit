package immibis.lavabukkit.asm;


import immibis.lavabukkit.nms.IClassAccessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.relauncher.IClassTransformer;
import cpw.mods.fml.relauncher.RelaunchClassLoader;

public class TransformingURLClassLoader extends URLClassLoader {
	public TransformingURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		
		transformers = new IClassTransformer[] {
			new NaughtyPluginTransformer(this),
			new PluginReflectionTransformer(this)
		};
	}
	
	public final IClassAccessor ACCESSOR = new IClassAccessor() {
		public byte[] getUntransformedClassBytes(String name) throws ClassNotFoundException {
			try {
				return TransformingURLClassLoader.this.getUntransformedClassBytes(name);
			} catch(ClassNotFoundException e) {
				RelaunchClassLoader cl = (RelaunchClassLoader)HookInsertionTransformer.class.getClassLoader();
				try {
					byte[] b = cl.getClassBytes(name);
					if(b == null)
						throw new ClassNotFoundException(name);
					else
						return b;
				} catch(IOException e2) {
					throw new ClassNotFoundException(name, e2);
				}
			}
		}
	};
	
	private IClassTransformer[] transformers;
	
	public byte[] getUntransformedClassBytes(String name) throws ClassNotFoundException {
		InputStream stream = getResourceAsStream(name.replace(".", "/")+".class");
		if(stream == null)
			throw new ClassNotFoundException(name);
		
		byte[] bytes;
		try {
			bytes = readFully(stream);
			stream.close();
		} catch(IOException e) {
			throw new ClassNotFoundException(name, e);
		}
		
		return bytes;
	}
	
	public byte[] getTransformedClassBytes(String name) throws ClassNotFoundException {
		byte[] bytes = getUntransformedClassBytes(name);
		
		for(IClassTransformer t : transformers)
			bytes = t.transform(name, bytes);
		
		return bytes;
	}
	
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytes = getTransformedClassBytes(name);
		
		return defineClass(name, bytes, 0, bytes.length);
	}
	
    private byte[] readFully(InputStream stream) throws IOException
    {
    	byte[] buffer = new byte[32768];
        ByteArrayOutputStream bos = new ByteArrayOutputStream(stream.available());
        int len;
        while ((len = stream.read(buffer)) >= 0)
            bos.write(buffer, 0, len);

        return bos.toByteArray();
    }
}
