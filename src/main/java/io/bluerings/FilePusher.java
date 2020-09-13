package io.bluerings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.interfaces.StreamHandler;

public class FilePusher implements StreamHandler {
	
	public interface FilePusherListener {
		public void fileSent(File file);
		public void fileSendFailed(File file);
	}
	
	protected File file;
	protected StreamEndpoint sep;
	protected int counter;
	protected FileInputStream fis;
	protected byte[] chunkBytes;
	protected int chunkLength;
	protected long progress;
	protected boolean completed;
	protected long start;
	protected long lastLoggedProgress;
	protected FilePusherListener listener;
	
	public FilePusher(File f, StreamEndpoint s, FilePusherListener fpl) {
		file = f;
		sep = s;
		counter = -1;
		chunkBytes = new byte[524288];
		chunkLength = 0;
		progress = 0;
		completed = false;
		start = System.currentTimeMillis();
		lastLoggedProgress = start;
		listener = fpl;
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
	
	protected void sendNextChunk() throws IOException {
			counter++;
			chunkLength = fis.read(chunkBytes);
			if(chunkLength > -1) {
				progress += chunkLength;
				sendChunk();
			} else {
				Payload chunk = new Payload(new byte[0]);
				chunk.metadata.put("done", "true");
				sep.send(chunk);
				completed = true;
			}
	}
	
	protected void sendChunk() {
		Payload chunk = new Payload(chunkLength == chunkBytes.length ? chunkBytes : Arrays.copyOf(chunkBytes, chunkLength));
		chunk.metadata.put("seq", "" + counter);
		sep.send(chunk);
		
		long now = System.currentTimeMillis();
		if(lastLoggedProgress < now - 10000) {
			System.out.println("Tranfering " + file.getName() + " " + (progress / 1024) + "k");
			lastLoggedProgress = now;
		}
	}
	
	public void receiveStreamData(Payload payload, StreamEndpoint streamEndpoint) {
		try {
			String ctl = payload.getString();
			if(ctl.equals("next"))
				sendNextChunk();
			else if(ctl.equals("resend"))
				sendChunk();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void streamClosed(StreamEndpoint streamEndpoint) {
		try {
			fis.close();
			sep.close();
			long end = System.currentTimeMillis();
			String size = (progress < 1024 ? progress + "B" : (progress < 1048576 ? (progress/1024) + "KB" : (progress / 1048576) + "MB"));
			if(completed) {
				System.out.println("Finished sending " + file.getName() + ", " + size + " in " + ((end - start) / 1000) + "s");
				if(listener != null) 
					listener.fileSent(file);
			} else {
				System.out.println("Failed sending " + file.getName() + " at " + size + " after " + ((end - start) / 1000) + "s");
				if(listener != null) 
					listener.fileSendFailed(file);				
			}				
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
