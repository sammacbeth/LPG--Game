package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class CompliantRoundsDesc extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public CompliantRoundsDesc(Cluster c) {
		super(c, new Comparator<PlayerHistory>() {
			@Override
			public int compare(PlayerHistory o1, PlayerHistory o2) {
				return Double.compare(o2.getCompliantRounds(),
						o1.getCompliantRounds());
			}
		});
	}

	@Override
	public Canon getCanon() {
		return Canon.F6;
	}
}