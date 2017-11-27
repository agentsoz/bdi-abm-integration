/**
 * 
 */
package io.github.agentsoz.bushfiretute;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import io.github.agentsoz.util.TestUtils;


/**
 * @author kainagel
 *
 */
public class Bushfire5Test {
	private static final Logger log = Logger.getLogger( Bushfire5Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@SuppressWarnings("static-method")
	@Test
	public void testFiveAgents() {

		String [] args2 = {"-c", "scenarios/hawkesbury/overallConfigFiveAgents.xml", "-seed", "4711", "-loglevel", "INFO", 
				"--matsim-output-directory", utils.getOutputDirectory() } ;
		BushfireMain.main( args2 );

		// ============
		
		final String actualEventsFilename = utils.getOutputDirectory() + "/run0.output_events.xml.gz";
		long actualEvents = CRCChecksum.getCRCFromFile(actualEventsFilename) ;
		log.warn("actual(events)="+actualEvents) ;

		long actualPlans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/run0.output_plans.xml.gz" ) ;
		log.warn("actual(plans)="+actualPlans);
		
		{
//			SortedMap<Id<Person>, List<Double>> actuals = TestUtils.collectArrivals(actualEventsFilename);
//			SortedMap<Id<Person>, List<Double>> expecteds = TestUtils.collectArrivals(utils.getInputDirectory() + "run0.output_events.xml.gz");
//			TestUtils.compareEventsWithSlack(expecteds, actuals, 500.);
		}

		{
			List<Long> expecteds = getExpectedsFromFiles( utils.getInputDirectory() , "run0.output_events.xml.gz" );
			expecteds.add(3907061199L) ; // tub gitlab
			expecteds.add(3810517158L) ; // tub gitlab
			log.warn("done with retrieving expecteds") ;
			TestUtils.checkSeveral(expecteds, actualEvents);
			log.warn("checking events files was successful") ;
		}
		{
			List<Long> expecteds = getExpectedsFromFiles( utils.getInputDirectory() , "run0.output_plans.xml.gz" );
			expecteds.add(2959191893L); // tub gitlab
			expecteds.add(1604079186L) ; // tub gitlab
			log.warn("done with retrieving expecteds") ;
			TestUtils.checkSeveral(expecteds, actualPlans);
			log.warn("checking events files was successful") ;
		}
		
	}

	public static List<Long> getExpectedsFromFiles(final String baseDir, final String cmpFileName ) {
		List<Long> expecteds = new ArrayList<>() ;
		try {
			Files.walkFileTree(new File(baseDir).toPath(), new SimpleFileVisitor<Path>() {
				@Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					final String filename = dir + "/" + cmpFileName;
					log.info( "checking for existence of " + filename );
					if ( Files.exists( new File( filename).toPath() ) ) {
						log.info( "will check against " + filename );
						long crc = CRCChecksum.getCRCFromFile( filename ) ;
						expecteds.add(crc) ;
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
		return expecteds ;
	}


	
}
