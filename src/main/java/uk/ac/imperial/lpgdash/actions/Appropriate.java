package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.Action;

public class Appropriate implements Action {

	Player player;
	double quantity;

	public Appropriate(double quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Appropriate [player=" + player.getId() + ", quantity="
				+ quantity + "]";
	}
}
