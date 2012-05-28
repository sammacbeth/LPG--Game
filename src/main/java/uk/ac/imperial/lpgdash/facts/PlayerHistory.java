package uk.ac.imperial.lpgdash.facts;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class PlayerHistory {

	double averageAllocated = 0;
	int roundsAllocated = 0;
	double averageDemanded = 0;
	final SummaryStatistics satisfaction = new SummaryStatistics();
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

	public SummaryStatistics getSatisfaction() {
		return satisfaction;
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
				+ averageDemanded + ", averageSatifaction="
				+ satisfaction.getMean() + ", averageProvided="
				+ averageProvided + ", roundsParticipated="
				+ roundsParticipated + "]";
	}

}
