package io.github.agentsoz.bushfiretute.matsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Replanner;

final class CustomReplanner extends Replanner{

	CustomReplanner(MATSimModel model, QSim activityEndRescheduler) {
		super(model, activityEndRescheduler);
	}

	static final void insertPickupAndWaitAtOtherLocation(Id<Person> agentId, Id<Link> newActivityLinkId, int pickupTime, MATSimModel model ) {
		// called at least once
		
		logger.debug("starting addNewLegAndActvityToPlan method..");
		double now = model.getTime() ; 
		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;

		int currentPlanIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		logger.trace("number of plan elements: " + plan.getPlanElements().size());
		logger.trace("current plan index : " + currentPlanIndex);

		PlanElement currentPE = plan.getPlanElements().get(currentPlanIndex);
		if ( ! ( currentPE instanceof Activity ) ) {
			return ;
		}
		Activity currentAct = (Activity) currentPE;
		if(currentAct.getType().equals("Wait")) {
			// yyyy why only for wait activity here, and for all activities in compagnon method?  kai, oct'17
			//ending the current activity if its a Wait activity:
			logger.debug("Wait/Current activity ending now:  {}", now);
			currentAct.setEndTime(now);
		}

		final PopulationFactory pf = model.getScenario().getPopulation().getFactory();

		// leg/route from current activity/position to pickup activity:
		Leg newLeg = pf.createLeg(TransportMode.car);
		model.getReplanner().getEditRoutes().relocateFutureLegRoute(newLeg, currentAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson() );
		logger.debug(" inserting leg into plan..");
		plan.getPlanElements().add(currentPlanIndex+1,newLeg);

		//ADDING Pickup ACTIVITY
		Activity pickupAct = pf.createActivityFromLinkId("Pickup", newActivityLinkId ) ;
		pickupAct.setMaximumDuration(pickupTime);
		logger.debug(" added {} type activity",pickupAct.getType());
		plan.getPlanElements().add(currentPlanIndex+2,pickupAct);

		//ADDING Wait ACTIVITY
		Activity waitAct = pf.createActivityFromLinkId("Wait", newActivityLinkId ) ;
		waitAct.setEndTime( Double.POSITIVE_INFINITY ) ;
		logger.debug(" added {} type activity with INFINITY end time..",waitAct.getType());
		plan.getPlanElements().add(currentPlanIndex+3,waitAct);

		//rerouting the leg after wait activity
		logger.debug("reRouting the leg after wait activity");
		Leg nextLeg = (Leg)plan.getPlanElements().get(currentPlanIndex+4);
		Activity nextAct = (Activity)plan.getPlanElements().get(currentPlanIndex+5);
		logger.trace("all evac activity info : {} ", nextAct.toString());
		model.getReplanner().getEditRoutes().relocateFutureLegRoute(nextLeg,newActivityLinkId,nextAct.getLinkId(),((HasPerson)agent).getPerson() );

		logger.trace("number of plan elements after adding pickup, wait, new leg : " + plan.getPlanElements().size());

		WithinDayAgentUtils.resetCaches(agent);
		model.getReplanner().getEditPlans().rescheduleActivityEnd(agent);

	}
	final boolean changeActivityEndTimeByActivityType(Id<Person> agentId, String actType, double newEndTime) {
		// used at least once
		
		logger.debug("received to modify the endtime of activity {}", actType);
		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);

		Activity activityToChange=null;
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		int evacIndex= 100;
		logger.debug("forceEndActivity: agent {} | current plan Index  {} | current time : {} ",agent.getId().toString(), currentIndex, model.getTime() );

		if (actType.equals("Current")) 
		{ 
			if( !(plan.getPlanElements().get(currentIndex) instanceof Activity) ) {
				logger.error("current plan element is not an activity, unable to forceEndActivity");
				return false;
			}
			activityToChange = (Activity) plan.getPlanElements().get(currentIndex);
		}

		if(actType.equals("Evacuation")) 
		{
			for(int i=currentIndex;i<plan.getPlanElements().size();i++) {
				PlanElement element =  plan.getPlanElements().get(i);
				if( !(element instanceof Activity) ) {
					continue;
				}
				if(((Activity) element).getType().equals("Evacuation")) {
					activityToChange=(Activity) element;
					evacIndex = i;
				}				
			}
		}
		//delaying activity by newEndTime
		if(activityToChange == null) {
			logger.error("could not find the activity to End");
			return false;
		}
		double endtime =  model.getTime() +  newEndTime;
		logger.debug("change end time of actvity with index {} to new end time  {} ", evacIndex, endtime );
		activityToChange.setEndTime(endtime);

		WithinDayAgentUtils.resetCaches(agent);
		//		if ( currentIndex != 1) // ??
		this.qsim.rescheduleActivityEnd(agent);
		return true;


	}
	/*
	void insertTempActivity(Id<Person> agentId) {
		// possibly never used
		
		Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
		MobsimAgent agent = mapping.get(agentId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;

		List<PlanElement> planElements = plan.getPlanElements() ;

		Activity evacAct = (Activity) planElements.get(2);
		//Temp Activivity 
		Activity tempAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("Temp", evacAct.getLinkId() ) ;
		tempAct.setEndTime(5.0);

		planElements.add(2,tempAct);

		WithinDayAgentUtils.resetCaches(agent);
		this.qsim.rescheduleActivityEnd(agent);
	}
	*/


}
