package io.bluerings;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;


public class Test {

	public static void main(String[] args) {
		try {
			/*
			String command = "java -cp repo\\simple-pod-0.0.1.jar io.redback.RedbackServer repo\\simple-pod-0.0.1-config.json repo\\simple-pod-0.0.1.properties";
			String[] list = command.split(" ");
			ProcessBuilder pb = new ProcessBuilder("mklink", "/H", "allo.jar", "C:\\Users\\ngron\\git\\bluerings\\repo\\simple-pod-0.0.1.jar");
			pb.directory(new File("C:\\Users\\ngron\\git\\bluerings\\work"));
			Process p = pb.start();
			String out = p.toString();
			int pos = out.indexOf("pid=");
			int pos2 = out.indexOf(",", pos);
			long pid = Long.valueOf(out.substring(pos + 4, pos2));
			System.out.println(pid);
			*/
			
			Optional<ProcessHandle> oph = ProcessHandle.of(6976);
			ProcessHandle ph = oph.get();
			ph.destroy();
			/*
		      ProcessBuilder processBuilder = new ProcessBuilder("notepad.exe");
		      Process process = processBuilder.start();
		      System.out.println("-- process handle --");
		      ProcessHandle processHandle = process.toHandle();
		      System.out.printf("PID: %s%n", processHandle.pid());
		      System.out.printf("isAlive: %s%n", processHandle.isAlive());
		      */
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
