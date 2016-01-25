package io.github.agentsoz.vaccination;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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

import io.github.agentsoz.vaccination.controller.ABMConnector;
import io.github.agentsoz.vaccination.controller.BDIConnector;
import ch.qos.logback.classic.Level;



public class Program {

	// Defaults
	private static String logFile = Program.class.getName() + ".log";
    private static Level logLevel = Level.INFO;
	private static String host = "localhost";
	private static int port = 2653;

	public static void main(String args[]) {
		
        // parse command line arguments
        parse(args);

		// create the logger
		Log.createLogger("", logLevel, logFile);
		
        // Create the BDI and ABM systems
		BDIConnector bdiSystem = new BDIConnector();
		ABMConnector abmSystem = new ABMConnector();
		abmSystem.setBdiSystem(bdiSystem);

		// Start the ABM thread and wait for it to finish
		try {
			abmSystem.start(host, port);
			abmSystem.join();
		} catch (Exception e) {
			Log.error(e.getMessage());
		}
		
		// Add a delay before exiting, to allow connected
		// processes to finish gracefully
		try { Thread.sleep(500); } catch (Exception e) {}
		System.exit(0);
	}
	
    public static String usage() {
    	return "usage: " + Program.class.getName() + "  [options]\n"
    			+ "  -h                   print this help message and exit\n"
    			+ "  -host <name>         hostname for connection (default is '"+host+"')\n"
                + "  -level <loglevel>    one of ERROR,WARN,INFO,DEBUG,TRACE (default is "+logLevel+")\n"
                + "  -log <file>          log file name (default is "+logFile+")\n"
                + "  -port <num>          port number for connection (default is '"+port+"')\n"
    			;
    }
    
    private static void abort(String err) {
    	if (err != null) {
    		System.out.println("\nERROR: " + err + "\n");
    	}
        System.out.println(usage());
        System.exit(0);
    }

	public static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-h":
				usage();
				abort(null);
				break;
			case "-host":
				if (i + 1 < args.length) {
					i++;
					host = args[i];
				}
				break;
			case "-level":
				if (i + 1 < args.length) {
					i++;
					try {
						logLevel = Level.toLevel(args[i]);
					} catch (Exception e) {
						abort("Option value '" + args[i]
								+ "' is not a know log level");
					}
				}
				break;
			case "-log":
				if (i + 1 < args.length) {
					i++;
					logFile = args[i];
				}
				break;
			case "-port":
				if (i + 1 < args.length) {
					i++;
					try {
						port = Integer.parseInt(args[i]);
					} catch (Exception e) {
						abort("Option value '" + args[i] + "' is not a number");
					}
				}
				break;
			}
		}
	}
}
