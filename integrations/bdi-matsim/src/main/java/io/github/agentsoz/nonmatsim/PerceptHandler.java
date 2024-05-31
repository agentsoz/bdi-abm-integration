package io.github.agentsoz.nonmatsim;

import io.github.agentsoz.bdimatsim.EventsMonitorRegistry;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;

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

/**
 * @author Edmund Kemsley, Dhirendra Singh
 */

public final class PerceptHandler {

	private final EventsMonitorRegistry eventsMonitors;

	/**
	 * Constructor
	 * 
	 * @param eventsMonitors
	 */ 
	protected PerceptHandler(final EventsMonitorRegistry eventsMonitors) {
		this.eventsMonitors = eventsMonitors ;
	}

	/**
	 * For a given agent, registers a {@link BDIPerceptHandler} to be called 
	 * whenever an event of type {@link MonitoredEventType} is triggered
	 * for {@code linkId}. 
	 * @param agentId
	 * @param linkId
	 * @param event
	 * @param handler
	 * @return
	 */
	public int registerBDIPerceptHandler(String agentId, MonitoredEventType event, String linkId, BDIPerceptHandler handler) {
		return eventsMonitors.registerMonitor(agentId, event, linkId, handler);
	}

}
