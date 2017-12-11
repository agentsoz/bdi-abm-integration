/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
public class BradfordNewsteadFireCrossing400Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = Logger.getLogger(BradfordNewsteadFireCrossing400Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@SuppressWarnings("static-method")
	@Test
	public void test() {

		String [] args = {
				"--config",  "scenarios/bradford-newstead-fire-crossing/scenario_main.xml",
				"--logfile", "scenarios/bradford-newstead-fire-crossing/scenario.log",
				"--loglevel", "INFO",
				//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", "scenarios/bradford-newstead-fire-crossing/safeline.%d%.out",
				MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:400}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/bradford-newstead-fire-crossing/jill.log\","+
						"programOutputFile: \"scenarios/bradford-newstead-fire-crossing/jill.out\","+
						"randomSeed: 12345"+ // jill random seed
						//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}"
		};

		Main.main(args);

		// print these so that we have them:
		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
		log.warn("actual(events)="+actualEventsCRC) ;

		long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		log.warn("actual(plans)="+actualPlansCRC) ;
		
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 5.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, true);

		
		long [] expectedPlans = new long [] {
				CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" )
		} ;
		
		TestUtils.checkSeveral(expectedPlans, actualPlansCRC);

	}
}