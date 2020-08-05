package io.bluerings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WindowsManager extends OSManager {

	public List<ProcessInfo> getAllProcesses() throws IOException {
		List<ProcessInfo> list = new ArrayList<ProcessInfo>();
	    /*String line;
	    String cmd = "wmic process get CommandLine, ProcessId, WorkingSetSize ";
	    Process p = Runtime.getRuntime().exec(cmd);
	    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    line = input.readLine();
	    int pos1 = line.indexOf("ProcessId");
	    int pos2 = line.indexOf("WorkingSetSize");
	    
	    while ((line = input.readLine()) != null) {
	    	if(line.length() > pos2) {
		    	String command = line.substring(0, pos1 - 1).trim().replace("  ", " ");
		    	String pid = line.substring(pos1, pos2 - 1).trim();
		    	String mem = line.substring(pos2).trim();
	    		list.add(new ProcessInfo(null, 0, Long.valueOf(pid), command, Long.valueOf(mem), 0));
	    	}
	    }
	    input.close();*/
	    return list;
	}

	public ProcessHandle startProcess(String path, String command) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.directory(new File(path));
		Process p = pb.start();
		return p.toHandle();
	}


}
