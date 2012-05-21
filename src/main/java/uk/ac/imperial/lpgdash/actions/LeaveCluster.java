package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.Action;

public class LeaveCluster implements Action {
	Player player;
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

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public Cluster getCluster() {
		return cluster;
	}

}
