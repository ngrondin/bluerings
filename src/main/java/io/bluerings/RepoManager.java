package io.bluerings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.bluerings.FileWatcher.FileWatcherListener;
import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.exceptions.FunctionErrorException;
import io.firebus.exceptions.FunctionTimeoutException;

public class RepoManager implements FileWatcherListener {

	protected Agent agent;
	protected List<String> localFiles;
	protected FileWatcher fileWatcher;
	
	public RepoManager(Agent a) {
		agent = a;
		localFiles = new ArrayList<String>();
	}
	
	public void initiate() throws FunctionErrorException, FunctionTimeoutException, IOException {
		File dir = new File("repo");
		if(!dir.exists())
			dir.mkdir();
		File[] filesList = dir.listFiles();
		for (File file : filesList) {
		    if (file.isFile()) 
		    	registerFile(file);
		}
		fileWatcher = new FileWatcher(System.getProperty("user.dir") + "/repo", this);
	}
	
	protected void registerFile(File file) {
    	String filename = file.getName();
		System.out.println("Registering file " + filename);
		agent.getFirebus().registerStreamProvider("repo_" + filename, new FileStreamProvider(file), 10);
		localFiles.add(filename);
	}
	
	protected void deregisterFile(File file) {
    	String filename = file.getName();
		System.out.println("Deregistering file " + filename);
		//TODO add derigistration to firebus
		localFiles.remove(filename);
	}

	
	public boolean hasLocalFile(String name) {
		return localFiles.contains(name);
	}
	
	public void retrieveFile(String filename) {
		System.out.println("Getting file " + filename);
		try {
			StreamEndpoint sep = agent.getFirebus().requestStream("repo_" + filename, new Payload(), 10000);
			if(sep != null)
				sep.setHandler(new FileReceiverHandler(filename));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void fileModified(File file) {
		
	}

	public void fileCreated(File file) {
		registerFile(file);
	}

	public void fileDeleted(File file) {
		deregisterFile(file);
	}
}
