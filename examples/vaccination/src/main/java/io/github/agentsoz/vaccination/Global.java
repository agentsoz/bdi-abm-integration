package io.github.agentsoz.vaccination;

import java.util.Random;

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

public class Global {

	/**
	 * A global random number generator. 
	 * You may assign to this a new object with a known
	 * seed, if you want to replicate runs.
	 */
	private static Random random = null;
	
	/** 
	 * A singleton Random object 
	 * @return
	 */
	public static Random getRandomInstance() {
		return (random == null) ? new Random() : random;
	}
	
	/**
	 * Message strings received from the ABM
	 */
	public enum MessageType {
		START,
		TAKE_CONTROL,
		TERMINATE_PROGRAM
		;
	}

	public class MessageID {
		/**
		 * Action IDs (exchanged with the ABM)
		 */
		public static final String ScheduleVaccination = "ScheduleVaccination";
		public static final String AttendVaccination = "AttendVaccination";
		public static final String DelayVaccination = "DelayVaccination";
		public static final String RejectVaccination = "RejectVaccination";

		/** 
		 * Percept IDs (incoming from the ABM)
		 */
		public static final String VaccTime = "vaccination-time";
	}
	
	/**
	 * Probability that a parent who has been through the
	 * vaccination process, will vaccinate again. Applies
	 * when the parent is considering to vaccinate or not
	 */
	public static final double RE_VACCINATION_PROBABILITY = 0.8;

	
	/**
	 * Possible deliberation decisions. Should really be an enum,
	 * but JACK does not support features past Java 1.4, including enums!
	 */
	public static final String UNDECIDED = "UNDECIDED";
	public static final String DECIDED_TO_VACCINATE = "DECIDED_TO_VACCINATE";
	public static final String DECIDED_TO_DELAY_VACCINATION = "DECIDED_TO_DELAY_VACCINATION";
	public static final String DECIDED_TO_REJECT_VACCINATION = "DECIDED_TO_REJECT_VACCINATION";

		
}