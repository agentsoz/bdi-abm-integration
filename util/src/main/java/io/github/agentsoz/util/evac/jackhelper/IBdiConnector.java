package io.github.agentsoz.util.evac.jackhelper;

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

/**
 * Interface providing functions used by JACK code. This should not strictly be
 * required, but is needed to get JACK code to compile (their compiler does not handle
 * external dependencies very well so this is somewhat of a hack). dsingh, 6/dec/2017
 */
public interface IBdiConnector {

    public int getMaxDistanceToRelatives();
    
    public int getPickupTime();
    
    public double getHighPanicThreshold();
    
    public double getMedPanicThreshold();
    
    public double getLowPanicThreshold();
    
    public int getDiffTurn();
}
