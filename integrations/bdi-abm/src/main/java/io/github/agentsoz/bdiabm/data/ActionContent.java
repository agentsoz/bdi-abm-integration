package io.github.agentsoz.bdiabm.data;

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

import java.io.*;

import com.google.gson.Gson;

/**
 * This class is used to store instance_id, action_type, parameters and status
 * of actions which are stored in {@link ActionContainer}
 * 
 * Example BDI action: instance_id = #1, action_type: DRIVE_TO, parameters:
 * coordinates of the destination, state: INITIATED
 * 
 */
public class ActionContent implements Serializable {

	/**
	 * Possible Action States.
	 */
	public enum State {
		INITIATED, RUNNING, PASSED, FAILED, SUSPENDED, DROPPED
	}

	/**
	 * State of the action
	 */
	private State state;

	/**
	 * Parameters, which should be stored with the action to use when processing
	 * the action
	 */
	private Object[] parameters;

	/**
	 * Using this field, users can assign instance IDs for action contents
	 */
	private String instance_id;

	/**
	 * Type of the action. This is same as the key string used in
	 * {@link ActionContainer}'s LinkedHashMap
	 */
	private String action_type;

	private static final long serialVersionUID = 2449238431892599285L;

	/**
	 * Public constructor
	 * 
	 * @param parameters
	 *            is the any objects that should be stored with the action for
	 *            future use. eg: coordinates of the destination.
	 * @param state
	 *            is the state of the action. eg: INITIATED
	 * @param action_type
	 *            is the identifier of the action. eg: DRIVE_TO
	 */
	public ActionContent(Object[] parameters, State state, String action_type) {
		this.parameters = parameters;
		this.state = state;
		this.action_type = action_type;
	}

	/**
	 * Public constructor. This constructor sets action to INITIATED state.
	 * 
	 * @param parameters
	 *            is the any objects that should be stored with the action for
	 *            future use. eg: coordinates of the destination.
	 * @param action_type
	 *            is the identifier of the action. eg: DRIVE_TO
	 */
	public ActionContent(Object[] parameters, String action_type) {
		this(parameters, State.INITIATED, action_type);
	}

	/**
	 * @return The action's parameter values
	 */
	public Object[] getParameters() {
		return parameters;
	}

	/**
	 * @return The action's state
	 */
	public State getState() {
		return state;
	}

	/**
	 * Set the parameter's values
	 * 
	 * @param parameters action parameter values
	 */
	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	/**
	 * Set the state of action
	 * 
	 * @param state the new state
	 */
	public void setState(State state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	/**
	 * @return The ID used for the action instance.
	 */
	public String getInstance_id() {
		return instance_id;
	}

	/**
	 * Set an ID for the action
	 * 
	 * @param instance_id the unique id of this instance of the action
	 */
	public void setInstance_id(String instance_id) {
		this.instance_id = instance_id;
	}

	/**
	 * @return The action type
	 */
	public String getAction_type() {
		return action_type;
	}

	/**
	 * Set the action type (identifier)
	 * 
	 * @param action_type the type of this action
	 */
	public void setAction_type(String action_type) {
		this.action_type = action_type;
	}
}
