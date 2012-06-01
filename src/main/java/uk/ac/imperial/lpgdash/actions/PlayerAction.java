package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.lpgdash.facts.Player;

abstract class PlayerAction extends TimestampedAction {

	Player player;

	PlayerAction() {
		super();
	}

	PlayerAction(int t, Player player) {
		super(t);
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

}
