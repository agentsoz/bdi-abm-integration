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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gson.Gson;

/**
 * This class stores all percepts of the agent
 */
public class PerceptContainer implements Serializable {
	private static final long serialVersionUID = -6502115824895738388L;

	/**
	 * {@link LinkedHashMap} where all the actions are stored. Key field is the
	 * percept's id, value field is the instance of{@link PerceptContent}, in
	 * which all information about the percept is stored.
	 */
	private LinkedHashMap<String, PerceptContent> container;

	/**
	 * @return Returns true if there are no percepts are stored in the container
	 */
	public synchronized boolean isEmpty() {
		return container.isEmpty();
	}

	/**
	 * Public constructor
	 */
	public PerceptContainer() {
		this(new LinkedHashMap<String, PerceptContent>());
	}

	/**
	 * Public constructor
	 * 
	 * @param container
	 *            is a {@link LinkedHashMap} where information about all percepts are
	 *            stored
	 */
	public PerceptContainer(LinkedHashMap<String, PerceptContent> container) {
		this.container = container;
	}

	/**
	 * @return IDs of all percepts
	 */
	public synchronized Set<String> perceptIDSet() {
		return container.keySet();
	}

	/**
	 * Shallow copy of the container
	 * 
	 * @param x
	 *            The AbtractActionContainer which content want to be referred
	 *            to. (Shallow copy)
	 */
	private void copyContainer(PerceptContainer x) {
		container = x.container;
	}

	/**
	 * Deep copy
	 * @param pc the container to copy from
	 */
	public synchronized void copy(PerceptContainer pc) {
		// shallow copy if the incoming container is null or empty
		if (pc.container == null) {
			return;
		}
		// else start by clearing this container
		clear();
		// now do a deep copy
		for (String key : pc.container.keySet()) {
			PerceptContent value = pc.container.get(key);
			container.put(key, value);
		}
	}

	/**
	 * Empties the container
	 */
	public synchronized void clear() {
		container.clear();
	}

	/**
	 * Read the value of a percept, and remove it from the container.
	 * 
	 * @param identifier
	 *            percept's id
	 * @return value of percept, or null if it does not exists
	 */
	public synchronized Object read(String identifier) {
		return container.remove(identifier).getValue();
	}

	/**
	 * put the percept inside container, if previously a percept with the same
	 * ID exist it will replace the old value.
	 * 
	 * @param identifier
	 *            percept's id
	 * @param value
	 *            percept's value
	 */
	public synchronized void put(String identifier, Object value) {
		container.put(identifier, new PerceptContent(identifier, value));
	}

	@Override
	public synchronized String toString() {
		return (isEmpty()) ? "{}" : new Gson().toJson(this);
	}
}
