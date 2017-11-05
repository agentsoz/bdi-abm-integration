package io.github.agentsoz.bushfiretute.matsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.SearchableNetwork;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimActionList;
import io.github.agentsoz.bdimatsim.MATSimAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.MATSimPerceptList;
import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bushfiretute.BDIModel;
import scenarioTWO.agents.EvacResident;

final class DRIVETOActionHandler implements BDIActionHandler {
	private BDIModel bdiModel;

	public DRIVETOActionHandler(BDIModel bdiModel) {
		this.bdiModel = bdiModel;
	}

	@Override
	public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
		// Get nearest link ID and calls the CustomReplanner to map to MATSim.
		Id<Link> newLinkId;
		double[] coords = (double[]) args[1];
		if (args[1] instanceof double[]) {
			newLinkId = ((SearchableNetwork) model.getScenario().getNetwork())
					.getNearestLinkExactly(new Coord(coords[0], coords[1])).getId();
		} else {
			throw new RuntimeException("Destination coordinates are not given");
		}

		final String dest = (String) args[2];
		((CustomReplanner)model.getReplanner()).moveToWaitAtOtherLocation(Id.createPersonId(agentID), newLinkId, dest);

		// Now register a event handler for when the agent arrives at the destination
		MATSimAgent agent = model.getBDIAgent(agentID);
		EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
		bdiAgent.log("has started driving to coords "+coords[0] + "," + coords[1] 
				+" i.e. link "+newLinkId.toString());
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(), 
				MonitoredEventType.ArrivedAtDestination, 
				newLinkId,
				new BDIPerceptHandler() {
					@Override
					public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
						MATSimAgent agent = model.getBDIAgent(agentId);
						EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
						Object[] params = { linkId.toString() , Long.toString(bdiAgent.getCurrentTime())};

						agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
						// (yyyy probably does not make a difference in terms of current results, but: Shouldn't this be
						// called earlier, maybe around where the replanner is called?  kai, oct'17)
						
						agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
						return true; //unregister this handler
					}
				});
		return true;
	}
}