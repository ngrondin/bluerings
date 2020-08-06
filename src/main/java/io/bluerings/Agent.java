package io.bluerings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.firebus.Firebus;
import io.firebus.logging.FirebusSimpleFormatter;
import io.firebus.utils.DataMap;

public class Agent extends Thread {
	protected Firebus firebus;
	protected String nodeType;
	protected String agentId;
	protected RepoManager repoManager;
	protected ProcessManager processManager;
	protected ConfigManager configManager;
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
			firebus = new Firebus(fbp, "bluerings", "blueringspasswd");
			repoManager = new RepoManager(this);
			processManager = new ProcessManager(this);
			configManager = new ConfigManager(this);
			active = true;
			start();
		} catch(Exception e) {
			
		}
	}
	
	public void run() {
		try {
			initiate();
			configManager.initiate();
			repoManager.initiate();
			processManager.initiate();
		} catch(Exception e) {
			e.printStackTrace();
		}
		while(active) {
			try {
				processManager.manageProcesses();				
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {Thread.sleep(5000); } catch(Exception e) {}
		}
	}


	public void initiate() throws UnknownHostException {
		InetAddress inetAddress = InetAddress.getLocalHost();
		agentId = inetAddress.getHostName();
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
	
	

	
	public static void main(String[] args) {
		Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
		try
		{
			FileHandler fh = new FileHandler("Agent.log");
			fh.setFormatter(new FirebusSimpleFormatter());
			fh.setLevel(Level.FINEST);
			Logger logger = null;
			logger = Logger.getLogger("io.bluerings");
			logger.addHandler(fh);
			logger.setLevel(Level.FINEST);
			logger = Logger.getLogger("io.firebus");
			logger.addHandler(fh);
			logger.setLevel(Level.FINE);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
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
