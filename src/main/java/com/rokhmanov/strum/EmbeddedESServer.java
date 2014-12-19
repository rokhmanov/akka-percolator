package com.rokhmanov.strum;

import java.io.File;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;

import static org.elasticsearch.node.NodeBuilder.*;

public class EmbeddedESServer {
	
	private Client client;
	private Node node;
	
	public Client getClient(){
		return this.client;
	}
	
	public EmbeddedESServer(File dataDirectory){
		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
		settings.put("node.name", "test-node-1");
		settings.put("path.data", dataDirectory.getAbsolutePath());
		this.node = nodeBuilder().local(true).settings(settings).node();
		this.client = node.client();
	}
	
	public void shutdown(){
		this.client.close();
		this.node.close();
	}
	
}
