package uk.ac.imperial.lpgdash.facts;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class Cluster {

	final int id;
	Allocation allocationMethod;
	final SummaryStatistics fairnessData;

	public Cluster(int id, Allocation allocationMethod) {
		super();
		this.id = id;
		this.allocationMethod = allocationMethod;
		this.fairnessData = new SummaryStatistics();
	}

	@Override
	public String toString() {
		return "Cluster " + id + "";
	}

	public int getId() {
		return id;
	}

	public Allocation getAllocationMethod() {
		return allocationMethod;
	}

	public SummaryStatistics getFairnessData() {
		return fairnessData;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cluster other = (Cluster) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
