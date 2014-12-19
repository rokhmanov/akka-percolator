package com.rokhmanov.strum;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import akka.actor.UntypedActor;

public class LogEntryProducerActor extends UntypedActor {

	private String[] devices = {"Desktop", "Tablet", "Phone", "TV"};
	private String[] useragents = {"Chrome", "Firefox", "Internet Explorer", "Safari", "HttpClient"};
	private String[] paths = {"/a", "/b", "/c", "/d", "/e"};
	private String[] methods = {"GET", "POST", "PUT", "DELETE"};
	private String[] statuses = {"200", "404", "201", "500"};
	
	@Override
	public void onReceive(Object message) throws Exception {
		if (message == SearchProtocol.Tick){
			getSender().tell(new SearchProtocol().new LogEntry(generateLogEntry()), getSelf());
		}
	}

	private JsonValue generateLogEntry() {
		JsonObject logEntry = Json.createObjectBuilder()
				.add("timestamp", new Timestamp(new Date().getTime()).toString())
				.add("response time", getRandomResponseTime() + "")
				.add("method", randomElement(methods))
				.add("path", randomElement(paths))
				.add("status", randomElement(statuses))
				.add("device", randomElement(devices))
				.add("user agent", randomElement(useragents))
				.build();		
		return logEntry;
	}

	private String randomElement(String[] elements) {
		Random rand = new Random(System.currentTimeMillis());
		int randIdx = rand.nextInt(elements.length);
		return elements[randIdx];
	}

	private int getRandomResponseTime() {		
		return new Random(System.currentTimeMillis()).nextInt(1000);
	}

}
