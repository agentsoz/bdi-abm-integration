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

import io.github.agentsoz.bdiabm.data.ActionContent;

public interface Agent {

	/**
	 * Initialises the agent with the given arguments
	 * @param args
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
	 * Packages a new action for the ABM
	 * @param actionID the action's ID
	 * @param parameters the action's data
	 */
    public void packageAction(String actionID, Object[] parameters);

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
	
}
