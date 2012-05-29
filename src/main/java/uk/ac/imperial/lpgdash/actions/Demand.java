package uk.ac.imperial.lpgdash.actions;

public class Demand extends PlayerAction {

	double quantity;

	public Demand(double quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Demand [quantity=" + quantity + ", player=" + player.getName() + ", t="
				+ t + "]";
	}

	public double getQuantity() {
		return quantity;
	}

}
