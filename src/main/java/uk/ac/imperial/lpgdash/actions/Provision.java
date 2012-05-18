package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.Action;

public class Provision implements Action {

	public Player player;
	public double quantity;

	public Provision(double quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Provision [player=" + player.getId() + ", quantity=" + quantity
				+ "]";
	}

	public Double getQuantity() {
		return quantity;
	}
}
