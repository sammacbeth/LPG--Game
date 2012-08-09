package uk.ac.imperial.lpgdash;

import uk.ac.imperial.presage2.core.cli.Presage2CLI;

public class LPGCLI extends Presage2CLI {

	protected LPGCLI() {
		super(LPGCLI.class);
	}

	public static void main(String[] args) {
		Presage2CLI cli = new LPGCLI();
		cli.invokeCommand(args);
	}

	@Command(name = "insert", description = "Insert a batch of simulations to run.")
	public void insert_batch(String[] args) {
		System.out.println(args);
	}

}
