package io.github.agentsoz.conservation.outputwriters;

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
 * Names of all output files are defined here.
 * 
 * @author Sewwandi Perera
 */
public class ConstantFileNames {

	private final static String CONFIG_PARAMETERS_FILE_NAME = "config_parameters.csv";

	private final static String TARGET_FILE_NAME = "target_table.csv";

	private final static String OUTPUT_LOG_FILE_NAME = "conservation_out_";

	private final static String AGENTS_PROGRESS_FILE_NAME = "agents_progress_";

	private final static String AGENTS_STAT_FILE_NAME = "agents_statistics_";

	private final static String AUCTIONS_STAT_FILE_NAME = "auction_statistics_";

	private final static String INPUT_PARAMS_FILE_NAME = "input_params_";

	private final static String BIDS_FILE_NAME = "bids_";

	private final static String LOWC_HIGHP_STAT_FILE = "lowc_highp_stats_";

	private final static String AGENTS_CE_FILE = "agents_ce_";

	private final static String AGENTS_PM_FILE = "agents_pm_";

	private final static String NUMBER_OF_BIDS_PER_AGENT = "number_of_bids_per_agent";

	private final static String NUMBER_OF_SUCCESSFUL_BIDS_PER_AGENT = "number_of_successful_bids_per_agent";

	private final static String TOTAL_OPPORTUNITY_COST_PER_AGENT = "total_opportunity_cost_per_agent";

	private final static String SUCCESSFUL_OPPORTUNITY_COST_PER_AGENT = "successful_opportunity_cost_per_agent";

	private final static String TOTAL_BID_PRICE_PER_AGENT = "total_bid_price_per_agent";

	private final static String SUCCESSFUL_BID_PRICE_PER_AGENT = "successful_bid_price_per_agent";

	public static String getLowCHighPStatFileName(int repeatNumber) {
		return LOWC_HIGHP_STAT_FILE + String.format("%03d", repeatNumber)
				+ ".csv";
	}

	public static String getConfigParametersFileName() {
		return CONFIG_PARAMETERS_FILE_NAME;
	}

	public static String getAgentsProgressFileName(int repeatNumber) {
		return AGENTS_PROGRESS_FILE_NAME + String.format("%03d", repeatNumber)
				+ ".csv";
	}

	public static String getAgentsStatsFileName(int repeatNumber) {
		return AGENTS_STAT_FILE_NAME + String.format("%03d", repeatNumber)
				+ ".csv";
	}

	public static String getAuctionStatsFileName(int repeatNumber) {
		return AUCTIONS_STAT_FILE_NAME + String.format("%03d", repeatNumber)
				+ ".csv";
	}

	public static String getInputParamsFileName(int repeatNumber) {
		return INPUT_PARAMS_FILE_NAME + String.format("%03d", repeatNumber)
				+ ".csv";
	}

	public static String getBidsFileName(int repeatNumber) {
		return BIDS_FILE_NAME + String.format("%03d", repeatNumber) + ".csv";
	}

	public static String getOutputLogFileName(int repeatNumber) {
		return OUTPUT_LOG_FILE_NAME + String.format("%03d", repeatNumber)
				+ ".log";
	}

	public static String getTargetFileName() {
		return TARGET_FILE_NAME;
	}

	public static String getAgentsCEFileName(int repeatNumber) {
		return AGENTS_CE_FILE + String.format("%03d", repeatNumber) + ".csv";
	}

	public static String getAgentsPmFile(int repeatNumber) {
		return AGENTS_PM_FILE + String.format("%03d", repeatNumber) + ".csv";
	}

	public static String getNumberOfBidsPerAgent(int repeatNumber) {
		return NUMBER_OF_BIDS_PER_AGENT + String.format("%03d", repeatNumber)
				+ ".csv";
	}

	public static String getNumberOfSuccessfulBidsPerAgent(int repeatNumber) {
		return NUMBER_OF_SUCCESSFUL_BIDS_PER_AGENT
				+ String.format("%03d", repeatNumber) + ".csv";
	}

	public static String getTotalOpportunityCostPerAgent(int repeatNumber) {
		return TOTAL_OPPORTUNITY_COST_PER_AGENT
				+ String.format("%03d", repeatNumber) + ".csv";
	}

	public static String getSuccessfulOpportunityCostPerAgent(int repeatNumber) {
		return SUCCESSFUL_OPPORTUNITY_COST_PER_AGENT
				+ String.format("%03d", repeatNumber) + ".csv";
	}

	public static String getTotalBidPricePerAgent(int repeatNumber) {
		return TOTAL_BID_PRICE_PER_AGENT + String.format("%03d", repeatNumber)
				+ ".csv";
	}

	public static String getSuccessfulBidPricePerAgent(int repeatNumber) {
		return SUCCESSFUL_BID_PRICE_PER_AGENT
				+ String.format("%03d", repeatNumber) + ".csv";
	}
}
