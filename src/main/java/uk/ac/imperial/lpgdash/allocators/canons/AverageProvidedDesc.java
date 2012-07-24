package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class AverageProvidedDesc extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public AverageProvidedDesc(Cluster c) {
		super(c, new Comparator<PlayerHistory>() {
			@Override
			public int compare(PlayerHistory o1, PlayerHistory o2) {
				return Double.compare(o2.getAverageProvided(),
						o1.getAverageProvided());
			}
		});
	}

	@Override
	public Canon getCanon() {
		return Canon.F3;
	}

}
