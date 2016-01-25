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
import io.github.agentsoz.bdiabm.data.AgentStateList;

public interface BDIServerInterface
{

	/**
	 * Initialises the BDI system
	 * @param agentDataContainer
	 * @param agentList
	 * @param abmServer
	 * @return
	 */
	public boolean init(AgentDataContainer agentDataContainer, AgentStateList agentList, ABMServerInterface abmServer, Object[] params);
	
	
	/**
	 * Starts the BDI system
	 */
	public void start();
	
	/**
	 * Handles an incoming agent data container
	 * @param agentDataContainer
	 */
	public void takeControl(AgentDataContainer agentDataContainer);

	/**
	 * Terminates the BDI system 
	 */
	public void finish();

}
