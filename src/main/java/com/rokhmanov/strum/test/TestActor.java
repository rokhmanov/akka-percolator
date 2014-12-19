package com.rokhmanov.strum.test;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import akka.actor.Scheduler;
import akka.actor.UntypedActor;

public class TestActor extends UntypedActor {
	BlockingQueue<Tuple<UUID, String>> queue = new LinkedBlockingQueue<>();
	private Scheduler scheduler = getContext().system().scheduler();
	private ExecutionContext ec = getContext().system().dispatcher();

	public void preStart(){
		scheduler.schedule(Duration.create(1, "seconds"), Duration.create(1, "seconds"), getSelf(), "add", ec, getSelf());
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if (message.equals("startsearch")){
			getSender().tell(Stream.generate(() -> produce()), getSelf());
		} else if (message.equals("add")) {
			queue.offer(new Tuple<UUID, String>(UUID.randomUUID(), new Date().toString()));
		}
	}

	private Tuple<UUID, String> produce() {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new Tuple<UUID, String>(null, "ERROR");
	}
}
