package uk.ac.imperial.lpgdash.facts;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class Cluster {

	final int id;
	Allocation allocationMethod;
	final SummaryStatistics fairnessData;
	double monitoringLevel = 0.0;
	double monitoringCost = 0.0;

	public Cluster(int id, Allocation allocationMethod) {
		super();
		this.id = id;
		this.allocationMethod = allocationMethod;
		this.fairnessData = new SummaryStatistics();
	}

	public Cluster(int id, Allocation allocationMethod, double monitoringLevel,
			double monitoringCost) {
		this(id, allocationMethod);
		this.monitoringLevel = monitoringLevel;
		this.monitoringCost = monitoringCost;
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

	public double getMonitoringLevel() {
		return monitoringLevel;
	}

	public double getMonitoringCost() {
		// normalise against average provision
		return monitoringCost*0.5;
	}

	public boolean isLC() {
		return allocationMethod == Allocation.LC_F1a
				|| allocationMethod == Allocation.LC_F1b
				|| allocationMethod == Allocation.LC_F1c
				|| allocationMethod == Allocation.LC_F2
				|| allocationMethod == Allocation.LC_F3
				|| allocationMethod == Allocation.LC_F4
				|| allocationMethod == Allocation.LC_F5
				|| allocationMethod == Allocation.LC_F6
				|| allocationMethod == Allocation.LC_FIXED
				|| allocationMethod == Allocation.LC_SO;
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
