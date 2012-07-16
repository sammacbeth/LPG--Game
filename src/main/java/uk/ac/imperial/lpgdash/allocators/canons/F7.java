package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Collections;
import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class F7 extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public F7(Cluster c) {
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
		return Canon.F7;
	}
}