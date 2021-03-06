package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class AverageDemandedAsc extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public AverageDemandedAsc(Cluster c) {
		super(c, new Comparator<PlayerHistory>() {
			@Override
			public int compare(PlayerHistory o1, PlayerHistory o2) {
				return Double.compare(o1.getAverageDemanded(),
						o2.getAverageDemanded());
			}
		});
	}

	@Override
	public Canon getCanon() {
		return Canon.F2;
	}

}
