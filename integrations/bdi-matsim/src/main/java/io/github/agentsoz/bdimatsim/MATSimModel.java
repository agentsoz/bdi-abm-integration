package io.github.agentsoz.bdimatsim;

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.ModelInterface;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.nonmatsim.PAAgentManager;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.ActionList;
import io.github.agentsoz.util.PerceptList;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlayPauseSimulationControl;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.utils.EditPlans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
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
 * @author QingyuChen, KaiNagel, Dhi Singh
 */
public final class MATSimModel implements ABMServerInterface, ModelInterface, QueryPerceptInterface, DataClient {
	private static final Logger log = LoggerFactory.getLogger(MATSimModel.class);
	public static final String MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR = "--matsim-output-directory";

	public static final String eGlobalStartHhMm = "startHHMM";
	private static final String eConfigFile = "configXml";
	private static final String eOutputDir = "outputDir";
	private static final String eCongestionEvaluationInterval = "congestionEvaluationInterval";
	private static final String eCongestionToleranceThreshold = "congestionToleranceThreshold";
	private static final String eCongestionReactionProbability = "congestionReactionProbability";

	// Defaults
	private String optConfigFile = null;
	private String optOutputDir = null;
	private double optStartTimeInSeconds = 1.0;
	private double optCongestionEvaluationInterval = 180;
	private double optCongestionToleranceThreshold = 0.25;
	private double optCongestionReactionProbability = 0.10;


	private Config config = null;
	private boolean configLoaded = false;
    private Object sequenceLock;


	private Scenario scenario = null;
	private boolean scenarioLoaded = false ;
	private boolean modelInitialised = false ;


	/**
	 * A helper class essentially provided by the framework, used here.  The only direct connection to matsim are the event
	 * monitors, which need to be registered, via the events monitor registry, as a matsim events handler.
	 */
	private PAAgentManager agentManager = null;

	/**
	 * This is in fact a MATSim class that provides a view onto the QSim.
	 */
	@Inject private MobsimDataProvider mobsimDataProvider ;
	@Inject private Replanner replanner;
	// yy This is working because MATSimModel is bound somewhere.

	private QSim qSim;

	/**
	 * can be null (so non-final is ok)
	 */
	private List<EventHandler> eventHandlers;

	private PlayPauseSimulationControl playPause;
	private final EventsMonitorRegistry eventsMonitors  = new EventsMonitorRegistry() ;
	private Thread matsimThread;

	private DataServer dataServer;
	private final Map<String, DataClient> dataListeners = createDataListeners();
	private AgentDataContainer adc = new AgentDataContainer();

	public enum RoutingMode {carFreespeed, carGlobalInformation}

	private Controler controller;

	public MATSimModel(Map<String, String> opts, DataServer dataServer) {
		this( new String [] {
				opts.get( eConfigFile ) ,
							  MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR , opts.get( eOutputDir) ,
							  eGlobalStartHhMm , opts.get( eGlobalStartHhMm )
		} ) ;

		// yyyy this is so far NOT the same as what is was originally, see below, since the code below
		// could pass "null" which the new code cannot.  (However, the "null" was not really handled
		// correctly in the receiving code so it needs to be repaired ...).  kai, nov'18
		
//		this(opts.get(eConfigFile), opts.get(eOutputDir), opts.get(eGlobalStartHhMm));
		
		registerDataServer(dataServer);

		if (opts == null) {
			return;
		}
		for (String opt : opts.keySet()) {
			log.info("Found option: {}={}", opt, opts.get(opt));
			switch(opt) {
				case eGlobalStartHhMm:
					optStartTimeInSeconds = convertTimeToSeconds(opts.get(opt).replace(":", ""));
					break;
				case eConfigFile:
					optConfigFile = opts.get(opt);
					break;
				case eOutputDir:
					optOutputDir = opts.get(opt);
					break;
				case eCongestionEvaluationInterval:
					optCongestionEvaluationInterval= Double.parseDouble(opts.get(opt));
					break;
				case eCongestionToleranceThreshold:
					optCongestionToleranceThreshold= Double.parseDouble(opts.get(opt));
					break;
				case eCongestionReactionProbability:
					optCongestionReactionProbability= Double.parseDouble(opts.get(opt));
					break;
				default:
					log.warn("Ignoring option: " + opt + "=" + opts.get(opt));
			}
		}
	}

//	public MATSimModel(String matSimFile, String matsimOutputDirectory, String startHHMM) {
//		// not the most elegant way of doing this ...
//		// yy maybe just pass the whole string from above and take apart ourselves?
//		this(
//		matsimOutputDirectory==null ?
//				new String[]{matSimFile} :
//				new String[]{ matSimFile, MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, matsimOutputDirectory, eGlobalStartHhMm, startHHMM }
//		);
//	}

	public MATSimModel( String[] args) {
		// Log level should be set in logback.xml, see
		// https://github.com/agentsoz/ees/blob/a769eb9497c444beac7cf823bfae05764eb06356/src/main/resources/logback.xml#L39
		//((ch.qos.logback.classic.Logger)log).setLevel( Level.DEBUG);

		config = ConfigUtils.loadConfig( args[0] ) ;

		Utils.parseAdditionalArguments(args, config);

		config.network().setTimeVariantNetwork(true);

		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);

		config.qsim().setSimStarttimeInterpretation( StarttimeInterpretation.onlyUseStarttime );

		config.controler().setWritePlansInterval(1);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.planCalcScore().setWriteExperiencedPlans(true);

		// we have to declare those routingModes where we want to use the network router:
		{
			Collection<String> modes = config.plansCalcRoute().getNetworkModes();
			Set<String> newModes = new TreeSet<>( modes ) ;
			for ( RoutingMode mode : RoutingMode.values() ) {
				newModes.add( mode.name() ) ;
			}
			config.plansCalcRoute().setNetworkModes( newModes );
		}

		// the router also needs scoring parameters:
		for ( RoutingMode mode : RoutingMode.values() ) {
			ModeParams params = new ModeParams(mode.name());
			config.planCalcScore().addModeParams(params);
		}

//		config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		//		ConfigUtils.setVspDefaults(config);

		// ---

		scenario = ScenarioUtils.createScenario(config);
		// (this is _created_ already here so that the scenario pointer can be final)

		// ---

		this.agentManager = new PAAgentManager(eventsMonitors) ;

	}

	public Config loadAndPrepareConfig() {
		// currently already done in constructor.  But I want to have this more
		// expressive than model.getConfig().  kai, nov'17
		configLoaded = true ;
		return config ;
	}

	public Scenario loadAndPrepareScenario() {
		if ( !configLoaded ) {
			loadAndPrepareConfig() ;
		}

		ScenarioUtils.loadScenario(scenario) ;

		scenarioLoaded=true ;
		return scenario ;
	}

	@Override
	public void init(Object[] args) {
		List<String> bdiAgentIDs = (List<String>)args[0];
		if ( !scenarioLoaded ) {
			loadAndPrepareScenario() ;
		}

		// yy this could now be done in upstream code.  But since upstream code is user code, maybe we don't want it in there?  kai, nov'17
		for(String agentId: bdiAgentIDs) {
			agentManager.createAndAddBDIAgent(agentId);

			// default actions:
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					ActionList.DRIVETO, new ActionHandlerForDriveto(this) );
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					ActionList.REPLAN_CURRENT_DRIVETO, new ActionHandlerForReplanDriveto(this) );
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					ActionList.PERCEIVE, new ActionHandlerForPerceive(this));
		}
		{
			// New default activity types
			{
				ActivityParams params = new ActivityParams("DriveTo");
				params.setScoringThisActivityAtAll(false);
				scenario.getConfig().planCalcScore().addActivityParams(params);
			}
			{
				ActivityParams params = new ActivityParams("Replan");
				params.setScoringThisActivityAtAll(false);
				scenario.getConfig().planCalcScore().addActivityParams(params);
			}
			// Any extra activity types provided by the caller
			if (args.length>1) {
				try {
					List<String> activityNames = (List<String>)args[1];
					for (String activity : activityNames) {
						ActivityParams ap = new ActivityParams(activity);
						ap.setScoringThisActivityAtAll(false);
						scenario.getConfig().planCalcScore().addActivityParams(ap);
					}
				} catch (Exception e) {
					log.error("Could not parse expected list of activity names from: {}", args[1]);
					e.printStackTrace();
				}

			}

		}
		// ---

		controller = new Controler( scenario );

		controller.getEvents().addHandler(eventsMonitors);

		// Register any supplied event handlers
		if (eventHandlers != null) {
			for (EventHandler handler : eventHandlers) {
				controller.getEvents().addHandler(handler);
			}
		}

		// infrastructure at QSim level (separating line not fully logical)
		controller.addOverridingQSimModule( new AbstractQSimModule() {
			@Override protected void configureQSim() {
				this.bind(Replanner.class).in( Singleton.class ) ;
				this.bind( MATSimModel.class ).toInstance( MATSimModel.this );

				// define the turn acceptance logic that reacts to blocked links:
				{
					ConfigurableQNetworkFactory qNetworkFactory = new ConfigurableQNetworkFactory( controller.getEvents(), scenario );
					qNetworkFactory.setTurnAcceptanceLogic( new TurnAcceptanceLogic() {
						TurnAcceptanceLogic delegate = new DefaultTurnAcceptanceLogic();

						@Override
						public AcceptTurn isAcceptingTurn( Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh, QNetwork qNetwork, double now ) {

							AcceptTurn accept = delegate.isAcceptingTurn( currentLink, currentLane, nextLinkId, veh, qNetwork, now );

							QLinkI nextQLink = qNetwork.getNetsimLink( nextLinkId );
							double speed = nextQLink.getLink().getFreespeed( now );
							if ( speed < 0.1 ) { // m/s
								accept = AcceptTurn.WAIT;
								Id<Person> driverId = veh.getDriver().getId();
								Id<Link> currentLinkId = veh.getCurrentLink().getId();
								Id<Vehicle> vehicleId = veh.getId();
								Id<Link> blockedLinkId = nextLinkId;
								NextLinkBlockedEvent nextLinkBlockedEvent = new NextLinkBlockedEvent( now, vehicleId,
										driverId, currentLinkId, blockedLinkId );
								log.debug(nextLinkBlockedEvent.toString());
								controller.getEvents().processEvent( nextLinkBlockedEvent );
								// yyyy this event is now generated both here and in the agent.  In general,
								// it should be triggered in the agent, giving the bdi time to compute.  However, the
								// blockage may happen between there and arriving at the node ...  kai, dec'17

							}
							//log.debug( "time=" + MATSimModel.this.getTime() + ";\t fromLink=" + currentLink.getId() +
							//			     ";\ttoLink=" + nextLinkId + ";\tanswer=" + accept.name() );
							return accept;
						}
					} );
					bind( QNetworkFactory.class ).toInstance( qNetworkFactory );
				}
			}
		} );

		// infrastructure at Controler level (separating line not fully logical)
		controller.addOverridingModule(new AbstractModule(){
			@Override public void install() {

				this.bind( MobsimDataProvider.class ).in( Singleton.class ) ;
				this.addMobsimListenerBinding().to( MobsimDataProvider.class ) ;
				// (pulls mobsim from Listener Event.  maybe not so good ...)

				// analysis:
				this.addControlerListenerBinding().to( OutputEvents2TravelDiaries.class );

				this.addMobsimListenerBinding().toInstance((MobsimInitializedListener) e -> {
					// memorize the qSim:
					qSim = (QSim) e.getQueueSimulation() ;

					// start the playPause functionality
					playPause = new PlayPauseSimulationControl( qSim ) ;
					playPause.pause();

					//						initialiseVisualisedAgents() ;
				}) ;
			}
		}) ;
		modelInitialised = true;

	}

	@Override
	public void start() {
		if (!modelInitialised) {
			log.warn("Model not initialised; cannot be run");
			return;
		}
		// wrap the controller into a thread and start it:
		this.matsimThread = new Thread( controller ) ;
		this.matsimThread.setName("matsim");
		matsimThread.start();

		// wait until the thread has initialized before returning:
		while( this.playPause==null ) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


	}

	public final Replanner getReplanner() {
		return this.replanner ;
	}

	@Override
	public Object[] step(double time, Object[] args) {
		if (args != null && args[0] != null && args[0] instanceof AgentDataContainer) {
			AgentDataContainer inAdc = (AgentDataContainer) args[0];
			return new Object[]{takeControl(time, inAdc)};
		}
		return null;
	}

	@Override public final AgentDataContainer takeControl(double time, AgentDataContainer agentDataContainer){
		runUntilV2( (int)(playPause.getLocalTime() + 1), agentDataContainer ) ;
		return adc;
	}

	public final void runUntilV2( long newTime , AgentDataContainer inAdc) {
		log.trace("Received {} ", inAdc);
		agentManager.updateActions(inAdc, adc);
		playPause.doStep( (int) (newTime) );
		agentManager.addTimePerceptForLapsedTimers(adc, newTime);
	}

	@Override
	public void setAgentDataContainer(AgentDataContainer adc) {
		this.adc = adc;
	}

	@Override
	public AgentDataContainer getAgentDataContainer() {
		return adc;
	}


	public final boolean isFinished() {
		return playPause.isFinished() ;
	}

	@Override
	public void finish() {
		playPause.play();
		while( matsimThread.isAlive() ) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public final Scenario getScenario() {
		// needed in the BDIActionHandlers!
		return this.scenario ;
	}

	public void setEventHandlers(List<EventHandler> eventHandlers) {
		this.eventHandlers = eventHandlers;
	}

	private final void setFreeSpeedExample(){
		// example how to set the freespeed of some link to zero:
		final double now = this.qSim.getSimTimer().getTimeOfDay();
		if ( now == 0.*3600. + 6.*60. ) {
			NetworkChangeEvent event = new NetworkChangeEvent( now ) ;
			event.setFreespeedChange(new NetworkChangeEvent.ChangeValue( NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS,  0. ));
			event.addLink( scenario.getNetwork().getLinks().get( Id.createLinkId( 51825 )));
			NetworkUtils.addNetworkChangeEvent( scenario.getNetwork(),event);

			for ( MobsimAgent agent : this.getMobsimDataProvider().getAgents().values() ) {
				if ( !(agent instanceof MATSimStubAgent) ) {
					this.getReplanner().reRouteCurrentLeg(agent, now);
				}
			}
		}
	}

	public final void registerDataServer( DataServer server ) {
		this.dataServer = server;
		server.subscribe(this, PerceptList.TAKE_CONTROL_ABM);
	}

	@Override public void receiveData(double time, String dataType, Object data) {
		// receiveData can be called prior to runUntil for a new time step,
		// which can be at a granularity greater than 1 sec. In that case
		// time != getTime() is excepted so am disabling this check. DS,Nov18
		//
		//if ( time+1 < getTime() || time-1 > getTime() ) {
		//	log.error( "given time in receiveData is {}, simulation time is {}.  " +
		//					   "Don't know what that means.  Will use given time.",
		//			time, getTime() );
		//}
		double now = time; //getTime() ;

		switch( dataType ) {
			case PerceptList.TAKE_CONTROL_ABM:
				dataListeners.get(dataType).receiveData(now, dataType, data);
				break;
			default:
				throw new RuntimeException("Unknown data type received: " + dataType) ;
		}
	}

	/**
	 * Creates a listener for each type of message we expect from the DataServer
	 * @return
	 */
	private Map<String, DataClient> createDataListeners() {
		Map<String, DataClient> listeners = new  HashMap<>();

		listeners.put(PerceptList.TAKE_CONTROL_ABM, (DataClient<io.github.agentsoz.bdiabm.v2.AgentDataContainer>) (time, dataType, data) -> {
			synchronized (sequenceLock) {
				adc.clear();
				runUntilV2((long) time, data);
				synchronized (this.getAgentManager().getAgentDataContainerV2()) {
					copy(this.getAgentManager().getAgentDataContainerV2(), adc);
					this.getAgentManager().getAgentDataContainerV2().clear();
				}
				dataServer.publish(PerceptList.AGENT_DATA_CONTAINER_FROM_ABM, adc);
			}
		});

		return listeners;
	}

	private void copy(io.github.agentsoz.bdiabm.v2.AgentDataContainer from, io.github.agentsoz.bdiabm.v2.AgentDataContainer to) {
		if (from != null) {
			Iterator<String> it = from.getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				// Copy percepts
				Map<String, PerceptContent> percepts = from.getAllPerceptsCopy(agentId);
				for (String perceptId : percepts.keySet()) {
					PerceptContent content = percepts.get(perceptId);
					to.putPercept(agentId, perceptId, content);
				}
				// Copy actions
				Map<String, ActionContent> actions = from.getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent content = actions.get(actionId);
					to.putAction(agentId, actionId, content);
				}
			}
		}
	}


	public void addNetworkChangeEvent(double speedInMpS, Link link, double startTime) {
		NetworkChangeEvent changeEvent = new NetworkChangeEvent( startTime ) ;
		changeEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue(
				NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS,  speedInMpS
		) ) ;
		changeEvent.addLink( link ) ;

		// (1) add to mobsim:
		this.qSim.addNetworkChangeEvent(changeEvent);

		// (2) add to replanner:
		this.replanner.addNetworkChangeEvent(changeEvent);
		// yyyy wanted to delay this until some agent has actually encountered it.  kai, feb'18

	}
	
	public static double convertTimeToSeconds(String startHHMM) {
		int hours = Integer.parseInt(startHHMM.substring(0, 2));
		int minutes = Integer.parseInt(startHHMM.substring(2, 4));
		double startTime = hours * 3600 + minutes * 60;
		log.debug("orig={}, hours={}, min={}, sTime={}", startHHMM, hours, minutes, startTime);
		return startTime;
	}
	

	public final double getTime() {
		return this.qSim.getSimTimer().getTimeOfDay() ;
	}

	@Override public Object queryPercept(String agentID, String perceptID, Object args) throws AgentNotFoundException {
		log.debug("received query from agent {} for percept {} with args {}", agentID, perceptID, args);
		MobsimAgent mobsimAgent = this.getMobsimAgentFromIdString(agentID) ;
		if (mobsimAgent == null) {
			throw new AgentNotFoundException("MobsimAgent " + agentID + " not found");
		}
		switch(perceptID) {
			case PerceptList.REQUEST_LOCATION:
				final Link link = scenario.getNetwork().getLinks().get( mobsimAgent.getCurrentLinkId() );
				Location[] coords = {
						new Location(link.getId().toString() + ":" + link.getFromNode().getId().toString(), link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()),
						new Location(link.getId().toString() + ":" + link.getToNode().getId().toString(), link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())
				};
				return coords;
			case PerceptList.REQUEST_DRIVING_DISTANCE_TO :
				if (args == null || !(args instanceof double[])) {
					throw new RuntimeException("Query percept '"+perceptID+"' expecting double[] coordinates argument, but found: " + args);
				}
				double[] dest = (double[]) args;
				Coord coord = new Coord( dest[0], dest[1] ) ;
				final Link destLink = NetworkUtils.getNearestLink(getScenario().getNetwork(), coord );
				Gbl.assertNotNull(destLink);
				final Link currentLink = scenario.getNetwork().getLinks().get( mobsimAgent.getCurrentLinkId() );
				final double now = getTime();
				//final Person person = scenario.getPopulation().getPersons().get(agentID);
				double res = 0.0;
				synchronized (this.replanner) {
					LeastCostPathCalculator.Path result = this.replanner.editRoutes(RoutingMode.carFreespeed).getPathCalculator().calcLeastCostPath(
							currentLink.getFromNode(), destLink.getFromNode(), now, null, null
					);
					res = RouteUtils.calcDistance(result);
				}
				return res;
			case PerceptList.REQUEST_DESTINATION_COORDINATES :
				double[] cords= {-1,-1};
				if(this.getReplanner().editPlans().isAtRealActivity(mobsimAgent)){ // if agent is currently in an activity

					int currentIndex = EditPlans.getCurrentPlanElementIndex(mobsimAgent);
					Plan plan = WithinDayAgentUtils.getModifiablePlan(mobsimAgent);
					if(currentIndex < plan.getPlanElements().size() - 1){ // not the last activity
						Activity nextAct = this.getReplanner().editPlans().findRealActAfter(mobsimAgent,currentIndex);
						cords[0] = nextAct.getCoord().getX();
						cords[1] = nextAct.getCoord().getY();
					}

				}
				else{ // if agent is currently in a leg
					
					Activity destAct = this.getReplanner().editTrips().findCurrentTrip(this.getMobsimAgentFromIdString(agentID)).getDestinationActivity();
					cords[0] = destAct.getCoord().getX();
					cords[1] = destAct.getCoord().getY();
				}
				return cords;
			default:
				throw new RuntimeException("Unknown query percept '"+perceptID+"' received from agent "+agentID+" with args " + args);
		}
	}

	public PAAgentManager getAgentManager() {
		return agentManager;
	}

	public MobsimDataProvider getMobsimDataProvider() {
		return mobsimDataProvider;
	}

	public MobsimAgent getMobsimAgentFromIdString( String idString ) {
		return this.getMobsimDataProvider().getAgent( Id.createPersonId(idString) ) ;
	}

	public EventsManager getEvents() {
		return this.qSim.getEventsManager() ;
	}

	public Config getConfig() {
		return config;
	}

	public double getOptCongestionEvaluationInterval() {
		return optCongestionEvaluationInterval;
	}

	public double getOptCongestionToleranceThreshold() {
		return optCongestionToleranceThreshold;
	}

	public double getOptCongestionReactionProbability() {
		return optCongestionReactionProbability;
	}

    public void useSequenceLock(Object sequenceLock) {
	    this.sequenceLock = sequenceLock;
    }

	public Controler getControler() {
		return controller;
	}
}
