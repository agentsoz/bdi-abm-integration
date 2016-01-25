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

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

public class Log {

	private static Logger logger;
	

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
	

	  public static Logger createLogger(String name, Level level, String file) {
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
	       logger = (Logger) LoggerFactory.getLogger(name);
	       logger.detachAndStopAllAppenders(); // detach console (doesn't seem to work)
	       logger.addAppender(fileAppender); // attach file appender
	       logger.setLevel(level);
	       logger.setAdditive(true); // set to true if root should log too

	       return logger;
	 }

}
