package uk.ac.imperial.lpgdash.facts;

public class PlayerHistory {

	double averageAllocated = 0;
	int roundsAllocated = 0;
	double averageDemanded = 0;
	double satisfaction = 0.5;
	double averageProvided = 0;
	int roundsParticipated = 0;
	int roundsAsHead = 0;
	int compliantRounds = 0;

	public PlayerHistory() {
		super();
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

	public double getSatisfaction() {
		return satisfaction;
	}

	public void setSatisfaction(double satisfaction) {
		this.satisfaction = satisfaction;
	}

	public double getAverageProvided() {
		return averageProvided;
	}

	public void setAverageProvided(double averageProvided) {
		this.averageProvided = averageProvided;
	}

	public int getRoundsParticipated() {
		return roundsParticipated;
	}

	public void setRoundsParticipated(int roundsParticipated) {
		this.roundsParticipated = roundsParticipated;
	}

	public int getRoundsAsHead() {
		return roundsAsHead;
	}

	public void setRoundsAsHead(int roundsAsHead) {
		this.roundsAsHead = roundsAsHead;
	}

	public int getCompliantRounds() {
		return compliantRounds;
	}

	public void setCompliantRounds(int compliantRounds) {
		this.compliantRounds = compliantRounds;
	}

	@Override
	public String toString() {
		return "PlayerHistory [averageAllocated=" + averageAllocated
				+ ", roundsAllocated=" + roundsAllocated + ", averageDemanded="
				+ averageDemanded + ", satisfaction=" + satisfaction
				+ ", averageProvided=" + averageProvided
				+ ", roundsParticipated=" + roundsParticipated
				+ ", roundsAsHead=" + roundsAsHead + ", compliantRounds="
				+ compliantRounds + "]";
	}

}
