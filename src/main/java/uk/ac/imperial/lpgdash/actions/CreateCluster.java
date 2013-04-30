package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.Cluster;

public class CreateCluster extends PlayerAction {
	final Cluster cluster;

	public CreateCluster(Cluster cluster) {
		super();
		this.cluster = cluster;
	}

	public Cluster getCluster() {
		return this.cluster;
	}
	
	public Allocation getAllocationMethod() {
		return this.cluster.getAllocationMethod();
	}
}
