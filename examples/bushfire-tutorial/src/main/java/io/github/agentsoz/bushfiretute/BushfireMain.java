package io.github.agentsoz.bushfiretute;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bushfiretute.matsim.ABMModel;
import io.github.agentsoz.util.Global;

public class BushfireMain {
	// Defaults
	private static String logFile = BushfireMain.class.getSimpleName() + ".log";
	private static String outFile = null;
	private static Level logLevel = Level.INFO;
	private static Logger logger = null;
	public static PrintStream writer;


	private static Long seed = null;
	private static String matsimOutputDirectory;

	public static void main(final String[] args) {
		// TODO Auto-generated method stub

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
//			System.exit(-1); // remove system.exit from junit-tested material. kai, nov'17
			throw new RuntimeException("failed to load configuration") ; 
		}

		// Initialise and hook up the BDI side
		BDIModel bdiModel = new BDIModel();
		
		// Start the MATSim controller
		List<String> config = new ArrayList<>() ;
		config.add( Config.getMatSimFile() ) ;
		if ( matsimOutputDirectory != null ) { 
			config.add( MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR ) ;
			config.add( matsimOutputDirectory ) ;
		}
		logger.info( config.toString() );
		new ABMModel(bdiModel).run( config.toArray( new String[config.size()] ) ) ;

//
//		// Initialise the MATSim model
//		// Finally, start the MATSim controller
//		String[] margs = { Config.getMatSimFile(), MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, matsimOutputDirectory };
//		String s = "starting matsim with args:";
//		for (int i = 0; i < margs.length; i++) {
//			s += margs[i];
//		}
//		logger.info(s);
//		abmModel.run(margs);

		// MATSim finished executing, so terminate the BDI model before exiting
		bdiModel.finish();

		writer.close();
//		System.exit(0);
		// prevents to test output afterwards.  kai, oct'17

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

}
