package uk.ac.imperial.lpgdash.actions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.inject.Inject;

import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.Action;
import uk.ac.imperial.presage2.core.environment.ActionHandler;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;

public class LPGActionHandler implements ActionHandler {

	final private Logger logger = Logger.getLogger(LPGActionHandler.class);
	final StatefulKnowledgeSession session;
	Map<UUID, Player> players = new HashMap<UUID, Player>();

	@Inject
	public LPGActionHandler(StatefulKnowledgeSession session,
			EnvironmentServiceProvider serviceProvider)
			throws UnavailableServiceException {
		super();
		this.session = session;
	}

	@Override
	public boolean canHandle(Action action) {
		return (action instanceof Demand || action instanceof Provision
				|| action instanceof Appropriate || action instanceof LeaveCluster);
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

	@Override
	public Input handle(Action action, UUID actor)
			throws ActionHandlingException {
		Player p = getPlayer(actor);
		if (action instanceof Demand) {
			((Demand) action).player = p;
		} else if (action instanceof Provision) {
			((Provision) action).player = p;
		} else if (action instanceof Appropriate) {
			((Appropriate) action).player = p;
		} else if (action instanceof LeaveCluster) {
			((LeaveCluster) action).player = p;
		}
		session.insert(action);
		logger.debug("Handling: " + action);
		return null;
	}
}
