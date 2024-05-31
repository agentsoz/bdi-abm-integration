package io.github.agentsoz.nonmatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.PerceptContainer;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.network.Link;

/**
 * Agent Object on the ABM side Holds information: an
 *         Agent's actionContainer and perceptContainer, the correct
 *         PerceptHandler and ActionHandler, and lists of driveToActions
 *         
 *         
 * @author Edmund Kemsley 
 */
public final class PAAgent {
	/* Design decisions/changes:
	 * - drive actions were never used, so I removed them.  This should make the agent type more general.  kai, oct/nov'17
	 * - because it was now more general, I renamed it first to AgentWithPerceptsAndActions, and now short to
	 *   PAAgent = PerceptActionAgent.  Could be renamed into something else if that makes more sense from the framework
	 *   perspective.  kai, oct/nov'17
	 */

	private final PerceptHandler perceptHandler;
	private final ActionHandler actionHandler = new ActionHandler();
	private final EventsMonitorRegistry eventsMonitorRegistry;
	private final String agentID;

	private ActionPerceptContainer actPerceptContainer;

	public final String getAgentID() {
		return agentID;
	}

	public final PerceptHandler getPerceptHandler() {
		return perceptHandler;
	}

	public final ActionHandler getActionHandler() {
		return actionHandler;
	}

	public final ActionContainer getActionContainer() {
		return this.actPerceptContainer.getActionContainer();
	}

	public final PerceptContainer getPerceptContainer() {
		return this.actPerceptContainer.getPerceptContainer();
	}

	PAAgent(EventsMonitorRegistry eventsMonitors, String agentID, ActionPerceptContainer actPerceptContainer) {
		this.eventsMonitorRegistry = eventsMonitors;
		this.perceptHandler = new PerceptHandler(eventsMonitors);
		this.agentID = agentID;
		this.actPerceptContainer = actPerceptContainer;
	}

	public boolean hasPersonArrivalEventMonitor() {
		return eventsMonitorRegistry.hasPersonArrivalEventMonitorFor(agentID);
	}

	public void removePersonArrivalEventMonitor() {
		eventsMonitorRegistry.removePersonArrivalEventMonitorFor(agentID);
	}
}
