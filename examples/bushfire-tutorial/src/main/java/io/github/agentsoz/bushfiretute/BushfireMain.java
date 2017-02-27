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

import java.io.IOException;
import java.util.Random;
import java.util.StringTokenizer;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bushfiretute.Config;
import io.github.agentsoz.dataInterface.DataServer;

public class BushfireMain {

	// Defaults
	private static String logFile = BushfireMain.class.getSimpleName() + ".log";
	private static Level logLevel = Level.INFO;
	private static Logger logger = null;
	
	// all application code should use this same instance of Random
    private static final Random random = new Random(); 
    private static Long seed = null;

	public static void main(final String[] args) throws IOException {
		// TODO Auto-generated method stub

		// Parse the command line arguments
		parse(args);

		// Create the logger
		logger = createLogger("", logFile);
		
	    // add seed to command line args if run replication needed
		if (seed != null) random.setSeed(seed);

		// Read in the configuration
		if (!Config.readConfig()) {
			logger.error("Failed to load configuration from '" + Config.getConfigFile() + "'. Aborting");
			System.exit(-1);
		}

		// Initialise and hook up the BDI side
		BDIModel bdiModel = new BDIModel();

		// Initialise the MATSim model
		ABMModel abmModel = new ABMModel(bdiModel, new MATSimBDIParameterHandler());
		// Finally, start the MATSim controller
		String[] margs = { Config.getMatSimFile() };
		String s = "starting matsim with args:";
		for (int i = 0; i < margs.length; i++) {
			s += margs[i];
		}
		logger.info(s);
		abmModel.run(null, margs);

		// MATSim finished executing, so terminate the BDI model before exiting
		bdiModel.finish();
		System.exit(0);

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
			}
		}
	}

	public static String usage() {
		return "usage:\n" + "  -c <config_file>     simulation configuration file" + "\n"
				+ "  -h                   print this help message and exit\n"
				+ "  -l <logfile>         logging output file name (default is '" + logFile + "')\n"
				+ "  -level <level>       log level; one of ERROR,WARN,INFO,DEBUG,TRACE (default is '" + logLevel
				+ "')\n";
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
		logger.setLevel(logLevel);
		logger.setAdditive(true); /* set to true if root should log too */

		return logger;
	}
	
	static Random getRandom() {
		return random;
	}

}
