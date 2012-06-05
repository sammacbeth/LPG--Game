package uk.ac.imperial.lpgdash.facts;

import java.util.HashMap;
import java.util.Map;

import uk.ac.imperial.lpgdash.allocators.canons.Canon;

public class BordaRank {

	final Player player;

	Map<Canon, Integer> functionRanks = new HashMap<Canon, Integer>();

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

	public int get(Canon c) {
		Integer rank = functionRanks.get(c);
		if (rank == null)
			return 0;
		else
			return rank;
	}

	public void set(Canon c, Integer rank) {
		functionRanks.put(c, rank);
	}

	public Player getPlayer() {
		return player;
	}

	@Override
	public String toString() {
		return "BordaRank [player=" + player.getName() + ", ranks="
				+ functionRanks.toString() + "]";
	}

}
