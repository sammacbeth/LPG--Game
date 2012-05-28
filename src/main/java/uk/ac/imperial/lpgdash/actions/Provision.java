package uk.ac.imperial.lpgdash.actions;

public class Provision extends PlayerAction {

	public double quantity;

	public Provision(double quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Provision [quantity=" + quantity + ", player=" + player
				+ ", t=" + t + "]";
	}

	public Double getQuantity() {
		return quantity;
	}
}
