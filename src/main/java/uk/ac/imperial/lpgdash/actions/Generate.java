package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.util.random.Random;

public class Generate {

	public final Player player;
	public final double g;
	public final double q;

	public Generate(Player player) {
		super();
		this.player = player;
		this.g = Random.randomDouble();
		this.q = Random.randomDouble();
	}

	public Player getPlayer() {
		return player;
	}

	public double getG() {
		return g;
	}

	public double getQ() {
		return q;
	}

}
