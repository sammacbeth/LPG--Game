package uk.ac.imperial.lpgdash.facts;

public class Cluster {

	int id;
	Allocation allocationMethod;

	public Cluster(int id, Allocation allocationMethod) {
		super();
		this.id = id;
		this.allocationMethod = allocationMethod;
	}

	@Override
	public String toString() {
		return "Cluster " + id + "";
	}

	public Allocation getAllocationMethod() {
		return allocationMethod;
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
