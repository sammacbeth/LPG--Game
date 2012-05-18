package uk.ac.imperial.lpgdash.facts;

public class MemberOf {

	public Player player;
	public Cluster cluster;

	public MemberOf(Player player, Cluster cluster) {
		super();
		this.player = player;
		this.cluster = cluster;
	}

	@Override
	public String toString() {
		return "MemberOf [player=" + player + ", cluster=" + cluster + "]";
	}

	public Player getPlayer() {
		return player;
	}

}
