package io.bluerings;

public class ProcessInfo {
	public String name;
	public int instance;
	public ProcessHandle processHandle;
	public String command;
	public String nodeId;
	public boolean active;
	
	public ProcessInfo(String n, int i, String c, String nid, ProcessHandle ph) {
		name = n;
		instance = i;
		processHandle = ph;
		command = c;
		nodeId = nid;
	}

}
