package io.bluerings;

import java.io.File;

import io.firebus.Payload;
import io.firebus.StreamEndpoint;
import io.firebus.exceptions.FunctionErrorException;
import io.firebus.information.StreamInformation;
import io.firebus.interfaces.StreamProvider;

public class FileStreamProvider implements StreamProvider {
	
	protected File file;
	
	public FileStreamProvider(File f) {
		file = f;
	}

	public void acceptStream(Payload payload, StreamEndpoint streamEndpoint) throws FunctionErrorException {
		new FilePusher(file, streamEndpoint);
	}

	public int getStreamIdleTimeout() {
		return 30000;
	}

	public StreamInformation getStreamInformation() {

		return null;
	}

}
