package io.bluerings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.ConsoleHandler;
//import java.util.logging.FileHandler;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
		active = true;
		nodeType = nt;
		if(nodeType == null)
			nodeType = "gen";
		System.out.println("Bluerings agent is of type '" + nodeType + "'");
		if(fbp != 0)
			System.out.println("Firebus port set to " + fbp);
		try {
			firebus = new Firebus(fbp, "bluerings", "blueringspasswd");
			repoManager = new RepoManager(this);
			configManager = new ConfigManager(this);
			processManager = new ProcessManager(this);
			logManager = new LogManager(this);
			InetAddress inetAddress = InetAddress.getLocalHost();
			agentId = inetAddress.getHostName();
			System.out.println("Agent id is '" + agentId + "'");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void initiate() {
		try {
			long start = System.currentTimeMillis();
			while(System.currentTimeMillis() < (start + 10000) && !firebus.hasConnections())
				Thread.sleep(1000);
			repoManager.initiate();
			configManager.initiate();
			processManager.initiate();
			logManager.initiate();
		} catch(Exception e) {
			e.printStackTrace();
		}
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
		Logger logger = Logger.getLogger("io.firebus");
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new Formatter() {
			public String format(LogRecord rec) {
				return rec.getMessage() + "\r\n";
			}
		});
		handler.setLevel(Level.FINE);
		logger.addHandler(handler);
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.FINE);
		
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
		agent.initiate();
		
	}
}
