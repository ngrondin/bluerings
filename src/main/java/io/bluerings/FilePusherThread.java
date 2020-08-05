package io.bluerings;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import io.firebus.Payload;
import io.firebus.StreamEndpoint;

public class FilePusherThread extends Thread {
	protected File file;
	protected StreamEndpoint sep;
	
	public FilePusherThread(File f, StreamEndpoint s) {
		file = f;
		sep = s;
		start();
	}
	
	public void run() {
		try {
			int counter = 0;
			FileInputStream fis = new FileInputStream(file);
			byte[] bytes = new byte[8192];
			int len = 0;
			while((len = fis.read(bytes)) > -1) {
				Payload chunk = new Payload(Arrays.copyOf(bytes, len));
				chunk.metadata.put("seq", "" + counter);
				sep.send(chunk);
				counter++;
				Thread.sleep(1);
			}
			fis.close();
			Thread.sleep(2000);
			System.out.println("Finished pushing file " + file.getName());
			sep.close();
		} catch(Exception e) {
			sep.close();
		}
	}

}
