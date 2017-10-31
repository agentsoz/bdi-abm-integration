package io.github.agentsoz.bushfiretute.shared;

import io.github.agentsoz.bdimatsim.MATSimActionList;

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
/**
 *
 * @author Dhirenda Singh
 *
 */
 public class ActionID {
	 public static final String DRIVETO = MATSimActionList.DRIVETO;
	 // means drive to some location
	 
	 /**
	 * This is the one described by {@link CustomReplanner#driveDirectlyToActivity}.
	 */
	public static final String CONNECT_TO = "connect to";
	 
	 public static final String PICKUP = "pick up";
	 //  wait specified time.  Does (currently) NOT wait until someone shows up.  There is usually a wait action after the 
	 // pickup action where the agent waits for the BDI system.
	 
	 public static final String DRIVETO_AND_PICKUP = "drive to and pick up";
	 // combining the two
	 
	 public static final String SET_DRIVE_TIME = "set drive time";
}
