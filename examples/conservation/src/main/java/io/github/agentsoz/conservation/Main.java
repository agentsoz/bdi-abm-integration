package io.github.agentsoz.conservation;

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

import static io.github.agentsoz.conservation.ConservationUtils.*;
import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.PerceptContainer;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.outputwriters.AgentsProgressWriter;
import io.github.agentsoz.conservation.outputwriters.AgentsStatisticsWriter;
import io.github.agentsoz.conservation.outputwriters.AuctionStatisticsWriter;
import io.github.agentsoz.conservation.outputwriters.AuctionSummaryWriter;
import io.github.agentsoz.conservation.outputwriters.BidsWriter;
import io.github.agentsoz.conservation.outputwriters.ConstantFileNames;
import io.github.agentsoz.conservation.outputwriters.LowCHighPStatistics;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Level;

/**
 * Main class of the conservation application
 * 
 * @author Sewwandi Perera
 */
public class Main {

	// Defaults
	/**
	 * How many time the same serias of auction cycles should be ran. Here, one
	 * test can have many repeats. One repeat can have many auction cycles.
	 */
	private static int repeats = 1;

	/**
	 * How many auction cycles should be ran for one repeat.
	 */
	private static int cycles = 1;

	/**
	 * Number of agents (land holders) in the population. This parameter can be
	 * set using the command-line argument "-a"
	 */
	private static int numLandholders = 400;

	/**
	 * GAMS model
	 */
	private static String gamsModel = null;

	/**
	 * The GAMS installation directory. It is compulsory to pass this parameter
	 * using command-line arguments when running the application.
	 * 
	 * command-line argument : "-gams_dir"
	 */
	private static String gamsDir = null;

	/**
	 * Number of packages used in the application
	 */
	private static int numPackages = 26;

	/**
	 * Log level
	 */
	private static Level logLevel = Level.INFO;

	/**
	 * This value is set based on the number of land holders and the
	 * targetPercentage
	 */
	private static String target = "";

	// State variables
	/**
	 * Path to the input file to GAMS model. This file contain the information
	 * about all bids made by land holders.
	 */
	private static String csvInputFile = null;

	/**
	 * Path to the output file from GAMS. GAMS output file contains the
	 * information about all bids made by agents during the auction cycle and
	 * which of them were successful.
	 */
	private static String csvOutputFile = null;

	/**
	 * Contains percept and action data about all agents
	 */
	private static AgentDataContainer adc;

	/**
	 * Agents state list
	 */
	private static AgentStateList asl;

	/**
	 * Instance of {@link AuctioneerModel}
	 */
	private static AuctioneerModel auctioneerModel;

	/**
	 * Instance of {@link LandholderModel}
	 */
	private static LandholderModel landholderModel;

	/**
	 * The list of pakcages used in the application.
	 */
	public static Package[] packages;

	/**
	 * Current cycle number
	 */
	public static int cycle;

	/**
	 * Current repeat number
	 */
	public static int repeat;

	/**
	 * All {@link Landholder}s in the application
	 */
	public static List<Landholder> landholders;

	/**
	 * The main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// parse command line arguments
		parse(args);
		ConservationUtils.init(numPackages);

		// Initialise variables
		auctioneerModel = new AuctioneerModel(gamsDir, gamsModel);
		landholderModel = new LandholderModel();
		asl = new AgentStateList();
		adc = new AgentDataContainer();

		long t0, t1;
		t0 = System.currentTimeMillis();

		if (repeats < 0)
			repeats = 1;
		while (repeat < repeats) {
			repeat++;
			// init all output writers for the repeat
			initOutputWriters(repeat);

			// initialise the agents
			initialiseSimulation(args);

			// save a reference to all agents
			landholders = new ArrayList<Landholder>();
			for (int i = 1; i <= numLandholders; i++) {
				landholders.add((Landholder) landholderModel
						.getAgentByName(Integer.toString(i)));
			}

			// set target
			setTarget();

			// send initial attributes of agents to the AgentsStatisticsWriter
			publishAgentsStats(0);
			AuctionStatisticsWriter.getInstance().open(repeat, landholders);

			for (cycle = 1; cycle <= cycles; cycle++) {
				Log.info("Started the auction cycle " + cycle
						+ " in the repeat " + repeat);

				// Set cycle number in output writers
				BidsWriter.getInstance().setCycleNumber(cycle);
				LowCHighPStatistics.getInstance().setCycleNumber(cycle);

				// Create new log files for this repeat and this cycle
				createGamsFiles(repeat, cycle);

				// set CSV file
				Log.setCSV(csvInputFile);

				// Conduct the auction
				auctioneerModel.conductAuction();

				// Send outputs
				publishAuctionSummary(repeat, cycle);
				publishAgentsStats(cycle);
				publishAuctionStats(cycle);

				Log.info("Completed the auction cycle " + cycle
						+ " in the repeat " + repeat);
			}
			// Close the output file
			Log.close();
		}
		t1 = System.currentTimeMillis();

		try {
			// Flush all output files
			AgentsStatisticsWriter.getInstance().flush();
			AgentsProgressWriter.getInstance().flush();

			// Close of file writers
			AuctionSummaryWriter.getInstance().close();
			BidsWriter.getInstance().close();
			LowCHighPStatistics.getInstance().close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		// report how long the test took
		Log.info("Finished running " + repeats + " repeats of " + cycles
				+ " auctions (" + (t1 - t0) + " ms)");

		System.exit(0);

	}

	/**
	 * Initialises output writers
	 * 
	 * @param repeat
	 */
	private static void initOutputWriters(int repeat) {
		// Create a new log file for this repeat
		Log.setLog(ConstantFileNames.getOutputLogFileName(repeat), logLevel);
		Log.open();

		// Output writers
		AgentsStatisticsWriter.getInstance().open(repeat, numLandholders);
		AgentsProgressWriter.getInstance().open(repeat, cycles);
		BidsWriter.getInstance().init(repeat);
		LowCHighPStatistics.getInstance().open(repeat);
	}

	/**
	 * Publishes statistics about the auction cycle to the
	 * {@link AuctionStatisticsWriter}
	 * 
	 * @param cycle
	 */
	private static void publishAuctionStats(int cycle) {
		AuctionStatisticsWriter.getInstance().addAuctionStatistics(cycle,
				auctioneerModel.getLatestAuctionResultSet().getResultSet(),
				landholders, target, packages, cycle == 0);
	}

	/**
	 * Publishes a summary of the auction cycle to the
	 * {@link AuctionSummaryWriter}
	 * 
	 * @param repeat
	 * @param cycle
	 */
	private static void publishAuctionSummary(int repeat, int cycle) {
		// write auction result summary to log file
		try {
			AuctionSummaryWriter.getInstance().writeSummary(repeat, cycle,
					auctioneerModel.getLatestAuctionResultSet().getResultSet());
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Publishes statistics about agents to the {@link AgentsStatisticsWriter}
	 * 
	 * @param cycle
	 */
	private static void publishAgentsStats(int cycle) {
		for (Landholder landholder : landholders) {
			// publish statistics
			AgentsStatisticsWriter.getInstance().addAllInfo(cycle,
					landholder.getName(),
					landholder.getConservationEthicBarometer(),
					landholder.getProfitMotiveBarometer(),
					landholder.getCurrentAuctionRound());
		}
	}

	/**
	 * Generates file paths for GAMS input and output files
	 * 
	 * @param repeat
	 * @param cycle
	 */
	private static void createGamsFiles(int repeat, int cycle) {
		csvInputFile = "conservation.in." + String.format("%03d", repeat)
				+ "r." + String.format("%03d", cycle) + "c.csv";
		csvOutputFile = "conservation.out." + String.format("%03d", repeat)
				+ "r." + String.format("%03d", cycle) + "c.csv";
	}

	/**
	 * Sets the target of the auction based on the number of land holders and
	 * the targetPercentage
	 */
	public static void setTarget() {
		int maxMalleefowls = 0;
		int maxPhascogales = 0;
		int maxPythons = 0;

		for (Package p : packages) {
			String[] species = p.description.split(",");
			int malleefowls = Integer.parseInt(species[0]);
			int phascogales = Integer.parseInt(species[1]);
			int pythons = Integer.parseInt(species[2]);

			if (maxMalleefowls < malleefowls) {
				maxMalleefowls = malleefowls;
			}
			if (maxPhascogales < phascogales) {
				maxPhascogales = phascogales;
			}
			if (maxPythons < pythons) {
				maxPythons = pythons;
			}
		}

		Log.debug("Target percentage : " + getTargetPercentage());

		maxMalleefowls = (int) (maxMalleefowls * numLandholders
				* getTargetPercentage() / 100);
		maxPhascogales = (int) (maxPhascogales * numLandholders
				* getTargetPercentage() / 100);
		maxPythons = (int) (maxPythons * numLandholders * getTargetPercentage() / 100);

		// Print target file
		FileWriter targetFileWriter;
		try {
			targetFileWriter = new FileWriter(
					ConstantFileNames.getTargetFileName());

			// append header
			targetFileWriter.append("dummy,mallNum,phasNum,pythNum\n1,");

			// append data
			targetFileWriter.append(Integer.toString(maxMalleefowls));
			targetFileWriter.append(",");
			targetFileWriter.append(Integer.toString(maxPhascogales));
			targetFileWriter.append(",");
			targetFileWriter.append(Integer.toString(maxPythons));
			targetFileWriter.append("\n");
			targetFileWriter.flush();
			targetFileWriter.close();
		} catch (IOException e) {
			Log.error(e.getMessage());
		}

		target = maxMalleefowls + " " + maxPhascogales + " " + maxPythons;

		Log.info("Target [mallefowls:" + maxMalleefowls + ", phascogales:"
				+ maxPhascogales + ", pythons:" + maxPythons + "]");
	}

	/**
	 * Initialises the simulation for a new repeat
	 * 
	 * @param args
	 */
	public static void initialiseSimulation(String[] args) {
		// generate the bid packages
		packages = Package.getPackages(numPackages);

		// initalise the agent data container
		adc = new AgentDataContainer();

		// initalise the agent state container
		asl = new AgentStateList();

		// create the agents, and their data/state containers
		String[] agentIds = new String[numLandholders];
		for (int i = 0; i < numLandholders; i++) {
			agentIds[i] = String.format("%d", i);
			asl.add(new AgentState(agentIds[i]));
			adc.getOrCreate(agentIds[i]);
		}

		// connect the two systems and initialise
		auctioneerModel.connect(landholderModel, adc, asl);
		landholderModel.init(adc, asl, auctioneerModel, args);

		// start the bdi system
		landholderModel.start();
	}

	/**
	 * Called by MatsimModel to signal news actions from BDI side Handles two
	 * types of changes, new actions (INITIATED) and dropped actions
	 */
	public static void updateActions(AgentDataContainer agentDataContainer) {
		/*
		 * Remove the former percepts as they are no longer needed
		 */
		for (String agentName : agentDataContainer.keySet()) {
			PerceptContainer newPerceptContainer = new PerceptContainer();
			agentDataContainer.getOrCreate(agentName).setPerceptContainer(
					newPerceptContainer);
		}

		for (String agent : agentDataContainer.keySet()) {
			ActionContainer actionContainer = agentDataContainer.getOrCreate(
					agent).getActionContainer();
			for (String action : actionContainer.actionIDSet()) {
				if (actionContainer.get(action).getState() == ActionContent.State.INITIATED) {
					// TODO: placeholder for handling this state if required
				}
				if (actionContainer.get(action).getState() == ActionContent.State.DROPPED) {
					// TODO: placeholder for handling this state if required
				}
			}
		}
	}

	/**
	 * Note for future additions: keep alphabetical order, and wrap long lines
	 */
	public static String usage() {
		return "usage: "
				+ Main.class.getName()
				+ "  [options]\n"
				+ "  -a <agents>                           number of landholder agents (default is "
				+ numLandholders
				+ ")\n"
				+ "  -bidAddon 							   the gap between defaultBids and increased bid number\n"
				+ " 									   (default is "
				+ getBidAddon()
				+ ")\n"
				+ "  -c <cycles>                           number of unique auctions to perform (default is "
				+ cycles
				+ ")\n"
				+ "  -conservationEthicModifier 		   The factor used to increase and decrease conservation ethic barometer \n"
				+ "										   of agents (default is "
				+ getConservationEthicModifier()
				+ ")\n"
				+ "  -conservationEthicSeed                seed value used when  generating conservation ethic barometer of agents\n"
				+ "										   (default is "
				+ getConservationEthicSeed()
				+ ")\n"
				+ "  -defaultMaxNumberOfBids			   number of bids by default a normal agent make \n"
				+ " 									   (default is "
				+ getDefaultMaxNumberOfBids()
				+ ")\n"
				+ "  -gams_dir <dir>                       path to directory where GAMS is installed\n"
				+ "  -gams_model <file>                    path to GAMS model to execute\n"
				+ "  -globalRandomSeed					   seed of the global random variable used throughout the application\n"
				+ " 									   (default is "
				+ getGlobalRandomSeed()
				+ ")\n"
				+ "  -h                                    print this help message and exit\n"
				+ "  -highCEAgentsPercentage			   percentage of agents having high conservation ethic \n"
				+ " 									   (default is "
				+ getHighCEAgentsPercentage()
				+ ")\n"
				+ "  -high_participation_prob              high participation probability (default is "
				+ getHighParticipationProbability()
				+ ")\n"
				+ "  -highProfitRangeMinMargin 			   minimum margin of high profit percentage range (default is "
				+ getHighProfitRangeMinMargin()
				+ "\n"
				+ "  -low_participation_prob               low participation probability (default is "
				+ getLowParticipationProbability()
				+ ")\n"
				+ "  -log_level <level>                    one of ERROR,WARN,INFO,DEBUG,TRACE (default is "
				+ logLevel
				+ ")\n"
				+ "  -lower_threshold_c                    lower threshold for conservation ethic barometer to be low\n"
				+ "										    (default is "
				+ getLowerThresholdC()
				+ ")\n"
				+ "  -lower_threshold_p                    lower threshold for profit motive barometer to be low (default is \n"
				+ "										   "
				+ getLowerThresholdP()
				+ ")\n"
				+ "  -max_c                                maximum value for conservation ethic barometer (default is "
				+ getMaxConservationEthic()
				+ ")\n"
				+ "  -max_p                                maximum value for profit motivation barometer (default is "
				+ getMaxProfitMotivation()
				+ ")\n"
				+ "  -medProfitRangeMinMargin			   minimum margin of medium profit range (default is "
				+ getMedProfitRangeMinMargin()
				+ "\n"
				+ "  -p <pacakges>                         number of conservation packages to use (default is "
				+ numPackages
				+ "')\n"
				+ "  -profitDifferenctial    			   the gap between low, medium and high profit percentage levels\n"
				+ "										   (default is "
				+ getProfitDifferenctial()
				+ ")\n"
				+ "  -profitMotivationModifier	 		   The factor used to increase and decrease profit motive barometer \n"
				+ "										   of agents (default is "
				+ getProfitMotivationModifier()
				+ ")\n"
				+ "  -profitMotivationSeed                 seed value used when  generating profit motive barometer of agents\n"
				+ "										   (default is "
				+ getProfitMotivationSeed()
				+ ")\n"
				+ "  -profitVariability 				   the variability in low, medium and high profit ranges\n"
				+ "										   (default is "
				+ getProfitVariability()
				+ ")\n"
				+ "  -r <repeats>                          number of times to run the trading strategy (default is "
				+ repeats
				+ ")\n"
				+ "  -socialNormUpdatePercentage           the percentage increased in agents' CE as a result of social norm\n"
				+ "                                        (default is "
				+ getSocialNormUpdatePercentage()
				+ ")\n"
				+ "  -targetPercentage					   The percentage of maximum possible target (if all agents bid on the highest package)\n"
				+ " 									   that should be assigned as the target (default is "
				+ getTargetPercentage()
				+ ")\n"
				+ "  -upper_threshold_c                    upper threshold for conservation ethic barometer to be high (default is "
				+ getUpperThresholdC()
				+ ")\n"
				+ "  -upper_threshold_p                    upper threshold for profit motive barometer to be high (default is "
				+ getUpperThresholdP() + ")\n";
	}

	/**
	 * Parse command-line argumets.
	 * 
	 * @param args
	 */
	public static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-a":
				if (i + 1 < args.length) {
					if (i + 1 < args.length) {
						i++;
						try {
							numLandholders = Integer.parseInt(args[i]);
						} catch (Exception e) {
							exit("Option value '" + args[i]
									+ "' is not a number");
						}
					}
					break;
				}
			case "-c":
				if (i + 1 < args.length) {
					if (i + 1 < args.length) {
						i++;
						try {
							cycles = Integer.parseInt(args[i]);
						} catch (Exception e) {
							exit("Option value '" + args[i]
									+ "' is not a number");
						}
					}
					break;
				}
			case "-gams_dir":
				if (i + 1 < args.length) {
					i++;
					gamsDir = args[i];
				}
				break;
			case "-gams_model":
				if (i + 1 < args.length) {
					i++;
					gamsModel = args[i];
				}
				break;
			case "-log_level":
				if (i + 1 < args.length) {
					i++;
					logLevel = Level.toLevel(args[i]); // ill-formed strings
														// default to DEBUG
				}
				break;
			case "-p":
				if (i + 1 < args.length) {
					i++;
					try {
						numPackages = Integer.parseInt(args[i]);
					} catch (Exception e) {
						exit("Could not parse numPackages value '" + args[i]
								+ "'. Will use the default of '" + repeats
								+ "'");
					}
				}
				break;
			case "-r":
				if (i + 1 < args.length) {
					i++;
					try {
						repeats = Integer.parseInt(args[i]);
					} catch (Exception e) {
						exit("Could not parse repeats value '" + args[i]
								+ "'. Will use the default of '" + repeats
								+ "'");
					}
				}
				break;
			case "-conservationEthicSeed":
				if (i + 1 < args.length) {
					i++;
					try {
						setConservationEthicSeed(Long.parseLong(args[i]));
					} catch (Exception e) {
						exit("Option value '" + args[i]
								+ "' is not in the format 'int:int'. Will "
								+ "use the default of '"
								+ getConservationEthicSeed() + "'");
					}
				}
				break;
			case "-profitMotivationSeed":
				if (i + 1 < args.length) {
					i++;
					try {
						setProfitMotivationSeed(Long.parseLong(args[i]));
					} catch (Exception e) {
						exit("Option value '" + args[i]
								+ "' is not in the format 'int:int'. Will "
								+ "use the default of '"
								+ getProfitMotivationSeed() + "'");
					}
				}
				break;
			case "-highCEAgentsPercentage":
				if (i + 1 < args.length) {
					i++;
					try {
						setHighCEAgentsPercentage(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getHighCEAgentsPercentage() + "'");
					}
				}
				break;
			case "-high_participation_prob":
				if (i + 1 < args.length) {
					i++;
					try {
						setHighParticipationProbability(Double
								.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getHighParticipationProbability() + "'");
					}
				}
				break;
			case "-low_participation_prob":
				if (i + 1 < args.length) {
					i++;
					try {
						setLowParticipationProbability(Double
								.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getLowParticipationProbability() + "'");
					}
				}
				break;
			case "-upper_threshold_c":
				if (i + 1 < args.length) {
					i++;
					try {
						setUpperThresholdP(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '" + getUpperThresholdC()
								+ "'");
					}
				}
				break;
			case "-lower_threshold_c":
				if (i + 1 < args.length) {
					i++;
					try {
						setLowerThresholdC(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '" + getLowerThresholdC()
								+ "'");
					}
				}
				break;
			case "-upper_threshold_p":
				if (i + 1 < args.length) {
					i++;
					try {
						setUpperThresholdP(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '" + getUpperThresholdP()
								+ "'");
					}
				}
				break;
			case "-lower_threshold_p":
				if (i + 1 < args.length) {
					i++;
					try {
						setLowerThresholdP(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '" + getLowerThresholdP()
								+ "'");
					}
				}
				break;
			case "-max_c":
				if (i + 1 < args.length) {
					i++;
					try {
						setMaxConservationEthic(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getMaxConservationEthic() + "'");
					}
				}
				break;
			case "-max_p":
				if (i + 1 < args.length) {
					i++;
					try {
						setMaxProfitMotivation(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getMaxProfitMotivation() + "'");
					}
				}
				break;
			case "-socialNormUpdatePercentage":
				if (i + 1 < args.length) {
					i++;
					try {
						setSocialNormUpdatePercentage(Double
								.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getSocialNormUpdatePercentage() + "'");
					}
				}
				break;
			case "-profitDifferenctial":
				if (i + 1 < args.length) {
					i++;
					try {
						setProfitDifferenctial(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getProfitDifferenctial() + "'");
					}
				}
				break;
			case "-defaultMaxNumberOfBids":
				if (i + 1 < args.length) {
					i++;
					try {
						setDefaultMaxNumberOfBids(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '" + args[i]
								+ "' is not in the format 'int:int'. Will "
								+ "use the default of '"
								+ getDefaultMaxNumberOfBids() + "'");
					}
				}
				break;
			case "-bidAddon":
				if (i + 1 < args.length) {
					i++;
					try {
						setBidAddon(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '" + getBidAddon() + "'");
					}
				}
				break;
			case "-profitVariability":
				if (i + 1 < args.length) {
					i++;
					try {
						setProfitVariability(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getProfitVariability() + "'");
					}
				}
				break;
			case "-conservationEthicModifier":
				if (i + 1 < args.length) {
					i++;
					try {
						setConservationEthicModifier(Double
								.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getConservationEthicModifier() + "'");
					}
				}
				break;
			case "-profitMotivationModifier":
				if (i + 1 < args.length) {
					i++;
					try {
						setProfitMotivationModifier(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getProfitMotivationModifier() + "'");
					}
				}
				break;
			case "-medProfitRangeMinMargin":
				if (i + 1 < args.length) {
					i++;
					try {
						setMedProfitRangeMinMargin(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getMedProfitRangeMinMargin() + "'");
					}
				}
				break;
			case "-highProfitRangeMinMargin":
				if (i + 1 < args.length) {
					i++;
					try {
						setHighProfitRangeMinMargin(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getHighProfitRangeMinMargin() + "'");
					}
				}
				break;
			case "-targetPercentage":
				if (i + 1 < args.length) {
					i++;
					try {
						setTargetPercentage(Double.parseDouble(args[i]));
					} catch (Exception e) {
						exit("Option value '"
								+ args[i]
								+ "' is not in the format 'double:double'. Will "
								+ "use the default of '"
								+ getTargetPercentage() + "'");
					}
				}
				break;
			case "-globalRandomSeed":
				if (i + 1 < args.length) {
					i++;
					try {
						setGlobalRandomSeed(Long.parseLong(args[i]));
					} catch (Exception e) {
						exit("Option value '" + args[i]
								+ "' is not in the format 'long'. Will "
								+ "use the default of '"
								+ getGlobalRandomSeed() + "'");
					}
				}
				break;
			case "-h":
				exit(null);
			}

			// print all configurable parameters to a output file
			printConfigParams();
		}

		// Abort if required args were not given
		if (gamsDir == null) {
			exit("Required option -gams_dir was not specified");
		} else if (gamsModel == null) {
			exit("Required option -gams_model was not specified");
		}
	}

	/**
	 * Prints all configurable parameters and their values to a csv file for
	 * future reference.
	 */
	private static void printConfigParams() {
		try {
			FileWriter writer = new FileWriter(
					ConstantFileNames.getConfigParametersFileName());
			// header line
			writer.append("a,bidAddon,c,conservationEthicModifier,defaultMaxNumberOfBids,"
					+ "high_participation_prob,highProfitRangeMinMargin,low_participation_prob,lower_threshold_c,"
					+ "lower_threshold_p,max_c,max_p,medProfitRangeMinMargin,p,profitDifferenctial,"
					+ "profitMotivationModifier,profitVariability,r,socialNormUpdatePercentage,"
					+ "upper_threshold_c,upper_threshold_p,targetPercentage,CE_seed,PM_seed,Global_seed,"
					+ "highCEAgentsPercentage\n");
			// table values
			writer.append(Integer.toString(numLandholders));
			writer.append(",");
			writer.append(Integer.toString(getBidAddon()));
			writer.append(",");
			writer.append(Integer.toString(cycles));
			writer.append(",");
			writer.append(Double.toString(getConservationEthicModifier()));
			writer.append(",");
			writer.append(Integer.toString(getDefaultMaxNumberOfBids()));
			writer.append(",");
			writer.append(Double.toString(getHighParticipationProbability()));
			writer.append(",");
			writer.append(Double.toString(getHighProfitRangeMinMargin()));
			writer.append(",");
			writer.append(Double.toString(getLowParticipationProbability()));
			writer.append(",");
			writer.append(Double.toString(getLowerThresholdC()));
			writer.append(",");
			writer.append(Double.toString(getLowerThresholdP()));
			writer.append(",");
			writer.append(Double.toString(getMaxConservationEthic()));
			writer.append(",");
			writer.append(Double.toString(getMaxProfitMotivation()));
			writer.append(",");
			writer.append(Double.toString(getMedProfitRangeMinMargin()));
			writer.append(",");
			writer.append(Integer.toString(numPackages));
			writer.append(",");
			writer.append(Double.toString(getProfitDifferenctial()));
			writer.append(",");
			writer.append(Double.toString(getProfitMotivationModifier()));
			writer.append(",");
			writer.append(Double.toString(getProfitVariability()));
			writer.append(",");
			writer.append(Integer.toString(repeats));
			writer.append(",");
			writer.append(Double.toString(getSocialNormUpdatePercentage()));
			writer.append(",");
			writer.append(Double.toString(getUpperThresholdC()));
			writer.append(",");
			writer.append(Double.toString(getUpperThresholdP()));
			writer.append(",");
			writer.append(Double.toString(getTargetPercentage()));
			writer.append(",");
			writer.append(Long.toString(getConservationEthicSeed()));
			writer.append(",");
			writer.append(Long.toString(getProfitMotivationSeed()));
			writer.append(",");
			writer.append(Long.toString(getGlobalRandomSeed()));
			writer.append(",");
			writer.append(Double.toString(getHighCEAgentsPercentage()));

			writer.flush();
			writer.close();
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Exit the simulation.
	 * 
	 * @param err
	 */
	private static void exit(String err) {
		if (err != null) {
			System.err.println("\nERROR: " + err + "\n");
		}
		System.out.println(usage());
		System.exit(0);
	}

	/**
	 * Returns the path to the input file to GAMS model.
	 * 
	 * @return
	 */
	public static String GAMSInputFile() {
		return csvInputFile;
	}

	/**
	 * Returns the path to the output file of the GAMS model.
	 * 
	 * @return
	 */
	public static String GAMSOutputFile() {
		return csvOutputFile;
	}

	/**
	 * Returns the number of packages used by the simulation.
	 * 
	 * @return
	 */
	public static int numPackages() {
		return numPackages;
	}

	/**
	 * Returns the number of land holders in the simulation.
	 * 
	 * @return
	 */
	public static int numLandholders() {
		return numLandholders;
	}
}
