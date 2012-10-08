package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class RoundsAsHeadDesc extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public RoundsAsHeadDesc(Cluster c) {
		super(c, new Comparator<PlayerHistory>() {
			@Override
			public int compare(PlayerHistory o1, PlayerHistory o2) {
				return Double.compare(o2.getRoundsAsHead(),
						o1.getRoundsAsHead());
			}
		});
	}

	@Override
	public Canon getCanon() {
		return Canon.F5;
	}
}
