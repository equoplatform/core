package com.make.equo.node.packages.tests.mocks;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.google.gson.JsonObject;
import com.make.equo.aer.api.IEquoLoggingService;

// Adding a service ranking temporarily as the logging service is currently mandatory to run the framework
@Component(property = { "service.ranking:Integer=10000" })
public class LoggingServiceMock implements IEquoLoggingService {

	private List<String> receivedMessages = new ArrayList<>();

	@Override
	public void logError(String message) {
	}

	@Override
	public void logInfo(String message) {
	}

	@Override
	public void logWarning(String message) {
	}

	@Override
	public void logError(String message, JsonObject tags) {
		receivedMessages.add(message);
	}

	@Override
	public void logInfo(String message, JsonObject tags) {
		receivedMessages.add(message);
	}

	@Override
	public void logWarning(String message, JsonObject tags) {
		receivedMessages.add(message);
	}

}
