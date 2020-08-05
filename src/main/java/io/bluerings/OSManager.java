package io.bluerings;

import java.io.IOException;
import java.util.List;

public abstract class OSManager {
	
	public abstract List<ProcessInfo> getAllProcesses() throws IOException;
	
	public abstract ProcessHandle startProcess(String path, String command) throws IOException;
	
}
