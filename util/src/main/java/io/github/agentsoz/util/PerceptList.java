package io.github.agentsoz.util;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
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

public final class PerceptList {
	public static final String ARRIVED = "arrived";
	public static final String ACTIVITY_STARTED = "activity_started";
	public static final String ACTIVITY_ENDED = "activity_ended";
	public static final String BLOCKED = "blocked";
//added oemer
	public static final String SUM_LINK_LENGTH = "sum_link_length";
	public static final String BROADCAST = "global";
	public static final String CONGESTION = "congestion";
	public static final String DEPARTED = "departed";
	public static final String STUCK = "stuck";
	public static final String TIME = "time";

	// Used to control the simulation run and data passing between BDI and ABM models
	public static final String TAKE_CONTROL_BDI = "take_control_bdi";
	public static final String TAKE_CONTROL_ABM = "take_control_abm";
	public static final String AGENT_DATA_CONTAINER_FROM_BDI = "agent_data_container_from_bdi";
	public static final String AGENT_DATA_CONTAINER_FROM_ABM = "agent_data_container_from_abm";

    // BDI Query Percept strings

    public static final String REQUEST_LOCATION = "request_location";
	public static final String REQUEST_DESTINATION_COORDINATES = "request_destination_coordinates";


	/**
	 * REQUEST_DRIVING_DISTANCE_TO works as follows:
	 * <ul>
	 *     <li>Requires double[] coordinates to calculate the distance to; </li>
	 *     <li>start location is assumed to be the "from node" of the agent's current link; since the agent is already on
	 *     this link, then the result is somewhat pessimistic</li>
	 *     <li>end location is assumed to be the "from node" of the link closest to the destination coordinates;</li>
	 *     <li>uses the free speed router to calculate the least cost distance to the destination</li>
	 * </ul>
	 */
	public static final String REQUEST_DRIVING_DISTANCE_TO = "request_driving_distance_to";


}

