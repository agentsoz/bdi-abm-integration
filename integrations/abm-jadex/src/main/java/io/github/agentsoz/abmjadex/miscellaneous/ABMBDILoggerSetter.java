package io.github.agentsoz.abmjadex.miscellaneous;

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


import java.io.IOException;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class ABMBDILoggerSetter 
{
	
	private static SimpleFormatter formatter;

	private static boolean isLogToConsole = false;
	private static Level logLevel = Level.INFO;
	private static FileHandler fileText;
	private static ConsoleHandler console;
	private static boolean isInitialized = false;
	
	public static void setup (Logger logger)
	{
		//Clean up all handlers
		Handler[] handlers = logger.getHandlers();
		for (int i = 0; i < handlers.length; i++)
		{
			logger.removeHandler(handlers[i]);
		}
		logger.setUseParentHandlers(false);
		
		if (isLogToConsole == false)
		{
//			Handler[] handlers = logger.getHandlers();
//			for (int i = 0; i < handlers.length; i++)
//			{
//				if (handlers[i].getClass().equals(ConsoleHandler.class))
//					logger.removeHandler(handlers[i]);
//			}

		}
		else
		{
			logger.addHandler(console);
		}
		
		logger.addHandler(fileText);
		
		logger.setLevel(logLevel);
	}
	
	public static void initialized(Properties prop, boolean logConsole, Level logLvl) throws SecurityException, IOException
	{
		if (isInitialized == false)
		{
			//get the build location for log files
			String location = prop.getProperty("logging_location", "Logging.txt");
			//Prepare the handler for the output
			fileText = new FileHandler(location);
			console = new ConsoleHandler();
			
			//format the output files
			formatter = new SimpleFormatter();
			fileText.setFormatter(formatter);
			console.setFormatter(formatter);
			
			isLogToConsole = logConsole;
			logLevel = logLvl;	
			
			isInitialized = true;
		}

	}
}
