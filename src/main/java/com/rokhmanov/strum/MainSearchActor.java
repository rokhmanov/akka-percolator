package com.rokhmanov.strum;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import javax.json.JsonValue;

import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.UntypedActor;

public class MainSearchActor extends UntypedActor {

	private Scheduler scheduler = getContext().system().scheduler();
	private ExecutionContext ec = getContext().system().dispatcher();
	ActorRef elasticSearchActor = getContext().actorOf(Props.create(ElasticSearchActor.class), "elasticSearchActor");
	ActorRef logEntryProducerActor = getContext().actorOf(Props.create(LogEntryProducerActor.class), "logEntryProducer");
	BlockingQueue<Tuple<String, JsonValue>> feed = new LinkedBlockingQueue<>();
	Map<UUID, String> channels = new ConcurrentHashMap<>();
	
	public void preStart(){
		scheduler.schedule(Duration.create(3, "seconds"), Duration.create(5, "milliseconds"), getSelf(), SearchProtocol.Tick, ec, getSelf());
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message == SearchProtocol.Tick){
			logEntryProducerActor.tell(SearchProtocol.Tick, getSelf());
		}
		else if(message == SearchProtocol.Init){
			elasticSearchActor.tell(SearchProtocol.Init, getSelf());
			getSender().tell(Stream.generate(() -> produce()), getSelf());
		}
		else if(message instanceof SearchProtocol.LogEntry){
			elasticSearchActor.tell(message, getSelf());
		}
		else if(message instanceof SearchProtocol.StartSearch){
			channels.put(((SearchProtocol.StartSearch) message).getId(), ((SearchProtocol.StartSearch) message).getSearchString());
			elasticSearchActor.tell(message, getSelf());
		}
		else if(message instanceof SearchProtocol.StopSearch){
			elasticSearchActor.tell(message, getSelf());
			channels.remove(((SearchProtocol.StopSearch)message).getId());
		}
		else if(message instanceof SearchProtocol.SearchMatch){
			SearchProtocol.SearchMatch match = (SearchProtocol.SearchMatch)message;
			match.getMatchingChannelIds().forEach(
					channelId -> {
						feed.offer(new Tuple<String, JsonValue>(channels.get(channelId), match.getLogEntry().getData()));
					}
			);		
		}		
	}

	private Tuple<String, JsonValue> produce() {
		try {
			return feed.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new Tuple<String, JsonValue>("ERROR", JsonValue.NULL);
	}

}
