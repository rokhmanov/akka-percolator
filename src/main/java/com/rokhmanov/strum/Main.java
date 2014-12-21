package com.rokhmanov.strum;

import java.io.File;
import java.util.concurrent.ExecutionException;
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
	private static String SEARCH_STRING = "90";
	private static int NUM_QUERIES = 100;
	
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
		
		Patterns.ask(worker, SearchProtocol.Init, timeout)
			.onSuccess(new OnSuccessSearch<Object>() , system.dispatcher());					
		
		for (int i = 0; i < NUM_QUERIES; i++) {
			worker.tell(new SearchProtocol().new StartSearch(SEARCH_STRING + i), ActorRef.noSender());
		}
		
	}
	
	private static void startup() throws InterruptedException, ExecutionException {
		esDataDirectory = new File(System.getProperty("user.dir"), "elasticsearch-data");
		FileUtils.deleteRecursively(esDataDirectory);
		esServer = new EmbeddedESServer(esDataDirectory);
	}
	
	private static void shutdown() {
		if(null != esServer)esServer.shutdown();
		FileUtils.deleteRecursively(esDataDirectory);
	}

}

class OnSuccessSearch<T> extends OnSuccess<T> {
	@Override
	public void onSuccess(Object obj) {
		Stream<Tuple<String, JsonValue>> feed = (Stream<Tuple<String, JsonValue>>)obj;
		feed.forEach(s -> System.out.println(s.x + "===>" + s.y));		
	}
}