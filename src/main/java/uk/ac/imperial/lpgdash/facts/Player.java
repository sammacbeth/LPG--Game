package uk.ac.imperial.lpgdash.facts;

import java.util.UUID;

public class Player {

	UUID id;
	double g = 0;
	double q = 0;
	double d = 0;
	double allocated = 0;
	double appropriated = 0;

	public Player(UUID aid) {
		super();
		this.id = aid;
	}

	public Player(UUID aid, double g, double q) {
		super();
		this.id = aid;
		this.g = g;
		this.q = q;
	}

	@Override
	public String toString() {
		return "Player [id=" + id + ", g=" + g + ", q=" + q + "]";
	}

	public UUID getId() {
		return id;
	}

	public double getG() {
		return g;
	}

	public void setG(double g) {
		this.g = g;
	}

	public double getQ() {
		return q;
	}

	public void setQ(double q) {
		this.q = q;
	}

	public double getD() {
		return d;
	}

	public void setD(double d) {
		this.d = d;
	}

	public double getAllocated() {
		return allocated;
	}

	public void setAllocated(double allocated) {
		this.allocated = allocated;
	}

	public double getAppropriated() {
		return appropriated;
	}

	public void setAppropriated(double appropriated) {
		this.appropriated = appropriated;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		Player other = (Player) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
