package uk.ac.imperial.lpgdash.facts;

public class PlayerHistory {
	final Player player;
	final Cluster cluster;

	double averageAllocated = 0;
	int roundsAllocated = 0;
	double averageDemanded = 0;
	double averageSatifaction = 0;
	double averageProvided = 0;
	int roundsParticipated = 0;

	public PlayerHistory(Player player, Cluster cluster) {
		super();
		this.player = player;
		this.cluster = cluster;
	}

	public double getAverageAllocated() {
		return averageAllocated;
	}

	public void setAverageAllocated(double averageAllocated) {
		this.averageAllocated = averageAllocated;
	}

	public int getRoundsAllocated() {
		return roundsAllocated;
	}

	public void setRoundsAllocated(int roundsAllocated) {
		this.roundsAllocated = roundsAllocated;
	}

	public double getAverageDemanded() {
		return averageDemanded;
	}

	public void setAverageDemanded(double averageDemanded) {
		this.averageDemanded = averageDemanded;
	}

	public double getAverageSatifaction() {
		return averageSatifaction;
	}

	public void setAverageSatifaction(double averageSatifaction) {
		this.averageSatifaction = averageSatifaction;
	}

	public double getAverageProvided() {
		return averageProvided;
	}

	public void setAverageProvided(double averageProvided) {
		this.averageProvided = averageProvided;
	}

	public Player getPlayer() {
		return player;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public int getRoundsParticipated() {
		return roundsParticipated;
	}

	public void setRoundsParticipated(int roundsParticipated) {
		this.roundsParticipated = roundsParticipated;
	}

	@Override
	public String toString() {
		return "PlayerHistory [player=" + player.getId() + ", cluster="
				+ cluster + ", averageAllocated=" + averageAllocated
				+ ", roundsAllocated=" + roundsAllocated + ", averageDemanded="
				+ averageDemanded + ", averageSatifaction="
				+ averageSatifaction + ", averageProvided=" + averageProvided
				+ ", roundsParticipated=" + roundsParticipated + "]";
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
		PlayerHistory other = (PlayerHistory) obj;
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
