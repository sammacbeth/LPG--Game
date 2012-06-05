package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

abstract class PlayerHistoryComparator implements Comparator<Player> {

	Cluster c;
	Comparator<PlayerHistory> delegate;

	PlayerHistoryComparator(Cluster c, Comparator<PlayerHistory> delegate) {
		super();
		this.c = c;
		this.delegate = delegate;
	}

	@Override
	public int compare(Player o1, Player o2) {
		PlayerHistory h1 = o1.getHistory().get(c);
		PlayerHistory h2 = o2.getHistory().get(c);
		if (h1 == null && h2 == null)
			return 0;
		else if (h1 == null) {
			return 1;
		} else if (h2 == null) {
			return -1;
		}
		return delegate.compare(h1, h2);
	}

}