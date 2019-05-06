package io.github.agentsoz.bdimatsim;

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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.util.ActionList;
import io.github.agentsoz.util.PerceptList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;

public final class ActionHandlerForPerceive implements BDIActionHandler {
	private static final Logger log = Logger.getLogger( ActionHandlerForPerceive.class ) ;

	private final MATSimModel model;

	public ActionHandlerForPerceive(MATSimModel model ) {
		this.model = model;
	}
	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		// assertions:
		Gbl.assertIf(args.length >= 1);

		for(int i=0; i<args.length; i++) {
			Gbl.assertIf(args[i] instanceof String);

			String eventToPerceive = (String) args[i];
			PAAgent paAgent = model.getAgentManager().getAgent(agentID);

			switch (eventToPerceive) {
				case PerceptList.BLOCKED:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(),
							MonitoredEventType.NextLinkBlockedEvent, null, new BDIPerceptHandler() {
								@Override
								public boolean handle(Id<Person> agentId, Id<Link> currentLinkId, MonitoredEventType monitoredEvent) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId.toString());
									Object[] params = {currentLinkId.toString()};
									PerceptContent pc = new PerceptContent(PerceptList.BLOCKED, params[0]);
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.BLOCKED, pc);
									return true;
								}
							}
					);
					break;
				case PerceptList.CONGESTION:
					// And yet another in case the agent gets stuck in congestion on the way
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(), MonitoredEventType.AgentInCongestionEvent,
							null, new BDIPerceptHandler() {
								@Override
								public boolean handle(Id<Person> agentId, Id<Link> currentLinkId, MonitoredEventType monitoredEvent) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId.toString());
									Object[] params = {currentLinkId.toString()};
									PerceptContent pc = new PerceptContent(PerceptList.CONGESTION, params[0]);
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.CONGESTION, pc);
									return true;
								}
							}
					);
					break;
				default:
					throw new RuntimeException("Cannot register for unknown percept type '" + eventToPerceive + "'");
			}
		}
		ActionContent ac = new ActionContent(null, ActionContent.State.PASSED, ActionList.PERCEIVE);
		model.getAgentManager().getAgentDataContainerV2().putAction(agentID, ActionList.PERCEIVE, ac);

		return true;
	}
}