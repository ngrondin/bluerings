package io.bluerings;

import java.net.InetAddress;
import java.net.UnknownHostException;
//import java.util.logging.FileHandler;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import io.firebus.Firebus;
import io.firebus.utils.DataMap;

public class Agent extends Thread {
	protected Firebus firebus;
	protected String nodeType;
	protected String agentId;
	protected RepoManager repoManager;
	protected ProcessManager processManager;
	protected ConfigManager configManager;
	protected LogManager logManager;
	protected boolean active;
	
	protected static int OS_WINDOWS = 1;
	protected static int OS_LINUX = 2;
	
	public Agent(String nt, int fbp) {
		nodeType = nt;
		if(nodeType == null)
			nodeType = "gen";
		System.out.println("Starting Bluerings agent of type '" + nodeType + "'");
		if(fbp != 0)
			System.out.println("Firebus port set to " + fbp);
		try {
			active = true;
			firebus = new Firebus(fbp, "bluerings", "blueringspasswd");
			repoManager = new RepoManager(this);
			configManager = new ConfigManager(this);
			if(!nodeType.equals("admin")) {
				processManager = new ProcessManager(this);
				logManager = new LogManager(this);
			}
			initiate();
			repoManager.initiate();
			configManager.initiate();
			if(!nodeType.equals("admin")) {
				processManager.initiate();
				logManager.initiate();
			}
		} catch(Exception e) {
			
		}
	}
	
	public void initiate() throws UnknownHostException {
		InetAddress inetAddress = InetAddress.getLocalHost();
		agentId = inetAddress.getHostName();
		System.out.println("Agent id is '" + agentId + "'");
	}

	public void addKnownAddress(String a, int p) {
		firebus.addKnownNodeAddress(a, p);
	}
	
	public DataMap getConfig() {
		return configManager.getConfig();
	}
	
	public Firebus getFirebus() {
		return firebus;
	}
	
	public String getNodeType() {
		return nodeType;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public RepoManager getRepoManager() {
		return repoManager;
	}
	
	public LogManager getLogManager() {
		return logManager;
	}
	

	
	public static void main(String[] args) {
		String nodeType = null;
		int fbPort = 0;
		String ka = null;
		for(int i = 0; i < args.length; i++) {
			String sw = args[i];
			if(sw.equals("-nt") && args.length > i + 1) {
				nodeType = args[i + 1];
				i++;
			} else if(sw.equals("-fbp") && args.length > i + 1) {
				fbPort = Integer.parseInt(args[i + 1]);
				i++;
			} else if(sw.equals("-fbka") && args.length > i + 1) {
				ka = args[i + 1];
				i++;
			}
		}
		if(nodeType == null) {
			nodeType = System.getenv("BLUERINGS_NODETYPE");
		}
		Agent agent = new Agent(nodeType, fbPort);
		if(ka != null) {
			String[] parts = ka.split(":");
			agent.addKnownAddress(parts[0], Integer.parseInt(parts[1]));
		}
		
	}
}
