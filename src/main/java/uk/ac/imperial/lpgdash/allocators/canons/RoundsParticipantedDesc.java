package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class RoundsParticipantedDesc extends PlayerHistoryComparator implements
		LegitimateClaimsCanon {

	public RoundsParticipantedDesc(Cluster c) {
		super(c, new Comparator<PlayerHistory>() {
			@Override
			public int compare(PlayerHistory o1, PlayerHistory o2) {
				return Double.compare(o2.getRoundsParticipated(),
						o1.getRoundsParticipated());
			}
		});
	}

	@Override
	public Canon getCanon() {
		return Canon.F4;
	}
}
