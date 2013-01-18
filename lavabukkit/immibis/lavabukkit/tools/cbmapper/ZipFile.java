package immibis.lavabukkit.tools.cbmapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFile {
	public Map<String, byte[]> files = new HashMap<String, byte[]>();
	
	public ZipFile(InputStream in) throws IOException {
		ZipInputStream zin = new ZipInputStream(in);
		
		byte[] buffer = new byte[32768];
		
		try {
			ZipEntry ze;
			while((ze = zin.getNextEntry()) != null) {
				ByteArrayOutputStream temp = new ByteArrayOutputStream(ze.getSize() < 0 ? 16384 : (int)ze.getSize());
				
				while(true) {
					int read = zin.read(buffer);
					if(read < 0)
						break;
					temp.write(buffer, 0, read);
				}
				
				files.put(ze.getName(), temp.toByteArray());
			}
			
		} finally {
			zin.close();
		}
	}
}
