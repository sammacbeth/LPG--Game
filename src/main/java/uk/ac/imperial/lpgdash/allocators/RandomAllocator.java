package uk.ac.imperial.lpgdash.allocators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.drools.runtime.StatefulKnowledgeSession;

import uk.ac.imperial.lpgdash.actions.Allocate;
import uk.ac.imperial.lpgdash.facts.Player;

public class RandomAllocator {

	public static Random rnd;
	
	public static void allocate(StatefulKnowledgeSession session,
			List<Player> players, double poolSize, int t) {
		players = new ArrayList<Player>(players);
		Collections.shuffle(players, rnd);
		for (Player p : players) {
			double allocation = Math.min(p.getD(), poolSize);
			session.insert(new Allocate(p, allocation, t));
			poolSize -= allocation;
		}
	}
}
