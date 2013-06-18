package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.util.random.Random;

public class Allocate extends TimestampedAction {

	Player player;
	double quantity;
	double order = 0;

	public Allocate(Player p, double quantity, int time) {
		super(time);
		this.player = p;
		this.quantity = quantity;
		this.order = Random.randomDouble();
	}

	public Allocate(Player p, double quantity, int time, double order) {
		this(p, quantity, time);
		this.order = order;
	}

	public Player getPlayer() {
		return player;
	}

	public double getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return "Allocate [player=" + player.getName() + ", quantity="
				+ quantity + ", t=" + t + "]";
	}

}
