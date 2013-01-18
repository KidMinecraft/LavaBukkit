package immibis.lavabukkit.nms;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import cpw.mods.fml.relauncher.RelaunchClassLoader;

public class Mappings {
	public static Mapping CB_to_obf;
	public static Mapping MCP_to_obf;
	public static Mapping obf_to_current;
	public static Mapping CB_to_current;
	public static Mapping MCP_to_current;
	public static Mapping current_to_MCP;
	
	public static boolean RUNNING_IN_MCP;
	
	public static final String CURRENT_VERSION_CB = "1.4.6";
	public static final String CURRENT_VERSION_MCP = "1.4.7";
	
	static {
		try {
			RelaunchClassLoader cl = (RelaunchClassLoader)Mappings.class.getClassLoader();
			RUNNING_IN_MCP = cl.getSources().get(0).toString().endsWith("/");
			
			CB_to_obf = Mapping.fromResource("/immibis/lavabukkit/nms/" + CURRENT_VERSION_CB + "-cb.txt");
			MCP_to_obf = Mapping.fromResource("/immibis/lavabukkit/nms/" + CURRENT_VERSION_MCP + "-mcp.txt");
			
			if(RUNNING_IN_MCP)
				obf_to_current = Mapping.reverse(MCP_to_obf);
			else
				obf_to_current = Mapping.identity();
			
			CB_to_current = Mapping.join(CB_to_obf, obf_to_current);
			MCP_to_current = Mapping.join(MCP_to_obf, obf_to_current);
			
			current_to_MCP = Mapping.reverse(MCP_to_current);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
