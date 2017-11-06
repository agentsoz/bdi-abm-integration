package io.github.agentsoz.bushfiretute.matsim;



import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.SearchableNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.MATSimActionHandler;
import io.github.agentsoz.bdimatsim.MATSimActionList;
import io.github.agentsoz.bdimatsim.MATSimAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.MATSimPerceptHandler;
import io.github.agentsoz.bdimatsim.MATSimPerceptList;
import io.github.agentsoz.bdimatsim.Replanner;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bdimatsim.app.MATSimApplicationInterface;
import io.github.agentsoz.bushfiretute.BDIModel;
import io.github.agentsoz.bushfiretute.Config;
import io.github.agentsoz.bushfiretute.Util;
import io.github.agentsoz.bushfiretute.datacollection.ScenarioTwoData;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.util.Global;
import scenarioTWO.agents.EvacResident;

public final class ABMModel implements MATSimApplicationInterface {
	static final Logger logger = LoggerFactory.getLogger("");

	private final MATSimModel matsimModel;
	private final BDIModel bdiModel;
	private Replanner replanner = null;

	public ABMModel(BDIModel bdiModel) {
		this.bdiModel = bdiModel;
		this.matsimModel = new MATSimModel(bdiModel);
		matsimModel.registerPlugin(this);
	}

	/**
	 * Provides a custom Replanner (extended) to use with MATSim.
	 */
	@Override
	public Replanner getReplanner(QSim qsim) {
		if (replanner == null) {
			replanner = new Replanner(matsimModel, qsim);
		}
		// (singleton pattern!)
		return replanner;
	}

	/**
	 * Use this to pre-process the BDI agents list if needed. For instance, 
	 * tasks like adding/removing specific agents, or renaming agents IDs, 
	 * should be done here. This function is called just prior to the
	 * BDI agent counterparts in MATSim being created.
	 * 
	 */
	@Override
	public void notifyBeforeCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {
	}

	/**
	 * Initialise the BDI agents with any application specific data. This 
	 * function is just immediately after the MATSim counterparts have been 
	 * created. 
	 */
	@Override
	public void notifyAfterCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {

		Map<Id<Link>,? extends Link> links = matsimModel.getScenario().getNetwork().getLinks();
		for (Id<Person> agentId : bdiAgentsIDs) {
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
			if (bdiAgent == null) {
				logger.warn("No BDI counterpart for MATSim agent '" + agentId
						+ "'. Should not happen, but will keep going");
				continue;
			}
			Plan plan = WithinDayAgentUtils.getModifiablePlan(matsimModel.getMobsimAgentMap().get(agentId));
			List<PlanElement> planElements = plan.getPlanElements();

			// Assign start location
			double lat = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getX();
			double lon = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getY();
			bdiAgent.startLocation = new double[] { lat, lon };
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

			// Assign dependent persons (to pick up before evacuating)
			assignDependentPersons(bdiAgent);
		}
	}

	/**
	 * This is where we register all application specific BDI actions, and/or
	 * overwrite default ones (like {@link MATSimActionList#DRIVETO}). 
	 * <p>
	 * This is also the place to register action-dependent percepts. 
	 * For instance, {@link MATSimPerceptList.ARRIVED} is conditional on the 
	 * agent arriving at the network link in action 
	 * {@link MATSimActionList.DRIVETO}, and so must be registered at the 
	 * same time.
	 * <p>
	 * Action-independent percepts should be registered using
	 * {@link this#registerNewBDIPercepts(MATSimPerceptHandler)}.
	 * <p>
	 * Note that actions/percepts are registered <strong>per agent</strong>,
	 * i.e. handlers passed in belong to specific agents.
	 */

	@Override
	public void registerNewBDIActions(MATSimActionHandler withHandler) {
		// overwrite default DRIVETO
		withHandler.registerBDIAction(MATSimActionList.DRIVETO, new DRIVETOActionHandler(bdiModel));

		// register new action
		withHandler.registerBDIAction(ActionID.CONNECT_TO, new CONNECT_TOActionHandler(bdiModel));

		// register new action
		withHandler.registerBDIAction(ActionID.DRIVETO_AND_PICKUP, new DRIVETO_AND_PICKUPActionHandler(bdiModel));

		// register new action
		withHandler.registerBDIAction(ActionID.SET_DRIVE_TIME, new SET_DRIVE_TIMEActionHandler(bdiModel));
	}

	/**
	 * Register any action-independent percepts here. Percepts that are
	 * conditional on actions (such as {@link MATSimPerceptList#ARRIVED} that
	 * is specific {@link ActionID#DRIVETO}
	 */
	@Override
	public void registerNewBDIPercepts(MATSimPerceptHandler withHandler) {
		// For all agents, register a percept for when they arrive at the safe
		// destination. We do this here, irrespective of whether there is any
		// BDI reasoning involved, i.e., where the percept is not conditional
		// on a BDI (drive) action. 
		// Such as for MATSim agents that leave as
		// planned (according to their MATSim plan). 
		// FIXME: add Safe arrival percept for all agents (in a for loop)

		for (Id<Person> agentID : matsimModel.getBDIAgentIDs()) {
			MATSimAgent agent = matsimModel.getBDIAgent(agentID);
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
			Id<Link> newLinkId;
			newLinkId = ((SearchableNetwork) matsimModel.getScenario().getNetwork())
					.getNearestLinkExactly(new Coord(bdiAgent.endLocation[0], bdiAgent.endLocation[1])).getId();

			agent.getPerceptHandler().registerBDIPerceptHandler(agent.getAgentID(),
					MonitoredEventType.ArrivedAtDestination, newLinkId, new BDIPerceptHandler() {
				@Override
				public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent,
						MATSimModel model) {
					MATSimAgent agent = model.getBDIAgent(agentId);
					EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
					Object[] params = { "Safe" , Long.toString(bdiAgent.getCurrentTime())};
					agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
					return true; // unregister this handler
				}
			});
		}
	}

	@Override
	public void run(String[] args) {
		matsimModel.run(args);
	}

	/**
	 * Randomly assign dependent persons to be picked up. Uses 
	 * Pk ({@link Config#getProportionWithKids()}) and 
	 * Pr ({@link Config#getProportionWithRelatives()) probabilities to calculate
	 * normalised probabilities, and then allocate kids and/or relatives
	 * with those probabilities. If both input probabilities are non-zero,
	 * then all four allocations are possible (no kids or relatives, one or the
	 * other, both kids and relatives). 
	 * <p>
	 * Some examples:
	 * <ul> 
	 * <li> Pk=0.0, Pr=0.0: results in always no kids or relatives</li>
	 * <li> Pk=0.0, 0.0&lt;Pr&lt;1.0: results in always relatives</li>
	 * <li> 0.0&lt;Pk&lt;1.0, Pr=0.0: results in always kids</li>
	 * <li> 0.0&lt;Pk&lt;1.0, 0.0&lt;Pr&lt;1.0: results in all four combinations of kids and relatives</li>
	 * </ul>
	 * 
	 * @param bdiAgent
	 */
	private static void assignDependentPersons(EvacResident bdiAgent) {
		if( ScenarioTwoData.totPickups <= Config.getMaxPickUps() ) {
			double[] pDependents = {Config.getProportionWithKids(), Config.getProportionWithRelatives()};
			pDependents = Util.normalise(pDependents);
			Random random = Global.getRandom();

			if (random.nextDouble() < pDependents[0]) {
				// Allocate dependent children
				ScenarioTwoData.agentsWithKids++;
				double[] sclCords = Config.getRandomSchoolCoords(bdiAgent.getId(),bdiAgent.startLocation);
				if(sclCords != null) { 
					bdiAgent.kidsNeedPickUp = true;  
					bdiAgent.schoolLocation = sclCords;
					bdiAgent.prepared_to_evac_flag = false;
					ScenarioTwoData.totPickups++;
					bdiAgent.log("has children at school coords " 
							+ sclCords[0] + "," +sclCords[1]);
				}
				else{
					bdiAgent.log("has children but there are no schools nearby");
					ScenarioTwoData.agentsWithKidsNoSchools++;
				}
			}
			if (random.nextDouble() < pDependents[1]) {
				// Allocate dependent adults
				ScenarioTwoData.agentsWithRels++;
				bdiAgent.relsNeedPickUp = true;
				bdiAgent.prepared_to_evac_flag = false;
				ScenarioTwoData.totPickups++;
				bdiAgent.log("has relatives");
			}
			if (!bdiAgent.relsNeedPickUp && !bdiAgent.kidsNeedPickUp) {
				bdiAgent.log("has neither children nor relatives");
			}
		}
	}

}