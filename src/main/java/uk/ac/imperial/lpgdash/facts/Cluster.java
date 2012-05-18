package uk.ac.imperial.lpgdash.facts;

public class Cluster {

	int id;

	public Cluster(int id) {
		super();
		this.id = id;
	}

	@Override
	public String toString() {
		return "Cluster " + id + "";
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
