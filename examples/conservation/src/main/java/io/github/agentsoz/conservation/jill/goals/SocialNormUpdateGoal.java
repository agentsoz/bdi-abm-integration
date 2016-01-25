package io.github.agentsoz.conservation.jill.goals;

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

import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.GoalInfo;

/**
 * This event is triggered by ReactOnAuctionResult plan and this is handled by
 * ApplySocialNormEffect plan.
 * 
 * @author Sewwandi Perera
 */
@GoalInfo(hasPlans = { "io.github.agentsoz.conservation.jill.plans.SocialNormUpdatePlan" })
public class SocialNormUpdateGoal extends Goal {
	/**
	 * Average of all land holders' conservation ethic barometer
	 */
	private double averageConservationEthic;

	public SocialNormUpdateGoal(String str) {
		super(str);
	}

	public SocialNormUpdateGoal(String str, double avgCE) {
		super(str);
		this.averageConservationEthic = avgCE;
	}

	public double getAverageConservationEthic() {
		return averageConservationEthic;
	}

	public void setAverageConservationEthic(double averageConservationEthic) {
		this.averageConservationEthic = averageConservationEthic;
	}

}
