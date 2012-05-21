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

	public Cluster getCluster() {
		return cluster;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cluster == null) ? 0 : cluster.hashCode());
		result = prime * result + ((player == null) ? 0 : player.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MemberOf other = (MemberOf) obj;
		if (cluster == null) {
			if (other.cluster != null)
				return false;
		} else if (!cluster.equals(other.cluster))
			return false;
		if (player == null) {
			if (other.player != null)
				return false;
		} else if (!player.equals(other.player))
			return false;
		return true;
	}

}
