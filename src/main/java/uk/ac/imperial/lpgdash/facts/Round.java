package uk.ac.imperial.lpgdash.facts;

public class Round {

	public int number;
	public final int roundLength = 1;

	public Round(int number) {
		super();
		this.number = number;
	}

	public int getNumber() {
		return number;
	}

	@Override
	public int hashCode() {
		return number;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Round other = (Round) obj;
		if (number != other.number)
			return false;
		return true;
	}

}
