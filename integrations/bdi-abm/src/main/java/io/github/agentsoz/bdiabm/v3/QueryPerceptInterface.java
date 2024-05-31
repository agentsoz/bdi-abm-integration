package io.github.agentsoz.bdiabm.v3;

/*-
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

/**
 * Interface to provide BDI query percepts
 */
public interface QueryPerceptInterface {
    /**
     * This is the ad hoc method for query percept.
     *
     * Example query: agentsID: agentX, perceptID: REQUEST_LOCATION,
     * returns the response: coordinates of agentX
     *
     * @param agentID ID of the agent
     * @param perceptID the percept to retrieve
     * @param args query percept arguments
     * @return the response for the query
     */
    Object queryPercept(String agentID, String perceptID, Object args) throws AgentNotFoundException;

}
