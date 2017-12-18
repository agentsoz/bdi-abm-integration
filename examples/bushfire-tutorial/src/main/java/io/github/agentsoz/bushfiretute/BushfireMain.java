package io.github.agentsoz.bushfiretute;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.agentsoz.bdimatsim.EventsMonitorRegistry;
import io.github.agentsoz.bdimatsim.Utils;
import io.github.agentsoz.bushfiretute.matsim.*;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.nonmatsim.ActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.util.Util;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bushfiretute.bdi.BDIModel;
import io.github.agentsoz.util.Global;
import io.github.agentsoz.bushfiretute.jack.agents.EvacResident;

public class BushfireMain {
	// Defaults
	private static String logFile ;
	private static String outFile ;
	private static Level logLevel ;
	private static Logger logger ;
	public static PrintStream writer;


	private static Long seed = null;
	private static String matsimOutputDirectory;

	public static void main(final String[] args) {
		logFile = BushfireMain.class.getSimpleName() + ".log";
		outFile = null ;
		logLevel = Level.INFO ;
		logger = null ;
		writer = null ;
		
		// Parse the command line arguments
		parse(args);

		// Create the logger
		if ( logger==null ) {
			logger = createLogger("io.github.agentsoz.bushfiretute.BushfireMain", logFile);
		}
		logger.setLevel(Level.INFO);

		logger.error("error");
		logger.warn("warn");
		logger.info("info");
		logger.debug("debug");
		logger.trace("trace");

		// Redirect the agent program output if specified
		if (outFile != null) {
			try {
				writer = new PrintStream(outFile, "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			writer = System.out;
		}

		// add seed to command line args if run replication needed
		if (seed != null) {
			Global.getRandom().setSeed(seed);
			logger.info( "random seed was set to " + seed );
		}

		// Read in the configuration
		if (!Config.readConfig()) {
			logger.error("Failed to load configuration from '" + Config.getConfigFile() + "'. Aborting");
			throw new RuntimeException("failed to load configuration") ;
		}

		// Create the data and time server
		DataServer dataServer = DataServer.getServer("BushfireTutorial");
		dataServer.setTime(0.0);

		// Create the BDI model
		BDIModel bdiModel = new BDIModel(dataServer);
		
		// Create the MATSim side
		List<String> config = new ArrayList<>() ;
		config.add( Config.getMatSimFile() ) ;
		if ( matsimOutputDirectory != null ) { 
			config.add( MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR ) ;
			config.add( matsimOutputDirectory ) ;
		}
		logger.info( config.toString() );
		MATSimModel matsimModel = new MATSimModel(config.toArray( new String[config.size()]));

		// Get the list of BDI agent IDs
		Scenario scenario = matsimModel.loadAndPrepareScenario();
		List<String> bdiAgentIDs = Utils.getBDIAgentIDs( scenario );

		// Initialise both models
		bdiModel.init(matsimModel.getAgentManager().getAgentDataContainer(),
				null, matsimModel,
				bdiAgentIDs.toArray( new String[bdiAgentIDs.size()] ));
		matsimModel.init( bdiAgentIDs);

		// Set up evacuation specifics
		BushfireMain.determineSafeCoordinatesFromMATSimPlans(bdiAgentIDs, bdiModel, matsimModel.getScenario() );
		BushfireMain.assignDependentPersons(bdiAgentIDs, bdiModel);
		BushfireMain.registerActionsWithAgents(bdiModel, matsimModel);
		BushfireMain.registerPerceptsWithAgents(bdiModel, matsimModel);

		// Now run until the simulation ends
		while ( true ) {
			bdiModel.takeControl( matsimModel.getAgentManager().getAgentDataContainer() );
			if( matsimModel.isFinished() ) {
				break ;
			}
			matsimModel.runUntil((long)dataServer.getTime(), matsimModel.getAgentManager().getAgentDataContainer());

			// increment time
			dataServer.stepTime();
		}

		// Finish up
		matsimModel.finish() ;
		bdiModel.finish();
	}

	/**
	 * command line arguments
	 */
	public static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-c":
				if (i + 1 < args.length) {
					i++;
					Config.setConfigFile(args[i]);
				}
				break;
			case "-h":
				exit(null);
				break;
			case "-logfile":
				if (i + 1 < args.length) {
					i++;
					logFile = args[i];
				}
				break;
			case "-loglevel":
				if (i + 1 < args.length) {
					i++;
					try {
						logLevel = Level.toLevel(args[i]);
					} catch (Exception e) {
						System.err.println("Could not parse log level '" + args[i] + "' : " + e.getMessage());
					}
				}
				break;
			case "-outfile":
				if (i + 1 < args.length) {
					i++;
					outFile = args[i];
				}
				break;
			case "-seed":
				if (i + 1 < args.length) {
					i++;
					seed = Long.parseLong( args[i] );
				}
				break;
			case "--matsim-output-directory":
				if (i + 1 < args.length) {
					i++;
					matsimOutputDirectory = args[i] ;
				}
				break;
			default:
				throw new RuntimeException("unknown config option") ;
			}
		}
	}

	public static String usage() {
		return "usage:\n" 
				+ "  -c FILE            simulation configuration file" + "\n"
				+ "  -h                 print this help message and exit\n"
				+ "  -logfile FILE      logging output file name (default is '" + logFile + "')\n"
				+ "  -loglevel LEVEL    log level; one of ERROR,WARN,INFO,DEBUG,TRACE (default is '" + logLevel + "')\n"
				+ "  -outfile FILE      program output file name (default is system out)\n"
				+ "\n";
	}

	private static void exit(String err) {
		if (err != null) {
			System.err.println("\nERROR: " + err + "\n");
		}
		System.out.println(usage());
		System.exit(0);
	}

	// logger method
	private static Logger createLogger(String string, String file) {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder ple = new PatternLayoutEncoder();

		ple.setPattern("%date %level [%thread] %caller{1}%msg%n%n");
		ple.setContext(lc);
		ple.start();
		FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
		fileAppender.setFile(file);
		fileAppender.setEncoder(ple);
		fileAppender.setAppend(false);
		fileAppender.setContext(lc);
		fileAppender.start();
		Logger logger = (Logger) LoggerFactory.getLogger(string);
		logger.detachAndStopAllAppenders(); // detach console (doesn't seem to
		// work)
		logger.addAppender(fileAppender); // attach file appender
		logger.info("setting loglevel to " + logLevel ) ;
		logger.setLevel(logLevel);
		logger.setAdditive(true); /* set to true if root should log too */

		return logger;
	}

	public static void determineSafeCoordinatesFromMATSimPlans(List<String> bdiAgentsIDs, BDIModel bdiModel, Scenario scenario) {
//		Map<Id<Link>,? extends Link> links = matsimModel.getScenario().getNetwork().getLinks();
		for (String agentId : bdiAgentsIDs) {
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId);
			if (bdiAgent == null) {
				logger.warn("No BDI counterpart for MATSim agent '" + agentId
						+ "'. Should not happen, but will keep going");
				continue;
			}
			//			Plan plan = WithinDayAgentUtils.getModifiablePlan(matsimModel.getMobsimAgentMap().get(agentId));

			Plan plan = scenario.getPopulation().getPersons().get( Id.createPersonId(agentId) ).getSelectedPlan() ;
			List<PlanElement> planElements = plan.getPlanElements();
			Activity startAct = (Activity) planElements.get(0) ;

			// Assign start location
			//			double lat = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getX();
			//			double lon = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getY();
			Coord startCoord = PopulationUtils.computeCoordFromActivity( startAct, scenario.getActivityFacilities(), scenario.getConfig() ) ;
			double lat = startCoord.getX() ;
			double lon = startCoord.getY();

			bdiAgent.startLocation = new double[] { lat, lon };
			// yy this is just used for finding school locations, so startLocation is a slight mis-nomer; it needs the home location. kai, nov'17

			bdiAgent.currentLocation = "home"; // agents always start at home
			bdiAgent.log("is at home at location "+lon+","+lat);

			for (int i = 0; i < planElements.size(); i++) {
				PlanElement element = planElements.get(i);
				if (!(element instanceof Activity)) {
					continue;
				}
				Activity act = (Activity) element;
				// Get departure time
				if (act.getType().equals("Evacuation")) {
					PlanElement pe = plan.getPlanElements().get(i + 1);
					if (!(pe instanceof Leg)) {
						logger.error("Utils : selected plan element to get deptime is not a leg");
						continue;
					}
					Leg depLeg = (Leg) pe;
					double depTime = depLeg.getDepartureTime();
					logger.trace("departure time of the depLeg : {}", depTime);
					bdiAgent.setDepTime(depTime);

				}
				// Assign coords of safe destination
				if (act.getType().equals("Safe")) {
					double safeX = act.getCoord().getX();
					double safeY = act.getCoord().getY();
					bdiAgent.endLocation = new double[] { safeX, safeY };
					bdiAgent.log("safe location is at "+safeX+","+safeY);
				}
			}
		}
	}

	public static void registerPerceptsWithAgents(final BDIModel bdiModel, final MATSimModel matsimModel) {
		// this is more complex than registerActions because for the percepts we need the linkIDs beforehand. kai, nov'17

		for (String agentID : matsimModel.getAgentManager().getBdiAgentIds() ) {
			PAAgent agent1 = matsimModel.getAgentManager().getAgent( agentID );
			EvacResident bdiAgent1 = bdiModel.getBDICounterpart(agentID.toString());
			Gbl.assertNotNull(bdiAgent1);
			final Coord endCoord = new Coord(bdiAgent1.endLocation[0], bdiAgent1.endLocation[1]);
			Id<Link> newLinkId = NetworkUtils.getNearestLinkExactly(matsimModel.getScenario().getNetwork(),
					endCoord).getId();

			agent1.getPerceptHandler().registerBDIPerceptHandler(agent1.getAgentID(),
					EventsMonitorRegistry.MonitoredEventType.ArrivedAtDestination, newLinkId.toString(), new BDIPerceptHandler() {
				@Override
				public boolean handle(Id<Person> agentId, Id<Link> linkId, EventsMonitorRegistry.MonitoredEventType monitoredEvent) {
					PAAgent agent = matsimModel.getAgentManager().getAgent( agentId.toString() );
					EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
					Object[] params = { "Safe" , Double.toString(bdiAgent.getCurrentTime())};
					agent.getPerceptContainer().put(PerceptList.ARRIVED, params);
					return true; // unregister this handler
				}
			});
		}
	}

	public static void registerActionsWithAgents(final BDIModel bdiModel, final MATSimModel matsimModel) {
		for(String agentId1: matsimModel.getAgentManager().getBdiAgentIds() ) {

			ActionHandler withHandler = matsimModel.getAgentManager().getAgent( agentId1 ).getActionHandler();

			// overwrite default DRIVETO
			withHandler.registerBDIAction(ActionList.DRIVETO, new DRIVETOActionHandler(bdiModel, matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionList.CONNECT_TO, new CONNECT_TOActionHandler(bdiModel, matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionList.DRIVETO_AND_PICKUP, new DRIVETO_AND_PICKUPActionHandler(bdiModel, matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionList.SET_DRIVE_TIME, new SET_DRIVE_TIMEActionHandler(bdiModel, matsimModel));
		}
	}

	/**
	 * Randomly assign dependent persons to be picked up. Uses
	 * Pk ({@link Config#getProportionWithKids()}) and
	 * Pr ({@link
	 * @param bdiModel TODO
	 * @param bdiAgent
	 */
	public static void assignDependentPersons(List<String> bdiAgentsIDs, BDIModel bdiModel) {
		for (String agentId : bdiAgentsIDs) {
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId);
			int totPickups = 0;
			if( totPickups <= Config.getMaxPickUps() ) {
				double[] pDependents = {Config.getProportionWithKids(), Config.getProportionWithRelatives()};
				pDependents = Util.normalise(pDependents);
				Random random = Global.getRandom();

				if (random.nextDouble() < pDependents[0]) {
					// Allocate dependent children
					double[] sclCords = Config.getRandomSchoolCoords(bdiAgent.getId(),bdiAgent.startLocation);
					if(sclCords != null) {
						bdiAgent.kidsNeedPickUp = true;
						bdiAgent.schoolLocation = sclCords;
						bdiAgent.prepared_to_evac_flag = false;
						bdiAgent.log("has children at school coords "
								+ sclCords[0] + "," +sclCords[1]);
					}
					else{
						bdiAgent.log("has children but there are no schools nearby");
					}
				}
				if (random.nextDouble() < pDependents[1]) {
					// Allocate dependent adults
					bdiAgent.relsNeedPickUp = true;
					bdiAgent.prepared_to_evac_flag = false;
					bdiAgent.log("has relatives");
				}
				if (!bdiAgent.relsNeedPickUp && !bdiAgent.kidsNeedPickUp) {
					bdiAgent.log("has neither children nor relatives");
				}
			}
		}
	}
}
