package uk.ac.imperial.lpgdash.actions;

import uk.ac.imperial.presage2.core.Action;

abstract class TimestampedAction implements Action {

	int t;

	TimestampedAction() {
		super();
	}

	TimestampedAction(int t) {
		super();
		this.t = t;
	}

	public int getT() {
		return t;
	}

	public void setT(int t) {
		this.t = t;
	}

}
