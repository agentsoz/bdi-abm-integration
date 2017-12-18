/**
 * 
 */
package io.github.agentsoz.bushfiretute;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import io.github.agentsoz.util.TestUtils;


/**
 * @author kainagel
 */
public class Bushfire1Test {
	private static final Logger log = Logger.getLogger( Bushfire1Test.class) ;
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@SuppressWarnings("static-method")
	@Test
	public void testOneAgent() {
		
		String[] args2 = {"-c", "scenarios/hawkesbury/overallConfigOneAgent.xml", "-seed", "4711", "-loglevel", "INFO",
				"--matsim-output-directory", utils.getOutputDirectory()};
		BushfireMain.main(args2);
		
		// ---
		
		final String actualEventsFilename = utils.getOutputDirectory() + "/run0.output_events.xml.gz";
		long actualEvents = CRCChecksum.getCRCFromFile(actualEventsFilename);
		log.info("actual(events)=" + actualEvents);
		
		long actualPlans = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/run0.output_plans.xml.gz");
		log.info("actual(plans)=" + actualPlans);
		
		String primaryExpectedEventsFilename = utils.getInputDirectory() + "/run0.output_events.xml.gz" ;
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 5.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);
		
//		{
//			List<Long> expecteds = Bushfire5Test.getExpectedsFromFiles(utils.getInputDirectory(), "run0.output_events.xml.gz");
//			log.warn("done with retrieving expecteds") ;
//			TestUtils.checkSeveral(expecteds, actualEvents);
//			log.warn("checking events files was successful") ;
//		}
		{
			List<Long> expecteds = Bushfire5Test.getExpectedsFromFiles(utils.getInputDirectory(), "run0.output_plans.xml.gz");
			log.warn("done with retrieving expecteds") ;
			TestUtils.checkSeveral(expecteds, actualPlans);
			log.warn("checking plans files was successful") ;
		}
		
	}
	
}
