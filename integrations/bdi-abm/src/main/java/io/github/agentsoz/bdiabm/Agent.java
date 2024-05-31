package io.github.agentsoz.bdiabm;

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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;

public interface Agent {

	/**
	 * Initialises the agent with the given arguments
	 * @param args incoming arguments
	 */
	public void init(String[] args);
	
	/** 
	 * Starts an initialised agent
	 */
	public void start();
	
	/**
	 * Handles a new percept
	 * @param perceptID the percept's ID
	 * @param parameters the percept data
	 */
	public abstract void handlePercept(String perceptID, Object parameters);


    /**
     * Updates the status of an action with the given content
     * @param actionID the action's ID
     * @param content the updated action data
     */
	public abstract void updateAction(String actionID, ActionContent content);

	/**
	 * Terminates the agent
	 */
	public abstract void kill();

	/**
	 * Sets the implementation for the {@link QueryPerceptInterface}
	 * @param queryInterface the interface to use
	 */
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface);

	/**
	 * Returns an object that implements {@link QueryPerceptInterface}
	 * @return the implementing object
	 */
	public QueryPerceptInterface getQueryPerceptInterface();

	/**
	 * Sets the implementation for the {@link EnvironmentActionInterface}
	 * @param envActInterface the interface to use
	 */
	public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface);

	/**
	 * Returns an object that implements {@link EnvironmentActionInterface}
	 * @return the interface in use
	 */
	public EnvironmentActionInterface getEnvironmentActionInterface();


}
