package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;

public class Demand extends PlayerAction {

	double quantity;

	public Demand(double quantity) {
		this.quantity = quantity;
	}

	public Demand(int t, Player player, double quantity) {
		super(t, player);
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Demand [quantity=" + quantity + ", player=" + player.getName()
				+ ", t=" + t + "]";
	}

	public double getQuantity() {
		return quantity;
	}

}
