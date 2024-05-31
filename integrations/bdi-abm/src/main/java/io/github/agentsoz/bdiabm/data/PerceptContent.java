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

import com.google.gson.Gson;

/**
 * This class is used to store percept_type, parameters and value of percepts
 * which are stored in {@link PerceptContainer}
 * 
 * Example percept : percept_type: ARRIVED_AT_DESTINATION, parameters: time
 * taken to arrive at destination, value: desination's coordinates
 * 
 * IMPORTANT: The ad hoc query percept can be found in
 * agentsoz.bdiabm.ABMServerInterface class as the queryPercep method.
 */
public class PerceptContent implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Type of the percept
	 */
	private String percept_type;

	/**
	 * Parameters, which should be stored with percept for processing
	 */
	private Object[] parameters;

	/**
	 * Percept's value
	 */
	private Object value;

	/**
	 * Constructor
	 * @param percept_type the type of this percept
	 * @param parameters any meta data associated with this percept
	 * @param value percept data
	 */
	public PerceptContent(String percept_type, Object[] parameters, Object value) {
		this.percept_type = percept_type;
		this.parameters = parameters;
		this.value = value;
	}

	/**
	 * Constructor
	 * @param percept_type the type of this percept
	 * @param value percept data
	 */
	public PerceptContent(String percept_type, Object value) {
		this(percept_type, null, value);
	}

	/**
	 * @return The type of the percept
	 */
	public String getPercept_type() {
		return percept_type;
	}

	/**
	 * Sets the type of the percept
	 * 
	 * @param percept_type the type of this percept
	 */
	public void setPercept_type(String percept_type) {
		this.percept_type = percept_type;
	}

	/**
	 * @return The parameters stored with the percept
	 */
	public Object[] getParameters() {
		return parameters;
	}

	/**
	 * Sets parameters of the percept
	 * 
	 * @param parameters any meta data associated with this percept
	 */
	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return The value of the percept
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the value of the percept
	 * 
	 * @param value percept data
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
