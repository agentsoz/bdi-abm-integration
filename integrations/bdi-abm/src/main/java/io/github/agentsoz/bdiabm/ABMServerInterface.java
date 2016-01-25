package io.github.agentsoz.bdiabm;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.PerceptContent;

public interface ABMServerInterface {
	public void takeControl(AgentDataContainer agentDataContainer);

	/**
	 * This is the ad hoc method for query percept.
	 * 
	 * Example query: agentsID: agentX, perceptID: REQUEST_LOCATION,
	 * returns the response: coordinates of agentX
	 * 
	 * @param agentID
	 *            ID of the agent
	 * @param perceptID
	 *            Query is similar to the percept_types defined in
	 *            {@link PerceptContent} class
	 * @return The response for the query
	 */
	public Object queryPercept(String agentID, String perceptID);

}
