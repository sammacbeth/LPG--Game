package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.Action;

public class Demand implements Action {

	Player player;
	double quantity;

	public Demand(double quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Demand [player=" + player.getId() + ", quantity=" + quantity + "]";
	}

}
