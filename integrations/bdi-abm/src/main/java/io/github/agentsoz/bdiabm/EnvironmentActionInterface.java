package io.github.agentsoz.bdiabm;


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
 * Interface to provide BDI-actions to the environment.
 */
public interface EnvironmentActionInterface {
    /**
     *
     * @param agentId the agent's unique identifier
     * @param actionID the action's uunique identifier
     * @param parameters action arguments
     * @param actionState action state (optional), is set to INITIALISED if not given
     */
    public void packageAction(String agentId, String actionID, Object[] parameters, String actionState);


}
