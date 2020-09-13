package io.bluerings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.interfaces.StreamHandler;

public class FileReceiverHandler implements StreamHandler {

	public interface FileReceiverListener {
		public void fileReceived(File file);
		public void fileReceiveFailed(File file);
	}
	
	protected File file;
	protected FileOutputStream fos;
	protected String filename;
	protected FileReceiverListener listener;
	protected long progress;
	protected long start;
	protected long lastLoggedProgress;
	protected int counter;
	protected boolean complete;
	
	public FileReceiverHandler(String fn, FileReceiverListener frl) {
		try {
			filename = fn;
			listener = frl;
			file = new File(filename);
			fos = new FileOutputStream(file);
			progress = 0;
			counter = 0;
			complete = false;
			start = System.currentTimeMillis();
			lastLoggedProgress = start;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void receiveStreamData(Payload payload, StreamEndpoint streamEndpoint) {
		try {
			byte[] bytes = payload.getBytes();
			if(payload.metadata.containsKey("seq")) {
				int seq = Integer.parseInt(payload.metadata.get("seq"));
				if(seq == counter) {
					progress += bytes.length;
					fos.write(bytes);
					counter++;
					streamEndpoint.send(new Payload("next"));
					long now = System.currentTimeMillis();
					if(lastLoggedProgress < now - 10000) {
						System.out.println("Tranfering " + filename + " " + (progress / 1024) + "k");
						lastLoggedProgress = now;
					}
				} else {
					streamEndpoint.close();
					fail();
				}
			} else if (payload.metadata.containsKey("done")) {
				streamEndpoint.close();
				fos.close();
				complete = true;
				long end = System.currentTimeMillis();
				String size = (progress < 1024 ? progress + "B" : (progress < 1048576 ? (progress/1024) + "KB" : (progress / 1048576) + "MB"));
				System.out.println("Finished receiving " + filename + ", " + size + " in " + ((end - start) / 1000) + "s");
				if(listener != null)
					listener.fileReceived(file);
			}
		} catch(Exception e) {
			if(listener != null) 
				listener.fileReceiveFailed(file);
			e.printStackTrace();
		}
	}

	public void streamClosed(StreamEndpoint streamEndpoint) {
		try {
			if(!complete)
				fail();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void fail() {
		try {
			fos.close();
			file.delete();
			System.out.println("Failed receiving " + filename);
			if(listener != null)
				listener.fileReceiveFailed(file);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}
