package io.github.agentsoz.util.evac;

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

public final class PerceptList {
	public static final String ARRIVED = "arrived";
	public static final String ARRIVED_CONNECT_TO = "arrived_to_connect_to";
	public static final String ARRIVED_AND_PICKED_UP = "arrived_and_picked_up";
	public static final String BLOCKED = "blocked";
	public static final String BROADCAST = "global";
	public static final String CONGESTION = "congestion";
	public static final String DISRUPTION = "disruption";
	public static final String EMERGENCY_MESSAGE = "emergency_message";
	public static final String FIELD_OF_VIEW = "field_of-view";
	public static final String FIRE = "fire";
	public static final String FIRE_ALERT = "fire_alert";
	public static final String FIRE_DATA  = "fire_data";
	public static final String LEAVENOW = "leave_now";
	public static final String PICKED_UP = "picked_up";
	public static final String TIME = "time";

	// BDI Query Percept strings
    public static final String REQUEST_LOCATION = "request_location";
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

