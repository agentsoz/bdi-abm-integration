package io.github.agentsoz.conservation;

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

import java.io.FileWriter;
import java.io.PrintWriter;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

public class Log {

	private static String outFile = null;
	private static String csvFile = null;
	private static PrintWriter csv = null;
	private static Level level = Level.WARN;
	private static Logger logger;
	
	public static void open() {
		// Create a new log file for this repeat
		createLogger("", outFile);
	}

	public static void close() {
		// Close the output file
		try {
			csv.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void setLog(String string, Level lvl) {
		outFile = string;
		level = lvl;
	}

	public static void setCSV(String string) {
		csvFile = string;
		try {
			csv = new PrintWriter(new FileWriter(csvFile, false), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void info(String s) {
		logger.info(s);
	}
	
	public static void trace(String s) {
		logger.trace(s);
	}

	public static void warn(String s) {
		logger.warn(s);
	}
	
	public static void debug(String s) {
		logger.debug(s);
	}
	
	public static void error(String s) {
		logger.error(s);
	}
	
	public static void csvWrite(String s) {
		csv.println(s);
	}

	  private static Logger createLogger(String string, String file) {
	       LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	       PatternLayoutEncoder ple = new PatternLayoutEncoder();
	       ple.setPattern("%date %level [%thread] %logger{10} [%file:%line]%n%msg%n%n");
	       ple.setContext(lc);
	       ple.start();
	       FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
	       fileAppender.setFile(file);
	       fileAppender.setEncoder(ple);
	       fileAppender.setAppend(false);
	       fileAppender.setContext(lc);
	       fileAppender.start();
	       logger = (Logger) LoggerFactory.getLogger(string);
	       logger.detachAndStopAllAppenders(); // detach console (doesn't seem to work)
	       logger.addAppender(fileAppender); // attach file appender
	       logger.setLevel(level);
	       logger.setAdditive(true); /* set to true if root should log too */

	       return logger;
	 }

}
