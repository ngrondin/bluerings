package io.bluerings;

import java.io.File;
import java.io.FileOutputStream;

import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.interfaces.StreamHandler;

public class FileReceiverHandler implements StreamHandler {

	protected FileOutputStream fos;
	protected String filename;
	
	public FileReceiverHandler(String fn) {
		try {
			filename = fn;
			File newFile = new File("repo/" + filename);
			fos = new FileOutputStream(newFile);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void receiveStreamData(Payload payload, StreamEndpoint streamEndpoint) {
		try {
			int seq = Integer.parseInt(payload.metadata.get("seq"));
			fos.write(payload.getBytes());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void streamClosed(StreamEndpoint streamEndpoint) {
		try {
			fos.close();
			System.out.println("Finished receiving " + filename);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
