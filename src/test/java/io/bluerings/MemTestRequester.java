package io.bluerings;

import java.security.MessageDigest;
import java.util.Formatter;

import io.firebus.Firebus;
import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.information.StreamInformation;
import io.firebus.interfaces.StreamHandler;

public class MemTestRequester {
	protected Firebus firebus;
	protected MessageDigest md;
		
	public MemTestRequester() {
		firebus = new Firebus();
		try {
			md = MessageDigest.getInstance("SHA-1"); 
			StreamEndpoint sep = firebus.requestStream("test", new Payload("hello?"), 10000);
			sep.setHandler(new StreamHandler() {
				public void receiveStreamData(Payload payload, StreamEndpoint streamEndpoint) {
					byte[] hash = md.digest(payload.getBytes());
					Formatter formatter = new Formatter();
					for (byte b : hash) {
				        formatter.format("%02x", b);
				    }
					streamEndpoint.send(new Payload(formatter.toString()));
					formatter.close();
					long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
					System.out.println(used);
				}

				public void streamClosed(StreamEndpoint streamEndpoint) {
					
				}
			});
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getStreamIdleTimeout() {
		return 10000;
	}

	public StreamInformation getStreamInformation() {
		return null;
	}
	
	public static void main(String args[]) {
		new MemTestRequester();
	}

}
