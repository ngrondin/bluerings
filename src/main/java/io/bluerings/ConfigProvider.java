package io.bluerings;

import io.firebus.Payload;
import io.firebus.exceptions.FunctionErrorException;
import io.firebus.information.ServiceInformation;
import io.firebus.interfaces.ServiceProvider;

public class ConfigProvider implements ServiceProvider {

	protected Agent agent;
	
	public ConfigProvider(Agent a) {
		agent = a;
	}
	
	public Payload service(Payload payload) throws FunctionErrorException {
		return new Payload(agent.getConfig().toString());
	}

	public ServiceInformation getServiceInformation() {
		return null;
	}

}
