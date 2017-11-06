package io.github.agentsoz.bushfiretute.matsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.SearchableNetwork;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bushfiretute.BDIModel;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.bushfiretute.shared.PerceptID;
import scenarioTWO.agents.EvacResident;

final class DRIVETO_AND_PICKUPActionHandler implements BDIActionHandler {
	private final BDIModel bdiModel;

	public DRIVETO_AND_PICKUPActionHandler(BDIModel bdiModel) {
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

		((CustomReplanner)model.getReplanner()).insertPickupAndWaitAtOtherLocation(Id.createPersonId(agentID), newLinkId, (int) args[3], model);

		// Now register a event handler for when the agent arrives at the destination
		MATSimAgent agent = model.getBDIAgent(agentID);
		EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
		bdiAgent.log("will drive to pickup from coords "+coords[0] + "," + coords[1] 
				+" i.e. link "+newLinkId.toString());
		
		// Now register a event handler for when the agent arrives and finished picking up the destination
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(), 
				MonitoredEventType.EndedActivity, 
				newLinkId,
				new BDIPerceptHandler() {
					@Override
					public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
						MATSimAgent agent = model.getBDIAgent(agentId);
						Object[] params = { linkId.toString() };

						agent.getActionContainer().register(ActionID.DRIVETO_AND_PICKUP, params);
						// (yyyy probably does not make a difference in terms of current results, but: Shouldn't this be
						// called earlier, maybe around where the replanner is called?  kai, oct'17)

						agent.getActionContainer().get(ActionID.DRIVETO_AND_PICKUP).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(PerceptID.ARRIVED_AND_PICKED_UP, params);
						return true; //unregister this handler
					}
				});
		return true;
	}
}