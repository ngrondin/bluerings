package io.bluerings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class LinuxManager extends OSManager {

	public List<ProcessInfo> getAllProcesses() throws IOException {
		List<ProcessInfo> list = new ArrayList<ProcessInfo>();
	    /*String line;
	    String cmd = "ps -eo pid,pcpu,rss,cmd";
	    Process p = Runtime.getRuntime().exec(cmd);
	    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    line = input.readLine();
	    int pos1 = line.indexOf("%CPU");
	    int pos2 = line.indexOf("RSS");
	    int pos3 = line.indexOf("CMD");
	    
	    while ((line = input.readLine()) != null) {
	    	if(line.length() > pos3) {
		    	String pid = line.substring(0, pos1 - 1).trim();
		    	String pcpu = line.substring(pos1, pos2 - 1).trim();
		    	String mem = line.substring(pos2, pos3 - 1).trim();
		    	String command = line.substring(pos3).trim().replace("  ", " ");
		    	list.add(new ProcessInfo(null, 0, Long.valueOf(pid), command, Long.valueOf(mem), Double.valueOf(pcpu)));
	    	}
	    }
	    input.close();*/
	    return list;
	}

	@Override
	public ProcessHandle startProcess(String path, String command) throws IOException {
		Process p = Runtime.getRuntime().exec(command);
		long pid = 0;
		try {
	        Field f = p.getClass().getDeclaredField("pid");
	        f.setAccessible(true);
	        pid = f.getLong(p);
	        f.setAccessible(false);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}


}
