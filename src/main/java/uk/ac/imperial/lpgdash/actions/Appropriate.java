package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;

public class Appropriate extends PlayerAction {

	double quantity;

	public Appropriate(double quantity) {
		this.quantity = quantity;
	}

	public Appropriate(int t, Player player, double quantity) {
		super(t, player);
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Appropriate [quantity=" + quantity + ", player="
				+ player.getName() + ", t=" + t + "]";
	}

	public double getQuantity() {
		return quantity;
	}
}
