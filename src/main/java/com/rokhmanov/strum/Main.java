package com.rokhmanov.strum;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.JsonValue;

import org.iq80.leveldb.util.FileUtils;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class Main {

	private static EmbeddedESServer esServer;
	private static File esDataDirectory;
	private static String SEARCH_STRING = "20";
	
	public static void main(String[] args) {
		try {
			startup();
		} catch (Exception e) {
			e.printStackTrace();
			shutdown();
			System.exit(1);
		}
		Timeout timeout = new Timeout(Duration.create(30, "seconds"));
		ActorSystem system = ActorSystem.create("strum");
		ActorRef worker = system.actorOf(Props.create(MainSearchActor.class), "worker");
		worker.tell(SearchProtocol.Init, null);
		
		for (int i = 0; i < 20; i++) {
			Patterns.ask(worker, new SearchProtocol().new StartSearch(SEARCH_STRING + i), timeout)
				.onSuccess(new OnSuccessSearch<Object>(SEARCH_STRING + i) , system.dispatcher());					
		}
		
		//shutdown();
	}
	
	private static void startup() throws InterruptedException, ExecutionException {
		esDataDirectory = new File(System.getProperty("user.dir"), "elasticsearch-data");
		// TODO: remove folder deletion 
		FileUtils.deleteRecursively(esDataDirectory);
		esServer = new EmbeddedESServer(esDataDirectory);
	}
	
	private static void shutdown() {
		if(null != esServer)esServer.shutdown();
		FileUtils.deleteRecursively(esDataDirectory);
	}

}

class OnSuccessSearch<T> extends OnSuccess<T> {
	String searchCriteria;
	public OnSuccessSearch(String searchCriteria){
		this.searchCriteria = searchCriteria;
	}
	@Override
	public void onSuccess(Object obj) {
		SearchProtocol.SearchFeed feed = (SearchProtocol.SearchFeed)obj;
		Stream.generate(new Supplier<JsonValue>(){
			@Override
			public JsonValue get() {
				try {
					return feed.getElements().take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return JsonValue.NULL;
			}
		}).forEach(s -> System.out.println(searchCriteria + "===>" + s));
	}
}