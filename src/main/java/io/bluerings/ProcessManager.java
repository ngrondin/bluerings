package io.bluerings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import io.firebus.Payload;
import io.firebus.interfaces.Consumer;
import io.firebus.utils.DataException;
import io.firebus.utils.DataList;
import io.firebus.utils.DataMap;

public class ProcessManager extends Thread implements Consumer {
	protected Agent agent;
	protected List<ProcessInfo> managedProcesses;
	protected long lastPublished;
	protected boolean active;
	
	public ProcessManager(Agent a) {
		agent = a;
		managedProcesses = new ArrayList<ProcessInfo>();
		lastPublished = 0;
		active = true;
	}
	
	public void initiate() throws IOException, DataException {
		boolean changed = false; 
		File dir = new File("work");
		if(!dir.exists())
			dir.mkdir();
		File file = new File("work/localprocesses.json");
		if(file.exists()) {
			DataMap savedPI = new DataMap(new FileInputStream("work/localprocesses.json"));
			DataList list = savedPI.getList("processes");
			for(int i = 0; i < list.size(); i++) {
				DataMap spi = list.getObject(i);
				String name = spi.getString("name");
				int instance = spi.getNumber("instance").intValue();
				String cmd = spi.getString("command");
				String node = agent.getAgentId();
				Optional<ProcessHandle> oph = ProcessHandle.of(spi.getNumber("pid").longValue());
				ProcessHandle ph = null;
				if(oph.isPresent()) {
					ph = oph.get();
					ProcessInfo pi = new ProcessInfo(name, instance, cmd, node, ph);
					managedProcesses.add(pi);
				}
			}
		}
		agent.getFirebus().registerConsumer("processes", this, 10);
		publish();
		if(changed)
			saveLocalProcessInformation();
		start();
	}
	
	public void run() {
		while(active) {
			try {
				manageProcesses();
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {Thread.sleep(5000); } catch(Exception e) {}
		}		
	}

	
	protected void manageProcesses() throws IOException {
		boolean changed = false; 

		//Killing dead, process with modified configs or over processes 
		for(int i = 0; i < managedProcesses.size(); i++) {
			ProcessInfo mpi = managedProcesses.get(i);
			if(mpi.processHandle != null) {
				if(mpi.processHandle.isAlive()) {
					DataMap config = agent.getConfig().getObject("processes").getObject(mpi.name);
					if(config != null) {
						String command = config.getString("command");
						int expectedCount = config.getNumber("count").intValue();
						if(!command.equals(mpi.command)) {
							System.out.println("Process" + mpi.name + ", command has changed");
							killProcess(mpi);
							i--;
							changed = true;
						} else if(mpi.instance >= expectedCount) {
							System.out.println("Process" + mpi.name + ", too many instances");
							killProcess(mpi);
							i--;
							changed = true;
						}
					} else {
						System.out.println("Process " + mpi.name + ", configuration has disappeared");
						killProcess(mpi);
						i--;
						changed = true;
					}
				} else {
					System.out.println("Process " + mpi.name + " already dead");
					killProcess(mpi);
					i--;
					changed = true;
				}
			}
		}
		
		//Starting new processes id needed
		Iterator<String> it = agent.getConfig().getObject("processes").keySet().iterator();
		while(it.hasNext()) {
			String name = it.next();
			DataMap procCfg = agent.getConfig().getObject("processes").getObject(name);
			if(procCfg.getString("nodetype").equals(agent.getNodeType())) {
				int expectedCount = procCfg.getNumber("count").intValue();
				int maxPerNode = procCfg.getNumber("maxpernode").intValue(); 
				boolean hasAllFiles = true;
				DataList fileList = procCfg.getList("files");
				for(int i = 0; i < fileList.size(); i++) {
					String filename = fileList.getString(i);
					if(!agent.getRepoManager().hasLocalFile(filename)) {
						hasAllFiles = false;
						agent.getRepoManager().retrieveFile(filename);
					}
				}
				if(hasAllFiles) {
					List<ProcessInfo> localList = listLocalProcessInstances(name);
					List<ProcessInfo> clusterList = listProcessInstances(name);
					if(clusterList.size() < expectedCount && localList.size() < maxPerNode) {
						for(int instance = 0; instance < expectedCount; instance++) {
							ProcessInfo pi = getProcessInfo(name, instance);
							if(pi == null) {
								startProcess(name, instance, procCfg);
								changed = true;
							}
						}
						
					}
				}
			}
		}
		if(changed) {
			saveLocalProcessInformation();
			saveClusterProcessInformation();
		}
		if(lastPublished < System.currentTimeMillis() - 10000)
			publish();
	}
	
	protected void startProcess(String name, int instance, DataMap config) throws IOException {
		System.out.println("Starting process " + name + " instance " + instance);
		String command = config.getString("command");
		String rootDir = System.getProperty("user.dir");
		String workDir = rootDir + "/work";
		String instDir = workDir + "/" + name + "-" + instance;
		File dir = new File(instDir);
		if(dir.exists())
			deleteWorkDirectory(dir);
		try {Thread.sleep(1000);} catch(Exception e) {}
		boolean workDirCreated = dir.mkdir();
		if(workDirCreated == true) { 
			DataList list = config.getList("files");
			for(int i = 0; i < list.size(); i++) {
				String filename = list.getString(i);
				if(!(new File(instDir + "/" + filename)).exists())
					Files.createLink(Paths.get(instDir + "/" + filename), Paths.get(rootDir + "/repo/" + filename));
			}
			ProcessBuilder pb = new ProcessBuilder(command.split(" "));
			pb.directory(new File(instDir));
			Process p = pb.start();
			ProcessHandle ph = p.toHandle();
			agent.getLogManager().addStream(ph.pid() + "-s", p.getInputStream());
			agent.getLogManager().addStream(ph.pid() + "-e", p.getErrorStream());
			ProcessInfo pi = new ProcessInfo(name, instance, command, agent.getAgentId(), ph);
			managedProcesses.add(pi);
		} else {
			System.out.println("Working directory not created properly");
		}
	}
	
	protected void killProcess(ProcessInfo pi) {
		System.out.println("Killing process " + pi.name + " with pid " + pi.processHandle.pid());
		if(pi.processHandle != null && pi.processHandle.isAlive()) {
			pi.processHandle.destroyForcibly();
		}
		managedProcesses.remove(pi);
		agent.getLogManager().removeStream(pi.processHandle.pid() + "-s");
		agent.getLogManager().removeStream(pi.processHandle.pid() + "-e");
	}
	
	boolean deleteWorkDirectory(File dir) {
	    File[] allContents = dir.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	        	deleteWorkDirectory(file);
	        }
	    }
	    return dir.delete();
	}
	
	protected void publish() {
		DataMap pub = new DataMap();
		DataList list = new DataList();
		for(ProcessInfo pi: managedProcesses) {
			if(pi.processHandle != null) {
				DataMap spi = new DataMap();
				spi.put("name", pi.name);
				spi.put("instance", pi.instance);
				spi.put("command", pi.command);
				list.add(spi);
			}
		}
		pub.put("processes", list);
		pub.put("node", agent.getAgentId());
		agent.getFirebus().publish("processes", new Payload(pub.toString()));
		lastPublished = System.currentTimeMillis();
	}

	public void consume(Payload payload) {
		try {
			DataMap pub = new DataMap(payload.getString());
			String node = pub.getString("node");
			if(!node.equals(agent.getAgentId())) {
				for(int i = 0; i < managedProcesses.size(); i++) {
					ProcessInfo pi = managedProcesses.get(i);
					if(pi.nodeId.equals(node)) {
						managedProcesses.remove(i);
						i--;
					}
				}
				
				DataList list = pub.getList("processes");
				for(int i = 0; i < list.size(); i++) {
					DataMap proc = list.getObject(i);
					managedProcesses.add(new ProcessInfo(proc.getString("name"), proc.getNumber("instance").intValue(), proc.getString("command"), node, null));
				}
				
				saveClusterProcessInformation();
				if(lastPublished < System.currentTimeMillis() - 10000)
					publish();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected ProcessInfo getProcessInfo(String name, int instance) {
		for(ProcessInfo pi: managedProcesses) 
			if(pi.name.equals(name) && pi.instance == instance)
				return pi;
		return null;
	}
	
	protected List<ProcessInfo> listProcessInstances(String name) {
		List<ProcessInfo> list = new ArrayList<ProcessInfo>();
		for(ProcessInfo pi: managedProcesses) 
			if(pi.name.equals(name))
				list.add(pi);		
		return list;
	}
	
	protected List<ProcessInfo> listLocalProcessInstances(String name) {
		List<ProcessInfo> list = new ArrayList<ProcessInfo>();
		for(ProcessInfo pi: managedProcesses) 
			if(pi.name.equals(name) && pi.processHandle != null)
				list.add(pi);		
		return list;
	}	
	
	protected void saveLocalProcessInformation() {
		DataMap savedPI = new DataMap();
		DataList list = new DataList();
		for(ProcessInfo pi: managedProcesses) {
			if(pi.processHandle != null) {
				DataMap spi = new DataMap();
				spi.put("name", pi.name);
				spi.put("instance", pi.instance);
				spi.put("command", pi.command);
				spi.put("pid", pi.processHandle.pid());
				list.add(spi);
			}
		}
		savedPI.put("processes", list);
		try {
			FileOutputStream fos = new FileOutputStream("work/localprocesses.json");
			fos.write(savedPI.toString().getBytes());
			fos.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void saveClusterProcessInformation() {
		try {
			DataMap map = createClusterMap();
			FileOutputStream fos = new FileOutputStream("work/cluster.json");
			fos.write(map.toString().getBytes());
			fos.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected DataMap createClusterMap() {
		DataMap pub = new DataMap();
		for(ProcessInfo pi: managedProcesses) {
			DataMap nameBloc = pub.getObject(pi.name);
			if(nameBloc == null) {
				nameBloc = new DataMap();
				pub.put(pi.name, nameBloc);
			}
			String instanceStr = Integer.toString(pi.instance);
			DataMap instBloc = nameBloc.getObject(instanceStr);
			if(instBloc == null) {
				instBloc = new DataMap();
				nameBloc.put(instanceStr, instBloc);
			}
			instBloc.put("command", pi.command);
			instBloc.put("node", pi.nodeId);
		}
		return pub;
	}	
}
