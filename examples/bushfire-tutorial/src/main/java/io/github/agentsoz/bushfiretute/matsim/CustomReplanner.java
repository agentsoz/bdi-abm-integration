package io.github.agentsoz.bushfiretute.matsim;

import org.matsim.core.mobsim.qsim.QSim;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Replanner;

final class CustomReplanner extends Replanner{

	CustomReplanner(MATSimModel model, QSim activityEndRescheduler) {
		super(model, activityEndRescheduler);
	}


}
