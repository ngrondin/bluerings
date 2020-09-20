package io.bluerings;

import java.util.Random;

import io.firebus.Firebus;
import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.exceptions.FunctionErrorException;
import io.firebus.information.StreamInformation;
import io.firebus.interfaces.StreamHandler;
import io.firebus.interfaces.StreamProvider;

public class MemTestProvider implements StreamProvider {
	protected Firebus firebus;
	protected Random rnd;
		
	public MemTestProvider() {
		firebus = new Firebus();
		rnd = new Random();
		firebus.registerStreamProvider("test", this, 10);		
	}
	
	public void acceptStream(Payload payload, StreamEndpoint streamEndpoint) throws FunctionErrorException {
		streamEndpoint.setHandler(new StreamHandler() {
			public void receiveStreamData(Payload payload, StreamEndpoint streamEndpoint) {
				byte[] bytes = new byte[1024000];
				rnd.nextBytes(bytes);
				streamEndpoint.send(new Payload(bytes));
				long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println(used);
			}

			public void streamClosed(StreamEndpoint streamEndpoint) {
				
			}
		});
		streamEndpoint.send(new Payload("start"));
	}

	public int getStreamIdleTimeout() {
		return 10000;
	}

	public StreamInformation getStreamInformation() {
		return null;
	}
	
	public static void main(String args[]) {
		new MemTestProvider();
	}


}
