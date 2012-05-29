package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;

public class Allocate extends TimestampedAction {

	Player player;
	double quantity;

	public Allocate(Player p, double quantity, int time) {
		super(time);
		this.player = p;
		this.quantity = quantity;
	}

	public Player getPlayer() {
		return player;
	}

	public double getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return "Allocate [player=" + player.getName() + ", quantity=" + quantity + ", t="
				+ t + "]";
	}

}
