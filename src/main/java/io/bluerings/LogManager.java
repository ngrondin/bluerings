package io.bluerings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.bluerings.FilePusher.FilePusherListener;
import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.interfaces.Consumer;
import io.firebus.utils.DataException;

public class LogManager extends Thread implements Consumer {
	protected Agent agent;
	protected Map<String, InputStream> streams;
	protected Map<String, ByteBuffer> buffers;
	protected boolean active;
	protected Object fileLock;

	public LogManager(Agent a) {
		agent = a;
		streams = new HashMap<String, InputStream>();
		buffers = new HashMap<String, ByteBuffer>();
		active = true;
		fileLock = new Object();
	}
	
	public void initiate() throws IOException, DataException {
		agent.getFirebus().registerConsumer("logcall", this, 10);
		File dir = new File("log");
		if(!dir.exists())
			dir.mkdir();
		start();
	}
	
	public void addStream(String id, InputStream is) {
		streams.put(id, is);
		buffers.put(id, ByteBuffer.wrap(new byte[10485766]));
	}
	
	public void removeStream(String id) {
		try {
			ByteBuffer bb = buffers.get(id);
			if(bb != null) { 
				writeBufferToFile(bb);
				buffers.remove(id);
				streams.remove(id);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		int count = 0;
		while(active) {
			try {
				Iterator<String> it = streams.keySet().iterator();
				while(it.hasNext()) {
					String id = it.next();
					InputStream is = streams.get(id);
					ByteBuffer bb = buffers.get(id);
					while(is.available() > 0) {
						bb.put((byte)is.read());
					}
					if(count == 10) {
						writeBufferToFile(bb);
						count = 0;
					} else {
						count++;
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {Thread.sleep(1000); } catch(Exception e) {}
		}		
	}
	
	protected void writeBufferToFile(ByteBuffer bb)  {
		try {
			synchronized(fileLock) {
				byte[] bytes = bb.array();
				int last = bb.position();
				int pos = last;
				for(;pos > 0 && bytes[pos] != 10; pos--);
				File file = new File("log/log.txt");
				if(file.length() > 5242800) {
					file.renameTo(new File("log/log" + System.currentTimeMillis() + ".txt"));
					file = new File("log/log.txt");
					file.createNewFile();
				}
				if(pos > 0) {
					FileOutputStream fos = new FileOutputStream("log/log.txt", true);
					fos.write(bytes, 0, pos);
					fos.close();
					bb.position(0);
					for(int i = pos; i < last; i++)
						bb.put(bytes[i]);
				}			
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	public void consume(Payload payload) {
		try {
			String streamProviderName = payload.getString();
			synchronized(fileLock) {
				File file = new File("log/log.txt");
				file.renameTo(new File("log/log" + System.currentTimeMillis() + ".txt"));
				file = new File("log/log.txt");
				file.createNewFile();
			}
			File dir = new File("log");
			File[] files = dir.listFiles();
			for(int i = 0; i < files.length; i++) {
				if(!files[i].getName().equals("log.txt")) {
					StreamEndpoint sep = agent.getFirebus().requestStream(streamProviderName, new Payload(), 10000);
					if(sep != null) {
						new FilePusher(files[i], sep, new FilePusherListener() {
							public void fileSent(File file) {
								try {
									synchronized(fileLock) {
										file.delete();
									}
								} catch(Exception e) {
									e.printStackTrace();
								}
							}
	
							public void fileSendFailed(File file) {
							}
						});
					}	
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
