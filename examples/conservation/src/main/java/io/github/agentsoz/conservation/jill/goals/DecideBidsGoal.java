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

import io.github.agentsoz.conservation.Package;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.GoalInfo;

/**
 * This event is triggered by ReactOnCallForBids plan. This is handled by four
 * plans named DecideBidsWhenHighCHighP.plan DecideBidsWhenHighCLowP.plan,
 * DecideBidsWhenLowCHighP.plan and DecideBidsWhenLowCLowP.plan
 * 
 * @author Sewwandi Perera
 */
@GoalInfo(hasPlans = {
		"io.github.agentsoz.conservation.jill.plans.DecideBidsWhenLowCLowPPlan",
		"io.github.agentsoz.conservation.jill.plans.DecideBidsWhenLowCHighPPlan",
		"io.github.agentsoz.conservation.jill.plans.DecideBidsWhenHighCLowPPlan",
		"io.github.agentsoz.conservation.jill.plans.DecideBidsWhenHighCHighPPlan" })
public class DecideBidsGoal extends Goal {
	/**
	 * All packages used in the auctio cycle
	 */
	private Package[] packages;

	public DecideBidsGoal(String str) {
		super(str);
		setPackages(null);
	}

	public Package[] getPackages() {
		return packages;
	}

	public void setPackages(Package[] packages) {
		this.packages = packages;
	}

}
