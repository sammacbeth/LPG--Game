package uk.ac.imperial.lpgdash.allocators.canons;

import java.util.Comparator;

import uk.ac.imperial.lpgdash.facts.Player;

public interface LegitimateClaimsCanon extends Comparator<Player> {

	Canon getCanon();

}
