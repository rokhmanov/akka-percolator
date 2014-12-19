package com.rokhmanov.strum.test;

import java.util.UUID;
import java.util.stream.Stream;

import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class Test {

	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("test");
		ActorRef testActor = system.actorOf(Props.create(TestActor.class)); 
		Future<Object> future = Patterns.ask(testActor, "startsearch", new Timeout(Duration.create(30, "seconds")));
		future.onSuccess(new OnSuccess<Object>(){
			@Override
			public void onSuccess(Object obj) {
				((Stream<Tuple<UUID, String>>)obj).forEach(s -> System.out.println(s.x + "===>" + s.y)); 
			}
		}, system.dispatcher());
	}

}
