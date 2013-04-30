package uk.ac.imperial.lpgdash;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.QueryResults;
import org.drools.runtime.rule.QueryResultsRow;

import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.MemberOf;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.lpgdash.facts.Round;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationRequest;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LPGService extends EnvironmentService {

	private final Logger logger = Logger.getLogger(this.getClass());
	final StatefulKnowledgeSession session;
	Map<UUID, Player> players = new HashMap<UUID, Player>();
	Map<UUID, MemberOf> members = new HashMap<UUID, MemberOf>();
	Set<Cluster> clusters = new HashSet<Cluster>();

	RoundType round = RoundType.INIT;
	int roundNumber = 0;
	private int numClusters = 0;

	@Inject
	protected LPGService(EnvironmentSharedStateAccess sharedState,
			StatefulKnowledgeSession session, EventBus eb) {
		super(sharedState);
		this.session = session;
		eb.subscribe(this);
	}

	@EventListener
	public void onIncrementTime(EndOfTimeCycle e) {
		if (round == RoundType.DEMAND) {
			round = RoundType.APPROPRIATE;
			session.insert(new Round(roundNumber, RoundType.APPROPRIATE));
		} else {
			round = RoundType.DEMAND;
			session.insert(new Round(++roundNumber, RoundType.DEMAND));
		}
		logger.info("Next round: " + round);
	}

	@Override
	public void registerParticipant(EnvironmentRegistrationRequest req) {

	}

	private synchronized Player getPlayer(final UUID id) {
		if (!players.containsKey(id)) {
			Collection<Object> rawPlayers = session
					.getObjects(new ObjectFilter() {
						@Override
						public boolean accept(Object object) {
							return object instanceof Player;
						}
					});
			for (Object pObj : rawPlayers) {
				Player p = (Player) pObj;
				players.put(p.getId(), p);
			}
		}
		return players.get(id);
	}

	private synchronized MemberOf getMemberOf(final UUID id) {
		MemberOf m = members.get(id);
		if (m == null || session.getFactHandle(m) == null) {
			members.remove(id);
			QueryResults results = session.getQueryResults("getMemberOf",
					new Object[] { getPlayer(id) });
			for (QueryResultsRow row : results) {
				members.put(id, (MemberOf) row.get("m"));
				return members.get(id);
			}
			return null;
		}
		return m;
	}

	public RoundType getRound() {
		return round;
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public double getG(UUID player) {
		return getPlayer(player).getG();
	}

	public double getQ(UUID player) {
		return getPlayer(player).getQ();
	}

	public double getAllocated(UUID player) {
		return getPlayer(player).getAllocated();
	}

	public double getAppropriated(UUID player) {
		return getPlayer(player).getAppropriated();
	}

	public Cluster getCluster(final UUID player) {
		MemberOf m = getMemberOf(player);
		if (m != null)
			return m.getCluster();
		else
			return null;
	}

	public synchronized Set<Cluster> getClusters() {
		// If we allow for dynamic creation of clusters, we must check which ones exist every time
		if (this.clusters.size() == 0) {
			Collection<Object> clusterSearch = session
					.getObjects(new ObjectFilter() {

						@Override
						public boolean accept(Object object) {
							return object instanceof Cluster;
						}
					});
			for (Object object : clusterSearch) {
				this.clusters.add((Cluster) object);
			}
		}
		return Collections.unmodifiableSet(this.clusters);
	}
	
	public int getNumClusters(){
		return getClusters().size();
	}
	
	public int getNextNumCluster(){
		this.clusters.clear();
		return numClusters++;
	}

	public Set<UUID> getOrphanPlayers(){
		Set<UUID> op = new HashSet<UUID>();
		for (Entry<UUID, Player> p : this.players.entrySet()){
			if (getCluster(p.getKey()) == null){
				op.add(p.getKey());
			}
		}
		return op;
	}
	
}
