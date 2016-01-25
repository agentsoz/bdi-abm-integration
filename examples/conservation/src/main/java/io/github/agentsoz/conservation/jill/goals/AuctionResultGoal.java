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
 * This is triggered from Landholder agent after receiving AUCTION_RESULT
 * percept.
 * 
 * @author Sewwandi Perera
 */
@GoalInfo(hasPlans = { "io.github.agentsoz.conservation.jill.plans.AuctionResultPlan" })
public class AuctionResultGoal extends Goal {
	/**
	 * Results about current auction round
	 */
	private AuctionRound auctionRound;

	/**
	 * Average of all land holders' conservation ethic barometer
	 */
	private double averageConservationEthic;

	public AuctionResultGoal(String str) {
		super(str);
	}

	public AuctionResultGoal(String str, AuctionRound round, double avgCE) {
		super(str);
		this.auctionRound = round;
		this.averageConservationEthic = avgCE;
	}

	public AuctionRound getAuctionRound() {
		return auctionRound;
	}

	public void setAuctionRound(AuctionRound auctionRound) {
		this.auctionRound = auctionRound;
	}

	public double getAverageConservationEthic() {
		return averageConservationEthic;
	}

	public void setAverageConservationEthic(double averageConservationEthic) {
		this.averageConservationEthic = averageConservationEthic;
	}

}
