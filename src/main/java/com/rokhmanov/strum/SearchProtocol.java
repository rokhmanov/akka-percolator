package com.rokhmanov.strum;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.json.JsonValue;

public class SearchProtocol {

	class LogEntry {
		private JsonValue data;
		public LogEntry (JsonValue jsElement){
			this.data = jsElement;
		}		
		public JsonValue getData(){
			return this.data;
		}
		public String toString(){
			return this.data.toString();
		}
	}
	
	
	class StartSearch {
		private String searchString;
		private UUID id;
		public StartSearch(String searchString){
			this.id = UUID.randomUUID();
			this.searchString = searchString;
		}
		public UUID getId(){
			return this.id;
		}
		public String getSearchString(){
			return this.searchString;
		}
		public String toString(){
			return "UUID:" + id + ", searchString:" + searchString;
		}
	}
	
	
	class StopSearch {
		private UUID id;
		public StopSearch(UUID id){
			this.id = id;
		}
		public UUID getId(){
			return id;
		}
		public String toString(){
			return id.toString();
		}
	}
	
	
	class SearchFeed {
		private BlockingQueue<JsonValue> out; 
		public SearchFeed(BlockingQueue<JsonValue> startSearching) {
			this.out = startSearching;
		}
		public BlockingQueue<JsonValue> getElements(){
			return out;
		}
	}
	
	
	class SearchMatch {
		private LogEntry logEntry;
		private List<UUID> matchingChannelIds;
		public SearchMatch(LogEntry logEntry, List<UUID> channelIds){
			this.logEntry = logEntry;
			this.matchingChannelIds = channelIds;
		}
		public LogEntry getLogEntry() {
			return logEntry;
		}
		public List<UUID> getMatchingChannelIds() {
			return matchingChannelIds;
		}
		public String toString(){
			return "LogEntry:" + logEntry + ", matchingChannelIDs:" + matchingChannelIds.toArray();
		}
	}
	
	final static Object Tick = new Object() {
		@Override
		public String toString() {
			return "Tick";
		}
	};
	
	final static Object Init = new Object() {
		@Override
		public String toString() {
			return "Init";
		}
	};
}
