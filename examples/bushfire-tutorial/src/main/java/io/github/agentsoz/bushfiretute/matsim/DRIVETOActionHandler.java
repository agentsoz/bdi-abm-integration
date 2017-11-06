package io.github.agentsoz.bushfiretute.matsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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