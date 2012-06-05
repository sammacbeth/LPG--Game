package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class F1a extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public F1a(Cluster c) {
		super(c, new Comparator<PlayerHistory>() {
			@Override
			public int compare(PlayerHistory o1, PlayerHistory o2) {
				return o1.getRoundsAllocated() - o2.getRoundsAllocated();
			}
		});
	}

}
