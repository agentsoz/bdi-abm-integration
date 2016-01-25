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

import io.github.agentsoz.conservation.LandholderHistory.AuctionRound;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.GoalInfo;

/**
 * This event is triggered by ReactOnAuctionResult plan and this is handled by
 * below plans.
 * 
 * 1. UnsuccessfulLowC.plan
 * 
 * 2. UnsuccessfulHighC.plan
 * 
 * 3. SuccessfulLowC.plan
 * 
 * 4. SuccessfulHighC.plan
 * 
 * 5. NotParticipatedLowC.plan
 * 
 * 6. NotParticipatedHighC.plan
 * 
 * @author Sewwandi Perera
 */
@GoalInfo(hasPlans = { "io.github.agentsoz.conservation.jill.plans.UnsuccessfulLowC",
		"io.github.agentsoz.conservation.jill.plans.UnsuccessfulHighC",
		"io.github.agentsoz.conservation.jill.plans.SuccessfulLowC",
		"io.github.agentsoz.conservation.jill.plans.SuccessfulHighC",
		"io.github.agentsoz.conservation.jill.plans.NotParticipatedLowC",
		"io.github.agentsoz.conservation.jill.plans.NotParticipatedHighC" })
public class UpdateConservationEthicGoal extends Goal {
	/**
	 * Results about current auction round
	 */
	private AuctionRound myResults;

	public UpdateConservationEthicGoal(String str) {
		super(str);
	}

	public UpdateConservationEthicGoal(String str, AuctionRound results) {
		super(str);
		this.setMyResults(results);
	}

	public AuctionRound getMyResults() {
		return myResults;
	}

	public void setMyResults(AuctionRound myResults) {
		this.myResults = myResults;
	}

}
