package uk.ac.imperial.lpgdash.facts;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class PlayerHistory {

	double averageAllocated = 0;
	public SummaryStatistics allocations = new SummaryStatistics();
	int roundsAllocated = 0;
	double averageDemanded = 0;
	public SummaryStatistics demands = new SummaryStatistics();
	double satisfaction = 0.5;
	double averageProvided = 0;
	public SummaryStatistics provisions = new SummaryStatistics();
	int roundsParticipated = 0;
	int roundsAsHead = 0;
	int compliantRounds = 0;

	public PlayerHistory() {
		super();
	}

	public double getAverageAllocated() {
		return allocations.getMean();
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

	public void incrementRoundsAllocated() {
		this.roundsAllocated++;
	}

	public double getAverageDemanded() {
		return demands.getMean();
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
		return provisions.getMean();
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

	public void incrementRoundsParticipated() {
		roundsParticipated++;
	}

	public int getRoundsAsHead() {
		return roundsAsHead;
	}

	public void setRoundsAsHead(int roundsAsHead) {
		this.roundsAsHead = roundsAsHead;
	}

	public void incrementRoundsAsHead() {
		roundsAsHead++;
	}

	public int getCompliantRounds() {
		return compliantRounds;
	}

	public void setCompliantRounds(int compliantRounds) {
		this.compliantRounds = compliantRounds;
	}

	public void incrementCompliantRounds() {
		compliantRounds++;
	}

	@Override
	public String toString() {
		return "PlayerHistory [averageAllocated=" + getAverageAllocated()
				+ ", roundsAllocated=" + roundsAllocated + ", averageDemanded="
				+ getAverageDemanded() + ", satisfaction=" + satisfaction
				+ ", averageProvided=" + getAverageProvided()
				+ ", roundsParticipated=" + roundsParticipated
				+ ", roundsAsHead=" + roundsAsHead + ", compliantRounds="
				+ compliantRounds + "]";
	}

}
