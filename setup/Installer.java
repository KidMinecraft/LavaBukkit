import java.io.*;
import java.net.*;
import java.util.zip.*;

public class Installer {
static void download(String url, String path) throws Exception {
	if(new File(path).exists()) return;
	System.out.print("downloading "+url);
	InputStream in = new URL(url).openStream();
	FileOutputStream out = new FileOutputStream(path);
	byte[] buffer = new byte[65536];
	int total = 0;
	int interval = 256*1024;
	int next = interval;
	while(true) {
		int read = in.read(buffer);
		if(read < 0) break;
		total += read;
		while(total >= next) {
			next += interval;
			System.out.print(".");
		}
		out.write(buffer, 0, read);
	}
	System.out.println();
	in.close();
	out.close();
}
static void extract(String zip, String path) throws Exception {
	System.out.println("extracting "+zip+" to "+path);
	ZipInputStream in = new ZipInputStream(new FileInputStream(zip));
	ZipEntry ze;
	while((ze = in.getNextEntry()) != null) {
		if(ze.isDirectory())
			continue;
		File f = new File(new File(path), ze.getName());
		File p = f.getParentFile();
		if(!p.exists() && !p.mkdirs())
			throw new IOException("Failed to create directory: "+p);
		OutputStream out = new FileOutputStream(f);
		copystream(in, out);
		out.close();
		in.closeEntry();
	}
	in.close();
}
static byte[] buffer = new byte[65536];
static void copystream(InputStream in, OutputStream out) throws Exception {
	while(true) {
		int read = in.read(buffer);
		if(read < 0) break;
		out.write(buffer, 0, read);
	}
}
static void downloadcached(String url, String cache, String dest) throws Exception {
	download(url, cache);
	File p = new File(dest).getParentFile();
	if(!p.exists() && !p.mkdirs())
		throw new IOException("Failed to create directory: "+p);
	System.out.println("copying "+cache+" to "+dest);
	OutputStream out = new FileOutputStream(dest);
	InputStream in = new FileInputStream(cache);
	copystream(in, out);
	in.close();
	out.close();
}
static void delete(String path) throws Exception {
	File f = new File(path);
	if(f.exists()) {
		System.out.println("deleting "+path);
		f.delete();
	}
}
public static void main(String[] args) throws Exception {
	String os = args.length > 0 ? args[0] : "unknown";

	String forge_install_cmd;

	if(os.equals("windows")) {
		forge_install_cmd = "cmd /c install.cmd 2>&1";
	} else {
		System.err.println("invalid os specified on command line");
		return;
	}

	String mcp = "mcp726a.zip";
	String forge = "minecraftforge-src-1.4.7-6.6.0.499.zip";
	String minecraft = "1.4.7";

	String cache_mcp = "cache/" + mcp;
	String cache_forge = "cache/" + forge;
	String cache_mc = "cache/minecraft-"+minecraft+".jar";
	String cache_mcs = "cache/minecraft_server-"+minecraft+".jar";

	download("http://mcp.ocean-labs.de/files/"+mcp, cache_mcp);
	download("http://files.minecraftforge.net/minecraftforge/"+forge, cache_forge);

	delete("../mcp/jars/minecraft_server.jar.backup");
	delete("../mcp/jars/bin/minecraft.jar.backup");

	downloadcached("http://assets.minecraft.net/"+minecraft.replace('.','_')+"/minecraft.jar", cache_mc, "../mcp/jars/bin/minecraft.jar");
	downloadcached("http://assets.minecraft.net/"+minecraft.replace('.','_')+"/minecraft_server.jar", cache_mcs, "../mcp/jars/minecraft_server.jar");

	//extract(cache_mcp, "../mcp");
	//extract(cache_forge, "../mcp"); // using ../mcp/forge results in an extra nested "forge" folder

	for(String s : new String[] {"lwjgl.jar", "jinput.jar", "lwjgl_util.jar"})
		downloadcached("http://s3.amazonaws.com/MinecraftDownload/"+s, "cache/"+s, "../mcp/jars/bin/"+s);

	System.out.println("running forge setup");
	final Process p = Runtime.getRuntime().exec(forge_install_cmd, null, new File("../mcp/forge"));

	new StreamGobbler(p.getInputStream()).start();
	new StreamGobbler(p.getErrorStream()).start();
	OutputStream out = p.getOutputStream();
	out.write("Yes\n".getBytes());
	out.close();
}
}

class StreamGobbler extends Thread {
	InputStream in;
	StreamGobbler(InputStream in) {this.in = in;}
	public void run() {
		try {
			Installer.copystream(in, System.out);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while(true) {
				String s = br.readLine();
				if(s == null) break;
				System.out.println(s);
			}
			br.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
