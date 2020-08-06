package io.bluerings;

import java.io.File;
import java.io.FileOutputStream;

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
	
	public FileReceiverHandler(String fn, FileReceiverListener frl) {
		try {
			filename = fn;
			listener = frl;
			file = new File("repo/" + filename);
			fos = new FileOutputStream(file);
			progress = 0;
			start = System.currentTimeMillis();
			lastLoggedProgress = start;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void receiveStreamData(Payload payload, StreamEndpoint streamEndpoint) {
		try {
			byte[] bytes = payload.getBytes();
			progress += bytes.length;
			fos.write(bytes);
			streamEndpoint.send(new Payload("ok"));
			long now = System.currentTimeMillis();
			if(lastLoggedProgress < now - 10000) {
				System.out.println("Tranfering " + filename + " " + (progress / 1024) + "k");
				lastLoggedProgress = now;
			}
		} catch(Exception e) {
			if(listener != null) 
				listener.fileReceiveFailed(file);
			e.printStackTrace();
		}
	}

	public void streamClosed(StreamEndpoint streamEndpoint) {
		try {
			fos.close();
			long end = System.currentTimeMillis();
			String size = (progress < 1024 ? progress + "B" : (progress < 1048576 ? (progress/1024) + "KB" : (progress / 1048576) + "MB"));
			System.out.println("Finished receiving " + filename + ", " + size + " in " + ((end - start) / 1000) + "s");
			if(listener != null)
				listener.fileReceived(file);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
