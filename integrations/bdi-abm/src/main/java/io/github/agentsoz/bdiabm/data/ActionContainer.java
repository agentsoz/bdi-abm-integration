package io.github.agentsoz.bdiabm.data;

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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gson.Gson;

/**
 * This class stores all actions created by the agent
 */
public class ActionContainer implements Serializable {
	private static final long serialVersionUID = -8535318524134143574L;

	/**
	 * {@link LinkedHashMap} where all the {@link ActionContent}s are stored. Keys are
	 * action types, values are {@link ActionContent}s
	 */
	private LinkedHashMap<String, ActionContent> container;

	/**
	 * Public constructor
	 */
	public ActionContainer() {
		this(new LinkedHashMap<String, ActionContent>());
	}

	/**
	 * Public constructor
	 * 
	 * @param container
	 *            {@link LinkedHashMap} of actions (keys are action types, values are
	 *            {@link ActionContent}s)
	 */
	public ActionContainer(LinkedHashMap<String, ActionContent> container) {
		this.container = container;
	}

	/**
	 * @return How many actions are stored in the container
	 */
	public int size() {
		return container.size();
	}

	/**
	 * 
	 * @return Return the set of action id in the container
	 */
	public Set<String> actionIDSet() {
		return container.keySet();
	}

	/**
	 * Shallow copy of the container
	 * 
	 * @param x
	 *            The AbtractActionContainer which content want to be referred
	 *            to. (Shallow copy)
	 */
	public void copyContainer(ActionContainer x) {
		container = x.container;
	}

	/**
	 * Register action to container action will be started with state INITIATED
	 * 
	 * @param identifier
	 * @param parameters
	 * @return true if registration is successful. Only if the action's id to be
	 *         registered is not contained yet in the container, then
	 *         registration could be made.
	 */
	public boolean register(String identifier, Object[] parameters) {
		boolean isValid = false;
		if (!container.containsKey(identifier)) {
			isValid = true;
			container
					.put(identifier, new ActionContent(parameters, identifier));
		}
		return isValid;
	}

	/**
	 * Remove an action from container
	 * 
	 * @param identifier
	 *            action's id
	 * @return The content which is removed
	 */
	public ActionContent remove(String identifier) {
		return container.remove(identifier);
	}

	/**
	 * get the respective Content of action
	 * 
	 * @param identifier
	 *            action's id
	 * @return content of action
	 */
	public ActionContent get(String identifier) {

		return container.get(identifier);
	}

	/**
	 * @return true if the container does not contain any actions
	 */
	public boolean isEmpty() {
		return container.isEmpty();
	}

	@Override
	public String toString() {
		return (isEmpty()) ? "{}" : new Gson().toJson(this);
	}
}
