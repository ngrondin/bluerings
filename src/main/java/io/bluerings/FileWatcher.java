package io.bluerings;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;


public class FileWatcher extends Thread {
	
	public interface FileWatcherListener {
		public void fileModified(File file);
		public void fileCreated(File file);
		public void fileDeleted(File file);
	}
	
	private WatchService watcher;
	private FileWatcherListener listener;

	
	public FileWatcher(String p,  FileWatcherListener l) throws IOException
	{
		listener = l;
		watcher = FileSystems.getDefault().newWatchService();
		Path start = Paths.get(p);
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
				return FileVisitResult.CONTINUE;
			}
		});
		setName("brFileWather");
		start();
	}
	
	public void run() 
	{
		try
		{
				WatchKey key = null;
				while ((key = watcher.take()) != null) 
				{
				    for (WatchEvent<?> event : key.pollEvents()) 
				    {
				    	Path path = (Path)event.context();
				    	if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
				    		listener.fileCreated(path.toFile());
				    	else if(event.kind() == StandardWatchEventKinds.ENTRY_DELETE)
				    		listener.fileDeleted(path.toFile());
				    	else if(event.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
				    		listener.fileModified(path.toFile());
				    }
				    key.reset();
				}				
		} 
		catch (InterruptedException x) 
		{
			return;
		}

	}
}

