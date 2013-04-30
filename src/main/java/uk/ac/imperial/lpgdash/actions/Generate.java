package uk.ac.imperial.lpgdash.actions;

import java.util.Random;

import uk.ac.imperial.lpgdash.facts.Player;

public class Generate extends TimestampedAction {

	public final Player player;
	public final double g;
	public final double q;

	public Generate(Player player, int time, Random rnd) {
		super(time);
		this.player = player;
		this.g = rnd.nextDouble() * player.getSizeMultiplier();
		this.q = this.g
				+ (rnd.nextDouble() * (player.getSizeMultiplier() - this.g));
	}

	public Generate(Player player, int time) {
		super(time);
		this.player = player;
		this.g = uk.ac.imperial.presage2.core.util.random.Random.randomDouble()
				* player.getSizeMultiplier();
		this.q = this.g
				+ (uk.ac.imperial.presage2.core.util.random.Random
						.randomDouble() * (player.getSizeMultiplier() - this.g));
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

	@Override
	public String toString() {
		return "Generate [player=" + player.getName() + ", g=" + g + ", q=" + q
				+ ", t=" + t + "]";
	}

}
