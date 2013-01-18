package immibis.lavabukkit.asm;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.relauncher.IClassTransformer;

public class TransformingURLClassLoader extends URLClassLoader {
	public TransformingURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		
		transformers = new IClassTransformer[] {
			new NaughtyPluginTransformer(),
			new PluginReflectionTransformer()
		};
	}
	
	private IClassTransformer[] transformers;
	
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
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
		
		for(IClassTransformer t : transformers)
			bytes = t.transform(name, bytes);
		
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
