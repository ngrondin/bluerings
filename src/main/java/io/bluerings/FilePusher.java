package io.bluerings;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.interfaces.StreamHandler;

public class FilePusher implements StreamHandler {
	protected File file;
	protected StreamEndpoint sep;
	protected int counter;
	protected FileInputStream fis;
	
	public FilePusher(File f, StreamEndpoint s) {
		file = f;
		sep = s;
		counter = 0;
		try {
			System.out.println("Starting to push file " + file.getName());
			fis = new FileInputStream(file);
			sep.setHandler(this);
			sendNextChunk();
		} catch(Exception e) {
			sep.close();
			e.printStackTrace();
		}
	}
	
	protected void sendNextChunk() {
		byte[] bytes = new byte[524288];
		try {
			int len = fis.read(bytes);
			if(len > -1) {
				Payload chunk = new Payload(Arrays.copyOf(bytes, len));
				chunk.metadata.put("seq", "" + counter);
				sep.send(chunk);
				counter++;
			} else {
				fis.close();
				sep.close();
				System.out.println("Finished pushing file " + file.getName());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	public void receiveStreamData(Payload payload, StreamEndpoint streamEndpoint) {
		sendNextChunk();
	}

	public void streamClosed(StreamEndpoint streamEndpoint) {
		try {
			fis.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
