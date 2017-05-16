package io.github.agentsoz.conservation.jill.goals;

import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.GoalInfo;

@GoalInfo(hasPlans = { "io.github.agentsoz.conservation.jill.plans.MeetExtensionOfficerPlan" })
public class MeetExtensionOfficerGoal extends Goal {

	public MeetExtensionOfficerGoal(String str) {
		super(str);
	}

}
