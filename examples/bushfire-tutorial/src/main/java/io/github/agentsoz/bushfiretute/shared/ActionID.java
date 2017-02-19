package io.github.agentsoz.bushfiretute.shared;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import io.github.agentsoz.bushfiretute.Config;


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
 * @author Alex Lutman
 *
 */
 public class ActionID {
    //An action that taxis may give to request their MATSim counterpart to drive to a given coordinate
    public static final String DRIVETO = "drive to";
    //An action that taxis may give to ask for a given goal

    public static final String CONNECT_TO = "connect to";

    public static final String PICKUP = "pick up";
    public static final String driveToAndPickUp = "drive to and pick up";
    //An action that recieves a destination and a pick up

    public static final String setDriveTime = "set drive time";
    //An action that changes the currently executing activity

    public static final String REQUESTLOCATION = "Request Location";

    public static final String REQUESTJOB = "request job";
    //An action that operators may give to ask to be notified with a list of any new job requests
    public static final String AWAITJOBS = "await jobs";


}
