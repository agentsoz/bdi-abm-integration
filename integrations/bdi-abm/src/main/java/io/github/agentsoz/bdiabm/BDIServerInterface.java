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

import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;

public interface BDIServerInterface
{

	/**
	 * Handles an incoming agent data container
	 * @param agentDataContainer
	 */
	public AgentDataContainer takeControl(double time, AgentDataContainer agentDataContainer);

	/**
	 * Sets the implementation for the {@link QueryPerceptInterface}
	 */
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface);

	/**
	 * Returns an object that implements {@link QueryPerceptInterface}
	 * @return the implementing object
	 */
	public QueryPerceptInterface getQueryPerceptInterface();

	/**
	 * Used to supply an external agent data container for use by this model
	 * @param adc
	 */
	public void setAgentDataContainer(AgentDataContainer adc);

	/**
	 * Returns the agent data container in use by this model
	 * @return
	 */
	public AgentDataContainer getAgentDataContainer();
}
