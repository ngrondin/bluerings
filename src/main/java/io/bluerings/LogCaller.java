package io.bluerings;

import java.io.File;

import io.bluerings.FileReceiverHandler.FileReceiverListener;
import io.firebus.Firebus;
import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.exceptions.FunctionErrorException;
import io.firebus.information.StreamInformation;
import io.firebus.interfaces.StreamProvider;

public class LogCaller implements StreamProvider, FileReceiverListener {
	protected Firebus firebus;
	protected int id;

	public LogCaller() {
		try {
			id = 0;
			firebus = new Firebus("bluerings", "blueringspasswd");
			firebus.registerStreamProvider("logreceiver", this, 10);
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void addKnownAddress(String a, int p) {
		firebus.addKnownNodeAddress(a, p);
	}
	
	public void call() {
		try {
			int count = 0;
			while(!firebus.hasConnections() && count < 20) {
				System.out.println("Waiting");
				count++;
				Thread.sleep(1000);
			}
			count = 0;
			while(count < 3) {
				System.out.println("Calling");
				firebus.publish("logcall", new Payload("logreceiver"));
				count++;
				Thread.sleep(2000);
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
	
	protected synchronized int getNextId() {
		return id++;
	}
	
	public void acceptStream(Payload payload, StreamEndpoint streamEndpoint) throws FunctionErrorException {
		System.out.println("Receiving a log file");
		String fileName = "log/log" + getNextId() + ".txt";
		FileReceiverHandler frh = new FileReceiverHandler(fileName, this);
		streamEndpoint.setHandler(frh);
	}

	public int getStreamIdleTimeout() {
		return 10000;
	}

	public StreamInformation getStreamInformation() {
		return null;
	}
	
	public void fileReceived(File file) {
		
	}

	public void fileReceiveFailed(File file) {
		
	}
	
	public void close() {
		firebus.close();
	}
	
	
	public static void main(String[] args) {
		String ka = null;
		for(int i = 0; i < args.length; i++) {
			String sw = args[i];
			if(sw.equals("-fbka") && args.length > i + 1) {
				ka = args[i + 1];
				i++;
			}
		}
		LogCaller logCaller = new LogCaller();
		if(ka != null) {
			String[] parts = ka.split(":");
			logCaller.addKnownAddress(parts[0], Integer.parseInt(parts[1]));
		}
		logCaller.call();
		logCaller.close();
	}
	
}
