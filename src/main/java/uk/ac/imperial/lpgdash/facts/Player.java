package uk.ac.imperial.lpgdash.facts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Player {

	UUID id;
	final String name;
	String type = "C";
	double g = 0;
	double q = 0;
	double d = 0;
	double allocated = 0;
	double appropriated = 0;
	double alpha = 0.1;
	double beta = 0.1;
	Role role = Role.PROSUMER;

	Map<Cluster, PlayerHistory> history = new HashMap<Cluster, PlayerHistory>();

	public Player(UUID aid) {
		super();
		this.id = aid;
		this.name = "n/a";
	}

	public Player(UUID id, String name, String type, double alpha, double beta) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.alpha = alpha;
		this.beta = beta;
	}

	@Override
	public String toString() {
		return "Player [" + name + ", type=" + type +", role=" + role + ", g=" + g + ", q=" + q
				+ "]";
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
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

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Map<Cluster, PlayerHistory> getHistory() {
		return history;
	}

	public double getAlpha() {
		return alpha;
	}

	public double getBeta() {
		return beta;
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
