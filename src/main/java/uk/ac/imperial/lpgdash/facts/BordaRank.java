package uk.ac.imperial.lpgdash.facts;

public class BordaRank {

	final Player player;

	int f1;
	int f1a;
	int f2;
	int f3;
	int f4;

	public BordaRank(Player player) {
		super();
		this.player = player;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((player == null) ? 0 : player.hashCode());
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
		BordaRank other = (BordaRank) obj;
		if (player == null) {
			if (other.player != null)
				return false;
		} else if (!player.equals(other.player))
			return false;
		return true;
	}

	public int getF1() {
		return f1;
	}

	public void setF1(int f1) {
		this.f1 = f1;
	}

	public int getF1a() {
		return f1a;
	}

	public void setF1a(int f1a) {
		this.f1a = f1a;
	}

	public int getF2() {
		return f2;
	}

	public void setF2(int f2) {
		this.f2 = f2;
	}

	public int getF3() {
		return f3;
	}

	public void setF3(int f3) {
		this.f3 = f3;
	}

	public int getF4() {
		return f4;
	}

	public void setF4(int f4) {
		this.f4 = f4;
	}

	public Player getPlayer() {
		return player;
	}

	@Override
	public String toString() {
		return "BordaRank [player=" + player.getId() + ", f1=" + f1 + ", f1a=" + f1a
				+ ", f2=" + f2 + ", f3=" + f3 + ", f4=" + f4 + "]";
	}
}