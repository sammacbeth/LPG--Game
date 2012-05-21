package uk.ac.imperial.lpgdash;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

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
import uk.ac.imperial.presage2.core.simulator.SimTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LPGService extends EnvironmentService {

	private final Logger logger = Logger.getLogger(this.getClass());
	final StatefulKnowledgeSession session;
	Map<UUID, Player> players = new HashMap<UUID, Player>();

	RoundType round = RoundType.INIT;

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
			session.insert(new Round(SimTime.get().intValue(),
					RoundType.APPROPRIATE));
		} else {
			round = RoundType.DEMAND;
			session.insert(new Round(SimTime.get().intValue(), RoundType.DEMAND));
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

	public RoundType getRound() {
		return round;
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
		Collection<Object> rawMembers = session.getObjects(new ObjectFilter() {
			@Override
			public boolean accept(Object object) {
				if (object instanceof MemberOf) {
					MemberOf m = (MemberOf) object;
					if (m.getPlayer().getId().equals(player)) {
						return true;
					}
				}
				return false;
			}
		});
		if (rawMembers.size() == 1) {
			return ((MemberOf) rawMembers.iterator().next()).getCluster();
		}
		return null;
	}

}
