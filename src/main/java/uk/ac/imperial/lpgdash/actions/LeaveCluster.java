package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;

public class LeaveCluster extends PlayerAction {
	final Cluster cluster;

	public LeaveCluster(Cluster cluster) {
		super();
		this.cluster = cluster;
	}

	public LeaveCluster(Player player, Cluster cluster) {
		super();
		this.player = player;
		this.cluster = cluster;
	}

	public Cluster getCluster() {
		return cluster;
	}

}
