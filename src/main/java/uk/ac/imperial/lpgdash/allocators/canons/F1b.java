package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class F1b extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public F1b(Cluster c) {
		super(c, new Comparator<PlayerHistory>() {
			@Override
			public int compare(PlayerHistory o1, PlayerHistory o2) {
				return Double.compare(o1.getSatisfaction(),
						o2.getSatisfaction());
			}
		});
	}

	@Override
	public Canon getCanon() {
		return Canon.F1b;
	}
}