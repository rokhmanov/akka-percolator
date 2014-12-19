package com.rokhmanov.strum;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.json.JsonValue;

import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.UntypedActor;

import com.rokhmanov.strum.SearchProtocol.SearchMatch;
import com.rokhmanov.strum.SearchProtocol.StopSearch;

public class MainSearchActor extends UntypedActor {

	private Scheduler scheduler = getContext().system().scheduler();
	private ExecutionContext ec = getContext().system().dispatcher();
	ActorRef elasticSearchActor = getContext().actorOf(Props.create(ElasticSearchActor.class), "elasticSearchActor");
	ActorRef logEntryProducerActor = getContext().actorOf(Props.create(LogEntryProducerActor.class), "logEntryProducer");
	Map<UUID, Queue<JsonValue>> channels = new HashMap<>();
	
	public void preStart(){
		scheduler.schedule(Duration.create(3, "seconds"), Duration.create(10, "milliseconds"), getSelf(), SearchProtocol.Tick, ec, getSelf());
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message == SearchProtocol.Tick){
			logEntryProducerActor.tell(SearchProtocol.Tick, getSelf());
		}
		else if(message == SearchProtocol.Init){
			elasticSearchActor.tell(SearchProtocol.Init, getSelf());
		}
		else if(message instanceof SearchProtocol.LogEntry){
			elasticSearchActor.tell(message, getSelf());
		}
		else if(message instanceof SearchProtocol.StartSearch){
			getSender().tell(new SearchProtocol().new SearchFeed(startSearching((SearchProtocol.StartSearch)message)), getSelf());
		}
		else if(message instanceof SearchProtocol.StopSearch){
			stopSearching((SearchProtocol.StopSearch) message);
		}
		else if(message instanceof SearchProtocol.SearchMatch){
			broadcastMatch((SearchProtocol.SearchMatch)message);
		}		
	}

	private BlockingQueue<JsonValue> startSearching(SearchProtocol.StartSearch searchMessage) {
		BlockingQueue<JsonValue> queue = new LinkedBlockingQueue<>();
		channels.put(searchMessage.getId(), queue);
		elasticSearchActor.tell(searchMessage, getSelf());
		return queue;
	}

	private void stopSearching(StopSearch stopMessage) {
		channels.remove(stopMessage.getId());
		elasticSearchActor.tell(stopMessage, getSelf());
	}

	private void broadcastMatch(SearchMatch match) {
		match.getMatchingChannelIds().forEach(
				channelId -> {
					if (channels.containsKey(channelId)){
						channels.get(channelId).offer(match.getLogEntry().getData());
					} 
				}
		);		
	}

}
