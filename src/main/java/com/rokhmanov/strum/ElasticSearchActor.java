package com.rokhmanov.strum;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import akka.actor.UntypedActor;

import com.rokhmanov.strum.SearchProtocol.LogEntry;
import com.rokhmanov.strum.SearchProtocol.StartSearch;
import com.rokhmanov.strum.SearchProtocol.StopSearch;

public class ElasticSearchActor extends UntypedActor {

	private Client client = ClientBuilder.newClient();
				
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof SearchProtocol.LogEntry){
			percolate((SearchProtocol.LogEntry)message);
		} else if (message instanceof SearchProtocol.StartSearch){
			registerQuery((SearchProtocol.StartSearch)message);
		} else if (message instanceof SearchProtocol.StopSearch){
			unregisterQuery((SearchProtocol.StopSearch)message);
		} else if(message == SearchProtocol.Init){
			createMapping();					
		}
	}

	private void createMapping(){		
		JsonObject stringStoreType = Json.createObjectBuilder().add("type", "string").add("store", true).build();
		JsonObject properties = Json.createObjectBuilder().add("properties", Json.createObjectBuilder()
				.add("path", stringStoreType)
				.add("method", stringStoreType)
				.add("response time", stringStoreType)
				.add("device", stringStoreType)
				.add("user agent", stringStoreType)
				.add("timestamp", stringStoreType)
				.add("status", stringStoreType))
				.build();		
		JsonObject mappings	= Json.createObjectBuilder().add("mappings", 
				Json.createObjectBuilder().add("logentry", properties))
				.build();	
		WebTarget webTarget = client.target("http://localhost:9200/logentries/");
		Response response = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.json(mappings));
		if (response.getStatusInfo().getStatusCode() != Response.Status.OK.getStatusCode()) {
			   throw new RuntimeException("Register Query failed. HTTP error code : " + response.getStatus());
		}		
	}
	
	private void registerQuery(StartSearch message) {
		JsonObject query = Json.createObjectBuilder().add("query",
				Json.createObjectBuilder().add("query_string",
						Json.createObjectBuilder().add("query", message.getSearchString()))).build();
		Response response = client.target("http://localhost:9200/logentries/.percolator/" + message.getId())
				.request(MediaType.APPLICATION_JSON).put(Entity.json(query));
		if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
			   throw new RuntimeException("Register Query failed. HTTP error code : " + response.getStatus());
		}		
	}


	private void unregisterQuery(StopSearch message) {
		Response response = client.target("http://localhost:9200/logentries/.percolator/" + message.getId())
				.request(MediaType.APPLICATION_JSON).delete();
		if (response.getStatusInfo().getStatusCode() != Response.Status.GONE.getStatusCode()) {
			   throw new RuntimeException("Unregister Query failed. HTTP error code : " + response.getStatus());
		}		
	}
	
	private void percolate(LogEntry message) {		
		JsonObject doc = Json.createObjectBuilder().add("doc", message.getData()).build();
		Response response = client.target("http://localhost:9200/logentries/logentry/_percolate/")
				.request(MediaType.APPLICATION_JSON).post(Entity.json(doc));
		if (response.getStatusInfo().getStatusCode() != Response.Status.OK.getStatusCode()) {
			   throw new RuntimeException("Percolate Query failed. HTTP error code : " + response.getStatus());
		}
		JsonObject reply = response.readEntity(JsonObject.class);
		JsonArray matches = reply.getJsonArray("matches");
		if(matches.size() > 0){
			List<UUID> uids = matches.stream().parallel().map(ElasticSearchActor::getMatchId).collect(Collectors.toList());
			getSender().tell(new SearchProtocol().new SearchMatch(message, uids), getSelf());
		}
	}
	
	private static UUID getMatchId(JsonValue match){
		return UUID.fromString(((JsonObject)match).getString("_id"));
	}
}


