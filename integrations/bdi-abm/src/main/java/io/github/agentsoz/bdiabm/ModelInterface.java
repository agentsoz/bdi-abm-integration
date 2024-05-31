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

public interface ModelInterface {
	/**
	 * Initialises the model
	 * @param params arguments to initialise with
	 */
	public void init(Object[] params);

	/**
	 * Starts the model
	 */
	public void start();

	/**
	 * Steps the model up to and including the given time step
	 * @param time the time to progress the model to
	 * @param args incoming data
	 * @return outgoing data
	 */
	public Object[] step(double time, Object[] args);

	/**
	 * Terminates the model
	 */
	public void finish();

}
