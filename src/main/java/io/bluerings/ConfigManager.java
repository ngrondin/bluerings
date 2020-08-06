package io.bluerings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.bluerings.FileWatcher.FileWatcherListener;
import io.firebus.Payload;
import io.firebus.exceptions.FunctionErrorException;
import io.firebus.exceptions.FunctionTimeoutException;
import io.firebus.information.ServiceInformation;
import io.firebus.interfaces.Consumer;
import io.firebus.interfaces.ServiceProvider;
import io.firebus.utils.DataException;
import io.firebus.utils.DataMap;

public class ConfigManager implements FileWatcherListener, ServiceProvider, Consumer {

	protected Agent agent;
	protected DataMap config;
	protected FileWatcher fileWatcher;
	
	public ConfigManager(Agent a) {
		agent = a;
		config = new DataMap("processes", new DataMap());
	}
	
	public void initiate() throws IOException, FunctionErrorException, DataException {
		if(agent.getNodeType().equals("admin")) {
			try {
				System.out.println("Admin node, using local config");
				config = new DataMap(new FileInputStream("config.json"));
			} catch (IOException e1) {
				System.out.println("No local config, using blank");
				saveConfig();
			}			
		} else {
			try {
				System.out.println("Calling for config");
				Payload resp = agent.getFirebus().requestService("getconfig", null, 20000);
				receivedConfig(resp);
			} catch(FunctionTimeoutException e) {
				System.out.println("No other configs available, using local one");
				try {
					config = new DataMap(new FileInputStream("config.json"));
				} catch (IOException e1) {
					System.out.println("No local config, using blank");
					saveConfig();
				}
			}
		}
		fileWatcher = new FileWatcher(System.getProperty("user.dir"), this);
		agent.getFirebus().registerServiceProvider("getconfig", this, 10);
		agent.getFirebus().registerConsumer("config", this, 10);
	}
	
	protected void receivedConfig(Payload payload) throws DataException, IOException {
		DataMap newConfig = new DataMap(payload.getString());
		if(config == null || (config != null && !newConfig.toString().equals(config.toString()))) {
			System.out.println("Received a config, saving it locally");
			config = newConfig;
			saveConfig();
		}
	}
	
	protected void saveConfig() throws IOException {
		System.out.println("Saving config locally");
		FileOutputStream fos = new FileOutputStream("config.json");
		fos.write(config.toString().getBytes());
		fos.close();			
	}

	
	public void fileModified(File file) {
		if(file.getName().equals("config.json")) {
			try {
				DataMap newConfig = new DataMap(new FileInputStream(file));
				if(!newConfig.toString().equals(config.toString())) {
					System.out.println("Config updated locally, reloading and publishing it out");
					config = newConfig;
					agent.getFirebus().publish("config", new Payload(config.toString()));
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void fileCreated(File file) {
		
	}

	public void fileDeleted(File file) {
		
	}

	public Payload service(Payload payload) throws FunctionErrorException {
		return new Payload(config.toString());
	}

	public void consume(Payload payload) {
		try {
			receivedConfig(payload);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public DataMap getConfig() {
		return config;
	}

	public ServiceInformation getServiceInformation() {
		return null;
	}
}