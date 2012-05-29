package uk.ac.imperial.lpgdash.actions;

public class Appropriate extends PlayerAction {

	double quantity;

	public Appropriate(double quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Appropriate [quantity=" + quantity + ", player=" + player.getName()
				+ ", t=" + t + "]";
	}

	public double getQuantity() {
		return quantity;
	}
}
